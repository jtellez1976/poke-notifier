package com.zehro_mc.pokenotifier.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ActivationFeedbackHUD {

    private static Text message;
    private static Formatting color;
    private static long displayUntil = -1L;

    public static void show(Text message, boolean isActivation) {
        ActivationFeedbackHUD.message = message;
        ActivationFeedbackHUD.color = isActivation ? Formatting.GREEN : Formatting.RED;
        ActivationFeedbackHUD.displayUntil = System.currentTimeMillis() + 3000L; // Mostrar por 3 segundos
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (System.currentTimeMillis() > displayUntil || message == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // --- Lógica de Animación (Fade Out) ---
        long timeRemaining = displayUntil - System.currentTimeMillis();
        float alpha = 1.0f;
        if (timeRemaining < 500) { // En el último medio segundo, se desvanece
            alpha = timeRemaining / 500.0f;
        }
        int alphaInt = (int) (alpha * 255);

        // --- Dibujado del Texto ---
        int textWidth = client.textRenderer.getWidth(message);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight / 4; // Posicionado en el cuarto superior de la pantalla

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.drawTextWithShadow(client.textRenderer, message, x, y, color.getColorValue() | (alphaInt << 24));

        RenderSystem.disableBlend();
    }
}