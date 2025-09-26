package com.boredhf.conexus.events;

/**
 * Configuration settings for the CrossServerEventService.
 * 
 * This class encapsulates all configurable parameters for cross-server
 * event broadcasting, making the system more flexible and production-ready.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class CrossServerEventConfiguration {
    
    // Event Broadcasting Settings
    private boolean enableCrossServerBroadcast = true;
    private boolean enableLocalEventProcessing = true;
    private boolean enableGracefulDegradation = true;
    
    // Circuit Breaker Settings
    private int circuitBreakerFailureThreshold = 5;
    private long circuitBreakerTimeoutMillis = 30000; // 30 seconds
    private String circuitBreakerName = "NetworkBroadcast";
    
    // Channel Settings
    private String eventBroadcastChannel = "conexus:events";
    
    // Retry Settings
    private int maxRetryAttempts = 3;
    private long retryDelayMillis = 1000; // 1 second
    private double retryBackoffMultiplier = 2.0;
    
    // Timeout Settings
    private long eventProcessingTimeoutMillis = 10000; // 10 seconds
    private long networkBroadcastTimeoutMillis = 5000;  // 5 seconds
    
    // Performance Settings
    private int maxConcurrentEvents = 100;
    private boolean useCustomThreadPool = false;
    private int threadPoolCoreSize = 4;
    private int threadPoolMaxSize = 16;
    private long threadPoolKeepAliveSeconds = 60;
    
    /**
     * Creates a new configuration with default settings.
     */
    public CrossServerEventConfiguration() {
        // Uses default values set above
    }
    
    /**
     * Creates a configuration for the given server ID.
     * This allows server-specific customizations.
     * 
     * @param serverId the server ID
     */
    public CrossServerEventConfiguration(String serverId) {
        this.circuitBreakerName = "NetworkBroadcast-" + serverId;
    }
    
    // Getters and Setters
    
    public boolean isEnableCrossServerBroadcast() {
        return enableCrossServerBroadcast;
    }
    
    public CrossServerEventConfiguration setEnableCrossServerBroadcast(boolean enableCrossServerBroadcast) {
        this.enableCrossServerBroadcast = enableCrossServerBroadcast;
        return this;
    }
    
    public boolean isEnableLocalEventProcessing() {
        return enableLocalEventProcessing;
    }
    
    public CrossServerEventConfiguration setEnableLocalEventProcessing(boolean enableLocalEventProcessing) {
        this.enableLocalEventProcessing = enableLocalEventProcessing;
        return this;
    }
    
    public boolean isEnableGracefulDegradation() {
        return enableGracefulDegradation;
    }
    
    public CrossServerEventConfiguration setEnableGracefulDegradation(boolean enableGracefulDegradation) {
        this.enableGracefulDegradation = enableGracefulDegradation;
        return this;
    }
    
    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }
    
    public CrossServerEventConfiguration setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
        return this;
    }
    
    public long getCircuitBreakerTimeoutMillis() {
        return circuitBreakerTimeoutMillis;
    }
    
    public CrossServerEventConfiguration setCircuitBreakerTimeoutMillis(long circuitBreakerTimeoutMillis) {
        this.circuitBreakerTimeoutMillis = circuitBreakerTimeoutMillis;
        return this;
    }
    
    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }
    
    public CrossServerEventConfiguration setCircuitBreakerName(String circuitBreakerName) {
        this.circuitBreakerName = circuitBreakerName;
        return this;
    }
    
    public String getEventBroadcastChannel() {
        return eventBroadcastChannel;
    }
    
    public CrossServerEventConfiguration setEventBroadcastChannel(String eventBroadcastChannel) {
        this.eventBroadcastChannel = eventBroadcastChannel;
        return this;
    }
    
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    public CrossServerEventConfiguration setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
        return this;
    }
    
    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }
    
    public CrossServerEventConfiguration setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
        return this;
    }
    
    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }
    
    public CrossServerEventConfiguration setRetryBackoffMultiplier(double retryBackoffMultiplier) {
        this.retryBackoffMultiplier = retryBackoffMultiplier;
        return this;
    }
    
    public long getEventProcessingTimeoutMillis() {
        return eventProcessingTimeoutMillis;
    }
    
    public CrossServerEventConfiguration setEventProcessingTimeoutMillis(long eventProcessingTimeoutMillis) {
        this.eventProcessingTimeoutMillis = eventProcessingTimeoutMillis;
        return this;
    }
    
    public long getNetworkBroadcastTimeoutMillis() {
        return networkBroadcastTimeoutMillis;
    }
    
    public CrossServerEventConfiguration setNetworkBroadcastTimeoutMillis(long networkBroadcastTimeoutMillis) {
        this.networkBroadcastTimeoutMillis = networkBroadcastTimeoutMillis;
        return this;
    }
    
    public int getMaxConcurrentEvents() {
        return maxConcurrentEvents;
    }
    
    public CrossServerEventConfiguration setMaxConcurrentEvents(int maxConcurrentEvents) {
        this.maxConcurrentEvents = maxConcurrentEvents;
        return this;
    }
    
    public boolean isUseCustomThreadPool() {
        return useCustomThreadPool;
    }
    
    public CrossServerEventConfiguration setUseCustomThreadPool(boolean useCustomThreadPool) {
        this.useCustomThreadPool = useCustomThreadPool;
        return this;
    }
    
    public int getThreadPoolCoreSize() {
        return threadPoolCoreSize;
    }
    
    public CrossServerEventConfiguration setThreadPoolCoreSize(int threadPoolCoreSize) {
        this.threadPoolCoreSize = threadPoolCoreSize;
        return this;
    }
    
    public int getThreadPoolMaxSize() {
        return threadPoolMaxSize;
    }
    
    public CrossServerEventConfiguration setThreadPoolMaxSize(int threadPoolMaxSize) {
        this.threadPoolMaxSize = threadPoolMaxSize;
        return this;
    }
    
    public long getThreadPoolKeepAliveSeconds() {
        return threadPoolKeepAliveSeconds;
    }
    
    public CrossServerEventConfiguration setThreadPoolKeepAliveSeconds(long threadPoolKeepAliveSeconds) {
        this.threadPoolKeepAliveSeconds = threadPoolKeepAliveSeconds;
        return this;
    }
    
    /**
     * Validates the configuration settings.
     * 
     * @throws IllegalArgumentException if any settings are invalid
     */
    public void validate() {
        if (circuitBreakerFailureThreshold < 1) {
            throw new IllegalArgumentException("Circuit breaker failure threshold must be at least 1");
        }
        
        if (circuitBreakerTimeoutMillis < 1000) {
            throw new IllegalArgumentException("Circuit breaker timeout must be at least 1000ms");
        }
        
        if (eventBroadcastChannel == null || eventBroadcastChannel.trim().isEmpty()) {
            throw new IllegalArgumentException("Event broadcast channel cannot be null or empty");
        }
        
        if (maxRetryAttempts < 0) {
            throw new IllegalArgumentException("Max retry attempts cannot be negative");
        }
        
        if (retryDelayMillis < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
        
        if (retryBackoffMultiplier < 1.0) {
            throw new IllegalArgumentException("Retry backoff multiplier must be at least 1.0");
        }
        
        if (eventProcessingTimeoutMillis < 1000) {
            throw new IllegalArgumentException("Event processing timeout must be at least 1000ms");
        }
        
        if (networkBroadcastTimeoutMillis < 1000) {
            throw new IllegalArgumentException("Network broadcast timeout must be at least 1000ms");
        }
        
        if (maxConcurrentEvents < 1) {
            throw new IllegalArgumentException("Max concurrent events must be at least 1");
        }
        
        if (useCustomThreadPool) {
            if (threadPoolCoreSize < 1) {
                throw new IllegalArgumentException("Thread pool core size must be at least 1");
            }
            
            if (threadPoolMaxSize < threadPoolCoreSize) {
                throw new IllegalArgumentException("Thread pool max size must be at least core size");
            }
            
            if (threadPoolKeepAliveSeconds < 0) {
                throw new IllegalArgumentException("Thread pool keep alive cannot be negative");
            }
        }
    }
    
    @Override
    public String toString() {
        return "CrossServerEventConfiguration{" +
               "enableCrossServerBroadcast=" + enableCrossServerBroadcast +
               ", enableLocalEventProcessing=" + enableLocalEventProcessing +
               ", enableGracefulDegradation=" + enableGracefulDegradation +
               ", circuitBreakerFailureThreshold=" + circuitBreakerFailureThreshold +
               ", circuitBreakerTimeoutMillis=" + circuitBreakerTimeoutMillis +
               ", eventBroadcastChannel='" + eventBroadcastChannel + '\'' +
               ", maxRetryAttempts=" + maxRetryAttempts +
               ", eventProcessingTimeoutMillis=" + eventProcessingTimeoutMillis +
               ", networkBroadcastTimeoutMillis=" + networkBroadcastTimeoutMillis +
               ", maxConcurrentEvents=" + maxConcurrentEvents +
               ", useCustomThreadPool=" + useCustomThreadPool +
               '}';
    }
}