package com.zehro_mc.pokenotifier.util;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class PrestigeEffects {

    public static void playChampionEffects(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        // Genera un rayo en la posición del jugador (solo el efecto visual, sin daño).
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
        lightning.setCosmetic(true);
        lightning.setPosition(player.getPos());
        world.spawnEntity(lightning);
    }
}