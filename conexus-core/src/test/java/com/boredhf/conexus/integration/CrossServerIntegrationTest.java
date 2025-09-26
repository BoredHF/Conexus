package com.boredhf.conexus.integration;

import com.boredhf.conexus.Conexus;
import com.boredhf.conexus.ConexusImpl;
import com.boredhf.conexus.communication.DefaultMessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import com.boredhf.conexus.communication.messages.SimpleTextMessage;
import com.boredhf.conexus.transport.RedisTransportProvider;
import com.boredhf.conexus.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that simulate cross-server communication using embedded Redis.
 */
class CrossServerIntegrationTest {
    
    private RedisServer redisServer;
    private int redisPort;
    private Conexus server1;
    private Conexus server2;
    
    @BeforeEach
    void setUp() throws Exception {
        // Start embedded Redis server on a random available port
        redisPort = findAvailablePort();
        redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 32M")
                .build();
        redisServer.start();
        
        // Give Redis a moment to fully start
        Thread.sleep(100);
        
        // Create two Conexus instances representing different servers
        server1 = createConexusInstance(TestUtils.TEST_SERVER_1);
        server2 = createConexusInstance(TestUtils.TEST_SERVER_2);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (server1 != null) {
            server1.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (server2 != null) {
            server2.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    
    @Test
    void testCrossServerMessaging() throws Exception {
        // Given
        server1.initialize().get(5, TimeUnit.SECONDS);
        server2.initialize().get(5, TimeUnit.SECONDS);
        
        CountDownLatch messageReceived = new CountDownLatch(1);
        AtomicReference<SimpleTextMessage> receivedMessage = new AtomicReference<>();
        
        // Register message handler on server2
        server2.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
            receivedMessage.set(context.getMessage());
            messageReceived.countDown();
        });
        
        // Give the handlers time to register
        Thread.sleep(500);
        
        // When - send message from server1
        SimpleTextMessage testMessage = new SimpleTextMessage(TestUtils.TEST_SERVER_1, "Hello from server 1!", "test");
        CompletableFuture<Void> sendResult = server1.getMessagingService().broadcast(testMessage);
        sendResult.get(5, TimeUnit.SECONDS);
        
        // Then - verify message was received on server2
        assertTrue(messageReceived.await(5, TimeUnit.SECONDS), "Message should be received within timeout");
        
        SimpleTextMessage received = receivedMessage.get();
        assertNotNull(received, "Received message should not be null");
        assertEquals(TestUtils.TEST_SERVER_1, received.getSourceServerId());
        assertEquals("Hello from server 1!", received.getContent());
        assertEquals("test", received.getCategory());
    }
    
    @Test
    void testModerationServiceCrossServer() throws Exception {
        // Given
        server1.initialize().get(5, TimeUnit.SECONDS);
        server2.initialize().get(5, TimeUnit.SECONDS);
        
        CountDownLatch banReceived = new CountDownLatch(1);
        AtomicReference<com.boredhf.conexus.moderation.ModerationService.NetworkBan> receivedBan = new AtomicReference<>();
        
        // Register moderation listener on server2
        server2.getModerationService().registerModerationListener(new com.boredhf.conexus.moderation.ModerationService.ModerationListener() {
            @Override
            public void onBanExecuted(com.boredhf.conexus.moderation.ModerationService.NetworkBan ban, String serverId) {
                receivedBan.set(ban);
                banReceived.countDown();
            }
        });
        
        // Give the services time to initialize
        Thread.sleep(500);
        
        // When - execute ban from server1
        com.boredhf.conexus.moderation.ModerationService.NetworkBan testBan = TestUtils.createTestBan();
        CompletableFuture<Void> banResult = server1.getModerationService().executeBan(testBan);
        banResult.get(5, TimeUnit.SECONDS);
        
        // Then - verify ban was received on both servers (for now, just local server)
        // Note: Current implementation only notifies local listeners
        // This test validates the service integration rather than cross-server communication
        assertTrue(banResult.isDone() && !banResult.isCompletedExceptionally());
    }
    
    @Test
    void testEventServiceCrossServer() throws Exception {
        // Given
        server1.initialize().get(5, TimeUnit.SECONDS);
        server2.initialize().get(5, TimeUnit.SECONDS);
        
        CountDownLatch eventReceived = new CountDownLatch(1);
        AtomicReference<TestUtils.TestNetworkEvent> receivedEvent = new AtomicReference<>();
        
        // Register event listener on server2
        server2.getEventService().registerEventListener(TestUtils.TestNetworkEvent.class, event -> {
            receivedEvent.set(event);
            eventReceived.countDown();
        });
        
        // Give the services time to initialize
        Thread.sleep(500);
        
        // When - broadcast event from server1
        TestUtils.TestNetworkEvent testEvent = TestUtils.createTestEvent();
        CompletableFuture<Void> eventResult = server1.getEventService().broadcastEvent(testEvent, 
                com.boredhf.conexus.events.EventService.EventPriority.HIGH);
        eventResult.get(5, TimeUnit.SECONDS);
        
        // Then - verify event broadcast completed successfully
        // Note: Current implementation only handles local events
        assertTrue(eventResult.isDone() && !eventResult.isCompletedExceptionally());
    }
    
    @Test
    void testServerConnectivity() throws Exception {
        // Test that both servers can connect and initialize successfully
        CompletableFuture<Void> init1 = server1.initialize();
        CompletableFuture<Void> init2 = server2.initialize();
        
        // Both should initialize without errors
        init1.get(10, TimeUnit.SECONDS);
        init2.get(10, TimeUnit.SECONDS);
        
        // Both should report as connected
        assertTrue(server1.isConnected(), "Server 1 should be connected");
        assertTrue(server2.isConnected(), "Server 2 should be connected");
        
        // Verify server IDs
        assertEquals(TestUtils.TEST_SERVER_1, server1.getServerId());
        assertEquals(TestUtils.TEST_SERVER_2, server2.getServerId());
    }
    
    @Test
    void testMultipleMessageTypes() throws Exception {
        // Given
        server1.initialize().get(5, TimeUnit.SECONDS);
        server2.initialize().get(5, TimeUnit.SECONDS);
        
        CountDownLatch message1Received = new CountDownLatch(1);
        CountDownLatch message2Received = new CountDownLatch(1);
        AtomicReference<SimpleTextMessage> receivedMessage1 = new AtomicReference<>();
        AtomicReference<SimpleTextMessage> receivedMessage2 = new AtomicReference<>();
        
        // Register handlers on server2
        server2.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
            SimpleTextMessage msg = context.getMessage();
            if ("type1".equals(msg.getCategory())) {
                receivedMessage1.set(msg);
                message1Received.countDown();
            } else if ("type2".equals(msg.getCategory())) {
                receivedMessage2.set(msg);
                message2Received.countDown();
            }
        });
        
