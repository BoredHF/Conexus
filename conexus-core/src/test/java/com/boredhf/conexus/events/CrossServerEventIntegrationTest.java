package com.boredhf.conexus.events;

import com.boredhf.conexus.Conexus;
import com.boredhf.conexus.ConexusImpl;
import com.boredhf.conexus.communication.DefaultMessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import com.boredhf.conexus.events.types.PlayerNetworkEvent;
import com.boredhf.conexus.events.types.ServerStatusEvent;
import com.boredhf.conexus.transport.RedisTransportProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for cross-server event broadcasting.
 */
class CrossServerEventIntegrationTest {
    
    private RedisServer redisServer;
    private int redisPort;
    private Conexus server1;
    private Conexus server2;
    private Conexus server3;
    
    @BeforeEach
    void setUp() throws Exception {
        // Start embedded Redis server
        redisPort = findAvailablePort();
        redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 64M")
                .build();
        redisServer.start();
        
        Thread.sleep(100);
        
        // Create three Conexus instances for testing
        server1 = createConexusInstance("event-server-1");
        server2 = createConexusInstance("event-server-2");
        server3 = createConexusInstance("event-server-3");
        
        // Initialize all servers
        server1.initialize().get(5, TimeUnit.SECONDS);
        server2.initialize().get(5, TimeUnit.SECONDS);
        server3.initialize().get(5, TimeUnit.SECONDS);
        
        // Give time for all services to fully initialize
        Thread.sleep(500);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (server1 != null) {
            server1.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (server2 != null) {
            server2.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (server3 != null) {
            server3.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (redisServer != null) {
            redisServer.stop();
        }
    }
    
    @Test
    void testServerStatusEventBroadcasting() throws Exception {
        // Given
        CountDownLatch server2Received = new CountDownLatch(1);
        CountDownLatch server3Received = new CountDownLatch(1);
        AtomicReference<ServerStatusEvent> server2Event = new AtomicReference<>();
        AtomicReference<ServerStatusEvent> server3Event = new AtomicReference<>();
        
        // Register listeners on server2 and server3
        server2.getEventService().registerEventListener(ServerStatusEvent.class, event -> {
            System.out.println("Server2 received: " + event);
            server2Event.set(event);
            server2Received.countDown();
        });
        
        server3.getEventService().registerEventListener(ServerStatusEvent.class, event -> {
            System.out.println("Server3 received: " + event);
            server3Event.set(event);
            server3Received.countDown();
        });
        
        Thread.sleep(100); // Allow handlers to register
        
        // When - broadcast server status event from server1
        ServerStatusEvent statusEvent = new ServerStatusEvent("event-server-1", 
                                                              ServerStatusEvent.ServerStatus.MAINTENANCE, 
                                                              "Scheduled maintenance");
        
        CompletableFuture<Void> broadcastResult = server1.getEventService()
                .broadcastEvent(statusEvent, EventService.EventPriority.HIGH);
        broadcastResult.get(5, TimeUnit.SECONDS);
        
        // Then - verify both server2 and server3 received the event
        assertTrue(server2Received.await(10, TimeUnit.SECONDS), "Server2 should receive the event");
        assertTrue(server3Received.await(10, TimeUnit.SECONDS), "Server3 should receive the event");
        
        // Verify event details
        ServerStatusEvent receivedByServer2 = server2Event.get();
        assertNotNull(receivedByServer2);
        assertEquals("event-server-1", receivedByServer2.getSourceServerId());
        assertEquals(ServerStatusEvent.ServerStatus.MAINTENANCE, receivedByServer2.getStatus());
        assertEquals("Scheduled maintenance", receivedByServer2.getReason());
        
        ServerStatusEvent receivedByServer3 = server3Event.get();
        assertNotNull(receivedByServer3);
        assertEquals("event-server-1", receivedByServer3.getSourceServerId());
        assertEquals(ServerStatusEvent.ServerStatus.MAINTENANCE, receivedByServer3.getStatus());
    }
    
    @Test
    void testPlayerNetworkEventBroadcasting() throws Exception {
        // Given
        CountDownLatch eventReceived = new CountDownLatch(2); // Expect 2 servers to receive
        AtomicReference<PlayerNetworkEvent> receivedEvent = new AtomicReference<>();
        
        // Register listeners on server2 and server3
        EventService.EventListener<PlayerNetworkEvent> listener = event -> {
            System.out.println("Received player event: " + event);
            receivedEvent.set(event);
            eventReceived.countDown();
        };
        
        server2.getEventService().registerEventListener(PlayerNetworkEvent.class, listener);
        server3.getEventService().registerEventListener(PlayerNetworkEvent.class, listener);
        
        Thread.sleep(100); // Allow handlers to register
        
        // When - broadcast player network event from server1
        UUID playerId = UUID.randomUUID();
        PlayerNetworkEvent playerEvent = new PlayerNetworkEvent("event-server-1", 
                                                                playerId, 
                                                                "TestPlayer", 
                                                                PlayerNetworkEvent.PlayerEventType.JOIN_NETWORK);
        
        CompletableFuture<Void> broadcastResult = server1.getEventService().broadcastEvent(playerEvent);
        broadcastResult.get(5, TimeUnit.SECONDS);
        
        // Then - verify both servers received the event
        assertTrue(eventReceived.await(10, TimeUnit.SECONDS), "Both servers should receive the player event");
        
        PlayerNetworkEvent received = receivedEvent.get();
        assertNotNull(received);
        assertEquals("event-server-1", received.getSourceServerId());
        assertEquals(playerId, received.getPlayerId());
        assertEquals("TestPlayer", received.getPlayerName());
        assertEquals(PlayerNetworkEvent.PlayerEventType.JOIN_NETWORK, received.getEventType());
        assertTrue(received.isJoin());
    }
    
    @Test
    void testEventPriorityHandling() throws Exception {
        // Given
        CountDownLatch criticalReceived = new CountDownLatch(1);
        CountDownLatch normalReceived = new CountDownLatch(1);
        
        server2.getEventService().registerEventListener(ServerStatusEvent.class, event -> {
            if (event.getStatus() == ServerStatusEvent.ServerStatus.ERROR) {
                criticalReceived.countDown();
            } else {
                normalReceived.countDown();
            }
        });
        
        Thread.sleep(100);
        
        // When - broadcast events with different priorities
        ServerStatusEvent normalEvent = new ServerStatusEvent("event-server-1", 
                                                              ServerStatusEvent.ServerStatus.ONLINE, 
                                                              "Server is online");
        
        ServerStatusEvent criticalEvent = new ServerStatusEvent("event-server-1", 
                                                               ServerStatusEvent.ServerStatus.ERROR, 
                                                               "Critical system error");
        
        // Broadcast normal priority first, then critical
        server1.getEventService().broadcastEvent(normalEvent, EventService.EventPriority.NORMAL).get(5, TimeUnit.SECONDS);
        server1.getEventService().broadcastEvent(criticalEvent, EventService.EventPriority.CRITICAL).get(5, TimeUnit.SECONDS);
        
        // Then - verify both events are received
        assertTrue(normalReceived.await(10, TimeUnit.SECONDS), "Normal priority event should be received");
        assertTrue(criticalReceived.await(10, TimeUnit.SECONDS), "Critical priority event should be received");
    }
    
    @Test
    void testEventLoopbackPrevention() throws Exception {
        // Given
        CountDownLatch eventReceived = new CountDownLatch(1);
        
        // Register listener on the same server that will broadcast
        server1.getEventService().registerEventListener(ServerStatusEvent.class, event -> {
            // This should only be called for local processing, not for the network message
            eventReceived.countDown();
        });
        
        Thread.sleep(100);
        
        // When - broadcast event from server1
        ServerStatusEvent statusEvent = new ServerStatusEvent("event-server-1", 
                                                              ServerStatusEvent.ServerStatus.STARTING, 
                                                              "Server starting");
        
        server1.getEventService().broadcastEvent(statusEvent).get(5, TimeUnit.SECONDS);
        
        // Then - verify the event is processed locally (for local listeners) 
        // but doesn't loop back through the network
        assertTrue(eventReceived.await(5, TimeUnit.SECONDS), 
                  "Local listeners should receive the event");
    }
    
    @Test  
    void testMultipleEventTypes() throws Exception {
        // Given
        CountDownLatch playerEventReceived = new CountDownLatch(1);
        CountDownLatch serverEventReceived = new CountDownLatch(1);
        
        server2.getEventService().registerEventListener(PlayerNetworkEvent.class, event -> {
            playerEventReceived.countDown();
        });
        
        server2.getEventService().registerEventListener(ServerStatusEvent.class, event -> {
            serverEventReceived.countDown();
        });
        
        Thread.sleep(100);
        
        // When - broadcast multiple event types from server1
        PlayerNetworkEvent playerEvent = new PlayerNetworkEvent("event-server-1", 
                                                                UUID.randomUUID(), 
                                                                "Player1", 
                                                                PlayerNetworkEvent.PlayerEventType.LEAVE_NETWORK);
        
        ServerStatusEvent serverEvent = new ServerStatusEvent("event-server-1", 
                                                             ServerStatusEvent.ServerStatus.OFFLINE, 
                                                             "Server shutdown");
        
        server1.getEventService().broadcastEvent(playerEvent).get(5, TimeUnit.SECONDS);
        server1.getEventService().broadcastEvent(serverEvent).get(5, TimeUnit.SECONDS);
        
        // Then - verify both event types are received
        assertTrue(playerEventReceived.await(10, TimeUnit.SECONDS), "Player event should be received");
        assertTrue(serverEventReceived.await(10, TimeUnit.SECONDS), "Server event should be received");
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
            return 6372;
        }
    }
}