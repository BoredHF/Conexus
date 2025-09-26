package com.boredhf.conexus.communication;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A typed message channel for pub/sub communication.
 * 
 * Channels provide a high-level abstraction over the underlying transport
 * and handle message serialization/deserialization automatically.
 * 
 * @param <T> the type of messages this channel handles
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface MessageChannel<T extends Message> {
    
    /**
     * Gets the name of this channel.
     * 
     * @return the channel name
     */
    String getName();
    
    /**
     * Gets the message type this channel handles.
     * 
     * @return the message type class
     */
    Class<T> getMessageType();
    
    /**
     * Publishes a message to this channel.
     * 
     * @param message the message to publish
     * @return a CompletableFuture that completes when the message is published
     */
    CompletableFuture<Void> publish(T message);
    
    /**
     * Subscribes to this channel with a message handler.
     * 
     * @param handler the message handler
     * @return a CompletableFuture that completes when subscription is established
     */
    CompletableFuture<Void> subscribe(Consumer<MessageContext<T>> handler);
    
    /**
     * Unsubscribes from this channel.
     * 
     * @return a CompletableFuture that completes when unsubscription is finished
     */
    CompletableFuture<Void> unsubscribe();
    
    /**
     * Checks if this channel is currently subscribed.
     * 
     * @return true if subscribed, false otherwise
     */
    boolean isSubscribed();
}