package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ServerDebugStatusPayload(boolean debugModeEnabled) implements CustomPayload {

    public static final CustomPayload.Id<ServerDebugStatusPayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "server_debug_status_payload"));

    public static final PacketCodec<PacketByteBuf, ServerDebugStatusPayload> CODEC = PacketCodec.of(
            ServerDebugStatusPayload::write,
            ServerDebugStatusPayload::new
    );

    public ServerDebugStatusPayload(PacketByteBuf buf) {
        this(buf.readBoolean());
    }

    private void write(PacketByteBuf buf) {
        buf.writeBoolean(debugModeEnabled);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}