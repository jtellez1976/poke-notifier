# Poke Notifier - Project Structure

## Directory Organization

### Root Structure
```
Poke-Notifier-mc1.21.1-fabric-0.16.14/
├── src/                    # Source code
│   ├── main/              # Server-side code
│   └── client/            # Client-side code
├── gradle/                # Gradle wrapper
├── libs/                  # Local dependencies
├── run/                   # Development runtime
├── logs/                  # Build and runtime logs
└── build files           # Gradle configuration
```

### Source Code Architecture
```
src/
├── main/java/com/zehro_mc/pokenotifier/
│   ├── PokeNotifier.java           # Main mod entry point
│   ├── commands/                   # Command implementations
│   ├── events/                     # Event system (swarms, hunts)
│   ├── globalhunt/                 # Global hunt functionality
│   ├── block/                      # Trophy blocks and entities
│   ├── item/                       # Trophy items
│   ├── config/                     # Configuration management
│   ├── utils/                      # Utility classes
│   └── mixins/                     # Minecraft code modifications
└── client/java/com/zehro_mc/pokenotifier/client/
    ├── PokeNotifierClient.java     # Client entry point
    ├── PokeNotifierCustomScreen.java # Main GUI
    └── mixins/                     # Client-side mixins
```

### Resource Structure
```
src/main/resources/
├── assets/poke-notifier/
│   ├── lang/                       # Localization files
│   ├── models/                     # 3D models for blocks/items
│   ├── textures/                   # Textures for blocks/items
│   └── blockstates/               # Block state definitions
├── data/
│   ├── minecraft/                  # Minecraft data overrides
│   └── poke-notifier/             # Mod-specific data
└── fabric.mod.json                # Mod metadata
```

## Core Components

### Event Management System
- **SwarmEventManager**: Handles mass Pokémon spawning events
- **GlobalHuntEvent**: Manages server-wide hunting challenges
- **EventScheduler**: Coordinates timing and cleanup of events

### GUI System
- **PokeNotifierCustomScreen**: Main configuration interface
- **Role-based tabs**: Different interfaces for players vs admins
- **Interactive panels**: Real-time feedback and controls

### Trophy System
- **Trophy Items**: Physical rewards for generation completion
- **Trophy Blocks**: Display pedestals and altars
- **TrophyAltarBlockEntity**: Handles trophy placement and effects

### Integration Layer
- **Xaero's Minimap**: Waypoint creation and management
- **Cobblemon API**: Pokémon data and event integration
- **AdvancementPlaques**: Enhanced notification display

## Architectural Patterns

### Client-Server Split
- **Server-side**: Event logic, data persistence, command processing
- **Client-side**: GUI rendering, local notifications, map integration
- **Shared**: Configuration classes, utility functions

### Event-Driven Architecture
- **Cobblemon Events**: Hook into Pokémon catch/spawn events
- **Custom Events**: Swarm creation, hunt completion, trophy placement
- **Event Listeners**: Modular handlers for different event types

### Configuration Management
- **JSON-based**: Human-readable configuration files
- **Hot-reload**: Runtime configuration updates without restart
- **Validation**: Type checking and constraint enforcement

### Data Persistence
- **Player Progress**: Individual hunt lists and completion status
- **Server State**: Active events, statistics, system status
- **Backup System**: Automatic data backup and rollback capabilities

## Dependencies and Integration

### Required Dependencies
- **Minecraft 1.21.1**: Base game platform
- **Fabric Loader 0.17.3+**: Mod loading framework
- **Fabric API**: Core Fabric functionality
- **Cobblemon 1.6.1+**: Pokémon game integration

### Optional Integrations
- **Xaero's Minimap/World Map**: Enhanced navigation
- **AdvancementPlaques + Iceberg**: Stylized notifications
- **ModMenu**: Configuration access in mod menu

### Build System
- **Gradle 8.8**: Build automation and dependency management
- **Fabric Loom**: Minecraft mod development toolchain
- **Custom Tasks**: Automated deployment to test environments