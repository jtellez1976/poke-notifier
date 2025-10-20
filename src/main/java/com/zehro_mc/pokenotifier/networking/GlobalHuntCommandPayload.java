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

public record GlobalHuntCommandPayload(
    String action,
    String parameter
) implements CustomPayload {
    
    public static final CustomPayload.Id<GlobalHuntCommandPayload> ID = new CustomPayload.Id<>(Identifier.of("poke-notifier", "global_hunt_command"));
    
    public static final PacketCodec<RegistryByteBuf, GlobalHuntCommandPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING, GlobalHuntCommandPayload::action,
        PacketCodecs.STRING, GlobalHuntCommandPayload::parameter,
        GlobalHuntCommandPayload::new
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    // Action types for admin commands
    public static final String ACTION_START_MANUAL = "start_manual";
    public static final String ACTION_CANCEL_EVENT = "cancel_event";
    public static final String ACTION_GET_STATUS = "get_status";
    public static final String ACTION_TOGGLE_SYSTEM = "toggle_system";
}