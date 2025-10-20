/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.api.PokeNotifierApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import com.zehro_mc.pokenotifier.ConfigServer;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PokeNotifierCustomScreen extends Screen {
    private final Screen parent;
    private AutocompleteTextFieldWidget pokemonNameField;
    private AutocompleteTextFieldWidget playerNameField; // For admin tools
    private CheckboxWidget shinyCheckbox; // For test spawn
    private ConfigClient clientConfig;
    private ConfigServer serverConfig;

    private List<Text> responseLines = new ArrayList<>();
    private int responseTimer = 0;

    private enum MainCategory { USER_TOOLS, EVENTS, ADMIN_TOOLS }
    private MainCategory currentMainCategory = MainCategory.USER_TOOLS;

    private enum UserSubCategory { NOTIFICATIONS, CUSTOM_HUNT, CATCH_EM_ALL, INFO }
    private UserSubCategory currentUserSubCategory = UserSubCategory.NOTIFICATIONS;

    private enum EventSubCategory { GLOBAL_HUNT, BOUNTY_SYSTEM, SWARM_EVENTS, RIVAL_BATTLES }
    private EventSubCategory currentEventSubCategory = EventSubCategory.GLOBAL_HUNT;

    private enum AdminSubCategory { SERVER_CONTROL, PLAYER_DATA, TESTING }
    private AdminSubCategory currentAdminSubCategory = AdminSubCategory.SERVER_CONTROL;

    public PokeNotifierCustomScreen(Screen parent) {
        // FIX: Title is now set dynamically in the constructor based on player role.
        // This is safe because the server sends an AdminStatusPayload right before opening the GUI.
        super(Text.literal(PokeNotifierClient.isPlayerAdmin ? "Poke Notifier Configurations (Admin)" : "Poke Notifier Configurations (User)"));
        this.parent = parent;
        // FIX: Set the default main category for admins.
        if (PokeNotifierClient.isPlayerAdmin) {
            this.currentMainCategory = MainCategory.ADMIN_TOOLS;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clientConfig = ConfigManager.getClientConfig();
        this.serverConfig = ConfigManager.getServerConfig();
        buildLayout();
    }

    private void buildLayout() {
        // Compact centered panel design
        int panelWidth = 420;
        int panelHeight = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        // --- Main Tabs (Top) ---
        int tabY = panelY + 25;
        int tabWidth = PokeNotifierClient.isPlayerAdmin ? 70 : 100;
        ButtonWidget userTab = ButtonWidget.builder(Text.literal("User Tools"), b -> {
            this.currentMainCategory = MainCategory.USER_TOOLS;
            this.clearAndInit();
        }).dimensions(panelX + 5, tabY, tabWidth, 20)
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Configure personal settings and notifications")))
        .build();
        userTab.active = this.currentMainCategory != MainCategory.USER_TOOLS;
        addDrawableChild(userTab);

        if (PokeNotifierClient.isPlayerAdmin) {
            ButtonWidget eventsTab = ButtonWidget.builder(Text.literal("🎪 Events"), b -> {
                this.currentMainCategory = MainCategory.EVENTS;
                this.clearAndInit();
            }).dimensions(panelX + 10 + tabWidth, tabY, tabWidth, 20)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Manage server events and systems")))
            .build();
            eventsTab.active = this.currentMainCategory != MainCategory.EVENTS;
            addDrawableChild(eventsTab);

            ButtonWidget adminTab = ButtonWidget.builder(Text.literal("👑 Admin"), b -> {
                this.currentMainCategory = MainCategory.ADMIN_TOOLS;
                this.clearAndInit();
            }).dimensions(panelX + 15 + tabWidth * 2, tabY, tabWidth, 20)
            .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Server administration and testing tools")))
            .build();
            adminTab.active = this.currentMainCategory != MainCategory.ADMIN_TOOLS;
            addDrawableChild(adminTab);
        }

        // --- Build Content based on Main Tab ---
        if (currentMainCategory == MainCategory.USER_TOOLS) {
            buildUserToolsLayout(panelX, panelY, panelWidth, panelHeight);
        } else if (currentMainCategory == MainCategory.EVENTS && PokeNotifierClient.isPlayerAdmin) {
            buildEventsLayout(panelX, panelY, panelWidth, panelHeight);
        } else if (currentMainCategory == MainCategory.ADMIN_TOOLS && PokeNotifierClient.isPlayerAdmin) {
            buildAdminToolsLayout(panelX, panelY, panelWidth, panelHeight);
        }

        // Close Button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(panelX + (panelWidth - 80) / 2, panelY + panelHeight - 28, 80, 18)
                .build());
    }

    // --- USER TOOLS LAYOUT ---
    private void buildUserToolsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        int navX = panelX + 10;
        int navY = panelY + 55;
        int navWidth = 100;

        addDrawableChild(createSubNavButton(navX, navY, navWidth, "🔔 Notifications", UserSubCategory.NOTIFICATIONS, currentUserSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 22, navWidth, "🎯 Custom Hunt", UserSubCategory.CUSTOM_HUNT, currentUserSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 44, navWidth, "🏆 Catch 'em All", UserSubCategory.CATCH_EM_ALL, currentUserSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 66, navWidth, "ℹ️ Info & Help", UserSubCategory.INFO, currentUserSubCategory));

        int contentX = panelX + navWidth + 20;
        int contentY = panelY + 55;
        int contentWidth = panelWidth - navWidth - 35;

        switch (currentUserSubCategory) {
            case NOTIFICATIONS -> buildNotificationsPanel(contentX, contentY, contentWidth);
            case CUSTOM_HUNT -> buildCustomHuntPanel(contentX, contentY, contentWidth);
            case CATCH_EM_ALL -> buildCatchEmAllPanel(contentX, contentY, contentWidth);
            case INFO -> buildInfoPanel(contentX, contentY, contentWidth);
        }
    }

    // --- EVENTS LAYOUT ---
    private void buildEventsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        // Left navigation panel for events
        int navX = panelX + 10;
        int navY = panelY + 55;
        int navWidth = 120;
        
        // Event navigation buttons
        addDrawableChild(createEventNavButton(navX, navY, navWidth, "🌍 Global Hunt", EventSubCategory.GLOBAL_HUNT, PokeNotifierClient.isGlobalHuntSystemEnabled));
        addDrawableChild(createEventNavButton(navX, navY + 22, navWidth, "💰 Bounty System", EventSubCategory.BOUNTY_SYSTEM, PokeNotifierClient.isServerBountySystemEnabled));
        addDrawableChild(createEventNavButton(navX, navY + 44, navWidth, "🌪️ Swarm Events", EventSubCategory.SWARM_EVENTS, false));
        addDrawableChild(createEventNavButton(navX, navY + 66, navWidth, "⚔️ Rival Battles", EventSubCategory.RIVAL_BATTLES, true));
        
        // Right details panel
        int detailsX = panelX + navWidth + 20;
        int detailsY = panelY + 55;
        int detailsWidth = panelWidth - navWidth - 35;
        
        // Build details panel based on selected event
        switch (currentEventSubCategory) {
            case GLOBAL_HUNT -> buildGlobalHuntDetailsPanel(detailsX, detailsY, detailsWidth);
            case BOUNTY_SYSTEM -> buildBountySystemDetailsPanel(detailsX, detailsY, detailsWidth);
            case SWARM_EVENTS -> buildSwarmEventsDetailsPanel(detailsX, detailsY, detailsWidth);
            case RIVAL_BATTLES -> buildRivalBattlesDetailsPanel(detailsX, detailsY, detailsWidth);
        }
    }
    
    private ButtonWidget createEventNavButton(int x, int y, int width, String text, EventSubCategory category, boolean enabled) {
        String status = enabled ? "ON" : "OFF";
        Text buttonText = Text.literal(text + ": " + status)
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
        
        ButtonWidget button = ButtonWidget.builder(buttonText, b -> {
            this.currentEventSubCategory = category;
            this.clearAndInit();
        }).dimensions(x, y, width, 18).build();
        
        button.active = !category.equals(currentEventSubCategory);
        return button;
    }

    // --- ADMIN TOOLS LAYOUT ---
    private void buildAdminToolsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        int navX = panelX + 10;
        int navY = panelY + 55;
        int navWidth = 110;

        addDrawableChild(createSubNavButton(navX, navY, navWidth, "⚙️ Server Control", AdminSubCategory.SERVER_CONTROL, currentAdminSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 22, navWidth, "👤 Player Data", AdminSubCategory.PLAYER_DATA, currentAdminSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 44, navWidth, "🔬 Testing", AdminSubCategory.TESTING, currentAdminSubCategory));

        int contentX = panelX + navWidth + 20;
        int contentY = panelY + 55;
        int contentWidth = panelWidth - navWidth - 35;

        switch (currentAdminSubCategory) {
            case SERVER_CONTROL -> buildServerControlPanel(contentX, contentY, contentWidth);
            case PLAYER_DATA -> buildPlayerDataPanel(contentX, contentY, contentWidth);
            case TESTING -> buildTestingPanel(contentX, contentY, contentWidth);
        }
    }

    private <T extends Enum<T>> ButtonWidget createSubNavButton(int x, int y, int width, String text, T category, T current) {
        String tooltip = getTooltipForCategory(category);
        ButtonWidget button = ButtonWidget.builder(Text.literal(text), b -> {
            if (category instanceof UserSubCategory) this.currentUserSubCategory = (UserSubCategory) category;
            if (category instanceof EventSubCategory) this.currentEventSubCategory = (EventSubCategory) category;
            if (category instanceof AdminSubCategory) this.currentAdminSubCategory = (AdminSubCategory) category;
            this.clearAndInit();
        }).dimensions(x, y, width, 18)
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(tooltip)))
        .build();
        button.active = !category.equals(current);
        return button;
    }
    
    private <T extends Enum<T>> String getTooltipForCategory(T category) {
        if (category instanceof UserSubCategory) {
            return switch ((UserSubCategory) category) {
                case NOTIFICATIONS -> "Configure chat, sound, and HUD alerts";
                case CUSTOM_HUNT -> "Manage your personal Pokémon hunt list";
                case CATCH_EM_ALL -> "Track Pokédex completion by generation";
                case INFO -> "View help, version, and mod information";
            };
        } else if (category instanceof AdminSubCategory) {
            return switch ((AdminSubCategory) category) {
                case SERVER_CONTROL -> "Toggle server modes and reload configs";
                case PLAYER_DATA -> "Manage player progress and backups";
                case TESTING -> "Spawn test Pokémon for debugging";
            };
        }
        return "";
    }

    // --- USER PANEL BUILDERS ---

    private void buildNotificationsPanel(int x, int y, int width) {
        // Create buttons for Chat, Sound, and HUD alerts
        addDrawableChild(createToggleButton("Chat Alerts", clientConfig.alert_chat_enabled, newValue -> clientConfig.alert_chat_enabled = newValue, x, y, width));
        addDrawableChild(createToggleButton("Sound Alerts", clientConfig.alert_sounds_enabled, newValue -> clientConfig.alert_sounds_enabled = newValue, x, y + 25, width));
        addDrawableChild(createToggleButton("HUD Alerts", clientConfig.alert_toast_enabled, newValue -> clientConfig.alert_toast_enabled = newValue, x, y + 50, width));

        // Create the Silent Mode button with improved logic
        Text silentModeText = Text.literal("Silent Mode: ").append(clientConfig.silent_mode_enabled ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        addDrawableChild(ButtonWidget.builder(silentModeText, button -> {
            clientConfig.silent_mode_enabled = !clientConfig.silent_mode_enabled;
            if (clientConfig.silent_mode_enabled) {
                // When silent mode is turned ON, turn off all other alerts
                clientConfig.alert_chat_enabled = false;
                clientConfig.alert_sounds_enabled = false;
                clientConfig.alert_toast_enabled = false;
            } else {
                // FIX: When silent mode is turned OFF, re-enable all other alerts
                clientConfig.alert_chat_enabled = true;
                clientConfig.alert_sounds_enabled = true;
                clientConfig.alert_toast_enabled = true;
            }
            // Re-initialize the screen to update all button states
            this.clearAndInit();
            displayResponse(List.of(Text.literal("Settings updated.").formatted(Formatting.GREEN)));
        }).dimensions(x, y + 85, width, 20).build());

        addDrawableChild(createToggleButton("Searching", clientConfig.searching_enabled, newValue -> clientConfig.searching_enabled = newValue, x, y + 110, width));
    }

    private void buildCustomHuntPanel(int x, int y, int width) {
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("e.g., Pikachu"));
        addDrawableChild(this.pokemonNameField);

        // Add button
        addDrawableChild(ButtonWidget.builder(Text.literal("➕ Add"), b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                    new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                        com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.ADD, pokemonName);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Request sent to add ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" to your custom list.").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            }
        }).dimensions(x, y + 25, width, 20)
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Add the entered Pokémon to your hunt list")))
        .build());

        // Clear List button
        addDrawableChild(ButtonWidget.builder(Text.literal("🗑️ Clear List"), b -> {
            com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                    com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.CLEAR, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Request sent to clear your custom list.").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 50, width, 20)
        .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Remove all Pokémon from your hunt list")))
        .build());
        
        // View List button
        addDrawableChild(ButtonWidget.builder(Text.literal("📋 View List"), b -> {
            com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                    com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.LIST, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting your custom list from the server...").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 75, width, 20).build());
    }

    private void buildCatchEmAllPanel(int x, int y, int width) {
        List<String> generations = Arrays.asList("gen1", "gen2", "gen3", "gen4", "gen5", "gen6", "gen7", "gen8", "gen9");
        int buttonWidth = (width - 5) / 2;
        int buttonHeight = 20;

        for (int i = 0; i < generations.size(); i++) {
            String gen = generations.get(i);
            int row = i / 2;
            int col = i % 2;
            int buttonX = x + col * (buttonWidth + 5);
            int buttonY = y + row * (buttonHeight + 5);

            ButtonWidget button = ButtonWidget.builder(Text.literal(getGenerationDisplayName(gen)), b -> {
                // Use networking payload instead of command
                com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload payload = 
                    new com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload(
                        com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload.Action.ENABLE, gen);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Requesting to track " + formatGenName(gen) + "...").formatted(Formatting.YELLOW)));
                
                // Update the current generation immediately for visual feedback
                PokeNotifierClient.currentCatchEmAllGeneration = gen;
                this.clearAndInit(); // Refresh the GUI to update button colors
            }).dimensions(buttonX, buttonY, buttonWidth, buttonHeight).build();

            if (gen.equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
                button.setMessage(Text.literal(getGenerationDisplayName(gen)).formatted(Formatting.GOLD, Formatting.UNDERLINE));
            }
            addDrawableChild(button);
        }

        int statusY = y + 5 * (buttonHeight + 5);
        
        // Stop Tracking button
        addDrawableChild(ButtonWidget.builder(Text.literal("⏹️ Stop Tracking"), b -> {
            if (PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
                com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload payload = 
                    new com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload(
                        com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload.Action.DISABLE, 
                        PokeNotifierClient.currentCatchEmAllGeneration);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Requesting to disable tracking...").formatted(Formatting.YELLOW)));
            }
        }).dimensions(x, statusY, width, 20).build());
        
        // View Status button
        addDrawableChild(ButtonWidget.builder(Text.literal("📊 View Status"), b -> {
            com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload payload = 
                new com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload(
                    com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload.Action.LIST, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting your active Catch 'em All modes...").formatted(Formatting.YELLOW)));
        }).dimensions(x, statusY + 25, width, 20).build());
    }

    private void buildInfoPanel(int x, int y, int width) {
        // Help button - client-side only, no admin required
        addDrawableChild(ButtonWidget.builder(Text.literal("Help"), b -> {
            List<Text> helpLines = new ArrayList<>();
            helpLines.add(Text.literal("--- Poke Notifier Help ---").formatted(Formatting.GOLD));
            helpLines.add(Text.literal("Use /pnc gui to access all mod settings.").formatted(Formatting.WHITE));
            helpLines.add(Text.literal("Notifications: Configure chat, sound, and HUD alerts").formatted(Formatting.AQUA));
            helpLines.add(Text.literal("Custom Hunt: Track specific Pokémon you want to find").formatted(Formatting.AQUA));
            helpLines.add(Text.literal("Catch 'em All: Complete Pokédex by generation").formatted(Formatting.AQUA));
            displayResponse(helpLines);
        }).dimensions(x, y, width, 20).build());
        
        // Version button - client-side only, no admin required
        addDrawableChild(ButtonWidget.builder(Text.literal("Version"), b -> {
            String modVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("poke-notifier")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("Unknown");
            List<Text> lines = new ArrayList<>(List.of(Text.literal("Poke Notifier ver. " + modVersion).formatted(Formatting.AQUA)));
            displayResponse(lines);
        }).dimensions(x, y + 25, width, 20).build());
        
        // Status button - client-side only, no admin required
        addDrawableChild(ButtonWidget.builder(Text.literal("Status"), b -> {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("--- Poke Notifier Client Status ---").formatted(Formatting.GOLD));
            lines.add(createClientStatusLine("Searching", clientConfig.searching_enabled));
            lines.add(createClientStatusLine("Silent Mode", clientConfig.silent_mode_enabled));
            lines.add(Text.literal("----------------------------").formatted(Formatting.GOLD));
            lines.add(createClientStatusLine("  Alert Sounds", clientConfig.alert_sounds_enabled));
            lines.add(createClientStatusLine("  Chat Alerts", clientConfig.alert_chat_enabled));
            lines.add(createClientStatusLine("  Toast Alerts (HUD)", clientConfig.alert_toast_enabled));
            displayResponse(lines);
        }).dimensions(x, y + 50, width, 20).build());

        // Only show Update Source selector for admins or in singleplayer
        if (PokeNotifierClient.isPlayerAdmin || (MinecraftClient.getInstance().isInSingleplayer() && MinecraftClient.getInstance().getServer() != null)) {
            // FIX: Use client-side cached value instead of server config that might not be synced
            String currentSource = PokeNotifierClient.currentUpdateSource != null ? PokeNotifierClient.currentUpdateSource : "unknown";
            addDrawableChild(CyclingButtonWidget.<String>builder(value -> Text.literal(value.substring(0, 1).toUpperCase() + value.substring(1)).formatted(Formatting.GOLD))
                    .values("modrinth", "curseforge", "none")
                    .initially(currentSource)
                    .build(x, y + 80, width, 20, Text.literal("Update Source"), (button, value) -> {
                        com.zehro_mc.pokenotifier.networking.UpdateSourcePayload payload = 
                            new com.zehro_mc.pokenotifier.networking.UpdateSourcePayload(value);
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                        // Update client cache immediately for visual feedback
                        PokeNotifierClient.currentUpdateSource = value;
                        displayResponse(List.of(Text.literal("Setting update source to: ").formatted(Formatting.YELLOW)
                                .append(Text.literal(value).formatted(Formatting.GOLD))));
                    }));
        } else {
            // Show message for non-admin users
            addDrawableChild(ButtonWidget.builder(Text.literal("Update Source: Admin Only"), b -> {
                displayResponse(List.of(Text.literal("Only server operators can change the update source.").formatted(Formatting.RED),
                        Text.literal("Contact an admin to modify update settings.").formatted(Formatting.GRAY)));
            }).dimensions(x, y + 80, width, 20).build()).active = false;
        }
    }

    // --- ADMIN PANEL BUILDERS ---

    private void buildServerControlPanel(int x, int y, int width) {
        // Debug Mode toggle
        Text debugModeText = Text.literal("Debug Mode: ").append(PokeNotifierClient.isServerDebugMode ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        addDrawableChild(ButtonWidget.builder(debugModeText, b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_DEBUG_MODE, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling debug mode...").formatted(Formatting.YELLOW)));
            
            // Immediate visual feedback
            PokeNotifierClient.isServerDebugMode = !PokeNotifierClient.isServerDebugMode;
            Text newText = Text.literal("Debug Mode: ").append(PokeNotifierClient.isServerDebugMode ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
            b.setMessage(newText);
        }).dimensions(x, y, width, 20).build());
        
        // Test Mode toggle
        Text testModeText = Text.literal("Test Mode: ").append(PokeNotifierClient.isServerTestMode ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        addDrawableChild(ButtonWidget.builder(testModeText, b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_TEST_MODE, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling test mode...").formatted(Formatting.YELLOW)));
            
            // Immediate visual feedback
            PokeNotifierClient.isServerTestMode = !PokeNotifierClient.isServerTestMode;
            Text newText = Text.literal("Test Mode: ").append(PokeNotifierClient.isServerTestMode ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
            b.setMessage(newText);
        }).dimensions(x, y + 25, width, 20).build());

        // Server Status button
        addDrawableChild(ButtonWidget.builder(Text.literal("Server Status"), b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.SERVER_STATUS, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting server status...").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 60, width, 20).build());
        
        // Reload Configs button
        addDrawableChild(ButtonWidget.builder(Text.literal("Reload Configs"), b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.RELOAD_CONFIG, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Reloading configurations...").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 85, width, 20).build());

        // Reset All Configs button with confirmation
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset All Configs"), b -> {
            this.client.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                        new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                            com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.RESET_CONFIG, "");
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                }
                this.client.setScreen(this);
            }, Text.literal("Confirm Reset"), Text.literal("This will reset ALL configs. Are you sure?")));
        }).dimensions(x, y + 110, width, 20).build());
        
        // Note: Event management has been moved to the Events tab
        addDrawableChild(ButtonWidget.builder(Text.literal("Event Management"), b -> {
            displayResponse(List.of(Text.literal("Event management has been moved to the Events tab.").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 140, width, 20).build()).active = false;
    }

    // --- EVENT PANEL BUILDERS ---
    
    private void buildBountySystemDetailsPanel(int x, int y, int width) {
        // System Toggle
        Text bountyToggleText = Text.literal(PokeNotifierClient.isServerBountySystemEnabled ? "🟢 Disable System" : "🔴 Enable System");
        addDrawableChild(ButtonWidget.builder(bountyToggleText, b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_BOUNTY_SYSTEM, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling bounty system...").formatted(Formatting.YELLOW)));
            
            PokeNotifierClient.isServerBountySystemEnabled = !PokeNotifierClient.isServerBountySystemEnabled;
            Text newText = Text.literal(PokeNotifierClient.isServerBountySystemEnabled ? "🟢 Disable System" : "🔴 Enable System");
            b.setMessage(newText);
        }).dimensions(x, y, width, 20).build());
    }
    
    private void buildSwarmEventsDetailsPanel(int x, int y, int width) {
        // Swarm controls
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width - 40, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("Pokémon Name for Swarm"));
        addDrawableChild(this.pokemonNameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("🌪️ Start Swarm"), b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.START_SWARM, pokemonName);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Starting swarm of ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("...").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            } else {
                displayResponse(List.of(Text.literal("Please enter a Pokémon name first.").formatted(Formatting.RED)));
            }
        }).dimensions(x, y + 25, width, 20).build());
    }
    
    private void buildRivalBattlesDetailsPanel(int x, int y, int width) {
        // Rival system is automatically enabled, only admin can disable it
        Text rivalSystemText = Text.literal("Rival System: ON").formatted(Formatting.GREEN);
        addDrawableChild(ButtonWidget.builder(rivalSystemText, b -> {
            // This system is always on, show info instead
            displayResponse(List.of(
                Text.literal("--- Rival System Information ---").formatted(Formatting.GOLD),
                Text.literal("The Rival system is automatically active.").formatted(Formatting.WHITE),
                Text.literal("When players hunt the same Pokémon and one captures it,").formatted(Formatting.AQUA),
                Text.literal("others receive a friendly rivalry notification.").formatted(Formatting.AQUA),
                Text.literal("Cooldown: 60 seconds between notifications").formatted(Formatting.GRAY),
                Text.literal("Override distance: 200 blocks").formatted(Formatting.GRAY)
            ));
        }).dimensions(x, y, width, 20).build());
        
        // Show current settings
        addDrawableChild(ButtonWidget.builder(Text.literal("📊 View Settings"), b -> {
            displayResponse(List.of(
                Text.literal("--- Rival System Settings ---").formatted(Formatting.GOLD),
                Text.literal("Notification Cooldown: 60 seconds").formatted(Formatting.AQUA),
                Text.literal("Override Distance: 200 blocks").formatted(Formatting.AQUA),
                Text.literal("Status: Always Active").formatted(Formatting.GREEN),
                Text.literal("").formatted(Formatting.WHITE),
                Text.literal("To modify settings, edit config-server.json").formatted(Formatting.GRAY),
                Text.literal("Then use 'Reload Configs' in Admin Tools").formatted(Formatting.GRAY)
            ));
        }).dimensions(x, y + 25, width, 20).build());
    }
    
    private void buildGlobalHuntDetailsPanel(int x, int y, int width) {
        int currentY = y;
        
        // System Toggle
        Text systemToggleText = Text.literal(PokeNotifierClient.isGlobalHuntSystemEnabled ? "🟢 Disable System" : "🔴 Enable System");
        addDrawableChild(ButtonWidget.builder(systemToggleText, b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_GLOBAL_HUNT_SYSTEM, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling Global Hunt system...").formatted(Formatting.YELLOW)));
            
            PokeNotifierClient.isGlobalHuntSystemEnabled = !PokeNotifierClient.isGlobalHuntSystemEnabled;
            Text newText = Text.literal(PokeNotifierClient.isGlobalHuntSystemEnabled ? "🟢 Disable System" : "🔴 Enable System");
            b.setMessage(newText);
        }).dimensions(x, currentY, width / 2 - 2, 20).build());
        
        // Event Status
        addDrawableChild(ButtonWidget.builder(Text.literal("📊 Status"), b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.GLOBAL_HUNT_STATUS, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting Global Hunt status...").formatted(Formatting.YELLOW)));
        }).dimensions(x + width / 2 + 2, currentY, width / 2 - 2, 20).build());
        
        currentY += 25;
        
        // Manual Event Controls
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, currentY, width - 40, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("Pokémon for Event"));
        addDrawableChild(this.pokemonNameField);
        
        this.shinyCheckbox = CheckboxWidget.builder(Text.literal("✨"), this.textRenderer).pos(x + width - 35, currentY).checked(false).build();
        addDrawableChild(this.shinyCheckbox);
        
        currentY += 25;
        
        addDrawableChild(ButtonWidget.builder(Text.literal("🚀 Start Event"), b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                String parameter = pokemonName + (this.shinyCheckbox.isChecked() ? " shiny" : "");
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.START_GLOBAL_HUNT, parameter);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Starting Global Hunt for ").append(Text.literal((this.shinyCheckbox.isChecked() ? "Shiny " : "") + pokemonName).formatted(Formatting.GOLD)).append("...").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            } else {
                displayResponse(List.of(Text.literal("Please enter a Pokémon name first.").formatted(Formatting.RED)));
            }
        }).dimensions(x, currentY, width / 2 - 2, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.literal("❌ Cancel Event"), b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.CANCEL_GLOBAL_HUNT, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Cancelling active Global Hunt...").formatted(Formatting.YELLOW)));
        }).dimensions(x + width / 2 + 2, currentY, width / 2 - 2, 20).build());
        
        currentY += 30;
        
        // Statistics Section (only for Global Hunt)
        Text statsTitle = Text.literal("📈 Global Hunt Statistics").formatted(Formatting.YELLOW);
        addDrawableChild(ButtonWidget.builder(statsTitle, b -> {}).dimensions(x, currentY, width, 20).build()).active = false;
        
        currentY += 25;
        
        Text totalEvents = Text.literal("Total Events: Loading...").formatted(Formatting.GRAY);
        addDrawableChild(ButtonWidget.builder(totalEvents, b -> {}).dimensions(x, currentY, width / 2, 15).build()).active = false;
        
        Text successRate = Text.literal("Success Rate: Loading...").formatted(Formatting.GRAY);
        addDrawableChild(ButtonWidget.builder(successRate, b -> {}).dimensions(x + width / 2, currentY, width / 2, 15).build()).active = false;
    }
    
    private void openGlobalHuntSettings() {
        displayResponse(List.of(
            Text.literal("--- Global Hunt Configuration ---").formatted(Formatting.GOLD),
            Text.literal("Current Settings:").formatted(Formatting.YELLOW),
            Text.literal("• Distance: 1500-4000 blocks from players").formatted(Formatting.AQUA),
            Text.literal("• Duration: 15 minutes per event").formatted(Formatting.AQUA),
            Text.literal("• Dimensions: Overworld only").formatted(Formatting.AQUA),
            Text.literal("• Auto Events: Every 2-6 hours").formatted(Formatting.AQUA),
            Text.literal("").formatted(Formatting.WHITE),
            Text.literal("To modify: Edit events/events_config.json").formatted(Formatting.YELLOW),
            Text.literal("Then use 'Reload Configs' in Admin Tools").formatted(Formatting.GRAY)
        ));
    }
    

    


    private void buildPlayerDataPanel(int x, int y, int width) {
        this.playerNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""),
                () -> this.client.getNetworkHandler().getPlayerList().stream().map(p -> p.getProfile().getName()).toList());
        this.playerNameField.setPlaceholder(Text.literal("Player Name"));
        addDrawableChild(this.playerNameField);

        // Autocomplete Gen button
        ButtonWidget autocompleteButton = ButtonWidget.builder(Text.literal("Autocomplete Gen"), b -> {
            String playerName = this.playerNameField.getText().trim();
            if (!playerName.isEmpty()) {
                this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                            new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                                com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.AUTOCOMPLETE_PLAYER, playerName);
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                    }
                    this.client.setScreen(this);
                }, Text.literal("Confirm Autocomplete"), Text.literal("Complete current gen for " + playerName + "?")));
            } else {
                displayResponse(List.of(Text.literal("Please enter a player name first.").formatted(Formatting.RED)));
            }
        }).dimensions(x, y + 25, width, 20).build();
        addDrawableChild(autocompleteButton);

        // Rollback Progress button
        ButtonWidget rollbackButton = ButtonWidget.builder(Text.literal("Rollback Progress"), b -> {
            String playerName = this.playerNameField.getText().trim();
            if (!playerName.isEmpty()) {
                this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                            new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                                com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.ROLLBACK_PLAYER, playerName);
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                    }
                    this.client.setScreen(this);
                }, Text.literal("Confirm Rollback"), Text.literal("Restore progress backup for " + playerName + "?")));
            } else {
                displayResponse(List.of(Text.literal("Please enter a player name first.").formatted(Formatting.RED)));
            }
        }).dimensions(x, y + 50, width, 20).build();
        addDrawableChild(rollbackButton);
    }

    private void buildTestingPanel(int x, int y, int width) {
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("Pokémon to Spawn"));
        addDrawableChild(this.pokemonNameField);

        this.shinyCheckbox = CheckboxWidget.builder(Text.literal("Shiny"), this.textRenderer).pos(x, y + 25).checked(false).build();
        addDrawableChild(this.shinyCheckbox);

        // Spawn Test Pokémon button
        addDrawableChild(ButtonWidget.builder(Text.literal("Spawn Test Pokémon"), b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                String parameter = pokemonName + (this.shinyCheckbox.isChecked() ? " shiny" : "");
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.SPAWN_POKEMON, parameter);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Spawning ").append(this.shinyCheckbox.isChecked() ? Text.literal("Shiny ").formatted(Formatting.GOLD) : Text.literal("")).append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("...").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            } else {
                displayResponse(List.of(Text.literal("Please enter a Pokémon name first.").formatted(Formatting.RED)));
            }
        }).dimensions(x, y + 50, width, 20).build());
    }



    // --- HELPER METHODS ---

    private ButtonWidget createToggleButton(String label, boolean currentValue, java.util.function.Consumer<Boolean> configUpdater, int x, int y, int width) {
        Text message = Text.literal(label + ": ").append(currentValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        return ButtonWidget.builder(message, button -> {
            boolean newValue = !currentValue;
            configUpdater.accept(newValue);
            button.setMessage(Text.literal(label + ": ").append(newValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED)));
            // FIX: Display feedback for user toggles
            displayResponse(List.of(Text.literal("Settings updated.").formatted(Formatting.GREEN)));
        }).dimensions(x, y, width, 18).build();
    }



    private String getGenerationDisplayName(String gen) {
        return switch (gen) {
            case "gen1" -> "[Gen 1] Kanto";
            case "gen2" -> "[Gen 2] Johto";
            case "gen3" -> "[Gen 3] Hoenn";
            case "gen4" -> "[Gen 4] Sinnoh";
            case "gen5" -> "[Gen 5] Unova";
            case "gen6" -> "[Gen 6] Kalos";
            case "gen7" -> "[Gen 7] Alola";
            case "gen8" -> "[Gen 8] Galar";
            case "gen9" -> "[Gen 9] Paldea";
            default -> "Unknown";
        };
    }
    
    private String formatGenName(String genName) {
        if (genName == null || !genName.toLowerCase().startsWith("gen")) return genName;
        return "Gen" + genName.substring(3);
    }

    private Text capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return Text.empty();
        }
        String capitalized = str.substring(0, 1).toUpperCase() + str.substring(1);
        return Text.literal(capitalized).formatted(Formatting.GOLD);
    }
    
    private Text createClientStatusLine(String label, boolean isEnabled) {
        Text message = Text.literal(label + " = ").formatted(Formatting.WHITE);
        if (isEnabled) {
            message = message.copy().append(Text.literal("ON").formatted(Formatting.GREEN));
        } else {
            message = message.copy().append(Text.literal("OFF").formatted(Formatting.RED));
        }
        return message;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = 420;
        int panelHeight = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        
        // Draw main panel background
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE01A1A1A);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF888888);
        
        // Draw separator line below title
        context.fill(panelX, panelY + 48, panelX + panelWidth, panelY + 49, 0xFF888888);

        super.render(context, mouseX, mouseY, delta);

        // Draw title centered
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 10, 0xFFFFFF);



        if (this.pokemonNameField != null && this.pokemonNameField.isVisible()) {
            this.pokemonNameField.renderSuggestions(context, mouseX, mouseY);
        }
        if (this.playerNameField != null && this.playerNameField.isVisible()) {
            this.playerNameField.renderSuggestions(context, mouseX, mouseY);
        }

        // Draw response panel below main panel
        if (!responseLines.isEmpty()) {
            int responsePanelY = panelY + panelHeight + 5;
            int textWidth = panelWidth - 10;
            int totalTextHeight = 0;
            for (Text line : responseLines) {
                totalTextHeight += this.textRenderer.getWrappedLinesHeight(line, textWidth);
            }
            int responsePanelHeight = Math.max(20, 5 + totalTextHeight);

            context.fill(panelX, responsePanelY, panelX + panelWidth, responsePanelY + responsePanelHeight, 0xE0000000);
            context.drawBorder(panelX, responsePanelY, panelWidth, responsePanelHeight, 0xFF888888);
            
            int currentTextY = responsePanelY + 4;
            for (Text line : responseLines) {
                context.drawTextWrapped(this.textRenderer, line, panelX + 5, currentTextY, textWidth, 0xFFFFFF);
                currentTextY += this.textRenderer.getWrappedLinesHeight(line, textWidth);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (responseTimer > 0) {
            responseTimer--;
            if (responseTimer == 0) {
                responseLines.clear();
            }
        }

        if (this.currentMainCategory == MainCategory.USER_TOOLS && this.currentUserSubCategory == UserSubCategory.CUSTOM_HUNT && this.children() != null) {
            for (var child : this.children()) {
                if (child instanceof ButtonWidget button && button.getMessage().getString().contains("➖ Remove")) {
                    button.active = this.pokemonNameField != null && !this.pokemonNameField.getText().isEmpty();
                }
            }
        }
        // FIX: Real-time update for admin player data buttons
        if (this.currentMainCategory == MainCategory.ADMIN_TOOLS && this.currentAdminSubCategory == AdminSubCategory.PLAYER_DATA && this.children() != null) {
            boolean hasPlayerName = this.playerNameField != null && !this.playerNameField.getText().isEmpty();
            for (var child : this.children()) {
                if (child instanceof ButtonWidget button) {
                    String msg = button.getMessage().getString();
                    if (msg.contains("Autocomplete") || msg.contains("Rollback")) {
                        button.active = hasPlayerName;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !responseLines.isEmpty()) {
            int panelWidth = 600;
            int panelHeight = 320;
            int panelX = (this.width - panelWidth) / 2;
            int panelY = (this.height - panelHeight) / 2;
            int responsePanelY = panelY + panelHeight + 5;
            int currentTextY = responsePanelY + 4;

            for (Text line : responseLines) {
                if (mouseY >= currentTextY && mouseY < currentTextY + this.textRenderer.fontHeight) {
                    // Handle click events in the response text
                    try {
                        if (line.getStyle().getClickEvent() != null) {
                            var clickEvent = line.getStyle().getClickEvent();
                            if (clickEvent.getAction() == net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND && 
                                clickEvent.getValue().startsWith("remove:")) {
                                String pokemonName = clickEvent.getValue().substring(7); // Remove "remove:" prefix
                                com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                                    new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                                        com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.REMOVE, pokemonName);
                                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                                displayResponse(List.of(Text.literal("Removing ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" from your custom list...").formatted(Formatting.YELLOW)));
                                return true;
                            } else {
                                this.handleTextClick(line.getStyle());
                                return true;
                            }
                        }
                        
                        // Check for click events in sibling components
                        if (line instanceof net.minecraft.text.MutableText mutableText) {
                            for (var sibling : mutableText.getSiblings()) {
                                if (sibling.getStyle().getClickEvent() != null) {
                                    var clickEvent = sibling.getStyle().getClickEvent();
                                    if (clickEvent.getAction() == net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND && 
                                        clickEvent.getValue().startsWith("remove:")) {
                                        String pokemonName = clickEvent.getValue().substring(7);
                                        com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                                            new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                                                com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.REMOVE, pokemonName);
                                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                                        displayResponse(List.of(Text.literal("Removing ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" from your custom list...").formatted(Formatting.YELLOW)));
                                        return true;
                                    } else {
                                        this.handleTextClick(sibling.getStyle());
                                        return true;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore click handling errors
                    }
                }
                currentTextY += this.textRenderer.getWrappedLinesHeight(line, panelWidth - 10);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void displayResponse(List<Text> lines) {
        this.responseLines = new ArrayList<>(lines);
        this.responseTimer = 200;
    }

    public void setPokemonNameField(String text) {
        if (this.pokemonNameField != null) {
            this.pokemonNameField.setText(text);
        }
    }

    @Override
    protected void clearAndInit() {
        this.clearChildren();
        this.init();
    }

    private void executeCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            // Remove leading slash if present since sendChatCommand adds it automatically
            String cleanCommand = command.startsWith("/") ? command.substring(1) : command;
            client.player.networkHandler.sendChatCommand(cleanCommand);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.pokemonNameField != null && this.pokemonNameField.isFocused() && this.pokemonNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.playerNameField != null && this.playerNameField.isFocused() && this.playerNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ConfigManager.saveClientConfigToFile();
        this.client.setScreen(this.parent);
    }
}