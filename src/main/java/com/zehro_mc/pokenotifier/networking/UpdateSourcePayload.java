/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A C2S payload for a player to set their preferred update checker source.
 */
public record UpdateSourcePayload(String source) implements CustomPayload {
    public static final CustomPayload.Id<UpdateSourcePayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "update_source_payload"));

    public static final PacketCodec<PacketByteBuf, UpdateSourcePayload> CODEC = PacketCodec.of(
            UpdateSourcePayload::write,
            UpdateSourcePayload::new
    );

    public UpdateSourcePayload(PacketByteBuf buf) {
        this(buf.readString());
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(source);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}