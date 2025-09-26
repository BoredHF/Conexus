package com.boredhf.conexus;

import com.boredhf.conexus.communication.MessagingService;
import com.boredhf.conexus.data.PlayerDataService;
import com.boredhf.conexus.events.EventService;
import com.boredhf.conexus.moderation.ModerationService;
import com.boredhf.conexus.transport.TransportProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Main interface for the Conexus cross-server communication library.
 * 
 * This interface provides access to all core services:
 * - Cross-server messaging
 * - Player data synchronization
 * - Event broadcasting
 * - Moderation tools
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface Conexus {
    
    /**
     * Gets the messaging service for cross-server communication.
     * 
     * @return the messaging service
     */
    MessagingService getMessagingService();
    
    /**
     * Gets the player data service for cross-server player data management.
     * 
     * @return the player data service
     */
    PlayerDataService getPlayerDataService();
    
    /**
     * Gets the event service for cross-server event broadcasting.
     * 
     * @return the event service
     */
    EventService getEventService();
    
    /**
     * Gets the moderation service for cross-server moderation actions.
     * 
     * @return the moderation service
     */
    ModerationService getModerationService();
    
    /**
     * Gets the current server identifier.
     * 
     * @return the server identifier
     */
    String getServerId();
    
    /**
     * Gets the transport provider being used for communication.
     * 
     * @return the transport provider
     */
    TransportProvider getTransportProvider();
    
    /**
     * Initializes the Conexus library with the given configuration.
     * 
     * @return a CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Shuts down the Conexus library gracefully.
     * 
     * @return a CompletableFuture that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();
    
    /**
     * Checks if the library is currently connected and operational.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
}