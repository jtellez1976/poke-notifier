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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import com.zehro_mc.pokenotifier.ConfigClient;
import com.zehro_mc.pokenotifier.ConfigManager;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PokeNotifierCustomScreen extends Screen {
    private final Screen parent;
    private AutocompleteTextFieldWidget pokemonNameField;
    private ConfigClient config;

    // --- NEW: Response Panel ---
    private List<Text> responseLines = new ArrayList<>();
    private int responseTimer = 0;

    private enum Category {
        NOTIFICATIONS,
        CUSTOM_HUNT,
        CATCH_EM_ALL,
        INFO
    }

    private Category currentCategory = Category.NOTIFICATIONS;

    public PokeNotifierCustomScreen(Screen parent) {
        super(Text.literal("PokéNotifier Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.config = ConfigManager.getClientConfig();
        buildLayout();
    }

    private void buildLayout() {
        // --- NEW: Centered Panel Layout ---
        int panelWidth = 320; // Increased width
        int panelHeight = 220; // Increased height
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        // Navigation Panel (Left side of our panel)
        int navX = panelX + 10;
        int navY = panelY + 30;
        int navWidth = 110; // Increased width for nav buttons

        addDrawableChild(createNavButton(navX, navY, navWidth, "🔔 Notifications", Category.NOTIFICATIONS));
        addDrawableChild(createNavButton(navX, navY + 25, navWidth, "🎯 Custom Hunt", Category.CUSTOM_HUNT));
        addDrawableChild(createNavButton(navX, navY + 50, navWidth, "🏆 Catch 'em All", Category.CATCH_EM_ALL));
        addDrawableChild(createNavButton(navX, navY + 75, navWidth, "ℹ️ Info & Help", Category.INFO));

        // Content Panel (Right side of our panel)
        int contentX = panelX + navWidth + 20;
        int contentY = panelY + 30;
        int contentWidth = panelWidth - navWidth - 40; // Adjusted width

        switch (currentCategory) {
            case NOTIFICATIONS -> buildNotificationsPanel(contentX, contentY, contentWidth);
            case CUSTOM_HUNT -> buildCustomHuntPanel(contentX, contentY, contentWidth);
            case CATCH_EM_ALL -> buildCatchEmAllPanel(contentX, contentY, contentWidth);
            case INFO -> buildInfoPanel(contentX, contentY, contentWidth);
        }

        // Close Button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(panelX + (panelWidth - 100) / 2, panelY + panelHeight - 25, 100, 20)
                .build());
    }

    private ButtonWidget createNavButton(int x, int y, int width, String text, Category category) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(text), b -> {
            this.currentCategory = category;
            this.clearAndInit();
        }).dimensions(x, y, width, 20).build();
        button.active = this.currentCategory != category;
        return button;
    }

    private void buildNotificationsPanel(int x, int y, int width) {
        // FIX: These buttons now modify the config directly for instant feedback.
        addDrawableChild(createToggleButton("Chat Alerts", config.alert_chat_enabled, newValue -> config.alert_chat_enabled = newValue, x, y, width));
        addDrawableChild(createToggleButton("Sound Alerts", config.alert_sounds_enabled, newValue -> config.alert_sounds_enabled = newValue, x, y + 25, width));
        addDrawableChild(createToggleButton("HUD Alerts", config.alert_toast_enabled, newValue -> config.alert_toast_enabled = newValue, x, y + 50, width));
        addDrawableChild(createToggleButton("Silent Mode", config.silent_mode_enabled, newValue -> config.silent_mode_enabled = newValue, x, y + 85, width));
        addDrawableChild(createToggleButton("Searching", config.searching_enabled, newValue -> config.searching_enabled = newValue, x, y + 110, width));
    }

    private ButtonWidget createToggleButton(String label, boolean currentValue, java.util.function.Consumer<Boolean> configUpdater, int x, int y, int width) {
        Text message = Text.literal(label + ": ").append(currentValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        return ButtonWidget.builder(message, button -> {
            boolean newValue = !currentValue;
            configUpdater.accept(newValue);
            button.setMessage(Text.literal(label + ": ").append(newValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED)));
        }).dimensions(x, y, width, 20).build();
    }

    private void buildCustomHuntPanel(int x, int y, int width) {
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("e.g., Pikachu"));
        addDrawableChild(this.pokemonNameField);

        int buttonWidth = (width - 10) / 2;
        addDrawableChild(createActionButton("➕ Add", "pnc customcatch add", x, y + 25, buttonWidth));

        ButtonWidget removeButton = createActionButton("➖ Remove", "pnc customcatch remove", x + buttonWidth + 10, y + 25, buttonWidth);
        removeButton.active = PokeNotifierClient.customHuntListSize > 0; // FIX: Disable if list is empty
        addDrawableChild(removeButton);

        addDrawableChild(createActionButton("🗑️ Clear List", "pnc customcatch clear", x, y + 50, width));
        addDrawableChild(createActionButton("📋 View List", "pnc customcatch view", x, y + 75, width));
    }

    private ButtonWidget createActionButton(String text, String command, int x, int y, int width) {
        return ButtonWidget.builder(Text.literal(text), button -> {
            String pokemonName = (this.pokemonNameField != null) ? this.pokemonNameField.getText().trim() : "";
            String finalCommand = command.contains("add") || command.contains("remove") ? command + " " + pokemonName : command;
            if (!finalCommand.endsWith(" ") || command.contains("view") || command.contains("clear") || command.contains("status") || command.contains("disable")) {
                executeCommand(finalCommand);
            }
        }).dimensions(x, y, width, 20).build();
    }

    private void buildCatchEmAllPanel(int x, int y, int width) {
        List<String> generations = Arrays.asList("gen1", "gen2", "gen3", "gen4", "gen5", "gen6", "gen7", "gen8", "gen9");
        int buttonWidth = (width - 5) / 2; // FIX: 2 buttons per row
        int buttonHeight = 20;

        for (int i = 0; i < generations.size(); i++) {
            String gen = generations.get(i);
            int row = i / 2;
            int col = i % 2;
            int buttonX = x + col * (buttonWidth + 5);
            int buttonY = y + row * (buttonHeight + 5);

            ButtonWidget button = ButtonWidget.builder(Text.literal(getGenerationDisplayName(gen)), b -> executeCommand("pnc catchemall enable " + gen))
                    .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                    .build();

            if (gen.equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
                button.setMessage(Text.literal(getGenerationDisplayName(gen)).formatted(Formatting.GOLD, Formatting.UNDERLINE));
            }
            addDrawableChild(button);
        }

        int statusY = y + 5 * (buttonHeight + 5); // FIX: Adjusted Y position
        addDrawableChild(createActionButton("⏹️ Stop Tracking", "pnc catchemall disable " + PokeNotifierClient.currentCatchEmAllGeneration, x, statusY, width));
        addDrawableChild(createActionButton("📊 View Status", "pnc catchemall status", x, statusY + 25, width));
    }

    private void buildInfoPanel(int x, int y, int width) {
        addDrawableChild(createActionButton("Help", "pnc help", x, y, width));
        addDrawableChild(createActionButton("Version", "pnc version", x, y + 25, width));
        addDrawableChild(createActionButton("Status", "pnc status", x, y + 50, width));

        // FIX: Use a CyclingButton for update source
        addDrawableChild(CyclingButtonWidget.<String>builder(value -> Text.literal("Update Source: " + value))
                .values("modrinth", "curseforge", "none")
                .initially(ConfigManager.getServerConfig().update_checker_source)
                .build(x, y + 85, width, 20, Text.literal("Update Source"), (button, value) -> {
                    executeCommand("pnc update " + value);
                }));
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // --- NEW: Draw the centered panel ---
        int panelWidth = 320;
        int panelHeight = 220;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE01A1A1A); // Darker, more transparent
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF888888); // Gray border

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 10, 0xFFFFFF);

        if (this.pokemonNameField != null && this.pokemonNameField.isVisible()) {
            this.pokemonNameField.renderSuggestions(context, mouseX, mouseY);
        }

        // --- NEW: Render the response panel ---
        if (!responseLines.isEmpty()) {
            int responsePanelY = panelY + panelHeight + 5;
            int responsePanelHeight = 5 + responseLines.size() * (this.textRenderer.fontHeight + 1);
            context.fill(panelX, responsePanelY, panelX + panelWidth, responsePanelY + responsePanelHeight, 0xE0000000);
            for (int i = 0; i < responseLines.size(); i++) {
                context.drawTextWithShadow(this.textRenderer, responseLines.get(i), panelX + 5, responsePanelY + 4 + i * (this.textRenderer.fontHeight + 1), 0xFFFFFF);
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
    }

    public void displayResponse(List<Text> lines) {
        this.responseLines = lines;
        this.responseTimer = 200; // Display for 10 seconds
    }

    private void executeCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(command);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.pokemonNameField != null && this.pokemonNameField.isFocused() && this.pokemonNameField.keyPressed(keyCode, scanCode, modifiers)) {
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