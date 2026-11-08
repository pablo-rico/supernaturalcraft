package org.papiricoh.supernaturalcraft.dyingsun;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The world's sun (v0.42), kept on the Overworld: whether it is dying ({@link DyingSunRules#DYING}, since {@link #startTick} of the
 * world's calendar, let out by {@link #firstReleaser}), restored for good ({@link #RESTORED}) or never touched. Amara's phase VI
 * pushes {@link #fightOverride} (the countdown, 1 → 0; -1 when no fight drives it). Old worlds read the defaults.
 */
public class DyingSunSavedData extends SavedData {

    public static final String NAME = "supernaturalcraft_dying_sun";

    private byte state = DyingSunRules.NONE;
    private long startTick;
    private long restoredTick;
    private @Nullable UUID firstReleaser;
    /** Phase VI's countdown (1..0) while a fight drives the sun, else -1. Not saved: a reloaded fight sets it again. */
    private float fightOverride = -1f;

    public static DyingSunSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(DyingSunSavedData::new, DyingSunSavedData::load, null), NAME);
    }

    private static DyingSunSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DyingSunSavedData d = new DyingSunSavedData();
        if (tag.contains("state")) d.state = tag.getByte("state");
        if (tag.contains("start")) d.startTick = tag.getLong("start");
        if (tag.contains("restored")) d.restoredTick = tag.getLong("restored");
        if (tag.hasUUID("releaser")) d.firstReleaser = tag.getUUID("releaser");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putByte("state", state);
        tag.putLong("start", startTick);
        tag.putLong("restored", restoredTick);
        if (firstReleaser != null) tag.putUUID("releaser", firstReleaser);
        return tag;
    }

    public byte state() {
        return state;
    }

    public long startTick() {
        return startTick;
    }

    public @Nullable UUID firstReleaser() {
        return firstReleaser;
    }

    public float fightOverride() {
        return fightOverride;
    }

    /** The sun begins to die (only from {@link DyingSunRules#NONE}). @return whether it began */
    boolean begin(long tick, UUID releaser) {
        if (state != DyingSunRules.NONE) return false;
        state = DyingSunRules.DYING;
        startTick = tick;
        firstReleaser = releaser;
        setDirty();
        return true;
    }

    /** The sun is whole again, for good. @return whether it was dying */
    boolean restore(long tick) {
        boolean was = state == DyingSunRules.DYING;
        if (state == DyingSunRules.RESTORED) return false;
        state = DyingSunRules.RESTORED;
        restoredTick = tick;
        fightOverride = -1f;
        setDirty();
        return was;
    }

    void override(float countdown) {
        fightOverride = countdown;
    }

    // --- tests ------------------------------------------------------------------------------------------------------------

    /** The whole state, to put back after a test ({@link #restoreSnapshot}). */
    public CompoundTag snapshot() {
        CompoundTag t = save(new CompoundTag(), null);
        t.putFloat("override", fightOverride);
        return t;
    }

    public void restoreSnapshot(CompoundTag t) {
        state = t.getByte("state");
        startTick = t.getLong("start");
        restoredTick = t.getLong("restored");
        firstReleaser = t.hasUUID("releaser") ? t.getUUID("releaser") : null;
        fightOverride = t.contains("override") ? t.getFloat("override") : -1f;
        setDirty();
    }

    /** Tests: set the state outright. */
    public void force(byte s, long start) {
        state = s;
        startTick = start;
        if (s != DyingSunRules.DYING) fightOverride = -1f;
        setDirty();
    }
}
