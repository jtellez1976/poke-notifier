package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * A S2C payload sent from the server to the client to sync the current update source.
 */
public record UpdateSourceSyncPayload(String source) implements CustomPayload {
    public static final Id<UpdateSourceSyncPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "update_source_sync_payload"));

    public static final PacketCodec<RegistryByteBuf, UpdateSourceSyncPayload> CODEC = PacketCodec.of(UpdateSourceSyncPayload::write, UpdateSourceSyncPayload::new);

    public UpdateSourceSyncPayload(RegistryByteBuf buf) {
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