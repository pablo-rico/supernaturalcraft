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

    public boolean inZone(BlockPos pos) {
        for (Zone z : zones) if (z.contains(pos)) return true;
        return false;
    }

    /** Whether {@code pos} is holy ground: a blessed circle, or near a consecrated block. */
    public static boolean holy(ServerLevel level, BlockPos pos) {
        if (get(level).inZone(pos)) return true;
        int r = SNConfig.CONSECRATED_RADIUS.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = -VERTICAL; dy <= VERTICAL; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.isLoaded(m) && level.getBlockState(m).is(AllTags.Blocks.CONSECRATED)) return true;
                }
            }
        }
        return false;
    }
}
