package com.boredhf.conexus.communication;

import java.util.concurrent.CompletableFuture;

/**
 * Context information for a received message.
 * 
 * Provides access to the message content, metadata, and the ability
 * to send responses back to the sender.
 * 
 * @param <T> the type of the message
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface MessageContext<T extends Message> {
    
    /**
     * Gets the message that was received.
     * 
     * @return the received message
     */
    T getMessage();
    
    /**
     * Gets the name of the channel this message was received on.
     * 
     * @return the channel name
     */
    String getChannelName();
    
    /**
     * Checks if this message expects a response.
     * 
     * @return true if a response is expected, false otherwise
     */
    boolean expectsResponse();
    
    /**
     * Sends a response message back to the sender.
     * Only valid if expectsResponse() returns true.
     * 
     * @param response the response message
     * @return a CompletableFuture that completes when the response is sent
     * @throws IllegalStateException if no response is expected
     */
    CompletableFuture<Void> sendResponse(Message response);
    
    /**
     * Acknowledges receipt of the message without sending a response.
     * 
     * @return a CompletableFuture that completes when acknowledgment is sent
     */
    CompletableFuture<Void> acknowledge();
}