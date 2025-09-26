# Examples

This document provides real-world examples of using Conexus in various scenarios.

## Table of Contents

- [Basic Examples](#basic-examples)
- [Cross-Server Chat](#cross-server-chat)
- [Player Data Synchronization](#player-data-synchronization)
- [Network Events](#network-events)
- [Moderation Tools](#moderation-tools)
- [Custom Message Types](#custom-message-types)
- [Advanced Patterns](#advanced-patterns)

## Basic Examples

### Simple Message Broadcasting

Send a simple message to all servers:

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    SimpleTextMessage message = new SimpleTextMessage(
        conexus.getServerId(),
        event.getPlayer().getName() + " joined " + conexus.getServerId(),
        "join-leave"
    );
    
    conexus.getMessagingService().broadcast(message)
        .exceptionally(throwable -> {
            getLogger().warning("Failed to broadcast join message: " + throwable.getMessage());
            return null;
        });
}
```

### Handling Messages

Listen for messages from other servers:

```java
@Override
public void onEnable() {
    // ... initialize Conexus ...
    
    // Register message handler
    conexus.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
        SimpleTextMessage message = context.getMessage();
        
        // Handle different message categories
        switch (message.getCategory()) {
            case "join-leave":
                if (getConfig().getBoolean("show-network-joins", true)) {
                    Bukkit.broadcastMessage("§7[Network] §f" + message.getContent());
                }
                break;
                
            case "global-chat":
                Bukkit.broadcastMessage("§7[Global] §f" + message.getContent());
                break;
                
            case "staff-alert":
                // Send only to players with permission
                Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission("network.staff"))
                    .forEach(player -> player.sendMessage("§c[STAFF] §f" + message.getContent()));
                break;
        }
    });
}
```

## Cross-Server Chat

### Global Chat System

Create a comprehensive global chat system:

```java
public class GlobalChatManager {
    private final Conexus conexus;
    private final Plugin plugin;
    
    public GlobalChatManager(Plugin plugin, Conexus conexus) {
        this.plugin = plugin;
        this.conexus = conexus;
        setupMessageHandlers();
    }
    
    private void setupMessageHandlers() {
        conexus.getMessagingService().registerHandler(GlobalChatMessage.class, this::handleGlobalMessage);
    }
    
    public void sendGlobalMessage(Player sender, String message) {
        GlobalChatMessage chatMessage = new GlobalChatMessage(
            conexus.getServerId(),
            sender.getUniqueId(),
            sender.getName(),
            message,
            System.currentTimeMillis()
        );
        
        conexus.getMessagingService().broadcast(chatMessage)
            .thenRun(() -> displayMessage(sender, message, conexus.getServerId()))
            .exceptionally(throwable -> {
                sender.sendMessage("§cFailed to send global message!");
                return null;
            });
    }
    
    private void handleGlobalMessage(MessageContext<GlobalChatMessage> context) {
        GlobalChatMessage message = context.getMessage();
        
        // Don't display our own messages (already shown locally)
        if (message.getSourceServerId().equals(conexus.getServerId())) {
            return;
        }
        
        // Run on main thread for Bukkit API access
        Bukkit.getScheduler().runTask(plugin, () -> {
            displayMessage(null, message.getMessage(), message.getSourceServerId());
        });
    }
    
    private void displayMessage(Player sender, String message, String serverName) {
        String formattedMessage = String.format("§7[§bGlobal§7] §7[§e%s§7] §f%s", 
            serverName, message);
        
        if (sender != null) {
            // Local message - show to all players except sender
            Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.equals(sender))
                .forEach(player -> player.sendMessage(formattedMessage));
        } else {
            // Remote message - show to all players
            Bukkit.broadcastMessage(formattedMessage);
        }
    }
}

// Custom message type for global chat
public class GlobalChatMessage extends BaseMessage {
    private final UUID senderId;
    private final String senderName;
    private final String message;
    private final long timestamp;
    
    public GlobalChatMessage(String sourceServerId, UUID senderId, String senderName, 
                            String message, long timestamp) {
        super(sourceServerId);
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    // Getters...
}

// Command handler
@Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        sender.sendMessage("Only players can use global chat!");
        return true;
    }
    
    if (args.length == 0) {
        sender.sendMessage("Usage: /g <message>");
        return true;
    }
    
    String message = String.join(" ", args);
    globalChatManager.sendGlobalMessage((Player) sender, message);
    return true;
}
```

### Private Cross-Server Messaging

Implement private messages between players on different servers:

```java
public class CrossServerMessaging {
    
    public void sendPrivateMessage(Player sender, String targetPlayerName, String message) {
        PrivateMessage privateMsg = new PrivateMessage(
            conexus.getServerId(),
            sender.getUniqueId(),
            sender.getName(),
            targetPlayerName,
            message
        );
        
        // Try to find target player on all servers
        conexus.getMessagingService().broadcast(privateMsg);
        
        // Inform sender
        sender.sendMessage("§7[MSG] §fMessage sent to §e" + targetPlayerName + "§f: " + message);
    }
    
    @EventHandler
    public void handlePrivateMessage(MessageContext<PrivateMessage> context) {
        PrivateMessage message = context.getMessage();
        
        // Check if target player is on this server
        Player target = Bukkit.getPlayer(message.getTargetPlayerName());
        if (target != null) {
            // Deliver message
            target.sendMessage("§7[MSG] §e" + message.getSenderName() + 
                " §7(§b" + message.getSourceServerId() + "§7)§f: " + message.getMessage());
            
            // Send delivery confirmation back to sender
            MessageDeliveryConfirmation confirmation = new MessageDeliveryConfirmation(
                conexus.getServerId(),
                message.getSenderId(),
                true,
                "Message delivered to " + target.getName()
            );
            
            conexus.getMessagingService().sendToServer(message.getSourceServerId(), confirmation);
        }
    }
}
```

## Player Data Synchronization

### Cross-Server Economy

Synchronize player economy data across servers:

```java
public class NetworkEconomy {
    private final PlayerDataService playerDataService;
    
    public NetworkEconomy(Conexus conexus) {
        this.playerDataService = conexus.getPlayerDataService();
        
        // Listen for balance changes from other servers
        playerDataService.addDataChangeListener(PlayerEconomyData.class, this::handleBalanceChange);
    }
    
    public CompletableFuture<Double> getBalance(UUID playerId) {
        return playerDataService.getPlayerData(playerId, PlayerEconomyData.class)
            .thenApply(data -> data != null ? data.getBalance() : 0.0);
    }
    
    public CompletableFuture<Boolean> setBalance(UUID playerId, double newBalance) {
        return playerDataService.updatePlayerData(playerId, PlayerEconomyData.class, data -> {
            if (data == null) {
                data = new PlayerEconomyData(playerId, 0.0, conexus.getServerId());
            }
            return data.withBalance(newBalance);
        }).thenApply(data -> true);
    }
    
    public CompletableFuture<Boolean> addBalance(UUID playerId, double amount) {
        return playerDataService.updatePlayerData(playerId, PlayerEconomyData.class, data -> {
            if (data == null) {
                data = new PlayerEconomyData(playerId, 0.0, conexus.getServerId());
            }
            return data.withBalance(data.getBalance() + amount);
        }).thenApply(data -> true);
    }
    
    private void handleBalanceChange(PlayerDataChangeEvent<PlayerEconomyData> event) {
        // Update local cache or notify plugins of balance change
        Player player = Bukkit.getPlayer(event.getPlayerId());
        if (player != null) {
            // Player is online - update their display
            updatePlayerBalanceDisplay(player, event.getNewValue().getBalance());
        }
    }
    
    private void updatePlayerBalanceDisplay(Player player, double newBalance) {
        // Update scoreboard, action bar, etc.
        player.sendActionBar("§6Balance: §f$" + String.format("%.2f", newBalance));
    }
}

// Player economy data class
public class PlayerEconomyData implements PlayerData {
    private final UUID playerId;
    private final double balance;
    private final int version;
    private final Instant lastModified;
    private final String lastModifiedBy;
    
    public PlayerEconomyData(UUID playerId, double balance, String serverId) {
        this.playerId = playerId;
        this.balance = balance;
        this.version = 1;
        this.lastModified = Instant.now();
        this.lastModifiedBy = serverId;
    }
    
    public PlayerEconomyData withBalance(double newBalance) {
        return new PlayerEconomyData(playerId, newBalance, lastModifiedBy);
    }
    
    // Implement PlayerData interface...
}
```

### Cross-Server Player Statistics

Track player statistics across all servers:

```java
public class NetworkStatsManager {
    
    public CompletableFuture<Void> incrementPlayerStat(UUID playerId, String statName, int amount) {
        return playerDataService.updatePlayerData(playerId, PlayerStatsData.class, data -> {
            if (data == null) {
                data = new PlayerStatsData(playerId, new HashMap<>(), conexus.getServerId());
            }
            
            Map<String, Integer> stats = new HashMap<>(data.getStats());
            stats.merge(statName, amount, Integer::sum);
            
            return data.withStats(stats);
        }).thenApply(data -> null);
    }
    
    public CompletableFuture<Integer> getPlayerStat(UUID playerId, String statName) {
        return playerDataService.getPlayerData(playerId, PlayerStatsData.class)
            .thenApply(data -> data != null ? data.getStats().getOrDefault(statName, 0) : 0);
    }
    
    public CompletableFuture<Map<String, Integer>> getAllPlayerStats(UUID playerId) {
        return playerDataService.getPlayerData(playerId, PlayerStatsData.class)
            .thenApply(data -> data != null ? data.getStats() : new HashMap<>());
    }
}

// Usage example
@EventHandler
public void onPlayerKill(PlayerDeathEvent event) {
    Player killer = event.getEntity().getKiller();
    if (killer != null) {
        // Increment kill count across network
        networkStats.incrementPlayerStat(killer.getUniqueId(), "kills", 1);
        
        // Increment death count for victim
        networkStats.incrementPlayerStat(event.getEntity().getUniqueId(), "deaths", 1);
    }
}
```

## Network Events

### Server Status Broadcasting

Broadcast server status updates to the network:

```java
public class ServerStatusManager {
    
    @EventHandler
    public void onServerStart(ServerLoadEvent event) {
        ServerStatusMessage status = new ServerStatusMessage(
            conexus.getServerId(),
            ServerStatus.ONLINE,
            Bukkit.getOnlinePlayers().size(),
            Bukkit.getMaxPlayers(),
            getServerMotd()
        );
        
        conexus.getMessagingService().broadcast(status);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        broadcastPlayerCount();
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Delay to ensure player has actually left
        Bukkit.getScheduler().runTaskLater(plugin, this::broadcastPlayerCount, 1L);
    }
    
    private void broadcastPlayerCount() {
        ServerStatusMessage status = new ServerStatusMessage(
            conexus.getServerId(),
            ServerStatus.ONLINE,
            Bukkit.getOnlinePlayers().size(),
            Bukkit.getMaxPlayers(),
            null  // Don't update MOTD every time
        );
        
        conexus.getMessagingService().broadcast(status);
    }
}

// Handle status updates from other servers
public void handleServerStatus(MessageContext<ServerStatusMessage> context) {
    ServerStatusMessage status = context.getMessage();
    
    // Update internal server list
    serverList.updateServer(status.getSourceServerId(), status);
    
    // Notify relevant plugins
    Bukkit.getPluginManager().callEvent(new NetworkServerStatusEvent(status));
}
```

### Cross-Server Events

Create custom events that span multiple servers:

```java
// Network-wide drop party event
public class NetworkDropParty {
    
    public void startDropParty(Location location, List<ItemStack> items, int durationSeconds) {
        DropPartyStartMessage message = new DropPartyStartMessage(
            conexus.getServerId(),
            location,
            items,
            durationSeconds,
            System.currentTimeMillis()
        );
        
        // Announce to all servers
        conexus.getMessagingService().broadcast(message);
        
        // Start local drop party
        executeDropParty(location, items, durationSeconds);
    }
    
    public void handleDropPartyStart(MessageContext<DropPartyStartMessage> context) {
        DropPartyStartMessage message = context.getMessage();
        
        // Don't handle our own events
        if (message.getSourceServerId().equals(conexus.getServerId())) {
            return;
        }
        
        // Announce to players
        String announcement = String.format(
            "§6[§eNetwork§6] §fDrop party started on §e%s§f! Duration: §b%d seconds",
            message.getSourceServerId(),
            message.getDurationSeconds()
        );
        
        Bukkit.broadcastMessage(announcement);
        
        // Optionally provide teleport option
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("network.teleport")) {
                player.spigot().sendMessage(
                    new ComponentBuilder("§7[Click to teleport] ")
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + message.getSourceServerId()))
                        .append("§6Drop Party on " + message.getSourceServerId())
                        .create()
                );
            }
        });
    }
}
```

## Moderation Tools

### Network-Wide Punishments

Implement cross-server punishment system:

```java
public class NetworkModerationManager {
    
