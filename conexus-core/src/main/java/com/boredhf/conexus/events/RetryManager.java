package com.boredhf.conexus.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Retry manager with exponential backoff for failed operations.
 * 
 * This provides configurable retry logic for network operations that may fail
 * due to temporary issues like network connectivity problems.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class RetryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryManager.class);
    
    private final ScheduledExecutorService scheduler;
    private final int maxAttempts;
    private final long baseDelayMillis;
    private final double backoffMultiplier;
    private final long maxDelayMillis;
    
    /**
     * Creates a retry manager with the given configuration.
     * 
     * @param scheduler the scheduler for delayed retries
     * @param maxAttempts maximum number of retry attempts
     * @param baseDelayMillis base delay between retries in milliseconds
     * @param backoffMultiplier multiplier for exponential backoff
     * @param maxDelayMillis maximum delay between retries
     */
    public RetryManager(ScheduledExecutorService scheduler, int maxAttempts, long baseDelayMillis, 
                       double backoffMultiplier, long maxDelayMillis) {
        this.scheduler = scheduler;
        this.maxAttempts = maxAttempts;
        this.baseDelayMillis = baseDelayMillis;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelayMillis = maxDelayMillis;
    }
    
    /**
     * Executes an operation with retry logic.
     * 
     * @param operation the operation to execute
     * @param operationName name for logging purposes
     * @param <T> the return type of the operation
     * @return CompletableFuture that completes with the operation result or fails after all retries
     */
    public <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation, 
            String operationName) {
        
        return executeWithRetry(operation, operationName, 1);
    }
    
    /**
     * Internal method for recursive retry execution.
     */
    private <T> CompletableFuture<T> executeWithRetry(
            Supplier<CompletableFuture<T>> operation, 
            String operationName, 
            int attempt) {
        
        logger.debug("Executing {} (attempt {} of {})", operationName, attempt, maxAttempts);
        
        try {
            CompletableFuture<T> operationFuture = operation.get();
            
            return operationFuture.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    if (attempt > 1) {
                        logger.info("{} succeeded on attempt {}/{}", operationName, attempt, maxAttempts);
                    }
                } else {
                    logger.debug("{} failed on attempt {}/{}: {}", 
                               operationName, attempt, maxAttempts, throwable.getMessage());
                }
            }).exceptionally(throwable -> {
                if (attempt < maxAttempts) {
                    logger.warn("{} failed on attempt {}/{}, will retry: {}", 
                              operationName, attempt, maxAttempts, throwable.getMessage());
                    
                    // Calculate delay with exponential backoff
                    long delay = calculateDelay(attempt);
                    
                    // Schedule the retry
                    CompletableFuture<T> retryFuture = new CompletableFuture<>();
                    scheduler.schedule(() -> {
                        executeWithRetry(operation, operationName, attempt + 1)
                            .whenComplete((result, retryThrowable) -> {
                                if (retryThrowable == null) {
                                    retryFuture.complete(result);
                                } else {
                                    retryFuture.completeExceptionally(retryThrowable);
                                }
                            });
                    }, delay, TimeUnit.MILLISECONDS);
                    
                    try {
                        return retryFuture.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    logger.error("{} failed after {} attempts, giving up: {}", 
                               operationName, maxAttempts, throwable.getMessage());
                    throw new RuntimeException(throwable);
                }
            });
            
        } catch (Exception e) {
            logger.error("{} threw exception during setup on attempt {}: {}", 
                       operationName, attempt, e.getMessage(), e);
            
            if (attempt < maxAttempts) {
                long delay = calculateDelay(attempt);
                CompletableFuture<T> retryFuture = new CompletableFuture<>();
                
                scheduler.schedule(() -> {
                    executeWithRetry(operation, operationName, attempt + 1)
                        .whenComplete((result, retryThrowable) -> {
                            if (retryThrowable == null) {
                                retryFuture.complete(result);
                            } else {
                                retryFuture.completeExceptionally(retryThrowable);
                            }
                        });
                }, delay, TimeUnit.MILLISECONDS);
                
                return retryFuture;
            } else {
                CompletableFuture<T> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(e);
                return failedFuture;
            }
        }
    }
    
    /**
     * Calculates the delay for the next retry attempt using exponential backoff.
     * 
     * @param attempt the current attempt number (1-based)
     * @return the delay in milliseconds
     */
    private long calculateDelay(int attempt) {
        long delay = (long) (baseDelayMillis * Math.pow(backoffMultiplier, attempt - 1));
        return Math.min(delay, maxDelayMillis);
    }
    
    /**
     * Creates a retry manager from configuration.
     * 
     * @param scheduler the scheduler for delayed retries
     * @param config the configuration containing retry settings
     * @return a new retry manager
     */
    public static RetryManager fromConfiguration(ScheduledExecutorService scheduler, 
                                               CrossServerEventConfiguration config) {
        return new RetryManager(
            scheduler,
            config.getMaxRetryAttempts(),
            config.getRetryDelayMillis(),
            config.getRetryBackoffMultiplier(),
            config.getRetryDelayMillis() * 10 // max delay is 10x base delay
        );
    }
    
    @Override
    public String toString() {
        return String.format("RetryManager{maxAttempts=%d, baseDelay=%dms, backoff=%.1f, maxDelay=%dms}", 
                           maxAttempts, baseDelayMillis, backoffMultiplier, maxDelayMillis);
    }
}