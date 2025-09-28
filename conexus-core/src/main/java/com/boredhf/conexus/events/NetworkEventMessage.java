package com.boredhf.conexus.events;

import com.boredhf.conexus.communication.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Message wrapper for broadcasting network events across servers.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class NetworkEventMessage extends BaseMessage {
    
    @JsonProperty("eventTypeString")
    private final String eventTypeString;
    
    @JsonProperty("eventDataJson")
    private final String eventDataJson;
    
    @JsonProperty("priority")
    private final EventService.EventPriority priority;
    
    @JsonProperty("originalServerId")
    private final String originalServerId;
    
    // Transient fields for reconstruction
    private transient Class<? extends EventService.NetworkEvent> eventType;
    private transient EventService.NetworkEvent eventData;
    
    /**
     * Creates a new network event message.
     * 
     * @param eventType the type of event being broadcast
     * @param eventData the event data
     * @param priority the event priority
     * @param sourceServerId the server broadcasting this event
     * @param registry the event registry to use for serialization
     */
    public NetworkEventMessage(Class<? extends EventService.NetworkEvent> eventType, 
                              EventService.NetworkEvent eventData,
                              EventService.EventPriority priority,
                              String sourceServerId,
                              NetworkEventRegistry registry) {
        super(sourceServerId);
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventTypeString = eventType.getName();
        this.eventDataJson = serializeEventData(eventData, registry);
        this.priority = priority;
        this.originalServerId = eventData.getSourceServerId();
        this.eventRegistry = registry;
    }
    
    /**
     * Constructor for deserialization.
     */
    public NetworkEventMessage(
            @JsonProperty("messageId") UUID messageId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("eventTypeString") String eventTypeString,
            @JsonProperty("eventDataJson") String eventDataJson,
            @JsonProperty("priority") EventService.EventPriority priority,
            @JsonProperty("originalServerId") String originalServerId) {
        super(messageId, timestamp, sourceServerId);
        this.eventTypeString = eventTypeString;
        this.eventDataJson = eventDataJson;
        this.priority = priority;
        this.originalServerId = originalServerId;
        // transient fields will be set via reconstructEvent()
    }
    
    /**
     * Gets the type of event being broadcast.
     * 
     * @return the event type class
     */
    public Class<? extends EventService.NetworkEvent> getEventType() {
        return eventType;
    }
    
    /**
     * Gets the event data.
     * 
     * @return the event data
     */
    public EventService.NetworkEvent getEventData() {
        return eventData;
    }
    
    /**
     * Gets the priority of the event.
     * 
     * @return the event priority
     */
    public EventService.EventPriority getPriority() {
        return priority;
    }
    
    // Instance registry for event serialization/deserialization
    private transient NetworkEventRegistry eventRegistry;
    
    /**
     * Sets the event registry for serialization/deserialization.
     * This should be called after deserialization and before calling reconstructEvent().
     * 
     * @param registry the event registry
     */
    public void setEventRegistry(NetworkEventRegistry registry) {
        this.eventRegistry = registry;
    }
    
    /**
     * Helper method to serialize event data to JSON.
     */
    private String serializeEventData(EventService.NetworkEvent event, NetworkEventRegistry registry) {
        if (registry == null) {
            throw new IllegalStateException("NetworkEventRegistry cannot be null.");
        }
        
        try {
            return registry.serializeEvent(event);
        } catch (NetworkEventRegistry.EventSerializationException e) {
            throw new RuntimeException("Failed to serialize event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reconstructs the event type and data from serialized strings.
     * This should be called after deserialization.
     * 
     * @param registry the event registry to use for deserialization
     */
    public void reconstructEvent(NetworkEventRegistry registry) {
        if (registry == null) {
            throw new IllegalStateException("NetworkEventRegistry cannot be null.");
        }
        
        // Store registry for potential future use
        this.eventRegistry = registry;
        
        try {
            // Get the event class from the registry
            Class<? extends EventService.NetworkEvent> clazz = registry.getEventClass(eventTypeString);
            if (clazz == null) {
                throw new RuntimeException("Unknown event type: " + eventTypeString);
            }
            this.eventType = clazz;
            
            // Deserialize the event data using the registry
            this.eventData = registry.deserializeEvent(eventTypeString, eventDataJson);
            
        } catch (NetworkEventRegistry.EventDeserializationException e) {
            throw new RuntimeException("Failed to reconstruct event of type " + eventTypeString + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct event of type " + eventTypeString + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Reconstructs the event type and data from serialized strings.
     * This should be called after deserialization.
     * Uses the previously set registry instance.
     * 
     * @deprecated Use {@link #reconstructEvent(NetworkEventRegistry)} to pass registry explicitly
     */
    @Deprecated
    public void reconstructEvent() {
        if (eventRegistry == null) {
            throw new IllegalStateException("NetworkEventRegistry not set. Call setEventRegistry() first or use reconstructEvent(NetworkEventRegistry).");
        }
        reconstructEvent(eventRegistry);
    }
    
    /**
     * Gets the ID of the server that originally triggered the event.
     * This may be different from getSourceServerId() which is the server
     * that sent this network message.
     * 
     * @return the original server ID
     */
    public String getOriginalServerId() {
        return originalServerId;
    }
    
    @Override
    public String toString() {
        return "NetworkEventMessage{" +
                "eventType=" + (eventType != null ? eventType.getSimpleName() : eventTypeString) +
                ", priority=" + priority +
                ", originalServerId='" + originalServerId + '\'' +
                ", " + super.toString() +
                '}';
    }
}