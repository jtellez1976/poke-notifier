# Poke Notifier - Development Guidelines

## Code Quality Standards

### File Header Convention
All source files must include the Mozilla Public License 2.0 header:
```java
/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
```

### Package Structure
- **Main Package**: `com.zehro_mc.pokenotifier`
- **Client Package**: `com.zehro_mc.pokenotifier.client`
- **Subpackages**: Organized by functionality (events, networking, util, model, etc.)

### Naming Conventions
- **Classes**: PascalCase (e.g., `PokeNotifierCustomScreen`, `SwarmEventManager`)
- **Methods**: camelCase (e.g., `buildUserToolsLayout`, `onPokemonCaptured`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MOD_ID`, `TRACKED_POKEMON`)
- **Variables**: camelCase with descriptive names
- **Files**: kebab-case for config files (e.g., `config-client.json`)

### Documentation Standards
- **Class Documentation**: Brief purpose and responsibility
- **Method Documentation**: JavaDoc for public methods with complex logic
- **Inline Comments**: Explain business logic, not obvious code
- **TODO/FIX Comments**: Used extensively for tracking improvements and bug fixes

## Architectural Patterns

### Configuration Management Pattern
```java
// Centralized configuration through ConfigManager
ConfigServer config = ConfigManager.getServerConfig();
config.debug_mode_enabled = true;
ConfigManager.saveServerConfigToFile();
```

### Event-Driven Architecture
- **Event Managers**: Specialized classes for each event type (GlobalHuntManager, SwarmEventManager)
- **Event Lifecycle**: start() → active monitoring → completion/timeout → cleanup
- **State Persistence**: Events save state to config files for server restarts

### Client-Server Communication
```java
// Networking payload pattern
ServerPlayNetworking.send(player, new AdminStatusPayload(
    player.hasPermissionLevel(2),
    config.debug_mode_enabled,
    // ... other parameters
));
```

### GUI Architecture Pattern
```java
// Hierarchical GUI structure
private enum MainCategory { USER_TOOLS, EVENTS, ADMIN_TOOLS }
private enum UserSubCategory { NOTIFICATIONS, CUSTOM_HUNT, CATCH_EM_ALL }

// Dynamic UI building based on permissions
if (PokeNotifierClient.isPlayerAdmin) {
    buildAdminToolsLayout(panelX, panelY, panelWidth, panelHeight);
}
```

## Common Implementation Patterns

### Singleton Pattern for Managers
```java
public class SwarmEventManager {
    private static SwarmEventManager instance;
    
    public static SwarmEventManager getInstance() {
        return instance;
    }
}
```

### Builder Pattern for Complex Objects
```java
ButtonWidget.builder(Text.literal("Debug Mode"), button -> {
    // Action logic
}).dimensions(x, y, width, height)
.tooltip(Tooltip.of(Text.literal("Toggle debug mode")))
.build();
```

### Lazy Loading Pattern
```java
public static ConfigClient getClientConfig() {
    if (configClient == null) {
        loadConfigClient();
    }
    return configClient;
}
```

### Error Handling Pattern
```java
try {
    // Risky operation
} catch (Exception e) {
    PokeNotifier.LOGGER.error("Operation failed: {}", e.getMessage(), e);
    // Graceful fallback
}
```

## Logging Standards

### Logging Levels
- **INFO**: System state changes, event lifecycle
- **WARN**: Recoverable errors, deprecated usage
- **ERROR**: Unrecoverable errors, exceptions
- **DEBUG**: Detailed execution flow (when debug mode enabled)

### Logging Format
```java
PokeNotifier.LOGGER.info("[SwarmManager] System toggled to: {}", swarmConfig.system_enabled);
PokeNotifier.LOGGER.error("Failed to spawn Pokemon for Global Hunt: {}", e.getMessage(), e);
```

## Networking Patterns

### Payload Structure
```java
public record AdminStatusPayload(
    boolean isAdmin,
    boolean debugMode,
    boolean testMode
    // ... other fields
) implements CustomPayload {
    public static final CustomPayload.Id<AdminStatusPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(PokeNotifier.MOD_ID, "admin_status"));
}
```

### Server-Side Packet Handling
```java
ServerPlayNetworking.registerGlobalReceiver(PayloadType.ID, (payload, context) -> {
    ServerPlayerEntity player = context.player();
    context.server().execute(() -> {
        // Handle payload on server thread
    });
});
```

## Configuration Patterns

### Version Migration
```java
if (fileVersion < currentVersion) {
    PokeNotifier.LOGGER.info("Migrating {} from version {} to {}", 
        configName, fileVersion, currentVersion);
    // Migration logic
}
```

### Secure Data Storage
```java
// Encrypt sensitive player data
String encryptedData = DataSecurityUtil.encrypt(jsonProgress);
Map<String, String> dataToSave = Map.of("data", encryptedData);
```

## UI Development Guidelines

### Responsive Layout
```java
// Centered panel design
int panelWidth = 420;
int panelHeight = 260;
int panelX = (this.width - panelWidth) / 2;
int panelY = (this.height - panelHeight) / 2;
```

### Permission-Based UI
```java
// Show admin features only to operators
if (PokeNotifierClient.isPlayerAdmin) {
    addDrawableChild(createAdminButton());
}
```

### Interactive Feedback
```java
// Immediate visual feedback
displayResponse(List.of(Text.literal("Settings updated.").formatted(Formatting.GREEN)));
```

## Performance Considerations

### Caching Strategy
```java
// Cache frequently accessed data
private static final Map<UUID, PlayerCatchProgress> playerCatchProgress = new ConcurrentHashMap<>();
```

### Async Operations
```java
// Use server execute for thread safety
world.getServer().execute(() -> {
    // Minecraft operations on server thread
});
```

### Resource Management
```java
// Proper resource cleanup
if (scheduler != null && !scheduler.isShutdown()) {
    scheduler.shutdown();
}
```

## Testing and Debugging

### Debug Mode Integration
```java
if (ConfigManager.getServerConfig().debug_mode_enabled) {
    PokeNotifier.LOGGER.debug("Debug information: {}", debugData);
}
```

### Test Mode Features
```java
// Test spawns marked for identification
pokemonEntity.getPokemon().getPersistentData().putBoolean("pokenotifier_test_spawn", true);
```

## Integration Patterns

### Cobblemon API Usage
```java
// Standard Pokemon creation
PokemonProperties props = PokemonProperties.Companion.parse(pokemonName);
PokemonEntity entity = props.createEntity(world);
```

### Fabric API Integration
```java
// Event registration
CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
    // Handle capture
    return Unit.INSTANCE;
});
```

### Optional Mod Integration
```java
// Runtime detection of optional dependencies
boolean xaeroAvailable = XaeroIntegration.isXaeroAvailable();
if (xaeroAvailable) {
    // Use Xaero features
}
```

## Code Organization Principles

### Separation of Concerns
- **Client**: UI, rendering, client-side state
- **Server**: Game logic, data persistence, networking
- **Common**: Shared models, utilities, constants

### Modular Design
- Each feature in separate package/class
- Clear interfaces between components
- Minimal coupling between modules

### Configuration-Driven Development
- Extensive use of configuration files
- Runtime configuration changes
- Environment-specific settings