# Conexus

<div align="center">
  <h3>🌐 Cross-Server Communication for Minecraft</h3>
  <p>A lightweight, extensible Java library that allows Minecraft plugins to communicate across multiple servers in a clean, standardized way.</p>
  
  <p>
    <a href="https://github.com/BoredHF/Conexus/actions/workflows/ci.yml"><img src="https://github.com/BoredHF/Conexus/actions/workflows/ci.yml/badge.svg" alt="CI Status"></a>
    <a href="https://github.com/BoredHF/Conexus/releases"><img src="https://img.shields.io/github/v/release/BoredHF/Conexus?include_prereleases&sort=semver" alt="Latest Release"></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License: MIT"></a>
    <a href="https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html"><img src="https://img.shields.io/badge/Java-17+-orange.svg" alt="Java 17+"></a>
    <a href="https://maven.apache.org"><img src="https://img.shields.io/badge/Maven-3.6+-red.svg" alt="Maven 3.6+"></a>
    <a href="https://github.com/BoredHF/Conexus/issues"><img src="https://img.shields.io/github/issues/BoredHF/Conexus" alt="Issues"></a>
    <a href="https://github.com/BoredHF/Conexus/stargazers"><img src="https://img.shields.io/github/stars/BoredHF/Conexus?style=social" alt="GitHub Stars"></a>
  </p>
  
  <p>
    <a href="#features">Features</a> •
    <a href="#quick-start">Quick Start</a> •
    <a href="#documentation">Documentation</a> •
    <a href="#examples">Examples</a> •
    <a href="#contributing">Contributing</a>
  </p>
</div>

---

## ✨ Features

- **🚀 Easy Integration** - Drop-in library for any Bukkit/Paper plugin
- **📡 Multiple Transports** - Redis (recommended), RabbitMQ, TCP, or custom implementations
- **💬 Cross-Server Messaging** - Type-safe pub/sub messaging with JSON serialization
- **👥 Player Data Sync** - Synchronized player data across your entire network
- **🛡️ Moderation Tools** - Network-wide bans, kicks, mutes, and audit logging
- **⚡ Event Broadcasting** - **Production-ready with circuit breaker, retry logic, metrics & graceful degradation** 🔥
- **🔧 Extensible** - Clean API for custom message types and transport providers
- **📝 Well Documented** - Comprehensive JavaDoc and examples

## 🏗️ Architecture

Conexus is built with a modular architecture:

```
conexus-core/          # Core library with all messaging functionality
conexus-plugin/        # Example Bukkit plugin demonstrating usage
```

### Core Components

- **TransportProvider** - Pluggable communication backends (Redis, RabbitMQ, etc.)
- **MessagingService** - High-level messaging operations and channel management
- **PlayerDataService** - Cross-server player data synchronization
- **EventService** - Custom event broadcasting and handling
- **ModerationService** - Network-wide moderation actions

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Redis server (recommended) or alternative transport
- Paper/Spigot 1.20.4 or higher

### Installation

#### As a Plugin (Easiest)

1. Download the latest `conexus-plugin-1.0.0-SNAPSHOT.jar` from releases
2. Place it in your server's `plugins/` folder
3. Configure Redis connection in `plugins/Conexus/config.yml`:
   ```yaml
   server-id: "lobby-1"  # Unique identifier for this server
   redis:
     host: "127.0.0.1"
     port: 6379
     password: ""
     database: 0
   ```
4. Restart your server
5. Test with `/cxsay Hello from server 1!`

#### As a Library Dependency

```xml
<dependency>
    <groupId>com.boredhf</groupId>
    <artifactId>conexus-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
// Initialize Conexus
RedisTransportProvider transport = new RedisTransportProvider("localhost", 6379);
MessageSerializer serializer = new MessageSerializer();
DefaultMessagingService messaging = new DefaultMessagingService("server-1", transport, serializer);
Conexus conexus = new ConexusImpl("server-1", transport, messaging);

// Connect
conexus.initialize().join();

// Send a cross-server message
SimpleTextMessage message = new SimpleTextMessage("server-1", "Hello from server 1!", "global");
conexus.getMessagingService().broadcast(message);

// Listen for messages
conexus.getMessagingService().registerHandler(SimpleTextMessage.class, context -> {
    SimpleTextMessage msg = context.getMessage();
    if ("global".equals(msg.getCategory())) {
        Bukkit.broadcastMessage("[Cross-Server] " + msg.getContent());
    }
});
```

## 📚 Documentation

- [**Cross-Server Events**](CROSS_SERVER_EVENTS_README.md) - **Production-ready event broadcasting system with circuit breaker, retry logic, and metrics** 🔥
- [**API Documentation**](docs/API.md) - Detailed API reference
- [**Configuration Guide**](docs/Configuration.md) - Complete configuration options
- [**Transport Providers**](docs/TransportProviders.md) - Available transport backends
- [**Custom Messages**](docs/CustomMessages.md) - Creating custom message types
- [**Plugin Integration**](docs/PluginIntegration.md) - Integrating Conexus into your plugin
- [**Examples**](docs/Examples.md) - Real-world usage examples
- [**Troubleshooting**](docs/Troubleshooting.md) - Common issues and solutions

## 🎯 Examples

### Cross-Server Chat
```java
// Send a chat message to all servers
ChatMessage chatMsg = new ChatMessage(serverId, playerName, message, "global");
messagingService.broadcast(chatMsg);
```

### Player Data Synchronization
```java
// Get player data from any server
PlayerEconomy economy = playerDataService.getPlayerData(playerUuid, PlayerEconomy.class).join();

// Update player balance across all servers
playerDataService.updatePlayerData(playerUuid, PlayerEconomy.class, data -> {
    data.setBalance(data.getBalance() + 100);
    return data;
}).join();
```

### Network Moderation
```java
// Ban a player network-wide
NetworkBan ban = new NetworkBan(playerUuid, "Cheating", moderatorUuid, Duration.ofDays(7));
moderationService.executeBan(ban);
```

### Custom Events
```java
// Broadcast a custom event
ServerShutdownEvent event = new ServerShutdownEvent(serverId, "Maintenance restart");
eventService.broadcastEvent(event, EventPriority.HIGH);
```

## 🔧 Building

```bash
# Clone the repository
git clone https://github.com/BoredHF/Conexus.git
cd Conexus

# Build the project
mvn clean package

# Built JARs will be available in:
# conexus-core/target/conexus-core-1.0.0-SNAPSHOT.jar
# conexus-plugin/target/conexus-plugin-1.0.0-SNAPSHOT.jar
```

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests (requires Redis)
mvn integration-test
```

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Roadmap

- [x] Core messaging infrastructure
- [x] Redis transport provider
- [x] Basic plugin example
- [x] Player data synchronization
- [x] Moderation tools
- [x] Event broadcasting system
- [ ] RabbitMQ transport provider
- [ ] REST API transport
- [ ] Web dashboard
- [ ] Metrics and monitoring

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with ❤️ for the Minecraft community
- Inspired by modern microservice communication patterns
- Thanks to all contributors and users

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/BoredHF/Conexus/issues)
- **Discussions**: [GitHub Discussions](https://github.com/BoredHF/Conexus/discussions)
- **Wiki**: [Project Wiki](https://github.com/BoredHF/Conexus/wiki)

---

<div align="center">
  <p>Made with ❤️ by <a href="https://github.com/BoredHF">BoredHF</a></p>
  <p>⭐ Star this project if you find it useful!</p>
</div>
