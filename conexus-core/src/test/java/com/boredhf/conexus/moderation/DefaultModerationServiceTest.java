package com.boredhf.conexus.moderation;

import com.boredhf.conexus.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultModerationService.
 */
@ExtendWith(MockitoExtension.class)
class DefaultModerationServiceTest {
    
    private DefaultModerationService moderationService;
    
    @BeforeEach
    void setUp() {
        moderationService = new DefaultModerationService(TestUtils.TEST_SERVER_1);
    }
    
    @Test
    void testExecuteBan() throws Exception {
        // Given
        ModerationService.NetworkBan ban = TestUtils.createTestBan();
        AtomicInteger listenerCalls = new AtomicInteger(0);
        
        // Register listener
        moderationService.registerModerationListener(new ModerationService.ModerationListener() {
            @Override
            public void onBanExecuted(ModerationService.NetworkBan receivedBan, String serverId) {
                assertEquals(ban.getPlayerId(), receivedBan.getPlayerId());
                assertEquals(ban.getReason(), receivedBan.getReason());
                assertEquals(ban.getModeratorId(), receivedBan.getModeratorId());
                assertEquals(TestUtils.TEST_SERVER_1, serverId);
                listenerCalls.incrementAndGet();
            }
        });
        
        // When
        CompletableFuture<Void> result = moderationService.executeBan(ban);
        TestUtils.waitFor(result);
        
        // Then
        assertEquals(1, listenerCalls.get(), "Listener should be called once");
        assertTrue(result.isDone(), "Future should be completed");
        assertFalse(result.isCompletedExceptionally(), "Future should not complete exceptionally");
    }
    
    @Test
    void testExecuteKick() throws Exception {
        // Given
        ModerationService.NetworkKick kick = TestUtils.createTestKick();
        AtomicInteger listenerCalls = new AtomicInteger(0);
        
        // Register listener
        moderationService.registerModerationListener(new ModerationService.ModerationListener() {
            @Override
            public void onKickExecuted(ModerationService.NetworkKick receivedKick, String serverId) {
                assertEquals(kick.getPlayerId(), receivedKick.getPlayerId());
                assertEquals(kick.getReason(), receivedKick.getReason());
                assertEquals(kick.getModeratorId(), receivedKick.getModeratorId());
                assertEquals(TestUtils.TEST_SERVER_1, serverId);
                listenerCalls.incrementAndGet();
            }
        });
        
        // When
        CompletableFuture<Void> result = moderationService.executeKick(kick);
        TestUtils.waitFor(result);
        
        // Then
        assertEquals(1, listenerCalls.get(), "Listener should be called once");
        assertTrue(result.isDone(), "Future should be completed");
    }
    
    @Test
    void testExecuteMute() throws Exception {
        // Given
        ModerationService.NetworkMute mute = TestUtils.createTestMute();
        AtomicInteger listenerCalls = new AtomicInteger(0);
        
        // Register listener
        moderationService.registerModerationListener(new ModerationService.ModerationListener() {
            @Override
            public void onMuteExecuted(ModerationService.NetworkMute receivedMute, String serverId) {
                assertEquals(mute.getPlayerId(), receivedMute.getPlayerId());
                assertEquals(mute.getReason(), receivedMute.getReason());
                assertEquals(mute.getModeratorId(), receivedMute.getModeratorId());
                assertEquals(TestUtils.TEST_SERVER_1, serverId);
                listenerCalls.incrementAndGet();
            }
        });
        
        // When
        CompletableFuture<Void> result = moderationService.executeMute(mute);
        TestUtils.waitFor(result);
        
        // Then
        assertEquals(1, listenerCalls.get(), "Listener should be called once");
        assertTrue(result.isDone(), "Future should be completed");
    }
    
