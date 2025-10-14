/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * An S2C payload to update the status of a tracked Pokémon (e.g., captured or despawned).
 */
public record StatusUpdatePayload(
        String uuid,
        String name,
        String rarityCategoryName,
        UpdateType updateType,
        @Nullable String playerName
) implements CustomPayload {

    public enum UpdateType {
        CAPTURED,
        DESPAWNED
    }

    public static final CustomPayload.Id<StatusUpdatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "status_update_payload"));

    public static final PacketCodec<PacketByteBuf, StatusUpdatePayload> CODEC = PacketCodec.of(
            StatusUpdatePayload::write,
            StatusUpdatePayload::new
    );

    public StatusUpdatePayload(PacketByteBuf buf) {
        this(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readEnumConstant(UpdateType.class),
                buf.readOptional(PacketByteBuf::readString).orElse(null)
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(uuid);
        buf.writeString(name);
        buf.writeString(rarityCategoryName);
        buf.writeEnumConstant(updateType);
        buf.writeOptional(Optional.ofNullable(playerName), PacketByteBuf::writeString);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}