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

public record EventConfigPayload(String eventType, String configKey, String value) implements CustomPayload {
    public static final CustomPayload.Id<EventConfigPayload> ID = new CustomPayload.Id<>(Identifier.of("poke-notifier", "event_config"));
    public static final PacketCodec<RegistryByteBuf, EventConfigPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, EventConfigPayload::eventType,
        PacketCodecs.STRING, EventConfigPayload::configKey,
        PacketCodecs.STRING, EventConfigPayload::value,
        EventConfigPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}