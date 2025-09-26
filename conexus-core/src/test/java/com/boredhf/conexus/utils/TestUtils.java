package com.boredhf.conexus.utils;

import com.boredhf.conexus.moderation.ModerationService;
import com.boredhf.conexus.events.EventService;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test utilities for creating test data and common test operations.
 */
public class TestUtils {
    
    public static final String TEST_SERVER_1 = "server-1";
    public static final String TEST_SERVER_2 = "server-2";
    public static final UUID TEST_PLAYER_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    public static final UUID TEST_MODERATOR_UUID = UUID.fromString("87654321-4321-4321-4321-cba987654321");
    
    /**
     * Creates a test NetworkBan with default values.
     */
    public static ModerationService.NetworkBan createTestBan() {
        return new ModerationService.NetworkBan(TEST_PLAYER_UUID, "Test ban reason", TEST_MODERATOR_UUID, Duration.ofDays(1));
    }
    
    /**
     * Creates a permanent test NetworkBan.
     */
    public static ModerationService.NetworkBan createPermanentTestBan() {
        return new ModerationService.NetworkBan(TEST_PLAYER_UUID, "Permanent ban reason", TEST_MODERATOR_UUID);
    }
    
    /**
     * Creates a test NetworkKick with default values.
     */
    public static ModerationService.NetworkKick createTestKick() {
        return new ModerationService.NetworkKick(TEST_PLAYER_UUID, "Test kick reason", TEST_MODERATOR_UUID);
    }
    
    /**
     * Creates a test NetworkMute with default values.
     */
    public static ModerationService.NetworkMute createTestMute() {
        return new ModerationService.NetworkMute(TEST_PLAYER_UUID, "Test mute reason", TEST_MODERATOR_UUID, Duration.ofHours(1));
    }
    
    /**
     * Creates a test NetworkWarning with default values.
     */
    public static ModerationService.NetworkWarning createTestWarning() {
        return new ModerationService.NetworkWarning(TEST_PLAYER_UUID, "Test warning reason", TEST_MODERATOR_UUID);
    }
    
    /**
     * Creates a test NetworkEvent implementation.
     */
    public static TestNetworkEvent createTestEvent() {
        return new TestNetworkEvent(TEST_SERVER_1, "test-event", "Test event data");
    }
    
    /**
     * Waits for a CompletableFuture to complete with a timeout.
     */
    public static <T> T waitFor(CompletableFuture<T> future, long timeout, TimeUnit unit) throws Exception {
        return future.get(timeout, unit);
    }
    
    /**
     * Waits for a CompletableFuture to complete with default timeout of 5 seconds.
     */
    public static <T> T waitFor(CompletableFuture<T> future) throws Exception {
        return waitFor(future, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Test implementation of NetworkEvent.
     */
    public static class TestNetworkEvent implements EventService.NetworkEvent {
        private final String sourceServerId;
        private final Instant timestamp;
        private final Map<String, Object> metadata;
        private final String eventType;
        private final String data;
        
        public TestNetworkEvent(String sourceServerId, String eventType, String data) {
            this.sourceServerId = sourceServerId;
            this.eventType = eventType;
            this.data = data;
            this.timestamp = Instant.now();
            this.metadata = new HashMap<>();
            this.metadata.put("eventType", eventType);
            this.metadata.put("data", data);
        }
        
        @Override
        public String getSourceServerId() {
            return sourceServerId;
        }
        
        @Override
        public Instant getTimestamp() {
            return timestamp;
        }
        
        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public String getData() {
            return data;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestNetworkEvent that = (TestNetworkEvent) obj;
            return sourceServerId.equals(that.sourceServerId) &&
                   eventType.equals(that.eventType) &&
                   data.equals(that.data);
        }
        
        @Override
        public int hashCode() {
            return sourceServerId.hashCode() + eventType.hashCode() + data.hashCode();
        }
        
        @Override
        public String toString() {
            return "TestNetworkEvent{" +
                    "sourceServerId='" + sourceServerId + '\'' +
                    ", eventType='" + eventType + '\'' +
                    ", data='" + data + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}