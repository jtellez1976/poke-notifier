package com.zehro_mc.pokenotifier.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NotificationHUD {

    private static Text title;
    private static Text description;
    private static Identifier icon;
    private static long displayUntil = -1L;

    // Icono de fallback (Poké Ball) si el sprite del Pokémon no se encuentra.
    private static final Identifier FALLBACK_ICON = Identifier.of("poke-notifier", "textures/gui/pokeball-icon.png");

    public static void show(Text title, Text description, Identifier icon) {
        NotificationHUD.title = title;
        NotificationHUD.description = description;
        NotificationHUD.icon = icon;
        NotificationHUD.displayUntil = System.currentTimeMillis() + 5000L; // Mostrar por 5 segundos
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (System.currentTimeMillis() > displayUntil || title == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int y = 10; // Posición vertical fija cerca de la parte superior

        // Habilitamos el blending para que la transparencia del PNG funcione correctamente
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Comprobamos si el sprite personalizado del Pokémon existe.
        boolean useCustomSprite = client.getResourceManager().getResource(icon).isPresent();

        if (useCustomSprite) {
            // --- CASO 1: El sprite del Pokémon SÍ existe ---
            int iconSize = 64; // Usamos el tamaño grande
            int padding = 0;
            int textWidth = client.textRenderer.getWidth(title);
            int totalWidth = iconSize + padding + textWidth;
            int x = (screenWidth - totalWidth) / 2;

            // Dibuja el sprite del Pokémon
            context.drawTexture(icon, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Dibuja el texto centrado verticalmente con el icono grande
            int textY = y + (iconSize / 2) - (client.textRenderer.fontHeight / 2);
            context.drawTextWithShadow(client.textRenderer, title, x + iconSize + padding, textY, 0xFFFFFF);

        } else {
            // --- CASO 2: El sprite del Pokémon NO existe (usamos el fallback) ---
            int iconSize = 32; // Usamos un tamaño más pequeño y apropiado para la Poké Ball
            int padding = 4;
            int textWidth = client.textRenderer.getWidth(title);
            int totalWidth = iconSize + padding + textWidth;
            int x = (screenWidth - totalWidth) / 2;

            // Dibuja la Poké Ball de fallback
            context.drawTexture(FALLBACK_ICON, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

            // Dibuja el texto centrado verticalmente con el icono pequeño
            int textY = y + (iconSize / 2) - (client.textRenderer.fontHeight / 2);
            context.drawTextWithShadow(client.textRenderer, title, x + iconSize + padding, textY, 0xFFFFFF);
        }

        RenderSystem.disableBlend();
    }
}