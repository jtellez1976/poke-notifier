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
 * Handshake payload to detect if the mod is installed on the server.
 * Client sends this on join, server responds if mod is present.
 */
public record ModHandshakePayload(boolean isRequest) implements CustomPayload {
    public static final Id<ModHandshakePayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "mod_handshake"));
    public static final PacketCodec<RegistryByteBuf, ModHandshakePayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBoolean(value.isRequest),
            buf -> new ModHandshakePayload(buf.readBoolean())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