    public CompletableFuture<Void> banPlayer(UUID playerId, String reason, UUID moderatorId, Duration duration) {
        NetworkPunishment punishment = new NetworkPunishment(
            PunishmentType.BAN,
            playerId,
            moderatorId,
            reason,
            duration,
            System.currentTimeMillis()
        );
        
        // Store punishment in database
        return storePunishment(punishment)
            .thenCompose(v -> {
                // Broadcast to all servers
                NetworkPunishmentMessage message = new NetworkPunishmentMessage(
                    conexus.getServerId(),
                    punishment
                );
                
                return conexus.getMessagingService().broadcast(message);
            });
    }
    
    public void handlePunishmentMessage(MessageContext<NetworkPunishmentMessage> context) {
        NetworkPunishment punishment = context.getMessage().getPunishment();
        
        // Apply punishment locally
        applyPunishment(punishment);
        
        // Log for audit trail
        getLogger().info(String.format(
            "Network %s applied to %s by %s: %s",
            punishment.getType(),
            punishment.getPlayerId(),
            punishment.getModeratorId(),
            punishment.getReason()
        ));
    }
    
    private void applyPunishment(NetworkPunishment punishment) {
        Player player = Bukkit.getPlayer(punishment.getPlayerId());
        if (player != null) {
            switch (punishment.getType()) {
                case BAN:
                    player.kickPlayer("§cYou have been banned from the network!\n§fReason: " + punishment.getReason());
                    break;
                case KICK:
                    player.kickPlayer("§cYou have been kicked from the network!\n§fReason: " + punishment.getReason());
                    break;
                case MUTE:
                    // Add to muted players list
                    mutedPlayers.add(punishment.getPlayerId());
                    player.sendMessage("§cYou have been muted network-wide!\n§fReason: " + punishment.getReason());
                    break;
            }
        }
    }
}
```

### Staff Chat System

Create a network-wide staff communication system:

```java
public class StaffChatManager {
    
