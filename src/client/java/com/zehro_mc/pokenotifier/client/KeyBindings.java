/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Manages keybindings for Poke Notifier.
 */
public class KeyBindings {
    
    public static KeyBinding OPEN_NOTIFIER_KEY;
    
    public static void register() {
        OPEN_NOTIFIER_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.poke-notifier.open_device",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.poke-notifier"
        ));
    }
}
