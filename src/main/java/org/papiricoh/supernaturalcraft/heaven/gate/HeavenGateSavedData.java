package org.papiricoh.supernaturalcraft.heaven.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Every gate of light in one dimension (v0.18): the rites' ones close on time, even across restarts; the plots' stay. */
public class HeavenGateSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_heaven_gates";
    private final Map<UUID, HeavenGate> gates = new LinkedHashMap<>();

    public static HeavenGateSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(HeavenGateSavedData::new, HeavenGateSavedData::load), NAME);
    }

    private static HeavenGateSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HeavenGateSavedData data = new HeavenGateSavedData();
        for (Tag t : tag.getList("Gates", Tag.TAG_COMPOUND)) {
            HeavenGate g = HeavenGate.load((CompoundTag) t);
            data.gates.put(g.id(), g);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        gates.values().forEach(g -> list.add(g.save()));
        tag.put("Gates", list);
        return tag;
    }

    public void add(HeavenGate gate) {
        gates.put(gate.id(), gate);
        setDirty();
    }

    public void remove(HeavenGate gate) {
        if (gates.remove(gate.id()) != null) setDirty();
    }

    public Collection<HeavenGate> all() {
        return gates.values();
    }

    public @Nullable HeavenGate at(BlockPos pos) {
        for (HeavenGate g : gates.values()) if (g.contains(pos)) return g;
        return null;
    }

    /** The permanent gate of this kind that belongs to {@code owner}'s plot, if any. */
    public @Nullable HeavenGate find(HeavenGate.Kind kind, UUID owner) {
        for (HeavenGate g : gates.values()) if (g.kind() == kind && g.owner().equals(owner)) return g;
        return null;
    }

    /** Forgets every gate of {@code owner}'s plot of these kinds (a plot written again re-registers them). */
    public void forget(UUID owner, List<HeavenGate.Kind> kinds) {
        if (gates.values().removeIf(g -> g.owner().equals(owner) && kinds.contains(g.kind()))) setDirty();
    }
}
