package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.PokeNotifier;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class CatchEmAllHUD {

    // Ya no necesitamos las texturas personalizadas, las eliminamos para mantener el código limpio.
    private static final Identifier BUTTON_TEXTURE = Identifier.of("minecraft", "widget/button");

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Si el modo no está activo o no hay información, no dibujamos nada.
        // CORRECCIÓN: Añadimos una comprobación para asegurarnos de que la variable no es nula o vacía.
        if (PokeNotifierClient.currentCatchEmAllGeneration == null || "none".equals(PokeNotifierClient.currentCatchEmAllGeneration) || PokeNotifierClient.catchTotalCount == 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // --- CONSTANTES DE DISEÑO ---
        int margin = 5;
        int boxPadding = 4;

        // Formateamos el texto del progreso
        String genName = PokeNotifierClient.currentCatchEmAllGeneration.substring(0, 1).toUpperCase() + PokeNotifierClient.currentCatchEmAllGeneration.substring(1);
        Text progressText = Text.literal(genName + ": ").formatted(Formatting.YELLOW) // Cambiamos el color a amarillo para mayor visibilidad
                .append(Text.literal(PokeNotifierClient.catchCaughtCount + "/" + PokeNotifierClient.catchTotalCount).formatted(Formatting.GREEN));

        // --- CÁLCULOS DE POSICIÓN Y TAMAÑO ---
        int textWidth = client.textRenderer.getWidth(progressText);
        int boxWidth = textWidth + (boxPadding * 2);
        int boxHeight = client.textRenderer.fontHeight + (boxPadding * 2);

        // --- POSICIONAMIENTO DINÁMICO DEL CONJUNTO ---
        ChatHud chatHud = client.inGameHud.getChatHud();
        int chatHeight = chatHud.isChatFocused() ? chatHud.getHeight() : 0;

        // La posición de la caja del score ahora es el ancla principal.
        int boxX = margin;
        int boxY = screenHeight - chatHeight - boxHeight - margin;

        // Habilitamos el blending para la transparencia
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // --- DIBUJADO DE ELEMENTOS (NUEVA LÓGICA) ---
        // 1. Dibujar el fondo del score usando el estilo de un botón de Minecraft.
        // El método drawGuiTexture se encarga de escalar la textura correctamente.
        context.drawGuiTexture(BUTTON_TEXTURE, boxX, boxY, boxWidth, boxHeight);

        // --- DIBUJADO DE TEXTO CON ESCALADO ---
        // 2. Dibujar el texto del score sobre el "botón", pero más pequeño.
        float scale = 0.8f; // Reducimos el texto a un 80% de su tamaño.
        int scaledTextWidth = (int) (textWidth * scale);

        // Calculamos la nueva posición para que el texto escalado quede centrado.
        int textX = boxX + (boxWidth - scaledTextWidth) / 2;
        int textY = boxY + (boxHeight - (int)(client.textRenderer.fontHeight * scale)) / 2;

        context.getMatrices().push(); // Guardamos el estado actual de la matriz
        context.getMatrices().translate(textX, textY, 0); // Nos movemos a la posición del texto
        context.getMatrices().scale(scale, scale, 1.0f); // Aplicamos la escala

        // Dibujamos el texto en la coordenada (0,0) porque ya nos hemos trasladado a la posición correcta.
        context.drawTextWithShadow(client.textRenderer, progressText, 0, 0, 0xFFFFFF);

        context.getMatrices().pop(); // Restauramos la matriz para no afectar a otros elementos del HUD

        RenderSystem.disableBlend();
    }
}