    @Test
    void testExecuteWarning() throws Exception {
        // Given
        ModerationService.NetworkWarning warning = TestUtils.createTestWarning();
        AtomicInteger listenerCalls = new AtomicInteger(0);
        
        // Register listener
        moderationService.registerModerationListener(new ModerationService.ModerationListener() {
            @Override
            public void onWarningIssued(ModerationService.NetworkWarning receivedWarning, String serverId) {
                assertEquals(warning.getPlayerId(), receivedWarning.getPlayerId());
                assertEquals(warning.getReason(), receivedWarning.getReason());
                assertEquals(warning.getModeratorId(), receivedWarning.getModeratorId());
                assertEquals(TestUtils.TEST_SERVER_1, serverId);
                listenerCalls.incrementAndGet();
            }
        });
        
        // When
        CompletableFuture<Void> result = moderationService.executeWarning(warning);
        TestUtils.waitFor(result);
        
        // Then
        assertEquals(1, listenerCalls.get(), "Listener should be called once");
        assertTrue(result.isDone(), "Future should be completed");
    }
    
    @Test
    void testExecuteUnban() throws Exception {
        // Given
        AtomicInteger listenerCalls = new AtomicInteger(0);
        
        // Register listener
        moderationService.registerModerationListener(new ModerationService.ModerationListener() {
            @Override
            public void onUnbanExecuted(java.util.UUID playerId, java.util.UUID moderatorId, String reason, String serverId) {
                assertEquals(TestUtils.TEST_PLAYER_UUID, playerId);
                assertEquals(TestUtils.TEST_MODERATOR_UUID, moderatorId);
                assertEquals("Unban reason", reason);
                assertEquals(TestUtils.TEST_SERVER_1, serverId);
                listenerCalls.incrementAndGet();
            }
        });
        
        // When
        CompletableFuture<Void> result = moderationService.executeUnban(TestUtils.TEST_PLAYER_UUID, TestUtils.TEST_MODERATOR_UUID, "Unban reason");
        TestUtils.waitFor(result);
        
        // Then
        assertEquals(1, listenerCalls.get(), "Listener should be called once");
        assertTrue(result.isDone(), "Future should be completed");
    }
    
    @Test
    void testGetActiveBan() throws Exception {
        // When
        CompletableFuture<Optional<ModerationService.NetworkBan>> result = moderationService.getActiveBan(TestUtils.TEST_PLAYER_UUID);
        Optional<ModerationService.NetworkBan> ban = TestUtils.waitFor(result);
        
        // Then
        assertFalse(ban.isPresent(), "Should return empty since no persistent storage is implemented yet");
    }
    
    @Test
    void testGetActiveMute() throws Exception {
        // When
        CompletableFuture<Optional<ModerationService.NetworkMute>> result = moderationService.getActiveMute(TestUtils.TEST_PLAYER_UUID);
        Optional<ModerationService.NetworkMute> mute = TestUtils.waitFor(result);
        
        // Then
        assertFalse(mute.isPresent(), "Should return empty since no persistent storage is implemented yet");
    }
    
    @Test
    void testGetWarnings() throws Exception {
        // When
        CompletableFuture<List<ModerationService.NetworkWarning>> result = moderationService.getWarnings(TestUtils.TEST_PLAYER_UUID);
        List<ModerationService.NetworkWarning> warnings = TestUtils.waitFor(result);
        
        // Then
        assertTrue(warnings.isEmpty(), "Should return empty list since no persistent storage is implemented yet");
    }
    
