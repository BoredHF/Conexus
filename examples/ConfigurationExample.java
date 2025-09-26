package com.boredhf.conexus.examples;

import com.boredhf.conexus.events.CrossServerEventConfiguration;
import com.boredhf.conexus.events.CrossServerEventService;
import com.boredhf.conexus.communication.*;

/**
 * Example showing different configuration setups for various deployment environments.
 */
public class ConfigurationExample {
    
    /**
     * Development environment configuration with fast failure detection
     */
    public static CrossServerEventConfiguration developmentConfig(String serverId) {
        return new CrossServerEventConfiguration(serverId)
            .setEnableCrossServerBroadcast(true)
            .setEnableLocalEventProcessing(true)
            .setEnableGracefulDegradation(true)
            
            // Fast failure detection for development
            .setCircuitBreakerFailureThreshold(2)
            .setCircuitBreakerTimeoutMillis(5000)  // 5 seconds
            .setCircuitBreakerName("DevEvents")
            
            // Quick retries
            .setMaxRetryAttempts(1)
            .setRetryDelayMillis(500)
            .setRetryBackoffMultiplier(1.5)
            
            // Shorter timeouts for faster feedback
            .setEventProcessingTimeoutMillis(5000)
            .setNetworkBroadcastTimeoutMillis(2000);
    }
    
    /**
     * Production environment configuration with resilience focus
     */
    public static CrossServerEventConfiguration productionConfig(String serverId) {
        return new CrossServerEventConfiguration(serverId)
            .setEnableCrossServerBroadcast(true)
            .setEnableLocalEventProcessing(true)
            .setEnableGracefulDegradation(true)
            
            // More tolerant circuit breaker for production stability
            .setCircuitBreakerFailureThreshold(10)
            .setCircuitBreakerTimeoutMillis(60000) // 1 minute
            .setCircuitBreakerName("ProdEvents")
            
            // More aggressive retry policy
            .setMaxRetryAttempts(5)
            .setRetryDelayMillis(2000)
            .setRetryBackoffMultiplier(2.0)
            
            // Longer timeouts for stability
            .setEventProcessingTimeoutMillis(15000)
            .setNetworkBroadcastTimeoutMillis(10000);
    }
    
    /**
     * High-throughput configuration for servers with many events
     */
    public static CrossServerEventConfiguration highThroughputConfig(String serverId) {
        return new CrossServerEventConfiguration(serverId)
            .setEnableCrossServerBroadcast(true)
            .setEnableLocalEventProcessing(true)
            .setEnableGracefulDegradation(true)
            
            // More lenient circuit breaker to handle bursts
            .setCircuitBreakerFailureThreshold(20)
            .setCircuitBreakerTimeoutMillis(30000)
            .setCircuitBreakerName("HighThroughputEvents")
            
            // Conservative retry to avoid overwhelming network
            .setMaxRetryAttempts(2)
            .setRetryDelayMillis(1000)
            .setRetryBackoffMultiplier(1.8)
            
            // Optimized for throughput
            .setEventProcessingTimeoutMillis(8000)
            .setNetworkBroadcastTimeoutMillis(4000);
    }
    
    /**
     * Testing configuration with minimal timeouts for fast test execution
     */
    public static CrossServerEventConfiguration testingConfig(String serverId) {
        return new CrossServerEventConfiguration(serverId)
            .setEnableCrossServerBroadcast(true)
            .setEnableLocalEventProcessing(true)
            .setEnableGracefulDegradation(false) // Fail fast in tests
            
            // Immediate failure for tests
            .setCircuitBreakerFailureThreshold(1)
            .setCircuitBreakerTimeoutMillis(100)
            .setCircuitBreakerName("TestEvents")
            
            // No retries for faster test execution
            .setMaxRetryAttempts(0)
            .setRetryDelayMillis(50)
            .setRetryBackoffMultiplier(1.0)
            
            // Minimal timeouts
            .setEventProcessingTimeoutMillis(1000)
            .setNetworkBroadcastTimeoutMillis(500);
    }
    
