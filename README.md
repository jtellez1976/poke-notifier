# Poke Notifier v1.5.0 - Cobblemon 1.7.0 Compatibility Update!

Poke Notifier has evolved from a simple notification mod into a comprehensive server event management system! This latest version introduces powerful new event systems, enhanced administration tools, and seamless map integration, making it the ultimate tool for community servers running Cobblemon.

## 🔧 Latest Updates in v1.5.0

### ✅ **Cobblemon 1.7.0 Compatibility**
- **Full API Compatibility**: Updated all Pokemon display name calls for Cobblemon 1.7.0
- **Tested & Verified**: Complete singleplayer and multiplayer testing confirmed
- **No Breaking Changes**: Seamless upgrade from v1.4.x with no data loss
- **Enhanced Pokemon Lists**: Updated and corrected Pokemon categorization for accuracy

### 📊 **Pokemon List Improvements**
- **MYTHICAL Category**: Corrected list with proper 22 Mythical Pokemon
- **ULTRA_RARE Category**: Refined to include only starter Pokemon (13 total)
- **Accurate Categorization**: All Pokemon now properly classified according to official standards

## 🌟 Core Features from v1.4.0

### 🌪️ **Swarm Events System**
- **Mass Pokémon Spawning**: Create server-wide swarm events with 15-25 Pokémon of the same species
- **Smart Performance**: Pokémon only spawn when players are nearby, optimizing server resources
- **Guaranteed Shiny**: Every swarm includes at least one shiny Pokémon with boosted odds
- **Dynamic Levels**: Pokémon spawn at levels based on your server's player average (±10 levels)
- **Automatic Management**: Events end naturally when all Pokémon are caught or time expires

### 🌍 **Global Hunt Events**
- **Server-Wide Challenges**: Admins can start hunt events for specific Pokémon species
- **Competitive Rewards**: First player to catch the target wins special prizes
- **Real-Time Tracking**: Live updates on hunt progress and winner announcements

### 🗺️ **Xaero's Minimap Integration**
- **Automatic Event Waypoints**: Swarm and hunt events create map waypoints automatically
- **Smart Cleanup**: Waypoints are removed when events end or Pokémon are caught
- **Protected Waypoints**: Important event waypoints are protected from accidental deletion
- **Click-to-Navigate**: Clickable coordinates in chat for easy navigation

### 📊 **Advanced System Monitoring**
- **Live System Logs**: Real-time server monitoring with scrollable system status
- **Event Statistics**: Complete tracking of all server events and player participation
- **Performance Metrics**: Monitor entity counts, player activity, and system health

## ✨ Core Features from v1.3.0

### 🎨 Unified GUI
- **One Command to Rule Them All:** The new `/pnc gui` command is now the single, universal entry point for both players and administrators. All other chat commands have been hidden to provide a clean, focused experience.
- **Elegant New Design:** The configuration screen is a sleek, centered panel that feels integrated with the game, not a disruptive full-screen menu.
- **Interactive Response Panel:** Command feedback is now displayed directly within the GUI in a temporary text box, so you no longer need to close the menu to see the results of your actions.

### 👑 Role-Based Interface
The GUI intelligently adapts to who is using it:
- **User Tools Tab:** All players have access to their personal settings, neatly organized into sub-categories:
  - `🔔 Notifications`: Toggle chat, sound, and HUD alerts.
  - `🎯 Custom Hunt`: Manage your personal hunt list with an interactive, clickable display.
  - `🏆 Catch 'em All`: Track your Pokédex completion for any generation.
  - `ℹ️ Info & Help`: View mod status, version, and configure the update checker.
- **Admin Tools Tab:** A separate, secure tab that is **only visible to OPs**. It provides a user-friendly interface for all server management commands, defaulting as the main view for admins to improve their workflow.

### 🛠️ Complete Admin Control via GUI
Administrators no longer need to memorize complex commands. Everything is now accessible with a click:
- **Server Control:** Toggle `Debug Mode` and `Test Mode`, and safely `Reload` or `Reset` all mod configurations (with confirmation prompts for dangerous actions).
- **Event Management:** Create and manage `Swarm Events`, `Global Hunts`, and `Bounty System` with full control over duration, rewards, and participants.
- **Player Data Management:** Easily select an online player from an autocomplete list to `Autocomplete` a generation's progress or `Rollback` their data from a backup.
- **Testing:** A dedicated panel to spawn any Pokémon for testing, with options for shiny, level, and location.
- **System Monitoring:** Real-time view of server status, active events, and system performance.

---

## 🎮 Universal Command

| Command    | Description                                  |
| :--------- | :------------------------------------------- |
| `/pnc gui` | Opens the main configuration screen for all users. |

---

## 🎯 **Perfect For**

- **Community Servers**: Create engaging server-wide Pokémon hunting events
- **Competitive Play**: Organize tournaments and challenges with built-in tracking
- **Casual Servers**: Enhance the hunting experience with smart notifications
- **Large Communities**: Manage multiple events and player progress efficiently

---

## 🤝 Contributing & Issues

Found a bug or have a great idea for a new feature? We'd love to hear from you!

Please report any issues or submit your suggestions on our GitHub Issues page.

You can also check out the source code and contribute to the project at our GitHub repository.

---

## 📋 **Installation & Compatibility**

### Required Dependencies
- **Minecraft Version**: 1.21.1
- **Fabric Loader**: 0.17.3+
- **Fabric API**: 0.104.0+1.21.1
- **Cobblemon**: 1.7.0+1.21.1

### Optional Dependencies
- **Xaero's Minimap/World Map**: For enhanced waypoint integration
- **AdvancementPlaques + Iceberg**: For stylized event notifications
- **ModMenu**: For easy mod configuration access

## 🔄 Version Compatibility

| Mod Version | Cobblemon Version | Minecraft Version | Status |
|-------------|-------------------|-------------------|--------|
| 1.5.x       | 1.7.0+           | 1.21.1           | ✅ Current |
| 1.4.x       | 1.6.1            | 1.21.1           | 🔒 Legacy |

### Installation Steps
1. Ensure you have Fabric Loader installed
2. Download the required dependencies listed above
3. Download **Poke Notifier v1.5.0** for Cobblemon 1.7.0+
4. Place all .jar files into your `mods` folder
5. Install optional dependencies for enhanced features
6. Start the game and use `/pnc gui` to access all features!

## 🚀 **Getting Started**

### For Players
- Use `/pnc gui` to access all mod features
- Configure notifications in the User Tools tab
- Track active events in the Events tab
- Customize map settings for waypoint integration

### For Server Admins
- Access advanced controls through the Admin Tools tab
- Create server events from the Events management panel
- Monitor server status with real-time system logs
- Manage player data and test features safely

### Creating Your First Event
1. Open `/pnc gui` as an admin
2. Navigate to Events → Swarm Events
3. Enter a Pokémon name and click "Start Swarm"
4. Watch as players receive notifications and waypoints are created
5. Monitor the event progress in real-time

Start building engaging Pokémon hunting experiences for your community!