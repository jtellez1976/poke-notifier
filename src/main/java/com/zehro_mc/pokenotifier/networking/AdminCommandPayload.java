package com.zehro_mc.pokenotifier.networking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload for admin commands sent from client to server.
 * Handles all administrative functions through networking instead of chat commands.
 */
public record AdminCommandPayload(Action action, String parameter) implements CustomPayload {
    public static final CustomPayload.Id<AdminCommandPayload> ID = new CustomPayload.Id<>(Identifier.of("poke-notifier", "admin_command"));
    
    public static final Codec<AdminCommandPayload> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Action.CODEC.fieldOf("action").forGetter(AdminCommandPayload::action),
                    Codec.STRING.fieldOf("parameter").forGetter(AdminCommandPayload::parameter)
            ).apply(instance, AdminCommandPayload::new)
    );
    
    public static final PacketCodec<RegistryByteBuf, AdminCommandPayload> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.codec(Action.CODEC), AdminCommandPayload::action,
            PacketCodecs.STRING, AdminCommandPayload::parameter,
            AdminCommandPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum Action {
        // Server Control
        TOGGLE_DEBUG_MODE,
        TOGGLE_TEST_MODE,
        TOGGLE_BOUNTY_SYSTEM,
        SERVER_STATUS,
        RELOAD_CONFIG,
        RESET_CONFIG,
        
        // Event Management
        START_SWARM,
        START_GLOBAL_HUNT,
        CANCEL_GLOBAL_HUNT,
        TOGGLE_GLOBAL_HUNT_SYSTEM,
        GLOBAL_HUNT_STATUS,
        
        // Player Data
        AUTOCOMPLETE_PLAYER,
        ROLLBACK_PLAYER,
        
        // Testing
        SPAWN_POKEMON,
        
        // Info
        HELP,
        VERSION,
        STATUS;

        public static final Codec<Action> CODEC = Codec.STRING.xmap(Action::valueOf, Action::name);
    }
}