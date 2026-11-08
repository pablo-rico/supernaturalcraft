package org.papiricoh.supernaturalcraft.hands;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The reliquary altars this world knows of (v0.42), kept by the Overworld: every altar notes itself when its chunk loads
 * ({@code HandAltarBlockEntity.onLoad}) and every one built by command or preview when it is built. The locator looks here
 * first (cheap), then runs the structure search for the ones nobody has come near yet. Capped at {@link #CAP} (oldest first out).
 */
public class ReliquaryIndex extends SavedData {

    private static final String NAME = "supernaturalcraft_reliquaries";
    public static final int CAP = 1024;

    private final Map<Long, S11Assets.Hand> altars = new LinkedHashMap<>();

    public static ReliquaryIndex get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ReliquaryIndex::new, ReliquaryIndex::load, null), NAME);
    }

    private static ReliquaryIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        ReliquaryIndex d = new ReliquaryIndex();
        for (Tag t : tag.getList("Altars", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            S11Assets.Hand h = kind(c.getString("Kind"));
            if (h != null) d.altars.put(c.getLong("Pos"), h);
        }
        return d;
    }

    static @Nullable S11Assets.Hand kind(String name) {
        for (S11Assets.Hand h : S11Assets.Hand.values()) if (h.reliquary.equals(name)) return h;
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, S11Assets.Hand> e : altars.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", e.getKey());
            c.putString("Kind", e.getValue().reliquary);
            list.add(c);
        }
        tag.put("Altars", list);
        return tag;
    }

    public void note(BlockPos pos, S11Assets.Hand kind) {
        long k = pos.asLong();
        if (altars.get(k) == kind) return;
        altars.put(k, kind);
        while (altars.size() > CAP) altars.remove(altars.keySet().iterator().next());
        setDirty();
    }

    public void forget(BlockPos pos) {
        if (altars.remove(pos.asLong()) != null) setDirty();
    }

    /** The nearest known altar of {@code kind} to {@code from} within {@code reach} blocks (on the ground plane), or null. */
    public @Nullable BlockPos nearest(S11Assets.Hand kind, BlockPos from, double reach) {
        BlockPos best = null;
        double d = reach * reach;
        for (Map.Entry<Long, S11Assets.Hand> e : altars.entrySet()) {
            if (e.getValue() != kind) continue;
            BlockPos p = BlockPos.of(e.getKey());
            double dx = p.getX() - from.getX(), dz = p.getZ() - from.getZ(), dd = dx * dx + dz * dz;
            if (dd <= d) {
                d = dd;
                best = p;
            }
        }
        return best;
    }

    public Collection<Long> all() {
        return altars.keySet();
    }
}
