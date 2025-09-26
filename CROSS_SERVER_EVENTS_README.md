# Cross-Server Event Broadcasting

A production-ready system for broadcasting events across multiple Minecraft servers in real-time.

## Overview

The Cross-Server Event Broadcasting system enables real-time event distribution and handling across multiple Minecraft servers via messaging infrastructure. This allows for:

- **Consistent state propagation** between servers
- **Real-time synchronization** of game events
- **Coordinated behaviors** across server networks
- **Scalable event distribution** with built-in resilience

## Key Features

✅ **Production Ready**
- Jackson-based JSON serialization
- Circuit breaker pattern for resilience
- Retry logic with exponential backoff
- Comprehensive metrics collection
- Graceful degradation support

✅ **Extensible Architecture**
- Plugin-friendly event registry
- Support for custom event types
- Configurable retry and circuit breaker policies
- Multiple transport providers (Redis, in-memory)

✅ **High Performance**
- Async processing throughout
- Efficient serialization/deserialization
- Connection pooling and reuse
- Built-in performance metrics

## Quick Start

### 1. Basic Setup

```java
import com.boredhf.conexus.events.*;
import com.boredhf.conexus.communication.*;

// Create messaging service (Redis-based)
MessagingService messagingService = new DefaultMessagingService(
    "my-server-id", 
    redisTransport, 
    messageSerializer
);

// Create cross-server event service with default configuration
CrossServerEventService eventService = new CrossServerEventService(
    "my-server-id", 
    messagingService
);

// Initialize the service
eventService.initialize().get();
```

### 2. Custom Configuration

```java
// Create custom configuration
CrossServerEventConfiguration config = new CrossServerEventConfiguration("my-server")
    .setEnableCrossServerBroadcast(true)
    .setEnableGracefulDegradation(true)
    .setCircuitBreakerFailureThreshold(5)
    .setCircuitBreakerTimeoutMillis(30000)
    .setMaxRetryAttempts(3)
    .setRetryDelayMillis(1000)
    .setRetryBackoffMultiplier(2.0);

// Create service with custom configuration
CrossServerEventService eventService = new CrossServerEventService(
    "my-server-id", 
    messagingService,
    config
);
```

### 3. Broadcasting Events

```java
// Create and broadcast a server status event
ServerStatusEvent statusEvent = new ServerStatusEvent(
    "my-server-id", 
    ServerStatusEvent.Status.RUNNING, 
    "Server is now online"
);

// Broadcast with default priority
eventService.broadcastEvent(statusEvent)
    .whenComplete((result, throwable) -> {
        if (throwable == null) {
            System.out.println("Event broadcast successful!");
        } else {
            System.err.println("Broadcast failed: " + throwable.getMessage());
        }
    });

// Broadcast with specific priority
eventService.broadcastEvent(statusEvent, EventService.EventPriority.HIGH);
```

### 4. Listening for Events

```java
// Register an event listener
eventService.registerEventListener(ServerStatusEvent.class, event -> {
    System.out.println("Received server status: " + event.getStatus());
    System.out.println("From server: " + event.getSourceServerId());
    System.out.println("Message: " + event.getMessage());
    
    // Handle the event...
    handleServerStatusChange(event);
});
```

## Custom Event Types

### 1. Creating Custom Events

```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerNetworkEvent implements EventService.NetworkEvent {
    
    private final String sourceServerId;
    private final String playerId;
    private final String action;
    private final Instant timestamp;
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public PlayerNetworkEvent(
            @JsonProperty("sourceServerId") String sourceServerId,
            @JsonProperty("playerId") String playerId,
            @JsonProperty("action") String action) {
        this.sourceServerId = sourceServerId;
        this.playerId = playerId;
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
    
    @JsonProperty("action")
    public String getAction() {
        return action;
    }
}
```

### 2. Registering Custom Event Types

```java
// If your event doesn't use Jackson annotations, provide a custom deserializer
eventService.registerEventType(PlayerNetworkEvent.class, jsonString -> {
    // Custom deserialization logic if needed
    return PlayerNetworkEvent.fromJson(jsonString);
});
```

## Configuration Options

### Circuit Breaker Settings

```java
config.setCircuitBreakerFailureThreshold(5)      // Open after 5 failures
      .setCircuitBreakerTimeoutMillis(30000)     // Stay open for 30 seconds
      .setCircuitBreakerName("MyServerEvents");  // Custom name for logging
```

### Retry Settings

```java
config.setMaxRetryAttempts(3)                    // Retry up to 3 times
      .setRetryDelayMillis(1000)                 // Base delay of 1 second
      .setRetryBackoffMultiplier(2.0);           // Double delay each retry
```

### Timeout Settings

```java
config.setEventProcessingTimeoutMillis(10000)   // 10 second processing timeout
      .setNetworkBroadcastTimeoutMillis(5000);   // 5 second network timeout
```

## Monitoring and Metrics

### Accessing Metrics

```java
// Get current metrics snapshot
EventMetrics.MetricsSnapshot metrics = eventService.getMetricsSnapshot();

System.out.println("Events processed: " + metrics.totalEventsProcessed);
System.out.println("Success rate: " + metrics.successRate + "%");
System.out.println("Avg processing time: " + metrics.avgProcessingTimeMs + "ms");
System.out.println("Circuit breaker state: " + metrics.circuitBreakerState);
```

### Logging Metrics

```java
// Log current metrics to logger at INFO level
eventService.logMetrics();
```

