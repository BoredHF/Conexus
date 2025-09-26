package com.boredhf.conexus.communication;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory messaging service implementation for testing.
 * 
 * This implementation allows multiple messaging services to communicate
 * with each other in memory without requiring external transport.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class InMemoryMessagingService implements MessagingService {
    
    private final String serverId;
    private final List<InMemoryMessagingService> peers = new ArrayList<>();
    private final Map<Class<? extends Message>, Consumer<MessageContext<? extends Message>>> handlers = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    public InMemoryMessagingService(String serverId) {
        this.serverId = serverId;
    }
    
    /**
     * Adds a peer messaging service for cross-communication.
     */
    public void addPeer(InMemoryMessagingService peer) {
        synchronized (peers) {
            peers.add(peer);
        }
    }
    
    /**
     * Removes a peer messaging service.
     */
    public void removePeer(InMemoryMessagingService peer) {
        synchronized (peers) {
            peers.remove(peer);
        }
    }
    
    public CompletableFuture<Void> initialize() {
        initialized.set(true);
        return CompletableFuture.completedFuture(null);
    }
    
    public CompletableFuture<Void> shutdown() {
        initialized.set(false);
        synchronized (peers) {
            peers.clear();
        }
        handlers.clear();
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public MessageChannel getChannel(String channelName) {
        throw new UnsupportedOperationException("Channels not implemented in test messaging service");
    }
    
    @Override
    public <T extends Message> MessageChannel<T> createChannel(String channelName, Class<T> messageType) {
        throw new UnsupportedOperationException("Channels not implemented in test messaging service");
    }
    
    @Override
    public CompletableFuture<Void> sendToServer(String targetServerId, Message message) {
        return CompletableFuture.runAsync(() -> {
            synchronized (peers) {
                for (InMemoryMessagingService peer : peers) {
                    if (peer.getServerId().equals(targetServerId)) {
                        peer.handleMessage(message);
                        return;
                    }
                }
            }
            throw new RuntimeException("No peer found with server ID: " + targetServerId);
        });
    }
    
    @Override
    public CompletableFuture<Void> broadcast(Message message) {
        return CompletableFuture.runAsync(() -> {
            synchronized (peers) {
                for (InMemoryMessagingService peer : peers) {
                    // Don't broadcast to ourselves
                    if (!peer.getServerId().equals(this.serverId)) {
                        peer.handleMessage(message);
                    }
                }
            }
        });
    }
    
    @Override
    public <T extends Message> CompletableFuture<T> sendRequest(String targetServerId, Message request, 
                                                                Class<T> responseType, long timeoutMs) {
        throw new UnsupportedOperationException("Request/response not implemented in test messaging service");
    }
    
    @Override
    public <T extends Message> void registerHandler(Class<T> messageType, Consumer<MessageContext<T>> handler) {
        handlers.put(messageType, (Consumer) handler);
    }
    
    @Override
    public <T extends Message> void unregisterHandler(Class<T> messageType) {
        handlers.remove(messageType);
    }
    
    @Override
    public String getServerId() {
        return serverId;
    }
    
    /**
     * Handles incoming messages by dispatching them to registered handlers.
     */
    private void handleMessage(Message message) {
        // Find handler for this message type
        Consumer<MessageContext<? extends Message>> handler = findHandlerFor(message.getClass());
        if (handler != null) {
            MessageContext<Message> context = new SimpleMessageContext<>(message);
            try {
                @SuppressWarnings("unchecked")
                Consumer<MessageContext<Message>> typedHandler = (Consumer<MessageContext<Message>>) (Consumer) handler;
                typedHandler.accept(context);
            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("DEBUG: No handler found for message type: " + message.getClass().getSimpleName());
        }
    }
    
    /**
     * Finds a handler for the given message class.
     */
    private Consumer<MessageContext<? extends Message>> findHandlerFor(Class<?> messageClass) {
        // Try exact match first
        @SuppressWarnings("unchecked")
        Consumer<MessageContext<? extends Message>> handler = 
            (Consumer<MessageContext<? extends Message>>) handlers.get(messageClass);
        if (handler != null) {
            return handler;
        }
        
        // Try assignable matches
        for (Map.Entry<Class<? extends Message>, Consumer<MessageContext<? extends Message>>> entry : handlers.entrySet()) {
            if (entry.getKey().isAssignableFrom(messageClass)) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Simple implementation of MessageContext for testing.
     */
    private static class SimpleMessageContext<T extends Message> implements MessageContext<T> {
        private final T message;
        
        public SimpleMessageContext(T message) {
            this.message = message;
        }
        
        @Override
        public T getMessage() {
            return message;
        }
        
        @Override
        public String getChannelName() {
            return "test-channel";
        }
        
        @Override
        public boolean expectsResponse() {
            return false;
        }
        
        @Override
        public CompletableFuture<Void> sendResponse(Message response) {
            throw new UnsupportedOperationException("Responses not supported in test context");
        }
        
        @Override
        public CompletableFuture<Void> acknowledge() {
            return CompletableFuture.completedFuture(null);
        }
    }
}