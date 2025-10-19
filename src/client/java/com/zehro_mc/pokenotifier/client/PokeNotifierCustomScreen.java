/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;

public class PokeNotifierCustomScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget pokemonNameField;
    private String selectedGeneration = "gen1";

    // Reference to the configuration
    private ConfigClient config;

    // Variables to store title positions
    private int alertTitleY = 0;
    private int modeTitleY = 0;
    private int customCatchTitleY = 0;
    private int catchEmAllTitleY = 0;

    public PokeNotifierCustomScreen(Screen parent) {
        super(Text.literal("PokéNotifier Configuration"));
        this.parent = parent;
        this.config = ConfigManager.getClientConfig();
    }

    @Override
    protected void init() {
        super.init();

        int panelWidth = 260;
        int panelX = (this.width - panelWidth) / 2;
        int currentY = 30;

        // ==================== ALERT PANEL ====================
        alertTitleY = currentY;
        currentY = createAlertPanel(panelX, currentY, panelWidth);
        currentY += 10;

        // ==================== MODE PANEL ====================
        modeTitleY = currentY;
        currentY = createModePanel(panelX, currentY, panelWidth);
        currentY += 10;

        // ==================== CUSTOM CATCH PANEL ====================
        customCatchTitleY = currentY;
        currentY = createCustomCatchPanel(panelX, currentY, panelWidth);
        currentY += 10;

        // ==================== CATCH 'EM ALL PANEL ====================
        catchEmAllTitleY = currentY;
        currentY = createCatchEmAllPanel(panelX, currentY, panelWidth);

        // ==================== CLOSE BUTTON ====================
        this.addDrawableChild(ButtonWidget.builder(Text.literal("✕ Close"), button -> {
            this.client.setScreen(this.parent);
        }).dimensions(panelX, this.height - 30, panelWidth, 20).build());
    }

    private int createAlertPanel(int x, int y, int width) {
        y += 20;

        // Toggle: Enable Chat Alerts
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(config.alert_chat_enabled ? "✅ Chat Alerts: ON" : "❌ Chat Alerts: OFF"),
                button -> {
                    executeCommand("pnc alerts chat");
                    config.alert_chat_enabled = !config.alert_chat_enabled;
                    button.setMessage(Text.literal(config.alert_chat_enabled ? "✅ Chat Alerts: ON" : "❌ Chat Alerts: OFF"));
                }
        ).dimensions(x, y, width, 20).build());
        y += 25;

        // Toggle: Enable Sound Alerts
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(config.alert_sounds_enabled ? "✅ Sound Alerts: ON" : "❌ Sound Alerts: OFF"),
                button -> {
                    executeCommand("pnc alerts sound");
                    config.alert_sounds_enabled = !config.alert_sounds_enabled;
                    button.setMessage(Text.literal(config.alert_sounds_enabled ? "✅ Sound Alerts: ON" : "❌ Sound Alerts: OFF"));
                }
        ).dimensions(x, y, width, 20).build());
        y += 25;

        // Toggle: Enable HUD (Toast) Alerts
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(config.alert_toast_enabled ? "✅ HUD Alerts: ON" : "❌ HUD Alerts: OFF"),
                button -> {
                    executeCommand("pnc alerts toast");
                    config.alert_toast_enabled = !config.alert_toast_enabled;
                    button.setMessage(Text.literal(config.alert_toast_enabled ? "✅ HUD Alerts: ON" : "❌ HUD Alerts: OFF"));
                }
        ).dimensions(x, y, width, 20).build());

        return y + 30;
    }

    private int createModePanel(int x, int y, int width) {
        y += 20;

        // Toggle: Silent Mode (Master Switch)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(config.silent_mode_enabled ? "🔇 Silent Mode: ON" : "🔊 Silent Mode: OFF"),
                button -> {
                    executeCommand("pnc silent");
                    config.silent_mode_enabled = !config.silent_mode_enabled;
                    button.setMessage(Text.literal(config.silent_mode_enabled ? "🔇 Silent Mode: ON" : "🔊 Silent Mode: OFF"));
                }
        ).dimensions(x, y, width, 20).build());
        y += 25;

        // Toggle: Enable Pokémon Searching (Master Switch para el mod)
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(config.searching_enabled ? "🔍 Searching: ENABLED" : "👁️ Searching: DISABLED"),
                button -> {
                    executeCommand("pnc search");
                    config.searching_enabled = !config.searching_enabled;
                    button.setMessage(Text.literal(config.searching_enabled ? "🔍 Searching: ENABLED" : "👁️ Searching: DISABLED"));
                }
        ).dimensions(x, y, width, 20).build());

        return y + 30;
    }

    private int createCustomCatchPanel(int x, int y, int width) {
        y += 20;

        // Text field for Pokémon name
        this.pokemonNameField = new TextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal("Enter Pokémon name"));
        this.pokemonNameField.setPlaceholder(Text.literal("Enter Pokémon name"));
        this.addDrawableChild(this.pokemonNameField);
        y += 25;

        // Buttons in a row
        int buttonWidth = (width - 10) / 3;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("➕ Add"), button -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                executeCommandAndClose("pnc customcatch add " + pokemonName);
            }
        }).dimensions(x, y, buttonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("➖ Remove"), button -> {
            String pokemonName = this.pokemonNameField.getText().trim();
            if (!pokemonName.isEmpty()) {
                executeCommandAndClose("pnc customcatch remove " + pokemonName);
            }
        }).dimensions(x + buttonWidth + 5, y, buttonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("🗑️ Clear"), button -> {
            executeCommandAndClose("pnc customcatch clear");
        }).dimensions(x + (buttonWidth + 5) * 2, y, buttonWidth, 20).build());
        y += 25;

        // Button to view the list
        this.addDrawableChild(ButtonWidget.builder(Text.literal("📋 View Hunt List"), button -> {
            executeCommandAndClose("pnc customcatch view");
        }).dimensions(x, y, width, 20).build());

        return y + 30;
    }

    private int createCatchEmAllPanel(int x, int y, int width) {
        y += 20;

        // Generation selector
        this.addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
                .values("gen1", "gen2", "gen3", "gen4", "gen5", "gen6", "gen7", "gen8", "gen9")
                .initially(this.selectedGeneration)
                .build(x, y, width, 20, Text.literal("Select Generation"),
                        (button, value) -> this.selectedGeneration = value));
        y += 25;

        // Control buttons
        int buttonWidth = (width - 5) / 2;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("▶ Start Tracking"), button -> {
            executeCommandAndClose("pnc catchemall enable " + this.selectedGeneration);
        }).dimensions(x, y, buttonWidth, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("⏹️ Stop Tracking"), button -> {
            executeCommandAndClose("pnc catchemall disable");
        }).dimensions(x + buttonWidth + 5, y, buttonWidth, 20).build());
        y += 25;

        // Button to view detailed status
        this.addDrawableChild(ButtonWidget.builder(Text.literal("📊 View Detailed Status"), button -> {
            executeCommandAndClose("pnc catchemall status");
        }).dimensions(x, y, width, 20).build());

        return y + 30;
    }

    private String getGenerationDisplayName(String gen) {
        switch (gen) {
            case "gen1": return "Kanto";
            case "gen2": return "Johto";
            case "gen3": return "Hoenn";
            case "gen4": return "Sinnoh";
            case "gen5": return "Unova";
            case "gen6": return "Kalos";
            case "gen7": return "Alola";
            case "gen8": return "Galar";
            case "gen9": return "Paldea";
            case "none": return "Not Tracking";
            default: return gen;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent background to see the chat
        context.fill(0, 0, this.width, this.height, 0x99000000);

        // Main panel
        int panelWidth = 280;
        int panelX = (this.width - panelWidth) / 2;
        context.fill(panelX - 5, 15, panelX + panelWidth + 5, this.height - 10, 0xFF1A1A1A);
        context.fill(panelX - 5, 15, panelX + panelWidth + 5, 16, 0xFFFFFF00);

        // Render titles and dividers
        renderTitlesAndDividers(context, panelX, panelWidth);

        super.render(context, mouseX, mouseY, delta);

        // Main title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 5, 0xFFFFFF);

        // Render Catch 'em All status
        renderCatchEmAllStatus(context, panelX, catchEmAllTitleY + 95, panelWidth);
    }

    private void renderTitlesAndDividers(net.minecraft.client.gui.DrawContext context, int x, int width) {
        // Section titles
        context.drawTextWithShadow(this.textRenderer, Text.literal("🔔 Alert Settings"), x, alertTitleY, 0xFFFF00);
        context.drawTextWithShadow(this.textRenderer, Text.literal("⚙️ Mode Settings"), x, modeTitleY, 0xFFFF00);
        context.drawTextWithShadow(this.textRenderer, Text.literal("🎯 Custom Catch List"), x, customCatchTitleY, 0xFFFF00);
        context.drawTextWithShadow(this.textRenderer, Text.literal("🏆 Catch 'em All"), x, catchEmAllTitleY, 0xFFFF00);

        // Divider lines
        context.fill(x, alertTitleY + 12, x + width, alertTitleY + 13, 0x44FFFFFF);
        context.fill(x, modeTitleY + 12, x + width, modeTitleY + 13, 0x44FFFFFF);
        context.fill(x, customCatchTitleY + 12, x + width, customCatchTitleY + 13, 0x44FFFFFF);
        context.fill(x, catchEmAllTitleY + 12, x + width, catchEmAllTitleY + 13, 0x44FFFFFF);
    }

    private void renderCatchEmAllStatus(net.minecraft.client.gui.DrawContext context, int x, int y, int width) {
        String status = "Current: " + getGenerationDisplayName(PokeNotifierClient.currentCatchEmAllGeneration);
        int color = "none".equals(PokeNotifierClient.currentCatchEmAllGeneration) ? 0xFF666666 : 0xFF00FF00;
        context.drawTextWithShadow(this.textRenderer, Text.literal(status), x, y, color);

        // Show progress if active
        if (!"none".equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
            String progress = PokeNotifierClient.catchCaughtCount + "/" + PokeNotifierClient.catchTotalCount + " caught";
            context.drawTextWithShadow(this.textRenderer, Text.literal(progress), x, y + 12, 0xFFFFFF);
        }
    }

    private void executeCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            client.player.networkHandler.sendChatCommand(command);
        }
    }

    private void executeCommandAndClose(String command) {
        executeCommand(command);
        // Close the screen to see the chat
        this.client.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        // Save configuration if necessary
        ConfigManager.saveClientConfigToFile();
        this.client.setScreen(null); // Always return to the game screen
    }
}