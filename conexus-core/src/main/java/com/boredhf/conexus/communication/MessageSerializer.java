package com.boredhf.conexus.communication;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Handles serialization and deserialization of messages to/from JSON.
 * 
 * Uses Jackson with proper configuration for cross-server message exchange.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class MessageSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageSerializer.class);
    
    private final ObjectMapper objectMapper;
    
    public MessageSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules();
        
        // Configure to ignore unknown properties during deserialization
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Configure polymorphic type handling for Message types
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Message.class)
                .build();
        
        this.objectMapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
    }
    
    /**
     * Serializes a message to JSON bytes.
     * 
     * @param message the message to serialize
     * @return the serialized message bytes
     * @throws MessageSerializationException if serialization fails
     */
    public byte[] serialize(Message message) throws MessageSerializationException {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (IOException e) {
            logger.error("Failed to serialize message: {}", message, e);
            throw new MessageSerializationException("Failed to serialize message", e);
        }
    }
    
    /**
     * Deserializes JSON bytes to a message.
     * 
     * @param data the serialized message bytes
     * @return the deserialized message
     * @throws MessageSerializationException if deserialization fails
     */
    public Message deserialize(byte[] data) throws MessageSerializationException {
        try {
            return objectMapper.readValue(data, Message.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize message from {} bytes", data.length, e);
            throw new MessageSerializationException("Failed to deserialize message", e);
        }
    }
    
    /**
     * Deserializes JSON bytes to a specific message type.
     * 
     * @param data the serialized message bytes
     * @param messageType the expected message type
     * @return the deserialized message
     * @throws MessageSerializationException if deserialization fails
     */
    public <T extends Message> T deserialize(byte[] data, Class<T> messageType) throws MessageSerializationException {
        try {
            return objectMapper.readValue(data, messageType);
        } catch (IOException e) {
            logger.error("Failed to deserialize message to type {}", messageType.getSimpleName(), e);
            throw new MessageSerializationException("Failed to deserialize message to " + messageType.getSimpleName(), e);
        }
    }
    
    /**
     * Exception thrown when message serialization/deserialization fails.
     */
    public static class MessageSerializationException extends Exception {
        public MessageSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}