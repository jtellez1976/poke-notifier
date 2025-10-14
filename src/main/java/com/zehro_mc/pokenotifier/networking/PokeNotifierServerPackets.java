/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * @deprecated This class appears to be unused. Packet registration is handled directly in the main mod initializer.
 */
@Deprecated
public class PokeNotifierServerPackets {
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CatchemallUpdatePayload.ID, CatchemallUpdatePayload.CODEC);
    }
}