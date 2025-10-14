/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.client.compat;

import com.cobblemon.mod.common.CobblemonItems;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.advancement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.Optional;

/**
 * Compatibility handler for displaying notifications using the AdvancementPlaques API.
 * This class should only be called if the AdvancementPlaques mod is confirmed to be loaded.
 */
public class AdvancementPlaquesCompat {

    public static void showPlaque(Text title, boolean isActivation) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Use the GOAL frame for a more prominent sound, and TASK for a subtler one.
        AdvancementFrame frameType = isActivation ? AdvancementFrame.GOAL : AdvancementFrame.TASK;

        // Create a dummy DisplayInfo. The key is to pass our text as the TITLE and not specify a background.
        Optional<AdvancementDisplay> displayInfo = Optional.of(new AdvancementDisplay(
                new ItemStack(CobblemonItems.POKE_BALL),
                title,
                Text.empty(),
                Optional.empty(), // An empty background prevents the "Advancement Made!" title from showing.
                frameType,
                true,  // showToast
                false, // announceToChat
                false  // hidden
        ));

        Advancement dummyAdvancement = new Advancement(Optional.empty(), displayInfo, AdvancementRewards.NONE, Collections.emptyMap(), AdvancementRequirements.EMPTY, false);
        AdvancementEntry dummyEntry = new AdvancementEntry(Identifier.of(PokeNotifier.MOD_ID, "dummy_advancement"), dummyAdvancement);

        // Create the toast and add it to the manager. AdvancementPlaques will intercept it and display the plaque.
        client.getToastManager().add(new AdvancementToast(dummyEntry));
    }
}