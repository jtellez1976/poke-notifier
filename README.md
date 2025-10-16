# Poke Notifier v1.1.0 - The Prestige & Events Update! is now live!!

Get ready for a completely new hunting experience! Version 1.1.0 brings dynamic event systems, a visual prestige system, and a huge number of quality-of-life improvements that make the mod more robust and fun than ever before.

## ✨ New Core Features

### 🏆 "Catch 'em All" Prestige System
The "Catch 'em All" mode has been completely revamped with a deep progression and reward system:
*   **Visual Ranks:** Show off your dedication! As you complete the Pokédex for each generation, your name in chat and above your head will gain a unique, colorful rank prefix that evolves with your achievements.
*   **3D Trophies:** Upon completing a generation, you will be awarded a **unique trophy** for that region. Place it in the world to display a slowly rotating 3D model that emits light and particles!
*   **Configurable Rewards:** Server admins can now define custom item rewards for players who complete each generation.
*   **Epic Celebrations:** Enjoy a **firework show** that grows in intensity with every Pokédex you complete. Masters who achieve it all will be shrouded in a unique aura of power!

### 🎯 Autonomous Bounty System
The server now creates its own challenges!
*   The system will automatically post bounties for random Pokémon (from the RARE and ULTRA_RARE lists).
*   The first player to capture the target Pokémon claims the reward.
*   Fully configurable: Admins can adjust the frequency, probability, duration, and rewards for bounty events.

### 🌊 Autonomous Swarm System
The world now feels more alive than ever!
*   The server can now trigger **Pokémon Swarms** (10-15 creatures) in random, accessible locations in the Overworld.
*   A **global announcement** is sent to all players with the Pokémon's name, the biome, and the **exact coordinates** of the event, starting a server-wide race to get there first!
*   Admins can manually trigger swarms with the `/pokenotifier swarm start <pokemon>` command.

### ⚔️ Rival System
Friendly competition has arrived.
*   When you're in "Catch 'em All" mode, if you capture a Pokémon that a nearby rival hasn't caught yet, that rival will get a private message letting them know you beat them to it!
*   An intelligent cooldown and proximity-override system prevents spam on busy servers, keeping the notifications relevant and fun.

### 🔄 Update Checker
*   The mod now checks if new versions are available on Modrinth or CurseForge.
*   Admins are notified in-game and can configure their preferred update source with a single click, no file editing required.

## 🔧 Technical Improvements & Fixes
*   **Data Security:** Player progress files are now encrypted to prevent manipulation.
*   **Automatic Migration:** Server configurations are automatically updated to new versions, preserving all admin customizations.
*   **Polished Commands:** The command interface has been cleaned up, hiding all internal commands from the autocomplete list for a cleaner admin experience.
*   **Visual Fix:** Fixed a bug that caused rank icons to be tinted by the color of chat text.
*   **Improved Compatibility:** Refactored all entity creation logic to use stable Cobblemon APIs, ensuring better future compatibility.



# Poke Notifier

**Never miss a rare Pokémon again!**

Poke Notifier is a server-side mod for Cobblemon designed to dramatically enhance the experience of finding and hunting rare Pokémon. It alerts players when a special Pokémon spawns, providing its name, rarity, and coordinates.

---

## ✨ Key Features

- **Rare Spawn Notifications:** Get real-time alerts when Legendary, Mythical, Shiny, Ultra Beast, and other rare Pokémon appear.
- **Multiple Alert Channels:** Receive notifications via chat messages, an on-screen HUD (Toast), and sound alerts. Fully configurable by the player!
- **"Catch 'em All" Mode & Prestige System:** The ultimate challenge for collectors!
  - Activate tracking for a specific generation and receive special notifications for Pokémon you haven't caught yet.
  - As you complete generations, your name in chat will gain a **visual rank prefix** that evolves with your achievements.
  - Upon completing a regional Pokédex, you will receive a **unique, placeable 3D trophy item**.
  - Enjoy **special effects and fireworks** to celebrate your accomplishments!
- **Autonomous Bounty System:** The server will automatically post bounties for random Pokémon, with configurable rewards for the first trainer to capture them!
- **Autonomous Swarm System:** Participate in community hunting events! The server will generate Pokémon swarms at random locations, announcing the coordinates to all players.
- **Rival System:** Engage in friendly competition. Get private notifications when a rival in "Catch 'em All" mode beats you to a capture.
- **Pokédex Synchronization:** Don't lose your progress! When using "Catch 'em All" mode for the first time, the mod **scans your PC and party** to credit you for all the Pokémon you've already caught. It's safe to install on existing servers.

---

## 🎮 Player Commands (Client)

All player commands start with `/pnc`.

| Command                               | Description                                         |
| :------------------------------------ | :-------------------------------------------------- |
| `/pnc help`                           | Displays a list of all available player commands.   |
| `/pnc status`                         | Shows your current notification settings.           |
| `/pnc silent <on|off>`                | Master switch to disable/enable all alerts.         |
| `/pnc alerts <chat|toast|sound> <on|off>` | Toggles a specific notification channel.            |
| `/pnc customcatch <add|remove> <pokemon>` | Adds or removes a Pokémon from your custom hunt list. |
| `/pnc customcatch <view|clear>`       | Views or clears your entire custom hunt list.       |
| `/pnc catchemall enable <generation>` | Starts tracking a Pokédex generation.               |
| `/pnc catchemall disable <generation>`| Stops tracking a generation.                        |
| `/pnc catchemall status`              | Shows which generation you are currently tracking.  |

---

## 🛠️ Admin Commands (Server)

All admin commands start with `/pokenotifier` and require permission level 2 (OP).

| Command                                       | Description                                                  |
| :-------------------------------------------- | :----------------------------------------------------------- |
| `/pokenotifier help`                          | Displays a list of all available admin commands.             |
| `/pokenotifier status`                        | Shows the server's current configuration status.             |
| `/pokenotifier config <reload/reset>`         | Reloads all configs or generates new default ones.           |
| `/pokenotifier test debug <enable/disable>`   | Toggles detailed debug logs in the server console.           |
| `/pokenotifier test mode <enable/disable>`    | Toggles notifications for non-natural (command) spawns.      |
| `/pokenotifier test spawn <pokemon> [shiny]`  | Spawns a specific Pokémon for testing.                       |
| `/pokenotifier data autocomplete <player>`    | **(Testing)** Autocompletes the active gen for a player.     |
| `/pokenotifier data rollback <player>`        | **(Testing)** Restores a player's progress from a backup.    |
| `/pokenotifier bounty system <enable/disable>`| Toggles the automatic bounty system.                         |
| `/pokenotifier swarm start <pokemon>`         | Manually starts a swarm of a specific Pokémon.             |

---

## 🤝 Contributing & Issues

Found a bug or have a great idea for a new feature? We'd love to hear from you!

Please report any issues or submit your suggestions on our GitHub Issues page.

You can also check out the source code and contribute to the project at our GitHub repository.

---

## 🔧 Installation

1.  Ensure you have Fabric Loader installed.
2.  Download the correct version of Fabric API and Cobblemon.
3.  Download the latest version of **Poke Notifier**.
4.  Place all three .jar files into your `mods` folder.
5.  (Optional) For more stylized mode activation notifications, install `AdvancementPlaques` and its dependency, `Iceberg`.

Start the game and enjoy the hunt!