# Poke Notifier v1.3.0 - The Unified GUI & Admin Tools Update!

This version introduces a massive overhaul to the user and admin experience by unifying all configuration options into a single, powerful, and intuitive Graphical User Interface (GUI). The mod is now easier to use for players and significantly more powerful for server operators.

## ✨ New Core Features in v1.3.0

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
- **Event Management:** Enable or disable the `Bounty System` and manually trigger a `Swarm` of any Pokémon.
- **Player Data Management:** Easily select an online player from an autocomplete list to `Autocomplete` a generation's progress or `Rollback` their data from a backup.
- **Testing:** A dedicated panel to spawn any Pokémon for testing, with a simple checkbox to make it `Shiny`.

---

## 🎮 Universal Command

| Command    | Description                                  |
| :--------- | :------------------------------------------- |
| `/pnc gui` | Opens the main configuration screen for all users. |

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