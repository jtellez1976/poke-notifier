package com.zehro_mc.pokenotifier.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CatchEmAllHUD {

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Si el modo no está activo o no hay información, no dibujamos nada.
        if ("none".equals(PokeNotifierClient.currentCatchEmAllGeneration) || PokeNotifierClient.catchTotalCount == 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // Formateamos el texto del progreso
        String genName = PokeNotifierClient.currentCatchEmAllGeneration.substring(0, 1).toUpperCase() + PokeNotifierClient.currentCatchEmAllGeneration.substring(1);
        Text progressText = Text.literal(genName + ": ").formatted(Formatting.YELLOW) // Cambiamos el color a amarillo para mayor visibilidad
                .append(Text.literal(PokeNotifierClient.catchCaughtCount + "/" + PokeNotifierClient.catchTotalCount).formatted(Formatting.GREEN));

        // --- CORRECCIÓN DE POSICIÓN ---
        // Movemos el HUD a la esquina inferior izquierda.
        int x = 5; // 5 píxeles de margen desde la izquierda
        int y = screenHeight - client.textRenderer.fontHeight - 5;

        // Habilitamos el blending para la transparencia
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.drawTextWithShadow(client.textRenderer, progressText, x, y, 0xFFFFFF);

        RenderSystem.disableBlend();
    }
}