# Poke Notifier - Technology Stack

## Programming Languages & Versions

### Primary Language
- **Java 21**: Main development language with modern features
- **Kotlin 2.0.21**: Used for Fabric Language Kotlin integration

### Target Platform
- **Minecraft 1.21.1**: Target game version
- **Fabric Loader 0.17.3**: Mod loading framework

## Build System & Tools

### Gradle Configuration
- **Gradle 8.8**: Build automation system
- **Fabric Loom 1.7-SNAPSHOT**: Minecraft mod development plugin
- **Memory Allocation**: 4GB JVM heap, optimized for large projects

### Build Features
- **Split Environment Source Sets**: Separate client/server compilation
- **Automatic Versioning**: Timestamp-based build versions
- **Multi-Target Deployment**: Automated copying to test environments
- **Process Isolation**: Windows command line length limit mitigation

## Core Dependencies

### Minecraft Ecosystem
- **Fabric API 0.104.0+1.21.1**: Core Fabric mod API
- **Yarn Mappings 1.21.1+build.3**: Deobfuscated Minecraft mappings
- **Cobblemon 1.6.1+1.21.1**: Pokémon mod integration (primary dependency)

### UI & Integration
- **ModMenu 11.0.1**: Configuration menu integration
- **Adventure Platform Fabric 5.14.1**: Text component system
- **Fabric Language Kotlin 1.12.3**: Kotlin runtime support

### Optional Dependencies (Compile-Only)
- **AdvancementPlaques 1.6.8**: Enhanced notification styling
- **Iceberg 1.3.2**: AdvancementPlaques dependency
- **Xaero's Minimap**: Runtime-detected map integration
- **Xaero's World Map**: Runtime-detected world map integration

## Development Commands

### Build & Deployment
```bash
# Standard build
./gradlew build

# Clean build
./gradlew clean build

# Deploy to test environments
./gradlew deployToTest

# Individual deployment targets
./gradlew copyModToClient
./gradlew copyModToServer
./gradlew copyModToVanillaLauncher

# Start test server
./gradlew startTestServer
```

### Development Tasks
```bash
# Generate sources JAR
./gradlew sourcesJar

# Run client in development
./gradlew runClient

# Run server in development
./gradlew runServer

# Process resources
./gradlew processResources
```

## Runtime Environment

### JVM Configuration
- **Source Compatibility**: Java 21
- **Target Compatibility**: Java 21
- **Fork Options**: 1-2GB heap allocation for compilation
- **IPv4 Stack**: Preferred for network operations

### Fabric Environment
- **Configuration Cache**: Disabled (IntelliJ compatibility)
- **Parallel Execution**: Enabled for build performance
- **Process Isolation**: Enabled for Windows compatibility

## Repository Configuration

### Maven Repositories
- **Maven Central**: Standard Java dependencies
- **Impact Dev**: Cobblemon and related mods
- **CurseMaven**: CurseForge mod dependencies
- **Shedaniel**: Cloth Config and related utilities
- **TerraformersMC**: ModMenu and Fabric utilities

### Local Dependencies
- **libs/ Directory**: Local JAR files for optional integrations
- **Compile-Only**: Optional dependencies not bundled with mod
- **Runtime Detection**: Dynamic loading of optional features

## CI/CD Integration

### GitHub Actions
- **build.yml**: Automated build and testing workflow
- **Multi-Platform**: Support for different operating systems
- **Artifact Generation**: Automated JAR file creation and distribution

### Quality Assurance
- **Qodana**: Code quality analysis configuration
- **Automated Testing**: Build verification on multiple environments
- **Dependency Validation**: Ensures compatibility with target versions