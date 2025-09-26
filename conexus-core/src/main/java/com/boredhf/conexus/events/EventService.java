package com.boredhf.conexus.events;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Service for broadcasting custom events across the network.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface EventService {
    
    /**
     * Broadcasts an event to all servers in the network.
     */
    <T extends NetworkEvent> CompletableFuture<Void> broadcastEvent(T event);
    
    /**
     * Broadcasts an event with specified priority.
     */
    <T extends NetworkEvent> CompletableFuture<Void> broadcastEvent(T event, EventPriority priority);
    
    /**
     * Registers an event listener.
     */
    <T extends NetworkEvent> void registerEventListener(Class<T> eventType, EventListener<T> listener);
    
    /**
     * Unregisters an event listener.
     */
    <T extends NetworkEvent> void unregisterEventListener(Class<T> eventType, EventListener<T> listener);
    
    /**
     * Base interface for network events.
     */
    interface NetworkEvent {
        String getSourceServerId();
        Instant getTimestamp();
        Map<String, Object> getMetadata();
    }
    
    /**
     * Event priority levels.
     */
    enum EventPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        EventPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Event listener interface.
     */
    @FunctionalInterface
    interface EventListener<T extends NetworkEvent> {
        void onEvent(T event);
    }
    
    /**
     * Event filter interface.
     */
    @FunctionalInterface
    interface EventFilter<T extends NetworkEvent> extends Predicate<T> {
        @Override
        boolean test(T event);
    }
}
