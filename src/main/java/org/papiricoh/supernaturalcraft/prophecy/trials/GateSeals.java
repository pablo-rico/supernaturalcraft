package org.papiricoh.supernaturalcraft.prophecy.trials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** The ground sealed by the Trials in a level (v0.36): no demon rises within {@link TrialRules#SEAL_RADIUS} of a seal. */
public class GateSeals extends SavedData {

    private static final String NAME = "supernaturalcraft_gate_seals";
    private final List<BlockPos> seals = new ArrayList<>();

    public static GateSeals get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(GateSeals::new, GateSeals::load, null), NAME);
    }

    private static GateSeals load(CompoundTag tag, HolderLookup.Provider registries) {
        GateSeals s = new GateSeals();
        for (long p : tag.getLongArray("Seals")) s.seals.add(BlockPos.of(p));
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Seals", seals.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public void add(BlockPos pos) {
        seals.add(pos.immutable());
        setDirty();
    }

    public List<BlockPos> all() {
        return List.copyOf(seals);
    }

    public boolean sealed(double x, double z) {
        for (BlockPos p : seals) if (TrialRules.sealed(x - p.getX() - 0.5, z - p.getZ() - 0.5)) return true;
        return false;
    }
}
