package com.zehro_mc.pokenotifier.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PokeNotifierServerPackets {
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CatchemallUpdatePayload.ID, CatchemallUpdatePayload.CODEC);
    }
}