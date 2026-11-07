package org.papiricoh.supernaturalcraft.apocalypse.blood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlockEntities;

import java.util.UUID;

/** The Panic Room door's lock (v0.40): who is shut in to sweat the blood out, and the day the cure ends. */
public class PanicRoomDoorBlockEntity extends BlockEntity {

    private @Nullable UUID occupant;
    private long lockedUntil = -1L;

    public PanicRoomDoorBlockEntity(BlockPos pos, BlockState state) {
        super(AllBlockEntities.PANIC_ROOM_DOOR.get(), pos, state);
    }

    public @Nullable UUID occupant() {
        return occupant;
    }

    public long lockedUntil() {
        return lockedUntil;
    }

    public boolean locked() {
        return occupant != null;
    }

    public void lock(UUID who, long untilDay) {
        occupant = who;
        lockedUntil = untilDay;
        setChanged();
    }

    public void unlock() {
        occupant = null;
        lockedUntil = -1L;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (occupant != null) tag.putUUID("Occupant", occupant);
        tag.putLong("LockedUntil", lockedUntil);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        occupant = tag.hasUUID("Occupant") ? tag.getUUID("Occupant") : null;
        lockedUntil = tag.contains("LockedUntil") ? tag.getLong("LockedUntil") : -1L;
    }
}
