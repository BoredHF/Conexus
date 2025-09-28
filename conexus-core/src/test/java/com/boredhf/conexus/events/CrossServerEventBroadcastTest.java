package com.boredhf.conexus.events;

import com.boredhf.conexus.communication.InMemoryMessagingService;
import com.boredhf.conexus.events.types.ServerStatusEvent;
import com.boredhf.conexus.events.types.ServerStatusEvent.ServerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for cross-server event broadcasting functionality.
 * 
 * @author BoredHF 
 * @since 1.0.0
 */
public class CrossServerEventBroadcastTest {
    
    private InMemoryMessagingService messagingServiceA;
    private InMemoryMessagingService messagingServiceB;
    private CrossServerEventService eventServiceA;
    private CrossServerEventService eventServiceB;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create two messaging services (simulating two servers)
        messagingServiceA = new InMemoryMessagingService("server-a");
        messagingServiceB = new InMemoryMessagingService("server-b");
        
        // Connect them so they can communicate
        messagingServiceA.addPeer(messagingServiceB);
        messagingServiceB.addPeer(messagingServiceA);
        
        // Create cross-server event services
        eventServiceA = new CrossServerEventService("server-a", messagingServiceA);
        eventServiceB = new CrossServerEventService("server-b", messagingServiceB);
        
        // Initialize services
        eventServiceA.initialize().get(5, TimeUnit.SECONDS);
        eventServiceB.initialize().get(5, TimeUnit.SECONDS);
        
        System.out.println("Test setup complete - servers initialized");
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (eventServiceA != null) {
            eventServiceA.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (eventServiceB != null) {
            eventServiceB.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (messagingServiceA != null) {
            messagingServiceA.shutdown().get(5, TimeUnit.SECONDS);
        }
        if (messagingServiceB != null) {
            messagingServiceB.shutdown().get(5, TimeUnit.SECONDS);
        }
        System.out.println("Test teardown complete");
    }
    
    @Test
    public void testCrossServerEventBroadcast() throws Exception {
        System.out.println("Starting cross-server event broadcast test...");
        
        // Prepare to receive events on server B
        CountDownLatch eventReceived = new CountDownLatch(1);
        AtomicReference<ServerStatusEvent> receivedEvent = new AtomicReference<>();
        
        eventServiceB.registerEventListener(ServerStatusEvent.class, event -> {
            System.out.println("Server B received event: " + event);
            receivedEvent.set(event);
            eventReceived.countDown();
        });
        
        // Give listeners time to register
        Thread.sleep(100);
        
        // Create and broadcast event from server A
        ServerStatusEvent originalEvent = new ServerStatusEvent("server-a", ServerStatus.ONLINE, "Server is now running");
        System.out.println("Server A broadcasting event: " + originalEvent);
        
        CompletableFuture<Void> broadcastFuture = eventServiceA.broadcastEvent(originalEvent, EventService.EventPriority.NORMAL);
        broadcastFuture.get(10, TimeUnit.SECONDS);
        
        System.out.println("Broadcast completed, waiting for event to be received...");
        
        // Wait for the event to be received on server B
        assertTrue(eventReceived.await(15, TimeUnit.SECONDS), "Event should be received within 15 seconds");
        
        // Verify the received event
        ServerStatusEvent received = receivedEvent.get();
        assertNotNull(received, "Received event should not be null");
        assertEquals("server-a", received.getSourceServerId(), "Source server ID should match");
        assertEquals(ServerStatus.ONLINE, received.getStatus(), "Status should match");
        assertEquals("Server is now running", received.getReason(), "Message should match");
        
        System.out.println("Cross-server event broadcast test completed successfully!");
    }
    
    @Test
    public void testEventLoopPrevention() throws Exception {
        System.out.println("Starting event loop prevention test...");
        
        // Setup event listeners to check for network loopbacks
        CountDownLatch crossServerEventsReceivedA = new CountDownLatch(1);
        CountDownLatch crossServerEventsReceivedB = new CountDownLatch(1);
        
        // Count cross-server events only (events originating from different servers)
        eventServiceA.registerEventListener(ServerStatusEvent.class, event -> {
            if (!event.getSourceServerId().equals("server-a")) {
                System.out.println("Server A received cross-server event from: " + event.getSourceServerId());
                crossServerEventsReceivedA.countDown();
            }
        });
        
        eventServiceB.registerEventListener(ServerStatusEvent.class, event -> {
            if (!event.getSourceServerId().equals("server-b")) {
                System.out.println("Server B received cross-server event from: " + event.getSourceServerId());
                crossServerEventsReceivedB.countDown();
            }
        });
        
        // Give listeners time to register
        Thread.sleep(100);
        
        // Broadcast event from server A
        ServerStatusEvent eventFromA = new ServerStatusEvent("server-a", ServerStatus.MAINTENANCE, "Server A maintenance");
        eventServiceA.broadcastEvent(eventFromA).get(10, TimeUnit.SECONDS);
        
        // Broadcast event from server B
        ServerStatusEvent eventFromB = new ServerStatusEvent("server-b", ServerStatus.ONLINE, "Server B running");
        eventServiceB.broadcastEvent(eventFromB).get(10, TimeUnit.SECONDS);
        
        // Server B should receive the event from Server A
        assertTrue(crossServerEventsReceivedB.await(10, TimeUnit.SECONDS), "Server B should receive cross-server event from A");
        
        // Server A should receive the event from Server B
        assertTrue(crossServerEventsReceivedA.await(10, TimeUnit.SECONDS), "Server A should receive cross-server event from B");
        
        System.out.println("Event loop prevention test completed successfully!");
    }
    
    @Test 
    public void testMultipleEventTypes() throws Exception {
        System.out.println("Starting multiple event types test...");
        
        // Test with different status events
        CountDownLatch eventsReceived = new CountDownLatch(3);
        AtomicInteger eventCount = new AtomicInteger(0);
        
        eventServiceB.registerEventListener(ServerStatusEvent.class, event -> {
            System.out.println("Received event #" + eventCount.incrementAndGet() + ": " + event.getStatus());
            eventsReceived.countDown();
        });
        
        Thread.sleep(100);
        
        // Broadcast multiple events with different statuses
        eventServiceA.broadcastEvent(new ServerStatusEvent("server-a", ServerStatus.STARTING, "Starting up")).get();
        eventServiceA.broadcastEvent(new ServerStatusEvent("server-a", ServerStatus.ONLINE, "Now running")).get();
        eventServiceA.broadcastEvent(new ServerStatusEvent("server-a", ServerStatus.SHUTTING_DOWN, "Shutting down")).get();
        
        assertTrue(eventsReceived.await(15, TimeUnit.SECONDS), "Should receive all 3 events");
        assertEquals(3, eventCount.get(), "Should have received exactly 3 events");
        
        System.out.println("Multiple event types test completed successfully!");
    }
}