    public void sendStaffMessage(Player sender, String message) {
        StaffChatMessage staffMessage = new StaffChatMessage(
            conexus.getServerId(),
            sender.getUniqueId(),
            sender.getName(),
            message,
            System.currentTimeMillis()
        );
        
        conexus.getMessagingService().broadcast(staffMessage);
        
        // Display locally
        displayStaffMessage(staffMessage, true);
    }
    
    public void handleStaffMessage(MessageContext<StaffChatMessage> context) {
        StaffChatMessage message = context.getMessage();
        
        // Don't display our own messages twice
        if (!message.getSourceServerId().equals(conexus.getServerId())) {
            displayStaffMessage(message, false);
        }
    }
    
    private void displayStaffMessage(StaffChatMessage message, boolean isLocal) {
        String prefix = isLocal ? "§7[§cSTAFF§7]" : "§7[§cSTAFF§7] §7[§e" + message.getSourceServerId() + "§7]";
        String formattedMessage = String.format("%s §b%s§f: %s", 
            prefix, message.getSenderName(), message.getMessage());
        
        // Send to all staff members
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission("network.staff.chat"))
            .forEach(player -> player.sendMessage(formattedMessage));
        
        // Log to console
        getLogger().info("STAFF CHAT: " + message.getSenderName() + 
            " (" + message.getSourceServerId() + "): " + message.getMessage());
    }
}
```

## Custom Message Types

### Complex Data Structures

Create sophisticated message types for complex data:

```java
// Player teleport request between servers
public class TeleportRequestMessage extends BaseMessage {
    private final UUID playerId;
    private final String playerName;
    private final String targetServer;
    private final Location targetLocation;
    private final TeleportReason reason;
    private final Map<String, Object> metadata;
    
