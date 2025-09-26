# Plugin Integration Guide

This guide shows how to integrate Conexus into your existing Bukkit/Paper plugins.

## Table of Contents

- [Adding Conexus as a Dependency](#adding-conexus-as-a-dependency)
- [Basic Integration](#basic-integration)
- [Advanced Integration](#advanced-integration)
- [Plugin.yml Configuration](#plugin-yml-configuration)
- [Best Practices](#best-practices)
- [Common Patterns](#common-patterns)
- [Migration Guide](#migration-guide)

## Adding Conexus as a Dependency

### Maven Dependency

Add Conexus to your plugin's `pom.xml`:

```xml
<dependencies>
    <!-- Your existing dependencies -->
    
    <!-- Conexus Core Library -->
    <dependency>
        <groupId>com.boredhf</groupId>
        <artifactId>conexus-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>provided</scope> <!-- Provided by the server or as a separate plugin -->
    </dependency>
    
    <!-- Spigot API -->
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.20.4-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle Dependency

Add Conexus to your plugin's `build.gradle`:

```gradle
dependencies {
    // Your existing dependencies
    
    // Conexus Core Library
    compileOnly 'com.boredhf:conexus-core:1.0.0-SNAPSHOT'
    
    // Spigot API
    compileOnly 'org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT'
}
```

## Basic Integration

### Simple Plugin Integration

Here's how to add Conexus to an existing plugin:

```java
public class MyPlugin extends JavaPlugin {
    private Conexus conexus;
    private MyPluginMessaging messaging;
    
    @Override
    public void onEnable() {
        // Check if Conexus plugin is available
        Plugin conexusPlugin = getServer().getPluginManager().getPlugin("Conexus");
        if (conexusPlugin == null) {
            getLogger().severe("Conexus plugin not found! Please install Conexus.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get Conexus instance from the plugin
        if (conexusPlugin instanceof ConexusPlugin) {
            this.conexus = ((ConexusPlugin) conexusPlugin).getConexus();
            setupCrossServerFeatures();
        }
    }
    
    private void setupCrossServerFeatures() {
        // Initialize your messaging system
        this.messaging = new MyPluginMessaging(this, conexus);
        messaging.initialize();
        
        getLogger().info("Cross-server features enabled!");
    }
    
    @Override
    public void onDisable() {
        if (messaging != null) {
            messaging.shutdown();
        }
    }
}
```

### Dedicated Messaging Class

Create a separate class to handle all cross-server communication:

```java
public class MyPluginMessaging {
    private final MyPlugin plugin;
    private final Conexus conexus;
    
    public MyPluginMessaging(MyPlugin plugin, Conexus conexus) {
        this.plugin = plugin;
        this.conexus = conexus;
    }
    
    public void initialize() {
        // Register message handlers
        conexus.getMessagingService().registerHandler(MyCustomMessage.class, this::handleCustomMessage);
        conexus.getMessagingService().registerHandler(PlayerDataSyncMessage.class, this::handlePlayerDataSync);
        
        plugin.getLogger().info("Cross-server messaging initialized");
    }
    
    public void shutdown() {
        // Unregister handlers
        conexus.getMessagingService().unregisterHandler(MyCustomMessage.class);
        conexus.getMessagingService().unregisterHandler(PlayerDataSyncMessage.class);
        
        plugin.getLogger().info("Cross-server messaging shutdown");
    }
    
    private void handleCustomMessage(MessageContext<MyCustomMessage> context) {
        MyCustomMessage message = context.getMessage();
        
        // Handle the message on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            processCustomMessage(message);
        });
    }
    
    private void handlePlayerDataSync(MessageContext<PlayerDataSyncMessage> context) {
        PlayerDataSyncMessage message = context.getMessage();
        
        // Process player data synchronization
        plugin.getPlayerDataManager().handleRemoteUpdate(message);
    }
    
    private void processCustomMessage(MyCustomMessage message) {
        // Your custom logic here
        plugin.getLogger().info("Received custom message from " + message.getSourceServerId());
    }
}
```

## Advanced Integration

### Standalone Library Integration

For more control, integrate Conexus directly without depending on the plugin:

```java
public class MyAdvancedPlugin extends JavaPlugin {
    private Conexus conexus;
    
    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        
        try {
            initializeConexus();
            setupPluginFeatures();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize cross-server communication: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void initializeConexus() {
        // Read Redis configuration from your plugin's config
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        int redisDatabase = getConfig().getInt("redis.database", 0);
        String serverId = getConfig().getString("server-id", "unknown");
        
        // Create transport provider
        RedisTransportProvider transport = new RedisTransportProvider(
            redisHost, 
            redisPort, 
            redisPassword.isEmpty() ? null : redisPassword, 
            redisDatabase
        );
        
        // Create messaging service
        MessageSerializer serializer = new MessageSerializer();
        DefaultMessagingService messaging = new DefaultMessagingService(serverId, transport, serializer);
        
        // Create Conexus instance
        conexus = new ConexusImpl(serverId, transport, messaging);
        
        // Initialize asynchronously
        conexus.initialize()
            .thenRun(() -> getLogger().info("Cross-server communication initialized"))
            .exceptionally(throwable -> {
                getLogger().severe("Failed to connect to Redis: " + throwable.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return null;
            });
    }
    
    private void setupPluginFeatures() {
        // Register your custom message handlers
        conexus.getMessagingService().registerHandler(MyPluginMessage.class, this::handlePluginMessage);
        
        // Start your plugin's specific services
        startEconomySync();
        startPlayerDataSync();
        startEventBroadcasting();
    }
    
    @Override
    public void onDisable() {
        if (conexus != null) {
            conexus.shutdown().join();
        }
    }
}
```

### Service-Based Architecture

For larger plugins, use a service-based approach:

```java
public class MyPluginServices {
    private final MyPlugin plugin;
    private final Conexus conexus;
    private final Map<String, PluginService> services = new HashMap<>();
    
    public MyPluginServices(MyPlugin plugin, Conexus conexus) {
        this.plugin = plugin;
        this.conexus = conexus;
        initializeServices();
    }
    
    private void initializeServices() {
        // Register different services
        registerService("economy", new EconomyService(plugin, conexus));
        registerService("playerdata", new PlayerDataService(plugin, conexus));
        registerService("messaging", new MessagingService(plugin, conexus));
        registerService("moderation", new ModerationService(plugin, conexus));
    }
    
    private void registerService(String name, PluginService service) {
        services.put(name, service);
        service.initialize();
        plugin.getLogger().info("Registered service: " + name);
    }
    
    public <T extends PluginService> T getService(String name, Class<T> type) {
        PluginService service = services.get(name);
        if (type.isInstance(service)) {
            return type.cast(service);
        }
        return null;
    }
    
    public void shutdown() {
        services.values().forEach(PluginService::shutdown);
        services.clear();
    }
}

// Base service interface
public interface PluginService {
    void initialize();
    void shutdown();
}

// Example economy service
public class EconomyService implements PluginService {
    private final MyPlugin plugin;
    private final Conexus conexus;
    
    public EconomyService(MyPlugin plugin, Conexus conexus) {
        this.plugin = plugin;
        this.conexus = conexus;
    }
    
    @Override
    public void initialize() {
        conexus.getMessagingService().registerHandler(EconomyUpdateMessage.class, this::handleEconomyUpdate);
    }
    
    @Override
    public void shutdown() {
        conexus.getMessagingService().unregisterHandler(EconomyUpdateMessage.class);
    }
    
    public void broadcastBalanceUpdate(UUID playerId, double newBalance) {
        EconomyUpdateMessage message = new EconomyUpdateMessage(
            conexus.getServerId(),
            playerId,
            newBalance
        );
        
        conexus.getMessagingService().broadcast(message);
    }
    
    private void handleEconomyUpdate(MessageContext<EconomyUpdateMessage> context) {
        // Handle economy updates from other servers
        EconomyUpdateMessage message = context.getMessage();
        
        // Update local economy data
        plugin.getEconomy().updatePlayerBalance(message.getPlayerId(), message.getNewBalance());
    }
}
```

## Plugin.yml Configuration

### Soft Dependency

If Conexus is optional for your plugin:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: '1.20'
softdepend: [Conexus]
description: My awesome plugin with optional cross-server features

commands:
  myplugin:
    description: Main plugin command
    usage: /myplugin <args>
```

### Hard Dependency

If your plugin requires Conexus:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: '1.20'
depend: [Conexus]
description: My awesome plugin with cross-server features

commands:
  myplugin:
    description: Main plugin command
    usage: /myplugin <args>
```

### Load Order

If you need to load after Conexus but before other plugins:

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.myplugin.MyPlugin
api-version: '1.20'
loadbefore: [OtherPlugin]
depend: [Conexus]
load: POSTWORLD  # Load after world generation
```

## Best Practices

### 1. Error Handling

Always handle errors gracefully:

```java
public void sendCrossServerMessage(MyMessage message) {
    conexus.getMessagingService().broadcast(message)
        .thenRun(() -> {
            plugin.getLogger().fine("Message sent successfully");
        })
        .exceptionally(throwable -> {
            plugin.getLogger().warning("Failed to send cross-server message: " + throwable.getMessage());
            // Fallback to local-only behavior
            handleMessageLocally(message);
            return null;
        });
}
```

### 2. Thread Safety

Always switch to the main thread for Bukkit API calls:

```java
private void handleCrossServerMessage(MessageContext<MyMessage> context) {
    MyMessage message = context.getMessage();
    
    // Switch to main thread for Bukkit API access
    Bukkit.getScheduler().runTask(plugin, () -> {
        Player player = Bukkit.getPlayer(message.getPlayerId());
        if (player != null) {
            player.sendMessage("Cross-server update: " + message.getContent());
        }
    });
}
```

### 3. Configuration Integration

Integrate Conexus configuration with your plugin's config:

```yaml
# config.yml for your plugin
myplugin:
  cross-server:
    enabled: true
    features:
      chat: true
      economy: true
      player-data: false
  
# Use these settings to conditionally enable features
cross-server:
  redis:
    host: "127.0.0.1"
    port: 6379
    database: 1  # Use different database than main Conexus
```

```java
@Override
public void onEnable() {
    if (!getConfig().getBoolean("myplugin.cross-server.enabled", false)) {
        getLogger().info("Cross-server features disabled in configuration");
        return;
    }
    
    // Initialize based on feature flags
    if (getConfig().getBoolean("myplugin.cross-server.features.chat", true)) {
        enableCrossServerChat();
    }
    
    if (getConfig().getBoolean("myplugin.cross-server.features.economy", true)) {
        enableCrossServerEconomy();
    }
}
```

### 4. Graceful Degradation

Design your plugin to work even if cross-server features fail:

```java
public class MyPluginFeature {
    private final boolean crossServerEnabled;
    
    public MyPluginFeature(MyPlugin plugin, Conexus conexus) {
        this.crossServerEnabled = (conexus != null && conexus.isConnected());
        
        if (!crossServerEnabled) {
            plugin.getLogger().warning("Cross-server features unavailable - running in local mode");
        }
    }
    
    public void performAction(Player player, String data) {
        if (crossServerEnabled) {
            // Send to all servers
            broadcastAction(player, data);
        } else {
            // Local-only action
            performLocalAction(player, data);
        }
    }
}
```

## Common Patterns

### 1. Plugin Hook Pattern

Create a hook system for other plugins to integrate:

```java
public class MyPluginAPI {
    private static MyPlugin instance;
    private static Conexus conexus;
    
    public static void initialize(MyPlugin plugin, Conexus conexus) {
        MyPluginAPI.instance = plugin;
        MyPluginAPI.conexus = conexus;
    }
    
    /**
     * Send a cross-server message through MyPlugin
     */
    public static CompletableFuture<Void> sendCrossServerMessage(String category, String message) {
        if (conexus == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Cross-server not available"));
        }
        
        MyPluginMessage msg = new MyPluginMessage(conexus.getServerId(), category, message);
        return conexus.getMessagingService().broadcast(msg);
    }
    
    /**
     * Check if cross-server features are available
     */
    public static boolean isCrossServerAvailable() {
        return conexus != null && conexus.isConnected();
    }
}
```

### 2. Event Bridge Pattern

Bridge Bukkit events to cross-server events:

```java
public class EventBridge implements Listener {
    private final Conexus conexus;
    
    public EventBridge(MyPlugin plugin, Conexus conexus) {
        this.conexus = conexus;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Broadcast join event to other servers
        PlayerJoinMessage message = new PlayerJoinMessage(
            conexus.getServerId(),
            event.getPlayer().getUniqueId(),
            event.getPlayer().getName()
        );
        
        conexus.getMessagingService().broadcast(message);
    }
    
    // Handle cross-server join events
    public void handleRemotePlayerJoin(MessageContext<PlayerJoinMessage> context) {
        PlayerJoinMessage message = context.getMessage();
        
        // Fire custom event for other plugins to handle
        CrossServerPlayerJoinEvent event = new CrossServerPlayerJoinEvent(
            message.getPlayerId(),
            message.getPlayerName(),
            message.getSourceServerId()
        );
        
        Bukkit.getPluginManager().callEvent(event);
    }
}
```

### 3. Data Synchronization Pattern

Synchronize plugin data across servers:

```java
public class DataSyncManager {
    private final LoadingCache<String, CompletableFuture<String>> pendingRequests;
    
    public DataSyncManager(MyPlugin plugin, Conexus conexus) {
        this.pendingRequests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .build(key -> new CompletableFuture<>());
            
        conexus.getMessagingService().registerHandler(DataRequestMessage.class, this::handleDataRequest);
        conexus.getMessagingService().registerHandler(DataResponseMessage.class, this::handleDataResponse);
    }
    
    public CompletableFuture<String> requestDataFromServer(String serverId, String dataKey) {
        String requestId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        
        pendingRequests.put(requestId, future);
        
        DataRequestMessage request = new DataRequestMessage(
            conexus.getServerId(),
            requestId,
            dataKey
        );
        
        conexus.getMessagingService().sendToServer(serverId, request)
            .exceptionally(throwable -> {
                future.completeExceptionally(throwable);
                pendingRequests.invalidate(requestId);
                return null;
            });
        
        return future;
    }
}
```

## Migration Guide

### From BungeeCord Plugin Messaging

If you're migrating from BungeeCord plugin messaging:

```java
// Old BungeeCord way
public void sendPluginMessage(String channel, byte[] data) {
    if (player.isOnline()) {
        player.sendPluginMessage(plugin, channel, data);
    }
}

// New Conexus way
public void sendCrossServerMessage(MyMessage message) {
    conexus.getMessagingService().broadcast(message);
}
```

### From Redis Pub/Sub

If you're migrating from direct Redis usage:

```java
// Old direct Redis way
public void publishMessage(String channel, String message) {
    try (Jedis jedis = jedisPool.getResource()) {
        jedis.publish(channel, message);
    }
}

// New Conexus way
public void sendMessage(MyMessage message) {
    MessageChannel<MyMessage> channel = conexus.getMessagingService()
        .createChannel("my-channel", MyMessage.class);
    channel.publish(message);
}
```

### Configuration Migration

```yaml
# Old direct Redis config
redis:
  host: "localhost"
  port: 6379
  password: "secret"

# New Conexus integration
cross-server:
  enabled: true
  # Conexus handles Redis connection
  features:
    my-feature: true
```

This integration approach ensures your plugin works seamlessly with Conexus while maintaining clean separation of concerns and proper error handling.