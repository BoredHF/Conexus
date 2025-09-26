package com.boredhf.conexus.examples;

import com.boredhf.conexus.events.*;
import com.boredhf.conexus.communication.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Example showing how to synchronize player data across multiple Minecraft servers.
 * This demonstrates real-world usage of the cross-server event broadcasting system.
 */
public class PlayerSyncExample {
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerSyncExample.class);
    
    private final CrossServerEventService eventService;
    private final Map<String, PlayerData> globalPlayerCache;
    
    public PlayerSyncExample(String serverId, MessagingService messagingService) {
        // Create custom configuration for player sync
        CrossServerEventConfiguration config = new CrossServerEventConfiguration(serverId)
            .setEnableCrossServerBroadcast(true)
            .setEnableGracefulDegradation(true)
            .setCircuitBreakerFailureThreshold(3) // Fail faster for player data
            .setCircuitBreakerTimeoutMillis(15000) // Shorter recovery time
            .setMaxRetryAttempts(2)
            .setRetryDelayMillis(500)
            .setRetryBackoffMultiplier(1.5);
            
        this.eventService = new CrossServerEventService(serverId, messagingService, config);
        this.globalPlayerCache = new HashMap<>();
        
        setupEventListeners();
    }
    
    /**
     * Initialize the service and set up event handling
     */
    public CompletableFuture<Void> initialize() {
        return eventService.initialize();
    }
    
    /**
     * Shutdown the service gracefully
     */
    public CompletableFuture<Void> shutdown() {
        return eventService.shutdown();
    }
    
    /**
     * Handle a player joining this server
     */
    public void handlePlayerJoin(String playerId, String playerName) {
        logger.info("Player {} ({}) joined this server", playerName, playerId);
        
        // Create and broadcast player join event
        PlayerSyncEvent joinEvent = new PlayerSyncEvent(
            eventService.getServerId(),
            playerId,
            playerName,
            PlayerSyncEvent.Action.JOIN
        );
        
        // Add current server info to metadata
        joinEvent.getMetadata().put("serverName", getServerDisplayName());
        joinEvent.getMetadata().put("playerCount", getCurrentPlayerCount());
        
        eventService.broadcastEvent(joinEvent, EventService.EventPriority.NORMAL)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.warn("Failed to broadcast player join event for {}: {}", 
                        playerId, throwable.getMessage());
                } else {
                    logger.debug("Successfully broadcast join event for player {}", playerId);
                }
            });
    }
    
    /**
     * Handle a player leaving this server
     */
    public void handlePlayerLeave(String playerId, String playerName) {
        logger.info("Player {} ({}) left this server", playerName, playerId);
        
        PlayerSyncEvent leaveEvent = new PlayerSyncEvent(
            eventService.getServerId(),
            playerId,
            playerName,
            PlayerSyncEvent.Action.LEAVE
        );
        
        leaveEvent.getMetadata().put("serverName", getServerDisplayName());
        leaveEvent.getMetadata().put("playerCount", getCurrentPlayerCount() - 1);
        
        eventService.broadcastEvent(leaveEvent, EventService.EventPriority.NORMAL);
    }
    
    /**
     * Update player data (like location, health, etc.) and sync across servers
     */
    public void syncPlayerData(String playerId, String playerName, Map<String, Object> playerData) {
        PlayerDataSyncEvent dataEvent = new PlayerDataSyncEvent(
            eventService.getServerId(),
            playerId,
            playerName,
            playerData
        );
        
        // Broadcast with low priority since this might happen frequently
        eventService.broadcastEvent(dataEvent, EventService.EventPriority.LOW)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.debug("Failed to sync player data for {}: {}", 
                        playerId, throwable.getMessage());
                }
            });
    }
    
    /**
     * Get cached player data from any server in the network
     */
    public PlayerData getGlobalPlayerData(String playerId) {
        return globalPlayerCache.get(playerId);
    }
    
    /**
     * Check if a player is online on any server in the network
     */
    public boolean isPlayerOnlineAnywhere(String playerId) {
        PlayerData data = globalPlayerCache.get(playerId);
        return data != null && data.isOnline();
    }
    
    /**
     * Get current metrics for monitoring
     */
    public void logCurrentMetrics() {
        eventService.logMetrics();
        logger.info("Global player cache size: {}", globalPlayerCache.size());
        
        long onlineCount = globalPlayerCache.values().stream()
            .mapToLong(data -> data.isOnline() ? 1 : 0)
            .sum();
        logger.info("Players online across network: {}", onlineCount);
    }
    
    private void setupEventListeners() {
        // Listen for player join/leave events from other servers
        eventService.registerEventListener(PlayerSyncEvent.class, this::handleRemotePlayerEvent);
        
        // Listen for player data updates from other servers
        eventService.registerEventListener(PlayerDataSyncEvent.class, this::handleRemotePlayerDataSync);
        
        // Listen for server status changes
        eventService.registerEventListener(ServerStatusEvent.class, this::handleServerStatusChange);
    }
    
    private void handleRemotePlayerEvent(PlayerSyncEvent event) {
        // Don't process our own events
        if (event.getSourceServerId().equals(eventService.getServerId())) {
            return;
        }
        
        logger.info("Player {} {} server {}", 
            event.getPlayerName(), 
            event.getAction().toString().toLowerCase(),
            event.getSourceServerId());
        
        // Update global player cache
        String playerId = event.getPlayerId();
        PlayerData playerData = globalPlayerCache.computeIfAbsent(playerId, 
            id -> new PlayerData(id, event.getPlayerName()));
        
        if (event.getAction() == PlayerSyncEvent.Action.JOIN) {
            playerData.setOnline(true);
            playerData.setCurrentServer(event.getSourceServerId());
            playerData.setLastSeen(event.getTimestamp());
            
            // Notify local players about network join
            notifyLocalPlayers("§a" + event.getPlayerName() + " joined " + 
                getServerDisplayName(event.getSourceServerId()));
                
        } else if (event.getAction() == PlayerSyncEvent.Action.LEAVE) {
            playerData.setOnline(false);
            playerData.setCurrentServer(null);
            playerData.setLastSeen(event.getTimestamp());
            
            // Notify local players about network leave
            notifyLocalPlayers("§c" + event.getPlayerName() + " left " + 
                getServerDisplayName(event.getSourceServerId()));
        }
    }
    
    private void handleRemotePlayerDataSync(PlayerDataSyncEvent event) {
        // Don't process our own events
        if (event.getSourceServerId().equals(eventService.getServerId())) {
            return;
        }
        
        String playerId = event.getPlayerId();
        PlayerData playerData = globalPlayerCache.computeIfAbsent(playerId, 
            id -> new PlayerData(id, event.getPlayerName()));
            
        // Update cached data
        playerData.updateData(event.getPlayerData());
        playerData.setLastSeen(event.getTimestamp());
        
        logger.debug("Updated cached data for player {} from server {}", 
            event.getPlayerName(), event.getSourceServerId());
    }
    
    private void handleServerStatusChange(ServerStatusEvent event) {
        logger.info("Server {} status changed to: {} - {}", 
            event.getSourceServerId(), event.getStatus(), event.getMessage());
            
        // If a server went offline, mark all its players as offline
        if (event.getStatus() == ServerStatusEvent.Status.STOPPED) {
            globalPlayerCache.values().stream()
                .filter(player -> event.getSourceServerId().equals(player.getCurrentServer()))
                .forEach(player -> {
                    player.setOnline(false);
                    player.setCurrentServer(null);
                });
        }
    }
    
    // Mock methods - would be implemented based on your server platform
    private String getServerDisplayName() {
        return "Server-" + eventService.getServerId();
    }
    
    private String getServerDisplayName(String serverId) {
        return "Server-" + serverId; // Could lookup from config
    }
    
    private int getCurrentPlayerCount() {
        return 10; // Would get actual count from server
    }
    
    private void notifyLocalPlayers(String message) {
        logger.info("Broadcasting to local players: {}", message);
        // Would send to all local players via server API
    }
    
    /**
     * Player synchronization event for join/leave actions
     */
    public static class PlayerSyncEvent implements EventService.NetworkEvent {
        
        public enum Action {
            JOIN, LEAVE
        }
        
        private final String sourceServerId;
        private final String playerId;
        private final String playerName;
        private final Action action;
        private final Instant timestamp;
        private final Map<String, Object> metadata;
        
        @JsonCreator
        public PlayerSyncEvent(
                @JsonProperty("sourceServerId") String sourceServerId,
                @JsonProperty("playerId") String playerId,
                @JsonProperty("playerName") String playerName,
                @JsonProperty("action") Action action) {
            this.sourceServerId = sourceServerId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.action = action;
            this.timestamp = Instant.now();
            this.metadata = new HashMap<>();
        }
        
        @Override
        @JsonProperty("sourceServerId")
        public String getSourceServerId() {
            return sourceServerId;
        }
        
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        @JsonProperty("playerId")
        public String getPlayerId() {
            return playerId;
        }
        
        @JsonProperty("playerName")
        public String getPlayerName() {
            return playerName;
        }
        
        @JsonProperty("action")
        public Action getAction() {
            return action;
        }
    }
    
    /**
     * Player data synchronization event for sharing player state
     */
    public static class PlayerDataSyncEvent implements EventService.NetworkEvent {
        
        private final String sourceServerId;
        private final String playerId;
        private final String playerName;
        private final Map<String, Object> playerData;
        private final Instant timestamp;
        private final Map<String, Object> metadata;
        
        @JsonCreator
        public PlayerDataSyncEvent(
                @JsonProperty("sourceServerId") String sourceServerId,
                @JsonProperty("playerId") String playerId,
                @JsonProperty("playerName") String playerName,
                @JsonProperty("playerData") Map<String, Object> playerData) {
            this.sourceServerId = sourceServerId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.playerData = new HashMap<>(playerData);
            this.timestamp = Instant.now();
            this.metadata = new HashMap<>();
        }
        
        @Override
        @JsonProperty("sourceServerId")
        public String getSourceServerId() {
            return sourceServerId;
        }
        
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        @JsonProperty("playerId")
        public String getPlayerId() {
            return playerId;
        }
        
        @JsonProperty("playerName")
        public String getPlayerName() {
            return playerName;
        }
        
        @JsonProperty("playerData")
        public Map<String, Object> getPlayerData() {
            return playerData;
        }
    }
    
    /**
     * Cached player data structure
     */
    public static class PlayerData {
        private final String playerId;
        private final String playerName;
        private boolean online;
        private String currentServer;
        private Instant lastSeen;
        private final Map<String, Object> cachedData;
        
        public PlayerData(String playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.online = false;
            this.cachedData = new HashMap<>();
            this.lastSeen = Instant.now();
        }
        
        public void updateData(Map<String, Object> newData) {
            cachedData.putAll(newData);
        }
        
        // Getters and setters
        public String getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
        public String getCurrentServer() { return currentServer; }
        public void setCurrentServer(String currentServer) { this.currentServer = currentServer; }
        public Instant getLastSeen() { return lastSeen; }
        public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
        public Map<String, Object> getCachedData() { return cachedData; }
    }
    
    // Example main method showing usage
    public static void main(String[] args) {
        // This would typically be set up in your server plugin initialization
        
        // Create messaging service (would use Redis in production)
        InMemoryMessagingService messaging = new InMemoryMessagingService("server-1");
        
        // Create player sync service
        PlayerSyncExample playerSync = new PlayerSyncExample("server-1", messaging);
        
        // Initialize
        playerSync.initialize().thenRun(() -> {
            logger.info("Player sync service initialized");
            
            // Simulate player joining
            playerSync.handlePlayerJoin("uuid-123", "TestPlayer");
            
            // Simulate player data sync
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("health", 20.0);
            playerData.put("level", 15);
            playerData.put("world", "world_nether");
            
            playerSync.syncPlayerData("uuid-123", "TestPlayer", playerData);
            
            // Log metrics
            playerSync.logCurrentMetrics();
        });
    }
}