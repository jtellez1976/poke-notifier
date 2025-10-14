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
import net.minecraft.util.math.BlockPos;

/**
 * An S2C payload containing all necessary information to notify a client about a rare spawn.
 */
public record WaypointPayload(
        String uuid,
        String name,
        BlockPos pos,
        int color,
        String status,
        String rarityCategoryName,
        String level,
        double distance,
        Identifier biomeId,
        Identifier spriteIdentifier
) implements CustomPayload {
    public static final CustomPayload.Id<WaypointPayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "waypoint_payload"));

    public static final PacketCodec<PacketByteBuf, WaypointPayload> CODEC = PacketCodec.of(
            WaypointPayload::write,
            WaypointPayload::new
    );

    public WaypointPayload(PacketByteBuf buf) {
        this(
                buf .readString(),
                buf.readString(),
                buf.readBlockPos(),
                buf.readInt(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readDouble(),
                buf.readIdentifier(),
                buf.readIdentifier()
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(uuid);
        buf.writeString(name);
        buf.writeBlockPos(pos);
        buf.writeInt(color);
        buf.writeString(status);
        buf.writeString(rarityCategoryName);
        buf.writeString(level);
        buf.writeDouble(distance);
        buf.writeIdentifier(biomeId);
        buf.writeIdentifier(spriteIdentifier);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}