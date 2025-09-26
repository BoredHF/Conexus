package com.boredhf.conexus.events.types;

import com.boredhf.conexus.events.EventService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Event fired when a server's status changes (startup, shutdown, maintenance, etc.).
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class ServerStatusEvent implements EventService.NetworkEvent {
    
    @JsonProperty("sourceServerId")
    private final String sourceServerId;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("status")
    private final ServerStatus status;
    
    @JsonProperty("reason")
    private final String reason;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new server status event.
     * 
     * @param sourceServerId the server ID
     * @param status the new status
     * @param reason optional reason for the status change
     */
    public ServerStatusEvent(String sourceServerId, ServerStatus status, String reason) {
        this.sourceServerId = sourceServerId;
        this.timestamp = Instant.now();
        this.status = status;
        this.reason = reason != null ? reason : "";
        this.metadata = new HashMap<>();
        
        // Add some useful metadata
        metadata.put("statusChange", status.name());
        metadata.put("timestamp", timestamp.toString());
    }
    
    /**
     * Constructor for deserialization.
     */
    public ServerStatusEvent(
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("status") ServerStatus status,
            @JsonProperty("reason") String reason,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.sourceServerId = sourceServerId;
        this.timestamp = timestamp;
        this.status = status;
        this.reason = reason;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    @Override
    public String getSourceServerId() {
        return sourceServerId;
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Gets the server status.
     * 
     * @return the status
     */
    public ServerStatus getStatus() {
        return status;
    }
    
    /**
     * Gets the reason for the status change.
     * 
     * @return the reason, or empty string if none provided
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Checks if this is a server startup event.
     * 
     * @return true if the server is starting up
     */
    public boolean isStartup() {
        return status == ServerStatus.STARTING || status == ServerStatus.ONLINE;
    }
    
    /**
     * Checks if this is a server shutdown event.
     * 
     * @return true if the server is shutting down or offline
     */
    public boolean isShutdown() {
        return status == ServerStatus.SHUTTING_DOWN || status == ServerStatus.OFFLINE;
    }
    
    /**
     * Checks if the server is available for players.
     * 
     * @return true if the server is online and accepting players
     */
    public boolean isAvailable() {
        return status == ServerStatus.ONLINE;
    }
    
    @Override
    public String toString() {
        return "ServerStatusEvent{" +
                "sourceServerId='" + sourceServerId + '\'' +
                ", status=" + status +
                ", reason='" + reason + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
    
    /**
     * Server status enumeration.
     */
    public enum ServerStatus {
        /**
         * Server is starting up.
         */
        STARTING,
        
        /**
         * Server is online and accepting players.
         */
        ONLINE,
        
        /**
         * Server is under maintenance.
         */
        MAINTENANCE,
        
        /**
         * Server is shutting down.
         */
        SHUTTING_DOWN,
        
        /**
         * Server is offline.
         */
        OFFLINE,
        
        /**
         * Server is in an error state.
         */
        ERROR
    }
}