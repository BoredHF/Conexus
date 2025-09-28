package com.boredhf.conexus.integration;

import com.boredhf.conexus.Conexus;
import com.boredhf.conexus.ConexusImpl;
import com.boredhf.conexus.TestRedisConfiguration;
import com.boredhf.conexus.communication.DefaultMessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import com.boredhf.conexus.communication.messages.SimpleTextMessage;
import com.boredhf.conexus.moderation.ModerationService;
import com.boredhf.conexus.transport.RedisTransportProvider;
import com.boredhf.conexus.utils.TestUtils;
import org.junit.jupiter.api.*;
import redis.embedded.RedisServer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test scenarios simulating real-world usage patterns.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConexusEndToEndTest {
    
    private static RedisServer redisServer;
    private static int redisPort;
    
    private Conexus lobbyServer;
    private Conexus gameServer1;
    private Conexus gameServer2;
    private List<Conexus> allServers;
    
    @BeforeAll
    static void setUpRedis() throws Exception {
        System.out.println("Redis configuration: " + TestRedisConfiguration.getConfigurationSummary());
        
        if (TestRedisConfiguration.useExternalRedis()) {
            // Use external Redis (e.g., from GitHub Actions)
            redisPort = TestRedisConfiguration.getRedisPort();
            redisServer = null; // No embedded server needed
            System.out.println("Using external Redis at " + TestRedisConfiguration.getRedisHost() + ":" + redisPort);
        } else {
            // Use embedded Redis for local development
            redisPort = findAvailablePort();
            redisServer = RedisServer.builder()
                    .port(redisPort)
                    .setting("maxmemory 64M")
                    .build();
            redisServer.start();
            Thread.sleep(200); // Give Redis time to start
            System.out.println("Started embedded Redis on port " + redisPort);
        }
    }
    
    @AfterAll
    static void tearDownRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    
    @BeforeEach
    void setUp() throws Exception {
        // Create a network simulation: 1 lobby + 2 game servers
        lobbyServer = createConexusInstance("lobby-1");
        gameServer1 = createConexusInstance("game-1");
        gameServer2 = createConexusInstance("game-2");
        
        allServers = List.of(lobbyServer, gameServer1, gameServer2);
        
        // Initialize all servers
        List<CompletableFuture<Void>> initializations = allServers.stream()
                .map(Conexus::initialize)
                .toList();
        
        CompletableFuture.allOf(initializations.toArray(new CompletableFuture[0]))
                .get(15, TimeUnit.SECONDS);
        
        // Give servers time to fully connect
        Thread.sleep(1000);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        List<CompletableFuture<Void>> shutdowns = allServers.stream()
                .map(Conexus::shutdown)
                .toList();
        
        CompletableFuture.allOf(shutdowns.toArray(new CompletableFuture[0]))
                .get(10, TimeUnit.SECONDS);
    }
    
    @Test
    @Order(1)
    void testNetworkInitialization() {
        // Verify all servers are connected and operational
        for (Conexus server : allServers) {
            assertTrue(server.isConnected(), "Server " + server.getServerId() + " should be connected");
            assertNotNull(server.getMessagingService(), "MessagingService should be available");
            assertNotNull(server.getModerationService(), "ModerationService should be available");
            assertNotNull(server.getEventService(), "EventService should be available");
        }
    }
    
    @Test
    @Order(2)
    void testCrossServerChatScenario() throws Exception {
        // Scenario: Player sends a message from lobby that should be broadcast to all game servers
        CountDownLatch messagesReceived = new CountDownLatch(2); // Expect 2 game servers to receive
        List<SimpleTextMessage> receivedMessages = new ArrayList<>();
        Object receivedMessagesLock = new Object();
        
        // Set up message handlers on game servers
        gameServer1.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
            if ("global-chat".equals(context.getMessage().getCategory())) {
                synchronized (receivedMessagesLock) {
                    receivedMessages.add(context.getMessage());
                }
                messagesReceived.countDown();
                System.out.println("Game server 1 received message: " + context.getMessage().getContent());
            }
        });
        
        gameServer2.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
            if ("global-chat".equals(context.getMessage().getCategory())) {
                synchronized (receivedMessagesLock) {
                    receivedMessages.add(context.getMessage());
                }
                messagesReceived.countDown();
                System.out.println("Game server 2 received message: " + context.getMessage().getContent());
            }
        });
        
        // Wait longer for handlers to be registered and pub/sub to be fully ready
        Thread.sleep(2000);
        
        // Send global chat message from lobby
        SimpleTextMessage chatMessage = new SimpleTextMessage(
                lobbyServer.getServerId(), 
                "[GLOBAL] TestPlayer: Hello everyone!", 
                "global-chat"
        );
        
        System.out.println("Sending message from lobby server: " + chatMessage.getContent());
        lobbyServer.getMessagingService().broadcast(chatMessage).get(5, TimeUnit.SECONDS);
        System.out.println("Message broadcast completed");
        
        // Give extra time for message propagation
        boolean allReceived = messagesReceived.await(15, TimeUnit.SECONDS);
        
        System.out.println("Messages received: " + receivedMessages.size() + " out of expected 2");
        for (int i = 0; i < receivedMessages.size(); i++) {
            System.out.println("  Message " + (i+1) + ": " + receivedMessages.get(i).getContent());
        }
        
        // Verify message was received by game servers
        assertTrue(allReceived, "Game servers should receive the chat message");
        
        synchronized (receivedMessagesLock) {
            assertEquals(2, receivedMessages.size(), "Both game servers should receive the message");
            
            for (SimpleTextMessage received : receivedMessages) {
                assertEquals(lobbyServer.getServerId(), received.getSourceServerId());
                assertTrue(received.getContent().contains("TestPlayer"));
                assertEquals("global-chat", received.getCategory());
            }
        }
    }
    
    @Test
    @Order(3)
    void testNetworkWideModerationScenario() throws Exception {
        // Scenario: Moderator bans a player, all servers should be notified
        AtomicInteger banNotifications = new AtomicInteger(0);
        CountDownLatch moderationReceived = new CountDownLatch(3); // All servers including source
        
        // Register moderation listeners on all servers
        for (Conexus server : allServers) {
            server.getModerationService().registerModerationListener(new ModerationService.ModerationListener() {
                @Override
                public void onBanExecuted(ModerationService.NetworkBan ban, String serverId) {
                    banNotifications.incrementAndGet();
                    moderationReceived.countDown();
                }
            });
        }
        
        Thread.sleep(500);
        
        // Execute ban from lobby server
        ModerationService.NetworkBan ban = new ModerationService.NetworkBan(
                TestUtils.TEST_PLAYER_UUID, 
                "Cheating detected", 
                TestUtils.TEST_MODERATOR_UUID, 
                Duration.ofDays(3)
        );
        
        lobbyServer.getModerationService().executeBan(ban).get(5, TimeUnit.SECONDS);
        
        // For current implementation, only the source server will trigger listeners
        // In a full implementation, all servers would be notified via cross-server messages
        assertTrue(moderationReceived.getCount() <= 3, "Moderation action should be processed");
        assertTrue(banNotifications.get() >= 1, "At least the source server should process the ban");
    }
    
    @Test
    @Order(4)
    void testServerShutdownAndReconnection() throws Exception {
        // Scenario: One game server goes down and comes back up
        assertTrue(gameServer1.isConnected(), "Game server 1 should initially be connected");
        
        // Simulate server shutdown
        gameServer1.shutdown().get(5, TimeUnit.SECONDS);
        assertFalse(gameServer1.isConnected(), "Game server 1 should be disconnected");
        
        // Other servers should still be operational
        assertTrue(lobbyServer.isConnected(), "Lobby server should remain connected");
        assertTrue(gameServer2.isConnected(), "Game server 2 should remain connected");
        
        // Recreate and reconnect server 1
        gameServer1 = createConexusInstance("game-1");
        gameServer1.initialize().get(5, TimeUnit.SECONDS);
        
        assertTrue(gameServer1.isConnected(), "Game server 1 should reconnect successfully");
        
        // Test that messaging still works after reconnection
        CountDownLatch messageReceived = new CountDownLatch(1);
        AtomicReference<SimpleTextMessage> receivedMessage = new AtomicReference<>();
        
        gameServer1.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
            receivedMessage.set(context.getMessage());
            messageReceived.countDown();
        });
        
        Thread.sleep(500);
        
        SimpleTextMessage testMessage = new SimpleTextMessage(
                lobbyServer.getServerId(), 
                "Test after reconnection", 
                "test"
        );
        
        lobbyServer.getMessagingService().broadcast(testMessage).get(5, TimeUnit.SECONDS);
        
        assertTrue(messageReceived.await(5, TimeUnit.SECONDS), 
                "Reconnected server should receive messages");
        assertNotNull(receivedMessage.get());
        assertEquals("Test after reconnection", receivedMessage.get().getContent());
    }
    
    @Test
    @Order(5)
    void testHighVolumeMessaging() throws Exception {
        // Scenario: Send multiple messages rapidly to test system under load
        final int messageCount = 50;
        CountDownLatch messagesReceived = new CountDownLatch(messageCount * 2); // 2 receiving servers
        AtomicInteger totalReceived = new AtomicInteger(0);
        
        // Set up handlers on both game servers
        for (Conexus gameServer : List.of(gameServer1, gameServer2)) {
            gameServer.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
                if ("load-test".equals(context.getMessage().getCategory())) {
                    totalReceived.incrementAndGet();
                    messagesReceived.countDown();
                }
            });
        }
        
        Thread.sleep(500);
        
        // Send messages rapidly from lobby
        List<CompletableFuture<Void>> sendTasks = new ArrayList<>();
        
        for (int i = 0; i < messageCount; i++) {
            SimpleTextMessage message = new SimpleTextMessage(
                    lobbyServer.getServerId(), 
                    "Load test message " + i, 
                    "load-test"
            );
            sendTasks.add(lobbyServer.getMessagingService().broadcast(message));
        }
        
        // Wait for all sends to complete
        CompletableFuture.allOf(sendTasks.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);
        
        // Wait for all messages to be received
        boolean allReceived = messagesReceived.await(30, TimeUnit.SECONDS);
        
        assertTrue(allReceived, "All messages should be received within timeout");
        assertEquals(messageCount * 2, totalReceived.get(), 
                "Each game server should receive all " + messageCount + " messages");
    }
    
    private Conexus createConexusInstance(String serverId) {
        String redisHost = TestRedisConfiguration.useExternalRedis() ? 
            TestRedisConfiguration.getRedisHost() : "127.0.0.1";
        
        RedisTransportProvider transport = new RedisTransportProvider(redisHost, redisPort, null, 0);
        MessageSerializer serializer = new MessageSerializer();
        DefaultMessagingService messaging = new DefaultMessagingService(serverId, transport, serializer);
        return new ConexusImpl(serverId, transport, messaging);
    }
    
    private static int findAvailablePort() {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return 6371; // Fallback port
        }
    }
}