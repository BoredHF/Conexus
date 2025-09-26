# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project setup with Maven multi-module structure
- Core messaging infrastructure with pluggable transport providers
- Redis transport provider implementation
- Cross-server messaging with JSON serialization
- Player data synchronization services
- Network-wide moderation tools
- Event broadcasting system with priority and filtering
- Example Bukkit plugin demonstrating usage
- Comprehensive documentation and examples

### Changed
- N/A

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

## [1.0.0-SNAPSHOT] - In Development

### Added
- **Core Library (`conexus-core`)**
  - Transport Provider API with Redis implementation
  - Message serialization with Jackson JSON
  - Cross-server messaging service
  - Player data synchronization
  - Event broadcasting system
  - Moderation service for network-wide actions
  - Comprehensive error handling and logging

- **Plugin Example (`conexus-plugin`)**
  - Bukkit plugin implementation
  - Configuration management
  - Command system for testing
  - Event handlers for cross-server communication

- **Documentation**
  - API documentation
  - Configuration guides
  - Integration examples
  - Architecture documentation

- **Development Tools**
  - Maven build configuration
  - Test infrastructure with JUnit 5
  - Integration testing with TestContainers
  - Code quality plugins

---

## Release Template

When creating new releases, use this template:

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes in existing functionality

### Deprecated
- Soon-to-be removed features

### Removed
- Removed features

### Fixed
- Bug fixes

### Security
- Security improvements
```