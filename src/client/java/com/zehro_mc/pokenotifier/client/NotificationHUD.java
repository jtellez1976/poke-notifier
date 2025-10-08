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
    private static long displayUntil = -1;

    // Apuntamos a nuestra textura personalizada de Poké Ball.
    private static final Identifier POKE_BALL_ICON = Identifier.of("poke-notifier", "textures/gui/pokeball-icon.png");

    public static void show(Text title, Text description) {
        NotificationHUD.title = title;
        NotificationHUD.description = description;
        NotificationHUD.displayUntil = System.currentTimeMillis() + 5000L; // Mostrar por 5 segundos
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (System.currentTimeMillis() > displayUntil || title == null) {
            return; // No mostrar si el tiempo ha expirado o no hay nada que mostrar
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();

        // Trunca el texto si es demasiado largo y luego lo dibuja.
        // Usamos un ancho máximo generoso, ya que no hay fondo.
        int maxTextWidth = 180;

        StringVisitable truncatedTitle = client.textRenderer.trimToWidth(title, maxTextWidth);
        Text drawableTitle = Text.literal(truncatedTitle.getString());

        StringVisitable truncatedDescription = client.textRenderer.trimToWidth(description, maxTextWidth);
        Text drawableDescription = Text.literal(truncatedDescription.getString());

        // Calculamos el ancho total para centrar el conjunto (icono + texto)
        int iconWidth = 26; // Ancho del icono ajustado
        int iconHeight = 24; // Alto del icono
        int padding = 5;
        int textWidth = Math.max(client.textRenderer.getWidth(drawableTitle), client.textRenderer.getWidth(drawableDescription));
        int totalWidth = iconWidth + padding + textWidth;

        int x = (screenWidth - totalWidth) / 2;
        int y = 10; // Cerca de la parte superior

        // Habilitamos el blending para que la transparencia del PNG funcione correctamente
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Dibuja el icono
        context.drawTexture(POKE_BALL_ICON, x, y, 0, 0, iconWidth, iconHeight, iconWidth, iconHeight);

        // Dibuja el texto
        context.drawTextWithShadow(client.textRenderer, drawableTitle, x + iconWidth + padding, y + 4, 0xFFFFD700);
        context.drawTextWithShadow(client.textRenderer, drawableDescription, x + iconWidth + padding, y + 15, 0xFFFFFFFF);

        RenderSystem.disableBlend();
    }
}