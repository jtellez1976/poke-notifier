package com.zehro_mc.pokenotifier;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StatusUpdatePayload(
        String name,
        int level,
        String rarityCategoryName,
        UpdateType updateType
) implements CustomPayload {

    public enum UpdateType {
        DESPAWNED,
        CAPTURED
    }

    public static final CustomPayload.Id<StatusUpdatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "status_update_payload"));

    public static final PacketCodec<PacketByteBuf, StatusUpdatePayload> CODEC = PacketCodec.of(
            StatusUpdatePayload::write,
            StatusUpdatePayload::new
    );

    public StatusUpdatePayload(PacketByteBuf buf) {
        this(buf.readString(), buf.readInt(), buf.readString(), buf.readEnumConstant(UpdateType.class));
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(name);
        buf.writeInt(level);
        buf.writeString(rarityCategoryName);
        buf.writeEnumConstant(updateType);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}