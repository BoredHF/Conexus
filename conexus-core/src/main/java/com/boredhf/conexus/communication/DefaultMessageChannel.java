package com.boredhf.conexus.communication;

import com.boredhf.conexus.transport.TransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Default implementation of MessageChannel.
 * 
 * Provides typed pub/sub messaging over the transport layer with
 * automatic serialization/deserialization.
 * 
 * @param <T> the type of messages this channel handles
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class DefaultMessageChannel<T extends Message> implements MessageChannel<T> {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageChannel.class);
    
    private final String name;
    private final Class<T> messageType;
    private final TransportProvider transportProvider;
    private final MessageSerializer serializer;
    private final String serverId;
    
    private final AtomicReference<Consumer<MessageContext<T>>> handler = new AtomicReference<>();
    private volatile boolean subscribed = false;
    
    public DefaultMessageChannel(String name, Class<T> messageType, TransportProvider transportProvider, 
                                MessageSerializer serializer, String serverId) {
        this.name = name;
        this.messageType = messageType;
        this.transportProvider = transportProvider;
        this.serializer = serializer;
        this.serverId = serverId;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Class<T> getMessageType() {
        return messageType;
    }
    
    @Override
    public CompletableFuture<Void> publish(T message) {
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] serializedMessage = serializer.serialize(message);
                transportProvider.publish(name, serializedMessage).join();
                
                logger.debug("Published message {} to channel {}", message.getMessageId(), name);
                
            } catch (Exception e) {
                logger.error("Failed to publish message to channel {}", name, e);
                throw new RuntimeException("Failed to publish message", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> subscribe(Consumer<MessageContext<T>> messageHandler) {
        return CompletableFuture.runAsync(() -> {
            try {
                this.handler.set(messageHandler);
                
                transportProvider.subscribe(name, this::handleRawMessage).join();
                subscribed = true;
                
                logger.info("Subscribed to channel {} for message type {}", name, messageType.getSimpleName());
                
            } catch (Exception e) {
                subscribed = false;
                logger.error("Failed to subscribe to channel {}", name, e);
                throw new RuntimeException("Failed to subscribe to channel", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe() {
        return CompletableFuture.runAsync(() -> {
            try {
                transportProvider.unsubscribe(name).join();
                subscribed = false;
                handler.set(null);
                
                logger.info("Unsubscribed from channel {}", name);
                
            } catch (Exception e) {
                logger.error("Failed to unsubscribe from channel {}", name, e);
                throw new RuntimeException("Failed to unsubscribe from channel", e);
            }
        });
    }
    
    @Override
    public boolean isSubscribed() {
        return subscribed && transportProvider.isConnected();
    }
    
    private void handleRawMessage(byte[] rawMessage) {
        try {
            T message = serializer.deserialize(rawMessage, messageType);
            
            // Don't handle messages from ourselves
            if (serverId.equals(message.getSourceServerId())) {
                return;
            }
            
            Consumer<MessageContext<T>> currentHandler = handler.get();
            if (currentHandler != null) {
                MessageContext<T> context = new DefaultMessageContext<>(message, name);
                currentHandler.accept(context);
            }
            
            logger.debug("Handled message {} on channel {}", message.getMessageId(), name);
            
        } catch (Exception e) {
            logger.error("Failed to handle message on channel {}", name, e);
        }
    }
    
    /**
     * Default implementation of MessageContext.
     */
    private static class DefaultMessageContext<T extends Message> implements MessageContext<T> {
        
        private final T message;
        private final String channelName;
        
        public DefaultMessageContext(T message, String channelName) {
            this.message = message;
            this.channelName = channelName;
        }
        
        @Override
        public T getMessage() {
            return message;
        }
        
        @Override
        public String getChannelName() {
            return channelName;
        }
        
        @Override
        public boolean expectsResponse() {
            // Basic channels don't support responses - that's for request/response messaging
            return false;
        }
        
        @Override
        public CompletableFuture<Void> sendResponse(Message response) {
            throw new IllegalStateException("This message context does not support responses");
        }
        
        @Override
        public CompletableFuture<Void> acknowledge() {
            // Simple acknowledgment - just return completed future
            return CompletableFuture.completedFuture(null);
        }
    }
}