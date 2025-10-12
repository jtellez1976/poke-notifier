package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.client.compat.AdvancementPlaquesCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.loader.api.FabricLoader;

public class ActivationFeedbackHUD {

    private static Text message;
    private static Formatting color;
    private static long displayUntil = -1L;
    private static boolean isActivation = false;

    // Variable para saber si el mod de placas está cargado. La comprobamos una sola vez.
    private static final boolean plaquesModLoaded = FabricLoader.getInstance().isModLoaded("advancementplaques");

    public static void show(Text message, boolean isActivation) {
        if (plaquesModLoaded) {
            // Si el mod está, llamamos a nuestro handler de compatibilidad.
            AdvancementPlaquesCompat.showPlaque(message, isActivation);
        } else {
            // Si no, usamos nuestro sistema de fallback.
            ActivationFeedbackHUD.message = message;
            ActivationFeedbackHUD.color = isActivation ? Formatting.GREEN : Formatting.RED;
            ActivationFeedbackHUD.displayUntil = System.currentTimeMillis() + 3000L;
            ActivationFeedbackHUD.isActivation = isActivation;
        }
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        // El render solo se ejecuta si el mod de placas NO está cargado.
        if (plaquesModLoaded || System.currentTimeMillis() > displayUntil || message == null) {
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

        // --- DIBUJADO DE TEXTO CON ESCALADO ---
        int textWidth = client.textRenderer.getWidth(message);
        float scale = 1.5f; // Aumentamos el texto a un 150% de su tamaño.
        float scaledTextWidth = textWidth * scale;

        // Calculamos la posición para que el texto escalado quede centrado.
        float x = (screenWidth - scaledTextWidth) / 2;
        int y = screenHeight / 4; // Posicionado en el cuarto superior de la pantalla

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.getMatrices().push(); // Guardamos el estado actual de la matriz
        context.getMatrices().translate(x, y, 0); // Nos movemos a la posición del texto
        context.getMatrices().scale(scale, scale, 1.0f); // Aplicamos la escala

        // Dibujamos el texto en la coordenada (0,0) porque ya nos hemos trasladado a la posición correcta.
        context.drawTextWithShadow(client.textRenderer, message, 0, 0, color.getColorValue() | (alphaInt << 24));

        context.getMatrices().pop(); // Restauramos la matriz para no afectar a otros elementos del HUD

        RenderSystem.disableBlend();
    }
}