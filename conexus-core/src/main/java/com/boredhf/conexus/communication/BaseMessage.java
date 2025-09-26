package com.boredhf.conexus.communication;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base implementation for messages.
 * 
 * Provides default implementations for common message metadata
 * and handles automatic ID and timestamp generation.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public abstract class BaseMessage implements Message {
    
    @JsonProperty("messageId")
    private final UUID messageId;
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("sourceServerId")
    private final String sourceServerId;
    
    /**
     * Creates a new base message with generated ID and current timestamp.
     * 
     * @param sourceServerId the ID of the server sending this message
     */
    protected BaseMessage(String sourceServerId) {
        this.messageId = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.sourceServerId = sourceServerId;
    }
    
    /**
     * Creates a new base message with specified values (for deserialization).
     * 
     * @param messageId the message ID
     * @param timestamp the message timestamp
     * @param sourceServerId the source server ID
     */
    protected BaseMessage(
            @JsonProperty("messageId") UUID messageId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceServerId") String sourceServerId) {
        this.messageId = messageId;
        this.timestamp = timestamp;
        this.sourceServerId = sourceServerId;
    }
    
    @Override
    public UUID getMessageId() {
        return messageId;
    }
    
    @Override
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getSourceServerId() {
        return sourceServerId;
    }
    
    @Override
    public String getMessageType() {
        return this.getClass().getSimpleName();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "messageId=" + messageId +
                ", timestamp=" + timestamp +
                ", sourceServerId='" + sourceServerId + '\'' +
                '}';
    }
}