    public TeleportRequestMessage(String sourceServerId, UUID playerId, String playerName,
                                 String targetServer, Location targetLocation, 
                                 TeleportReason reason, Map<String, Object> metadata) {
        super(sourceServerId);
        this.playerId = playerId;
        this.playerName = playerName;
        this.targetServer = targetServer;
        this.targetLocation = targetLocation;
        this.reason = reason;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
    
    // Getters...
}

// Handler for teleport requests
public void handleTeleportRequest(MessageContext<TeleportRequestMessage> context) {
    TeleportRequestMessage message = context.getMessage();
    
    // Check if this server should handle the request
    if (!message.getTargetServer().equals(conexus.getServerId())) {
        return;
    }
    
    // Validate teleport request
    if (!isValidTeleportRequest(message)) {
        sendTeleportResponse(message, false, "Invalid teleport request");
        return;
    }
    
    // Execute teleport
    executeTeleport(message)
        .thenRun(() -> sendTeleportResponse(message, true, "Teleport successful"))
        .exceptionally(throwable -> {
            sendTeleportResponse(message, false, "Teleport failed: " + throwable.getMessage());
            return null;
        });
}
```

### Batch Operations

Handle multiple operations in a single message:

```java
// Batch player data update
public class BatchPlayerUpdateMessage extends BaseMessage {
    private final List<PlayerDataUpdate> updates;
    private final String operation;
    private final Map<String, Object> parameters;
    
