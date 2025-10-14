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
 * A C2S payload for managing a player's custom hunt list.
 */
public record CustomListUpdatePayload(Action action, String pokemonName) implements CustomPayload {

    public enum Action {
        ADD,
        REMOVE,
        LIST,
        CLEAR
    }

    public static final CustomPayload.Id<CustomListUpdatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "custom_list_update_payload"));

    public static final PacketCodec<PacketByteBuf, CustomListUpdatePayload> CODEC = PacketCodec.of(
            CustomListUpdatePayload::write,
            CustomListUpdatePayload::new
    );

    public CustomListUpdatePayload(PacketByteBuf buf) {
        this(buf.readEnumConstant(Action.class), buf.readString());
    }

    private void write(PacketByteBuf buf) {
        buf.writeEnumConstant(action);
        buf.writeString(pokemonName);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}