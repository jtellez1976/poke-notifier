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

import java.util.Map;

public record EventConfigSyncPayload(String eventType, Map<String, String> configs) implements CustomPayload {
    public static final CustomPayload.Id<EventConfigSyncPayload> ID = new CustomPayload.Id<>(Identifier.of("poke-notifier", "event_config_sync"));
    public static final PacketCodec<RegistryByteBuf, EventConfigSyncPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, EventConfigSyncPayload::eventType,
        PacketCodecs.map(java.util.HashMap::new, PacketCodecs.STRING, PacketCodecs.STRING), EventConfigSyncPayload::configs,
        EventConfigSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}