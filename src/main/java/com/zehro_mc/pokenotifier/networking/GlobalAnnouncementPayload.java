package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GlobalAnnouncementPayload(String playerName, String regionName) implements CustomPayload {
    public static final Id<GlobalAnnouncementPayload> ID = new Id<>(Identifier.of(PokeNotifier.MOD_ID, "global_announcement_payload"));
    public static final PacketCodec<RegistryByteBuf, GlobalAnnouncementPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeString(value.playerName).writeString(value.regionName),
            buf -> new GlobalAnnouncementPayload(buf.readString(), buf.readString())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}