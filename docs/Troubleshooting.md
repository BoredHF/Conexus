# Troubleshooting Guide

This guide covers common issues you might encounter when using Conexus and their solutions.

## Table of Contents

- [Connection Issues](#connection-issues)
- [Message Delivery Problems](#message-delivery-problems)
- [Performance Issues](#performance-issues)
- [Configuration Problems](#configuration-problems)
- [Development and Testing](#development-and-testing)
- [Logging and Debugging](#logging-and-debugging)
- [Common Error Messages](#common-error-messages)

## Connection Issues

### Redis Connection Failed

**Symptoms:**
- "Connection refused" errors
- "Unable to connect to Redis" messages
- Services not starting

**Solutions:**

1. **Check Redis Server Status**
   ```bash
   # Check if Redis is running
   redis-cli ping
   # Should return "PONG"
   
   # Check Redis logs
   sudo journalctl -u redis
   ```

2. **Verify Connection Details**
   ```yaml
   redis:
     host: "127.0.0.1"  # Correct IP address
     port: 6379         # Correct port
     password: ""       # Include if password is set
     database: 0        # Correct database number
   ```

3. **Network Connectivity**
   ```bash
   # Test network connectivity
   telnet 127.0.0.1 6379
   
   # Check firewall settings
   sudo ufw status
   ```

4. **Redis Configuration**
   ```bash
   # Check Redis config
   redis-cli config get bind
   redis-cli config get protected-mode
   
   # If needed, update Redis config
   sudo nano /etc/redis/redis.conf
   ```

### Connection Pool Exhausted

**Symptoms:**
- "Could not get a resource from the pool" errors
- Timeouts during high load
- Connection refused after initial success

**Solutions:**

1. **Increase Pool Size**
   ```yaml
   redis:
     connection-pool:
       max-total: 20     # Increase from default 8
       max-idle: 10      # Increase from default 8
       min-idle: 2       # Maintain minimum connections
   ```

2. **Connection Leak Detection**
   ```java
   // Enable connection monitoring
   RedisConfig config = RedisConfig.builder()
       .host("localhost")
       .port(6379)
       .connectionPoolConfig(
           ConnectionPoolConfig.builder()
               .maxTotal(20)
               .testOnBorrow(true)
               .testWhileIdle(true)
               .timeBetweenEvictionRuns(30000)
               .build()
       )
       .build();
   ```

3. **Check for Connection Leaks**
   - Ensure all connections are properly closed
   - Use try-with-resources for connection management
   - Monitor connection metrics

### Intermittent Connection Drops

**Symptoms:**
- Sporadic message delivery failures
- "Connection reset by peer" errors
- Services reconnecting frequently

**Solutions:**

1. **Enable Connection Keepalive**
   ```yaml
   redis:
     timeout: 5000
     socket-timeout: 3000
     connection-pool:
       test-while-idle: true
       time-between-eviction-runs: 30000
   ```

2. **Network Stability**
   - Check network hardware
   - Monitor network latency
   - Consider using Redis Sentinel for high availability

3. **Redis Memory Management**
   ```bash
   # Check Redis memory usage
   redis-cli info memory
   
   # Configure appropriate maxmemory policy
   redis-cli config set maxmemory-policy allkeys-lru
   ```

## Message Delivery Problems

### Messages Not Being Received

**Symptoms:**
- Handlers not being called
- Silent message delivery failures
- One-way communication issues

**Diagnostic Steps:**

1. **Check Channel Subscription**
   ```java
   // Verify channels are properly subscribed
   logger.info("Subscribed channels: {}", messagingService.getSubscribedChannels());
   ```

2. **Verify Message Serialization**
   ```java
   // Test message serialization
   String json = messageSerializer.serialize(message);
   Message deserialized = messageSerializer.deserialize(json, MessageType.class);
   ```

3. **Check Redis Pub/Sub**
   ```bash
   # Monitor Redis pub/sub in real-time
   redis-cli monitor
   
   # Check active channels
   redis-cli pubsub channels
   ```

**Solutions:**

1. **Handler Registration Issues**
   ```java
   // Ensure handlers are registered before initialization
   messagingService.registerHandler(MyMessage.class, handler);
   conexus.initialize().join(); // Initialize after registration
   ```

2. **Channel Name Mismatches**
   ```java
   // Use consistent channel naming
   public static final String CHANNEL_GLOBAL = "conexus:global";
   messagingService.subscribe(CHANNEL_GLOBAL);
   ```

3. **Message Type Issues**
   ```java
   // Ensure message types are properly configured
   @JsonTypeName("my_message")
   public class MyMessage implements Message {
       // Implementation
   }
   ```

### Message Duplication

**Symptoms:**
- Same message received multiple times
- Duplicate event processing
- Inconsistent state across servers

**Solutions:**

1. **Implement Idempotency**
   ```java
   private final Set<String> processedMessages = ConcurrentHashMap.newKeySet();
   
   messagingService.registerHandler(MyMessage.class, context -> {
       String messageId = context.getMessage().getMessageId();
       if (processedMessages.add(messageId)) {
           // Process message only once
           processMessage(context.getMessage());
       }
   });
   ```

2. **Message Deduplication**
   ```java
   // Use TTL cache for message IDs
   private final Cache<String, Boolean> messageCache = Caffeine.newBuilder()
       .expireAfterWrite(Duration.ofMinutes(5))
       .maximumSize(10000)
       .build();
   ```

3. **Check Subscription Logic**
   - Ensure channels aren't subscribed multiple times
   - Verify proper cleanup on shutdown/restart

### Message Ordering Issues

**Symptoms:**
- Messages processed out of order
- State inconsistencies
- Race conditions

**Solutions:**

1. **Use Message Sequencing**
   ```java
   public class SequencedMessage implements Message {
       private long sequenceNumber;
       private String sequenceGroup;
       
       // Implement sequence-based processing
   }
   ```

2. **Single-Threaded Processing**
   ```java
   // Process messages sequentially for specific entities
   private final Map<String, Queue<Message>> messageQueues = new ConcurrentHashMap<>();
   private final ExecutorService sequentialExecutor = Executors.newSingleThreadExecutor();
   ```

## Performance Issues

### High Latency

**Symptoms:**
- Slow message delivery
- Timeouts
- Poor user experience

**Diagnostic Tools:**

1. **Measure Message Latency**
   ```java
   public class LatencyTracker {
       private static final Logger logger = LoggerFactory.getLogger(LatencyTracker.class);
       
       public void trackMessage(Message message) {
           long sendTime = message.getTimestamp();
           long receiveTime = System.currentTimeMillis();
           long latency = receiveTime - sendTime;
           
           logger.info("Message latency: {}ms for type {}", 
                      latency, message.getMessageType());
       }
   }
   ```

2. **Monitor Redis Performance**
   ```bash
   # Redis latency monitoring
   redis-cli --latency
   redis-cli --latency-history
   
   # Redis slow log
   redis-cli slowlog get 10
   ```

**Solutions:**

1. **Optimize Message Size**
   ```java
   // Minimize message payload
   public class OptimizedMessage implements Message {
       // Use primitive types where possible
       private int count;      // instead of Integer
       private boolean flag;   // instead of Boolean
       
       // Exclude unnecessary fields from serialization
       @JsonIgnore
       private transient Object heavyObject;
   }
   ```

2. **Connection Pool Tuning**
   ```yaml
   redis:
     connection-pool:
       max-total: 20
       max-idle: 10
       min-idle: 5
       max-wait: 1000
   ```

3. **Batch Processing**
   ```java
   // Batch related messages
   private final List<Message> messageBatch = new ArrayList<>();
   private final ScheduledExecutorService batchProcessor = 
       Executors.newScheduledThreadPool(1);
   
   // Process batches every 100ms
   batchProcessor.scheduleAtFixedRate(this::processBatch, 100, 100, MILLISECONDS);
   ```

### High Memory Usage

**Symptoms:**
- OutOfMemoryError exceptions
- Gradual memory increase
- GC pressure

**Solutions:**

1. **Message Queue Management**
   ```java
   // Limit queue sizes
   private final BlockingQueue<Message> messageQueue = 
       new ArrayBlockingQueue<>(1000);
   
   // Implement backpressure
   if (!messageQueue.offer(message, 1, TimeUnit.SECONDS)) {
       logger.warn("Message queue full, dropping message: {}", message.getMessageType());
   }
   ```

2. **Cache Configuration**
   ```java
   // Configure cache eviction
   private final Cache<String, Object> cache = Caffeine.newBuilder()
       .maximumSize(10_000)
       .expireAfterWrite(Duration.ofMinutes(30))
       .removalListener((key, value, cause) -> 
           logger.debug("Cache entry removed: {} ({})", key, cause))
       .build();
   ```

3. **Memory Leak Detection**
   ```java
   // Monitor object creation
   @Override
   public void finalize() {
       logger.debug("Object finalized: {}", this.getClass().getSimpleName());
   }
   ```

### CPU Usage Spikes

**Symptoms:**
- High CPU utilization
- Thread contention
- Slow response times

**Solutions:**

1. **Thread Pool Optimization**
   ```java
   // Configure appropriate thread pools
   private final ThreadPoolExecutor messageProcessor = new ThreadPoolExecutor(
       4,                              // Core threads
       16,                             // Max threads
       60L, TimeUnit.SECONDS,          // Keep alive
       new LinkedBlockingQueue<>(100), // Queue size
       new ThreadFactoryBuilder()
           .setNameFormat("message-processor-%d")
           .build()
   );
   ```

2. **Reduce Serialization Overhead**
   ```java
   // Cache serialized messages
   private final Cache<Object, String> serializationCache = 
       Caffeine.newBuilder()
           .maximumSize(1000)
           .expireAfterWrite(Duration.ofMinutes(1))
           .build();
   ```

## Configuration Problems

### Invalid Configuration

**Symptoms:**
- Services failing to start
- Configuration parsing errors
- Default values being used unexpectedly

**Solutions:**

1. **Validate Configuration**
   ```java
   // Implement configuration validation
   public class ConfigValidator {
       public void validate(Config config) {
           Objects.requireNonNull(config.getServerId(), "Server ID must be specified");
           
           if (config.getRedis().getHost().isEmpty()) {
               throw new IllegalArgumentException("Redis host cannot be empty");
           }
           
           if (config.getRedis().getPort() < 1 || config.getRedis().getPort() > 65535) {
               throw new IllegalArgumentException("Invalid Redis port: " + config.getRedis().getPort());
           }
       }
   }
   ```

2. **Configuration Schema**
   ```yaml
   # Example complete configuration
   server-id: "lobby-1"
   
   redis:
     host: "127.0.0.1"
     port: 6379
     password: ""
     database: 0
     timeout: 5000
     connection-pool:
       max-total: 8
       max-idle: 8
       min-idle: 0
   
   messaging:
     default-channel: "conexus:global"
     message-timeout: 30000
   
   logging:
     level: "INFO"
     file: "conexus.log"
   ```

### Environment-Specific Issues

**Solutions:**

1. **Environment Detection**
   ```java
   public class EnvironmentConfig {
       public static boolean isProduction() {
           return "production".equals(System.getProperty("environment"));
       }
       
       public static boolean isDevelopment() {
           return "development".equals(System.getProperty("environment"));
       }
   }
   ```

2. **Configuration Profiles**
   ```yaml
   # config-development.yml
   redis:
     host: "localhost"
     port: 6379
   
   # config-production.yml
   redis:
     host: "redis.example.com"
     port: 6380
     password: "${REDIS_PASSWORD}"
   ```

## Development and Testing

### Unit Testing Issues

**Solutions:**

1. **Mock Transport Provider**
   ```java
   @ExtendWith(MockitoExtension.class)
   class MessagingServiceTest {
       @Mock
       private TransportProvider transportProvider;
       
       @Test
       void testMessageBroadcast() {
           // Test implementation
           when(transportProvider.isConnected()).thenReturn(true);
           
           messagingService.broadcast(testMessage);
           
           verify(transportProvider).send(eq("conexus:global"), any());
       }
   }
   ```

2. **Integration Test Setup**
   ```java
   @Testcontainers
   class IntegrationTest {
       @Container
       static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
           .withExposedPorts(6379);
       
       @Test
       void testRealRedisIntegration() {
           String redisHost = redis.getHost();
           Integer redisPort = redis.getMappedPort(6379);
           
           // Test with real Redis instance
       }
   }
   ```

## Logging and Debugging

### Enable Debug Logging

1. **Configuration**
   ```yaml
   logging:
     level:
       com.boredhf.conexus: DEBUG
       redis.clients.jedis: INFO
   ```

2. **Programmatic Logging**
   ```java
   public class ConexusLogger {
       private static final Logger logger = LoggerFactory.getLogger(ConexusLogger.class);
       
       public static void enableDebugLogging() {
           LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
           Logger conexusLogger = context.getLogger("com.boredhf.conexus");
           conexusLogger.setLevel(Level.DEBUG);
       }
   }
   ```

### Message Tracing

```java
public class MessageTracer {
    private static final Logger logger = LoggerFactory.getLogger(MessageTracer.class);
    
    public void traceMessage(Message message, String operation) {
        logger.debug("Message {} - {} - Type: {}, Source: {}, ID: {}", 
                    operation,
                    message.getClass().getSimpleName(),
                    message.getMessageType(),
                    message.getSourceServerId(),
                    message.getMessageId());
    }
}
```

## Common Error Messages

### "Message handler not found"

**Cause:** Handler not registered for message type

**Solution:**
```java
// Register handler before initialization
messagingService.registerHandler(MyMessage.class, handler);
```

### "Serialization failed"

**Cause:** Message class not properly configured for JSON serialization

**Solution:**
```java
// Ensure proper constructors and annotations
public class MyMessage implements Message {
    // Default constructor required
    public MyMessage() {}
    
    // Use proper JSON annotations
    @JsonProperty("customField")
    private String customField;
}
```

### "Connection timeout"

**Cause:** Network issues or Redis overload

**Solution:**
```yaml
redis:
  timeout: 10000        # Increase timeout
  socket-timeout: 5000  # Socket-level timeout
```

### "Channel subscription failed"

**Cause:** Connection issues or invalid channel names

**Solution:**
```java
// Validate channel names
private void validateChannelName(String channel) {
    if (channel == null || channel.trim().isEmpty()) {
        throw new IllegalArgumentException("Channel name cannot be null or empty");
    }
}
```

### "Memory leak detected"

**Cause:** Improper resource cleanup

**Solution:**
```java
// Implement proper cleanup
@Override
public void close() {
    messageHandlers.clear();
    connectionPool.close();
    executorService.shutdown();
}
```

---

For additional help:
- Check the [GitHub Issues](https://github.com/BoredHF/Conexus/issues)
- Join our [Discussions](https://github.com/BoredHF/Conexus/discussions)
- Review the [API Documentation](API.md)