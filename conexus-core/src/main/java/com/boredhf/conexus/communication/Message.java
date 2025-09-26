package com.boredhf.conexus.communication;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all messages in the Conexus system.
 * 
 * All messages must be serializable and include basic metadata
 * like ID, timestamp, and source server information.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public interface Message {
    
    /**
     * Gets the unique identifier for this message.
     * 
     * @return the message ID
     */
    UUID getMessageId();
    
    /**
     * Gets the timestamp when this message was created.
     * 
     * @return the message timestamp
     */
    Instant getTimestamp();
    
    /**
     * Gets the ID of the server that sent this message.
     * 
     * @return the source server ID
     */
    String getSourceServerId();
    
    /**
     * Gets the type identifier for this message.
     * Used for routing and deserialization.
     * 
     * @return the message type
     */
    String getMessageType();
}