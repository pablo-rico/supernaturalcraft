package org.papiricoh.supernaturalcraft.angel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Whether Heaven's angels walk this world yet, and whether the Fall has come (v0.22), kept by the Overworld. Angels come
 * once anyone has beaten Lilith; the fallen once anyone has beaten Metatron, whose fall casts them down on the next night
 * ({@link TheFall}).
 */
public class HostsSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_hosts";

    private boolean angels;
    /** Whether the Leviathans walk the earth (v0.25): anyone has beaten Raphael ({@code leviathan.LeviathanSpawns}). */
    private boolean leviathans;
    /** The Fall: whether Metatron has been beaten, whether its night is still to come, whether it is falling now. */
    private boolean fallen, fallPending, fallNight;

    public static HostsSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(HostsSavedData::new, HostsSavedData::load, null), NAME);
    }

    private static HostsSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HostsSavedData d = new HostsSavedData();
        d.angels = tag.getBoolean("Angels");
        d.fallen = tag.getBoolean("Fallen");
        d.fallPending = tag.getBoolean("FallPending");
        d.fallNight = tag.getBoolean("FallNight");
        d.leviathans = tag.getBoolean("Leviathans");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Angels", angels);
        tag.putBoolean("Fallen", fallen);
        tag.putBoolean("FallPending", fallPending);
        tag.putBoolean("FallNight", fallNight);
        tag.putBoolean("Leviathans", leviathans);
        return tag;
    }

    /** Whether angels walk the earth (Lilith beaten). */
    public boolean angels() {
        return angels;
    }

    public void setAngels(boolean open) {
        if (angels != open) {
            angels = open;
            setDirty();
        }
    }

    /** Whether the Leviathans walk the earth (Raphael beaten). */
    public boolean leviathans() {
        return leviathans;
    }

    public void setLeviathans(boolean open) {
        if (leviathans != open) {
            leviathans = open;
            setDirty();
        }
    }

    /** Whether the fallen walk the earth (the Fall has come). */
    public boolean fallen() {
        return fallen;
    }

    /** Opens the fallen; with {@code pending}, the Fall's night is still to be seen. */
    public void setFallen(boolean open, boolean pending) {
        fallen = open;
        fallPending = open && pending;
        if (!open) fallNight = false;
        setDirty();
    }

    public boolean fallPending() {
        return fallPending;
    }

    public boolean fallNight() {
        return fallNight;
    }

    void setFallNight(boolean on) {
        fallNight = on;
        if (on) fallPending = false;
        setDirty();
    }
}
