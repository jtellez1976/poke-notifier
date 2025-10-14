package com.zehro_mc.pokenotifier.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
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
import net.minecraft.world.World;

public class PrestigeEffects {

    public static void playMasterEffects(ServerPlayerEntity masterPlayer) {
        MinecraftServer server = masterPlayer.getServer();
        if (server == null) return;

        // --- CORRECCIÓN: Iteramos sobre todos los jugadores y les enviamos el sonido individualmente ---
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.playSoundToPlayer(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 1.0F, 1.0F);
        }
    }

    public static void launchCelebratoryFireworks(ServerPlayerEntity player) {
        World world = player.getWorld();
        if (world.isClient) return;

        // Creamos un cohete de fuegos artificiales con una explosión aleatoria.
        ItemStack fireworkRocket = new ItemStack(Items.FIREWORK_ROCKET);

        // Creamos una explosión con forma de esfera grande y colores aleatorios.
        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                FireworkExplosionComponent.Type.LARGE_BALL,
                IntList.of(0xE67E22, 0xF1C40F, 0x2ECC71), // CORRECCIÓN: Usamos IntList para los colores.
                IntList.of(), // Sin colores de desvanecimiento
                true, // Con rastro
                false // Sin centelleo
        );

        // Añadimos la explosión y la duración del vuelo al cohete.
        fireworkRocket.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, java.util.List.of(explosion)));

        // Lanzamos el cohete desde la posición del jugador.
        world.spawnEntity(new FireworkRocketEntity(world, player.getX(), player.getY(), player.getZ(), fireworkRocket));
    }

    public static void playMasterAchievementEffects(ServerPlayerEntity player) {
        if (player.getWorld().isClient) return;

        // 1. Efecto de Tótem de la Inmortalidad: Envuelve al jugador en partículas verdes y amarillas.
        player.getWorld().sendEntityStatus(player, EntityStatuses.USE_TOTEM_OF_UNDYING);

        // 4. Aura de Portal del End: Crea un anillo de partículas de portal alrededor del jugador.
        if (player.getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getBodyY(0.5), player.getZ(), 50, 0.5, 0.5, 0.5, 0.1);
        }
    }
}