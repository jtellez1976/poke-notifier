package com.zehro_mc.pokenotifier;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record WaypointPayload(
        String name,
        String initials,
        BlockPos pos,
        int color,
        int level,
        Identifier biomeId,
        String rarityCategoryName,
        double distance
) implements CustomPayload {

    public static final CustomPayload.Id<WaypointPayload> ID = new CustomPayload.Id<>(PokeNotifier.WAYPOINT_CHANNEL_ID);

    public static final PacketCodec<PacketByteBuf, WaypointPayload> CODEC = PacketCodec.of(
            WaypointPayload::write,
            WaypointPayload::new
    );

    public WaypointPayload(PacketByteBuf buf) {
        this(buf.readString(), buf.readString(), buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readIdentifier(), buf.readString(), buf.readDouble());
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(name);
        buf.writeString(initials);
        buf.writeBlockPos(pos);
        buf.writeInt(color);
        buf.writeInt(level);
        buf.writeIdentifier(biomeId);
        buf.writeString(rarityCategoryName);
        buf.writeDouble(distance);
    }

    @Override
    public Id<WaypointPayload> getId() {
        return ID;
    }
}