### Monitoring Circuit Breaker

```java
CircuitBreaker circuitBreaker = eventService.getNetworkCircuitBreaker();
System.out.println("Circuit breaker state: " + circuitBreaker.getState());
System.out.println("Failure count: " + circuitBreaker.getFailureCount());
```

## Error Handling

### Graceful Degradation

```java
// Enable graceful degradation (continues local processing when network fails)
config.setEnableGracefulDegradation(true);

// Or disable to fail fast on network issues
config.setEnableGracefulDegradation(false);
```

### Exception Handling

```java
eventService.broadcastEvent(myEvent)
    .exceptionally(throwable -> {
        logger.error("Event broadcast failed", throwable);
        
        // Handle specific error types
        if (throwable.getCause() instanceof CircuitBreakerOpenException) {
            // Circuit breaker is open, handle accordingly
        }
        
        return null; // Return appropriate default value
    });
```

## Best Practices

### 1. Event Design

- **Keep events small**: Only include necessary data
- **Make events immutable**: Use final fields and no setters
- **Include timestamps**: For ordering and debugging
- **Add metadata**: Store contextual information

### 2. Error Handling

- **Use graceful degradation**: Don't let network issues break local functionality
- **Monitor circuit breaker**: React to prolonged network issues
- **Log appropriately**: Use structured logging for debugging

### 3. Performance

- **Batch related events**: Avoid sending many small events rapidly
- **Use appropriate priorities**: Reserve HIGH/CRITICAL for important events
- **Monitor metrics**: Track success rates and processing times

### 4. Testing

```java
// Use in-memory transport for tests
InMemoryMessagingService testMessaging = new InMemoryMessagingService("test-server");
CrossServerEventService testService = new CrossServerEventService("test-server", testMessaging);

// Create test configuration with shorter timeouts
CrossServerEventConfiguration testConfig = new CrossServerEventConfiguration("test")
    .setCircuitBreakerTimeoutMillis(1000)  // 1 second for tests
    .setMaxRetryAttempts(1);               // Single retry for faster tests
```

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                   CrossServerEventService                       │
├─────────────────────────────────────────────────────────────────┤
│  • Event broadcasting with retry logic                         │
│  • Circuit breaker for network resilience                      │  
│  • Local and cross-server event handling                       │
│  • Metrics collection and monitoring                           │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   NetworkEventRegistry                          │
├─────────────────────────────────────────────────────────────────┤
│  • Jackson-based JSON serialization                            │
│  • Custom deserializer support                                 │
│  • Event type registration and lookup                          │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MessagingService                             │
├─────────────────────────────────────────────────────────────────┤
│  • Redis transport (production)                                │
│  • In-memory transport (testing)                               │
│  • Message routing and delivery                                │
└─────────────────────────────────────────────────────────────────┘
```

### Event Flow

1. **Event Creation**: Developer creates event implementing `NetworkEvent`
2. **Local Processing**: Event is processed by local listeners first
3. **Serialization**: Event is serialized to JSON using Jackson
4. **Network Transmission**: Event is sent via messaging service with retry logic
5. **Circuit Breaker**: Network failures are tracked and circuit breaker protects against cascading failures
6. **Remote Deserialization**: Receiving servers deserialize the JSON back to event objects
7. **Remote Processing**: Remote servers process the event with their local listeners
8. **Metrics Recording**: Success/failure metrics are recorded throughout the process

## Troubleshooting

### Common Issues

**Events not being received**
- Check network connectivity between servers
- Verify messaging service configuration
- Check circuit breaker state: `eventService.getNetworkCircuitBreaker().getState()`

**Serialization errors**
- Ensure custom events have proper Jackson annotations
- Register custom deserializers for complex types
- Check event registry: `eventService.getEventRegistry().getRegisteredEventTypes()`

**Performance issues**
- Monitor metrics: `eventService.logMetrics()`
- Check for circuit breaker opening frequently
- Verify network latency and Redis performance

### Debug Logging

Enable debug logging to see detailed event flow:

```java
// In logback.xml or log4j2.xml
<logger name="com.boredhf.conexus.events" level="DEBUG"/>
```

## Integration Examples

### Spigot/Paper Plugin

```java
public class MyPlugin extends JavaPlugin {
    
    private CrossServerEventService eventService;
    
    @Override
    public void onEnable() {
        // Initialize messaging service
        MessagingService messaging = createMessagingService();
        
        // Create event service
        eventService = new CrossServerEventService(getServerId(), messaging);
        eventService.initialize();
        
        // Register event listeners
        setupEventListeners();
        
        getLogger().info("Cross-server events enabled!");
    }
    
    @Override
    public void onDisable() {
        if (eventService != null) {
            eventService.shutdown();
        }
    }
    
    private void setupEventListeners() {
        // Listen for player join events from other servers
        eventService.registerEventListener(PlayerNetworkEvent.class, event -> {
            if ("JOIN".equals(event.getAction())) {
                Bukkit.broadcastMessage("§a" + event.getPlayerId() + " joined " + event.getSourceServerId());
            }
        });
        
        // Listen for server status changes
        eventService.registerEventListener(ServerStatusEvent.class, event -> {
            getLogger().info("Server " + event.getSourceServerId() + " is now " + event.getStatus());
        });
    }
}
```

## Support and Contributing

For issues, questions, or contributions, please refer to the project repository.

---

*This documentation covers the production-ready Cross-Server Event Broadcasting system. For basic Conexus library usage, see the main README.*