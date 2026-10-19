package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.List;
import org.papiricoh.supernaturalcraft.debug.AuditStats;

/**
 * Holy ground in one dimension (v0.13): the circles the rite Consecrate Ground has blessed (a radius round its altar), plus,
 * computed on the spot, anywhere within {@code SNConfig.CONSECRATED_RADIUS} of a {@code #consecrated} block (a ritual altar,
 * the choir's altar, an Enochian pillar). Angels pray here; it is where Grace comes from.
 */
public class ConsecratedGround extends SavedData {

    private static final String NAME = "supernaturalcraft_consecrated";
    /** How far up and down a consecrated block's holiness reaches (blocks). */
    private static final int VERTICAL = 4;

    public record Zone(BlockPos center, int radius) {
        boolean contains(BlockPos p) {
            double dx = p.getX() - center.getX(), dz = p.getZ() - center.getZ();
            return dx * dx + dz * dz <= (double) radius * radius && Math.abs(p.getY() - center.getY()) <= radius;
        }
    }

    private final List<Zone> zones = new ArrayList<>();

    public static ConsecratedGround get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(ConsecratedGround::new, ConsecratedGround::load), NAME);
    }

    private static ConsecratedGround load(CompoundTag tag, HolderLookup.Provider registries) {
        ConsecratedGround data = new ConsecratedGround();
        for (Tag t : tag.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag z = (CompoundTag) t;
            NbtUtils.readBlockPos(z, "Center").ifPresent(c -> data.zones.add(new Zone(c, z.getInt("Radius"))));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Zone z : zones) {
            CompoundTag t = new CompoundTag();
            t.put("Center", NbtUtils.writeBlockPos(z.center()));
            t.putInt("Radius", z.radius());
            list.add(t);
        }
        tag.put("Zones", list);
        return tag;
    }

    /** Blesses a circle (a second rite on the same spot only widens it). */
    public void consecrate(BlockPos center, int radius) {
        zones.removeIf(z -> z.center().equals(center) && z.radius() <= radius);
        zones.add(new Zone(center.immutable(), radius));
        setDirty();
    }

    public List<Zone> zones() {
        return List.copyOf(zones);
    }

    /** Everything as a tag (tests save and restore the world's state around themselves). */
    public CompoundTag snapshot() {
        return save(new CompoundTag(), null);
    }

    public void restore(CompoundTag tag) {
        zones.clear();
        zones.addAll(load(tag, null).zones);
        setDirty();
    }

    /** Takes away the holy ground round {@code center} (tests, commands). @return whether there was any */
    public boolean remove(BlockPos center) {
        boolean any = zones.removeIf(z -> z.center().equals(center));
        if (any) setDirty();
        return any;
    }

    public boolean inZone(BlockPos pos) {
        for (Zone z : zones) if (z.contains(pos)) return true;
        return false;
    }

    /** Whether {@code pos} is holy ground: a blessed circle, or near a consecrated block. */
    public static boolean holy(ServerLevel level, BlockPos pos) {
        long sn$t = AuditStats.start();
        try {
            if (get(level).inZone(pos)) return true;
            int r = SNConfig.CONSECRATED_RADIUS.get();
            // v0.19: only chunk sections whose palette may hold a consecrated block are read block by block.
            int minY = pos.getY() - VERTICAL, maxY = pos.getY() + VERTICAL;
            for (int cx = (pos.getX() - r) >> 4; cx <= (pos.getX() + r) >> 4; cx++) {
                for (int cz = (pos.getZ() - r) >> 4; cz <= (pos.getZ() + r) >> 4; cz++) {
                    net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    for (int sy = level.getSectionIndex(Math.max(minY, level.getMinBuildHeight()));
                         sy <= level.getSectionIndex(Math.min(maxY, level.getMaxBuildHeight() - 1)); sy++) {
                        var section = chunk.getSection(sy);
                        if (section.hasOnlyAir() || !section.maybeHas(b -> b.is(AllTags.Blocks.CONSECRATED))) continue;
                        int baseY = level.getMinBuildHeight() + sy * 16;
                        for (int y = Math.max(minY, baseY); y <= Math.min(maxY, baseY + 15); y++) {
                            for (int x = Math.max(pos.getX() - r, cx << 4); x <= Math.min(pos.getX() + r, (cx << 4) + 15); x++) {
                                for (int z = Math.max(pos.getZ() - r, cz << 4); z <= Math.min(pos.getZ() + r, (cz << 4) + 15); z++) {
                                    int dx = x - pos.getX(), dz = z - pos.getZ();
                                    if (dx * dx + dz * dz > r * r) continue;
                                    if (section.getBlockState(x & 15, y & 15, z & 15).is(AllTags.Blocks.CONSECRATED)) return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        } finally {
            AuditStats.stop("allegiance.consecratedScan", sn$t);
        }
    }
}
