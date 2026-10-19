package org.papiricoh.supernaturalcraft.coven.rowena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Rowena's world state (v0.21), kept in the overworld: where her house stands (once worked out) and whether it is known to be
 * built. Who has met her and what she thinks of them is each hunter's own ({@code ROWENA} attachment).
 */
public class RowenaSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_rowena";

    private @Nullable BlockPos house;
    private int rotation;
    private boolean built;

    public static RowenaSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(RowenaSavedData::new, RowenaSavedData::load, null), NAME);
    }

    private static RowenaSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        RowenaSavedData d = new RowenaSavedData();
        d.house = NbtUtils.readBlockPos(tag, "House").orElse(null);
        d.rotation = tag.getInt("Rotation");
        d.built = tag.getBoolean("Built");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (house != null) tag.put("House", NbtUtils.writeBlockPos(house));
        tag.putInt("Rotation", rotation);
        tag.putBoolean("Built", built);
        return tag;
    }

    /** The house's origin ({@code RowenaHouseLayout}: the hall's floor centre), or null until worked out. */
    public @Nullable BlockPos house() {
        return house;
    }

    public int rotation() {
        return rotation;
    }

    public boolean built() {
        return built;
    }

    public void setHouse(@Nullable BlockPos origin, int rotation, boolean built) {
        this.house = origin == null ? null : origin.immutable();
        this.rotation = Math.floorMod(rotation, 4);
        this.built = built;
        setDirty();
    }

    /** Everything as a tag (tests save and restore the world's state around themselves). */
    public CompoundTag snapshot() {
        return save(new CompoundTag(), null);
    }

    public void restore(CompoundTag tag) {
        RowenaSavedData d = load(tag, null);
        house = d.house;
        rotation = d.rotation;
        built = d.built;
        setDirty();
    }
}
