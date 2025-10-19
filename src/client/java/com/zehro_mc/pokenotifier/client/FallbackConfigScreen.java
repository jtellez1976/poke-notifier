package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A simple fallback screen to show if the main config screen fails to load.
 */
public class FallbackConfigScreen extends Screen {
    private final Screen parent;
    private final String errorMessage;

    public FallbackConfigScreen(Screen parent, String errorMessage) {
        super(Text.literal("Poke Notifier - Error"));
        this.parent = parent;
        this.errorMessage = errorMessage;
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            if (this.client != null) this.client.setScreen(parent);
        }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 30, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Configuration screen could not be loaded."), this.width / 2, 60, 0xFF5555);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Error: " + errorMessage), this.width / 2, 75, 0xFF5555);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Check the logs for more details."), this.width / 2, 90, 0xFF5555);
    }
}