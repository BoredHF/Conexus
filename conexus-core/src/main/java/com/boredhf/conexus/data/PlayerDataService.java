package com.boredhf.conexus.data;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for managing cross-server player data synchronization.
 * 
 * Provides high-level operations for:
 * - Storing and retrieving player data across servers
 * - Real-time data synchronization
 * - Conflict resolution
 * - Caching for performance
 * - Change notifications
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface PlayerDataService {
    
    /**
     * Gets player data for a specific player and data type.
     * 
     * @param playerId the player's UUID
     * @param dataType the type of data to retrieve
     * @return a CompletableFuture containing the player data, or null if not found
     */
    <T extends PlayerData> CompletableFuture<T> getPlayerData(UUID playerId, Class<T> dataType);
    
    /**
     * Sets player data for a specific player.
     * 
     * @param playerId the player's UUID
     * @param data the data to store
     * @return a CompletableFuture that completes when data is stored
     */
    <T extends PlayerData> CompletableFuture<Void> setPlayerData(UUID playerId, T data);
    
    /**
     * Updates player data atomically with a modifier function.
     * 
     * @param playerId the player's UUID
     * @param dataType the type of data to update
     * @param modifier the function to modify the data
     * @return a CompletableFuture containing the updated data
     */
    <T extends PlayerData> CompletableFuture<T> updatePlayerData(UUID playerId, Class<T> dataType, DataModifier<T> modifier);
    
    /**
     * Deletes player data for a specific data type.
     * 
     * @param playerId the player's UUID
     * @param dataType the type of data to delete
     * @return a CompletableFuture that completes when data is deleted
     */
    <T extends PlayerData> CompletableFuture<Void> deletePlayerData(UUID playerId, Class<T> dataType);
    
    /**
     * Checks if player data exists for a specific player and data type.
     * 
     * @param playerId the player's UUID
     * @param dataType the type of data to check
     * @return a CompletableFuture containing true if data exists, false otherwise
     */
    <T extends PlayerData> CompletableFuture<Boolean> hasPlayerData(UUID playerId, Class<T> dataType);
    
    /**
     * Gets a player data container that provides convenient access to all data types for a player.
     * 
     * @param playerId the player's UUID
     * @return the player data container
     */
    PlayerDataContainer getPlayerContainer(UUID playerId);
    
    /**
     * Registers a listener for player data changes.
     * 
     * @param dataType the type of data to listen for
     * @param listener the change listener
     */
    <T extends PlayerData> void addDataChangeListener(Class<T> dataType, Consumer<PlayerDataChangeEvent<T>> listener);
    
    /**
     * Unregisters a data change listener.
     * 
     * @param dataType the type of data to stop listening for
     * @param listener the listener to remove
     */
    <T extends PlayerData> void removeDataChangeListener(Class<T> dataType, Consumer<PlayerDataChangeEvent<T>> listener);
    
    /**
     * Forces synchronization of cached data with the backend storage.
     * 
     * @param playerId the player's UUID
     * @return a CompletableFuture that completes when synchronization is done
     */
    CompletableFuture<Void> syncPlayerData(UUID playerId);
    
    /**
     * Clears cached data for a player (useful when player disconnects).
     * 
     * @param playerId the player's UUID
     * @return a CompletableFuture that completes when cache is cleared
     */
    CompletableFuture<Void> clearPlayerCache(UUID playerId);
}