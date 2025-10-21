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

public record AdminStatusPayload(
    boolean isPlayerAdmin,
    boolean isDebugMode,
    boolean isTestMode,
    boolean isBountySystemEnabled,
    boolean isGlobalHuntSystemEnabled,
    boolean hasActiveGlobalHunt,
    String activeGlobalHuntPokemon
) implements CustomPayload {
    public static final Id<AdminStatusPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "admin_status_payload"));

    // FIX: Use PacketCodec.of() for broader compatibility instead of the newer ofMember().
    public static final PacketCodec<RegistryByteBuf, AdminStatusPayload> CODEC = PacketCodec.of(AdminStatusPayload::write, AdminStatusPayload::new);

    public AdminStatusPayload(RegistryByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readString());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeBoolean(isPlayerAdmin);
        buf.writeBoolean(isDebugMode);
        buf.writeBoolean(isTestMode);
        buf.writeBoolean(isBountySystemEnabled);
        buf.writeBoolean(isGlobalHuntSystemEnabled);
        buf.writeBoolean(hasActiveGlobalHunt);
        buf.writeString(activeGlobalHuntPokemon);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}