package org.papiricoh.supernaturalcraft.hunter.wall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;

import java.util.UUID;

/** Who painted an alarm sigil, and when it may chime again (not saved: a reload rings at once). */
public class AlarmSigilBlockEntity extends BlockEntity {

    @Nullable
    private UUID owner;
    private long quietUntil;

    public AlarmSigilBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.ALARM_SIGIL.get(), pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public long quietUntil() {
        return quietUntil;
    }

    public void setQuietUntil(long t) {
        quietUntil = t;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
