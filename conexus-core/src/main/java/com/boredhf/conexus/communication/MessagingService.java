package com.boredhf.conexus.communication;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Core messaging service for cross-server communication.
 * 
 * Provides high-level messaging operations including:
 * - Channel-based pub/sub messaging
 * - Direct server-to-server messaging
 * - Broadcast messaging to all servers
 * - Request-response patterns
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface MessagingService {
    
    /**
     * Gets a message channel by name.
     * 
     * @param channelName the name of the channel
     * @return the message channel
     */
    MessageChannel getChannel(String channelName);
    
    /**
     * Creates and registers a custom message channel.
     * 
     * @param channelName the name of the channel
     * @param messageType the type of messages this channel handles
     * @return the created message channel
     */
    <T extends Message> MessageChannel<T> createChannel(String channelName, Class<T> messageType);
    
    /**
     * Sends a message to a specific server.
     * 
     * @param targetServerId the target server ID
     * @param message the message to send
     * @return a CompletableFuture that completes when the message is sent
     */
    CompletableFuture<Void> sendToServer(String targetServerId, Message message);
    
    /**
     * Broadcasts a message to all connected servers.
     * 
     * @param message the message to broadcast
     * @return a CompletableFuture that completes when the message is sent
     */
    CompletableFuture<Void> broadcast(Message message);
    
    /**
     * Sends a message and waits for a response.
     * 
     * @param targetServerId the target server ID
     * @param request the request message
     * @param responseType the expected response type
     * @param timeoutMs timeout in milliseconds
     * @return a CompletableFuture containing the response
     */
    <T extends Message> CompletableFuture<T> sendRequest(String targetServerId, Message request, Class<T> responseType, long timeoutMs);
    
    /**
     * Registers a global message handler for a specific message type.
     * 
     * @param messageType the message type to handle
     * @param handler the message handler
     */
    <T extends Message> void registerHandler(Class<T> messageType, Consumer<MessageContext<T>> handler);
    
    /**
     * Unregisters a message handler.
     * 
     * @param messageType the message type to stop handling
     */
    <T extends Message> void unregisterHandler(Class<T> messageType);
    
    /**
     * Gets the current server ID.
     * 
     * @return the server ID
     */
    String getServerId();
}