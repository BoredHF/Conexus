package com.boredhf.conexus.moderation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default implementation of ModerationService.
 * This is a basic implementation that will be expanded in future versions.
 * 
 * @since 1.0.0
 */
public class DefaultModerationService implements ModerationService {
    
    private final String serverId;
    private final List<ModerationListener> listeners = new CopyOnWriteArrayList<>();
    
    public DefaultModerationService(String serverId) {
        this.serverId = serverId;
    }
    
    @Override
    public CompletableFuture<Void> executeBan(NetworkBan ban) {
        // Notify listeners
        listeners.forEach(listener -> listener.onBanExecuted(ban, serverId));
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> executeUnban(UUID playerId, UUID moderatorId, String reason) {
        // Notify listeners
        listeners.forEach(listener -> listener.onUnbanExecuted(playerId, moderatorId, reason, serverId));
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> executeKick(NetworkKick kick) {
        // Notify listeners
        listeners.forEach(listener -> listener.onKickExecuted(kick, serverId));
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> executeMute(NetworkMute mute) {
        // Notify listeners
        listeners.forEach(listener -> listener.onMuteExecuted(mute, serverId));
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> executeUnmute(UUID playerId, UUID moderatorId, String reason) {
        // Notify listeners
        listeners.forEach(listener -> listener.onUnmuteExecuted(playerId, moderatorId, reason, serverId));
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Void> executeWarning(NetworkWarning warning) {
        // Notify listeners
        listeners.forEach(listener -> listener.onWarningIssued(warning, serverId));
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public CompletableFuture<Optional<NetworkBan>> getActiveBan(UUID playerId) {
        // TODO: Implement persistent storage
        return CompletableFuture.completedFuture(Optional.empty());
    }
    
    @Override
    public CompletableFuture<Optional<NetworkMute>> getActiveMute(UUID playerId) {
        // TODO: Implement persistent storage
        return CompletableFuture.completedFuture(Optional.empty());
    }
    
    @Override
    public CompletableFuture<List<NetworkWarning>> getWarnings(UUID playerId) {
        // TODO: Implement persistent storage
        return CompletableFuture.completedFuture(List.of());
    }
    
    @Override
    public void registerModerationListener(ModerationListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void unregisterModerationListener(ModerationListener listener) {
        listeners.remove(listener);
    }
}