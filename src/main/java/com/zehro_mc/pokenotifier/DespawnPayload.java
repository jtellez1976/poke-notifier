package com.zehro_mc.pokenotifier;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DespawnPayload(
        String name,
        int level,
        String rarityCategoryName
) implements CustomPayload {

    public static final CustomPayload.Id<DespawnPayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "despawn_payload"));

    public static final PacketCodec<PacketByteBuf, DespawnPayload> CODEC = PacketCodec.of(
            DespawnPayload::write,
            DespawnPayload::new
    );

    public DespawnPayload(PacketByteBuf buf) {
        this(buf.readString(), buf.readInt(), buf.readString());
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(name);
        buf.writeInt(level);
        buf.writeString(rarityCategoryName);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
