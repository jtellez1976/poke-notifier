package com.zehro_mc.pokenotifier.block;

import com.mojang.serialization.MapCodec;
import com.zehro_mc.pokenotifier.block.entity.TrophyDisplayBlockEntity;
import com.zehro_mc.pokenotifier.component.ModDataComponents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.WorldView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TrophyDisplayBlock extends BlockWithEntity {

    // --- CORRECCIÓN: Definimos una hitbox para que el bloque sea interactuable ---
    private static final VoxelShape SHAPE = Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);

    public TrophyDisplayBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        // --- CORRECCIÓN: Implementamos el MapCodec requerido por Minecraft 1.20.5+ ---
        return createCodec(TrophyDisplayBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyDisplayBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // Hacemos que el bloque sea invisible, solo el BlockEntityRenderer dibujará algo.
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Devolvemos la hitbox que definimos.
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof TrophyDisplayBlockEntity be) {
                String ownerUuid = be.getOwnerUuid();
                // --- LÓGICA DE RECUPERACIÓN SEGURA (CORREGIDA) ---
                if (ownerUuid != null && !ownerUuid.isEmpty() && ownerUuid.equals(player.getUuidAsString())) {
                    // El jugador es el dueño, le devolvemos el trofeo.
                    String trophyId = be.getTrophyId();
                    Item trophyItem = Registries.ITEM.get(Identifier.of(trophyId));
                    ItemStack trophyStack = new ItemStack(trophyItem);
                    // Restauramos los datos de autenticidad al objeto.
                    trophyStack.set(ModDataComponents.OWNER_NAME, player.getName().getString());
                    trophyStack.set(ModDataComponents.OWNER_UUID, player.getUuidAsString());

                    player.getInventory().offerOrDrop(trophyStack);
                    world.removeBlock(pos, false);
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
                    return ActionResult.SUCCESS;
                }
            }
        }
        return ActionResult.PASS; // Si no es el dueño, no hace nada.
    }

    // --- CORRECCIÓN: Implementamos getPickStack para compatibilidad con Jade/WTHIT ---
    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        if (world.getBlockEntity(pos) instanceof TrophyDisplayBlockEntity be) {
            String trophyId = be.getTrophyId();
            if (trophyId != null && !trophyId.isEmpty()) {
                Item trophyItem = Registries.ITEM.get(Identifier.of(trophyId));
                return new ItemStack(trophyItem);
            }
        }
        return super.getPickStack(world, pos, state);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        // --- EFECTO DE PARTÍCULAS ---
        if (random.nextInt(5) == 0) { // Controla la frecuencia de las partículas
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
        }
    }
}