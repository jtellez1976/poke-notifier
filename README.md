# Poke Notifier

**Never miss a rare Pokémon again!**

Poke Notifier is a server-side mod for Cobblemon designed to dramatically enhance the experience of finding and hunting rare Pokémon. It alerts players when a special Pokémon spawns, providing its name, rarity, and coordinates.

---

### ⭐ What's New in v1.1.0 (Development) ⭐

This version introduces major under-the-hood improvements and a complete command overhaul for a more professional and user-friendly experience.

*   **Data Encryption:** Player progress files are now fully encrypted to prevent tampering and ensure fair play.
*   **Automatic Data Migration:** Existing players' progress is automatically and safely migrated to the new secure format.
*   **Hierarchical Command Structure:** All commands have been reorganized into logical subcommands (e.g., `/pnc alerts chat`, `/pokenotifier test spawn`) for a more intuitive and scalable system.
*   **Enhanced Server Logs:** The mod now features a clean, professional startup banner in the server console for better visibility.

---

## ✨ Key Features

- **Rare Spawn Notifications:** Get real-time alerts when a Legendary, Mythical, Shiny, Ultra Beast, and other rare Pokémon appear.
- **Multiple Alert Channels:** Receive notifications via chat messages, an on-screen HUD (Toast), and sound alerts. Fully configurable by the player!
- **"Catch 'em All" Mode:** The ultimate challenge for collectors! Enable tracking for a specific generation and receive special notifications for Pokémon you haven't caught yet.
- **Visual Chat Rank System:** Show off your progress in style! As you complete generations, your name in the chat will gain a unique, multi-colored rank prefix that evolves with your achievements.
    - **Trainee:** `🛡[Trainee]`
    - **Great:** `🏆 [Great]`
    - **Expert:** `🏆🏆 [Expert]`
    - **Veteran:** `🏆🏆🏆 [Veteran]`
    - **Master:** `⚡🏆⚡[Master]` (with your username in gold).
- **Prestige Trophy System:** Upon completing a region's Pokédex, you will receive a **unique trophy badge**. This collectible item can be placed in the world, where it will display as a **slowly rotating 3D model** that emits light and particle effects.
- **Spectacular Celebration Effects:**
    - **Dynamic Fireworks:** Upon completing any generation, the sky ignites with a celebratory firework display that **grows in scale and intensity** as you complete more Pokédexes!
    - **"Pokémon Master" Aura:** When you complete all 9 generations, your character is enveloped in a unique aura of power, combining the ethereal particles of the **Totem of Undying** and the swirling energy of the **End Portal**.
- **Master's Welcome:** Players who reach the "Master" rank are announced with a **global thunder sound** and a golden chat message every time they join the server.
- **Pokédex Synchronization:** Don't lose your progress! When using the "Catch 'em All" mode for the first time, the mod **scans your PC and party** to credit you for all the Pokémon you've already caught. It's safe to install on existing servers.
- **Configurable Rewards:** The rewards for completing a generation are **fully customizable** by server administrators.

---

## 🎮 Player Commands (Client)

All player commands start with `/pnc`.

| Command                               | Description                                         |
| :------------------------------------ | :-------------------------------------------------- |
| `/pnc help`                           | Displays a list of all available player commands.   |
| `/pnc status`                         | Shows your current notification settings.           |
| `/pnc silent <on/off>`                | Master switch to disable/enable all alerts.         |
| `/pnc alerts <chat/toast/sound> <on/off>` | Toggles a specific notification channel.            |
| `/pnc customcatch <add/remove> <pokemon>` | Adds or removes a Pokémon from your custom hunt list. |
| `/pnc customcatch <view/clear>`       | Views or clears your entire custom hunt list.       |
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
| `/pokenotifier data autocomplete <player> <gen>`| **(Testing)** Autocompletes a generation for a player.       |
| `/pokenotifier data rollback <player>`        | **(Testing)** Restores a player's progress from a backup.    |

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