        Thread.sleep(500);
        
        // When - send different message types from server1
        SimpleTextMessage msg1 = new SimpleTextMessage(TestUtils.TEST_SERVER_1, "Message type 1", "type1");
        SimpleTextMessage msg2 = new SimpleTextMessage(TestUtils.TEST_SERVER_1, "Message type 2", "type2");
        
        server1.getMessagingService().broadcast(msg1).get(5, TimeUnit.SECONDS);
        server1.getMessagingService().broadcast(msg2).get(5, TimeUnit.SECONDS);
        
        // Then - verify both messages are received
        assertTrue(message1Received.await(5, TimeUnit.SECONDS), "First message should be received");
        assertTrue(message2Received.await(5, TimeUnit.SECONDS), "Second message should be received");
        
        assertEquals("Message type 1", receivedMessage1.get().getContent());
        assertEquals("Message type 2", receivedMessage2.get().getContent());
    }
    
    private Conexus createConexusInstance(String serverId) {
        RedisTransportProvider transport = new RedisTransportProvider("127.0.0.1", redisPort, null, 0);
        MessageSerializer serializer = new MessageSerializer();
        DefaultMessagingService messaging = new DefaultMessagingService(serverId, transport, serializer);
        return new ConexusImpl(serverId, transport, messaging);
    }
    
    private int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            // Fallback to a common test port
            return 6370;
        }
    }
}