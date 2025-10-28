# Poke Notifier - Technology Stack

## Programming Languages
- **Java 21**: Primary development language (source and target compatibility)
- **JSON**: Configuration files, data storage, and resource definitions
- **Gradle**: Build script configuration

## Framework and Platform
- **Minecraft 1.21.1**: Target game version
- **Fabric Loader 0.17.3+**: Mod loading framework
- **Fabric API 0.104.0+1.21.1**: Core Fabric functionality
- **Fabric Loom 1.7-SNAPSHOT**: Development toolchain

## Core Dependencies
```gradle
// Required
minecraft "com.mojang:minecraft:1.21.1"
mappings "net.fabricmc:yarn:1.21.1+build.3:v2"
modImplementation "net.fabricmc:fabric-loader:0.17.3"
modImplementation "net.fabricmc.fabric-api:fabric-api:0.104.0+1.21.1"
modImplementation "com.cobblemon:fabric:1.6.1+1.21.1"
modImplementation "net.fabricmc:fabric-language-kotlin:1.12.3+kotlin.2.0.21"
modApi "net.kyori:adventure-platform-fabric:5.14.1"

// GUI
modImplementation "com.terraformersmc:modmenu:11.0.1"

// Optional (compile-only)
modCompileOnly files('libs/AdvancementPlaques-1.21.1-fabric-1.6.8.jar')
modCompileOnly files('libs/Iceberg-1.21.1-fabric-1.3.2.jar')
```

## Build System Configuration

### Gradle Properties
```properties
# Memory and Performance
org.gradle.jvmargs=-Xmx4G -Djava.net.preferIPv4Stack=true
org.gradle.parallel=true
fabric.loom.useProcessIsolation=true

# Version Information
mod_version=1.4.0
maven_group=com.zehro_mc.pokenotifier
archives_base_name=poke-notifier
```

### Java Compilation Settings
- **Release Target**: Java 21
- **Memory Allocation**: 1G-2G heap for compilation
- **Fork Options**: Enabled to handle Windows command line limits
- **Sources JAR**: Generated automatically

## Development Commands

### Build and Test
```bash
# Build the mod
./gradlew build

# Clean build artifacts
./gradlew clean

# Run client for testing
./gradlew runClient

# Run server for testing
./gradlew runServer
```

### Custom Deployment Tasks
```bash
# Deploy to all test environments
./gradlew deployToTest

# Deploy to specific environments
./gradlew copyModToClient
./gradlew copyModToServer
./gradlew copyModToVanillaLauncher

# Start test server after deployment
./gradlew startTestServer
```

## Development Environment

### IDE Configuration
- **IntelliJ IDEA**: Recommended IDE with Fabric support
- **Configuration Cache**: Disabled due to compatibility issues
- **Process Isolation**: Enabled for better Windows compatibility

### Local Dependencies
- **libs/ Directory**: Contains optional mod JARs for compilation
- **Runtime Detection**: Xaero's integration uses runtime detection only
- **Version Management**: Timestamped builds for development tracking

### Testing Setup
- **Client Instance**: CurseForge launcher integration
- **Server Instance**: Local test server with auto-deployment
- **Vanilla Launcher**: Alternative testing environment

## File Structure and Resources

### Asset Pipeline
```
assets/poke-notifier/
├── lang/en_us.json          # Localization strings
├── models/                  # Block and item 3D models
├── textures/               # PNG textures for blocks/items
└── blockstates/            # Block state JSON definitions
```

### Data Generation
```
data/
├── minecraft/              # Vanilla data overrides
└── poke-notifier/         # Mod-specific data files
```

### Configuration Management
- **JSON Format**: Human-readable configuration files
- **Hot Reload**: Runtime configuration updates
- **Validation**: Built-in type checking and constraints

## Performance Optimizations

### Memory Management
- **4GB Gradle Heap**: Handles large dependency resolution
- **Process Isolation**: Prevents memory leaks during builds
- **Parallel Builds**: Faster compilation on multi-core systems

### Runtime Optimizations
- **Smart Spawning**: Pokémon only spawn near players
- **Event Cleanup**: Automatic removal of expired events
- **Waypoint Management**: Protected waypoints with smart cleanup

### Windows Compatibility
- **Argument Files**: Bypass command line length limits
- **IPv4 Stack**: Preferred for network operations
- **Fork Options**: Separate JVM processes for compilation