    /**
     * Example of setting up event service with Redis for production
     */
    public static CrossServerEventService setupProductionService(String serverId, String redisHost, int redisPort) {
        // Create Redis-based messaging service
        RedisTransport redisTransport = new RedisTransport(redisHost, redisPort);
        MessageSerializer serializer = new DefaultMessageSerializer();
        
        MessagingService messagingService = new DefaultMessagingService(
            serverId,
            redisTransport,
            serializer
        );
        
        // Use production configuration
        CrossServerEventConfiguration config = productionConfig(serverId);
        
        return new CrossServerEventService(serverId, messagingService, config);
    }
    
    /**
     * Example of setting up event service for development with in-memory transport
     */
    public static CrossServerEventService setupDevelopmentService(String serverId) {
        // Create in-memory messaging service for development
        InMemoryMessagingService messagingService = new InMemoryMessagingService(serverId);
        
        // Use development configuration
        CrossServerEventConfiguration config = developmentConfig(serverId);
        
        return new CrossServerEventService(serverId, messagingService, config);
    }
    
    /**
     * Example showing how to customize configuration for specific needs
     */
    public static void customConfigurationExample() {
        String serverId = "lobby-server-1";
        
        // Start with production base config
        CrossServerEventConfiguration config = productionConfig(serverId)
            // Customize for lobby server specific needs
            .setCircuitBreakerFailureThreshold(15) // Lobby servers handle more load
            .setMaxRetryAttempts(3) // Lobby events are less critical
            .setEnableGracefulDegradation(true); // Always allow graceful degradation
            
        // Additional customizations could be:
        // - Different timeout values based on network conditions
        // - Environment-specific circuit breaker names
        // - Adjusted retry policies based on event criticality
        
        System.out.println("Custom configuration created for: " + config.getServerId());
        System.out.println("Circuit breaker threshold: " + config.getCircuitBreakerFailureThreshold());
        System.out.println("Max retry attempts: " + config.getMaxRetryAttempts());
    }
    
    /**
     * Example showing environment-based configuration selection
     */
    public static CrossServerEventService createServiceForEnvironment(String serverId, String environment) {
        MessagingService messagingService;
        CrossServerEventConfiguration config;
        
        switch (environment.toLowerCase()) {
            case "development":
            case "dev":
                messagingService = new InMemoryMessagingService(serverId);
                config = developmentConfig(serverId);
                break;
                
            case "testing":
            case "test":
                messagingService = new InMemoryMessagingService(serverId);
                config = testingConfig(serverId);
                break;
                
            case "staging":
                // Use Redis for staging but with development-like settings
                messagingService = createRedisMessagingService(serverId, "staging-redis", 6379);
                config = developmentConfig(serverId)
                    .setCircuitBreakerTimeoutMillis(15000) // Bit more tolerance than dev
                    .setMaxRetryAttempts(2);
                break;
                
            case "production":
            case "prod":
                messagingService = createRedisMessagingService(serverId, "prod-redis", 6379);
                config = productionConfig(serverId);
                break;
                
            case "high-throughput":
                messagingService = createRedisMessagingService(serverId, "redis-cluster", 6379);
                config = highThroughputConfig(serverId);
                break;
                
            default:
                throw new IllegalArgumentException("Unknown environment: " + environment);
        }
        
        return new CrossServerEventService(serverId, messagingService, config);
    }
    
    private static MessagingService createRedisMessagingService(String serverId, String host, int port) {
        RedisTransport redisTransport = new RedisTransport(host, port);
        MessageSerializer serializer = new DefaultMessageSerializer();
        return new DefaultMessagingService(serverId, redisTransport, serializer);
    }
    
    public static void main(String[] args) {
        // Example usage
        System.out.println("=== Conexus Event Service Configuration Examples ===\n");
        
        // Show custom configuration
        customConfigurationExample();
        System.out.println();
        
        // Create services for different environments
        String serverId = "example-server";
        
        try {
            CrossServerEventService devService = createServiceForEnvironment(serverId, "development");
            System.out.println("Development service created successfully");
            
            CrossServerEventService testService = createServiceForEnvironment(serverId, "testing");
            System.out.println("Testing service created successfully");
            
            // In a real application, you would initialize and use the service:
            // devService.initialize().thenRun(() -> {
            //     System.out.println("Service initialized and ready");
            //     // Register event listeners, start broadcasting events, etc.
            // });
            
        } catch (Exception e) {
            System.err.println("Failed to create service: " + e.getMessage());
        }
        
        System.out.println("\n=== Configuration Examples Complete ===");
    }
}