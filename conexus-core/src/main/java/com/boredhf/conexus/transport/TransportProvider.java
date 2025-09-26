package com.boredhf.conexus.transport;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Abstract transport provider that enables different communication backends.
 * 
 * This allows Conexus to support multiple transport mechanisms:
 * - Redis (recommended for production)
 * - RabbitMQ
 * - TCP sockets
 * - In-memory (for testing)
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface TransportProvider {
    
    /**
     * Connects to the transport backend.
     * 
     * @return a CompletableFuture that completes when connection is established
     */
    CompletableFuture<Void> connect();
    
    /**
     * Disconnects from the transport backend.
     * 
     * @return a CompletableFuture that completes when disconnection is finished
     */
    CompletableFuture<Void> disconnect();
    
    /**
     * Checks if the transport is currently connected.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Publishes a message to a specific channel.
     * 
     * @param channel the channel to publish to
     * @param message the message to publish
     * @return a CompletableFuture that completes when the message is published
     */
    CompletableFuture<Void> publish(String channel, byte[] message);
    
    /**
     * Subscribes to a channel and registers a message handler.
     * 
     * @param channel the channel to subscribe to
     * @param messageHandler the handler for received messages
     * @return a CompletableFuture that completes when subscription is established
     */
    CompletableFuture<Void> subscribe(String channel, Consumer<byte[]> messageHandler);
    
    /**
     * Unsubscribes from a channel.
     * 
     * @param channel the channel to unsubscribe from
     * @return a CompletableFuture that completes when unsubscription is finished
     */
    CompletableFuture<Void> unsubscribe(String channel);
    
    /**
     * Stores data with a key.
     * 
     * @param key the key to store data under
     * @param data the data to store
     * @return a CompletableFuture that completes when data is stored
     */
    CompletableFuture<Void> store(String key, byte[] data);
    
    /**
     * Stores data with a key and expiration time.
     * 
     * @param key the key to store data under
     * @param data the data to store
     * @param ttlMillis time to live in milliseconds
     * @return a CompletableFuture that completes when data is stored
     */
    CompletableFuture<Void> store(String key, byte[] data, long ttlMillis);
    
    /**
     * Retrieves data by key.
     * 
     * @param key the key to retrieve data for
     * @return a CompletableFuture containing the data, or null if not found
     */
    CompletableFuture<byte[]> retrieve(String key);
    
    /**
     * Deletes data by key.
     * 
     * @param key the key to delete
     * @return a CompletableFuture that completes when data is deleted
     */
    CompletableFuture<Void> delete(String key);
    
    /**
     * Checks if a key exists.
     * 
     * @param key the key to check
     * @return a CompletableFuture containing true if the key exists, false otherwise
     */
    CompletableFuture<Boolean> exists(String key);
    
    /**
     * Gets the name/type of this transport provider.
     * 
     * @return the transport provider name
     */
    String getName();
}