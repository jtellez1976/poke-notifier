package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CurrentGenPayload(String generationName) implements CustomPayload {
    public static final Id<CurrentGenPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "current_gen_payload"));
    public static final PacketCodec<RegistryByteBuf, CurrentGenPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.generationName),
            buf -> new CurrentGenPayload(buf.readString())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}