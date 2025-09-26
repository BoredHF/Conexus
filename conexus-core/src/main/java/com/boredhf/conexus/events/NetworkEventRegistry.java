package com.boredhf.conexus.events;

import com.boredhf.conexus.communication.MessageSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Registry for managing NetworkEvent types and their serialization/deserialization.
 * 
 * This registry allows the cross-server event system to support multiple event types
 * by providing proper JSON serialization and deserialization capabilities.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class NetworkEventRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkEventRegistry.class);
    
    private final MessageSerializer messageSerializer;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends EventService.NetworkEvent>> eventTypes = new ConcurrentHashMap<>();
    private final Map<String, Function<String, EventService.NetworkEvent>> deserializers = new ConcurrentHashMap<>();
    
    public NetworkEventRegistry(MessageSerializer messageSerializer) {
        this.messageSerializer = messageSerializer;
        
        // Initialize Jackson ObjectMapper with proper configuration
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // Configure to handle missing properties gracefully
        this.objectMapper.configure(
            com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, 
            false
        );
        
        // Register built-in event types
        registerBuiltInEventTypes();
    }
    
    /**
     * Registers built-in event types.
     */
    private void registerBuiltInEventTypes() {
        registerEventType(ServerStatusEvent.class, ServerStatusEvent::fromString);
        logger.info("Registered built-in event types: ServerStatusEvent");
    }
    
    /**
     * Registers a new event type with its deserializer.
     * 
     * @param eventType the event class
     * @param deserializer function to deserialize JSON string to event instance
     * @param <T> the event type
     */
    public <T extends EventService.NetworkEvent> void registerEventType(
            Class<T> eventType, 
            Function<String, T> deserializer) {
        
        String typeName = eventType.getName();
        eventTypes.put(typeName, eventType);
        deserializers.put(typeName, (Function<String, EventService.NetworkEvent>) deserializer);
        
        logger.debug("Registered event type: {}", typeName);
    }
    
    /**
     * Serializes a NetworkEvent to JSON string.
     * 
     * @param event the event to serialize
     * @return JSON representation of the event
     * @throws EventSerializationException if serialization fails
     */
    public String serializeEvent(EventService.NetworkEvent event) throws EventSerializationException {
        try {
            // First try Jackson serialization for proper JSON
            return objectMapper.writeValueAsString(event);
        } catch (Exception jacksonException) {
            // Fallback to toString for events that don't work with Jackson
            logger.debug("Jackson serialization failed for {}, falling back to toString: {}", 
                       event.getClass().getSimpleName(), jacksonException.getMessage());
            
            try {
                String result = event.toString();
                if (result == null || result.trim().isEmpty()) {
                    throw new EventSerializationException(
                        "Both Jackson and toString serialization failed for type " + event.getClass().getName(), 
                        jacksonException);
                }
                return result;
            } catch (Exception toStringException) {
                EventSerializationException combined = new EventSerializationException(
                    "All serialization methods failed for type " + event.getClass().getName());
                combined.addSuppressed(jacksonException);
                combined.addSuppressed(toStringException);
                throw combined;
            }
        }
    }
    
    /**
     * Deserializes a JSON string back to a NetworkEvent.
     * 
     * @param eventTypeName the fully qualified class name of the event type
     * @param jsonData the JSON representation of the event
     * @return the deserialized event
     * @throws EventDeserializationException if deserialization fails
     */
    public EventService.NetworkEvent deserializeEvent(String eventTypeName, String jsonData) 
            throws EventDeserializationException {
        
        // Get the event class
        Class<? extends EventService.NetworkEvent> eventClass = eventTypes.get(eventTypeName);
        if (eventClass == null) {
            throw new EventDeserializationException("Unknown event type: " + eventTypeName);
        }
        
        // First try Jackson deserialization if the data looks like JSON
        if (jsonData.trim().startsWith("{") && jsonData.trim().endsWith("}")) {
            try {
                return objectMapper.readValue(jsonData, eventClass);
            } catch (Exception jacksonException) {
                logger.debug("Jackson deserialization failed for {}, trying custom deserializer: {}", 
                           eventTypeName, jacksonException.getMessage());
                
                // Fall back to custom deserializer
                Function<String, EventService.NetworkEvent> deserializer = deserializers.get(eventTypeName);
                if (deserializer != null) {
                    try {
                        return deserializer.apply(jsonData);
                    } catch (Exception deserializerException) {
                        EventDeserializationException combined = new EventDeserializationException(
                            "Both Jackson and custom deserializer failed for type " + eventTypeName);
                        combined.addSuppressed(jacksonException);
                        combined.addSuppressed(deserializerException);
                        throw combined;
                    }
                } else {
                    throw new EventDeserializationException(
                        "Jackson deserialization failed and no custom deserializer registered for: " + eventTypeName, 
                        jacksonException);
                }
            }
        } else {
            // Data doesn't look like JSON, use custom deserializer
            Function<String, EventService.NetworkEvent> deserializer = deserializers.get(eventTypeName);
            if (deserializer == null) {
                throw new EventDeserializationException(
                    "Data is not JSON format and no custom deserializer registered for event type: " + eventTypeName);
            }
            
            try {
                return deserializer.apply(jsonData);
            } catch (Exception e) {
                throw new EventDeserializationException(
                    "Failed to deserialize event of type " + eventTypeName + ": " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Gets the event class for a given type name.
     * 
     * @param eventTypeName the fully qualified class name
     * @return the event class, or null if not registered
     */
    public Class<? extends EventService.NetworkEvent> getEventClass(String eventTypeName) {
        return eventTypes.get(eventTypeName);
    }
    
    /**
     * Checks if an event type is registered.
     * 
     * @param eventTypeName the fully qualified class name
     * @return true if registered, false otherwise
     */
    public boolean isEventTypeRegistered(String eventTypeName) {
        return eventTypes.containsKey(eventTypeName);
    }
    
    /**
     * Gets all registered event type names.
     * 
     * @return set of registered event type names
     */
    public java.util.Set<String> getRegisteredEventTypes() {
        return eventTypes.keySet();
    }
    
    /**
     * Exception thrown when event serialization fails.
     */
    public static class EventSerializationException extends Exception {
        public EventSerializationException(String message) {
            super(message);
        }
        
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when event deserialization fails.
     */
    public static class EventDeserializationException extends Exception {
        public EventDeserializationException(String message) {
            super(message);
        }
        
        public EventDeserializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}