package org.papiricoh.supernaturalcraft.eclipse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;

/**
 * Whether a ritual eclipse hangs over this dimension, and until when. A fight against the Darkness
 * locks it: it will not lift while she is still being fought.
 */
public class EclipseSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_eclipse";

    private boolean active;
    private long startTick, endTick;
    private Optional<BlockPos> altar = Optional.empty();
    private boolean locked;

    public static EclipseSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(EclipseSavedData::new, EclipseSavedData::load, null), NAME);
    }

    private static EclipseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        EclipseSavedData d = new EclipseSavedData();
        d.active = tag.getBoolean("Active");
        d.startTick = tag.getLong("Start");
        d.endTick = tag.getLong("End");
        d.locked = tag.getBoolean("Locked");
        d.altar = NbtUtils.readBlockPos(tag, "Altar");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Active", active);
        tag.putLong("Start", startTick);
        tag.putLong("End", endTick);
        tag.putBoolean("Locked", locked);
        altar.ifPresent(p -> tag.put("Altar", NbtUtils.writeBlockPos(p)));
        return tag;
    }

    public boolean active() {
        return active;
    }

    public long startTick() {
        return startTick;
    }

    public long endTick() {
        return endTick;
    }

    public boolean locked() {
        return locked;
    }

    public Optional<BlockPos> altar() {
        return altar;
    }

    /** True once the eclipse has run its course and nothing holds it. */
    public boolean expired(long now) {
        return active && !locked && now >= endTick;
    }

    void begin(long now, int ticks, BlockPos at) {
        active = true;
        startTick = now;
        endTick = now + ticks;
        altar = Optional.of(at);
        setDirty();
    }

    void end() {
        active = false;
        locked = false;
        altar = Optional.empty();
        setDirty();
    }

    /** While locked, an eclipse does not lift; unlocking gives it {@code graceTicks} more. */
    public void setLocked(boolean locked, long now, int graceTicks) {
        this.locked = locked;
        if (!locked) endTick = Math.max(endTick, now + graceTicks);
        setDirty();
    }

    /** Test hook: let the eclipse lapse at {@code tick}. */
    public void setEndTick(long tick) {
        endTick = tick;
        setDirty();
    }
}
