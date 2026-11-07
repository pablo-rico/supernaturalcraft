package org.papiricoh.supernaturalcraft.apocalypse.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Alastair's warehouse in this world (v0.40): where it stands (origin and turn) and whether it is built. One per world, raised
 * the first time Ruby sends a hunter there. Old worlds have none of it.
 */
public class WarehouseSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_warehouse";

    private @Nullable BlockPos origin;
    private int q;
    private boolean built;

    public static WarehouseSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(WarehouseSavedData::new, WarehouseSavedData::load, null), NAME);
    }

    private static WarehouseSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarehouseSavedData d = new WarehouseSavedData();
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
