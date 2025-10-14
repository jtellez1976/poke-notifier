/*
 * Copyright (C) 2024 ZeHrOx
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zehro_mc.pokenotifier.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.util.math.Vec3d;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * A utility class for handling special visual and sound effects related to player prestige.
 */
public class PrestigeEffects {

    public static void playMasterEffects(ServerPlayerEntity masterPlayer) {
        MinecraftServer server = masterPlayer.getServer();
        if (server == null) return;

        // Announce the Master's arrival to everyone.
        Text championMessage = Text.literal("A Master, ").append(masterPlayer.getDisplayName()).append(Text.literal(", has joined the server!")).formatted(Formatting.GOLD);
        server.getPlayerManager().broadcast(championMessage, false);

        // Play the thunder sound for every player, regardless of their location.
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.playSoundToPlayer(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    public static void launchCelebratoryFireworks(ServerPlayerEntity player, int completedGens) {
        World world = player.getWorld();
        if (world.isClient) return;

        // Scale the number of rockets based on player progress for a more impressive show.
        int rocketCount = 1 + (completedGens / 2);

        for (int i = 0; i < rocketCount; i++) {
            ItemStack fireworkRocket = new ItemStack(Items.FIREWORK_ROCKET);

            // Create an explosion with varied shapes and colors.
            FireworkExplosionComponent.Type shape = world.random.nextBoolean() ? FireworkExplosionComponent.Type.LARGE_BALL : FireworkExplosionComponent.Type.BURST;
            boolean hasTwinkle = world.random.nextBoolean();

            FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                    shape,
                    IntList.of(0xE67E22, 0xF1C40F, 0x2ECC71, 0x3498DB, 0x9B59B6), // Orange, Yellow, Green, Blue, Purple
                    IntList.of(),
                    true,
                    hasTwinkle
            );

            fireworkRocket.set(DataComponentTypes.FIREWORKS, new FireworksComponent(world.random.nextInt(2) + 1, java.util.List.of(explosion)));

            // Launch the rocket with a slight variation in its trajectory for a better visual spread.
            FireworkRocketEntity rocketEntity = new FireworkRocketEntity(world, player.getX(), player.getY(), player.getZ(), fireworkRocket);
            Vec3d velocity = new Vec3d(
                    world.random.nextGaussian() * 0.1,
                    0.5, // Upward thrust
                    world.random.nextGaussian() * 0.1
            );
            rocketEntity.setVelocity(velocity);
            world.spawnEntity(rocketEntity);
        }
    }

    public static void playMasterAchievementEffects(ServerPlayerEntity player) {
        if (player.getWorld().isClient) return;
        if (!(player.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld)) return;

        // --- FIX: Spawn particles directly instead of using entity status to avoid showing the totem item on the HUD ---
        // 1. Totem of Undying effect (particles only).
        serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getBodyY(0.5), player.getZ(), 35, 0.5, 0.8, 0.5, 0.15);

        // 2. End Portal aura: Creates a ring of portal particles around the player.
        serverWorld.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getBodyY(0.5), player.getZ(), 50, 0.5, 0.5, 0.5, 0.1);
    }
}