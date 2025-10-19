package com.zehro_mc.pokenotifier.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A very simple, vanilla Minecraft screen for testing purposes.
 */
public class SimpleGuiScreen extends Screen {
    // --- FIX: Use a no-argument constructor to avoid context conflicts ---
    public SimpleGuiScreen() {
        super(Text.literal("Poke Notifier - Test GUI"));
    }

    @Override
    protected void init() {
        super.init();

        // Add a simple "Back" button to close the screen
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            if (this.client != null) {
                this.client.setScreen(null); // Passing null returns to the game screen
            }
        }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 40, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("If you can see this, the command works!"), this.width / 2, 60, 0xAAAAAA);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null); // Ensure closing with ESC also works
        }
    }
}