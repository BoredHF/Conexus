package com.boredhf.conexus.communication;

import com.boredhf.conexus.transport.TransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Default implementation of MessagingService.
 * 
 * Provides cross-server messaging with channels, direct messaging,
 * broadcasting, and request/response patterns.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class DefaultMessagingService implements MessagingService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessagingService.class);

    private final String serverId;
    private final TransportProvider transport;
    private final MessageSerializer serializer;

    private final ConcurrentMap<String, MessageChannel> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<? extends Message>, Consumer<MessageContext<? extends Message>>> messageHandlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<Message>> pendingRequests = new ConcurrentHashMap<>(); 
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "Conexus-Messaging-Scheduler-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public DefaultMessagingService(String serverId, TransportProvider transport, MessageSerializer serializer) {
        this.serverId = serverId;
        this.transport = transport;
        this.serializer = serializer;
    }

    public CompletableFuture<Void> initialize() {
        if (!initialized.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Subscribe to direct messaging and broadcast channels
        CompletableFuture<Void> directSub = transport.subscribe("conexus:direct:" + serverId, this::handleDirectMessage);
        CompletableFuture<Void> broadcastSub = transport.subscribe("conexus:broadcast", this::handleBroadcastMessage);
        
        return CompletableFuture.allOf(directSub, broadcastSub);
    }

    public CompletableFuture<Void> shutdown() {
        if (!initialized.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }
        
        CompletableFuture<Void> directUnsub = transport.unsubscribe("conexus:direct:" + serverId);
        CompletableFuture<Void> broadcastUnsub = transport.unsubscribe("conexus:broadcast");
        
        return CompletableFuture.allOf(directUnsub, broadcastUnsub)
                .whenComplete((v, t) -> scheduler.shutdownNow());
    }

    @Override
    public MessageChannel getChannel(String channelName) {
        return channels.computeIfAbsent(channelName, name -> 
            new DefaultMessageChannel<>(name, Message.class, transport, serializer, serverId));
    }

    @Override
    public <T extends Message> MessageChannel<T> createChannel(String channelName, Class<T> messageType) {
        DefaultMessageChannel<T> channel = new DefaultMessageChannel<>(channelName, messageType, transport, serializer, serverId);
        channels.put(channelName, channel);
        return channel;
    }

    @Override
    public CompletableFuture<Void> sendToServer(String targetServerId, Message message) {
        try {
            byte[] data = serializer.serialize(message);
            return transport.publish(directChannel(targetServerId), data);
        } catch (MessageSerializer.MessageSerializationException e) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    @Override
    public CompletableFuture<Void> broadcast(Message message) {
        try {
            byte[] data = serializer.serialize(message);
            return transport.publish("conexus:broadcast", data);
        } catch (MessageSerializer.MessageSerializationException e) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
    }

    @Override
    public <T extends Message> CompletableFuture<T> sendRequest(String targetServerId, Message request, Class<T> responseType, long timeoutMs) {
        UUID correlationId = request.getMessageId();
        CompletableFuture<Message> waiter = new CompletableFuture<>();
        pendingRequests.put(correlationId, waiter);

        // Schedule timeout
        ScheduledFuture<?> timeout = scheduler.schedule(() -> {
            CompletableFuture<Message> pending = pendingRequests.remove(correlationId);
            if (pending != null && !pending.isDone()) {
                pending.completeExceptionally(new TimeoutException("Request " + correlationId + " timed out after " + timeoutMs + "ms"));
            }
        }, timeoutMs, TimeUnit.MILLISECONDS);

        // Wrap to complete with typed response
        CompletableFuture<T> typed = waiter.thenApply(response -> {
            if (!responseType.isInstance(response)) {
                throw new CompletionException(new ClassCastException("Expected " + responseType.getSimpleName() + " but got " + response.getClass().getSimpleName()));
            }
            return responseType.cast(response);
        });

        // Send the request to the target server
        return sendToServer(targetServerId, request).thenCompose(v -> typed.whenComplete((r, t) -> timeout.cancel(true)));
    }

    @Override
    public <T extends Message> void registerHandler(Class<T> messageType, Consumer<MessageContext<T>> handler) {
        messageHandlers.put(messageType, (Consumer) handler);
    }

    @Override
    public <T extends Message> void unregisterHandler(Class<T> messageType) {
        messageHandlers.remove(messageType);
    }

    @Override
    public String getServerId() {
        return serverId;
    }

    private void handleBroadcastMessage(byte[] bytes) {
        handleIncoming(bytes);
    }

    private void handleDirectMessage(byte[] bytes) {
        handleIncoming(bytes);
    }

    private void handleIncoming(byte[] bytes) {
        try {
            Message msg = serializer.deserialize(bytes);
            if (serverId.equals(msg.getSourceServerId())) {
                return; // ignore loopback
            }

            // Pending request? deliver and return
            CompletableFuture<Message> waiter = pendingRequests.remove(msg.getMessageId());
            if (waiter != null) {
                waiter.complete(msg);
                return;
            }

            // Find a registered handler by exact class match or assignable
            Consumer<MessageContext<? extends Message>> handler = findHandlerFor(msg.getClass());
            if (handler != null) {
                MessageContext<Message> ctx = new SimpleMessageContext<>(msg);
                @SuppressWarnings("unchecked")
                Consumer<MessageContext<Message>> cast = (Consumer<MessageContext<Message>>) (Consumer) handler;
                cast.accept(ctx);
            } else {
                logger.debug("No handler registered for message type {}", msg.getClass().getSimpleName());
            }
        } catch (Exception e) {
            logger.error("Failed to handle incoming message bytes", e);
        }
    }

    private Consumer<MessageContext<? extends Message>> findHandlerFor(Class<?> cls) {
        // Exact match
        @SuppressWarnings("unchecked")
        Consumer<MessageContext<? extends Message>> h = (Consumer) messageHandlers.get(cls);
        if (h != null) return h;
        // Assignable match (first found)
        for (Class<? extends Message> key : messageHandlers.keySet()) {
            if (key.isAssignableFrom(cls)) {
                return messageHandlers.get(key);
            }
        }
        return null;
    }

    private static class SimpleMessageContext<T extends Message> implements MessageContext<T> {
        private final T message;
        SimpleMessageContext(T message) { this.message = message; }
        @Override public T getMessage() { return message; }
        @Override public String getChannelName() { return "conexus"; }
        @Override public boolean expectsResponse() { return false; }
        @Override public CompletableFuture<Void> sendResponse(Message response) { return CompletableFuture.failedFuture(new IllegalStateException("No response supported")); }
        @Override public CompletableFuture<Void> acknowledge() { return CompletableFuture.completedFuture(null); }
    }

    private String directChannel(String sid) {
        return "conexus:direct:" + sid;
    }
}
