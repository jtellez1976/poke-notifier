# Poke Notifier

**Never miss a rare Pokémon again!**

Poke Notifier is a server-side mod for Cobblemon designed to dramatically enhance the experience of finding and hunting rare Pokémon. It alerts players when a special Pokémon spawns, providing its name, rarity, and coordinates.

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
  - **Fireworks:** When you complete any generation, the sky will light up with fireworks launched from your position!
  - **"Pokémon Master" Effects:** Upon completing all 9 generations, you will be enveloped in an aura of power, combining the visual effects of the Totem of Undying and the End Portal.
- **Master's Welcome:** Players who reach the "Master" rank are announced with a **global thunder sound** and a golden chat message every time they join the server.
- **Pokédex Synchronization:** Don't lose your progress! When using the "Catch 'em All" mode for the first time, the mod **scans your PC and party** to credit you for all the Pokémon you've already caught. It's safe to install on existing servers.
- **Configurable Rewards:** The rewards for completing a generation are **fully customizable** by server administrators.

---

## 🎮 Player Commands (Client)

All player commands start with `/pnc`.

### Alert Management
| Command                 | Description                                                      |
| :---------------------- | :--------------------------------------------------------------- |
| `/pnc status`           | Shows the current status of all your client-side settings.       |
| `/pnc silent ON/OFF`    | Toggles **Silent Mode**, a master switch for all notifications.    |
| `/pnc alert_chat ON/OFF`  | Toggles chat notifications.                                      |
| `/pnc alert_toast ON/OFF` | Toggles on-screen HUD (Toast) notifications.                     |
| `/pnc alert_sound ON/OFF` | Toggles sound alerts.                                            |

### Custom Hunt List
| Command                       | Description                               |
| :---------------------------- | :---------------------------------------- |
| `/pnc customlist add <pokemon>`   | Adds a Pokémon to your custom hunt list.  |
| `/pnc customlist remove <pokemon>`| Removes a Pokémon from your list.       |
| `/pnc customlist list`        | Shows all Pokémon currently on your list. |
| `/pnc customlist clear`       | Completely clears your hunt list.         |

### "Catch 'em All" Mode
| Command                           | Description                                                      |
| :-------------------------------- | :--------------------------------------------------------------- |
| `/pnc catchemall enable <generation>` | Activates hunt mode for a specific generation (e.g., `gen1`, `gen2`). |
| `/pnc catchemall disable <generation>`| Deactivates hunt mode for that generation.                     |
| `/pnc catchemall list`            | Shows which generation you are currently tracking.               |

---

## 🛠️ Admin Commands (Server)

All admin commands start with `/pokenotifier` and require permission level 2 (OP).

| Command                                   | Description                                                                    |
| :---------------------------------------- | :----------------------------------------------------------------------------- |
| `/pokenotifier status`                    | Shows the status of the server-side configuration.                             |
| `/pokenotifier reloadconfig`              | Reloads all mod configuration files without a server restart.                  |
| `/pokenotifier reloadconfig new`          | Deletes current configurations and generates new default ones.                 |
| `/pokenotifier debug_mode enable/disable` | Toggles detailed debug logs in the server console.                             |
| `/pokenotifier test_mode enable/disable`  | Toggles test mode. When active, the mod will also notify non-natural spawns. |
| `/pokenotifier testspawn <pokemon> [shiny]` | Spawns a specific Pokémon for testing. Only works if `test_mode` is enabled.   |
| `/pokenotifier autocompletegen <player> <gen>` | **(Testing)** Completes 99% of a player's Pokédex for a specific generation. |
| `/pokenotifier rollback <player>`         | **(Testing)** Restores a player's progress from the backup.                    |

---

## 🔧 Installation

1.  Ensure you have Fabric Loader installed.
2.  Download the correct version of Fabric API and Cobblemon.
3.  Download the latest version of **Poke Notifier**.
4.  Place all three .jar files into your `mods` folder.
5.  (Optional) For more stylized mode activation notifications, install `AdvancementPlaques` and its dependency, `Iceberg`.

Start the game and enjoy the hunt!