package org.papiricoh.supernaturalcraft.purgatory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Purgatory's memory for the whole world (v0.39), kept in the Overworld's data: where Eve's glade is (worked out once from the
 * seed, {@code EveGladeSite}) and whether it has been raised, and where the landing near Purgatory's heart is. Fields are read with
 * {@code contains} and a default (older saves lack them).
 */
public class PurgatorySavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_purgatory";

    private @Nullable BlockPos glade;
    private int gladeQ;
    private boolean gladeBuilt;
    private @Nullable BlockPos landing;
    private int fights;

    public static PurgatorySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PurgatorySavedData::new, PurgatorySavedData::load), NAME);
    }

    private static PurgatorySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PurgatorySavedData d = new PurgatorySavedData();
        if (tag.contains("Glade")) d.glade = NbtUtils.readBlockPos(tag, "Glade").orElse(null);
        d.gladeQ = tag.contains("GladeQ") ? tag.getInt("GladeQ") : 0;
        d.gladeBuilt = tag.getBoolean("GladeBuilt");
        if (tag.contains("Landing")) d.landing = NbtUtils.readBlockPos(tag, "Landing").orElse(null);
        d.fights = tag.getInt("Fights");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (glade != null) tag.put("Glade", NbtUtils.writeBlockPos(glade));
        tag.putInt("GladeQ", gladeQ);
        tag.putBoolean("GladeBuilt", gladeBuilt);
        if (landing != null) tag.put("Landing", NbtUtils.writeBlockPos(landing));
        tag.putInt("Fights", fights);
        return tag;
    }

    public @Nullable BlockPos glade() {
        return glade;
    }

    public int gladeQ() {
        return gladeQ;
    }

    public boolean gladeBuilt() {
        return gladeBuilt;
    }

    public void setGlade(@Nullable BlockPos at, int q, boolean built) {
        glade = at == null ? null : at.immutable();
        gladeQ = q;
        gladeBuilt = built;
        setDirty();
    }

    public @Nullable BlockPos landing() {
        return landing;
    }

    public void setLanding(@Nullable BlockPos at) {
        landing = at == null ? null : at.immutable();
        setDirty();
    }

    /** How many times Eve has been fought (for the record; the rematch rite needs none). */
    public int fights() {
        return fights;
    }

    public void fought() {
        fights++;
        setDirty();
    }

    /** Forget the glade and the landing (commands): both are worked out again. */
    public void reset() {
        glade = null;
        gladeQ = 0;
        gladeBuilt = false;
        landing = null;
        setDirty();
    }
}
