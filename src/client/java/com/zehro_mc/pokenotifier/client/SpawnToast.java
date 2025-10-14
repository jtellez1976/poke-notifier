/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A custom Toast implementation for displaying spawn notifications.
 * This class seems to be unused in favor of the custom NotificationHUD.
 */
public class SpawnToast implements Toast {
    /** The standard texture sheet for all toasts in Minecraft. */
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
        // Draw the standard toast background.
        context.drawTexture(TOASTS_TEXTURE, 0, 0, 0, 0, this.getWidth(), this.getHeight());

        // Draw the text.
        context.drawText(manager.getClient().textRenderer, this.title, 30, 7, 0xFFFFD700, true); // Gold title with shadow
        context.drawText(manager.getClient().textRenderer, this.description, 30, 18, 0xFFFFFFFF, true); // White description with shadow

        // Draw the mod's icon.
        context.drawTexture(ICON_TEXTURE, 8, 8, 0, 0, 16, 16, 16, 16);

        return startTime >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }
}