package com.boredhf.conexus.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Metrics collection for cross-server event broadcasting.
 * 
 * This provides basic observability into the event system's performance,
 * tracking success rates, failure rates, and processing times.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class EventMetrics {
    
    private static final Logger logger = LoggerFactory.getLogger(EventMetrics.class);
    
    private final String serverId;
    private final Instant startTime = Instant.now();
    
    // Overall counters
    private final LongAdder totalEventsProcessed = new LongAdder();
    private final LongAdder totalEventsBroadcast = new LongAdder();
    private final LongAdder totalBroadcastFailures = new LongAdder();
    private final LongAdder totalRetryAttempts = new LongAdder();
    private final LongAdder totalCircuitBreakerOpens = new LongAdder();
    
    // Timing metrics
    private final AtomicLong totalProcessingTimeNanos = new AtomicLong(0);
    private final AtomicLong minProcessingTimeNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong(0);
    
    // Per-event-type metrics
    private final Map<String, LongAdder> eventTypeCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> eventTypeFailures = new ConcurrentHashMap<>();
    
    // Circuit breaker state tracking
    private volatile CircuitBreaker.State lastCircuitBreakerState = CircuitBreaker.State.CLOSED;
    private volatile Instant lastCircuitBreakerStateChange = Instant.now();
    
    public EventMetrics(String serverId) {
        this.serverId = serverId;
    }
    
    /**
     * Records a successful event broadcast.
     * 
     * @param eventType the type of event that was broadcast
     * @param processingTimeNanos the time it took to process the event in nanoseconds
     */
    public void recordEventBroadcast(String eventType, long processingTimeNanos) {
        totalEventsBroadcast.increment();
        totalEventsProcessed.increment();
        
        // Update timing metrics
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        updateMinTime(processingTimeNanos);
        updateMaxTime(processingTimeNanos);
        
        // Update per-type metrics
        eventTypeCounters.computeIfAbsent(eventType, k -> new LongAdder()).increment();
        
        logger.debug("Recorded successful broadcast for {} in {}μs", 
                   eventType, processingTimeNanos / 1000);
    }
    
    /**
     * Records a failed event broadcast.
     * 
     * @param eventType the type of event that failed to broadcast
     * @param processingTimeNanos the time spent trying to process the event
     */
    public void recordEventBroadcastFailure(String eventType, long processingTimeNanos) {
        totalBroadcastFailures.increment();
        totalEventsProcessed.increment();
        
        // Update timing metrics (even failures have processing time)
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        updateMinTime(processingTimeNanos);
        updateMaxTime(processingTimeNanos);
        
        // Update per-type failure metrics
        eventTypeFailures.computeIfAbsent(eventType, k -> new LongAdder()).increment();
        
        logger.debug("Recorded failed broadcast for {} after {}μs", 
                   eventType, processingTimeNanos / 1000);
    }
    
    /**
     * Records a retry attempt.
     */
    public void recordRetryAttempt() {
        totalRetryAttempts.increment();
    }
    
    /**
     * Records a circuit breaker state change.
     * 
     * @param newState the new circuit breaker state
     */
    public void recordCircuitBreakerStateChange(CircuitBreaker.State newState) {
        if (newState != lastCircuitBreakerState) {
            if (newState == CircuitBreaker.State.OPEN) {
                totalCircuitBreakerOpens.increment();
            }
            
            logger.info("Circuit breaker state changed from {} to {} for server {}", 
                      lastCircuitBreakerState, newState, serverId);
            
            lastCircuitBreakerState = newState;
            lastCircuitBreakerStateChange = Instant.now();
        }
    }
    
    /**
     * Gets a snapshot of current metrics.
     * 
     * @return metrics snapshot
     */
    public MetricsSnapshot getSnapshot() {
        long totalProcessed = totalEventsProcessed.sum();
        long totalBroadcasts = totalEventsBroadcast.sum();
        long totalFailures = totalBroadcastFailures.sum();
        long totalRetries = totalRetryAttempts.sum();
        long circuitBreakerOpens = totalCircuitBreakerOpens.sum();
        
        double successRate = totalBroadcasts > 0 ? 
            (double) (totalBroadcasts - totalFailures) / totalBroadcasts * 100.0 : 0.0;
        
        long totalTimeNanos = totalProcessingTimeNanos.get();
        double avgProcessingTimeNanos = totalProcessed > 0 ? 
            (double) totalTimeNanos / totalProcessed : 0.0;
        
        return new MetricsSnapshot(
            serverId,
            startTime,
            Instant.now(),
            totalProcessed,
            totalBroadcasts,
            totalFailures,
            totalRetries,
            circuitBreakerOpens,
            successRate,
            avgProcessingTimeNanos / 1_000_000.0, // Convert to milliseconds
            minProcessingTimeNanos.get() == Long.MAX_VALUE ? 0 : 
                minProcessingTimeNanos.get() / 1_000_000.0,
            maxProcessingTimeNanos.get() / 1_000_000.0,
            lastCircuitBreakerState,
            new ConcurrentHashMap<>(eventTypeCounters),
            new ConcurrentHashMap<>(eventTypeFailures)
        );
    }
    
    /**
     * Logs current metrics at INFO level.
     */
    public void logCurrentMetrics() {
        MetricsSnapshot snapshot = getSnapshot();
        
        logger.info("Event Metrics for {}: {} processed, {:.1f}% success rate, {:.2f}ms avg processing time", 
                  serverId, snapshot.totalEventsProcessed, snapshot.successRate, snapshot.avgProcessingTimeMs);
        
        if (snapshot.totalBroadcastFailures > 0) {
            logger.info("Failures: {} broadcasts, {} retries, {} circuit breaker opens", 
                      snapshot.totalBroadcastFailures, snapshot.totalRetryAttempts, snapshot.circuitBreakerOpens);
        }
        
        if (!snapshot.eventTypeCounters.isEmpty()) {
            logger.debug("Event types processed: {}", snapshot.eventTypeCounters);
        }
    }
    
    private void updateMinTime(long timeNanos) {
        minProcessingTimeNanos.updateAndGet(current -> Math.min(current, timeNanos));
    }
    
    private void updateMaxTime(long timeNanos) {
        maxProcessingTimeNanos.updateAndGet(current -> Math.max(current, timeNanos));
    }
    
    /**
     * Snapshot of metrics at a point in time.
     */
    public static class MetricsSnapshot {
        public final String serverId;
        public final Instant startTime;
        public final Instant snapshotTime;
        public final long totalEventsProcessed;
        public final long totalEventsBroadcast;
        public final long totalBroadcastFailures;
        public final long totalRetryAttempts;
        public final long circuitBreakerOpens;
        public final double successRate;
        public final double avgProcessingTimeMs;
        public final double minProcessingTimeMs;
        public final double maxProcessingTimeMs;
        public final CircuitBreaker.State circuitBreakerState;
        public final Map<String, LongAdder> eventTypeCounters;
        public final Map<String, LongAdder> eventTypeFailures;
        
        public MetricsSnapshot(String serverId, Instant startTime, Instant snapshotTime,
                             long totalEventsProcessed, long totalEventsBroadcast, 
                             long totalBroadcastFailures, long totalRetryAttempts,
                             long circuitBreakerOpens, double successRate, 
                             double avgProcessingTimeMs, double minProcessingTimeMs, 
                             double maxProcessingTimeMs, CircuitBreaker.State circuitBreakerState,
                             Map<String, LongAdder> eventTypeCounters, 
                             Map<String, LongAdder> eventTypeFailures) {
            this.serverId = serverId;
            this.startTime = startTime;
            this.snapshotTime = snapshotTime;
            this.totalEventsProcessed = totalEventsProcessed;
            this.totalEventsBroadcast = totalEventsBroadcast;
            this.totalBroadcastFailures = totalBroadcastFailures;
            this.totalRetryAttempts = totalRetryAttempts;
            this.circuitBreakerOpens = circuitBreakerOpens;
            this.successRate = successRate;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.minProcessingTimeMs = minProcessingTimeMs;
            this.maxProcessingTimeMs = maxProcessingTimeMs;
            this.circuitBreakerState = circuitBreakerState;
            this.eventTypeCounters = eventTypeCounters;
            this.eventTypeFailures = eventTypeFailures;
        }
        
        @Override
        public String toString() {
            return String.format(
                "EventMetrics{server=%s, processed=%d, broadcasts=%d, failures=%d, " +
                "successRate=%.1f%%, avgTime=%.2fms, circuitBreaker=%s}", 
                serverId, totalEventsProcessed, totalEventsBroadcast, totalBroadcastFailures,
                successRate, avgProcessingTimeMs, circuitBreakerState
            );
        }
    }
}
