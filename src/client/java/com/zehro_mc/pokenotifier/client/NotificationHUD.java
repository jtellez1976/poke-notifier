package com.zehro_mc.pokenotifier.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StringVisitable;
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

        // Usamos un ancho máximo generoso, ya que no hay fondo.
        int maxTextWidth = 220;

        StringVisitable truncatedTitle = client.textRenderer.trimToWidth(title, maxTextWidth);
        Text drawableTitle = Text.literal(truncatedTitle.getString());

        StringVisitable truncatedDescription = client.textRenderer.trimToWidth(description, maxTextWidth);
        Text drawableDescription = Text.literal(truncatedDescription.getString());

        // Calculamos el ancho total para centrar el conjunto (icono + texto)
        int iconSize = 64; // Un tamaño grande y prominente para el icono
        int padding = 0; // Eliminamos el espaciado para que el texto esté pegado al icono
        int textWidth = Math.max(client.textRenderer.getWidth(drawableTitle), client.textRenderer.getWidth(drawableDescription));
        int totalWidth = iconSize + padding + textWidth;

        int x = (screenWidth - totalWidth) / 2;
        int y = 10; // Cerca de la parte superior

        // Habilitamos el blending para que la transparencia del PNG funcione correctamente
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Lógica de Fallback
        Identifier iconToDraw = FALLBACK_ICON;
        if (client.getResourceManager().getResource(icon).isPresent()) {
            iconToDraw = icon;
        }
        context.drawTexture(iconToDraw, x, y, 0, 0, iconSize, iconSize, iconSize, iconSize);

        // Ajustamos la posición vertical del texto para que quede centrado con el nuevo tamaño del icono
        context.drawTextWithShadow(client.textRenderer, drawableTitle, x + iconSize + padding, y + 22, 0xFFFFD700);
        context.drawTextWithShadow(client.textRenderer, drawableDescription, x + iconSize + padding, y + 33, 0xFFFFFFFF);

        RenderSystem.disableBlend();
    }
}