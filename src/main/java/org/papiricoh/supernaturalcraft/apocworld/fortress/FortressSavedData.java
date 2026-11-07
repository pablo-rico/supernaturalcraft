package org.papiricoh.supernaturalcraft.apocworld.fortress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Michael's fortress in this world (v0.41): where it stands (origin and turn, in the Apocalypse World or its stand-in) and
 * whether it is built. One per world, raised the first time a hunter reaches the burned world. Kept in the Overworld's storage.
 */
public class FortressSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_au_fortress";

    private @Nullable BlockPos origin;
    private int q;
    private boolean built;

    public static FortressSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(FortressSavedData::new, FortressSavedData::load, null), NAME);
    }

    private static FortressSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        FortressSavedData d = new FortressSavedData();
        if (tag.contains("Origin")) d.origin = NbtUtils.readBlockPos(tag, "Origin").orElse(null);
        d.q = tag.getInt("Q");
        d.built = tag.getBoolean("Built");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (origin != null) tag.put("Origin", NbtUtils.writeBlockPos(origin));
        tag.putInt("Q", q);
        tag.putBoolean("Built", built);
        return tag;
    }

    public @Nullable BlockPos origin() {
        return origin;
    }

    public int q() {
        return q;
    }

    public boolean built() {
        return built;
    }

    public void set(@Nullable BlockPos at, int turn, boolean isBuilt) {
        origin = at == null ? null : at.immutable();
        q = turn;
        built = isBuilt;
        setDirty();
    }
}
