package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

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