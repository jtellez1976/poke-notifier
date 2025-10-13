package com.zehro_mc.pokenotifier.block.entity;

import com.zehro_mc.pokenotifier.PokeNotifier;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class TrophyDisplayBlockEntity extends BlockEntity {

    private String trophyId = "";
    private String ownerUuid = "";

    public TrophyDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY_DISPLAY_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("trophyId", trophyId);
        nbt.putString("ownerUuid", ownerUuid);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.trophyId = nbt.getString("trophyId");
        this.ownerUuid = nbt.getString("ownerUuid");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    public void setTrophyData(String trophyId, String ownerUuid) {
        this.trophyId = trophyId;
        this.ownerUuid = ownerUuid;
        markDirty();
    }

    public String getTrophyId() { return trophyId; }
    public String getOwnerUuid() { return ownerUuid; }
}