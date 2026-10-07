package org.papiricoh.supernaturalcraft.hell.rift;

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

/** Every open rift in one dimension, kept so they close on time even across restarts. */
public class HellRiftSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_rifts";
    private final Map<UUID, HellRift> rifts = new LinkedHashMap<>();

    public static HellRiftSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(HellRiftSavedData::new, HellRiftSavedData::load), NAME);
    }

    private static HellRiftSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HellRiftSavedData data = new HellRiftSavedData();
        for (Tag t : tag.getList("Rifts", Tag.TAG_COMPOUND)) {
            HellRift r = HellRift.load((CompoundTag) t);
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

    public void add(HellRift rift) {
        rifts.put(rift.id(), rift);
        setDirty();
    }

    public void remove(HellRift rift) {
        rifts.remove(rift.id());
        setDirty();
    }

    public Collection<HellRift> all() {
        return rifts.values();
    }

    public @Nullable HellRift at(BlockPos pos) {
        for (HellRift r : rifts.values()) {
            if (r.anchor().distManhattan(pos) <= 6 && r.contains(pos)) return r;
        }
        return null;
    }
}