    @Test
    void testMultipleListeners() throws Exception {
        // Given
        AtomicInteger listener1Calls = new AtomicInteger(0);
        AtomicInteger listener2Calls = new AtomicInteger(0);
        
        ModerationService.ModerationListener listener1 = new ModerationService.ModerationListener() {
            @Override
            public void onBanExecuted(ModerationService.NetworkBan ban, String serverId) {
                listener1Calls.incrementAndGet();
            }
        };
        
        ModerationService.ModerationListener listener2 = new ModerationService.ModerationListener() {
            @Override
            public void onBanExecuted(ModerationService.NetworkBan ban, String serverId) {
                listener2Calls.incrementAndGet();
            }
        };
        
        moderationService.registerModerationListener(listener1);
        moderationService.registerModerationListener(listener2);
        
        // When
        ModerationService.NetworkBan ban = TestUtils.createTestBan();
        CompletableFuture<Void> result = moderationService.executeBan(ban);
        TestUtils.waitFor(result);
        
        // Then
        assertEquals(1, listener1Calls.get(), "First listener should be called once");
        assertEquals(1, listener2Calls.get(), "Second listener should be called once");
    }
    
    @Test
    void testUnregisterListener() throws Exception {
        // Given
        AtomicInteger listenerCalls = new AtomicInteger(0);
        
        ModerationService.ModerationListener listener = new ModerationService.ModerationListener() {
            @Override
            public void onBanExecuted(ModerationService.NetworkBan ban, String serverId) {
                listenerCalls.incrementAndGet();
            }
        };
        
        moderationService.registerModerationListener(listener);
        
        // Execute first ban - should trigger listener
        ModerationService.NetworkBan ban1 = TestUtils.createTestBan();
        TestUtils.waitFor(moderationService.executeBan(ban1));
        assertEquals(1, listenerCalls.get(), "Listener should be called once");
        
        // Unregister listener
        moderationService.unregisterModerationListener(listener);
        
        // Execute second ban - should not trigger listener
        ModerationService.NetworkBan ban2 = TestUtils.createTestBan();
        TestUtils.waitFor(moderationService.executeBan(ban2));
        
        // Then
        assertEquals(1, listenerCalls.get(), "Listener should not be called after unregistering");
    }
    
    @Test
    void testNetworkBanProperties() {
        // Test temporary ban
        ModerationService.NetworkBan tempBan = new ModerationService.NetworkBan(
                TestUtils.TEST_PLAYER_UUID, "Temp ban", TestUtils.TEST_MODERATOR_UUID, Duration.ofMinutes(30));
        
        assertEquals(TestUtils.TEST_PLAYER_UUID, tempBan.getPlayerId());
        assertEquals("Temp ban", tempBan.getReason());
        assertEquals(TestUtils.TEST_MODERATOR_UUID, tempBan.getModeratorId());
        assertFalse(tempBan.isPermanent());
        assertTrue(tempBan.isActive());
        assertNotNull(tempBan.getExpiresAt());
        
        // Test permanent ban
        ModerationService.NetworkBan permBan = new ModerationService.NetworkBan(
                TestUtils.TEST_PLAYER_UUID, "Perm ban", TestUtils.TEST_MODERATOR_UUID);
        
        assertTrue(permBan.isPermanent());
        assertTrue(permBan.isActive());
        assertNull(permBan.getExpiresAt());
    }
    
    @Test
    void testNetworkMuteProperties() {
        // Test temporary mute
        ModerationService.NetworkMute tempMute = new ModerationService.NetworkMute(
                TestUtils.TEST_PLAYER_UUID, "Temp mute", TestUtils.TEST_MODERATOR_UUID, Duration.ofMinutes(10));
        
        assertEquals(TestUtils.TEST_PLAYER_UUID, tempMute.getPlayerId());
        assertEquals("Temp mute", tempMute.getReason());
        assertEquals(TestUtils.TEST_MODERATOR_UUID, tempMute.getModeratorId());
        assertFalse(tempMute.isPermanent());
        assertTrue(tempMute.isActive());
        assertNotNull(tempMute.getExpiresAt());
        
        // Test permanent mute
        ModerationService.NetworkMute permMute = new ModerationService.NetworkMute(
                TestUtils.TEST_PLAYER_UUID, "Perm mute", TestUtils.TEST_MODERATOR_UUID);
        
        assertTrue(permMute.isPermanent());
        assertTrue(permMute.isActive());
        assertNull(permMute.getExpiresAt());
    }
}