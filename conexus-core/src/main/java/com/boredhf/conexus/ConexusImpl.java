package com.boredhf.conexus;

import com.boredhf.conexus.communication.MessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import com.boredhf.conexus.data.PlayerDataService;
import com.boredhf.conexus.events.EventService;
import com.boredhf.conexus.events.CrossServerEventService;
import com.boredhf.conexus.moderation.ModerationService;
import com.boredhf.conexus.transport.TransportProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of the Conexus interface.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class ConexusImpl implements Conexus {
    
    private final String serverId;
    private final TransportProvider transportProvider;
    private final MessagingService messagingService;
    private final PlayerDataService playerDataService;
    private final EventService eventService;
    private final ModerationService moderationService;
    
    public ConexusImpl(String serverId, TransportProvider transportProvider, MessagingService messagingService) {
        this.serverId = serverId;
        this.transportProvider = transportProvider;
        this.messagingService = messagingService;
        
        // Initialize services with full implementations
        MessageSerializer serializer = new com.boredhf.conexus.communication.MessageSerializer();
        // Temporarily disable PlayerDataService for testing
        this.playerDataService = null; // new DefaultPlayerDataService(serverId, transportProvider, serializer);
        this.eventService = new CrossServerEventService(serverId, messagingService);
        this.moderationService = new com.boredhf.conexus.moderation.DefaultModerationService(serverId);
    }
    
    @Override
    public MessagingService getMessagingService() {
        return messagingService;
    }
    
    @Override
    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }
    
    @Override
    public EventService getEventService() {
        return eventService;
    }
    
    @Override
    public ModerationService getModerationService() {
        return moderationService;
    }
    
    @Override
    public String getServerId() {
        return serverId;
    }
    
    @Override
    public TransportProvider getTransportProvider() {
        return transportProvider;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return transportProvider.connect()
                .thenCompose(v -> {
                    // Initialize messaging service if it has an initialize method
                    try {
                        if (messagingService.getClass().getMethod("initialize") != null) {
                            return ((CompletableFuture<Void>) messagingService.getClass().getMethod("initialize").invoke(messagingService));
                        }
                    } catch (Exception e) {
                        // Method doesn't exist or failed, continue
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(v -> {
                    // Initialize cross-server event service
                    try {
                        if (eventService instanceof CrossServerEventService) {
                            return ((CrossServerEventService) eventService).initialize();
                        }
                    } catch (Exception e) {
                        // Failed to initialize event service, continue
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        CompletableFuture<Void> messagingShutdown = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> playerDataShutdown = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> eventServiceShutdown = CompletableFuture.completedFuture(null);
        
        try {
            if (messagingService.getClass().getMethod("shutdown") != null) {
                messagingShutdown = ((CompletableFuture<Void>) messagingService.getClass().getMethod("shutdown").invoke(messagingService));
            }
        } catch (Exception e) {
            // Method doesn't exist or failed, continue
        }
        
        // Temporarily disabled PlayerDataService
        // try {
        //     if (playerDataService instanceof DefaultPlayerDataService) {
        //         playerDataShutdown = ((DefaultPlayerDataService) playerDataService).shutdown();
        //     }
        // } catch (Exception e) {
        //     // Failed to shutdown player data service, continue
        // }
        
        try {
            if (eventService instanceof CrossServerEventService) {
                eventServiceShutdown = ((CrossServerEventService) eventService).shutdown();
            }
        } catch (Exception e) {
            // Failed to shutdown event service, continue
        }
        
        return CompletableFuture.allOf(messagingShutdown, eventServiceShutdown)
                .thenCompose(v -> transportProvider.disconnect());
    }
    
    @Override
    public boolean isConnected() {
        return transportProvider.isConnected();
    }
}