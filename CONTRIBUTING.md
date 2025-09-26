# Contributing to Conexus

Thank you for your interest in contributing to Conexus! This document provides guidelines and information for contributors.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Submitting Changes](#submitting-changes)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)

## Code of Conduct

By participating in this project, you agree to abide by our Code of Conduct. Please be respectful and constructive in all interactions.

## Getting Started

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/Conexus.git
   cd Conexus
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/BoredHF/Conexus.git
   ```

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Redis server (for integration tests)
- Git

### Building the Project

```bash
# Clone and build
mvn clean compile

# Run tests
mvn test

# Run integration tests (requires Redis)
mvn integration-test

# Build everything
mvn clean package
```

## Making Changes

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes, following our [Coding Standards](#coding-standards)

3. Write or update tests for your changes

4. Update documentation if needed

5. Commit your changes with a clear message:
   ```bash
   git commit -m "Add feature: brief description
   
   Longer description if needed, explaining what and why."
   ```

## Submitting Changes

1. Push your changes to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. Create a Pull Request on GitHub with:
   - Clear title and description
   - Link to any related issues
   - Screenshots or examples if applicable
   - Confirmation that tests pass

3. Wait for review and address any feedback

## Coding Standards

### Java Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and reasonably sized
- Add JavaDoc comments for public APIs
- Use `@Nullable` and `@NonNull` annotations where appropriate

### Code Organization

- Place new features in appropriate packages
- Keep related classes together
- Separate interfaces from implementations
- Use builder patterns for complex objects

### Error Handling

- Use appropriate exception types
- Log errors at appropriate levels
- Provide meaningful error messages
- Handle edge cases gracefully

### Example Code Style

```java
/**
 * Service for managing cross-server messaging operations.
 */
public class MessagingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingService.class);
    
    /**
     * Broadcasts a message to all connected servers.
     *
     * @param message the message to broadcast
     * @return a future that completes when the message is sent
     * @throws IllegalArgumentException if message is null
     */
    public CompletableFuture<Void> broadcast(@NonNull Message message) {
        Objects.requireNonNull(message, "Message cannot be null");
        
        LOGGER.debug("Broadcasting message of type: {}", message.getClass().getSimpleName());
        
        return CompletableFuture.runAsync(() -> {
            try {
                transportProvider.send(message);
            } catch (Exception e) {
                LOGGER.error("Failed to broadcast message", e);
                throw new MessageDeliveryException("Failed to broadcast message", e);
            }
        });
    }
}
```

## Testing

### Unit Tests

- Write unit tests for all new functionality
- Use JUnit 5 and Mockito
- Aim for high code coverage
- Test both success and failure scenarios

### Integration Tests

- Write integration tests for complex workflows
- Use TestContainers for Redis testing
- Test with realistic data and scenarios

### Test Structure

```java
@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {
    
    @Mock
    private TransportProvider transportProvider;
    
    @InjectMocks
    private MessagingService messagingService;
    
    @Test
    void broadcast_ShouldSendMessageThroughTransport() {
        // Given
        Message message = new SimpleTextMessage("server-1", "test", "category");
        
        // When
        CompletableFuture<Void> result = messagingService.broadcast(message);
        
        // Then
        assertThat(result).succeedsWithin(Duration.ofSeconds(1));
        verify(transportProvider).send(message);
    }
}
```

## Documentation

### JavaDoc

- Document all public APIs
- Include parameter descriptions
- Document thrown exceptions
- Provide usage examples where helpful

### README Updates

- Update README.md if you add new features
- Include code examples for new functionality
- Update the feature list if applicable

### Additional Documentation

- Update relevant files in the `docs/` directory
- Add new documentation files for significant features
- Include diagrams or examples where helpful

## Pull Request Guidelines

### Before Submitting

- [ ] Code builds without warnings
- [ ] All tests pass
- [ ] New functionality is tested
- [ ] Documentation is updated
- [ ] Changes are backwards compatible (or breaking changes are documented)

### PR Description Template

```markdown
## Description
Brief description of what this PR does.

## Changes Made
- List of specific changes
- Another change

## Testing
How has this been tested?

## Related Issues
Closes #123
```

## Getting Help

- Check existing issues and discussions
- Create a new issue for bugs or feature requests
- Join our discussions for questions
- Review existing code for examples

## Recognition

Contributors will be recognized in our README and release notes. Thank you for helping make Conexus better!