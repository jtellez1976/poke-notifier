package com.zehro_mc.pokenotifier.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class PokeNotifierDeviceItem extends TrinketItem {

    public PokeNotifierDeviceItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient) {
            // Send packet to client to open GUI
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    serverPlayer,
                    new com.zehro_mc.pokenotifier.networking.OpenGuiPayload()
                );
            }
        } else {
            // Play activation sound
            player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
        }
        
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            if (entity.getWorld().isClient) {
                // Play tech-like sound
                player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
            } else {
                // Spawn subtle particles on server
                spawnEquipParticles(entity);
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        if (entity instanceof PlayerEntity player && entity.getWorld().isClient) {
            // Play softer tech sound
            player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.4f, 1.3f);
        }
    }
    
    private void spawnEquipParticles(LivingEntity entity) {
        if (!entity.getWorld().isClient) {
            net.minecraft.server.world.ServerWorld world = (net.minecraft.server.world.ServerWorld) entity.getWorld();
            // Spawn subtle portal particles around player
            world.spawnParticles(
                net.minecraft.particle.ParticleTypes.PORTAL,
                entity.getX(),
                entity.getY() + 1.0,
                entity.getZ(),
                10, // count
                0.3, 0.3, 0.3, // spread
                0.1 // speed
            );
        }
    }

    @Environment(EnvType.CLIENT)
    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        
        tooltip.add(Text.literal("A sophisticated tracking device")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("for Pokémon trainers.")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        
        tooltip.add(Text.literal("Press ")
            .formatted(Formatting.GRAY)
            .append(Text.literal("[N]").formatted(Formatting.YELLOW))
            .append(Text.literal(" to open").formatted(Formatting.GRAY)));
        
        tooltip.add(Text.literal("Right-click to use")
            .formatted(Formatting.GRAY));
        
        tooltip.add(Text.literal("Equip in Trinkets slot")
            .formatted(Formatting.GRAY));
        
        // Show owner if present
        String owner = getOwner(stack);
        if (owner != null && !owner.isEmpty()) {
            tooltip.add(Text.empty());
            tooltip.add(Text.literal("Owner: ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(owner).formatted(Formatting.AQUA)));
        }
    }
    
    public static void setOwner(ItemStack stack, String playerName) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putString("PokeNotifierOwner", playerName);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
    
    public static String getOwner(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component != null) {
            NbtCompound nbt = component.copyNbt();
            if (nbt.contains("PokeNotifierOwner")) {
                return nbt.getString("PokeNotifierOwner");
            }
        }
        return null;
    }
}
