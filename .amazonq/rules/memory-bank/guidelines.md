# Poke Notifier - Development Guidelines

## Code Quality Standards

### File Headers and Copyright
- **Consistent License Headers**: All Java files include Mozilla Public License 2.0 header with copyright notice
- **Standard Format**: Copyright (C) 2024 ZeHrOx with MPL 2.0 reference and URL
- **File Documentation**: Headers provide clear licensing and attribution information

### Code Formatting Patterns
- **Indentation**: 4-space indentation consistently used throughout codebase
- **Line Length**: Generally kept under 120 characters with logical line breaks
- **Brace Style**: Opening braces on same line, closing braces on new line
- **Method Spacing**: Single blank line between methods, logical grouping with comments

### Naming Conventions
- **Classes**: PascalCase (e.g., `PokeNotifier`, `SwarmEventManager`, `GlobalHuntEvent`)
- **Methods**: camelCase with descriptive names (e.g., `onPokemonCaptured`, `validateStructure`)
- **Variables**: camelCase for local variables, UPPER_CASE for constants
- **Packages**: Lowercase with dots (e.g., `com.zehro_mc.pokenotifier.events`)

## Architectural Patterns

### Singleton Pattern Usage
- **Manager Classes**: SwarmEventManager and GlobalHuntManager use singleton pattern
- **Instance Management**: Static getInstance() methods with null checks
- **Thread Safety**: Proper initialization in server context

```java
private static SwarmEventManager instance;
public static SwarmEventManager getInstance() {
    return instance;
}
```

### Event-Driven Architecture
- **Cobblemon Integration**: Subscribe to Cobblemon events with Priority.NORMAL
- **Custom Events**: Create custom payload classes for client-server communication
- **Event Handlers**: Separate listener classes for different event types

### Configuration Management
- **JSON-based Config**: Use JSON files for persistent configuration
- **Hot Reload**: Support runtime configuration updates
- **Validation**: Built-in validation for configuration values

### Client-Server Communication
- **Payload Pattern**: Custom payload classes for network communication
- **Bidirectional**: Both client-to-server and server-to-client messaging
- **Type Safety**: Strongly typed payload parameters

```java
public record CustomListUpdatePayload(Action action, String pokemonName) implements CustomPayload {
    public static final CustomPayload.Id<CustomListUpdatePayload> ID = new CustomPayload.Id<>(IDENTIFIER);
}
```

## Semantic Patterns

### Error Handling and Logging
- **Comprehensive Logging**: Use SLF4J logger with appropriate levels (info, warn, error, debug)
- **Contextual Messages**: Include relevant context in log messages
- **Exception Handling**: Try-catch blocks with meaningful error messages
- **Debug Mode**: Conditional debug logging based on configuration

```java
public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
if (ConfigManager.getServerConfig().debug_mode_enabled) {
    LOGGER.debug("[SwarmManager] Debug information: {}", details);
}
```

### Resource Management
- **Proper Cleanup**: Always clean up resources in finally blocks or try-with-resources
- **Entity Management**: Track and validate entity references, remove invalid ones
- **Memory Management**: Clear collections and cancel scheduled tasks on shutdown

### Null Safety and Validation
- **Null Checks**: Consistent null checking before object usage
- **Early Returns**: Use early returns to reduce nesting
- **Validation Methods**: Separate validation logic into dedicated methods
- **Optional Usage**: Use Optional where appropriate for nullable values

## Internal API Usage Patterns

### Minecraft/Fabric API Integration
- **Server Execution**: Use `server.execute()` for thread-safe operations
- **World Access**: Proper world and chunk loading before block operations
- **Player Management**: Access players through server player manager
- **Block State Management**: Use proper block state setting with update flags

```java
world.getServer().execute(() -> {
    // Thread-safe operations here
    world.setBlockState(pos, state, 3); // Update flag 3 for client sync
});
```

### Cobblemon API Integration
- **Pokemon Creation**: Use PokemonProperties.Companion.parse() for Pokemon creation
- **Storage Access**: Access party and PC through Cobblemon.INSTANCE.getStorage()
- **Entity Spawning**: Create entities through props.createEntity(world)
- **Data Persistence**: Use Pokemon.getPersistentData() for custom data

```java
PokemonProperties props = PokemonProperties.Companion.parse(pokemonName);
PokemonEntity entity = props.createEntity(world);
entity.getPokemon().getPersistentData().putBoolean("custom_flag", true);
```

### GUI Development Patterns
- **Screen Inheritance**: Extend Screen class for custom GUIs
- **Widget Management**: Use addDrawableChild() for proper widget lifecycle
- **State Management**: Implement clearAndInit() for dynamic GUI updates
- **Event Handling**: Override mouse and keyboard event methods

### Networking Patterns
- **Payload Registration**: Register payloads in mod initialization
- **Server Receivers**: Use ServerPlayNetworking.registerGlobalReceiver()
- **Client Sending**: Use ClientPlayNetworking.send() for client-to-server
- **Thread Safety**: Execute network operations on proper threads

## Frequently Used Annotations

### Fabric/Minecraft Annotations
- `@Override`: Consistently used for method overrides
- `@Nullable`: Used for methods that may return null
- `@Environment(EnvType.CLIENT)`: For client-side only code

### Custom Annotations
- No custom annotations observed, relies on standard Java and Fabric annotations

## Common Code Idioms

### Configuration Access Pattern
```java
ConfigServer config = ConfigManager.getServerConfig();
if (config.debug_mode_enabled) {
    // Debug operations
}
```

### Entity Validation Pattern
```java
swarmEntities.removeIf(uuid -> {
    PokemonEntity entity = (PokemonEntity) world.getEntity(uuid);
    return entity == null || !entity.isAlive() || entity.isRemoved();
});
```

### Player Notification Pattern
```java
for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
    player.sendMessage(message, false);
    ServerPlayNetworking.send(player, payload);
}
```

### Safe Block Operations Pattern
```java
// Force chunk loading
world.getChunk(x >> 4, z >> 4);
// Validate position
if (y >= world.getBottomY() && y <= world.getTopY()) {
    world.setBlockState(pos, state);
}
```

## Performance Considerations

### Tick-based Operations
- **Throttled Checks**: Use tick counters to limit expensive operations
- **Batch Processing**: Group related operations together
- **Conditional Execution**: Skip operations when not needed

### Memory Optimization
- **Collection Management**: Regular cleanup of tracked entities and data
- **Lazy Loading**: Load resources only when needed
- **Weak References**: Use appropriate reference types for cached data

### Network Optimization
- **Payload Efficiency**: Send only necessary data in network packets
- **Update Throttling**: Limit frequency of client updates
- **Conditional Sync**: Only sync when state actually changes

## Testing and Debugging

### Debug Mode Integration
- **Configurable Logging**: Debug messages controlled by server configuration
- **Test Mode**: Special test mode for spawning and validation
- **Admin Commands**: Comprehensive admin interface for testing

### Error Recovery
- **Graceful Degradation**: Continue operation when non-critical components fail
- **Fallback Values**: Provide sensible defaults when configuration is invalid
- **State Validation**: Regular validation of internal state consistency