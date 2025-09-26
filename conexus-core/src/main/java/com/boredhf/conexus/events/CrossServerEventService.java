package com.boredhf.conexus.events;

import com.boredhf.conexus.communication.MessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cross-server aware EventService implementation.
 * 
 * This implementation extends the basic DefaultEventService with cross-server
 * event broadcasting capabilities using the MessagingService.
 * 
 * Features:
 * - Local event handling with priority support
 * - Cross-server event broadcasting
 * - Event filtering to prevent loops
 * - Priority-based event routing
 * - Graceful handling of messaging failures
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class CrossServerEventService implements EventService {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossServerEventService.class);
    private static final String EVENT_BROADCAST_CHANNEL = "conexus:events";
    
    private final String serverId;
    private final MessagingService messagingService;
    private final NetworkEventRegistry eventRegistry;
    private final CircuitBreaker networkCircuitBreaker;
    private final CrossServerEventConfiguration configuration;
    private final RetryManager retryManager;
    private final ScheduledExecutorService retryScheduler;
    private final EventMetrics metrics;
    private final Map<Class<?>, List<EventListener<? extends NetworkEvent>>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    /**
     * Creates a CrossServerEventService with default configuration.
     */
    public CrossServerEventService(String serverId, MessagingService messagingService) {
        this(serverId, messagingService, new CrossServerEventConfiguration(serverId));
    }
    
    /**
     * Creates a CrossServerEventService with custom configuration.
     */
    public CrossServerEventService(String serverId, MessagingService messagingService, 
                                  CrossServerEventConfiguration configuration) {
        this.serverId = serverId;
        this.messagingService = messagingService;
        this.configuration = configuration;
        
        // Validate configuration
        configuration.validate();
        
        // Create event registry with a basic message serializer
        // In production, this should be injected or configured properly
        this.eventRegistry = new NetworkEventRegistry(new MessageSerializer());
        
        // Initialize circuit breaker for network resilience using configuration
        this.networkCircuitBreaker = new CircuitBreaker(
            configuration.getCircuitBreakerName(),
            configuration.getCircuitBreakerFailureThreshold(),
            configuration.getCircuitBreakerTimeoutMillis()
        );
        
        // Initialize retry manager with scheduler
        this.retryScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "Conexus-Retry-" + serverId + "-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
        this.retryManager = RetryManager.fromConfiguration(retryScheduler, configuration);
        
        // Initialize metrics collection
        this.metrics = new EventMetrics(serverId);
        
        // Initialize the static registry for NetworkEventMessage
        NetworkEventMessage.setEventRegistry(this.eventRegistry);
        
        logger.info("CrossServerEventService created for server {} with configuration: {}", 
                  serverId, configuration);
        logger.debug("Retry manager: {}", retryManager);
    }
    
    /**
     * Initializes the cross-server event service by registering message handlers.
     * 
     * @return CompletableFuture that completes when initialization is done
     */
    public CompletableFuture<Void> initialize() {
        if (!initialized.compareAndSet(false, true)) {
            logger.debug("CrossServerEventService already initialized for server {}", serverId);
            return CompletableFuture.completedFuture(null);
        }
        
        logger.info("Initializing CrossServerEventService for server {}", serverId);
        
        // Register handler for incoming cross-server events
        messagingService.registerHandler(NetworkEventMessage.class, context -> {
            logger.debug("NetworkEventMessage handler called for server {}", serverId);
            NetworkEventMessage eventMessage = context.getMessage();
            handleIncomingNetworkEvent(eventMessage);
        });
        
        logger.debug("Registered NetworkEventMessage handler for server {}", serverId);
        logger.info("CrossServerEventService initialized for server {}", serverId);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Shuts down the cross-server event service.
     * 
     * @return CompletableFuture that completes when shutdown is done
     */
    public CompletableFuture<Void> shutdown() {
        if (!initialized.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Unregister message handler
        messagingService.unregisterHandler(NetworkEventMessage.class);
        
        // Shutdown retry scheduler
        retryScheduler.shutdown();
        try {
            if (!retryScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                retryScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            retryScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear listeners
        listeners.clear();
        
        logger.info("CrossServerEventService shutdown complete for server {}", serverId);
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public <T extends NetworkEvent> CompletableFuture<Void> broadcastEvent(T event) {
        return broadcastEvent(event, EventPriority.NORMAL);
    }
    
    @Override
    public <T extends NetworkEvent> CompletableFuture<Void> broadcastEvent(T event, EventPriority priority) {
        long startTime = System.nanoTime();
        String eventType = event.getClass().getSimpleName();
        
        CompletableFuture<Void> localProcessing = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> networkBroadcast = CompletableFuture.completedFuture(null);
        
        // Handle local listeners first
        if (configuration.isEnableLocalEventProcessing()) {
            localProcessing = CompletableFuture.runAsync(() -> {
                try {
                    notifyLocalListeners(event, priority);
                } catch (Exception e) {
                    logger.error("Error processing local event listeners for {} on server {}: {}", 
                               event.getClass().getSimpleName(), serverId, e.getMessage(), e);
                    // Don't re-throw - we don't want local processing failures to affect network broadcasting
                }
            }).exceptionally(throwable -> {
                logger.error("Async local event processing failed for {} on server {}: {}", 
                           event.getClass().getSimpleName(), serverId, throwable.getMessage(), throwable);
                return null; // Return null to indicate completion despite the error
            });
        }
        
        // Broadcast to other servers if enabled and circuit breaker allows
        if (configuration.isEnableCrossServerBroadcast()) {
            if (!networkCircuitBreaker.allowRequest()) {
                logger.warn("Circuit breaker is OPEN, skipping cross-server broadcast for {} from server {}", 
                          event.getClass().getSimpleName(), serverId);
                
                if (!configuration.isEnableGracefulDegradation()) {
                    CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(
                        new RuntimeException("Circuit breaker is open, cross-server broadcasting disabled"));
                    networkBroadcast = failedFuture;
                }
                // If graceful degradation is enabled, networkBroadcast remains a completed future
            } else {
                // Use retry logic for network broadcasting
                String operationName = "broadcast-" + event.getClass().getSimpleName();
                
                networkBroadcast = retryManager.executeWithRetry(() -> {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            logger.debug("Creating NetworkEventMessage for cross-server broadcast from server {}", serverId);
                            NetworkEventMessage eventMessage = new NetworkEventMessage(
                                (Class<? extends NetworkEvent>) event.getClass(),
                                event,
                                priority,
                                serverId
                            );
                            
                            logger.debug("Broadcasting NetworkEventMessage: {} from server {}", 
                                       eventMessage.getEventType().getSimpleName(), serverId);
                            return messagingService.broadcast(eventMessage);
                        } catch (Exception e) {
                            logger.debug("Error creating network event message for {} from server {}: {}", 
                                       event.getClass().getSimpleName(), serverId, e.getMessage());
                            throw e;
                        }
                    }).thenCompose(future -> future);
                }, operationName).whenComplete((result, throwable) -> {
                    // Update circuit breaker based on final result
                    if (throwable == null) {
                        networkCircuitBreaker.recordSuccess();
                    } else {
                        networkCircuitBreaker.recordFailure();
                        logger.error("Network broadcast failed after retries for {} from server {}: {}", 
                                   event.getClass().getSimpleName(), serverId, throwable.getMessage());
                    }
                });
            }
        }
        
        // Return combined future
        return CompletableFuture.allOf(localProcessing, networkBroadcast)
                .whenComplete((result, throwable) -> {
                    long processingTime = System.nanoTime() - startTime;
                    
                    if (throwable != null) {
                        metrics.recordEventBroadcastFailure(eventType, processingTime);
                        logger.error("Error broadcasting event {} with priority {}: {}", 
                                   eventType, priority, throwable.getMessage(), throwable);
                    } else {
                        metrics.recordEventBroadcast(eventType, processingTime);
                        logger.debug("Successfully broadcast event {} with priority {} from server {} in {:.2f}ms", 
                                   eventType, priority, serverId, processingTime / 1_000_000.0);
                    }
                    
                    // Update circuit breaker state metrics
                    metrics.recordCircuitBreakerStateChange(networkCircuitBreaker.getState());
                });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends NetworkEvent> void registerEventListener(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add((EventListener<? extends NetworkEvent>) listener);
        
        logger.debug("Registered event listener for {} on server {}", eventType.getSimpleName(), serverId);
    }
    
    @Override
    public <T extends NetworkEvent> void unregisterEventListener(Class<T> eventType, EventListener<T> listener) {
        List<EventListener<? extends NetworkEvent>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
            if (eventListeners.isEmpty()) {
                listeners.remove(eventType);
            }
            
            logger.debug("Unregistered event listener for {} on server {}", eventType.getSimpleName(), serverId);
        }
    }
    
    /**
     * Handles incoming cross-server network events.
     * 
     * @param eventMessage the network event message
     */
    private void handleIncomingNetworkEvent(NetworkEventMessage eventMessage) {
        try {
            logger.debug("Received NetworkEventMessage from server {} on server {}", 
                       eventMessage.getOriginalServerId(), serverId);
            
            // Don't process events that originated from this server to prevent loops
            if (serverId.equals(eventMessage.getOriginalServerId())) {
                logger.debug("Ignoring loopback event from same server {}", eventMessage.getOriginalServerId());
                return;
            }
            
            // Reconstruct the event from serialized data
            eventMessage.reconstructEvent();
            
            NetworkEvent event = eventMessage.getEventData();
            EventPriority priority = eventMessage.getPriority();
            
            logger.debug("Processing cross-server event {} from server {} with priority {}", 
                       event.getClass().getSimpleName(), eventMessage.getOriginalServerId(), priority);
            
            // Notify local listeners about the cross-server event
            notifyLocalListeners(event, priority);
            
        } catch (Exception e) {
            logger.error("Error handling incoming network event", e);
        }
    }
    
    /**
     * Notifies local listeners about an event.
     * 
     * @param event the event to notify about
     * @param priority the event priority
     */
    @SuppressWarnings("unchecked")
    private <T extends NetworkEvent> void notifyLocalListeners(T event, EventPriority priority) {
        List<EventListener<? extends NetworkEvent>> eventListeners = listeners.get(event.getClass());
        if (eventListeners == null || eventListeners.isEmpty()) {
            logger.debug("No local listeners found for event type {}", event.getClass().getSimpleName());
            return;
        }
        
        logger.debug("Notifying {} local listeners for event {} with priority {}", 
                   eventListeners.size(), event.getClass().getSimpleName(), priority);
        
        // Sort listeners by priority if needed (for future enhancement)
        // For now, process all listeners
        for (EventListener<? extends NetworkEvent> listener : eventListeners) {
            try {
                ((EventListener<NetworkEvent>) listener).onEvent(event);
            } catch (Exception e) {
                logger.error("Error in event listener for {} on server {}", 
                           event.getClass().getSimpleName(), serverId, e);
                // Continue processing other listeners even if one fails
            }
        }
    }
    
    /**
     * Gets the configuration used by this service.
     * 
     * @return the configuration
     */
    public CrossServerEventConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Enables or disables cross-server event broadcasting.
     * 
     * @param enabled true to enable cross-server broadcasting, false to disable
     * @deprecated Use configuration object directly for better type safety
     */
    @Deprecated
    public void setCrossServerBroadcastEnabled(boolean enabled) {
        configuration.setEnableCrossServerBroadcast(enabled);
        logger.info("Cross-server event broadcasting {} on server {}", 
                  enabled ? "enabled" : "disabled", serverId);
    }
    
    /**
     * Enables or disables local event processing.
     * 
     * @param enabled true to enable local processing, false to disable
     * @deprecated Use configuration object directly for better type safety
     */
    @Deprecated
    public void setLocalEventProcessingEnabled(boolean enabled) {
        configuration.setEnableLocalEventProcessing(enabled);
        logger.info("Local event processing {} on server {}", 
                  enabled ? "enabled" : "disabled", serverId);
    }
    
    /**
     * Gets the number of registered listeners for a specific event type.
     * 
     * @param eventType the event type
     * @return the number of listeners
     */
    public int getListenerCount(Class<? extends NetworkEvent> eventType) {
        List<EventListener<? extends NetworkEvent>> eventListeners = listeners.get(eventType);
        return eventListeners != null ? eventListeners.size() : 0;
    }
    
    /**
     * Gets the total number of registered listeners across all event types.
     * 
     * @return the total listener count
     */
    public int getTotalListenerCount() {
        return listeners.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * Checks if cross-server broadcasting is enabled.
     * 
     * @return true if enabled, false otherwise
     * @deprecated Use getConfiguration().isEnableCrossServerBroadcast() instead
     */
    @Deprecated
    public boolean isCrossServerBroadcastEnabled() {
        return configuration.isEnableCrossServerBroadcast();
    }
    
    /**
     * Checks if local event processing is enabled.
     * 
     * @return true if enabled, false otherwise
     * @deprecated Use getConfiguration().isEnableLocalEventProcessing() instead
     */
    @Deprecated
    public boolean isLocalEventProcessingEnabled() {
        return configuration.isEnableLocalEventProcessing();
    }
    
    /**
     * Registers a custom event type with its deserializer.
     * This allows the cross-server event system to handle additional event types
     * beyond the built-in ones.
     * 
     * @param eventType the event class
     * @param deserializer function to deserialize JSON string to event instance
     * @param <T> the event type
     */
    public <T extends NetworkEvent> void registerEventType(
            Class<T> eventType, 
            java.util.function.Function<String, T> deserializer) {
        eventRegistry.registerEventType(eventType, deserializer);
        logger.info("Registered custom event type {} for cross-server broadcasting on server {}", 
                  eventType.getSimpleName(), serverId);
    }
    
    /**
     * Gets the event registry used by this service.
     * 
     * @return the network event registry
     */
    public NetworkEventRegistry getEventRegistry() {
        return eventRegistry;
    }
    
    /**
     * Gets the circuit breaker used for network resilience.
     * 
     * @return the network circuit breaker
     */
    public CircuitBreaker getNetworkCircuitBreaker() {
        return networkCircuitBreaker;
    }
    
    /**
     * Enables or disables graceful degradation.
     * When enabled, the service continues to work locally even when
     * cross-server broadcasting fails.
     * 
     * @param enabled true to enable graceful degradation, false to fail fast
     */
    public void setGracefulDegradationEnabled(boolean enabled) {
        configuration.setEnableGracefulDegradation(enabled);
        logger.info("Graceful degradation {} on server {}", 
                  enabled ? "enabled" : "disabled", serverId);
    }
    
    /**
     * Checks if graceful degradation is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isGracefulDegradationEnabled() {
        return configuration.isEnableGracefulDegradation();
    }
    
    /**
     * Gets the event metrics for this service.
     * 
     * @return the event metrics
     */
    public EventMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Gets a snapshot of current metrics.
     * 
     * @return metrics snapshot
     */
    public EventMetrics.MetricsSnapshot getMetricsSnapshot() {
        return metrics.getSnapshot();
    }
    
    /**
     * Logs current metrics at INFO level.
     */
    public void logMetrics() {
        metrics.logCurrentMetrics();
    }
}
