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

    private enum MainCategory { USER_TOOLS, ADMIN_TOOLS }
    private MainCategory currentMainCategory = MainCategory.USER_TOOLS;

    private enum UserSubCategory { NOTIFICATIONS, CUSTOM_HUNT, CATCH_EM_ALL, INFO }
    private UserSubCategory currentUserSubCategory = UserSubCategory.NOTIFICATIONS;

    private enum AdminSubCategory { SERVER_CONTROL, EVENT_MANAGEMENT, PLAYER_DATA, TESTING }
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
        int panelWidth = 420;
        int panelHeight = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        // --- Main Tabs (Top) ---
        int tabY = panelY + 25;
        int tabWidth = 100;
        ButtonWidget userTab = ButtonWidget.builder(Text.literal("User Tools"), b -> {
            this.currentMainCategory = MainCategory.USER_TOOLS;
            this.clearAndInit();
        }).dimensions(panelX + 5, tabY, tabWidth, 20).build();
        userTab.active = this.currentMainCategory != MainCategory.USER_TOOLS;
        addDrawableChild(userTab);

        if (PokeNotifierClient.isPlayerAdmin) {
            ButtonWidget adminTab = ButtonWidget.builder(Text.literal("👑 Admin Tools"), b -> {
                this.currentMainCategory = MainCategory.ADMIN_TOOLS;
                this.clearAndInit();
            }).dimensions(panelX + 10 + tabWidth, tabY, tabWidth + 10, 20).build();
            adminTab.active = this.currentMainCategory != MainCategory.ADMIN_TOOLS;
            addDrawableChild(adminTab);
        }

        // --- Build Content based on Main Tab ---
        if (currentMainCategory == MainCategory.USER_TOOLS) {
            buildUserToolsLayout(panelX, panelY, panelWidth, panelHeight);
        } else if (currentMainCategory == MainCategory.ADMIN_TOOLS && PokeNotifierClient.isPlayerAdmin) {
            buildAdminToolsLayout(panelX, panelY, panelWidth, panelHeight);
        }

        // Close Button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                .dimensions(panelX + (panelWidth - 100) / 2, panelY + panelHeight - 28, 100, 20)
                .build());
    }

    // --- USER TOOLS LAYOUT ---
    private void buildUserToolsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        int navX = panelX + 10;
        int navY = panelY + 60;
        int navWidth = 110;

        addDrawableChild(createSubNavButton(navX, navY, navWidth, "🔔 Notifications", UserSubCategory.NOTIFICATIONS, currentUserSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 25, navWidth, "🎯 Custom Hunt", UserSubCategory.CUSTOM_HUNT, currentUserSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 50, navWidth, "🏆 Catch 'em All", UserSubCategory.CATCH_EM_ALL, currentUserSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 75, navWidth, "ℹ️ Info & Help", UserSubCategory.INFO, currentUserSubCategory));

        int contentX = panelX + navWidth + 20;
        int contentY = panelY + 60;
        int contentWidth = panelWidth - navWidth - 40;

        switch (currentUserSubCategory) {
            case NOTIFICATIONS -> buildNotificationsPanel(contentX, contentY, contentWidth);
            case CUSTOM_HUNT -> buildCustomHuntPanel(contentX, contentY, contentWidth);
            case CATCH_EM_ALL -> buildCatchEmAllPanel(contentX, contentY, contentWidth);
            case INFO -> buildInfoPanel(contentX, contentY, contentWidth);
        }
    }

    // --- ADMIN TOOLS LAYOUT ---
    private void buildAdminToolsLayout(int panelX, int panelY, int panelWidth, int panelHeight) {
        int navX = panelX + 10;
        int navY = panelY + 60;
        int navWidth = 130;

        addDrawableChild(createSubNavButton(navX, navY, navWidth, "⚙️ Server Control", AdminSubCategory.SERVER_CONTROL, currentAdminSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 25, navWidth, "🎉 Event Management", AdminSubCategory.EVENT_MANAGEMENT, currentAdminSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 50, navWidth, "👤 Player Data", AdminSubCategory.PLAYER_DATA, currentAdminSubCategory));
        addDrawableChild(createSubNavButton(navX, navY + 75, navWidth, "🔬 Testing", AdminSubCategory.TESTING, currentAdminSubCategory));

        int contentX = panelX + navWidth + 20;
        int contentY = panelY + 60;
        int contentWidth = panelWidth - navWidth - 40;

        switch (currentAdminSubCategory) {
            case SERVER_CONTROL -> buildServerControlPanel(contentX, contentY, contentWidth);
            case EVENT_MANAGEMENT -> buildEventManagementPanel(contentX, contentY, contentWidth);
            case PLAYER_DATA -> buildPlayerDataPanel(contentX, contentY, contentWidth);
            case TESTING -> buildTestingPanel(contentX, contentY, contentWidth);
        }
    }

    private <T extends Enum<T>> ButtonWidget createSubNavButton(int x, int y, int width, String text, T category, T current) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(text), b -> {
            if (category instanceof UserSubCategory) this.currentUserSubCategory = (UserSubCategory) category;
            if (category instanceof AdminSubCategory) this.currentAdminSubCategory = (AdminSubCategory) category;
            this.clearAndInit();
        }).dimensions(x, y, width, 20).build();
        button.active = !category.equals(current);
        return button;
    }

    // --- USER PANEL BUILDERS ---

    private void buildNotificationsPanel(int x, int y, int width) {
        addDrawableChild(createToggleButton("Chat Alerts", clientConfig.alert_chat_enabled, newValue -> clientConfig.alert_chat_enabled = newValue, x, y, width));
        addDrawableChild(createToggleButton("Sound Alerts", clientConfig.alert_sounds_enabled, newValue -> clientConfig.alert_sounds_enabled = newValue, x, y + 25, width));
        addDrawableChild(createToggleButton("HUD Alerts", clientConfig.alert_toast_enabled, newValue -> clientConfig.alert_toast_enabled = newValue, x, y + 50, width));
        addDrawableChild(createToggleButton("Silent Mode", clientConfig.silent_mode_enabled, newValue -> clientConfig.silent_mode_enabled = newValue, x, y + 85, width));
        addDrawableChild(createToggleButton("Searching", clientConfig.searching_enabled, newValue -> clientConfig.searching_enabled = newValue, x, y + 110, width));
    }

    private void buildCustomHuntPanel(int x, int y, int width) {
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("e.g., Pikachu"));
        addDrawableChild(this.pokemonNameField);

        int buttonWidth = (width - 10) / 2;
        addDrawableChild(createActionButton("➕ Add", "pnc customcatch add", x, y + 25, buttonWidth));

        ButtonWidget removeButton = createActionButton("➖ Remove", "pnc customcatch remove", x + buttonWidth + 10, y + 25, buttonWidth);
        addDrawableChild(removeButton);

        addDrawableChild(createActionButton("🗑️ Clear List", "pnc customcatch clear", x, y + 50, width));
        addDrawableChild(createActionButton("📋 View List", "pnc customcatch view", x, y + 75, width));
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

            ButtonWidget button = ButtonWidget.builder(Text.literal(getGenerationDisplayName(gen)), b -> executeCommand("pnc catchemall enable " + gen))
                    .dimensions(buttonX, buttonY, buttonWidth, buttonHeight)
                    .build();

            if (gen.equals(PokeNotifierClient.currentCatchEmAllGeneration)) {
                button.setMessage(Text.literal(getGenerationDisplayName(gen)).formatted(Formatting.GOLD, Formatting.UNDERLINE));
            }
            addDrawableChild(button);
        }

        int statusY = y + 5 * (buttonHeight + 5);
        addDrawableChild(createActionButton("⏹️ Stop Tracking", "pnc catchemall disable " + PokeNotifierClient.currentCatchEmAllGeneration, x, statusY, width));
        addDrawableChild(createActionButton("📊 View Status", "pnc catchemall status", x, statusY + 25, width));
    }

    private void buildInfoPanel(int x, int y, int width) {
        addDrawableChild(createActionButton("Help", "pnc help", x, y, width));
        addDrawableChild(createActionButton("Version", "pnc version", x, y + 25, width));
        addDrawableChild(createActionButton("Status", "pnc status", x, y + 50, width));


        addDrawableChild(CyclingButtonWidget.<String>builder(this::capitalize)
                .values("modrinth", "curseforge", "none")
                .initially(serverConfig.update_checker_source)
                .build(x, y + 105, width, 20, Text.empty(), (button, value) -> { // FIX: Adjusted Y position
                    executeCommand("pnc update " + value);
                }));
    }

    // --- ADMIN PANEL BUILDERS ---

    private void buildServerControlPanel(int x, int y, int width) {
        addDrawableChild(createAdminToggleButton("Debug Mode", PokeNotifierClient.isServerDebugMode, "pokenotifier test debug", x, y, width));
        addDrawableChild(createAdminToggleButton("Test Mode", PokeNotifierClient.isServerTestMode, "pokenotifier test mode", x, y + 25, width));

        addDrawableChild(createActionButton("Server Status", "pokenotifier status", x, y + 60, width));
        addDrawableChild(createActionButton("Reload Configs", "pokenotifier config reload", x, y + 85, width));

        addDrawableChild(ButtonWidget.builder(Text.literal("Reset All Configs"), b -> {
            this.client.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) executeCommand("pokenotifier config reset");
                this.client.setScreen(this);
            }, Text.literal("Confirm Reset"), Text.literal("This will reset ALL configs. Are you sure?")));
        }).dimensions(x, y + 110, width, 20).build());
    }

    private void buildEventManagementPanel(int x, int y, int width) {
        addDrawableChild(createAdminToggleButton("Bounty System", PokeNotifierClient.isServerBountySystemEnabled, "pokenotifier bounty system", x, y, width));
        this.pokemonNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y + 35, width, 20, Text.literal(""), () -> PokeNotifierApi.getAllPokemonNames().toList());
        this.pokemonNameField.setPlaceholder(Text.literal("Pokémon for Swarm"));
        addDrawableChild(this.pokemonNameField);

        addDrawableChild(createActionButton("Start Manual Swarm", "pokenotifier swarm start", x, y + 60, width));
    }

    private void buildPlayerDataPanel(int x, int y, int width) {
        this.playerNameField = new AutocompleteTextFieldWidget(this.textRenderer, x, y, width, 20, Text.literal(""),
                () -> this.client.getNetworkHandler().getPlayerList().stream().map(p -> p.getProfile().getName()).toList());
        this.playerNameField.setPlaceholder(Text.literal("Player Name"));
        addDrawableChild(this.playerNameField);

        ButtonWidget autocompleteButton = ButtonWidget.builder(Text.literal("Autocomplete Gen"), b -> {
            String playerName = this.playerNameField.getText();
            if (!playerName.isEmpty()) {
                this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) executeCommand("pokenotifier data autocomplete " + playerName);
                    this.client.setScreen(this);
                }, Text.literal("Confirm Autocomplete"), Text.literal("Complete current gen for " + playerName + "?")));
            }
        }).dimensions(x, y + 25, width, 20).build();
        addDrawableChild(autocompleteButton);

        ButtonWidget rollbackButton = ButtonWidget.builder(Text.literal("Rollback Progress"), b -> {
            String playerName = this.playerNameField.getText();
            if (!playerName.isEmpty()) {
                this.client.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) executeCommand("pokenotifier data rollback " + playerName);
                    this.client.setScreen(this);
                }, Text.literal("Confirm Rollback"), Text.literal("Restore progress backup for " + playerName + "?")));
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

        addDrawableChild(createActionButton("Spawn Test Pokémon", "pokenotifier test spawn", x, y + 50, width));
    }

    private ButtonWidget createAdminToggleButton(String label, boolean currentValue, String commandBase, int x, int y, int width) {
        Text message = Text.literal(label + ": ").append(currentValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        return ButtonWidget.builder(message, button -> {
            boolean newValue = !currentValue;
            executeCommand(commandBase + (newValue ? " enable" : " disable"));
            // The server will handle the config change, but we update the button for instant feedback.
            // Note: This is an optimistic update. The true state will be reflected on next screen open.
            button.setMessage(Text.literal(label + ": ").append(newValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED)));
        }).dimensions(x, y, width, 20).build();
    }

    // --- HELPER METHODS ---

    private ButtonWidget createToggleButton(String label, boolean currentValue, java.util.function.Consumer<Boolean> configUpdater, int x, int y, int width) {
        Text message = Text.literal(label + ": ").append(currentValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED));
        return ButtonWidget.builder(message, button -> {
            boolean newValue = !currentValue;
            configUpdater.accept(newValue);
            button.setMessage(Text.literal(label + ": ").append(newValue ? Text.literal("ON").formatted(Formatting.GREEN) : Text.literal("OFF").formatted(Formatting.RED)));
        }).dimensions(x, y, width, 20).build();
    }

    private ButtonWidget createActionButton(String text, String command, int x, int y, int width) {
        return ButtonWidget.builder(Text.literal(text), button -> {
            String pokemonName = (this.pokemonNameField != null) ? this.pokemonNameField.getText().trim() : "";
            String finalCommand = command.contains("add") || command.contains("remove") || command.contains("start") || command.contains("spawn") ? command + " " + pokemonName : command;
            if (!finalCommand.endsWith(" ") || command.contains("view") || command.contains("clear") || command.contains("status") || command.contains("disable") || command.contains("reload") || command.contains("reset") || command.contains("help") || command.contains("version")) {
                executeCommand(finalCommand);
            }
        }).dimensions(x, y, width, 20).build();
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

    private Text capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return Text.empty();
        }
        String capitalized = str.substring(0, 1).toUpperCase() + str.substring(1);
        return Text.literal(capitalized).formatted(Formatting.GOLD);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int panelWidth = 420;
        int panelHeight = 260;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE01A1A1A);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF888888);
        context.fill(panelX, panelY + 50, panelX + panelWidth, panelY + 51, 0xFF888888);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelY + 10, 0xFFFFFF);

        if (currentMainCategory == MainCategory.USER_TOOLS && currentUserSubCategory == UserSubCategory.INFO) {
            context.drawTextWithShadow(this.textRenderer, "Update Source:", panelX + 140, panelY + 60 + 92, 0xFFFFFF);
        }

        if (this.pokemonNameField != null && this.pokemonNameField.isVisible()) {
            this.pokemonNameField.renderSuggestions(context, mouseX, mouseY);
        }
        if (this.playerNameField != null && this.playerNameField.isVisible()) {
            this.playerNameField.renderSuggestions(context, mouseX, mouseY);
        }

        if (!responseLines.isEmpty()) {
            int responsePanelY = panelY + panelHeight + 5;
            int textWidth = panelWidth - 10;
            int totalTextHeight = 0;
            for (Text line : responseLines) {
                totalTextHeight += this.textRenderer.getWrappedLinesHeight(line, textWidth);
            }
            int responsePanelHeight = 5 + totalTextHeight;

            context.fill(panelX, responsePanelY, panelX + panelWidth, responsePanelY + responsePanelHeight, 0xE0000000);
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
            int panelWidth = 420;
            int panelHeight = 240;
            int panelX = (this.width - panelWidth) / 2;
            int panelY = (this.height - panelHeight) / 2;
            int responsePanelY = panelY + panelHeight + 5;
            int currentTextY = responsePanelY + 4;

            for (Text line : responseLines) {
                if (mouseY >= currentTextY && mouseY < currentTextY + this.textRenderer.fontHeight) {
                    if (this.handleTextClick(line.getStyle())) return true;
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