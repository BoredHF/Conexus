package com.boredhf.conexus.plugin;

import com.boredhf.conexus.Conexus;
import com.boredhf.conexus.ConexusImpl;
import com.boredhf.conexus.communication.DefaultMessagingService;
import com.boredhf.conexus.communication.MessageSerializer;
import com.boredhf.conexus.communication.messages.SimpleTextMessage;
import com.boredhf.conexus.plugin.commands.CrossServerSayCommand;
import com.boredhf.conexus.plugin.listeners.PlayerJoinLeaveListener;
import com.boredhf.conexus.transport.RedisTransportProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main plugin class for Conexus cross-server communication.
 * 
 * This plugin serves as both a library and a working example of
 * how to use Conexus for cross-server communication in Minecraft.
 * 
 * @author BoredHF
 * @since 1.0.0
 */
public class ConexusPlugin extends JavaPlugin {
    
    private static final Logger logger = LoggerFactory.getLogger(ConexusPlugin.class);
    
    private ConexusImpl conexus;
    
    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        try {
            // Initialize Conexus
            initializeConexus();
            
            // Register listeners
            registerListeners();
            
            // Register commands
            registerCommands();
            
            logger.info("Conexus plugin enabled successfully! Server ID: {}", conexus.getServerId());
            
        } catch (Exception e) {
            logger.error("Failed to enable Conexus plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        if (conexus != null) {
            try {
                conexus.shutdown().join();
                logger.info("Conexus plugin disabled successfully");
            } catch (Exception e) {
                logger.error("Error during Conexus shutdown", e);
            }
        }
    }
    
    /**
     * Gets the Conexus instance for this plugin.
     * 
     * @return the Conexus instance
     */
    public Conexus getConexus() {
        return conexus;
    }
    
    private void initializeConexus() {
        String serverId = getConfig().getString("server-id", "unknown");
        
        // Redis configuration
        String redisHost = getConfig().getString("redis.host", "127.0.0.1");
        int redisPort = getConfig().getInt("redis.port", 6379);
        String redisPassword = getConfig().getString("redis.password", "");
        int redisDb = getConfig().getInt("redis.database", 0);
        
        // Create Redis transport
        RedisTransportProvider transport = new RedisTransportProvider(
            redisHost, 
            redisPort, 
            redisPassword.isEmpty() ? null : redisPassword, 
            redisDb
        );
        
        // Create messaging service
        MessageSerializer serializer = new MessageSerializer();
        DefaultMessagingService messagingService = new DefaultMessagingService(serverId, transport, serializer);
        
        // Create Conexus implementation
        conexus = new ConexusImpl(serverId, transport, messagingService);
        
        // Initialize asynchronously
        conexus.initialize().thenRun(() -> {
            logger.info("Conexus initialized and connected to Redis at {}:{}", redisHost, redisPort);
            
            // Register a simple message handler for demo
            registerDemoMessageHandler();
            
        }).exceptionally(throwable -> {
            logger.error("Failed to initialize Conexus", throwable);
            getServer().getPluginManager().disablePlugin(this);
            return null;
        });
    }
    
    private void registerDemoMessageHandler() {
        conexus.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
            SimpleTextMessage message = context.getMessage();
            
            if ("global".equals(message.getCategory())) {
                // Broadcast to all players on this server
                getServer().broadcastMessage("§7[§bCross-Server§7] §f" + message.getContent());
                logger.info("Received cross-server message: {}", message.getContent());
            }
        });
    }
    
    private void registerListeners() {
        if (getConfig().getBoolean("plugin.broadcast-joins", true)) {
            getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
        }
    }
    
    private void registerCommands() {
        if (getConfig().getBoolean("plugin.enable-demo-command", true)) {
            getCommand("cxsay").setExecutor(new CrossServerSayCommand(this));
        }
    }
}