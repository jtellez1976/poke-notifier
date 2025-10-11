package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CatchemallUpdatePayload(Action action, String generationName) implements CustomPayload {

    public enum Action {
        ENABLE,
        DISABLE,
        LIST
    }

    public static final CustomPayload.Id<CatchemallUpdatePayload> ID = new CustomPayload.Id<>(
            Identifier.of(PokeNotifier.MOD_ID, "catchemall_update_payload"));

    public static final PacketCodec<PacketByteBuf, CatchemallUpdatePayload> CODEC = PacketCodec.of(
            CatchemallUpdatePayload::write,
            CatchemallUpdatePayload::new
    );

    public CatchemallUpdatePayload(PacketByteBuf buf) {
        this(buf.readEnumConstant(Action.class), buf.readString());
    }

    private void write(PacketByteBuf buf) {
        buf.writeEnumConstant(action);
        buf.writeString(generationName);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}