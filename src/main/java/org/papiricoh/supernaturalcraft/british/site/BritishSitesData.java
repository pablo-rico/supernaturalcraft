package org.papiricoh.supernaturalcraft.british.site;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Season 12's unique places in this world (v0.41): for each {@link SiteSpec#id()}, where it stands (origin and turn) and
 * whether it is built. One of each per world, raised the first time someone needs it. Old worlds have none of it.
 */
public class BritishSitesData extends SavedData {

    private static final String NAME = "supernaturalcraft_british_sites";

    public record Entry(BlockPos origin, int q, boolean built) {
    }

    private final Map<String, Entry> sites = new HashMap<>();

    public static BritishSitesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BritishSitesData::new, BritishSitesData::load, null), NAME);
    }

    private static BritishSitesData load(CompoundTag tag, HolderLookup.Provider registries) {
        BritishSitesData d = new BritishSitesData();
        for (String k : tag.getAllKeys()) {
            CompoundTag s = tag.getCompound(k);
            BlockPos o = NbtUtils.readBlockPos(s, "Origin").orElse(null);
            if (o != null) d.sites.put(k, new Entry(o, s.getInt("Q"), s.getBoolean("Built")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        sites.forEach((k, e) -> {
            CompoundTag s = new CompoundTag();
            s.put("Origin", NbtUtils.writeBlockPos(e.origin()));
            s.putInt("Q", e.q());
            s.putBoolean("Built", e.built());
            tag.put(k, s);
        });
        return tag;
    }

    public @Nullable Entry get(String id) {
        return sites.get(id);
    }

    public void set(String id, @Nullable BlockPos origin, int q, boolean built) {
        if (origin == null) sites.remove(id);
        else sites.put(id, new Entry(origin.immutable(), q, built));
        setDirty();
    }
}
