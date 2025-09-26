package com.boredhf.conexus.events.types;

import com.boredhf.conexus.events.EventService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a player performs significant network actions (join, leave, server switch, etc.).
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class PlayerNetworkEvent implements EventService.NetworkEvent {
    
    @JsonProperty("sourceServerId")
    private final String sourceServerId;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("playerId")
    private final UUID playerId;
    
    @JsonProperty("playerName")
    private final String playerName;
    
    @JsonProperty("eventType")
    private final PlayerEventType eventType;
    
    @JsonProperty("targetServerId")
    private final String targetServerId;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new player network event.
     * 
     * @param sourceServerId the server where the event occurred
     * @param playerId the player's UUID
     * @param playerName the player's name
     * @param eventType the type of event
     */
    public PlayerNetworkEvent(String sourceServerId, UUID playerId, String playerName, PlayerEventType eventType) {
        this(sourceServerId, playerId, playerName, eventType, null);
    }
    
    /**
     * Creates a new player network event with a target server.
     * 
     * @param sourceServerId the server where the event occurred
     * @param playerId the player's UUID
     * @param playerName the player's name
     * @param eventType the type of event
     * @param targetServerId the target server (for transfers/switches)
     */
    public PlayerNetworkEvent(String sourceServerId, UUID playerId, String playerName, 
                             PlayerEventType eventType, String targetServerId) {
        this.sourceServerId = sourceServerId;
        this.timestamp = Instant.now();
        this.playerId = playerId;
        this.playerName = playerName;
        this.eventType = eventType;
        this.targetServerId = targetServerId;
        this.metadata = new HashMap<>();
        
        // Add useful metadata
        metadata.put("playerId", playerId.toString());
        metadata.put("playerName", playerName);
        metadata.put("eventType", eventType.name());
        metadata.put("timestamp", timestamp.toString());
        if (targetServerId != null) {
            metadata.put("targetServerId", targetServerId);
        }
    }
    
    /**
     * Constructor for deserialization.
     */
    public PlayerNetworkEvent(
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("playerName") String playerName,
            @JsonProperty("eventType") PlayerEventType eventType,
            @JsonProperty("targetServerId") String targetServerId,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.sourceServerId = sourceServerId;
        this.timestamp = timestamp;
        this.playerId = playerId;
        this.playerName = playerName;
        this.eventType = eventType;
        this.targetServerId = targetServerId;
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
     * Gets the player's UUID.
     * 
     * @return the player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the player's name.
     * 
     * @return the player name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Gets the event type.
     * 
     * @return the event type
     */
    public PlayerEventType getEventType() {
        return eventType;
    }
    
    /**
     * Gets the target server ID for transfer events.
     * 
     * @return the target server ID, or null if not applicable
     */
    public String getTargetServerId() {
        return targetServerId;
    }
    
    /**
     * Checks if this is a player join event.
     * 
     * @return true if the player is joining the network
     */
    public boolean isJoin() {
        return eventType == PlayerEventType.JOIN_NETWORK;
    }
    
    /**
     * Checks if this is a player leave event.
     * 
     * @return true if the player is leaving the network
     */
    public boolean isLeave() {
        return eventType == PlayerEventType.LEAVE_NETWORK;
    }
    
    /**
     * Checks if this is a server switch event.
     * 
     * @return true if the player is switching servers
     */
    public boolean isServerSwitch() {
        return eventType == PlayerEventType.SERVER_SWITCH;
    }
    
    /**
     * Adds custom metadata to the event.
     * 
     * @param key the metadata key
     * @param value the metadata value
     * @return this event for method chaining
     */
    public PlayerNetworkEvent withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }
    
    @Override
    public String toString() {
        return "PlayerNetworkEvent{" +
                "sourceServerId='" + sourceServerId + '\'' +
                ", playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", eventType=" + eventType +
                ", targetServerId='" + targetServerId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
    
    /**
     * Player event types.
     */
    public enum PlayerEventType {
        /**
         * Player joined the network for the first time or after being offline.
         */
        JOIN_NETWORK,
        
        /**
         * Player left the network (disconnected).
         */
        LEAVE_NETWORK,
        
        /**
         * Player switched from one server to another within the network.
         */
        SERVER_SWITCH,
        
        /**
         * Player was kicked from a server.
         */
        KICKED,
        
        /**
         * Player was banned from the network.
         */
        BANNED,
        
        /**
         * Player was unbanned from the network.
         */
        UNBANNED,
        
        /**
         * Player achieved a significant milestone or achievement.
         */
        ACHIEVEMENT,
        
        /**
         * Player performed an important action worth broadcasting.
         */
        IMPORTANT_ACTION
    }
}