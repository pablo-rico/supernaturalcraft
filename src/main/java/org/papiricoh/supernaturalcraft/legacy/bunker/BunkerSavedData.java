package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** The bunker's place in the world, kept in the overworld: where it stands (once computed) and whether it is known built. */
public class BunkerSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_bunker";

    private @Nullable BlockPos origin;
    private int rotation;
    private boolean built;

    public static BunkerSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(new Factory<>(BunkerSavedData::new, BunkerSavedData::load, null), NAME);
    }

    private static BunkerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BunkerSavedData d = new BunkerSavedData();
        d.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(null);
        d.rotation = tag.getInt("Rotation");
        d.built = tag.getBoolean("Built");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (origin != null) tag.put("Origin", NbtUtils.writeBlockPos(origin));
        tag.putInt("Rotation", rotation);
        tag.putBoolean("Built", built);
        return tag;
    }

    /** The hut's floor centre ({@link BunkerLayout} origin), or null until worked out. */
    public @Nullable BlockPos origin() {
        return origin;
    }

    public int rotation() {
        return rotation;
    }

    public boolean built() {
        return built;
    }

    public void setBunker(@Nullable BlockPos origin, int rotation, boolean built) {
        this.origin = origin == null ? null : origin.immutable();
        this.rotation = Math.floorMod(rotation, 4);
        this.built = built;
        setDirty();
    }

    /** Everything as a tag (tests save and restore the record around themselves). */
    public CompoundTag snapshot() {
        return save(new CompoundTag(), null);
    }

    public void restore(CompoundTag tag) {
        BunkerSavedData d = load(tag, null);
        origin = d.origin;
        rotation = d.rotation;
        built = d.built;
        setDirty();
    }
}
