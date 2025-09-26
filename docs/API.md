# API Documentation

This document provides a comprehensive reference for the Conexus API.

## Table of Contents

- [Core Interfaces](#core-interfaces)
- [Messaging](#messaging)
- [Transport Providers](#transport-providers)
- [Player Data](#player-data)
- [Events](#events)
- [Moderation](#moderation)

## Core Interfaces

### Conexus

The main entry point for the Conexus library.

```java
public interface Conexus {
    MessagingService getMessagingService();
    PlayerDataService getPlayerDataService();
    EventService getEventService();
    ModerationService getModerationService();
    String getServerId();
    TransportProvider getTransportProvider();
    CompletableFuture<Void> initialize();
    CompletableFuture<Void> shutdown();
    boolean isConnected();
}
```

**Example:**
```java
Conexus conexus = new ConexusImpl("server-1", transport, messaging);
conexus.initialize().join();
```

### ConexusImpl

Default implementation of the Conexus interface.

```java
public ConexusImpl(String serverId, TransportProvider transportProvider, MessagingService messagingService)
```

## Messaging

### MessagingService

High-level messaging operations for cross-server communication.

```java
public interface MessagingService {
    MessageChannel getChannel(String channelName);
    <T extends Message> MessageChannel<T> createChannel(String channelName, Class<T> messageType);
    CompletableFuture<Void> sendToServer(String targetServerId, Message message);
    CompletableFuture<Void> broadcast(Message message);
    <T extends Message> CompletableFuture<T> sendRequest(String targetServerId, Message request, Class<T> responseType, long timeoutMs);
    <T extends Message> void registerHandler(Class<T> messageType, Consumer<MessageContext<T>> handler);
    <T extends Message> void unregisterHandler(Class<T> messageType);
    String getServerId();
}
```

#### Key Methods

**`sendToServer(String targetServerId, Message message)`**
- Sends a message to a specific server
- Returns: `CompletableFuture<Void>` that completes when message is sent
- Example: `messagingService.sendToServer("lobby-1", new SimpleTextMessage("Hello!"))`

**`broadcast(Message message)`**
- Broadcasts a message to all connected servers
- Returns: `CompletableFuture<Void>` that completes when message is broadcast
- Example: `messagingService.broadcast(new SimpleTextMessage("Global announcement!"))`

**`registerHandler(Class<T> messageType, Consumer<MessageContext<T>> handler)`**
- Registers a handler for a specific message type
- Handlers receive all messages of that type from all servers
- Example:
```java
messagingService.registerHandler(SimpleTextMessage.class, context -> {
    SimpleTextMessage msg = context.getMessage();
    Bukkit.broadcastMessage("[" + msg.getSourceServerId() + "] " + msg.getContent());
});
```

### MessageChannel

Type-safe pub/sub communication channel.

```java
public interface MessageChannel<T extends Message> {
    String getName();
    Class<T> getMessageType();
    CompletableFuture<Void> publish(T message);
    CompletableFuture<Void> subscribe(Consumer<MessageContext<T>> handler);
    CompletableFuture<Void> unsubscribe();
    boolean isSubscribed();
}
```

**Example:**
```java
MessageChannel<SimpleTextMessage> globalChat = messagingService.createChannel("global-chat", SimpleTextMessage.class);

// Subscribe to messages
globalChat.subscribe(context -> {
    SimpleTextMessage msg = context.getMessage();
    // Handle message
});

// Publish a message
globalChat.publish(new SimpleTextMessage("server-1", "Hello world!", "chat"));
```

### Message Types

#### Message

Base interface for all messages.

```java
public interface Message {
    UUID getMessageId();
    Instant getTimestamp();
    String getSourceServerId();
    String getMessageType();
}
```

#### BaseMessage

Abstract base class providing common functionality.

```java
public abstract class BaseMessage implements Message {
    protected BaseMessage(String sourceServerId);
    // Metadata automatically generated
}
```

#### SimpleTextMessage

Basic text message implementation.

```java
public class SimpleTextMessage extends BaseMessage {
    public SimpleTextMessage(String sourceServerId, String content, String category);
    public String getContent();
    public String getCategory();
}
```

### MessageContext

Context information for received messages.

```java
public interface MessageContext<T extends Message> {
    T getMessage();
    String getChannelName();
    boolean expectsResponse();
    CompletableFuture<Void> sendResponse(Message response);
    CompletableFuture<Void> acknowledge();
}
```

## Transport Providers

### TransportProvider

Abstract interface for communication backends.

```java
public interface TransportProvider {
    CompletableFuture<Void> connect();
    CompletableFuture<Void> disconnect();
    boolean isConnected();
    CompletableFuture<Void> publish(String channel, byte[] message);
    CompletableFuture<Void> subscribe(String channel, Consumer<byte[]> messageHandler);
    CompletableFuture<Void> unsubscribe(String channel);
    CompletableFuture<Void> store(String key, byte[] data);
    CompletableFuture<Void> store(String key, byte[] data, long ttlMillis);
    CompletableFuture<byte[]> retrieve(String key);
    CompletableFuture<Void> delete(String key);
    CompletableFuture<Boolean> exists(String key);
    String getName();
}
```

### RedisTransportProvider

Redis implementation of TransportProvider.

```java
public class RedisTransportProvider implements TransportProvider {
    public RedisTransportProvider(String host, int port);
    public RedisTransportProvider(String host, int port, String password, int database);
}
```

**Example:**
```java
// Basic Redis connection
RedisTransportProvider redis = new RedisTransportProvider("localhost", 6379);

// Redis with authentication and database
RedisTransportProvider redis = new RedisTransportProvider("redis.example.com", 6379, "password123", 1);
```

## Player Data

### PlayerDataService

Service for cross-server player data synchronization.

```java
public interface PlayerDataService {
    <T extends PlayerData> CompletableFuture<T> getPlayerData(UUID playerId, Class<T> dataType);
    <T extends PlayerData> CompletableFuture<Void> setPlayerData(UUID playerId, T data);
    <T extends PlayerData> CompletableFuture<T> updatePlayerData(UUID playerId, Class<T> dataType, DataModifier<T> modifier);
    <T extends PlayerData> CompletableFuture<Void> deletePlayerData(UUID playerId, Class<T> dataType);
    <T extends PlayerData> CompletableFuture<Boolean> hasPlayerData(UUID playerId, Class<T> dataType);
    PlayerDataContainer getPlayerContainer(UUID playerId);
    <T extends PlayerData> void addDataChangeListener(Class<T> dataType, Consumer<PlayerDataChangeEvent<T>> listener);
    <T extends PlayerData> void removeDataChangeListener(Class<T> dataType, Consumer<PlayerDataChangeEvent<T>> listener);
    CompletableFuture<Void> syncPlayerData(UUID playerId);
    CompletableFuture<Void> clearPlayerCache(UUID playerId);
}
```

### PlayerData

Base interface for player data types.

```java
public interface PlayerData {
    int getVersion();
    Instant getLastModified();
    String getLastModifiedBy();
    String getDataType();
    PlayerData withUpdatedMetadata(String serverId);
}
```

**Example Custom Player Data:**
```java
public class PlayerEconomy implements PlayerData {
    private final double balance;
    private final int version;
    private final Instant lastModified;
    private final String lastModifiedBy;
    
    public PlayerEconomy(double balance, String serverId) {
        this.balance = balance;
        this.version = 1;
        this.lastModified = Instant.now();
        this.lastModifiedBy = serverId;
    }
    
    // Implement required methods...
    
    public double getBalance() { return balance; }
    public PlayerEconomy setBalance(double newBalance) {
        return new PlayerEconomy(newBalance, lastModifiedBy);
    }
}
```

## Events

### EventService

Service for cross-server event broadcasting (planned feature).

```java
public interface EventService {
    // Will be implemented in future version
}
```

## Moderation

### ModerationService

Service for network-wide moderation actions (planned feature).

```java
public interface ModerationService {
    // Will be implemented in future version
}
```

## Error Handling

### Common Exceptions

**`MessageSerializationException`**
- Thrown when message serialization/deserialization fails
- Usually indicates incompatible message formats or corrupted data

**`ConnectionException`** 
- Thrown when transport connection fails
- Check transport provider configuration and network connectivity

**`TimeoutException`**
- Thrown when request/response operations timeout
- Consider increasing timeout values or checking network latency

### Best Practices

1. **Always handle CompletableFuture exceptions:**
   ```java
   conexus.getMessagingService().broadcast(message)
       .exceptionally(throwable -> {
           logger.error("Failed to broadcast message", throwable);
           return null;
       });
   ```

2. **Use appropriate timeout values:**
   ```java
   // For local network: 1-5 seconds
   // For internet: 10-30 seconds
   CompletableFuture<ResponseMessage> response = messagingService.sendRequest(
       "target-server", request, ResponseMessage.class, 5000L
   );
   ```

3. **Register handlers early:**
   ```java
   // Register handlers during plugin initialization
   @Override
   public void onEnable() {
       // Initialize Conexus
       // Register all message handlers
       // Then connect
       conexus.initialize();
   }
   ```

4. **Clean shutdown:**
   ```java
   @Override
   public void onDisable() {
       if (conexus != null) {
           conexus.shutdown().join();
       }
   }
   ```

## Thread Safety

- All Conexus APIs are thread-safe
- CompletableFuture operations can be called from any thread
- Message handlers are called asynchronously and should handle synchronization if needed
- When interacting with Bukkit API from handlers, use `Bukkit.getScheduler().runTask()`

## Performance Considerations

- Message serialization has overhead - avoid sending large objects frequently
- Use appropriate message batching for high-frequency updates
- Consider message TTL for non-critical data
- Monitor Redis memory usage with frequent data updates
- Use connection pooling for high-throughput applications