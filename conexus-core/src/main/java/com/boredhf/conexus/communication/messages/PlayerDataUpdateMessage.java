package com.boredhf.conexus.communication.messages;

import com.boredhf.conexus.communication.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Message sent when player data is updated on a server.
 * This notifies other servers to invalidate their cache for the player.
 * 
 * @since 1.0.0
 */
public class PlayerDataUpdateMessage extends BaseMessage {
    
    @JsonProperty("playerId")
    private final UUID playerId;
    
    @JsonProperty("dataType")
    private final String dataType;
    
    @JsonProperty("serializedData")
    private final String serializedData;
    
    @JsonProperty("version")
    private final long version;
    
    /**
     * Creates a new player data update message.
     */
    public PlayerDataUpdateMessage(
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("serializedData") String serializedData,
            @JsonProperty("version") long version) {
        super(sourceServerId);
        this.playerId = playerId;
        this.dataType = dataType;
        this.serializedData = serializedData;
        this.version = version;
    }
    
    /**
     * Constructor for deserialization.
     */
    public PlayerDataUpdateMessage(
            @JsonProperty("messageId") UUID messageId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("dataType") String dataType,
            @JsonProperty("serializedData") String serializedData,
            @JsonProperty("version") long version) {
        super(messageId, timestamp, sourceServerId);
        this.playerId = playerId;
        this.dataType = dataType;
        this.serializedData = serializedData;
        this.version = version;
    }
    
    public UUID getPlayerId() { return playerId; }
    public String getDataType() { return dataType; }
    public String getSerializedData() { return serializedData; }
    public long getVersion() { return version; }
}