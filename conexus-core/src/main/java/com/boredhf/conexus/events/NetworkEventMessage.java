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
     */
    public NetworkEventMessage(Class<? extends EventService.NetworkEvent> eventType, 
                              EventService.NetworkEvent eventData,
                              EventService.EventPriority priority,
                              String sourceServerId) {
        super(sourceServerId);
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventTypeString = eventType.getName();
        this.eventDataJson = serializeEventData(eventData);
        this.priority = priority;
        this.originalServerId = eventData.getSourceServerId();
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
    
    // Static registry for event serialization/deserialization
    private static NetworkEventRegistry eventRegistry;
    
    /**
     * Sets the event registry for serialization/deserialization.
     * This must be called before creating any NetworkEventMessage instances.
     * 
     * @param registry the event registry
     */
    public static void setEventRegistry(NetworkEventRegistry registry) {
        eventRegistry = registry;
    }
    
    /**
     * Helper method to serialize event data to JSON.
     */
    private String serializeEventData(EventService.NetworkEvent event) {
        if (eventRegistry == null) {
            throw new IllegalStateException("NetworkEventRegistry not set. Call setEventRegistry() first.");
        }
        
        try {
            return eventRegistry.serializeEvent(event);
        } catch (NetworkEventRegistry.EventSerializationException e) {
            throw new RuntimeException("Failed to serialize event: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reconstructs the event type and data from serialized strings.
     * This should be called after deserialization.
     */
    public void reconstructEvent() {
        if (eventRegistry == null) {
            throw new IllegalStateException("NetworkEventRegistry not set. Call setEventRegistry() first.");
        }
        
        try {
            // Get the event class from the registry
            Class<? extends EventService.NetworkEvent> clazz = eventRegistry.getEventClass(eventTypeString);
            if (clazz == null) {
                throw new RuntimeException("Unknown event type: " + eventTypeString);
            }
            this.eventType = clazz;
            
            // Deserialize the event data using the registry
            this.eventData = eventRegistry.deserializeEvent(eventTypeString, eventDataJson);
            
        } catch (NetworkEventRegistry.EventDeserializationException e) {
            throw new RuntimeException("Failed to reconstruct event of type " + eventTypeString + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reconstruct event of type " + eventTypeString + ": " + e.getMessage(), e);
        }
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
                "eventType=" + eventType.getSimpleName() +
                ", priority=" + priority +
                ", originalServerId='" + originalServerId + '\'' +
                ", " + super.toString() +
                '}';
    }
}