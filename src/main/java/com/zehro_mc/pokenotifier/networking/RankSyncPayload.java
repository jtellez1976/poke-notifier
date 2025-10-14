/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * An S2C payload to synchronize player ranks (as integers) to all clients.
 */
public record RankSyncPayload(HashMap<UUID, Integer> ranks) implements CustomPayload {
    public static final Id<RankSyncPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "rank_sync"));

    // Manually define the UUID codec to prevent potential compilation issues.
    private static final PacketCodec<ByteBuf, UUID> UUID_CODEC = PacketCodec.of(
            (uuid, buf) -> buf.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits()),
            buf -> new UUID(buf.readLong(), buf.readLong())
    );

    public static final PacketCodec<ByteBuf, RankSyncPayload> CODEC = PacketCodec.of(
            (value, buf) -> PacketCodecs.map(HashMap::new, UUID_CODEC, PacketCodecs.VAR_INT).encode(buf, value.ranks()),
            buf -> new RankSyncPayload(PacketCodecs.map(HashMap::new, UUID_CODEC, PacketCodecs.VAR_INT).decode(buf))
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}