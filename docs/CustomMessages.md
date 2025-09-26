# Custom Messages

Conexus provides a flexible message system that allows you to create custom message types for your specific cross-server communication needs. This guide covers how to create, register, and use custom messages.

## Table of Contents

- [Overview](#overview)
- [Built-in Message Types](#built-in-message-types)
- [Creating Custom Messages](#creating-custom-messages)
- [Message Serialization](#message-serialization)
- [Registration and Handlers](#registration-and-handlers)
- [Best Practices](#best-practices)
- [Examples](#examples)

## Overview

Conexus uses a type-safe message system where each message implements the `Message` interface. Messages are automatically serialized to JSON for transport and deserialized back to their original types when received.

Key features:
- **Type Safety**: Compile-time message type checking
- **Automatic Serialization**: JSON serialization/deserialization
- **Custom Fields**: Support for complex data structures
- **Versioning**: Built-in support for message versioning
- **Validation**: Automatic message validation

## Built-in Message Types

Conexus includes several built-in message types for common use cases:

### SimpleTextMessage
```java
SimpleTextMessage message = new SimpleTextMessage(
    "server-1",           // source server
    "Hello World!",       // content
    "global"             // category
);
```

### PlayerJoinMessage
```java
PlayerJoinMessage message = new PlayerJoinMessage(
    "lobby-1",                    // source server
    UUID.fromString("..."),       // player UUID
    "PlayerName",                 // player name
    "127.0.0.1"                  // player IP
);
```

### PlayerDataSyncMessage
```java
PlayerDataSyncMessage message = new PlayerDataSyncMessage(
    "survival-1",                 // source server
    UUID.fromString("..."),       // player UUID
    PlayerData.class,             // data type
    playerDataObject              // data payload
);
```

### ModerationActionMessage
```java
ModerationActionMessage message = new ModerationActionMessage(
    "admin-server",               // source server
    ModerationAction.BAN,         // action type
    UUID.fromString("..."),       // target player
    UUID.fromString("..."),       // moderator
    "Cheating",                   // reason
    Duration.ofDays(7)            // duration
);
```

## Creating Custom Messages

### Basic Custom Message

Create a custom message by implementing the `Message` interface:

```java
public class ServerStatusMessage implements Message {
    private String serverId;
    private String status;
    private int playerCount;
    private long timestamp;
    private Map<String, Object> metadata;
    
    // Constructor
    public ServerStatusMessage(String serverId, String status, int playerCount) {
        this.serverId = serverId;
        this.status = status;
        this.playerCount = playerCount;
        this.timestamp = System.currentTimeMillis();
        this.metadata = new HashMap<>();
    }
    
    // Default constructor for deserialization
    public ServerStatusMessage() {}
    
    @Override
    public String getMessageType() {
        return "server_status";
    }
    
    @Override
    public String getSourceServerId() {
        return serverId;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getMessageId() {
        return UUID.randomUUID().toString();
    }
    
    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    // Helper methods
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
}
```

### Advanced Custom Message with Validation

```java
@JsonTypeName("economy_transaction")
public class EconomyTransactionMessage implements Message {
    @JsonProperty("serverId")
    private String serverId;
    
    @JsonProperty("fromPlayer")
    private UUID fromPlayer;
    
    @JsonProperty("toPlayer")
    private UUID toPlayer;
    
    @JsonProperty("amount")
    @JsonDeserialize(using = BigDecimalDeserializer.class)
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("reason")
    private String reason;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    public EconomyTransactionMessage() {}
    
    public EconomyTransactionMessage(String serverId, UUID fromPlayer, UUID toPlayer, 
                                   BigDecimal amount, String currency, String reason) {
        this.serverId = Objects.requireNonNull(serverId, "Server ID cannot be null");
        this.fromPlayer = fromPlayer;
        this.toPlayer = Objects.requireNonNull(toPlayer, "To player cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
        this.transactionId = UUID.randomUUID().toString();
        
        validate();
    }
    
    private void validate() {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency.length() > 10) {
            throw new IllegalArgumentException("Currency code too long");
        }
        if (fromPlayer != null && fromPlayer.equals(toPlayer)) {
            throw new IllegalArgumentException("Cannot transfer to self");
        }
    }
    
    @Override
    public String getMessageType() {
        return "economy_transaction";
    }
    
    @Override
    public String getSourceServerId() {
        return serverId;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getMessageId() {
        return transactionId;
    }
    
    // Getters and setters...
}
```

### Message with Complex Data Structures

```java
public class MinigameResultMessage implements Message {
    private String serverId;
    private String minigameName;
    private String gameId;
    private List<PlayerResult> results;
    private GameStatistics statistics;
    private long timestamp;
    
    public MinigameResultMessage() {}
    
    public MinigameResultMessage(String serverId, String minigameName, String gameId) {
        this.serverId = serverId;
        this.minigameName = minigameName;
        this.gameId = gameId;
        this.results = new ArrayList<>();
        this.statistics = new GameStatistics();
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getMessageType() {
        return "minigame_result";
    }
    
    // ... other overrides ...
    
    public void addPlayerResult(UUID playerId, String playerName, int score, int rank) {
        results.add(new PlayerResult(playerId, playerName, score, rank));
    }
    
    // Nested classes for complex data
    public static class PlayerResult {
        private UUID playerId;
        private String playerName;
        private int score;
        private int rank;
        private Map<String, Object> achievements;
        
        public PlayerResult() {
            this.achievements = new HashMap<>();
        }
        
        public PlayerResult(UUID playerId, String playerName, int score, int rank) {
            this();
            this.playerId = playerId;
            this.playerName = playerName;
            this.score = score;
            this.rank = rank;
        }
        
        // Getters and setters...
    }
    
    public static class GameStatistics {
        private Duration gameDuration;
        private int totalPlayers;
        private String winCondition;
        private Map<String, Integer> eventCounts;
        
        public GameStatistics() {
            this.eventCounts = new HashMap<>();
        }
        
        // Getters and setters...
    }
}
```

## Message Serialization

### Custom Serializers

For complex data types, you might need custom serializers:

```java
public class LocationSerializer extends JsonSerializer<Location> {
    @Override
    public void serialize(Location location, JsonGenerator gen, SerializerProvider serializers) 
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("world", location.getWorld().getName());
        gen.writeNumberField("x", location.getX());
        gen.writeNumberField("y", location.getY());
        gen.writeNumberField("z", location.getZ());
        gen.writeNumberField("yaw", location.getYaw());
        gen.writeNumberField("pitch", location.getPitch());
        gen.writeEndObject();
    }
}

public class LocationDeserializer extends JsonDeserializer<Location> {
    @Override
    public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        String worldName = node.get("world").asText();
        double x = node.get("x").asDouble();
        double y = node.get("y").asDouble();
        double z = node.get("z").asDouble();
        float yaw = (float) node.get("yaw").asDouble();
        float pitch = (float) node.get("pitch").asDouble();
        
        World world = Bukkit.getWorld(worldName);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
```

Register custom serializers with your message:

```java
@JsonSerialize(using = LocationSerializer.class)
@JsonDeserialize(using = LocationDeserializer.class)
private Location location;
```

### Message Versioning

Support multiple message versions:

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "version")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PlayerDataV1.class, name = "v1"),
    @JsonSubTypes.Type(value = PlayerDataV2.class, name = "v2")
})
public abstract class PlayerDataMessage implements Message {
    public abstract int getVersion();
}

public class PlayerDataV2 extends PlayerDataMessage {
    private UUID playerId;
    private String playerName;
    private PlayerStats stats;
    private List<Achievement> achievements; // New in v2
    
    @Override
    public int getVersion() {
        return 2;
    }
    
    @Override
    public String getMessageType() {
        return "player_data_v2";
    }
    
    // Migration from v1
    public static PlayerDataV2 fromV1(PlayerDataV1 v1) {
        PlayerDataV2 v2 = new PlayerDataV2();
        v2.playerId = v1.getPlayerId();
        v2.playerName = v1.getPlayerName();
        v2.stats = v1.getStats();
        v2.achievements = new ArrayList<>(); // Default empty list
        return v2;
    }
}
```

## Registration and Handlers

### Register Message Handlers

```java
// Register a handler for your custom message
messagingService.registerHandler(ServerStatusMessage.class, context -> {
    ServerStatusMessage message = context.getMessage();
    String serverId = message.getSourceServerId();
    String status = message.getStatus();
    int playerCount = message.getPlayerCount();
    
    getLogger().info("Server {} status: {} ({} players)", 
                    serverId, status, playerCount);
    
    // Update server status in local cache
    serverStatusCache.put(serverId, message);
});
```

### Advanced Handler with Filtering

```java
// Handler with filtering and processing
messagingService.registerHandler(EconomyTransactionMessage.class, context -> {
    EconomyTransactionMessage message = context.getMessage();
    
    // Only process transactions involving our server
    if (!isLocalPlayer(message.getToPlayer()) && !isLocalPlayer(message.getFromPlayer())) {
        return; // Skip processing
    }
    
    // Process the transaction
    economyManager.processTransaction(
        message.getFromPlayer(),
        message.getToPlayer(),
        message.getAmount(),
        message.getCurrency(),
        message.getReason()
    );
    
    // Log the transaction
    auditLogger.logTransaction(message);
});
```

### Conditional Message Routing

```java
// Route messages based on content
messagingService.registerHandler(MinigameResultMessage.class, context -> {
    MinigameResultMessage message = context.getMessage();
    
    // Route to appropriate handlers based on minigame type
    switch (message.getMinigameName().toLowerCase()) {
        case "bedwars":
            bedwarsResultHandler.handle(message);
            break;
        case "skywars":
            skywarsResultHandler.handle(message);
            break;
        default:
            genericMinigameHandler.handle(message);
            break;
    }
});
```

## Best Practices

### Message Design

1. **Keep Messages Immutable**: Design messages to be immutable after creation
2. **Use Builder Pattern**: For complex messages with many optional fields
3. **Include Validation**: Validate message data in constructors
4. **Version Your Messages**: Plan for schema evolution
5. **Use Meaningful Types**: Choose appropriate data types for fields

### Performance

1. **Minimize Message Size**: Keep messages as small as possible
2. **Avoid Deep Nesting**: Limit nested object depth
3. **Use Efficient Serialization**: Consider binary formats for high-frequency messages
4. **Pool Objects**: Reuse message objects when possible
5. **Batch Related Messages**: Send multiple related updates in one message

### Error Handling

1. **Graceful Degradation**: Handle missing or invalid fields gracefully
2. **Default Values**: Provide sensible defaults for optional fields
3. **Validation Messages**: Include clear validation error messages
4. **Backward Compatibility**: Maintain compatibility with older message versions

### Security

1. **Input Validation**: Always validate incoming message data
2. **Sensitive Data**: Never include sensitive information in messages
3. **Rate Limiting**: Implement rate limiting for message types
4. **Authentication**: Verify message source when necessary

## Examples

### Complete Custom Message Example

```java
// 1. Define the message
public class QuestCompletionMessage implements Message {
    private String serverId;
    private UUID playerId;
    private String playerName;
    private String questId;
    private String questName;
    private List<QuestReward> rewards;
    private long completionTime;
    private String messageId;
    
    public QuestCompletionMessage() {}
    
    public QuestCompletionMessage(String serverId, UUID playerId, String playerName,
                                String questId, String questName) {
        this.serverId = serverId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.questId = questId;
        this.questName = questName;
        this.rewards = new ArrayList<>();
        this.completionTime = System.currentTimeMillis();
        this.messageId = UUID.randomUUID().toString();
    }
    
    @Override
    public String getMessageType() { return "quest_completion"; }
    
    @Override
    public String getSourceServerId() { return serverId; }
    
    @Override
    public long getTimestamp() { return completionTime; }
    
    @Override
    public String getMessageId() { return messageId; }
    
    // Add reward helper
    public void addReward(String type, int amount, String description) {
        rewards.add(new QuestReward(type, amount, description));
    }
    
    // Nested reward class
    public static class QuestReward {
        private String type;
        private int amount;
        private String description;
        
        public QuestReward() {}
        
        public QuestReward(String type, int amount, String description) {
            this.type = type;
            this.amount = amount;
            this.description = description;
        }
        
        // Getters and setters...
    }
    
    // Getters and setters for all fields...
}

// 2. Register handler
messagingService.registerHandler(QuestCompletionMessage.class, context -> {
    QuestCompletionMessage message = context.getMessage();
    
    // Broadcast achievement to all online players
    String announcement = ChatColor.GREEN + message.getPlayerName() + 
                         " completed quest: " + ChatColor.GOLD + message.getQuestName();
    Bukkit.broadcastMessage(announcement);
    
    // Update global quest statistics
    questStatsManager.recordCompletion(message.getQuestId(), message.getPlayerId());
    
    // Process rewards if player is online locally
    Player player = Bukkit.getPlayer(message.getPlayerId());
    if (player != null && player.isOnline()) {
        for (QuestCompletionMessage.QuestReward reward : message.getRewards()) {
            rewardManager.giveReward(player, reward);
        }
    }
});

// 3. Send the message
QuestCompletionMessage message = new QuestCompletionMessage(
    "survival-1", 
    player.getUniqueId(), 
    player.getName(),
    "dragon_slayer",
    "Dragon Slayer"
);
message.addReward("experience", 1000, "1000 XP");
message.addReward("currency", 500, "$500");
message.addReward("item", 1, "Dragon Egg");

messagingService.broadcast(message);
```

This comprehensive guide should help you create powerful custom messages for your cross-server communication needs!