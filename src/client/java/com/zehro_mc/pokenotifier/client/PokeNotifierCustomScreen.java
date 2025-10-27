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
    
    // System Status side panel variables
    private List<Text> systemStatusLines = new ArrayList<>();
    private int systemStatusTimer = 0;
    private int systemStatusScrollOffset = 0;

    private enum MainCategory { USER_TOOLS, EVENTS, ADMIN_TOOLS }
    private MainCategory currentMainCategory = MainCategory.USER_TOOLS;

    private enum UserSubCategory { NOTIFICATIONS, CUSTOM_HUNT, CATCH_EM_ALL, MAP_SETTINGS, INFO }
    private UserSubCategory currentUserSubCategory = UserSubCategory.NOTIFICATIONS;

    private enum EventSubCategory { GLOBAL_HUNT, BOUNTY_SYSTEM, SWARM_EVENTS, RIVAL_BATTLES }
    private EventSubCategory currentEventSubCategory = EventSubCategory.GLOBAL_HUNT;

    private enum AdminSubCategory { SYSTEM_STATUS, SERVER_CONTROL, PLAYER_DATA, TESTING }
    private AdminSubCategory currentAdminSubCategory = AdminSubCategory.SYSTEM_STATUS;

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

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // No hacer nada - evita el blur automático
    }
    
    private void drawTabs(DrawContext context, int panelX, int panelY, int panelWidth) {
        int tabWidth = PokeNotifierClient.isPlayerAdmin ? 85 : 120;
        int tabHeight = 20;
        int tabY = panelY + 5;
        
        // Dibujar barra de pestañas completa
        context.fill(panelX, tabY, panelX + panelWidth, tabY + tabHeight, 0xFF1A1A1A);
        context.fill(panelX, tabY, panelX + panelWidth, tabY + 1, 0xFF3C3C3C);
        
        // User Tools tab
        int userTabX = panelX + 5;
        boolean userActive = currentMainCategory == MainCategory.USER_TOOLS;
        drawTab(context, userTabX, tabY, tabWidth, tabHeight, userActive);
        
        if (PokeNotifierClient.isPlayerAdmin) {
            // Events tab
            int eventsTabX = panelX + 8 + tabWidth;
            boolean eventsActive = currentMainCategory == MainCategory.EVENTS;
            drawTab(context, eventsTabX, tabY, tabWidth, tabHeight, eventsActive);
            
            // Admin tab
            int adminTabX = panelX + 11 + tabWidth * 2;
            boolean adminActive = currentMainCategory == MainCategory.ADMIN_TOOLS;
            drawTab(context, adminTabX, tabY, tabWidth, tabHeight, adminActive);
        }
    }
    
    private void drawTab(DrawContext context, int x, int y, int width, int height, boolean active) {
        String tabText;
        if (x == (this.width - 420) / 2 + 5) {
            tabText = "User Tools";
        } else if (PokeNotifierClient.isPlayerAdmin && x == (this.width - 420) / 2 + 8 + (PokeNotifierClient.isPlayerAdmin ? 85 : 120)) {
            tabText = "Events";
        } else {
            tabText = "Admin";
        }
        
        if (active) {
            // Pestaña activa - elevada y conectada
            context.fill(x, y - 2, x + width, y + height, 0xFF2D2D30);
            context.fill(x, y - 2, x + width, y - 1, 0xFF4A4A4A);
            context.fill(x, y - 2, x + 1, y + height, 0xFF4A4A4A);
            context.fill(x + width - 1, y - 2, x + width, y + height, 0xFF0F0F0F);
            
            // Texto de pestaña activa
            int textX = x + (width - this.textRenderer.getWidth(tabText)) / 2;
            int textY = y + (height - this.textRenderer.fontHeight) / 2 - 1;
            context.drawText(this.textRenderer, tabText, textX, textY, 0xFFFFFF, false);
        } else {
            // Pestaña inactiva - hundida
            context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF1F1F1F);
            context.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF2A2A2A);
            context.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF2A2A2A);
            context.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF0A0A0A);
            
            // Texto de pestaña inactiva
            int textX = x + (width - this.textRenderer.getWidth(tabText)) / 2;
            int textY = y + (height - this.textRenderer.fontHeight) / 2 + 1;
            context.drawText(this.textRenderer, tabText, textX, textY, 0xAAAAAA, false);
        }
    }



    private void buildLayout() {
        // Compact centered panel design
        int panelWidth = 420;
        int panelHeight = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        // Las pestañas se dibujan visualmente en render() y se manejan clics en mouseClicked()

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
                .dimensions(panelX + (panelWidth - 70) / 2, panelY + panelHeight - 26, 70, 16)
                .build());
    }

    // --- USER TOOLS LAYOUT ---
    private void buildUserToolsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        // Panel izquierdo - solo navegación
        int navX = panelX + 10;
        int navY = panelY + 35;
        int navWidth = 110;

        addDrawableChild(createIconButton(navX, navY, navWidth, GuiIcons.NOTIFICATIONS, "Notifications", UserSubCategory.NOTIFICATIONS, currentUserSubCategory));
        addDrawableChild(createIconButton(navX, navY + 22, navWidth, GuiIcons.CUSTOM_HUNT, "Custom Hunt", UserSubCategory.CUSTOM_HUNT, currentUserSubCategory));
        addDrawableChild(createIconButton(navX, navY + 44, navWidth, GuiIcons.CATCH_EM_ALL, "Catch 'em All", UserSubCategory.CATCH_EM_ALL, currentUserSubCategory));
        addDrawableChild(createIconButton(navX, navY + 66, navWidth, GuiIcons.MAP_SETTINGS, "Map Settings", UserSubCategory.MAP_SETTINGS, currentUserSubCategory));
        addDrawableChild(createIconButton(navX, navY + 88, navWidth, GuiIcons.INFO_HELP, "Info & Help", UserSubCategory.INFO, currentUserSubCategory));

        // Panel derecho - contenido
        int contentX = panelX + navWidth + 25;
        int contentY = panelY + 35;
        int contentWidth = panelWidth - navWidth - 40;

        switch (currentUserSubCategory) {
            case NOTIFICATIONS -> buildNotificationsPanel(contentX, contentY, contentWidth);
            case CUSTOM_HUNT -> buildCustomHuntPanel(contentX, contentY, contentWidth);
            case CATCH_EM_ALL -> buildCatchEmAllPanel(contentX, contentY, contentWidth);
            case MAP_SETTINGS -> buildMapSettingsPanel(contentX, contentY, contentWidth);
            case INFO -> buildInfoPanel(contentX, contentY, contentWidth);
        }
    }

    // --- EVENTS LAYOUT ---
    private void buildEventsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        // Panel izquierdo - navegación de eventos
        int navX = panelX + 10;
        int navY = panelY + 35;
        int navWidth = 110;
        
        addDrawableChild(createEventIconButton(navX, navY, navWidth, GuiIcons.GLOBAL_HUNT, "Global Hunt", EventSubCategory.GLOBAL_HUNT, PokeNotifierClient.isGlobalHuntSystemEnabled));
        addDrawableChild(createEventIconButton(navX, navY + 22, navWidth, GuiIcons.BOUNTY_SYSTEM, "Bounty System", EventSubCategory.BOUNTY_SYSTEM, PokeNotifierClient.isServerBountySystemEnabled));
        addDrawableChild(createEventIconButton(navX, navY + 44, navWidth, GuiIcons.SWARM_EVENTS, "Swarm Events", EventSubCategory.SWARM_EVENTS, PokeNotifierClient.isSwarmSystemEnabled));
        addDrawableChild(createEventIconButton(navX, navY + 66, navWidth, GuiIcons.RIVAL_BATTLES, "Rival Battles", EventSubCategory.RIVAL_BATTLES, true));
        
        // Panel derecho - detalles del evento
        int contentX = panelX + navWidth + 25;
        int contentY = panelY + 35;
        int contentWidth = panelWidth - navWidth - 40;
        
        switch (currentEventSubCategory) {
            case GLOBAL_HUNT -> buildGlobalHuntDetailsPanel(contentX, contentY, contentWidth);
            case BOUNTY_SYSTEM -> buildBountySystemDetailsPanel(contentX, contentY, contentWidth);
            case SWARM_EVENTS -> buildSwarmEventsDetailsPanel(contentX, contentY, contentWidth);
            case RIVAL_BATTLES -> buildRivalBattlesDetailsPanel(contentX, contentY, contentWidth);
        }
    }
    
    private ButtonWidget createEventIconButton(int x, int y, int width, net.minecraft.util.Identifier iconId, String text, EventSubCategory category, boolean enabled) {
        IconButton button = new IconButton(x, y, width, 18, iconId, text, b -> {
            this.currentEventSubCategory = category;
            this.clearAndInit();
        });
        
        button.active = !category.equals(currentEventSubCategory);
        return button;
    }

    // --- ADMIN TOOLS LAYOUT ---
    private void buildAdminToolsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        // Panel izquierdo - navegación admin
        int navX = panelX + 10;
        int navY = panelY + 35;
        int navWidth = 110;

        addDrawableChild(createIconButton(navX, navY, navWidth, GuiIcons.SYSTEM_STATUS, "System Status", AdminSubCategory.SYSTEM_STATUS, currentAdminSubCategory));
        addDrawableChild(createIconButton(navX, navY + 22, navWidth, GuiIcons.SERVER_CONTROL, "Server Control", AdminSubCategory.SERVER_CONTROL, currentAdminSubCategory));
        addDrawableChild(createIconButton(navX, navY + 44, navWidth, GuiIcons.PLAYER_DATA, "Player Data", AdminSubCategory.PLAYER_DATA, currentAdminSubCategory));
        addDrawableChild(createIconButton(navX, navY + 66, navWidth, GuiIcons.TESTING, "Testing", AdminSubCategory.TESTING, currentAdminSubCategory));

        // Panel derecho - contenido admin
        int contentX = panelX + navWidth + 25;
        int contentY = panelY + 35;
        int contentWidth = panelWidth - navWidth - 40;

        switch (currentAdminSubCategory) {
            case SYSTEM_STATUS -> buildSystemStatusPanel(contentX, contentY, contentWidth);
            case SERVER_CONTROL -> buildServerControlPanel(contentX, contentY, contentWidth);
            case PLAYER_DATA -> buildPlayerDataPanel(contentX, contentY, contentWidth);
            case TESTING -> buildTestingPanel(contentX, contentY, contentWidth);
        }
    }

    private <T extends Enum<T>> ButtonWidget createSubNavButton(int x, int y, int width, Text text, T category, T current) {
        String tooltip = getTooltipForCategory(category);
        ButtonWidget button = ButtonWidget.builder(text, b -> {
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
    
    private <T extends Enum<T>> ButtonWidget createIconButton(int x, int y, int width, net.minecraft.util.Identifier iconId, String labelText, T category, T current) {
        String tooltip = getTooltipForCategory(category);
        IconButton button = new IconButton(x, y, width, 18, iconId, labelText, b -> {
            if (category instanceof UserSubCategory) this.currentUserSubCategory = (UserSubCategory) category;
            if (category instanceof EventSubCategory) this.currentEventSubCategory = (EventSubCategory) category;
            if (category instanceof AdminSubCategory) this.currentAdminSubCategory = (AdminSubCategory) category;
            this.clearAndInit();
        });
        button.active = !category.equals(current);
        return button;
    }
    

    
    private <T extends Enum<T>> String getTooltipForCategory(T category) {
        if (category instanceof UserSubCategory) {
            return switch ((UserSubCategory) category) {
                case NOTIFICATIONS -> "Configure chat, sound, and HUD alerts";
                case CUSTOM_HUNT -> "Manage your personal Pokémon hunt list";
                case CATCH_EM_ALL -> "Track Pokédex completion by generation";
                case MAP_SETTINGS -> "Configure waypoint and map integration settings";
                case INFO -> "View help, version, and mod information";
            };
        } else if (category instanceof AdminSubCategory) {
            return switch ((AdminSubCategory) category) {
                case SYSTEM_STATUS -> "View status of all mod services and configurations";
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
        net.minecraft.util.Identifier silentModeIcon = clientConfig.silent_mode_enabled ? GuiIcons.ON : GuiIcons.OFF;
        addDrawableChild(new IconButton(x, y + 85, width, 18, silentModeIcon, "Silent Mode", button -> {
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
        }));

        addDrawableChild(createToggleButton("Searching", clientConfig.searching_enabled, newValue -> clientConfig.searching_enabled = newValue, x, y + 105, width));
    }

    private void buildCustomHuntPanel(int x, int y, int width) {
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("e.g., Pikachu"));
        addDrawableChild(this.pokemonNameField);

        // Add button
        addDrawableChild(new IconButton(x, y + 25, width, 18, GuiIcons.ADD, "Add", b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                    new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                        com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.ADD, pokemonName);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Request sent to add ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" to your custom list.").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            }
        }));

        // Clear List button
        addDrawableChild(new IconButton(x, y + 48, width, 18, GuiIcons.CLEAR, "Clear List", b -> {
            com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                    com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.CLEAR, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Request sent to clear your custom list.").formatted(Formatting.YELLOW)));
        }));
        
        // View List button
        addDrawableChild(new IconButton(x, y + 71, width, 18, GuiIcons.VIEW_LIST, "View List", b -> {
            com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload payload = 
                new com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload(
                    com.zehro_mc.pokenotifier.networking.CustomListUpdatePayload.Action.LIST, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting your custom list from the server...").formatted(Formatting.YELLOW)));
        }));
    }

    private void buildCatchEmAllPanel(int x, int y, int width) {
        List<String> generations = Arrays.asList("gen1", "gen2", "gen3", "gen4", "gen5", "gen6", "gen7", "gen8", "gen9");
        int buttonWidth = (width - 5) / 2;
        int buttonHeight = 18;

        for (int i = 0; i < generations.size(); i++) {
            String gen = generations.get(i);
            int row = i / 2;
            int col = i % 2;
            int buttonX = x + col * (buttonWidth + 5);
            int buttonY = y + row * (buttonHeight + 5);

            net.minecraft.util.Identifier genIcon = gen.equals(PokeNotifierClient.currentCatchEmAllGeneration) ? GuiIcons.TRACK_GEN : null;
            
            IconButton button = new IconButton(buttonX, buttonY, buttonWidth, buttonHeight, genIcon, getGenerationDisplayName(gen), b -> {
                // Check if Auto-Waypoint is enabled and warn user
                if (clientConfig.auto_waypoint_enabled) {
                    displayResponse(List.of(
                        Text.literal("Auto-Waypoint is active! Disabling it to prevent massive waypoints.").formatted(Formatting.YELLOW),
                        Text.literal("Catch 'em All would create waypoints for every Pokemon.").formatted(Formatting.GRAY)
                    ));
                    clientConfig.auto_waypoint_enabled = false;
                    ConfigManager.saveClientConfigToFile();
                }
                
                // Use networking payload instead of command
                com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload payload = 
                    new com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload(
                        com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload.Action.ENABLE, gen);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Requesting to track " + formatGenName(gen) + "...").formatted(Formatting.YELLOW)));
                
                // Update the current generation immediately for visual feedback
                PokeNotifierClient.currentCatchEmAllGeneration = gen;
                this.clearAndInit(); // Refresh the GUI to update button colors
            });

            addDrawableChild(button);
        }

        int statusY = y + 5 * (buttonHeight + 5);
        
        // Stop Tracking button
        IconButton stopTrackingButton = new IconButton(x, statusY, width, 18, GuiIcons.OFF, "Stop Tracking", b -> {
            if (PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
                com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload payload = 
                    new com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload(
                        com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload.Action.DISABLE, 
                        PokeNotifierClient.currentCatchEmAllGeneration);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Requesting to disable tracking...").formatted(Formatting.YELLOW)));
            }
        });
        // Only enable if there's an active generation being tracked
        stopTrackingButton.active = PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration);
        addDrawableChild(stopTrackingButton);
        
        // View Status button
        addDrawableChild(new IconButton(x, statusY + 23, width, 18, GuiIcons.SYSTEM_STATUS, "View Status", b -> {
            com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload payload = 
                new com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload(
                    com.zehro_mc.pokenotifier.networking.CatchemallUpdatePayload.Action.LIST, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting your active Catch 'em All modes...").formatted(Formatting.YELLOW)));
        }));
    }

    private void buildMapSettingsPanel(int x, int y, int width) {
        int currentY = y;
        
        // Waypoint Creation Toggle
        addDrawableChild(createToggleButton("Create Waypoints", clientConfig.create_waypoints_enabled, newValue -> {
            clientConfig.create_waypoints_enabled = newValue;
            ConfigManager.saveClientConfigToFile();
            this.clearAndInit();
        }, x, currentY, width));
        currentY += 20;
        
        // Event Waypoint Toggle
        addDrawableChild(createToggleButton("Event Waypoints", clientConfig.event_waypoints_enabled, newValue -> {
            clientConfig.event_waypoints_enabled = newValue;
            ConfigManager.saveClientConfigToFile();
            this.clearAndInit();
        }, x, currentY, width));
        currentY += 20;
        
        // Auto-Remove Waypoints Toggle
        addDrawableChild(createToggleButton("Auto-Remove Waypoints", clientConfig.auto_remove_waypoints, newValue -> {
            clientConfig.auto_remove_waypoints = newValue;
            ConfigManager.saveClientConfigToFile();
            this.clearAndInit();
        }, x, currentY, width));
        currentY += 20;
        
        // Auto-Waypoint Toggle with conflict detection
        boolean isCatchEmAllActive = PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration);
        
        // Auto-disable if both are active (server-side protection)
        if (isCatchEmAllActive && clientConfig.auto_waypoint_enabled) {
            clientConfig.auto_waypoint_enabled = false;
            ConfigManager.saveClientConfigToFile();
        }
        
        net.minecraft.util.Identifier autoWaypointIcon = clientConfig.auto_waypoint_enabled ? GuiIcons.ON : GuiIcons.OFF;
        IconButton autoWaypointButton = new IconButton(x, currentY, width, 18, autoWaypointIcon, "Auto-Waypoint", button -> {
            boolean currentCatchEmAllActive = PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration);
            boolean newValue = !clientConfig.auto_waypoint_enabled;
            
            if (newValue && currentCatchEmAllActive) {
                displayResponse(List.of(
                    Text.literal("Cannot enable Auto-Waypoint while Catch 'em All is active!").formatted(Formatting.RED),
                    Text.literal("This would create massive waypoints. Disable Catch 'em All first.").formatted(Formatting.YELLOW)
                ));
                return; // Don't change the state
            }
            
            clientConfig.auto_waypoint_enabled = newValue;
            ConfigManager.saveClientConfigToFile();
            this.clearAndInit(); // Refresh to update icon
            displayResponse(List.of(Text.literal("Settings updated.").formatted(Formatting.GREEN)));
        });
        addDrawableChild(autoWaypointButton);
        currentY += 25;
        
        // Xaero's Integration Status
        boolean xaeroAvailable = com.zehro_mc.pokenotifier.client.compat.XaeroIntegration.isXaeroAvailable();
        Text xaeroStatusText = Text.literal("Xaero's Integration: ")
            .append(xaeroAvailable ? Text.literal("AVAILABLE").formatted(Formatting.GREEN) : Text.literal("NOT FOUND").formatted(Formatting.RED));
        addDrawableChild(ButtonWidget.builder(xaeroStatusText, b -> {
            List<Text> statusLines = new ArrayList<>();
            statusLines.add(Text.literal("--- Xaero's Integration Status ---").formatted(Formatting.GOLD));
            if (xaeroAvailable) {
                statusLines.add(Text.literal("✓ Xaero's mods detected").formatted(Formatting.GREEN));
                statusLines.add(Text.literal("✓ Waypoint buttons will appear in chat").formatted(Formatting.GREEN));
                statusLines.add(Text.literal("✓ Click [Add] to create waypoints").formatted(Formatting.AQUA));
                if (clientConfig.auto_remove_waypoints) {
                    statusLines.add(Text.literal("✓ Waypoints auto-removed when Pokemon caught").formatted(Formatting.YELLOW));
                } else {
                    statusLines.add(Text.literal("⚠ Auto-removal disabled").formatted(Formatting.GRAY));
                }
            } else {
                statusLines.add(Text.literal("✗ Xaero's mods not found").formatted(Formatting.RED));
                statusLines.add(Text.literal("→ Coordinates shown instead").formatted(Formatting.GRAY));
                statusLines.add(Text.literal("→ Install Xaero's Minimap/Worldmap for waypoints").formatted(Formatting.GRAY));
            }
            displayResponse(statusLines);
        }).dimensions(x, currentY, width, 18).build());
        currentY += 22;
        
        // Xaero-specific buttons
        if (xaeroAvailable) {
            // Waypoint Statistics
            addDrawableChild(ButtonWidget.builder(Text.literal("Waypoint Statistics"), b -> {
                int trackedCount = com.zehro_mc.pokenotifier.client.compat.WaypointTracker.getTrackedWaypointCount();
                boolean autoRemovalEnabled = com.zehro_mc.pokenotifier.client.compat.WaypointTracker.isAutoRemovalEnabled();
                
                List<Text> statsLines = new ArrayList<>();
                statsLines.add(Text.literal("--- Waypoint Statistics ---").formatted(Formatting.GOLD));
                statsLines.add(Text.literal("Currently Tracked: " + trackedCount).formatted(Formatting.AQUA));
                statsLines.add(Text.literal("Auto-Removal: " + (autoRemovalEnabled ? "Enabled" : "Disabled")).formatted(autoRemovalEnabled ? Formatting.GREEN : Formatting.RED));
                statsLines.add(Text.literal("Waypoint Creation: " + (clientConfig.create_waypoints_enabled ? "Enabled" : "Disabled")).formatted(clientConfig.create_waypoints_enabled ? Formatting.GREEN : Formatting.RED));
                displayResponse(statsLines);
            }).dimensions(x, currentY, width, 18).build());
            currentY += 22;
            
            // Clear All Waypoints Button
            addDrawableChild(ButtonWidget.builder(Text.literal("Clear All Tracked Waypoints"), b -> {
                int trackedCount = com.zehro_mc.pokenotifier.client.compat.WaypointTracker.getTrackedWaypointCount();
                if (trackedCount > 0) {
                    this.client.setScreen(new ConfirmScreen(confirmed -> {
                        if (confirmed) {
                            com.zehro_mc.pokenotifier.client.compat.WaypointTracker.clearAllTrackedWaypoints();
                            displayResponse(List.of(Text.literal("Cleared all tracked waypoints.").formatted(Formatting.GREEN)));
                        }
                        this.client.setScreen(this);
                    }, Text.literal("Clear Waypoints"), Text.literal("Remove all " + trackedCount + " tracked waypoints?")));
                } else {
                    displayResponse(List.of(Text.literal("No waypoints are currently being tracked.").formatted(Formatting.GRAY)));
                }
            }).dimensions(x, currentY, width, 18).build());
        } else {
            // About Waypoint Features when Xaero's not available
            addDrawableChild(ButtonWidget.builder(Text.literal("About Waypoint Features"), b -> {
                List<Text> infoLines = new ArrayList<>();
                infoLines.add(Text.literal("--- Waypoint Features ---").formatted(Formatting.GOLD));
                infoLines.add(Text.literal("Install Xaero's Minimap or Worldmap to enable:").formatted(Formatting.YELLOW));
                infoLines.add(Text.literal("• Clickable waypoint buttons in chat").formatted(Formatting.AQUA));
                infoLines.add(Text.literal("• Automatic waypoint creation for Pokemon").formatted(Formatting.AQUA));
                infoLines.add(Text.literal("• Auto-removal when Pokemon are caught").formatted(Formatting.AQUA));
                infoLines.add(Text.literal("• Waypoint tracking and management").formatted(Formatting.AQUA));
                infoLines.add(Text.literal("").formatted(Formatting.WHITE));
                infoLines.add(Text.literal("Without Xaero's: Coordinates are shown instead").formatted(Formatting.GRAY));
                displayResponse(infoLines);
            }).dimensions(x, currentY, width, 18).build());
        }
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

    private void buildSystemStatusPanel(int x, int y, int width) {
        // Refresh button at the top
        addDrawableChild(new IconButton(x, y, width, 18, GuiIcons.REFRESH, "Refresh Status", b -> {
            systemStatusLines = getSystemStatusLines();
            systemStatusTimer = 600; // Show for 30 seconds (600 ticks)
            systemStatusScrollOffset = 0; // Reset scroll
        }));
        
        // System Status Text Box - starts below the button
        // The text box will be rendered in the render method
    }

    private void buildServerControlPanel(int x, int y, int width) {
        // Debug Mode toggle
        net.minecraft.util.Identifier debugModeIcon = PokeNotifierClient.isServerDebugMode ? GuiIcons.ON : GuiIcons.OFF;
        addDrawableChild(new IconButton(x, y, width, 18, debugModeIcon, "Debug Mode", b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_DEBUG_MODE, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling debug mode...").formatted(Formatting.YELLOW)));
            
            // Immediate visual feedback
            PokeNotifierClient.isServerDebugMode = !PokeNotifierClient.isServerDebugMode;
            this.clearAndInit(); // Refresh to update icon
        }));
        
        // Test Mode toggle
        net.minecraft.util.Identifier testModeIcon = PokeNotifierClient.isServerTestMode ? GuiIcons.ON : GuiIcons.OFF;
        addDrawableChild(new IconButton(x, y + 23, width, 18, testModeIcon, "Test Mode", b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_TEST_MODE, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling test mode...").formatted(Formatting.YELLOW)));
            
            // Immediate visual feedback
            PokeNotifierClient.isServerTestMode = !PokeNotifierClient.isServerTestMode;
            this.clearAndInit(); // Refresh to update icon
        }));

        // Server Status button
        addDrawableChild(ButtonWidget.builder(Text.literal("Server Status"), b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.SERVER_STATUS, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting server status...").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 55, width, 18).build());
        
        // Reload Configs button
        addDrawableChild(ButtonWidget.builder(Text.literal("Reload Configs"), b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.RELOAD_CONFIG, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Reloading configurations...").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 78, width, 18).build());

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
        }).dimensions(x, y + 101, width, 18).build());
        
        // Note: Event management has been moved to the Events tab
        addDrawableChild(ButtonWidget.builder(Text.literal("Event Management"), b -> {
            displayResponse(List.of(Text.literal("Event management has been moved to the Events tab.").formatted(Formatting.YELLOW)));
        }).dimensions(x, y + 124, width, 18).build()).active = false;
    }

    // --- EVENT PANEL BUILDERS ---
    
    private void buildBountySystemDetailsPanel(int x, int y, int width) {
        // System Toggle
        net.minecraft.util.Identifier bountyToggleIcon = PokeNotifierClient.isServerBountySystemEnabled ? GuiIcons.ON : GuiIcons.OFF;
        String bountyToggleText = PokeNotifierClient.isServerBountySystemEnabled ? "Disable System" : "Enable System";
        addDrawableChild(new IconButton(x, y, width, 18, bountyToggleIcon, bountyToggleText, b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_BOUNTY_SYSTEM, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling bounty system...").formatted(Formatting.YELLOW)));
            
            PokeNotifierClient.isServerBountySystemEnabled = !PokeNotifierClient.isServerBountySystemEnabled;
            this.clearAndInit(); // Refresh to update icon
        }));
    }
    
    private void buildSwarmEventsDetailsPanel(int x, int y, int width) {
        int currentY = y;
        
        // Automatic Mode Toggle - independent of manual swarms
        net.minecraft.util.Identifier systemToggleIcon = PokeNotifierClient.isSwarmSystemEnabled ? GuiIcons.ON : GuiIcons.OFF;
        String systemToggleText = PokeNotifierClient.isSwarmSystemEnabled ? "Disable Automatic" : "Enable Automatic";
        
        IconButton systemToggleButton = new IconButton(x, currentY, width / 2 - 2, 18, systemToggleIcon, systemToggleText, b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_SWARM_SYSTEM, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Toggling automatic swarm mode...").formatted(Formatting.YELLOW)));
            
            PokeNotifierClient.isSwarmSystemEnabled = !PokeNotifierClient.isSwarmSystemEnabled;
            this.clearAndInit(); // Refresh entire GUI to update all states
        });
        
        addDrawableChild(systemToggleButton);
        
        // Swarm Status
        addDrawableChild(new IconButton(x + width / 2 + 2, currentY, width / 2 - 2, 18, GuiIcons.SYSTEM_STATUS, "Status", b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.SWARM_STATUS, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting Swarm status...").formatted(Formatting.YELLOW)));
        }));
        
        currentY += 23;
        
        // Manual Swarm Controls
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, currentY, width - 40, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("Pokémon for Swarm"));
        addDrawableChild(this.pokemonNameField);
        
        currentY += 23;
        
        // Note: "Here" option temporarily disabled due to positioning issues
        currentY += 3;
        
        // Start Swarm - ALWAYS enabled regardless of automatic mode
        IconButton startSwarmButton = new IconButton(x, currentY, width / 2 - 2, 18, GuiIcons.ON, "Start Swarm", b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                String parameter = pokemonName; // Removed "here" option temporarily
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.START_SWARM, parameter);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Starting manual swarm of ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append(" (shiny guaranteed)...").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            } else {
                displayResponse(List.of(Text.literal("Please enter a Pokémon name first.").formatted(Formatting.RED)));
            }
        });
        
        // Only disable if there's already an active swarm
        startSwarmButton.active = !PokeNotifierClient.hasActiveSwarm;
        addDrawableChild(startSwarmButton);
        
        IconButton cancelSwarmButton = new IconButton(x + width / 2 + 2, currentY, width / 2 - 2, 18, GuiIcons.OFF, "Cancel Swarm", b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.CANCEL_SWARM, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Cancelling active Swarm...").formatted(Formatting.YELLOW)));
            
            // Update client state immediately
            PokeNotifierClient.hasActiveSwarm = false;
            PokeNotifierClient.activeSwarmPokemon = "";
            this.clearAndInit(); // Refresh GUI to show updated state
        });
        
        // Only enable if there's an active swarm
        cancelSwarmButton.active = PokeNotifierClient.hasActiveSwarm;
        addDrawableChild(cancelSwarmButton);
        
        currentY += 26;
        
        // Active Swarm Status
        if (PokeNotifierClient.hasActiveSwarm && !PokeNotifierClient.activeSwarmPokemon.isEmpty()) {
            Text activeSwarmTitle = Text.literal("🌪️ Active Swarm").formatted(Formatting.GREEN);
            addDrawableChild(ButtonWidget.builder(activeSwarmTitle, b -> {}).dimensions(x, currentY, width, 18).build()).active = false;
            
            currentY += 21;
            
            Text activePokemon = Text.literal("Target: " + PokeNotifierClient.activeSwarmPokemon).formatted(Formatting.GOLD);
            addDrawableChild(ButtonWidget.builder(activePokemon, b -> {
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.SWARM_STATUS, "");
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Requesting detailed swarm status...").formatted(Formatting.YELLOW)));
            }).dimensions(x, currentY, width, 18).build());
        } else {
            Text noActiveSwarm = Text.literal("No Active Swarm").formatted(Formatting.GRAY);
            addDrawableChild(ButtonWidget.builder(noActiveSwarm, b -> {}).dimensions(x, currentY, width, 18).build()).active = false;
        }
    }
    
    private void buildRivalBattlesDetailsPanel(int x, int y, int width) {
        // Rival system is automatically enabled, only admin can disable it
        addDrawableChild(new IconButton(x, y, width, 18, GuiIcons.ON, "Rival System", b -> {
            // This system is always on, show info instead
            displayResponse(List.of(
                Text.literal("--- Rival System Information ---").formatted(Formatting.GOLD),
                Text.literal("The Rival system is automatically active.").formatted(Formatting.WHITE),
                Text.literal("When players hunt the same Pokémon and one captures it,").formatted(Formatting.AQUA),
                Text.literal("others receive a friendly rivalry notification.").formatted(Formatting.AQUA),
                Text.literal("Cooldown: 60 seconds between notifications").formatted(Formatting.GRAY),
                Text.literal("Override distance: 200 blocks").formatted(Formatting.GRAY)
            ));
        }));
        
        // Show current settings
        addDrawableChild(new IconButton(x, y + 23, width, 18, GuiIcons.SYSTEM_STATUS, "View Settings", b -> {
            displayResponse(List.of(
                Text.literal("--- Rival System Settings ---").formatted(Formatting.GOLD),
                Text.literal("Notification Cooldown: 60 seconds").formatted(Formatting.AQUA),
                Text.literal("Override Distance: 200 blocks").formatted(Formatting.AQUA),
                Text.literal("Status: Always Active").formatted(Formatting.GREEN),
                Text.literal("").formatted(Formatting.WHITE),
                Text.literal("To modify settings, edit config-server.json").formatted(Formatting.GRAY),
                Text.literal("Then use 'Reload Configs' in Admin Tools").formatted(Formatting.GRAY)
            ));
        }));
    }
    
    private void buildGlobalHuntDetailsPanel(int x, int y, int width) {
        int currentY = y;
        
        // System Toggle - show proper status
        boolean hasActiveEvent = PokeNotifierClient.hasActiveGlobalHunt;
        net.minecraft.util.Identifier systemToggleIcon;
        String systemToggleText;
        
        if (hasActiveEvent) {
            systemToggleIcon = GuiIcons.ON; // Show as active when event is running
            systemToggleText = "Event Active";
        } else {
            systemToggleIcon = PokeNotifierClient.isGlobalHuntSystemEnabled ? GuiIcons.ON : GuiIcons.OFF;
            systemToggleText = PokeNotifierClient.isGlobalHuntSystemEnabled ? "Disable System" : "Enable System";
        }
        
        IconButton systemToggleButton = new IconButton(x, currentY, width / 2 - 2, 18, systemToggleIcon, systemToggleText, b -> {
            if (!hasActiveEvent) {
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.TOGGLE_GLOBAL_HUNT_SYSTEM, "");
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Toggling Global Hunt system...").formatted(Formatting.YELLOW)));
                
                PokeNotifierClient.isGlobalHuntSystemEnabled = !PokeNotifierClient.isGlobalHuntSystemEnabled;
                this.clearAndInit(); // Refresh entire GUI to update all states
            } else {
                displayResponse(List.of(Text.literal("Cannot disable system while an event is active. Cancel the event first.").formatted(Formatting.RED)));
            }
        });
        
        if (hasActiveEvent) {
            systemToggleButton.active = false;
        }
        addDrawableChild(systemToggleButton);
        
        // Event Status
        addDrawableChild(new IconButton(x + width / 2 + 2, currentY, width / 2 - 2, 18, GuiIcons.SYSTEM_STATUS, "Status", b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.GLOBAL_HUNT_STATUS, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Requesting Global Hunt status...").formatted(Formatting.YELLOW)));
        }));
        
        currentY += 25;
        
        // Manual Event Controls
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, currentY, width - 40, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("Pokémon for Event"));
        addDrawableChild(this.pokemonNameField);
        
        addDrawableChild(new IconButton(x + width - 25, currentY, 20, 18, GuiIcons.SHINY, "", b -> {
            // Toggle shiny state
        }));
        
        currentY += 25;
        
        IconButton startEventButton = new IconButton(x, currentY, width / 2 - 2, 18, GuiIcons.START_EVENT, "Start Event", b -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                String parameter = pokemonName;
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.START_GLOBAL_HUNT, parameter);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Starting Global Hunt for ").append(Text.literal(pokemonName).formatted(Formatting.GOLD)).append("...").formatted(Formatting.YELLOW)));
                this.pokemonNameField.setText("");
            } else {
                displayResponse(List.of(Text.literal("Please enter a Pokémon name first.").formatted(Formatting.RED)));
            }
        });
        
        // Disable start button if there's already an active event
        if (PokeNotifierClient.hasActiveGlobalHunt) {
            startEventButton.active = false;
        }
        addDrawableChild(startEventButton);
        
        IconButton cancelEventButton = new IconButton(x + width / 2 + 2, currentY, width / 2 - 2, 18, GuiIcons.OFF, "Cancel Event", b -> {
            com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                    com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.CANCEL_GLOBAL_HUNT, "");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
            displayResponse(List.of(Text.literal("Cancelling active Global Hunt...").formatted(Formatting.YELLOW)));
            
            // Update client state immediately
            PokeNotifierClient.hasActiveGlobalHunt = false;
            PokeNotifierClient.activeGlobalHuntPokemon = "";
            this.clearAndInit(); // Refresh GUI to show updated state
        });
        
        // Only enable cancel button if there's an active event
        if (!PokeNotifierClient.hasActiveGlobalHunt) {
            cancelEventButton.active = false;
        }
        addDrawableChild(cancelEventButton);
        
        currentY += 30;
        
        // Active Event Status
        if (PokeNotifierClient.hasActiveGlobalHunt && !PokeNotifierClient.activeGlobalHuntPokemon.isEmpty()) {
            Text activeEventTitle = Text.literal("🎯 Active Event").formatted(Formatting.GREEN);
            addDrawableChild(ButtonWidget.builder(activeEventTitle, b -> {}).dimensions(x, currentY, width, 18).build()).active = false;
            
            currentY += 21;
            
            Text activePokemon = Text.literal("Target: " + PokeNotifierClient.activeGlobalHuntPokemon).formatted(Formatting.GOLD);
            addDrawableChild(ButtonWidget.builder(activePokemon, b -> {
                com.zehro_mc.pokenotifier.networking.AdminCommandPayload payload = 
                    new com.zehro_mc.pokenotifier.networking.AdminCommandPayload(
                        com.zehro_mc.pokenotifier.networking.AdminCommandPayload.Action.GLOBAL_HUNT_STATUS, "");
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
                displayResponse(List.of(Text.literal("Requesting detailed event status...").formatted(Formatting.YELLOW)));
            }).dimensions(x, currentY, width, 18).build());
            
            currentY += 26;
        } else {
            Text noActiveEvent = Text.literal("No Active Event").formatted(Formatting.GRAY);
            addDrawableChild(ButtonWidget.builder(noActiveEvent, b -> {}).dimensions(x, currentY, width, 18).build()).active = false;
            
            currentY += 26;
        }
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
        }).dimensions(x, y + 23, width, 18).build();
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
        }).dimensions(x, y + 46, width, 18).build();
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
        }).dimensions(x, y + 48, width, 18).build());
    }



    // --- HELPER METHODS ---

    private ButtonWidget createToggleButton(String label, boolean currentValue, java.util.function.Consumer<Boolean> configUpdater, int x, int y, int width) {
        net.minecraft.util.Identifier iconId = currentValue ? GuiIcons.ON : GuiIcons.OFF;
        String statusText = currentValue ? "ON" : "OFF";
        
        IconButton button = new IconButton(x, y, width, 18, iconId, label + ": " + statusText, b -> {
            boolean newValue = !currentValue;
            configUpdater.accept(newValue);
            displayResponse(List.of(Text.literal("Settings updated.").formatted(Formatting.GREEN)));
            this.clearAndInit(); // Refresh to update icon
        });
        
        return button;
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
            message = message.copy().append(Text.literal("ENABLED").formatted(Formatting.GREEN));
        } else {
            message = message.copy().append(Text.literal("DISABLED").formatted(Formatting.RED));
        }
        return message;
    }
    
    
    private Text createStatusLine(String label, boolean isEnabled) {
        return Text.literal(label + ": ").append(isEnabled ? Text.literal("ENABLED").formatted(Formatting.GREEN) : Text.literal("DISABLED").formatted(Formatting.RED));
    }
    
    private List<Text> getSystemStatusLines() {
        List<Text> lines = new ArrayList<>();
        
        // Environment Detection
        boolean isMultiplayer = !MinecraftClient.getInstance().isInSingleplayer();
        String environment = isMultiplayer ? "Multiplayer Server" : "Singleplayer";
        String modVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("poke-notifier")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("Unknown");
        
        // Header with environment
        lines.add(Text.literal("ENVIRONMENT: " + environment).formatted(Formatting.AQUA));
        lines.add(Text.literal("MOD VERSION: v" + modVersion).formatted(Formatting.WHITE));
        lines.add(Text.literal(""));
        
        // Core Systems
        lines.add(Text.literal("CORE SYSTEMS:").formatted(Formatting.YELLOW));
        lines.add(Text.literal("├─ Debug Mode: ").append(createStatusText(PokeNotifierClient.isServerDebugMode)));
        lines.add(Text.literal("├─ Test Mode: ").append(createStatusText(PokeNotifierClient.isServerTestMode)));
        lines.add(Text.literal("└─ Silent Mode: ").append(createStatusText(clientConfig.silent_mode_enabled)));
        lines.add(Text.literal(""));
        
        // Event Systems
        lines.add(Text.literal("EVENT SYSTEMS:").formatted(Formatting.YELLOW));
        lines.add(Text.literal("├─ Swarm Events: ").append(createStatusText(PokeNotifierClient.isSwarmSystemEnabled)).append(getActiveEventInfo("swarm")));
        lines.add(Text.literal("├─ Global Hunt: ").append(createStatusText(PokeNotifierClient.isGlobalHuntSystemEnabled)).append(getActiveEventInfo("hunt")));
        lines.add(Text.literal("├─ Bounty System: ").append(createStatusText(PokeNotifierClient.isServerBountySystemEnabled)));
        lines.add(Text.literal("└─ Rival Battles: ").append(createStatusText(true)));
        lines.add(Text.literal(""));
        
        // Player Features
        boolean isCatchEmAllActive = PokeNotifierClient.currentCatchEmAllGeneration != null && !"none".equals(PokeNotifierClient.currentCatchEmAllGeneration);
        lines.add(Text.literal("PLAYER FEATURES:").formatted(Formatting.YELLOW));
        if (isCatchEmAllActive) {
            lines.add(Text.literal("├─ Catch Em All: ").append(Text.literal("ACTIVE (" + PokeNotifierClient.currentCatchEmAllGeneration + ")").formatted(Formatting.GREEN)));
        } else {
            lines.add(Text.literal("├─ Catch Em All: ").append(Text.literal("INACTIVE").formatted(Formatting.GRAY)));
        }
        lines.add(Text.literal("├─ Custom Hunt Lists: ").append(Text.literal("CONFIGURED").formatted(Formatting.AQUA)));
        lines.add(Text.literal("└─ Notifications: ").append(createStatusText(clientConfig.alert_chat_enabled || clientConfig.alert_sounds_enabled || clientConfig.alert_toast_enabled)));
        lines.add(Text.literal(""));
        
        // Integrations
        boolean xaeroAvailable = com.zehro_mc.pokenotifier.client.compat.XaeroIntegration.isXaeroAvailable();
        lines.add(Text.literal("INTEGRATIONS:").formatted(Formatting.YELLOW));
        lines.add(Text.literal("├─ Xaero Maps: ").append(createStatusText(xaeroAvailable, "DETECTED", "NOT FOUND")));
        lines.add(Text.literal("├─ Advancement Plaques: ").append(createStatusText(isAdvancementPlaquesAvailable(), "DETECTED", "NOT FOUND")));
        lines.add(Text.literal("└─ ModMenu: ").append(createStatusText(isModMenuAvailable(), "DETECTED", "NOT FOUND")));
        lines.add(Text.literal(""));
        
        // Active Events Summary
        lines.add(Text.literal("ACTIVE EVENTS:").formatted(Formatting.YELLOW));
        if (PokeNotifierClient.hasActiveSwarm && !PokeNotifierClient.activeSwarmPokemon.isEmpty()) {
            lines.add(Text.literal("├─ Swarm: ").append(Text.literal(PokeNotifierClient.activeSwarmPokemon + " (" + PokeNotifierClient.swarmRemainingMinutes + "min)").formatted(Formatting.GREEN)));
        } else {
            lines.add(Text.literal("├─ Swarm: ").append(Text.literal("None").formatted(Formatting.GRAY)));
        }
        
        if (PokeNotifierClient.hasActiveGlobalHunt && !PokeNotifierClient.activeGlobalHuntPokemon.isEmpty()) {
            lines.add(Text.literal("├─ Global Hunt: ").append(Text.literal(PokeNotifierClient.activeGlobalHuntPokemon).formatted(Formatting.GREEN)));
        } else {
            lines.add(Text.literal("├─ Global Hunt: ").append(Text.literal("None").formatted(Formatting.GRAY)));
        }
        
        lines.add(Text.literal("└─ Custom Hunts: ").append(Text.literal(PokeNotifierClient.customHuntListSize + " active").formatted(Formatting.AQUA)));
        lines.add(Text.literal(""));
        
        // Timestamp
        java.time.LocalTime now = java.time.LocalTime.now();
        String timestamp = String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
        lines.add(Text.literal("Last Updated: " + timestamp).formatted(Formatting.GRAY));
        
        return lines;
    }
    
    private Text createStatusText(boolean enabled) {
        return enabled ? Text.literal("ENABLED").formatted(Formatting.GREEN) : Text.literal("DISABLED").formatted(Formatting.RED);
    }
    
    private Text createStatusText(boolean condition, String trueText, String falseText) {
        return condition ? Text.literal(trueText).formatted(Formatting.GREEN) : Text.literal(falseText).formatted(Formatting.RED);
    }
    
    private Text getActiveEventInfo(String eventType) {
        if ("swarm".equals(eventType) && PokeNotifierClient.hasActiveSwarm) {
            return Text.literal(" (1 active)").formatted(Formatting.AQUA);
        } else if ("hunt".equals(eventType) && PokeNotifierClient.hasActiveGlobalHunt) {
            return Text.literal(" (1 active)").formatted(Formatting.AQUA);
        }
        return Text.literal("");
    }
    
    private boolean isAdvancementPlaquesAvailable() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("advancementplaques");
    }
    
    private boolean isModMenuAvailable() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu");
    }
    
    private void renderSystemStatusTextBox(DrawContext context, int x, int y, int width, int height) {
        if (systemStatusLines.isEmpty()) {
            // Show placeholder text
            context.fill(x, y, x + width, y + height, 0xFF000000);
            context.drawBorder(x, y, width, height, 0xFFFFFFFF);
            
            Text placeholder = Text.literal("Click 'Refresh Status' to view system information").formatted(Formatting.GRAY);
            int textX = x + (width - this.textRenderer.getWidth(placeholder)) / 2;
            int textY = y + (height - this.textRenderer.fontHeight) / 2;
            context.drawText(this.textRenderer, placeholder, textX, textY, 0x80FFFFFF, false);
            return;
        }
        
        // Draw text box background
        context.fill(x, y, x + width, y + height, 0xFF000000);
        context.drawBorder(x, y, width, height, 0xFFFFFFFF);
        
        // Draw timer indicator in top-right
        if (systemStatusTimer > 0) {
            int remainingSeconds = systemStatusTimer / 20;
            Text timerText = Text.literal(remainingSeconds + "s").formatted(Formatting.GRAY);
            context.drawText(this.textRenderer, timerText, x + width - this.textRenderer.getWidth(timerText) - 5, y + 3, 0x80FFFFFF, false);
        }
        
        // Content area with padding
        int contentX = x + 5;
        int contentY = y + 5;
        int contentWidth = width - 10;
        int contentHeight = height - 10;
        
        // Enable scissor for scrolling
        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        
        int lineHeight = this.textRenderer.fontHeight + 1;
        int startY = contentY - systemStatusScrollOffset;
        
        for (int i = 0; i < systemStatusLines.size(); i++) {
            Text line = systemStatusLines.get(i);
            int lineY = startY + (i * lineHeight);
            
            // Only render lines that are visible
            if (lineY >= contentY - lineHeight && lineY <= contentY + contentHeight) {
                context.drawText(this.textRenderer, line, contentX, lineY, 0xFFFFFF, false);
            }
        }
        
        context.disableScissor();
        
        // Draw scroll indicator if needed
        int totalContentHeight = systemStatusLines.size() * lineHeight;
        if (totalContentHeight > contentHeight) {
            // Draw scrollbar
            int scrollbarX = x + width - 8;
            int scrollbarHeight = Math.max(10, (contentHeight * contentHeight) / totalContentHeight);
            int maxScrollOffset = Math.max(0, totalContentHeight - contentHeight);
            int scrollbarY = contentY + (systemStatusScrollOffset * (contentHeight - scrollbarHeight)) / Math.max(1, maxScrollOffset);
            
            context.fill(scrollbarX, contentY, scrollbarX + 6, contentY + contentHeight, 0x40FFFFFF);
            context.fill(scrollbarX, scrollbarY, scrollbarX + 6, scrollbarY + scrollbarHeight, 0xFFFFFFFF);
        }
    }
    


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw clean solid background
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // Panel principal con pestañas
        int mainPanelWidth = 420;
        int mainPanelHeight = 260;
        int mainPanelX = (this.width - mainPanelWidth) / 2;
        int mainPanelY = (this.height - mainPanelHeight) / 2;
        
        // Dibujar pestañas
        drawTabs(context, mainPanelX, mainPanelY, mainPanelWidth);
        
        // Dibujar panel principal conectado a pestañas
        context.fill(mainPanelX, mainPanelY + 25, mainPanelX + mainPanelWidth, mainPanelY + mainPanelHeight, 0xFF2D2D30);
        context.fill(mainPanelX, mainPanelY + 25, mainPanelX + mainPanelWidth, mainPanelY + 26, 0xFF4A4A4A);
        context.fill(mainPanelX, mainPanelY + 25, mainPanelX + 1, mainPanelY + mainPanelHeight, 0xFF4A4A4A);
        context.fill(mainPanelX + mainPanelWidth - 1, mainPanelY + 25, mainPanelX + mainPanelWidth, mainPanelY + mainPanelHeight, 0xFF0F0F0F);
        context.fill(mainPanelX, mainPanelY + mainPanelHeight - 1, mainPanelX + mainPanelWidth, mainPanelY + mainPanelHeight, 0xFF0F0F0F);
        
        // Dibujar división vertical centrada entre navegación y contenido
        int navEndX = mainPanelX + 10 + 110; // Final del panel de navegación
        int contentStartX = mainPanelX + 10 + 110 + 25; // Inicio del contenido
        int dividerX = navEndX + 12; // Centrado en el espacio de 25px
        context.fill(dividerX, mainPanelY + 30, dividerX + 1, mainPanelY + mainPanelHeight - 30, 0xFF1A1A1A);

        super.render(context, mouseX, mouseY, delta);

        int panelWidth = 420;
        int panelHeight = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        // Draw title centered relative to panel
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, mainPanelX + mainPanelWidth / 2, mainPanelY - 12, 0xFFFFFF);
        
        // Draw version in bottom right corner with small text
        Text versionText = Text.literal("v1.4.0");
        context.drawText(this.textRenderer, versionText, panelX + panelWidth - this.textRenderer.getWidth(versionText) - 3, panelY + panelHeight - 10, 0x80FFFFFF, false);
        
        // Render System Status text box if in System Status panel
        if (currentMainCategory == MainCategory.ADMIN_TOOLS && currentAdminSubCategory == AdminSubCategory.SYSTEM_STATUS) {
            int contentX = panelX + 135; // After navigation and divider
            int contentY = panelY + 58; // Below the refresh button
            int contentWidth = panelWidth - 150; // Account for divider
            int textBoxHeight = 115; // Smaller to fit below button
            
            renderSystemStatusTextBox(context, contentX, contentY, contentWidth, textBoxHeight);
        }



        if (this.pokemonNameField != null && this.pokemonNameField.isVisible()) {
            this.pokemonNameField.renderSuggestions(context, mouseX, mouseY);
        }
        if (this.playerNameField != null && this.playerNameField.isVisible()) {
            this.playerNameField.renderSuggestions(context, mouseX, mouseY);
        }

        // Draw response panel below main panel with improved integration
        if (!responseLines.isEmpty()) {
            int responsePanelY = panelY + panelHeight + 3; // Closer to main panel
            int textWidth = panelWidth - 12; // More padding
            int totalTextHeight = 0;
            for (Text line : responseLines) {
                totalTextHeight += this.textRenderer.getWrappedLinesHeight(line, textWidth);
            }
            int responsePanelHeight = Math.max(24, 8 + totalTextHeight); // Minimum height and padding

            // Draw background matching main panel style - solid black with white border
            context.fill(panelX, responsePanelY, panelX + panelWidth, responsePanelY + responsePanelHeight, 0xFF000000);
            context.drawBorder(panelX, responsePanelY, panelWidth, responsePanelHeight, 0xFFFFFFFF);
            
            // Add subtle inner border for depth
            context.fill(panelX + 1, responsePanelY + 1, panelX + panelWidth - 1, responsePanelY + 2, 0x40FFFFFF);
            
            int currentTextY = responsePanelY + 6; // More top padding
            for (Text line : responseLines) {
                context.drawTextWrapped(this.textRenderer, line, panelX + 6, currentTextY, textWidth, 0xFFFFFF);
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
        
        // Handle system status timer
        if (systemStatusTimer > 0) {
            systemStatusTimer--;
            if (systemStatusTimer == 0) {
                systemStatusLines.clear();
                systemStatusScrollOffset = 0;
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
        
        // Update swarm button state in real-time
        if (this.currentMainCategory == MainCategory.EVENTS && this.currentEventSubCategory == EventSubCategory.SWARM_EVENTS && this.children() != null) {
            for (var child : this.children()) {
                if (child instanceof ButtonWidget button) {
                    String msg = button.getMessage().getString();
                    if (msg.contains("Start Swarm")) {
                        button.active = !PokeNotifierClient.hasActiveSwarm; // Independent of automatic mode
                    } else if (msg.contains("Cancel Swarm")) {
                        button.active = PokeNotifierClient.hasActiveSwarm;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Detectar clics en pestañas
        if (button == 0) {
            int panelWidth = 420;
            int panelX = (this.width - panelWidth) / 2;
            int panelY = (this.height - 260) / 2;
            int tabWidth = PokeNotifierClient.isPlayerAdmin ? 85 : 120;
            int tabY = panelY + 5;
            int tabHeight = 20;
            
            // User Tools tab
            int userTabX = panelX + 5;
            if (mouseX >= userTabX && mouseX <= userTabX + tabWidth && mouseY >= tabY - 2 && mouseY <= tabY + tabHeight) {
                this.currentMainCategory = MainCategory.USER_TOOLS;
                this.clearAndInit();
                return true;
            }
            
            if (PokeNotifierClient.isPlayerAdmin) {
                // Events tab
                int eventsTabX = panelX + 8 + tabWidth;
                if (mouseX >= eventsTabX && mouseX <= eventsTabX + tabWidth && mouseY >= tabY - 2 && mouseY <= tabY + tabHeight) {
                    this.currentMainCategory = MainCategory.EVENTS;
                    this.clearAndInit();
                    return true;
                }
                
                // Admin tab
                int adminTabX = panelX + 11 + tabWidth * 2;
                if (mouseX >= adminTabX && mouseX <= adminTabX + tabWidth && mouseY >= tabY - 2 && mouseY <= tabY + tabHeight) {
                    this.currentMainCategory = MainCategory.ADMIN_TOOLS;
                    this.clearAndInit();
                    return true;
                }
            }
        }
        
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
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Handle scrolling in System Status text box
        if (currentMainCategory == MainCategory.ADMIN_TOOLS && currentAdminSubCategory == AdminSubCategory.SYSTEM_STATUS && !systemStatusLines.isEmpty()) {
            int panelWidth = 420;
            int panelHeight = 260;
            int panelX = (this.width - panelWidth) / 2;
            int panelY = (this.height - panelHeight) / 2;
            int contentX = panelX + 135; // After navigation and divider
            int contentY = panelY + 58; // Below the refresh button
            int contentWidth = panelWidth - 150; // Account for divider
            int textBoxHeight = 115; // Smaller to fit below button
            
            // Check if mouse is over the text box
            if (mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= contentY && mouseY <= contentY + textBoxHeight) {
                int lineHeight = this.textRenderer.fontHeight + 1;
                int totalContentHeight = systemStatusLines.size() * lineHeight;
                int contentHeight = textBoxHeight - 10; // Account for padding
                
                if (totalContentHeight > contentHeight) {
                    int scrollAmount = (int) (verticalAmount * 20); // Increased scroll speed
                    systemStatusScrollOffset = Math.max(0, Math.min(totalContentHeight - contentHeight, systemStatusScrollOffset - scrollAmount));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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