package com.boredhf.conexus.plugin.listeners;

import com.boredhf.conexus.communication.messages.SimpleTextMessage;
import com.boredhf.conexus.plugin.ConexusPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for broadcasting player join/leave events across servers.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class PlayerJoinLeaveListener implements Listener {
    
    private final ConexusPlugin plugin;
    
    public PlayerJoinLeaveListener(ConexusPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String message = event.getPlayer().getName() + " joined " + plugin.getConexus().getServerId();
        
        SimpleTextMessage joinMessage = new SimpleTextMessage(
            plugin.getConexus().getServerId(),
            message,
            "join-leave"
        );
        
        plugin.getConexus().getMessagingService()
            .broadcast(joinMessage)
            .exceptionally(throwable -> {
                plugin.getLogger().warning("Failed to broadcast join message: " + throwable.getMessage());
                return null;
            });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String message = event.getPlayer().getName() + " left " + plugin.getConexus().getServerId();
        
        SimpleTextMessage quitMessage = new SimpleTextMessage(
            plugin.getConexus().getServerId(),
            message,
            "join-leave"
        );
        
        plugin.getConexus().getMessagingService()
            .broadcast(quitMessage)
            .exceptionally(throwable -> {
                plugin.getLogger().warning("Failed to broadcast quit message: " + throwable.getMessage());
                return null;
            });
    }
}
