/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GlobalHuntPayload(
    String action,
    String pokemon,
    String world,
    int x,
    int y,
    int z,
    boolean isShiny,
    int durationMinutes
) implements CustomPayload {
    
    public static final CustomPayload.Id<GlobalHuntPayload> ID = new CustomPayload.Id<>(Identifier.of("poke-notifier", "global_hunt"));
    
    public static final PacketCodec<RegistryByteBuf, GlobalHuntPayload> CODEC = PacketCodec.of(
        (payload, buf) -> {
            buf.writeString(payload.action());
            buf.writeString(payload.pokemon());
            buf.writeString(payload.world());
            buf.writeVarInt(payload.x());
            buf.writeVarInt(payload.y());
            buf.writeVarInt(payload.z());
            buf.writeBoolean(payload.isShiny());
            buf.writeVarInt(payload.durationMinutes());
        },
        (buf) -> new GlobalHuntPayload(
            buf.readString(),
            buf.readString(),
            buf.readString(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readVarInt()
        )
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    // Action types
    public static final String ACTION_START = "start";
    public static final String ACTION_END = "end";
    public static final String ACTION_CAPTURED = "captured";
    public static final String ACTION_TIMEOUT = "timeout";
}