package com.boredhf.conexus.moderation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for network-wide moderation actions across multiple servers.
 * 
 * <p>The ModerationService provides methods for executing moderation actions
 * that affect players across the entire network, including bans, kicks, mutes,
 * warnings, and audit logging.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public interface ModerationService {
    
    /**
     * Executes a network-wide ban.
     * The player will be banned from all servers in the network.
     * 
     * @param ban the ban to execute
     * @return a CompletableFuture that will be completed when the ban is executed
     */
    CompletableFuture<Void> executeBan(NetworkBan ban);
    
    /**
     * Removes a network-wide ban (unban).
     * 
     * @param playerId the UUID of the player to unban
     * @param moderatorId the UUID of the moderator performing the unban
     * @param reason the reason for unbanning
     * @return a CompletableFuture that will be completed when the unban is executed
     */
    CompletableFuture<Void> executeUnban(UUID playerId, UUID moderatorId, String reason);
    
    /**
     * Executes a network-wide kick.
     * The player will be kicked from all servers they are currently on.
     * 
     * @param kick the kick to execute
     * @return a CompletableFuture that will be completed when the kick is executed
     */
    CompletableFuture<Void> executeKick(NetworkKick kick);
    
    /**
     * Executes a network-wide mute.
     * The player will be muted on all servers in the network.
     * 
     * @param mute the mute to execute
     * @return a CompletableFuture that will be completed when the mute is executed
     */
    CompletableFuture<Void> executeMute(NetworkMute mute);
    
    /**
     * Removes a network-wide mute (unmute).
     * 
     * @param playerId the UUID of the player to unmute
     * @param moderatorId the UUID of the moderator performing the unmute
     * @param reason the reason for unmuting
     * @return a CompletableFuture that will be completed when the unmute is executed
     */
    CompletableFuture<Void> executeUnmute(UUID playerId, UUID moderatorId, String reason);
    
    /**
     * Issues a network-wide warning to a player.
     * 
     * @param warning the warning to issue
     * @return a CompletableFuture that will be completed when the warning is issued
     */
    CompletableFuture<Void> executeWarning(NetworkWarning warning);
    
    /**
     * Checks if a player is currently banned.
     * 
     * @param playerId the UUID of the player
     * @return a CompletableFuture that will be completed with the active ban, or empty if not banned
     */
    CompletableFuture<Optional<NetworkBan>> getActiveBan(UUID playerId);
    
    /**
     * Checks if a player is currently muted.
     * 
     * @param playerId the UUID of the player
     * @return a CompletableFuture that will be completed with the active mute, or empty if not muted
     */
    CompletableFuture<Optional<NetworkMute>> getActiveMute(UUID playerId);
    
    /**
     * Gets all warnings for a player.
     * 
     * @param playerId the UUID of the player
     * @return a CompletableFuture that will be completed with a list of warnings
     */
    CompletableFuture<List<NetworkWarning>> getWarnings(UUID playerId);
    
    /**
     * Registers a moderation action listener to be notified of moderation events.
     * 
     * @param listener the listener to register
     */
    void registerModerationListener(ModerationListener listener);
    
    /**
     * Unregisters a moderation action listener.
     * 
     * @param listener the listener to unregister
     */
    void unregisterModerationListener(ModerationListener listener);
    
    /**
     * Represents a network-wide ban.
     */
    class NetworkBan {
        private final UUID playerId;
        private final String reason;
        private final UUID moderatorId;
        private final Instant issuedAt;
        private final Instant expiresAt;
        private final boolean permanent;
        
        public NetworkBan(UUID playerId, String reason, UUID moderatorId, Duration duration) {
            this.playerId = playerId;
            this.reason = reason;
            this.moderatorId = moderatorId;
            this.issuedAt = Instant.now();
            this.expiresAt = duration != null ? issuedAt.plus(duration) : null;
            this.permanent = duration == null;
        }
        
        public NetworkBan(UUID playerId, String reason, UUID moderatorId) {
            this(playerId, reason, moderatorId, null); // Permanent ban
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public String getReason() { return reason; }
        public UUID getModeratorId() { return moderatorId; }
        public Instant getIssuedAt() { return issuedAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public boolean isPermanent() { return permanent; }
        public boolean isActive() { return permanent || (expiresAt != null && Instant.now().isBefore(expiresAt)); }
    }
    
    /**
     * Represents a network-wide kick.
     */
    class NetworkKick {
        private final UUID playerId;
        private final String reason;
        private final UUID moderatorId;
        private final Instant issuedAt;
        
        public NetworkKick(UUID playerId, String reason, UUID moderatorId) {
            this.playerId = playerId;
            this.reason = reason;
            this.moderatorId = moderatorId;
            this.issuedAt = Instant.now();
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public String getReason() { return reason; }
        public UUID getModeratorId() { return moderatorId; }
        public Instant getIssuedAt() { return issuedAt; }
    }
    
    /**
     * Represents a network-wide mute.
     */
    class NetworkMute {
        private final UUID playerId;
        private final String reason;
        private final UUID moderatorId;
        private final Instant issuedAt;
        private final Instant expiresAt;
        private final boolean permanent;
        
        public NetworkMute(UUID playerId, String reason, UUID moderatorId, Duration duration) {
            this.playerId = playerId;
            this.reason = reason;
            this.moderatorId = moderatorId;
            this.issuedAt = Instant.now();
            this.expiresAt = duration != null ? issuedAt.plus(duration) : null;
            this.permanent = duration == null;
        }
        
        public NetworkMute(UUID playerId, String reason, UUID moderatorId) {
            this(playerId, reason, moderatorId, null); // Permanent mute
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public String getReason() { return reason; }
        public UUID getModeratorId() { return moderatorId; }
        public Instant getIssuedAt() { return issuedAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public boolean isPermanent() { return permanent; }
        public boolean isActive() { return permanent || (expiresAt != null && Instant.now().isBefore(expiresAt)); }
    }
    
    /**
     * Represents a network-wide warning.
     */
    class NetworkWarning {
        private final UUID playerId;
        private final String reason;
        private final UUID moderatorId;
        private final Instant issuedAt;
        
        public NetworkWarning(UUID playerId, String reason, UUID moderatorId) {
            this.playerId = playerId;
            this.reason = reason;
            this.moderatorId = moderatorId;
            this.issuedAt = Instant.now();
        }
        
        // Getters
        public UUID getPlayerId() { return playerId; }
        public String getReason() { return reason; }
        public UUID getModeratorId() { return moderatorId; }
        public Instant getIssuedAt() { return issuedAt; }
    }
    
    /**
     * Listener interface for moderation events.
     */
    interface ModerationListener {
        default void onBanExecuted(NetworkBan ban, String serverId) {}
        default void onUnbanExecuted(UUID playerId, UUID moderatorId, String reason, String serverId) {}
        default void onKickExecuted(NetworkKick kick, String serverId) {}
        default void onMuteExecuted(NetworkMute mute, String serverId) {}
        default void onUnmuteExecuted(UUID playerId, UUID moderatorId, String reason, String serverId) {}
        default void onWarningIssued(NetworkWarning warning, String serverId) {}
    }
}
