package com.zehro_mc.pokenotifier.networking;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PokeNotifierPayloads {

    public static void register() {
        // These payloads are sent from the server to the client.
        // They must be registered on both sides.
        PayloadTypeRegistry.playS2C().register(StatusUpdatePayload.ID, StatusUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WaypointPayload.ID, WaypointPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CatchProgressPayload.ID, CatchProgressPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModeStatusPayload.ID, ModeStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GlobalAnnouncementPayload.ID, GlobalAnnouncementPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RankSyncPayload.ID, RankSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDebugStatusPayload.ID, ServerDebugStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenGuiPayload.ID, OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GuiResponsePayload.ID, GuiResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AdminStatusPayload.ID, AdminStatusPayload.CODEC);

        // These payloads are sent from the client to the server.
        // They must also be registered on both sides.
        PayloadTypeRegistry.playC2S().register(CustomListUpdatePayload.ID, CustomListUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CatchemallUpdatePayload.ID, CatchemallUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateSourcePayload.ID, UpdateSourcePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminCommandPayload.ID, AdminCommandPayload.PACKET_CODEC);

        PokeNotifier.LOGGER.info("[Networking] All Poke Notifier payloads registered successfully.");
    }
}
