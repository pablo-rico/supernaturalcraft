package org.papiricoh.supernaturalcraft.purgatory.rift;

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
import java.util.Map;
import java.util.UUID;

/** Every open Purgatory rift in one dimension (v0.39), kept so they close on time across restarts. */
public class PurgatoryRiftSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_purgatory_rifts";
    private final Map<UUID, PurgatoryRift> rifts = new LinkedHashMap<>();

    public static PurgatoryRiftSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(PurgatoryRiftSavedData::new, PurgatoryRiftSavedData::load), NAME);
    }

    private static PurgatoryRiftSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PurgatoryRiftSavedData data = new PurgatoryRiftSavedData();
        for (Tag t : tag.getList("Rifts", Tag.TAG_COMPOUND)) {
            PurgatoryRift r = PurgatoryRift.load((CompoundTag) t);
            data.rifts.put(r.id(), r);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        rifts.values().forEach(r -> list.add(r.save()));
        tag.put("Rifts", list);
        return tag;
    }

    public void add(PurgatoryRift rift) {
        rifts.put(rift.id(), rift);
        setDirty();
    }

    public void remove(PurgatoryRift rift) {
        rifts.remove(rift.id());
        setDirty();
    }

    public Collection<PurgatoryRift> all() {
        return rifts.values();
    }

    public @Nullable PurgatoryRift at(BlockPos pos) {
        for (PurgatoryRift r : rifts.values()) {
            if (r.anchor().distManhattan(pos) <= 6 && r.contains(pos)) return r;
        }
        return null;
    }
}
