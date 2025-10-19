package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A C2S payload sent from the client to the server to update the update checker source.
 */
public record UpdateSourcePayload(String source) implements CustomPayload {
    public static final Id<UpdateSourcePayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "update_source_payload"));

    // FIX: Use PacketCodec.of() for broader compatibility.
    public static final PacketCodec<RegistryByteBuf, UpdateSourcePayload> CODEC = PacketCodec.of(UpdateSourcePayload::write, UpdateSourcePayload::new);

    public UpdateSourcePayload(RegistryByteBuf buf) {
        this(buf.readString());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeString(this.source);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}