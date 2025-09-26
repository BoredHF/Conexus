# Migration Guide: Cross-Server Event Broadcasting

This guide helps you migrate from the basic cross-server event broadcasting system to the new **production-ready** version with circuit breaker, retry logic, metrics, and enhanced reliability.

## What's New

✅ **Production Features Added**
- Circuit breaker pattern for network resilience
- Retry logic with exponential backoff
- Comprehensive metrics and monitoring
- Graceful degradation support
- JSON serialization with Jackson
- Configurable timeout and retry policies
- Enhanced error handling and logging

## Breaking Changes

### 1. Event Serialization 

**Before (String-based):**
```java
// Old toString() serialization - fragile and error-prone
public String toString() {
    return sourceServerId + "|" + status + "|" + message;
}
```

**After (Jackson JSON):**
```java
// New Jackson-based JSON serialization - robust and type-safe
@JsonCreator
public ServerStatusEvent(
        @JsonProperty("sourceServerId") String sourceServerId,
        @JsonProperty("status") Status status,
        @JsonProperty("message") String message) {
    // Constructor implementation
}
```

**Migration:** Add Jackson annotations to your custom event classes.

### 2. Service Configuration

**Before (Basic):**
```java
CrossServerEventService eventService = new CrossServerEventService("server-1", messagingService);
```

**After (Configurable):**
```java
// Create configuration for your environment
CrossServerEventConfiguration config = new CrossServerEventConfiguration("server-1")
    .setEnableCrossServerBroadcast(true)
    .setEnableGracefulDegradation(true)
    .setCircuitBreakerFailureThreshold(5)
    .setMaxRetryAttempts(3);

CrossServerEventService eventService = new CrossServerEventService("server-1", messagingService, config);
```

**Migration:** Update service creation to use configuration objects.

### 3. Error Handling

**Before (Basic):**
```java
eventService.broadcastEvent(event); // Fire and forget
```

**After (Robust):**
```java
eventService.broadcastEvent(event)
    .whenComplete((result, throwable) -> {
        if (throwable != null) {
            logger.warn("Event broadcast failed: {}", throwable.getMessage());
            // Handle failure appropriately
        }
    });
```

**Migration:** Add proper error handling to your event broadcasts.

## Step-by-Step Migration

### Step 1: Update Dependencies

Ensure you have the latest version with Jackson support:

```xml
<dependency>
    <groupId>com.boredhf</groupId>
    <artifactId>conexus-core</artifactId>
    <version>1.0.0-SNAPSHOT</version> <!-- Latest version -->
</dependency>
```

### Step 2: Add Jackson Annotations to Custom Events

**Example: Migrating a Custom Event**

```java
// BEFORE: Basic event class
public class PlayerJoinEvent implements EventService.NetworkEvent {
    private String sourceServerId;
    private String playerName;
    
    // Basic constructor and getters
    
    @Override
    public String toString() {
        return sourceServerId + "|" + playerName; // Fragile!
    }
}

// AFTER: Jackson-annotated event class
public class PlayerJoinEvent implements EventService.NetworkEvent {
    private final String sourceServerId;
    private final String playerName;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public PlayerJoinEvent(
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("playerName") String playerName) {
        this.sourceServerId = sourceServerId;
        this.playerName = playerName;
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
    
    @JsonProperty("playerName")
    public String getPlayerName() {
        return playerName;
    }
}
```

### Step 3: Update Service Initialization

**Before:**
```java
public class MyPlugin extends JavaPlugin {
    private CrossServerEventService eventService;
    
    @Override
    public void onEnable() {
        MessagingService messaging = createMessagingService();
        eventService = new CrossServerEventService(getServerId(), messaging);
        eventService.initialize();
    }
}
```

**After:**
```java
public class MyPlugin extends JavaPlugin {
    private CrossServerEventService eventService;
    
    @Override
    public void onEnable() {
        MessagingService messaging = createMessagingService();
        
        // Create production configuration
        CrossServerEventConfiguration config = new CrossServerEventConfiguration(getServerId())
            .setEnableCrossServerBroadcast(true)
            .setEnableGracefulDegradation(true)
            .setCircuitBreakerFailureThreshold(5)
            .setCircuitBreakerTimeoutMillis(30000)
            .setMaxRetryAttempts(3)
            .setRetryDelayMillis(1000)
            .setRetryBackoffMultiplier(2.0);
            
        eventService = new CrossServerEventService(getServerId(), messaging, config);
        eventService.initialize().whenComplete((result, throwable) -> {
            if (throwable != null) {
                getLogger().severe("Failed to initialize event service: " + throwable.getMessage());
            } else {
                getLogger().info("Event service initialized successfully");
            }
        });
    }
    
    @Override
    public void onDisable() {
        if (eventService != null) {
            eventService.shutdown().join(); // Wait for graceful shutdown
        }
    }
}
```

