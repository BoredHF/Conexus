# Transport Providers

Transport providers are the backbone of Conexus's cross-server communication system. They handle the actual message delivery between servers, allowing you to choose the best communication method for your infrastructure.

## Table of Contents

- [Overview](#overview)
- [Available Providers](#available-providers)
- [Creating Custom Providers](#creating-custom-providers)
- [Configuration](#configuration)
- [Performance Considerations](#performance-considerations)

## Overview

Conexus uses a pluggable transport provider system that allows you to switch between different communication backends without changing your application code. Each provider implements the `TransportProvider` interface and handles:

- Message serialization/deserialization
- Connection management
- Error handling and reconnection
- Channel subscription/publishing

## Available Providers

### Redis Provider (Recommended)

The Redis provider uses Redis Pub/Sub for message delivery, offering excellent performance and reliability.

**Advantages:**
- ✅ High performance and low latency
- ✅ Built-in clustering support
- ✅ Persistent connections
- ✅ Excellent reliability
- ✅ Message ordering guarantees
- ✅ Pattern-based subscriptions

**Use Cases:**
- High-throughput server networks
- Real-time communication requirements
- Networks requiring message ordering
- Production environments

**Configuration:**
```yaml
transport:
  provider: "redis"
  redis:
    host: "127.0.0.1"
    port: 6379
    password: ""
    database: 0
    timeout: 2000
    ssl: false
    cluster:
      enabled: false
      nodes: []
    connection-pool:
      max-total: 8
      max-idle: 8
      min-idle: 0
```

**Example Usage:**
```java
RedisTransportProvider provider = new RedisTransportProvider(
    RedisConfig.builder()
        .host("localhost")
        .port(6379)
        .database(0)
        .connectionPoolConfig(
            ConnectionPoolConfig.builder()
                .maxTotal(10)
                .maxIdle(5)
                .build()
        )
        .build()
);
```

### RabbitMQ Provider (Coming Soon)

The RabbitMQ provider will use AMQP for enterprise-grade message queuing.

**Planned Advantages:**
- ✅ Enterprise-grade message queuing
- ✅ Message persistence and durability
- ✅ Complex routing capabilities
- ✅ Message acknowledgments
- ✅ Dead letter queues
- ✅ High availability clustering

**Use Cases:**
- Enterprise environments
- Networks requiring guaranteed delivery
- Complex message routing requirements
- Critical systems requiring message persistence

### TCP Provider (Coming Soon)

The TCP provider will use direct TCP connections for simple deployments.

**Planned Advantages:**
- ✅ No external dependencies
- ✅ Simple configuration
- ✅ Direct peer-to-peer communication
- ✅ Low resource usage

**Use Cases:**
- Small server networks
- Simple deployments
- Development environments
- Networks without external infrastructure

### HTTP/REST Provider (Coming Soon)

The HTTP provider will use REST APIs for maximum compatibility.

**Planned Advantages:**
- ✅ Universal compatibility
- ✅ Firewall-friendly
- ✅ Load balancer support
- ✅ Easy debugging and monitoring

**Use Cases:**
- Mixed infrastructure environments
- Networks with strict firewall policies
- Integration with existing REST APIs
- Development and testing

## Creating Custom Providers

You can create custom transport providers by implementing the `TransportProvider` interface:

```java
public interface TransportProvider {
    /**
     * Initialize the transport provider
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Send a message to the specified channel
     */
    CompletableFuture<Void> send(String channel, String message);
    
    /**
     * Subscribe to a channel
     */
    CompletableFuture<Void> subscribe(String channel, MessageHandler handler);
    
    /**
     * Unsubscribe from a channel
     */
    CompletableFuture<Void> unsubscribe(String channel);
    
    /**
     * Check if the provider is connected
     */
    boolean isConnected();
    
    /**
     * Close the provider and clean up resources
     */
    CompletableFuture<Void> close();
}
```

### Custom Provider Example

```java
public class CustomTransportProvider implements TransportProvider {
    private final CustomConfig config;
    private CustomClient client;
    private final Map<String, MessageHandler> subscriptions = new ConcurrentHashMap<>();
    
    public CustomTransportProvider(CustomConfig config) {
        this.config = config;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            client = new CustomClient(config);
            client.connect();
            
            // Set up message listeners
            client.onMessage((channel, message) -> {
                MessageHandler handler = subscriptions.get(channel);
                if (handler != null) {
                    handler.handle(channel, message);
                }
            });
        });
    }
    
    @Override
    public CompletableFuture<Void> send(String channel, String message) {
        return CompletableFuture.runAsync(() -> {
            if (!isConnected()) {
                throw new RuntimeException("Not connected");
            }
            client.publish(channel, message);
        });
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String channel, MessageHandler handler) {
        return CompletableFuture.runAsync(() -> {
            subscriptions.put(channel, handler);
            client.subscribe(channel);
        });
    }
    
    @Override
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
    
    @Override
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            if (client != null) {
                client.disconnect();
                subscriptions.clear();
            }
        });
    }
}
```

### Provider Registration

Register your custom provider with Conexus:

```java
// Create your custom provider
CustomTransportProvider customProvider = new CustomTransportProvider(config);

// Use it with Conexus
Conexus conexus = ConexusBuilder.builder()
    .serverId("my-server")
    .transportProvider(customProvider)
    .build();
```

## Configuration

### Provider Selection

Choose your transport provider in the configuration:

```yaml
transport:
  provider: "redis"  # redis, rabbitmq, tcp, http, or custom
  # Provider-specific configuration follows
```

### Connection Pooling

Most providers support connection pooling for better performance:

```yaml
transport:
  redis:
    connection-pool:
      max-total: 8      # Maximum number of connections
      max-idle: 8       # Maximum idle connections
      min-idle: 0       # Minimum idle connections
      max-wait: -1      # Max wait time for connection
      test-on-borrow: true
      test-on-return: false
      test-while-idle: true
```

### Retry and Failover

Configure retry behavior and failover options:

```yaml
transport:
  retry:
    enabled: true
    max-attempts: 3
    initial-delay: 1000    # milliseconds
    max-delay: 30000       # milliseconds
    backoff-multiplier: 2.0
  
  failover:
    enabled: true
    health-check-interval: 30000  # milliseconds
    failback-delay: 60000         # milliseconds
```

## Performance Considerations

### Provider Performance Comparison

| Provider | Latency | Throughput | Resource Usage | Complexity |
|----------|---------|------------|----------------|------------|
| Redis    | Very Low | Very High | Low | Low |
| RabbitMQ | Low | High | Medium | Medium |
| TCP      | Very Low | Medium | Very Low | Low |
| HTTP     | Medium | Low | Low | Low |

### Optimization Tips

1. **Connection Pooling**: Use connection pools for better resource management
2. **Message Batching**: Batch multiple messages when possible
3. **Compression**: Enable compression for large messages
4. **Channel Strategy**: Use specific channels instead of wildcards
5. **Monitoring**: Monitor provider health and performance metrics

### Resource Usage

Monitor these metrics for optimal performance:

- **Connection Count**: Keep within reasonable limits
- **Memory Usage**: Watch for message queue buildup
- **Network Bandwidth**: Monitor message throughput
- **CPU Usage**: Check serialization/deserialization overhead

### Troubleshooting

Common issues and solutions:

1. **Connection Failures**: Check network connectivity and credentials
2. **Message Loss**: Verify provider reliability guarantees
3. **High Latency**: Review network topology and provider configuration
4. **Memory Leaks**: Ensure proper subscription cleanup
5. **Thread Exhaustion**: Configure appropriate thread pools

For more detailed troubleshooting, see the [Troubleshooting Guide](Troubleshooting.md).