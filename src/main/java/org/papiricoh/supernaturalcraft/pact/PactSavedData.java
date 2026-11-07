package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Season 6 in this world (v0.39): where Crowley's lab stands (origin and turn) and whether it is built, and how often Godstiel
 * has been called back. One lab per world, raised the first time Crowley hands it to a hunter. Old worlds have none of it.
 */
public class PactSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_pact";

    private @Nullable BlockPos lab;
    private int labQ;
    private boolean labBuilt;
    private int rematches;

    public static PactSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PactSavedData::new, PactSavedData::load, null), NAME);
    }

    private static PactSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PactSavedData d = new PactSavedData();
        if (tag.contains("Lab")) d.lab = NbtUtils.readBlockPos(tag, "Lab").orElse(null);
        d.labQ = tag.getInt("LabQ");
        d.labBuilt = tag.getBoolean("LabBuilt");
        d.rematches = tag.getInt("Rematches");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (lab != null) tag.put("Lab", NbtUtils.writeBlockPos(lab));
        tag.putInt("LabQ", labQ);
        tag.putBoolean("LabBuilt", labBuilt);
        tag.putInt("Rematches", rematches);
        return tag;
    }

    public @Nullable BlockPos lab() {
        return lab;
    }

    public int labQ() {
        return labQ;
    }

    public boolean labBuilt() {
        return labBuilt;
    }

    public void setLab(@Nullable BlockPos origin, int q, boolean built) {
        lab = origin == null ? null : origin.immutable();
        labQ = q;
        labBuilt = built;
        setDirty();
    }

    public int rematches() {
        return rematches;
    }

    public void rematched() {
        rematches++;
        setDirty();
    }

    /** Forgets the lab (commands: it will be raised again elsewhere). */
    public void reset() {
        lab = null;
        labQ = 0;
        labBuilt = false;
        rematches = 0;
        setDirty();
    }
}
