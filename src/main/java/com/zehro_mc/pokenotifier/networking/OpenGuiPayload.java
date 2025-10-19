/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * An S2C payload sent from the server to a client to command it to open the
 * configuration GUI.
 */
public record OpenGuiPayload() implements CustomPayload {
    public static final Id<OpenGuiPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "open_gui_payload"));

    public static final PacketCodec<RegistryByteBuf, OpenGuiPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {}, // No data to write
            buf -> new OpenGuiPayload() // No data to read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}