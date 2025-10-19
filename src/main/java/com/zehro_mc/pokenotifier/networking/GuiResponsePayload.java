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
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record GuiResponsePayload(List<Text> lines) implements CustomPayload {
    public static final Id<GuiResponsePayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "gui_response_payload"));

    public static final PacketCodec<RegistryByteBuf, GuiResponsePayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                buf.writeInt(payload.lines.size());
                for (Text line : payload.lines) {
                    TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, line);
                }
            },
            buf -> {
                int size = buf.readInt();
                List<Text> lines = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    lines.add(TextCodecs.REGISTRY_PACKET_CODEC.decode(buf));
                }
                return new GuiResponsePayload(lines);
            });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}