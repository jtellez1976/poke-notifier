# Poke Notifier v1.6.0 - Player Experience Update!

Poke Notifier has evolved from a simple notification mod into a comprehensive server event management system! This latest version introduces the **Poké Notifier Device** - a personal tracking tool that puts all mod features at your fingertips.

## 🔧 Latest Updates in v1.6.0

### 🌐 **Client-Only Mode - NEW!**
Use Poke Notifier on **ANY server** - even without the mod installed!

- **Automatic Detection**: Mod detects if server has it installed (3-second check)
- **Full Functionality**: Custom Hunt and Catch 'em All work everywhere
- **Local Storage**: Your data saved locally in client-only mode
- **Smart GUI**: Admin tabs automatically hidden on servers without mod
- **Seamless Switch**: Automatically uses full features when available

#### What Works in Client-Only Mode:
✅ Rarity detection (Legendary, Mythical, Ultra Rare, Shiny)
✅ Custom Hunt list management
✅ Catch 'em All tracking with progress HUD
✅ Chat notifications with clickable coordinates
✅ Sound notifications
✅ Manual waypoint creation ([ADD] button)
✅ Waypoint auto-cleanup when caught
✅ Toast messages with region names

#### Server-Only Features:
❌ Admin Tools (requires OP permissions)
❌ Events (Swarm, Global Hunt)
❌ Trophy system
❌ First join gift

### 🎮 **Poké Notifier Device**
- **Personal Tracking Device**: Craftable item that provides instant access to all mod features
- **Quick Access**: Press **[N]** or right-click the device to open the configuration GUI
- **Equipable**: Can be equipped in Trinkets necklace slot for hands-free access
- **Ownership System**: Each device displays its crafter/owner like Cobblemon badges
- **First Join Gift**: New players receive a free device on their first server join
- **Tech-Themed Effects**: Beacon sounds and portal particles when equipped
- **Fireproof**: Won't be destroyed by lava or fire

### 📜 **Crafting Recipe**
```
[Paper]    [Redstone] [Paper]
[Iron]     [Clock]    [Iron]
[Paper]    [Redstone] [Paper]
```
Accessible early-game recipe using common materials!

### ⌨️ **Keybind System**
- **Default Key**: [N] - Opens GUI when you have the device
- **Customizable**: Rebind in Controls menu under "Poke Notifier" category
- **Smart Detection**: Works with device in inventory, hotbar, or equipped

### 🎒 **Trinkets Integration**
- **Necklace Slot**: Equip device for hands-free GUI access
- **Bundled**: Trinkets API included - no separate download needed
- **Graceful Fallback**: Works perfectly without Trinkets installed

## 🔧 Updates from v1.5.0

### ✅ **Cobblemon 1.7.0 Compatibility**
- **Full API Compatibility**: Updated all Pokemon display name calls for Cobblemon 1.7.0
- **Tested & Verified**: Complete singleplayer and multiplayer testing confirmed
- **No Breaking Changes**: Seamless upgrade from v1.4.x with no data loss
- **Enhanced Pokemon Lists**: Updated and corrected Pokemon categorization for accuracy

### 🗺️ **Map Integration Simplification**
- **Xaero's Minimap Only**: Streamlined to single map provider for better reliability
- **JourneyMap Removed**: Eliminated non-functional integration
- **Improved Performance**: Reduced overhead with simplified architecture
- **Runtime Detection**: Automatic detection without hard dependencies

### 🎨 **Creative Mode Tab**
- **Custom Item Group**: New "Poke Notifier" tab in creative inventory
- **All Items Accessible**: 9 generation trophies + 3 trophy blocks
- **Organized Display**: Trophies first, then blocks for easy access
- **Kanto Trophy Icon**: Distinctive tab icon for quick identification

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

## 🎮 Universal Access

| Method    | Description                                  |
| :--------- | :------------------------------------------- |
| **Poké Notifier Device** | Right-click or press **[N]** to open GUI (all players) |
| `/pnc gui` | Command access (OP only) |

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
| 1.6.x       | 1.7.0+           | 1.21.1           | ✅ Current |
| 1.5.x       | 1.7.0+           | 1.21.1           | 🔒 Legacy |
| 1.4.x       | 1.6.1            | 1.21.1           | 🔒 Legacy |

### Installation Steps
1. Ensure you have Fabric Loader installed
2. Download the required dependencies listed above
3. Download **Poke Notifier v1.6.0** for Cobblemon 1.7.0+
4. Place all .jar files into your `mods` folder
5. Install optional dependencies for enhanced features
6. Start the game - you'll receive a Poké Notifier Device on first join!
7. Press **[N]** or right-click the device to access all features!

## 🚀 **Getting Started**

### 🌐 Client-Only Mode vs Full Mode

**Client-Only Mode** (Server WITHOUT mod):
- Automatic detection when joining servers
- Local data storage in `config/poke-notifier/client-only-data/`
- Custom Hunt and Catch 'em All fully functional
- No admin features (expected)
- Works on vanilla servers, modded servers, anywhere!

**Full Mode** (Server WITH mod):
- All features available
- Server-side data synchronization
- Admin tools for OPs
- Events system (Swarm, Global Hunt)
- Trophy system and rewards

### Important Note: Data Separation
**When playing in singleplayer (client-only mode) and then opening a local server with the same world**, the Pokémon catch progress will appear "shared" between both modes. This is expected behavior because both modes read from the same Cobblemon world data (Party + PC). 

**On external servers**, the data is completely separated since each server has its own independent world data.

### For New Players
1. Join the server for the first time
2. Receive your free Poké Notifier Device
3. Press **[N]** or right-click the device to open the GUI
4. Configure your notifications in the User Tools tab
5. Start tracking Pokémon and participating in events!

### Crafting Your Own Device
If you lose your device or want extras:
```
[Paper]    [Redstone] [Paper]
[Iron]     [Clock]    [Iron]
[Paper]    [Redstone] [Paper]
```

### For Players
- Press **[N]** or right-click your device to access all features
- Configure notifications in the User Tools tab
- Track active events in the Events tab
- Customize map settings for waypoint integration
- Equip device in Trinkets necklace slot for hands-free access

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