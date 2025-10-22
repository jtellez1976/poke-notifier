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

public record SwarmStatusPayload(
    boolean hasActiveSwarm,
    String pokemonName,
    String location,
    String biome,
    int remainingMinutes,
    int remainingEntities
) implements CustomPayload {
    
    public static final CustomPayload.Id<SwarmStatusPayload> ID = new CustomPayload.Id<>(Identifier.of("poke-notifier", "swarm_status"));
    
    public static final PacketCodec<RegistryByteBuf, SwarmStatusPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.BOOL, SwarmStatusPayload::hasActiveSwarm,
        PacketCodecs.STRING, SwarmStatusPayload::pokemonName,
        PacketCodecs.STRING, SwarmStatusPayload::location,
        PacketCodecs.STRING, SwarmStatusPayload::biome,
        PacketCodecs.VAR_INT, SwarmStatusPayload::remainingMinutes,
        PacketCodecs.VAR_INT, SwarmStatusPayload::remainingEntities,
        SwarmStatusPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}