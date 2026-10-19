package org.papiricoh.supernaturalcraft.coven.church;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.List;

/**
 * The cleansed churches of one dimension (v0.21): each a sanctuary round its altar that no demon or spirit may enter — those
 * inside are driven out to its edge every second ({@link #tick}), with a HEX_PULSE where holiness turns them. Bosses are not
 * bound by it. Also remembers which churches were consecrated (by their structure start), so each rewards its first rite once.
 */
public class Sanctuaries extends SavedData {

    private static final String NAME = "supernaturalcraft_sanctuaries";
    public static final int TICK_EVERY = 20;

    /** One sanctuary: its centre, radius and the church it belongs to (its structure start's chunk, packed). */
    public record Sanctuary(BlockPos centre, int radius, long church) {
        public boolean contains(Vec3 p) {
            double dx = p.x - (centre.getX() + 0.5), dz = p.z - (centre.getZ() + 0.5);
            return dx * dx + dz * dz <= (double) radius * radius && Math.abs(p.y - centre.getY()) <= radius;
        }
    }

    private final List<Sanctuary> zones = new ArrayList<>();

    public static Sanctuaries get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(Sanctuaries::new, Sanctuaries::load, null), NAME);
    }

    private static Sanctuaries load(CompoundTag tag, HolderLookup.Provider registries) {
        Sanctuaries s = new Sanctuaries();
        for (Tag t : tag.getList("Zones", Tag.TAG_COMPOUND)) {
            CompoundTag z = (CompoundTag) t;
            NbtUtils.readBlockPos(z, "Centre").ifPresent(c -> s.zones.add(new Sanctuary(c, z.getInt("Radius"), z.getLong("Church"))));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Sanctuary z : zones) {
            CompoundTag t = new CompoundTag();
            t.put("Centre", NbtUtils.writeBlockPos(z.centre()));
            t.putInt("Radius", z.radius());
            t.putLong("Church", z.church());
            list.add(t);
        }
        tag.put("Zones", list);
        return tag;
    }

    public List<Sanctuary> zones() {
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

    /** Takes away every sanctuary round {@code centre} (tests, commands). @return whether any was */
    public boolean remove(BlockPos centre) {
        boolean any = zones.removeIf(z -> z.centre().equals(centre));
        if (any) setDirty();
        return any;
    }

    /** Whether the church whose start is {@code church} has been consecrated. */
    public boolean consecrated(long church) {
        for (Sanctuary z : zones) if (z.church() == church) return true;
        return false;
    }

    public void add(Sanctuary s) {
        zones.removeIf(z -> z.church() == s.church() && z.centre().equals(s.centre()));
        zones.add(s);
        setDirty();
    }

    public boolean inside(Vec3 p) {
        for (Sanctuary z : zones) if (z.contains(p)) return true;
        return false;
    }

    /** Whether {@code e} is barred from a sanctuary: a demon or a spirit that is no boss and no player. */
    public static boolean barred(Entity e) {
        if (!(e instanceof Mob) || e.getType().is(AllTags.Entities.BOSSES)) return false;
        return Kin.isDemon(e) || e.getType().is(AllTags.Entities.SPIRITS) || e.getType().is(AllTags.Entities.DEMONS);
    }

    /** Drives every barred creature out of every sanctuary of {@code level}. */
    public static void tick(ServerLevel level) {
        Sanctuaries data = get(level);
        if (data.zones.isEmpty()) return;
        for (Sanctuary z : data.zones) {
            if (!level.isLoaded(z.centre())) continue;
            Vec3 c = Vec3.atBottomCenterOf(z.centre());
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(z.centre()).inflate(z.radius()),
                    e -> e.isAlive() && barred(e) && z.contains(e.position()))) {
                drive(level, e, c, z.radius());
            }
        }
    }

    private static void drive(ServerLevel level, LivingEntity e, Vec3 centre, int radius) {
        Vec3 away = e.position().subtract(centre).multiply(1, 0, 1);
        if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
        away = away.normalize();
        Vec3 edge = centre.add(away.scale(radius + 1.5));
        if (e instanceof Mob m) {
            m.getNavigation().stop();
            m.setTarget(null);
        }
        e.teleportTo(edge.x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(edge.x), (int) Math.floor(edge.z)), edge.z);
        e.push(away.x * 0.4, 0.2, away.z * 0.4);
        e.hurtMarked = true;
        MagicFx.hex(level, e, e.position().add(0, e.getBbHeight() / 2, 0), MagicFxPayload.HEX_PROTECT, 1.2f, 20);
    }
}
