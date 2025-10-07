package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record WaypointPayload(
        String uuid,
        String name,
        BlockPos pos,
        int color,
        String rarityCategoryName,
        String level,
        double distance,
        Identifier biomeId
) implements CustomPayload {
    public static final CustomPayload.Id<WaypointPayload> ID = new CustomPayload.Id<>(PokeNotifier.WAYPOINT_CHANNEL_ID);

    public static final PacketCodec<PacketByteBuf, WaypointPayload> CODEC = PacketCodec.of(
            WaypointPayload::write,
            WaypointPayload::new
    );

    public WaypointPayload(PacketByteBuf buf) {
        this(
                buf .readString(),
                buf.readString(),
                buf.readBlockPos(),
                buf.readInt(),
                buf.readString(),
                buf.readString(),
                buf.readDouble(),
                buf.readIdentifier()
        );
    }

    private void write(PacketByteBuf buf) {
        buf.writeString(uuid);
        buf.writeString(name);
        buf.writeBlockPos(pos);
        buf.writeInt(color);
        buf.writeString(rarityCategoryName);
        buf.writeString(level);
        buf.writeDouble(distance);
        buf.writeIdentifier(biomeId);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}