    public BatchPlayerUpdateMessage(String sourceServerId, List<PlayerDataUpdate> updates,
                                   String operation, Map<String, Object> parameters) {
        super(sourceServerId);
        this.updates = updates;
        this.operation = operation;
        this.parameters = parameters;
    }
    
    // Handle batch updates
    public void handleBatchUpdate(MessageContext<BatchPlayerUpdateMessage> context) {
        BatchPlayerUpdateMessage message = context.getMessage();
        
        // Process updates asynchronously
        CompletableFuture.runAsync(() -> {
            int processed = 0;
            int errors = 0;
            
            for (PlayerDataUpdate update : message.getUpdates()) {
                try {
                    processPlayerDataUpdate(update);
                    processed++;
                } catch (Exception e) {
                    errors++;
                    getLogger().warning("Failed to process player data update: " + e.getMessage());
                }
            }
            
            // Send result back to source server
            BatchUpdateResultMessage result = new BatchUpdateResultMessage(
                conexus.getServerId(),
                message.getMessageId(),
                processed,
                errors
            );
            
            conexus.getMessagingService().sendToServer(message.getSourceServerId(), result);
        });
    }
}
```

## Advanced Patterns

### Request-Response Pattern

Implement sophisticated request-response communication:

```java
public class NetworkDataService {
    
