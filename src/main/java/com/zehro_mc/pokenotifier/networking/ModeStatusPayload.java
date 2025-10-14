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
 * An S2C payload to show a feedback message (e.g., via AdvancementPlaques) on the client.
 */
public record ModeStatusPayload(String message, boolean isActivation) implements CustomPayload {
    public static final Id<ModeStatusPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "mode_status_payload"));
    public static final PacketCodec<RegistryByteBuf, ModeStatusPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.message).writeBoolean(value.isActivation),
            buf -> new ModeStatusPayload(buf.readString(), buf.readBoolean())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}