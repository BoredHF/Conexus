package com.boredhf.conexus.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of EventService.
 * This is a basic implementation that will be expanded in future versions.
 * 
 * @since 1.0.0
 */
public class DefaultEventService implements EventService {
    
    private final String serverId;
    private final Map<Class<?>, List<EventListener<? extends NetworkEvent>>> listeners = new ConcurrentHashMap<>();
    
    public DefaultEventService(String serverId) {
        this.serverId = serverId;
    }
    
    @Override
    public <T extends NetworkEvent> CompletableFuture<Void> broadcastEvent(T event) {
        return broadcastEvent(event, EventPriority.NORMAL);
    }
    
    @Override
    public <T extends NetworkEvent> CompletableFuture<Void> broadcastEvent(T event, EventPriority priority) {
        // TODO: Implement cross-server broadcasting
        // For now, just notify local listeners
        notifyListeners(event);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends NetworkEvent> void registerEventListener(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventListener<? extends NetworkEvent>) listener);
    }
    
    @Override
    public <T extends NetworkEvent> void unregisterEventListener(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<? extends NetworkEvent>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends NetworkEvent> void notifyListeners(T event) {
        List<EventListener<? extends NetworkEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener<? extends NetworkEvent> listener : eventListeners) {
                try {
                    ((EventListener<NetworkEvent>) listener).onEvent(event);
                } catch (Exception e) {
                    // Log error but continue with other listeners
                    System.err.println("Error handling event: " + e.getMessage());
                }
            }
        }
    }
}