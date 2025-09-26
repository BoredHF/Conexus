package com.boredhf.conexus.communication.messages;

import com.boredhf.conexus.communication.BaseMessage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * A simple text message for basic cross-server communication.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class SimpleTextMessage extends BaseMessage {
    
    @JsonProperty("content")
    private final String content;
    
    @JsonProperty("category")
    private final String category;
    
    /**
     * Creates a new text message.
     * 
     * @param sourceServerId the source server ID
     * @param content the message content
     * @param category optional category for filtering
     */
    public SimpleTextMessage(String sourceServerId, String content, String category) {
        super(sourceServerId);
        this.content = content;
        this.category = category;
    }
    
    /**
     * Constructor for deserialization.
     */
    public SimpleTextMessage(
            @JsonProperty("messageId") UUID messageId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("content") String content,
            @JsonProperty("category") String category) {
        super(messageId, timestamp, sourceServerId);
        this.content = content;
        this.category = category;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getCategory() {
        return category;
    }
    
    @Override
    public String toString() {
        return "SimpleTextMessage{" +
                "content='" + content + '\'' +
                ", category='" + category + '\'' +
                ", " + super.toString() +
                '}';
    }
}