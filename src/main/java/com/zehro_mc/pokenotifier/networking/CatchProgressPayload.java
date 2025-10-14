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
 * An S2C payload that sends "Catch 'em All" progress to the client for HUD rendering.
 */
public record CatchProgressPayload(String generationName, int caughtCount, int totalCount) implements CustomPayload {
    public static final Id<CatchProgressPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "catch_progress_payload"));
    public static final PacketCodec<RegistryByteBuf, CatchProgressPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.generationName).writeInt(value.caughtCount).writeInt(value.totalCount),
            buf -> new CatchProgressPayload(buf.readString(), buf.readInt(), buf.readInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}