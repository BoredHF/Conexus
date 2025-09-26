package com.boredhf.conexus.events;

import com.boredhf.conexus.Conexus;
import com.boredhf.conexus.ConexusImpl;
import com.boredhf.conexus.communication.DefaultMessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import com.boredhf.conexus.events.types.ServerStatusEvent;
import com.boredhf.conexus.transport.RedisTransportProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Debug test to diagnose event broadcasting issues.
 */
class EventBroadcastDebugTest {
    
    private RedisServer redisServer;
    private int redisPort;
    private Conexus server1;
    private Conexus server2;
    
    @BeforeEach
    void setUp() throws Exception {
        redisPort = findAvailablePort();
        redisServer = RedisServer.builder()
                .port(redisPort)
                .setting("maxmemory 32M")
                .build();
        redisServer.start();
        
        Thread.sleep(100);
        
        server1 = createConexusInstance("debug-server-1");
        server2 = createConexusInstance("debug-server-2");
        
        server1.initialize().get(5, TimeUnit.SECONDS);
        server2.initialize().get(5, TimeUnit.SECONDS);
        
        System.out.println("Both servers initialized");
        Thread.sleep(1000); // Give extra time for initialization
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
    void debugEventBroadcasting() throws Exception {
        AtomicBoolean eventReceived = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        
        // Register listener on server2 with detailed logging
        server2.getEventService().registerEventListener(ServerStatusEvent.class, event -> {
            System.out.println("=== EVENT RECEIVED ON SERVER2 ===");
            System.out.println("Event: " + event);
            System.out.println("Source Server: " + event.getSourceServerId());
            System.out.println("Status: " + event.getStatus());
            System.out.println("Reason: " + event.getReason());
            eventReceived.set(true);
            latch.countDown();
        });
        
        // Check if the event service is the right type
        System.out.println("Server1 event service: " + server1.getEventService().getClass().getSimpleName());
        System.out.println("Server2 event service: " + server2.getEventService().getClass().getSimpleName());
        
        // Test messaging service directly first
        System.out.println("=== Testing direct messaging ===");
        AtomicBoolean directMessageReceived = new AtomicBoolean(false);
        server2.getMessagingService().registerHandler(NetworkEventMessage.class, context -> {
            System.out.println("NetworkEventMessage received directly: " + context.getMessage());
            directMessageReceived.set(true);
        });
        
        // Test with a simple text message first
        AtomicBoolean simpleMessageReceived = new AtomicBoolean(false);
        server2.getMessagingService().registerHandler(com.boredhf.conexus.communication.messages.SimpleTextMessage.class, context -> {
            System.out.println("SimpleTextMessage received: " + context.getMessage());
            simpleMessageReceived.set(true);
        });
        
        Thread.sleep(500); // Allow handlers to register
        
        // Try broadcasting a simple text message first
        System.out.println("Testing simple text message broadcast...");
        com.boredhf.conexus.communication.messages.SimpleTextMessage simpleMsg = 
            new com.boredhf.conexus.communication.messages.SimpleTextMessage("debug-server-1", "Test message", "debug");
        server1.getMessagingService().broadcast(simpleMsg).get(5, TimeUnit.SECONDS);
        Thread.sleep(2000);
        System.out.println("Simple message received: " + simpleMessageReceived.get());
        
        // Try creating and broadcasting NetworkEventMessage directly
        System.out.println("Testing direct NetworkEventMessage broadcast...");
        NetworkEventMessage directEventMsg = new NetworkEventMessage(
            ServerStatusEvent.class,
            new ServerStatusEvent("debug-server-1", ServerStatusEvent.ServerStatus.MAINTENANCE, "Direct test"),
            EventService.EventPriority.NORMAL,
            "debug-server-1"
        );
        server1.getMessagingService().broadcast(directEventMsg).get(5, TimeUnit.SECONDS);
        Thread.sleep(2000);
        System.out.println("Direct NetworkEventMessage received: " + directMessageReceived.get());
        
        Thread.sleep(500); // Allow registration
        
        // Create and broadcast event
        System.out.println("=== Broadcasting event from server1 ===");
        ServerStatusEvent testEvent = new ServerStatusEvent("debug-server-1", 
                                                            ServerStatusEvent.ServerStatus.ONLINE, 
                                                            "Debug test");
        
        System.out.println("Created event: " + testEvent);
        
        CompletableFuture<Void> broadcastResult = server1.getEventService().broadcastEvent(testEvent);
        System.out.println("Broadcast initiated...");
        
        try {
            broadcastResult.get(5, TimeUnit.SECONDS);
            System.out.println("Broadcast completed successfully");
        } catch (Exception e) {
            System.out.println("Broadcast failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Wait for event
        System.out.println("Waiting for event to be received...");
        boolean received = latch.await(10, TimeUnit.SECONDS);
        
        System.out.println("Event received within timeout: " + received);
        System.out.println("Event received flag: " + eventReceived.get());
        
        // Additional debugging - check if CrossServerEventService is configured correctly
        if (server1.getEventService() instanceof CrossServerEventService) {
            CrossServerEventService css1 = (CrossServerEventService) server1.getEventService();
            System.out.println("Server1 CrossServerEventService - Cross-server broadcast enabled: " + css1.isCrossServerBroadcastEnabled());
            System.out.println("Server1 CrossServerEventService - Local processing enabled: " + css1.isLocalEventProcessingEnabled());
        }
        
        if (server2.getEventService() instanceof CrossServerEventService) {
            CrossServerEventService css2 = (CrossServerEventService) server2.getEventService();
            System.out.println("Server2 CrossServerEventService - Cross-server broadcast enabled: " + css2.isCrossServerBroadcastEnabled());
            System.out.println("Server2 CrossServerEventService - Local processing enabled: " + css2.isLocalEventProcessingEnabled());
            System.out.println("Server2 CrossServerEventService - Listener count for ServerStatusEvent: " + css2.getListenerCount(ServerStatusEvent.class));
        }
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
            return 6373;
        }
    }
}