### Step 4: Add Proper Error Handling

**Before:**
```java
eventService.broadcastEvent(event); // No error handling
```

**After:**
```java
eventService.broadcastEvent(event, EventService.EventPriority.NORMAL)
    .whenComplete((result, throwable) -> {
        if (throwable != null) {
            if (throwable.getCause() instanceof CircuitBreakerOpenException) {
                logger.debug("Circuit breaker is open, event not sent");
            } else {
                logger.warn("Failed to broadcast event: {}", throwable.getMessage());
            }
        } else {
            logger.debug("Event broadcast successful");
        }
    });
```

### Step 5: Add Monitoring (Optional)

```java
// Log metrics periodically
Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
    eventService.logMetrics();
}, 0L, 20 * 60L); // Every minute

// Or get metrics programmatically
EventMetrics.MetricsSnapshot metrics = eventService.getMetricsSnapshot();
getLogger().info("Event success rate: {}%", metrics.successRate);
```

## Configuration Migration

### Environment-Specific Configs

Replace hard-coded values with environment-appropriate configurations:

**Development:**
```java
CrossServerEventConfiguration devConfig = new CrossServerEventConfiguration(serverId)
    .setCircuitBreakerFailureThreshold(2)     // Fail fast
    .setCircuitBreakerTimeoutMillis(5000)     // Quick recovery
    .setMaxRetryAttempts(1);                  // Minimal retries
```

**Production:**
```java
CrossServerEventConfiguration prodConfig = new CrossServerEventConfiguration(serverId)
    .setCircuitBreakerFailureThreshold(10)    // More tolerant
    .setCircuitBreakerTimeoutMillis(60000)    // Longer recovery
    .setMaxRetryAttempts(5);                  // More retries
```

## Testing Your Migration

### 1. Unit Tests

```java
@Test
public void testEventSerialization() {
    PlayerJoinEvent event = new PlayerJoinEvent("server-1", "TestPlayer");
    
    // Test Jackson serialization
    NetworkEventRegistry registry = new NetworkEventRegistry();
    String json = registry.serializeEvent(event);
    PlayerJoinEvent deserialized = registry.deserializeEvent(json, PlayerJoinEvent.class);
    
    assertEquals(event.getPlayerName(), deserialized.getPlayerName());
    assertEquals(event.getSourceServerId(), deserialized.getSourceServerId());
}
```

### 2. Integration Tests

```java
@Test
public void testEventBroadcastWithRetry() {
    // Use test configuration with fast timeouts
    CrossServerEventConfiguration testConfig = new CrossServerEventConfiguration("test-server")
        .setMaxRetryAttempts(2)
        .setRetryDelayMillis(100)
        .setCircuitBreakerTimeoutMillis(1000);
        
    CrossServerEventService eventService = new CrossServerEventService(
        "test-server", messagingService, testConfig);
        
    // Test event broadcasting
    PlayerJoinEvent event = new PlayerJoinEvent("test-server", "TestPlayer");
    CompletableFuture<Void> future = eventService.broadcastEvent(event);
    
    assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
}
```

## Rollback Plan

If you need to rollback, you can:

1. **Disable new features** while keeping the updated code:
```java
CrossServerEventConfiguration rollbackConfig = new CrossServerEventConfiguration(serverId)
    .setMaxRetryAttempts(0)              // Disable retries
    .setEnableGracefulDegradation(false) // Disable graceful degradation
    .setCircuitBreakerFailureThreshold(Integer.MAX_VALUE); // Effectively disable circuit breaker
```

2. **Use legacy mode** (if implemented):
```java
eventService.setLegacyMode(true); // Fall back to old behavior
```

3. **Revert to previous version** and restore your old event classes.

## Need Help?

- Check the [Cross-Server Events Documentation](CROSS_SERVER_EVENTS_README.md)
- Review the [examples](examples/) directory
- Open an issue on GitHub if you encounter problems

## Performance Notes

The new system is **more performant** than the old one:

- ✅ JSON serialization is faster than string manipulation
- ✅ Circuit breaker prevents cascading failures
- ✅ Retry logic reduces message loss
- ✅ Metrics help identify performance bottlenecks
- ✅ Graceful degradation maintains service availability

---

*This migration guide covers upgrading to the production-ready cross-server event broadcasting system. Follow the steps carefully and test thoroughly before deploying to production.*