package org.papiricoh.supernaturalcraft.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Every arena in one dimension, persisted so a crash or restart mid-fight can still be cleaned up. */
public class ArenaSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_arenas";
    private final Map<UUID, ArenaController> arenas = new LinkedHashMap<>();

    public static ArenaSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ArenaSavedData::new,
                (tag, registries) -> load(tag, registries)), NAME);
    }

    private static ArenaSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ArenaSavedData data = new ArenaSavedData();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("Arenas", Tag.TAG_COMPOUND)) {
            ArenaController a = ArenaController.load((CompoundTag) t, blocks);
            data.arenas.put(a.id(), a);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        arenas.values().forEach(a -> list.add(a.save()));
        tag.put("Arenas", list);
        return tag;
    }

    public ArenaController create(BlockPos center, int radius) {
        ArenaController a = new ArenaController(UUID.randomUUID(), center, radius);
        arenas.put(a.id(), a);
        setDirty();
        return a;
    }

    public @Nullable ArenaController get(UUID id) {
        return id == null ? null : arenas.get(id);
    }

    public Collection<ArenaController> all() {
        return arenas.values();
    }

    public boolean hasActive() {
        return arenas.values().stream().anyMatch(ArenaController::isActive);
    }

    public @Nullable ArenaController at(net.minecraft.world.phys.Vec3 pos) {
        for (ArenaController a : arenas.values()) {
            if (a.isActive() && a.contains(pos)) return a;
        }
        return null;
    }

    public void removeClosed() {
        if (arenas.values().removeIf(a -> a.status() == ArenaController.Status.CLOSED)) setDirty();
    }
}
