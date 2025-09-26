package com.boredhf.conexus.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple circuit breaker implementation for cross-server event broadcasting.
 * 
 * This provides resilience against cascading failures by temporarily stopping
 * attempts to broadcast events when the messaging service is experiencing failures.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class CircuitBreaker {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    public enum State {
        CLOSED,     // Normal operation
        OPEN,       // Failure threshold exceeded, blocking calls
        HALF_OPEN   // Testing if service has recovered
    }
    
    private final String name;
    private final int failureThreshold;
    private final long timeoutMillis;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    
    /**
     * Creates a new circuit breaker.
     * 
     * @param name the name of this circuit breaker for logging
     * @param failureThreshold number of failures before opening the circuit
     * @param timeoutMillis time to wait before transitioning to half-open
     */
    public CircuitBreaker(String name, int failureThreshold, long timeoutMillis) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeoutMillis = timeoutMillis;
    }
    
    /**
     * Checks if a call should be allowed through the circuit breaker.
     * 
     * @return true if the call should proceed, false if it should be blocked
     */
    public boolean allowRequest() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                if (shouldAttemptReset()) {
                    logger.info("Circuit breaker {} transitioning to HALF_OPEN", name);
                    state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    return true;
                }
                return false;
                
            case HALF_OPEN:
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Records a successful operation.
     */
    public void recordSuccess() {
        failureCount.set(0);
        successCount.incrementAndGet();
        
        if (state.get() == State.HALF_OPEN) {
            logger.info("Circuit breaker {} transitioning to CLOSED after successful operation", name);
            state.set(State.CLOSED);
        }
    }
    
    /**
     * Records a failed operation.
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        if (failures >= failureThreshold && state.compareAndSet(State.CLOSED, State.OPEN)) {
            logger.warn("Circuit breaker {} OPENED after {} failures (threshold: {})", name, failures, failureThreshold);
        }
        
        if (state.get() == State.HALF_OPEN) {
            logger.info("Circuit breaker {} transitioning back to OPEN after failure in half-open state", name);
            state.set(State.OPEN);
        }
    }
    
    /**
     * Gets the current state of the circuit breaker.
     * 
     * @return the current state
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Gets the current failure count.
     * 
     * @return the failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Gets the current success count.
     * 
     * @return the success count
     */
    public int getSuccessCount() {
        return successCount.get();
    }
    
    /**
     * Resets the circuit breaker to closed state.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        lastFailureTime.set(0);
        logger.info("Circuit breaker {} manually reset to CLOSED state", name);
    }
    
    /**
     * Checks if enough time has passed to attempt a reset.
     * 
     * @return true if we should attempt to reset
     */
    private boolean shouldAttemptReset() {
        return System.currentTimeMillis() - lastFailureTime.get() >= timeoutMillis;
    }
    
    @Override
    public String toString() {
        return String.format("CircuitBreaker{name='%s', state=%s, failures=%d, successes=%d}", 
                           name, state.get(), failureCount.get(), successCount.get());
    }
}