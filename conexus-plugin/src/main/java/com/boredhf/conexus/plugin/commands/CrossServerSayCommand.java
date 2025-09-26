package com.boredhf.conexus.plugin.commands;

import com.boredhf.conexus.communication.messages.SimpleTextMessage;
import com.boredhf.conexus.plugin.ConexusPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to broadcast cross-server messages.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class CrossServerSayCommand implements CommandExecutor {
    
    private final ConexusPlugin plugin;
    
    public CrossServerSayCommand(ConexusPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /cxsay <message>");
            return true;
        }
        
        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        String fullMessage = senderName + " @ " + plugin.getConexus().getServerId() + ": " + message;
        
        // Create and broadcast the message
        SimpleTextMessage textMessage = new SimpleTextMessage(
            plugin.getConexus().getServerId(),
            fullMessage,
            "global"
        );
        
        plugin.getConexus().getMessagingService()
            .broadcast(textMessage)
            .thenRun(() -> {
                sender.sendMessage("§aBroadcasted cross-server message!");
            })
            .exceptionally(throwable -> {
                sender.sendMessage("§cFailed to broadcast message: " + throwable.getMessage());
                plugin.getLogger().warning("Failed to broadcast cross-server message: " + throwable.getMessage());
                return null;
            });
        
        return true;
    }
}