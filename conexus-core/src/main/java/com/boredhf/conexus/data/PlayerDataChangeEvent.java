package com.boredhf.conexus.data;

import com.boredhf.conexus.communication.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Event that represents a change to player data across servers.
 * 
 * @param <T> the type of player data that changed
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class PlayerDataChangeEvent<T extends PlayerData> extends BaseMessage {
    
    @JsonProperty("playerId")
    private final UUID playerId;
    
    @JsonProperty("dataType")
    private final Class<T> dataType;
    
    @JsonProperty("data")
    private final T data;
    
    @JsonProperty("changeType")
    private final ChangeType changeType;
    
    @JsonProperty("version")
    private final long version;
    
    /**
     * Creates a new player data change event.
     * 
     * @param playerId the UUID of the player whose data changed
     * @param dataType the class type of the data that changed
     * @param data the new data (null for DELETE operations)
     * @param changeType the type of change that occurred
     * @param sourceServerId the server that initiated the change
     * @param version the version number of this change
     */
    public PlayerDataChangeEvent(UUID playerId, Class<T> dataType, T data, 
                               ChangeType changeType, String sourceServerId, long version) {
        super(sourceServerId);
        this.playerId = playerId;
        this.dataType = dataType;
        this.data = data;
        this.changeType = changeType;
        this.version = version;
    }
    
    /**
     * Constructor for deserialization.
     */
    public PlayerDataChangeEvent(
            @JsonProperty("messageId") UUID messageId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("dataType") Class<T> dataType,
            @JsonProperty("data") T data,
            @JsonProperty("changeType") ChangeType changeType,
            @JsonProperty("version") long version) {
        super(messageId, timestamp, sourceServerId);
        this.playerId = playerId;
        this.dataType = dataType;
        this.data = data;
        this.changeType = changeType;
        this.version = version;
    }
    
    /**
     * Gets the UUID of the player whose data changed.
     * 
     * @return the player UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets the class type of the data that changed.
     * 
     * @return the data type class
     */
    public Class<T> getDataType() {
        return dataType;
    }
    
    /**
     * Gets the new data value (null for DELETE operations).
     * 
     * @return the data, or null if deleted
     */
    public T getData() {
        return data;
    }
    
    /**
     * Gets the type of change that occurred.
     * 
     * @return the change type
     */
    public ChangeType getChangeType() {
        return changeType;
    }
    
    /**
     * Gets the version number of this change.
     * 
     * @return the version number
     */
    public long getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return "PlayerDataChangeEvent{" +
                "playerId=" + playerId +
                ", dataType=" + dataType.getSimpleName() +
                ", changeType=" + changeType +
                ", version=" + version +
                ", " + super.toString() +
                '}';
    }
    
    /**
     * Change types for player data events.
     */
    public enum ChangeType {
        SET, UPDATE, DELETE
    }
}
