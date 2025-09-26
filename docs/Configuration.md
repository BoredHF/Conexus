# Configuration Guide

This guide covers all configuration options for Conexus.

## Table of Contents

- [Plugin Configuration](#plugin-configuration)
- [Redis Configuration](#redis-configuration)
- [Programmatic Configuration](#programmatic-configuration)
- [Environment Variables](#environment-variables)
- [Advanced Configuration](#advanced-configuration)

## Plugin Configuration

When using Conexus as a plugin, configuration is handled via `config.yml` in your server's `plugins/Conexus/` directory.

### Default Configuration

```yaml
# Conexus Configuration
# Cross-server communication library for Minecraft

# Server identifier - must be unique across all servers in the network
server-id: "lobby-1"

# Redis connection settings
redis:
  host: "127.0.0.1"
  port: 6379
  password: "" # Leave empty if no password
  database: 0
  
# Plugin settings
plugin:
  # Whether to log debug messages
  debug: false
  
  # Whether to broadcast join/leave messages across servers
  broadcast-joins: true
  
  # Whether to enable the demo /cxsay command
  enable-demo-command: true
```

### Configuration Options

#### Server Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `server-id` | String | "lobby-1" | Unique identifier for this server. Must be unique across your network. |

**Examples:**
- `"lobby-1"` - Main lobby server
- `"survival-1"` - First survival server  
- `"creative"` - Creative server
- `"proxy"` - Proxy server

#### Redis Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `redis.host` | String | "127.0.0.1" | Redis server hostname or IP address |
| `redis.port` | Integer | 6379 | Redis server port |
| `redis.password` | String | "" | Redis password (leave empty if no auth) |
| `redis.database` | Integer | 0 | Redis database number (0-15) |

**Redis Cluster Configuration:**
```yaml
redis:
  # For Redis Cluster, use comma-separated host:port pairs
  host: "redis1.example.com:6379,redis2.example.com:6379,redis3.example.com:6379"
  # Or use environment variable
  host: "${REDIS_CLUSTER_NODES}"
```

#### Plugin Settings

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `plugin.debug` | Boolean | false | Enable debug logging for troubleshooting |
| `plugin.broadcast-joins` | Boolean | true | Broadcast player join/leave messages across servers |
| `plugin.enable-demo-command` | Boolean | true | Enable the `/cxsay` demo command |

### Advanced Configuration

```yaml
# Advanced Redis settings
redis:
  host: "127.0.0.1"
  port: 6379
  password: ""
  database: 0
  
  # Connection pool settings
  pool:
    max-total: 20          # Maximum connections in pool
    max-idle: 10           # Maximum idle connections
    min-idle: 5            # Minimum idle connections
    max-wait-millis: 2000  # Maximum wait time for connection
    
  # Connection timeouts
  timeout:
    connect: 2000          # Connection timeout (ms)
    socket: 2000           # Socket timeout (ms)
    
  # Retry settings
  retry:
    attempts: 3            # Number of retry attempts
    delay: 1000           # Delay between retries (ms)

# Messaging settings
messaging:
  # Default timeout for request/response operations (ms)
  default-timeout: 5000
  
  # Message serialization settings
  serialization:
    pretty-print: false    # Pretty print JSON (for debugging)
    include-type-info: true # Include @class property in JSON
    
# Performance tuning
performance:
  # Thread pool settings
  thread-pool:
    core-size: 2           # Core thread pool size
    max-size: 10           # Maximum thread pool size
    queue-size: 100        # Task queue size
    
  # Caching settings
  cache:
    enabled: true          # Enable local caching
    max-size: 1000         # Maximum cache entries
    ttl-seconds: 300       # Cache entry TTL
```

## Redis Configuration

### Basic Setup

For a simple single-server Redis setup:

```yaml
redis:
  host: "localhost"
  port: 6379
```

### Production Setup

For production environments with authentication:

```yaml
redis:
  host: "redis.yournetwork.com"
  port: 6379
  password: "${REDIS_PASSWORD}"  # Use environment variable
  database: 1                    # Use dedicated database
```

### Redis Cluster

For Redis Cluster setups:

```yaml
redis:
  # Comma-separated list of cluster nodes
  host: "redis1:6379,redis2:6379,redis3:6379"
  # Cluster-specific settings
  cluster:
    enabled: true
    max-redirections: 3
```

### Redis Sentinel

For Redis Sentinel (high availability):

```yaml
redis:
  sentinel:
    enabled: true
    master-name: "mymaster"
    nodes: "sentinel1:26379,sentinel2:26379,sentinel3:26379"
    password: "${SENTINEL_PASSWORD}"
```

### SSL/TLS Configuration

For secure Redis connections:

```yaml
redis:
  host: "redis.yournetwork.com"
  port: 6380
  ssl:
    enabled: true
    verify-peer: true
    ca-cert-file: "/path/to/ca-cert.pem"
    cert-file: "/path/to/client-cert.pem"
    key-file: "/path/to/client-key.pem"
```

## Programmatic Configuration

When using Conexus as a library, you configure it programmatically:

### Basic Setup

```java
// Create transport provider
RedisTransportProvider transport = new RedisTransportProvider("localhost", 6379);

// Create services
MessageSerializer serializer = new MessageSerializer();
DefaultMessagingService messaging = new DefaultMessagingService("server-1", transport, serializer);

// Create Conexus instance
Conexus conexus = new ConexusImpl("server-1", transport, messaging);
```

### Advanced Configuration

```java
// Create transport with custom configuration
RedisTransportProvider.Builder transportBuilder = RedisTransportProvider.builder()
    .host("redis.example.com")
    .port(6379)
    .password("secure-password")
    .database(1)
    .maxConnections(20)
    .connectionTimeout(Duration.ofSeconds(2))
    .socketTimeout(Duration.ofSeconds(2));
    
RedisTransportProvider transport = transportBuilder.build();

// Create messaging service with custom settings
MessagingServiceConfig messagingConfig = MessagingServiceConfig.builder()
    .defaultTimeout(Duration.ofSeconds(5))
    .enableDebugLogging(false)
    .threadPoolSize(4)
    .build();

DefaultMessagingService messaging = new DefaultMessagingService(
    "server-1", 
    transport, 
    new MessageSerializer(),
    messagingConfig
);
```

## Environment Variables

Conexus supports environment variable substitution in configuration files:

### Common Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `CONEXUS_SERVER_ID` | Server identifier | `export CONEXUS_SERVER_ID="lobby-1"` |
| `REDIS_HOST` | Redis hostname | `export REDIS_HOST="redis.example.com"` |
| `REDIS_PORT` | Redis port | `export REDIS_PORT="6379"` |
| `REDIS_PASSWORD` | Redis password | `export REDIS_PASSWORD="secure123"` |
| `REDIS_DATABASE` | Redis database | `export REDIS_DATABASE="1"` |

### Usage in Configuration

```yaml
server-id: "${CONEXUS_SERVER_ID:lobby-1}"  # Default to "lobby-1" if not set

redis:
  host: "${REDIS_HOST:127.0.0.1}"
  port: "${REDIS_PORT:6379}"
  password: "${REDIS_PASSWORD:}"
  database: "${REDIS_DATABASE:0}"
```

### Docker Environment

Example Docker environment file (`.env`):

```bash
# Server configuration
CONEXUS_SERVER_ID=lobby-1

# Redis configuration
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=mysecurepassword
REDIS_DATABASE=0

# Feature flags
ENABLE_DEBUG=false
BROADCAST_JOINS=true
ENABLE_DEMO_COMMAND=true
```

## Advanced Configuration

### Message Routing

Configure custom message routing rules:

```yaml
messaging:
  routing:
    # Route messages by server groups
    groups:
      survival:
        servers: ["survival-1", "survival-2", "survival-3"]
        channels: ["survival-chat", "survival-events"]
      creative:
        servers: ["creative-1", "creative-2"]
        channels: ["creative-chat", "creative-events"]
      
    # Global channels available to all servers
    global-channels: ["announcements", "staff-chat"]
```

### Rate Limiting

Configure rate limiting to prevent message spam:

```yaml
messaging:
  rate-limiting:
    enabled: true
    
    # Per-server limits
    server:
      messages-per-second: 100
      burst-size: 200
      
    # Per-channel limits
    channel:
      messages-per-second: 50
      burst-size: 100
      
    # Per-message-type limits
    message-types:
      SimpleTextMessage:
        messages-per-second: 20
        burst-size: 50
```

### Monitoring and Metrics

Configure monitoring and metrics collection:

```yaml
monitoring:
  enabled: true
  
  # Metrics providers
  providers:
    - type: "prometheus"
      port: 9090
      path: "/metrics"
    - type: "influxdb"
      url: "http://influxdb:8086"
      database: "conexus"
      
  # What to monitor
  metrics:
    - "message.sent.count"
    - "message.received.count"
    - "message.processing.time"
    - "connection.status"
    - "redis.operations.count"
```

### Logging Configuration

Configure detailed logging:

```yaml
logging:
  level: INFO
  
  # Log different components at different levels
  loggers:
    "com.boredhf.conexus.transport": DEBUG
    "com.boredhf.conexus.messaging": INFO
    "com.boredhf.conexus.data": WARN
    
  # Output configuration
  output:
    console:
      enabled: true
      pattern: "%d{HH:mm:ss} [%level] [%thread] %logger{36} - %msg%n"
    file:
      enabled: true
      path: "logs/conexus.log"
      max-size: "10MB"
      max-files: 5
```

## Validation and Testing

### Configuration Validation

Conexus automatically validates your configuration on startup. Common validation errors:

- **Invalid server-id**: Must be non-empty and unique
- **Invalid Redis connection**: Host/port must be reachable
- **Invalid database number**: Must be 0-15 for standard Redis
- **Missing required fields**: All required fields must be present

### Testing Configuration

Test your configuration before deploying:

```bash
# Test Redis connectivity
redis-cli -h your-redis-host -p 6379 ping

# Test from within Minecraft server
/conexus test-connection

# Check configuration validity
/conexus config validate
```

### Configuration Templates

#### Single Server Development

```yaml
server-id: "dev-server"
redis:
  host: "127.0.0.1"
  port: 6379
plugin:
  debug: true
  broadcast-joins: true
  enable-demo-command: true
```

#### Multi-Server Network

```yaml
# lobby-1 server
server-id: "lobby-1"
redis:
  host: "${REDIS_HOST}"
  port: 6379
  password: "${REDIS_PASSWORD}"
  database: 0
plugin:
  debug: false
  broadcast-joins: true
  enable-demo-command: false
```

#### High-Performance Setup

```yaml
server-id: "${SERVER_ID}"
redis:
  host: "${REDIS_HOST}"
  port: 6379
  password: "${REDIS_PASSWORD}"
  database: 0
  pool:
    max-total: 50
    max-idle: 20
    min-idle: 10
performance:
  thread-pool:
    core-size: 4
    max-size: 20
  cache:
    enabled: true
    max-size: 5000
```

## Troubleshooting Configuration

### Common Issues

1. **Connection Refused**
   ```
   Solution: Check Redis host/port, ensure Redis is running
   ```

2. **Authentication Failed**
   ```
   Solution: Verify Redis password, check environment variables
   ```

3. **Database Not Found**
   ```
   Solution: Ensure database number is valid (0-15)
   ```

4. **Server ID Conflicts**
   ```
   Solution: Ensure each server has a unique server-id
   ```

### Debug Mode

Enable debug mode for detailed logging:

```yaml
plugin:
  debug: true
```

This will show:
- Connection attempts and results
- Message sending/receiving details
- Configuration parsing information
- Performance metrics