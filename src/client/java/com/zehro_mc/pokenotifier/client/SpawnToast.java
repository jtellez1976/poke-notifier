package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SpawnToast implements Toast {
    // Manually define the Identifier for the standard toast texture sheet.
    // This is the most robust way to reference it across different Minecraft versions.
    private static final Identifier TOASTS_TEXTURE = Identifier.of("minecraft", "textures/gui/toasts.png");

    private static final Identifier ICON_TEXTURE = Identifier.of(PokeNotifier.MOD_ID, "icon.png");
    private final Text title;
    private final Text description;

    public SpawnToast(Text title, Text description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
        // Draw the toast background using the standard Minecraft texture sheet (toasts.png)
        // This is the most robust and compatible method.
        context.drawTexture(TOASTS_TEXTURE, 0, 0, 0, 0, this.getWidth(), this.getHeight());

        // Dibuja el texto
        context.drawText(manager.getClient().textRenderer, this.title, 30, 7, 0xFFFFD700, true); // Gold title with shadow
        context.drawText(manager.getClient().textRenderer, this.description, 30, 18, 0xFFFFFFFF, true); // White description with shadow

        // Dibuja el icono del mod. Asegúrate de que 'icon.png' esté en 'resources/assets/poke-notifier/icon.png'
        context.drawTexture(ICON_TEXTURE, 8, 8, 0, 0, 16, 16, 16, 16);

        // El Toast permanece visible por 5 segundos (5000 ms)
        return startTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}