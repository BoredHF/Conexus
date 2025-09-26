package com.boredhf.conexus.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Redis implementation of TransportProvider.
 * 
 * Uses Redis for both pub/sub messaging and data storage.
 * Recommended for production use due to Redis's reliability and performance.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class RedisTransportProvider implements TransportProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisTransportProvider.class);
    
    private final String host;
    private final int port;
    private final String password;
    private final int database;
    
    private JedisPool jedisPool;
    private JedisPubSub pubSub;
    private ExecutorService executorService;
    private final ConcurrentHashMap<String, Consumer<byte[]>> subscriptions = new ConcurrentHashMap<>();
    
    private volatile boolean connected = false;
    
    /**
     * Creates a new Redis transport provider.
     * 
     * @param host Redis host
     * @param port Redis port
     * @param password Redis password (null if no auth)
     * @param database Redis database number
     */
    public RedisTransportProvider(String host, int port, String password, int database) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.database = database;
    }
    
    /**
     * Creates a new Redis transport provider with default settings.
     * 
     * @param host Redis host
     * @param port Redis port
     */
    public RedisTransportProvider(String host, int port) {
        this(host, port, null, 0);
    }
    
    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                JedisPoolConfig config = new JedisPoolConfig();
                config.setMaxTotal(20);
                config.setMaxIdle(10);
                config.setMinIdle(5);
                config.setTestOnBorrow(true);
                config.setTestOnReturn(true);
                config.setTestWhileIdle(true);
                
                if (password != null && !password.isEmpty()) {
                    jedisPool = new JedisPool(config, host, port, 2000, password, database);
                } else {
                    jedisPool = new JedisPool(config, host, port, 2000);
                }
                
                // Test connection
                try (var jedis = jedisPool.getResource()) {
                    jedis.ping();
                }
                
                executorService = Executors.newCachedThreadPool(r -> {
                    Thread t = new Thread(r, "Conexus-Redis-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                });
                
                setupPubSub();
                connected = true;
                
                logger.info("Connected to Redis at {}:{} (database: {})", host, port, database);
                
            } catch (Exception e) {
                connected = false;
                logger.error("Failed to connect to Redis", e);
                throw new RuntimeException("Failed to connect to Redis", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            try {
                connected = false;
                
                if (pubSub != null && pubSub.isSubscribed()) {
                    try {
                        pubSub.unsubscribe();
                    } catch (Exception e) {
                        // Ignore disconnection errors during shutdown
                        logger.debug("Redis unsubscribe failed during disconnect (expected): {}", e.getMessage());
                    }
                }
                
                if (executorService != null) {
                    executorService.shutdown();
                }
                
                if (jedisPool != null) {
                    jedisPool.close();
                }
                
                logger.info("Disconnected from Redis");
                
            } catch (Exception e) {
                logger.error("Error during Redis disconnection", e);
                // Don't throw the error in disconnect - just log it
            }
        });
    }
    
    @Override
    public boolean isConnected() {
        return connected && jedisPool != null && !jedisPool.isClosed();
    }
    
    @Override
    public CompletableFuture<Void> publish(String channel, byte[] message) {
        return CompletableFuture.runAsync(() -> {
            try (var jedis = jedisPool.getResource()) {
                jedis.publish(channel, new String(message));
            } catch (Exception e) {
                logger.error("Failed to publish message to channel: {}", channel, e);
                throw new RuntimeException("Failed to publish message", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> subscribe(String channel, Consumer<byte[]> messageHandler) {
        return CompletableFuture.runAsync(() -> {
            subscriptions.put(channel, messageHandler);
            logger.debug("Added subscription for channel: {}", channel);
        });
    }
    
    @Override
    public CompletableFuture<Void> unsubscribe(String channel) {
        return CompletableFuture.runAsync(() -> {
            subscriptions.remove(channel);
            
            if (pubSub != null && pubSub.isSubscribed()) {
                pubSub.unsubscribe(channel);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> store(String key, byte[] data) {
        return CompletableFuture.runAsync(() -> {
            try (var jedis = jedisPool.getResource()) {
                jedis.set(key.getBytes(), data);
            } catch (Exception e) {
                logger.error("Failed to store data for key: {}", key, e);
                throw new RuntimeException("Failed to store data", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> store(String key, byte[] data, long ttlMillis) {
        return CompletableFuture.runAsync(() -> {
            try (var jedis = jedisPool.getResource()) {
                jedis.psetex(key.getBytes(), ttlMillis, data);
            } catch (Exception e) {
                logger.error("Failed to store data with TTL for key: {}", key, e);
                throw new RuntimeException("Failed to store data with TTL", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<byte[]> retrieve(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (var jedis = jedisPool.getResource()) {
                return jedis.get(key.getBytes());
            } catch (Exception e) {
                logger.error("Failed to retrieve data for key: {}", key, e);
                throw new RuntimeException("Failed to retrieve data", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> delete(String key) {
        return CompletableFuture.runAsync(() -> {
            try (var jedis = jedisPool.getResource()) {
                jedis.del(key.getBytes());
            } catch (Exception e) {
                logger.error("Failed to delete data for key: {}", key, e);
                throw new RuntimeException("Failed to delete data", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> exists(String key) {
        return CompletableFuture.supplyAsync(() -> {
            try (var jedis = jedisPool.getResource()) {
                return jedis.exists(key.getBytes());
            } catch (Exception e) {
                logger.error("Failed to check existence for key: {}", key, e);
                throw new RuntimeException("Failed to check key existence", e);
            }
        });
    }
    
    @Override
    public String getName() {
        return "Redis";
    }
    
    private void setupPubSub() {
        pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                handleChannelMessage(channel, message);
            }
            
            @Override
            public void onPMessage(String pattern, String channel, String message) {
                handleChannelMessage(channel, message);
            }
            
            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                logger.debug("Subscribed to channel: {} (total: {})", channel, subscribedChannels);
            }
            
            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                logger.debug("Unsubscribed from channel: {} (total: {})", channel, subscribedChannels);
            }
            
            @Override
            public void onPSubscribe(String pattern, int subscribedChannels) {
                logger.debug("Pattern subscribed: {} (total: {})", pattern, subscribedChannels);
            }
            
            @Override
            public void onPUnsubscribe(String pattern, int subscribedChannels) {
                logger.debug("Pattern unsubscribed: {} (total: {})", pattern, subscribedChannels);
            }
            
            private void handleChannelMessage(String channel, String message) {
                Consumer<byte[]> handler = subscriptions.get(channel);
                if (handler != null) {
                    try {
                        handler.accept(message.getBytes());
                        logger.debug("Delivered message to handler for channel: {}", channel);
                    } catch (Exception e) {
                        logger.error("Error handling message on channel: {}", channel, e);
                    }
                } else {
                    logger.debug("No handler found for channel: {} (available: {})", channel, subscriptions.keySet());
                }
            }
        };
        
        // Start pub/sub in background thread
        executorService.submit(() -> {
            try (var jedis = jedisPool.getResource()) {
                jedis.psubscribe(pubSub, "*");
            } catch (Exception e) {
                logger.error("Error in pub/sub thread", e);
                connected = false;
            }
        });
    }
}