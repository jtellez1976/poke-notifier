package com.zehro_mc.pokenotifier.component;

import com.mojang.serialization.Codec;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final ComponentType<String> REGION_NAME = register("region_name", builder -> builder.codec(Codec.STRING).packetCodec(PacketCodecs.STRING));
    public static final ComponentType<String> OWNER_NAME = register("owner_name", builder -> builder.codec(Codec.STRING).packetCodec(PacketCodecs.STRING));
    public static final ComponentType<String> OWNER_UUID = register("owner_uuid", builder -> builder.codec(Codec.STRING).packetCodec(PacketCodecs.STRING));

    private static <T> ComponentType<T> register(String path, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(PokeNotifier.MOD_ID, path), builderOperator.apply(ComponentType.builder()).build());
    }

    public static void registerModDataComponents() {
        PokeNotifier.LOGGER.info("Registering ModDataComponents for " + PokeNotifier.MOD_ID);
    }
}