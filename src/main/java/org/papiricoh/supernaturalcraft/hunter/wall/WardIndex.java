package org.papiricoh.supernaturalcraft.hunter.wall;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the Enochian wards of a level are painted (v0.36), bucketed by chunk so a question costs a few map lookups. Kept by
 * the ward block itself ({@link EnochianWardBlock#onPlace}/{@code onRemove}); an entry whose block is gone (a world edited
 * from outside) is dropped the first time it is asked about in a loaded chunk.
 */
public class WardIndex extends SavedData {

    private static final String NAME = "supernaturalcraft_wards";
    /** Packed block position → radius. */
    private final Map<Long, Integer> wards = new HashMap<>();
    private final Map<Long, List<Long>> byChunk = new HashMap<>();

    /** A ward that holds a point. */
    public record Ward(BlockPos pos, int radius) {
    }

    public static WardIndex get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(WardIndex::new, WardIndex::load, null), NAME);
    }

    private static WardIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        WardIndex index = new WardIndex();
        long[] pos = tag.getLongArray("Pos");
        int[] radius = tag.getIntArray("Radius");
        for (int i = 0; i < Math.min(pos.length, radius.length); i++) index.put(pos[i], radius[i]);
        return index;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] pos = new long[wards.size()];
        int[] radius = new int[wards.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : wards.entrySet()) {
            pos[i] = e.getKey();
            radius[i++] = e.getValue();
        }
        tag.putLongArray("Pos", pos);
        tag.putIntArray("Radius", radius);
        return tag;
    }

    public boolean isEmpty() {
        return wards.isEmpty();
    }

    public void add(BlockPos pos, int radius) {
        put(pos.asLong(), radius);
        setDirty();
    }

    public void remove(BlockPos pos) {
        Long key = pos.asLong();
        if (wards.remove(key) == null) return;
        List<Long> bucket = byChunk.get(chunk(key));
        if (bucket != null) {
            bucket.remove(key);
            if (bucket.isEmpty()) byChunk.remove(chunk(key));
        }
        setDirty();
    }

    private void put(long key, int radius) {
        if (wards.put(key, radius) == null) byChunk.computeIfAbsent(chunk(key), k -> new ArrayList<>()).add(key);
    }

    private static long chunk(long packed) {
        return ChunkPos.asLong(BlockPos.getX(packed) >> 4, BlockPos.getZ(packed) >> 4);
    }

    /** The ward holding {@code at}, or null. */
    @Nullable
    public Ward holding(ServerLevel level, Vec3 at) {
        if (wards.isEmpty()) return null;
        int reach = (WardGeometry.SUPREME_RADIUS >> 4) + 1;
        int cx = (int) Math.floor(at.x) >> 4, cz = (int) Math.floor(at.z) >> 4;
        List<Long> stale = null;
        Ward found = null;
        for (int x = cx - reach; x <= cx + reach && found == null; x++) {
            for (int z = cz - reach; z <= cz + reach && found == null; z++) {
                List<Long> bucket = byChunk.get(ChunkPos.asLong(x, z));
                if (bucket == null) continue;
                for (long key : bucket) {
                    int bx = BlockPos.getX(key), by = BlockPos.getY(key), bz = BlockPos.getZ(key);
                    int r = wards.get(key);
                    if (!WardGeometry.inside(at.x, at.y, at.z, bx, by, bz, r)) continue;
                    BlockPos p = BlockPos.of(key);
                    if (level.isLoaded(p) && !(level.getBlockState(p).getBlock() instanceof EnochianWardBlock)) {
                        if (stale == null) stale = new ArrayList<>();
                        stale.add(key);
                        continue;
                    }
                    found = new Ward(p, r);
                    break;
                }
            }
        }
        if (stale != null) stale.forEach(k -> remove(BlockPos.of(k)));
        return found;
    }

    public static boolean warded(ServerLevel level, Vec3 at) {
        return get(level).holding(level, at) != null;
    }
}
