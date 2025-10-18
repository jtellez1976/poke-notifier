package com.zehro_mc.pokenotifier.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Synchronizes Pokémon status updates between server and client.
 * Compatible with Fabric Loader 0.17.x (Minecraft 1.21.1)
 */
public record StatusUpdatePayload(
        String uuid,
        String name,
        String rarity,
        UpdateType updateType,
        String playerName
) implements CustomPayload {

    public static final Id<StatusUpdatePayload> ID =
            new Id<>(Identifier.of("poke-notifier", "status_update_payload"));

    public static final PacketCodec<RegistryByteBuf, StatusUpdatePayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeString(payload.uuid);
                        buf.writeString(payload.name);
                        buf.writeString(payload.rarity);
                        buf.writeEnumConstant(payload.updateType);
                        buf.writeNullable(payload.playerName, (b, s) -> b.writeString(s));
                    },
                    buf -> new StatusUpdatePayload(
                            buf.readString(),
                            buf.readString(),
                            buf.readString(),
                            buf.readEnumConstant(UpdateType.class),
                            buf.readNullable((b) -> b.readString())
                    )
            );


    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /** Represents status update types for Pokémon lifecycle events. */
    public enum UpdateType {
        SPAWNED,
        DESPAWNED,
        CAPTURED
    }
}
