package com.boredhf.conexus;

/**
 * Configuration for Redis connection in tests.
 * Supports both embedded Redis and external Redis (like GitHub Actions service).
 */
public class TestRedisConfiguration {
    
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 6379;
    
    /**
     * Gets the Redis host to use for tests.
     * Checks environment variables first, falls back to localhost.
     */
    public static String getRedisHost() {
        return System.getenv().getOrDefault("REDIS_HOST", DEFAULT_HOST);
    }
    
    /**
     * Gets the Redis port to use for tests.
     * Checks environment variables first, falls back to 6379.
     */
    public static int getRedisPort() {
        String portEnv = System.getenv("REDIS_PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                System.err.println("Invalid REDIS_PORT environment variable: " + portEnv + ", using default " + DEFAULT_PORT);
            }
        }
        return DEFAULT_PORT;
    }
    
    /**
     * Checks if we should use external Redis (from environment) instead of embedded Redis.
     */
    public static boolean useExternalRedis() {
        return System.getenv("REDIS_HOST") != null || System.getenv("REDIS_PORT") != null;
    }
    
    /**
     * Creates a configuration summary for logging.
     */
    public static String getConfigurationSummary() {
        if (useExternalRedis()) {
            return String.format("External Redis at %s:%d", getRedisHost(), getRedisPort());
        } else {
            return "Embedded Redis (will be started dynamically)";
        }
    }
}