    public CompletableFuture<ServerInfoResponse> getServerInfo(String targetServerId) {
        ServerInfoRequest request = new ServerInfoRequest(
            conexus.getServerId(),
            Arrays.asList("players", "tps", "memory", "plugins")
        );
        
        return conexus.getMessagingService()
            .sendRequest(targetServerId, request, ServerInfoResponse.class, 10000L);
    }
    
    // Handle incoming requests
    public void handleServerInfoRequest(MessageContext<ServerInfoRequest> context) {
        ServerInfoRequest request = context.getMessage();
        
        // Gather requested information
        Map<String, Object> info = new HashMap<>();
        for (String infoType : request.getRequestedInfo()) {
            switch (infoType) {
                case "players":
                    info.put("players", Bukkit.getOnlinePlayers().size());
                    info.put("max-players", Bukkit.getMaxPlayers());
                    break;
                case "tps":
                    info.put("tps", getCurrentTPS());
                    break;
                case "memory":
                    Runtime runtime = Runtime.getRuntime();
                    info.put("used-memory", runtime.totalMemory() - runtime.freeMemory());
                    info.put("max-memory", runtime.maxMemory());
                    break;
                case "plugins":
                    info.put("plugins", Arrays.stream(Bukkit.getPluginManager().getPlugins())
                        .map(Plugin::getName)
                        .collect(Collectors.toList()));
                    break;
            }
        }
        
        // Send response
        ServerInfoResponse response = new ServerInfoResponse(
            conexus.getServerId(),
            request.getMessageId(),
            info
        );
        
        context.sendResponse(response);
    }
}

// Usage
networkDataService.getServerInfo("lobby-1")
    .thenAccept(response -> {
        getLogger().info("Server lobby-1 has " + response.getInfo().get("players") + " players");
    })
    .exceptionally(throwable -> {
        getLogger().warning("Failed to get server info: " + throwable.getMessage());
        return null;
    });
```

### Circuit Breaker Pattern

Implement resilience patterns for network communication:

```java
public class ResilientMessagingService {
    private final MessagingService messagingService;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    
    public CompletableFuture<Void> sendMessageWithCircuitBreaker(String serverId, Message message) {
        CircuitBreaker circuitBreaker = circuitBreakers.computeIfAbsent(
            serverId, 
            k -> new CircuitBreaker(5, Duration.ofMinutes(1))  // 5 failures, 1 minute timeout
        );
        
        if (circuitBreaker.isOpen()) {
            return CompletableFuture.failedFuture(
                new RuntimeException("Circuit breaker is open for server: " + serverId)
            );
        }
        
        return messagingService.sendToServer(serverId, message)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    circuitBreaker.recordFailure();
                } else {
                    circuitBreaker.recordSuccess();
                }
            });
    }
    
    // Simple circuit breaker implementation
    private static class CircuitBreaker {
        private final int failureThreshold;
        private final Duration timeout;
        private int failureCount = 0;
        private long lastFailureTime = 0;
        private boolean open = false;
        
        public CircuitBreaker(int failureThreshold, Duration timeout) {
            this.failureThreshold = failureThreshold;
            this.timeout = timeout;
        }
        
        public boolean isOpen() {
            if (open && System.currentTimeMillis() - lastFailureTime > timeout.toMillis()) {
                open = false;  // Half-open state
                failureCount = 0;
            }
            return open;
        }
        
        public void recordFailure() {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
            if (failureCount >= failureThreshold) {
                open = true;
            }
        }
        
        public void recordSuccess() {
            failureCount = 0;
            open = false;
        }
    }
}
```

These examples demonstrate the flexibility and power of Conexus for building sophisticated cross-server communication systems. Each pattern can be adapted and extended based on your specific needs.