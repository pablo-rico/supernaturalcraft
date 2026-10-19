package org.papiricoh.supernaturalcraft.cain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Cain's world state (v0.21), kept in the overworld: where his farm stands (once worked out), whether it is known to be built,
 * and where his grave is once he has fallen the first time. Who has beaten him is each hunter's own advancement.
 */
public class CainSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_cain";

    private @Nullable BlockPos farm;
    private int rotation;
    private boolean built;
    private @Nullable BlockPos grave;

    public static CainSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(CainSavedData::new, CainSavedData::load, null), NAME);
    }

    private static CainSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CainSavedData d = new CainSavedData();
        d.farm = NbtUtils.readBlockPos(tag, "Farm").orElse(null);
        d.rotation = tag.getInt("Rotation");
        d.built = tag.getBoolean("Built");
        d.grave = NbtUtils.readBlockPos(tag, "Grave").orElse(null);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (farm != null) tag.put("Farm", NbtUtils.writeBlockPos(farm));
        tag.putInt("Rotation", rotation);
        tag.putBoolean("Built", built);
        if (grave != null) tag.put("Grave", NbtUtils.writeBlockPos(grave));
        return tag;
    }

    /** The farm's origin ({@code CainFarmLayout}: the yard's middle), or null until worked out. */
    public @Nullable BlockPos farm() {
        return farm;
    }

    public int rotation() {
        return rotation;
    }

    public boolean built() {
        return built;
    }

    /** His headstone, once he has fallen (null before). */
    public @Nullable BlockPos grave() {
        return grave;
    }

    public void setFarm(@Nullable BlockPos origin, int rotation, boolean built) {
        this.farm = origin == null ? null : origin.immutable();
        this.rotation = Math.floorMod(rotation, 4);
        this.built = built;
        setDirty();
    }

    public void setGrave(@Nullable BlockPos at) {
        this.grave = at == null ? null : at.immutable();
        setDirty();
    }

    /** Everything as a tag (tests save and restore the world's state around themselves). */
    public CompoundTag snapshot() {
        return save(new CompoundTag(), null);
    }

    public void restore(CompoundTag tag) {
        CainSavedData d = load(tag, null);
        farm = d.farm;
        rotation = d.rotation;
        built = d.built;
        grave = d.grave;
        setDirty();
    }
}
