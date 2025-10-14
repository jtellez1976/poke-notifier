/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.component;

import com.mojang.serialization.Codec;
import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

/**
 * Registers all custom data components used by the mod, such as for storing trophy ownership.
 */
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