package org.papiricoh.supernaturalcraft.apocworld.rift;

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

/** Every open rift to or from the Apocalypse World in one dimension (v0.41), kept so they close on time across restarts. */
public class AuRiftSavedData extends SavedData {

    private static final String NAME = "supernaturalcraft_au_rifts";
    private final Map<UUID, AuRift> rifts = new LinkedHashMap<>();

    public static AuRiftSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(AuRiftSavedData::new, AuRiftSavedData::load), NAME);
    }

    private static AuRiftSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AuRiftSavedData data = new AuRiftSavedData();
        for (Tag t : tag.getList("Rifts", Tag.TAG_COMPOUND)) {
            AuRift r = AuRift.load((CompoundTag) t);
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

    public void add(AuRift rift) {
        rifts.put(rift.id(), rift);
        setDirty();
    }

    public void remove(AuRift rift) {
        rifts.remove(rift.id());
        setDirty();
    }

    public Collection<AuRift> all() {
        return rifts.values();
    }

    public @Nullable AuRift at(BlockPos pos) {
        for (AuRift r : rifts.values()) {
            if (r.anchor().distManhattan(pos) <= 6 && r.contains(pos)) return r;
        }
        return null;
    }
}
