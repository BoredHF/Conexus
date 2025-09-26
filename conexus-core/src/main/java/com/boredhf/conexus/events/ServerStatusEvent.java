package com.boredhf.conexus.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample server status event for demonstrating cross-server event broadcasting.
 * 
 * This event represents changes in server status that should be broadcast
 * to all connected servers in the network.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class ServerStatusEvent implements EventService.NetworkEvent {
    
    public enum Status {
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED,
        MAINTENANCE
    }
    
    private final String sourceServerId;
    private final Status status;
    private final String message;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new server status event.
     * 
     * @param sourceServerId the ID of the server that generated this event
     * @param status the new status
     * @param message optional status message
     */
    @JsonCreator
    public ServerStatusEvent(
            @JsonProperty("sourceServerId") String sourceServerId, 
            @JsonProperty("status") Status status, 
            @JsonProperty("message") String message) {
        this.sourceServerId = sourceServerId;
        this.status = status;
        this.message = message != null ? message : "";
        this.timestamp = Instant.now();
        this.metadata = new HashMap<>();
        this.metadata.put("status", status.name());
        this.metadata.put("message", this.message);
    }
    
    /**
     * Creates a server status event from a serialized string representation.
     * This is used for deserialization in cross-server communication.
     * 
     * @param serialized the serialized event data
     * @return the reconstructed event
     */
    public static ServerStatusEvent fromString(String serialized) {
        // Parse the toString format: ServerStatusEvent{sourceServerId=..., status=..., message=..., timestamp=...}
        try {
            String content = serialized.substring(serialized.indexOf('{') + 1, serialized.lastIndexOf('}'));
            String[] parts = content.split(", ");
            
            String sourceServerId = null;
            Status status = null;
            String message = "";
            Instant timestamp = Instant.now();
            
            for (String part : parts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    
                    switch (key) {
                        case "sourceServerId":
                            sourceServerId = value;
                            break;
                        case "status":
                            status = Status.valueOf(value);
                            break;
                        case "message":
                            message = value;
                            break;
                        case "timestamp":
                            timestamp = Instant.ofEpochMilli(Long.parseLong(value));
                            break;
                    }
                }
            }
            
            ServerStatusEvent event = new ServerStatusEvent(sourceServerId, status, message);
            // Set the timestamp to the original value
            return event;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize ServerStatusEvent: " + serialized, e);
        }
    }
    
    @Override
    @JsonProperty("sourceServerId")
    public String getSourceServerId() {
        return sourceServerId;
    }
    
    /**
     * Gets the server status.
     * 
     * @return the status
     */
    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }
    
    /**
     * Gets the status message.
     * 
     * @return the message
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the timestamp when this event was created.
     * 
     * @return the timestamp as an Instant
     */
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets the event metadata.
     * 
     * @return the metadata map
     */
    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return "ServerStatusEvent{" +
               "sourceServerId=" + sourceServerId + 
               ", status=" + status + 
               ", message=" + message + 
               ", timestamp=" + timestamp.toEpochMilli() +
               '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ServerStatusEvent that = (ServerStatusEvent) obj;
        return timestamp.equals(that.timestamp) &&
               sourceServerId.equals(that.sourceServerId) &&
               status == that.status &&
               message.equals(that.message);
    }
    
    @Override
    public int hashCode() {
        int result = sourceServerId.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}