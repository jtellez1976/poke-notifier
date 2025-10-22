# Poke Notifier - Project Structure

## Directory Organization

### Root Structure
```
Poke-Notifier-mc1.21.1-fabric-0.16.14/
├── src/                          # Source code
│   ├── main/                     # Server-side code
│   └── client/                   # Client-side code
├── gradle/                       # Gradle wrapper
├── libs/                         # Local dependencies
├── run/                          # Development runtime
├── .github/workflows/            # CI/CD automation
└── build.gradle                  # Build configuration
```

### Source Code Architecture

#### Main Module (`src/main/`)
- **Core Package**: `com.zehro_mc.pokenotifier`
  - Main mod entry point and initialization
  - Configuration management (client/server)
  - API interfaces and utilities

- **Event Systems**: 
  - `events/` - Core event handling and management
  - `globalhunt/` - Global hunt event implementation
  - `bounty/` - Bounty system mechanics
  - `swarm/` - Swarm event coordination

- **Data Management**:
  - Configuration persistence and synchronization
  - Player data storage and backup systems
  - Event state management

#### Client Module (`src/client/`)
- **GUI Framework**: Custom screen implementations
- **Notification Systems**: HUD overlays and alerts
- **Client Configuration**: User preference management
- **Integration Handlers**: Xaero's map integration

### Core Components & Relationships

#### Configuration System
- **ConfigManager**: Central configuration coordinator
- **ConfigClient**: Client-side settings and preferences
- **ConfigServer**: Server-side administrative settings
- **Synchronization**: Real-time config sync between client/server

#### Event Management Architecture
- **Event Managers**: Specialized handlers for each event type
- **State Persistence**: Event progress and player participation tracking
- **Notification Pipeline**: Multi-channel alert distribution system

#### GUI Framework
- **PokeNotifierCustomScreen**: Main interface controller
- **Role-Based Rendering**: Dynamic UI adaptation based on permissions
- **Interactive Components**: Custom widgets and input handlers
- **Response System**: In-GUI feedback and status display

#### API Layer
- **PokeNotifierApi**: Public interface for external integrations
- **Event Hooks**: Extensible event system for third-party mods
- **Data Access**: Controlled access to internal systems

### Architectural Patterns

#### Client-Server Split Architecture
- Clear separation between client and server responsibilities
- Secure administrative functions isolated to server-side
- Efficient data synchronization protocols

#### Event-Driven Design
- Modular event system with pluggable handlers
- Asynchronous processing for performance optimization
- Centralized event coordination and state management

#### Configuration-First Approach
- Extensive configurability for all major features
- Runtime configuration updates without restarts
- Hierarchical permission-based access control

#### Integration-Ready Framework
- Modular design supporting optional dependencies
- Runtime detection of compatible mods
- Graceful degradation when dependencies unavailable

### Build System Integration
- **Fabric Loom**: Minecraft mod development framework
- **Multi-Environment**: Separate client/server source sets
- **Dependency Management**: Local libs + Maven repositories
- **Automated Deployment**: Custom tasks for test environment deployment