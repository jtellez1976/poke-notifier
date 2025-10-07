package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record StatusUpdatePayload(
        String uuid,
        String name,
        String rarityCategoryName,
        UpdateType updateType,
        @Nullable String playerName
) implements CustomPayload {

    public enum UpdateType {
        CAPTURED,
        DESPAWNED
    }

    public static final CustomPayload.Id<StatusUpdatePayload> ID = new CustomPayload.Id<>(PokeNotifier.STATUS_UPDATE_CHANNEL_ID);

    public static final PacketCodec<PacketByteBuf, StatusUpdatePayload> CODEC = PacketCodec.of(
            StatusUpdatePayload::write,
            StatusUpdatePayload::new
    );

    public StatusUpdatePayload(PacketByteBuf buf) {
        this(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readEnumConstant(UpdateType.class),
                buf.readOptional(PacketByteBuf::readString).orElse(null)
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(uuid);
        buf.writeString(name);
        buf.writeString(rarityCategoryName);
        buf.writeEnumConstant(updateType);
        buf.writeOptional(Optional.ofNullable(playerName), PacketByteBuf::writeString);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}