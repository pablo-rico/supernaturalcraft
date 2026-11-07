package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.coven.church.ChurchStructure;
import org.papiricoh.supernaturalcraft.coven.church.Sanctuaries;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What each kind of seal event does in the world (v0.40): where it happens ({@link #site}), who and what it raises there
 * ({@link #stage}) and how its creatures behave each second ({@link #tick}): the demons go for the wards when no hunter is close,
 * the angels go for the rebels, the Witnesses for the hunter the seal called, the plagues spread a step at a time
 * ({@link SealPlagues}). Everything it raises carries the event's id and goes when it ends ({@link #clear}).
 */
public final class SealStaging {

    /** A driver this close to a hunter fights the hunter (its own AI); farther, it goes for the wards. */
    public static final double ENGAGE = 12;
    /** Creatures that stray farther than this from the site are walked back. */
    public static final double LEASH = 20;
    /** At most this many Witnesses, swarms or summoned guards at a site at once. */
    public static final int EXTRAS_CAP = 6;
    /** The most livestock a plague kills at one site. */
    public static final int LIVESTOCK_MAX = 6;

    private SealStaging() {
    }

    // --- where -----------------------------------------------------------------------------------------------------------------

    /**
     * A site for a seal event called on {@code hunter}: {@link SealRules#MIN_DISTANCE}-{@link SealRules#MAX_DISTANCE} blocks away,
     * on dry ground in a loaded chunk, never in an arena or a sanctuary; for a priest, the nearest church if one stands near.
     */
    public static @Nullable BlockPos site(ServerLevel level, ServerPlayer hunter, SealKind kind, RandomSource r) {
        if (kind == SealKind.PRIEST) {
            BlockPos church = church(level, hunter.blockPosition());
            if (church != null) return church;
        }
        for (int attempt = 0; attempt < 24; attempt++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = SealRules.MIN_DISTANCE + r.nextDouble() * (SealRules.MAX_DISTANCE - SealRules.MIN_DISTANCE);
            int x = (int) Math.floor(hunter.getX() + Math.cos(a) * d), z = (int) Math.floor(hunter.getZ() + Math.sin(a) * d);
            BlockPos at = ground(level, x, z);
            if (at != null && fits(level, at)) return at;
        }
        return null;
    }

    /** Whether an event may stand at {@code at}: loaded and ticking, outside any arena and sanctuary, not on water. */
    public static boolean fits(ServerLevel level, BlockPos at) {
        if (!level.isLoaded(at) || !level.isPositionEntityTicking(at)) return false;
        Vec3 c = Vec3.atBottomCenterOf(at);
        if (ArenaSavedData.get(level).at(c) != null) return false;
        if (Sanctuaries.get(level).inside(c)) return false;
        BlockState below = level.getBlockState(at.below());
        return below.isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP) && level.getFluidState(at).isEmpty()
                && level.getFluidState(at.below()).isEmpty() && level.getBlockState(at).canBeReplaced();
    }

    /** The open ground at (x, z): the first standable spot under the top block (leaves left out), or null if unloaded. */
    public static @Nullable BlockPos ground(ServerLevel level, int x, int z) {
        if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) return null;
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
    }

    /** The middle of the nearest unconsecrated church within six chunks (only loaded ones are looked at), or null. */
    static @Nullable BlockPos church(ServerLevel level, BlockPos near) {
        Structure type = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ChurchStructure.KEY);
        if (type == null) return null;
        ChunkPos c = new ChunkPos(near);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
                if (chunk == null) continue;
                StructureStart start = chunk.getStartForStructure(type);
                if (start == null || !start.isValid()) continue;
                BlockPos mid = start.getBoundingBox().getCenter();
                BlockPos g = ground(level, mid.getX(), mid.getZ());
                if (g == null || Sanctuaries.get(level).inside(Vec3.atBottomCenterOf(g)) || ArenaSavedData.get(level).at(Vec3.atBottomCenterOf(g)) != null) continue;
                double d = g.distSqr(near);
                if (d < bestD) {
                    bestD = d;
                    best = g;
                }
            }
        }
        return best;
    }

    // --- who comes -------------------------------------------------------------------------------------------------------------

    /** Raises the event's creatures and its hex (once, as its site first loads). */
    public static void stage(ServerLevel level, SealEvent e, int worldStage) {
        e.staged = true;
        RandomSource r = level.random;
        BlockPos c = e.pos;
        if (e.kind.focus && level.getBlockState(c).canBeReplaced() && level.getBlockEntity(c) == null) {
            level.setBlockAndUpdate(c, AllBlocks.SEAL_HEX_FOCUS.get().defaultBlockState());
            e.focus = c;
        }
        switch (e.kind) {
            case WITNESSES -> {
                driver(level, e, AllEntities.DEMON_OCCULTIST.get(), ring(level, c, 2, 0, 1));
                int n = 3 + worldStage / 2;
                for (int i = 0; i < n; i++) extra(level, e, AllEntities.WITNESS.get(), ring(level, c, 6, i, n));
            }
            case REAPERS -> {
                for (int i = 0; i < 3; i++) ward(level, e, AllEntities.SEAL_REAPER.get(), ring(level, c, 4, i, 3));
                driver(level, e, AllEntities.BLACK_EYED_DEMON.get(), ring(level, c, 9, 0, 3));
                driver(level, e, AllEntities.BLACK_EYED_DEMON.get(), ring(level, c, 9, 1, 3));
                driver(level, e, AllEntities.DEMON_OCCULTIST.get(), ring(level, c, 9, 2, 3));
            }
            case CEMETERY -> {
                driver(level, e, AllEntities.DEMON_OCCULTIST.get(), ring(level, c, 2, 0, 1));
                for (int i = 0; i < 4; i++) {
                    extra(level, e, i % 2 == 0 ? EntityType.ZOMBIE : EntityType.SKELETON, ring(level, c, 6, i, 4));
                }
                SealPlagues.graves(level, e);
            }
            case SLAIN_ANGELS -> {
                for (int i = 0; i < 2; i++) ward(level, e, AllEntities.ANGEL.get(), ring(level, c, 3, i, 2));
                for (int i = 0; i < 3; i++) driver(level, e, AllEntities.REBEL_ANGEL.get(), ring(level, c, 9, i, 3));
            }
            case FLEEING_ANGEL -> {
                ward(level, e, AllEntities.ANGEL.get(), ring(level, c, 1, 0, 1));
                for (int i = 0; i < 2; i++) driver(level, e, AllEntities.REBEL_ANGEL.get(), ring(level, c, 12, i, 2));
            }
            case GARRISON -> {
                for (int i = 0; i < 4; i++) ward(level, e, AllEntities.ANGEL.get(), ring(level, c, 3, i, 4));
                for (int i = 0; i < 4; i++) driver(level, e, AllEntities.REBEL_ANGEL.get(), ring(level, c, 11, i, 4));
            }
            case BLOOD_WATER, WITHERED_CROPS, DEAD_LIVESTOCK -> extra(level, e, AllEntities.BLACK_EYED_DEMON.get(), ring(level, c, 3, 0, 1));
            case SWARMS -> {
                for (int i = 0; i < 2; i++) extra(level, e, AllEntities.FLY_SWARM.get(), ring(level, c, 3, i, 2));
            }
            case SACRIFICE -> {
                for (int i = 0; i < 3; i++) bound(level, e, ring(level, c, 2, i, 3), null);
                driver(level, e, AllEntities.DEMON_OCCULTIST.get(), ring(level, c, 4, 0, 2));
                driver(level, e, AllEntities.BLACK_EYED_DEMON.get(), ring(level, c, 4, 1, 2));
            }
            case PRIEST -> {
                bound(level, e, ring(level, c, 1, 0, 1), VillagerProfession.CLERIC);
                for (int i = 0; i < 3; i++) driver(level, e, AllEntities.BLACK_EYED_DEMON.get(), ring(level, c, 7, i, 3));
            }
            case WITCH -> driver(level, e, worldStage >= 3 ? AllEntities.COVEN_WITCH.get() : AllEntities.HEDGE_WITCH.get(), ring(level, c, 2, 0, 1));
        }
        // From the second stage the Host joins in where it is not the angels' own seal: an angel comes for the demons.
        if (worldStage >= SealRules.ANGEL_STAGE && e.kind.family != SealFamily.ANGELS) {
            extra(level, e, AllEntities.ANGEL.get(), ring(level, c, 14, r.nextInt(8), 8));
        }
        e.wardsAtStart = e.wards.size();
        e.wardsAlive = e.wards.size();
    }

    /** {@code i}-th of {@code n} spots on a ring of {@code radius} round {@code c}, on the ground. */
    static Vec3 ring(ServerLevel level, BlockPos c, double radius, int i, int n) {
        double a = (i + 0.5) * Math.PI * 2 / Math.max(1, n) + (c.getX() * 31 + c.getZ()) * 0.01;
        int x = (int) Math.floor(c.getX() + 0.5 + Math.cos(a) * radius), z = (int) Math.floor(c.getZ() + 0.5 + Math.sin(a) * radius);
        BlockPos g = ground(level, x, z);
        if (g == null || Math.abs(g.getY() - c.getY()) > 6) g = new BlockPos(x, c.getY(), z);
        return Vec3.atBottomCenterOf(g);
    }

    static @Nullable Mob raise(ServerLevel level, SealEvent e, EntityType<? extends Mob> type, Vec3 at) {
        Mob m = type.create(level);
        if (m == null) return null;
        m.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        m.setPersistenceRequired();
        m.getPersistentData().putInt(SealEvent.TAG, e.id);
        level.addFreshEntity(m);
        return m;
    }

    static void driver(ServerLevel level, SealEvent e, EntityType<? extends Mob> type, Vec3 at) {
        Mob m = raise(level, e, type, at);
        if (m != null) e.drivers.add(m.getUUID());
    }

    static void ward(ServerLevel level, SealEvent e, EntityType<? extends Mob> type, Vec3 at) {
        Mob m = raise(level, e, type, at);
        if (m != null) e.wards.add(m.getUUID());
    }

    static @Nullable Mob extra(ServerLevel level, SealEvent e, EntityType<? extends Mob> type, Vec3 at) {
        Mob m = raise(level, e, type, at);
        if (m != null) e.extras.add(m.getUUID());
        return m;
    }

    /** A villager bound for the knife (it cannot run): a sacrifice, or the priest. */
    static void bound(ServerLevel level, SealEvent e, Vec3 at, @Nullable VillagerProfession profession) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) return;
        v.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360f, 0);
        v.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        if (profession != null) v.setVillagerData(v.getVillagerData().setProfession(profession).setLevel(2));
        v.setNoAi(true);
        v.setPersistenceRequired();
        v.getPersistentData().putInt(SealEvent.TAG, e.id);
        level.addFreshEntity(v);
        e.wards.add(v.getUUID());
    }

    // --- each second ---------------------------------------------------------------------------------------------------------

    /** The event's creatures, each second, while its site is loaded. */
    public static void tick(ServerLevel level, SealEvent e, List<ServerPlayer> hunters, long now) {
        List<LivingEntity> wards = alive(level, e.wards);
        List<LivingEntity> drivers = alive(level, e.drivers);
        List<LivingEntity> extras = alive(level, e.extras);
        ServerPlayer called = e.owner == null ? null : level.getServer().getPlayerList().getPlayer(e.owner);
        for (LivingEntity d : drivers) {
            if (!(d instanceof Mob m)) continue;
            ServerPlayer near = nearest(hunters, d, ENGAGE);
            if (near != null) {
                if (m.getTarget() == null || !m.getTarget().isAlive()) m.setTarget(near);
            } else if (!wards.isEmpty()) {
                LivingEntity w = nearestOf(wards, d);
                if (m.getTarget() != w) m.setTarget(w);
            } else if (d.distanceToSqr(Vec3.atBottomCenterOf(e.pos)) > 8 * 8) {
                m.getNavigation().moveTo(e.pos.getX() + 0.5, e.pos.getY(), e.pos.getZ() + 0.5, 1.0);
            }
            leash(m, e);
        }
        for (LivingEntity w : wards) {
            if (!(w instanceof Mob m) || m.isNoAi()) continue;
            LivingEntity foe = drivers.isEmpty() ? null : nearestOf(drivers, w);
            if (e.kind == SealKind.FLEEING_ANGEL && foe != null && foe.distanceToSqr(w) < 10 * 10) {
                // It runs from the rebels, towards the hunter if one is there.
                Vec3 away = w.position().subtract(foe.position()).normalize().scale(6);
                ServerPlayer h = nearest(hunters, w, 48);
                Vec3 to = h != null ? h.position() : w.position().add(away);
                m.getNavigation().moveTo(to.x, to.y, to.z, 1.3);
            } else if (foe != null) {
                if (m.getTarget() != foe) m.setTarget(foe);
            }
            leash(m, e);
        }
        for (LivingEntity x : extras) {
            if (!(x instanceof Mob m)) continue;
            if (x instanceof WitnessEntity) {
                LivingEntity prey = called != null && called.isAlive() && called.level() == level && called.distanceToSqr(x) < 64 * 64
                        && !SealRites.warded(called) ? called : nearest(hunters, x, 32);
                if (prey != null && !SealRites.warded(prey) && m.getTarget() != prey) m.setTarget(prey);
            } else if (x.getType() == AllEntities.ANGEL.get() && !drivers.isEmpty()) {
                LivingEntity foe = nearestOf(drivers, x);
                if (m.getTarget() != foe) m.setTarget(foe);
            }
            if (!(x instanceof WitnessEntity)) leash(m, e);
        }
        // Plagues spread, witches call guards, the hex pulses.
        switch (e.kind) {
            case BLOOD_WATER, WITHERED_CROPS, CEMETERY -> {
                if (now % 100 < 20) SealPlagues.step(level, e);
            }
            case DEAD_LIVESTOCK -> {
                if (now % 600 < 20 && e.plagueSteps < LIVESTOCK_MAX) killLivestock(level, e);
            }
            case SWARMS -> {
                if (extras.size() < 2 && now % 200 < 20) extra(level, e, AllEntities.FLY_SWARM.get(), ring(level, e.pos, 3, level.random.nextInt(4), 4));
            }
            case WITCH -> {
                if (now % 800 < 20 && extras.size() < 3 && !drivers.isEmpty()) {
                    extra(level, e, AllEntities.BLACK_EYED_DEMON.get(), ring(level, e.pos, 4, level.random.nextInt(6), 6));
                    drivers.getFirst().playSound(net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
                }
            }
            default -> {
            }
        }
    }

    private static void leash(Mob m, SealEvent e) {
        if (m.getTarget() == null && m.distanceToSqr(Vec3.atBottomCenterOf(e.pos)) > LEASH * LEASH) {
            m.getNavigation().moveTo(e.pos.getX() + 0.5, e.pos.getY(), e.pos.getZ() + 0.5, 1.0);
        }
    }

    /** The plague of the dead livestock: one beast round the hex dies (never a named, leashed or tamed one). */
    static void killLivestock(ServerLevel level, SealEvent e) {
        AABB box = new AABB(e.pos).inflate(16, 6, 16);
        for (Animal a : level.getEntitiesOfClass(Animal.class, box, SealStaging::livestock)) {
            level.sendParticles(org.papiricoh.supernaturalcraft.registry.AllParticles.HEX_MOTE.get(), a.getX(), a.getY() + 0.8, a.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
            a.hurt(level.damageSources().wither(), Float.MAX_VALUE);
            e.plagueSteps++;
            return;
        }
    }

    /** Whether a plague may take {@code a}: farm stock, no name, no leash, no owner, not a seal's own. */
    public static boolean livestock(Animal a) {
        if (a.hasCustomName() || a.isLeashed() || a.getPersistentData().contains(SealEvent.TAG)) return false;
        if (a instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwnerUUID() != null) return false;
        if (a instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h && h.isTamed()) return false;
        EntityType<?> t = a.getType();
        return t == EntityType.COW || t == EntityType.SHEEP || t == EntityType.PIG || t == EntityType.CHICKEN || t == EntityType.GOAT
                || t == EntityType.RABBIT;
    }

    /** Whether a block is a crop a plague can wither. */
    public static boolean crop(BlockState s) {
        return s.getBlock() instanceof CropBlock;
    }

    static List<LivingEntity> alive(ServerLevel level, List<UUID> ids) {
        List<LivingEntity> out = new ArrayList<>();
        for (UUID id : ids) {
            Entity x = level.getEntity(id);
            if (x instanceof LivingEntity l && l.isAlive()) out.add(l);
        }
        return out;
    }

    /** How many of {@code ids} are alive, counting as alive any not found while the site's chunks are not all ticking. */
    static int count(ServerLevel level, List<UUID> ids) {
        int n = 0;
        for (UUID id : ids) {
            Entity x = level.getEntity(id);
            if (x instanceof LivingEntity l && l.isAlive()) n++;
        }
        return n;
    }

    static @Nullable ServerPlayer nearest(List<ServerPlayer> hunters, Entity to, double within) {
        ServerPlayer best = null;
        double bestD = within * within;
        for (ServerPlayer p : hunters) {
            double d = p.distanceToSqr(to);
            if (d < bestD && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    static LivingEntity nearestOf(List<LivingEntity> of, Entity to) {
        LivingEntity best = of.getFirst();
        double bestD = Double.MAX_VALUE;
        for (LivingEntity l : of) {
            double d = l.distanceToSqr(to);
            if (d < bestD) {
                bestD = d;
                best = l;
            }
        }
        return best;
    }

    // --- the end ---------------------------------------------------------------------------------------------------------------

    /**
     * Sends away what the event raised (each in a wisp of smoke) and takes up its hex: {@code held} — the dead wards are left in
     * peace and bound villagers go free; broken — the demons leave with their prize.
     */
    public static void clear(ServerLevel level, SealEvent e, boolean held) {
        for (List<UUID> ids : List.of(e.drivers, e.extras)) {
            for (UUID id : ids) {
                Entity x = level.getEntity(id);
                if (x != null && x.isAlive()) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, x.getX(), x.getY() + 1, x.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
                    x.discard();
                }
            }
        }
        for (UUID id : e.wards) {
            Entity x = level.getEntity(id);
            if (x == null || !x.isAlive()) continue;
            if (x instanceof Villager v) {
                v.setNoAi(false);
                v.getPersistentData().remove(SealEvent.TAG);
            } else {
                // Reapers and angels go back to their work.
                level.sendParticles(org.papiricoh.supernaturalcraft.registry.AllParticles.GRACE.get(), x.getX(), x.getY() + 1, x.getZ(), 12, 0.3, 0.6, 0.3, 0.04);
                x.discard();
            }
        }
        if (e.focus != null && level.isLoaded(e.focus) && level.getBlockState(e.focus).is(AllBlocks.SEAL_HEX_FOCUS.get())) {
            level.setBlockAndUpdate(e.focus, Blocks.AIR.defaultBlockState());
        }
    }

    /** Whether {@code e}'s creature {@code x} counts as the event's (a demon of {@code #demons}, a rebel, a ward…). */
    public static boolean demonic(Entity x) {
        return x.getType().is(AllTags.Entities.DEMONS) || x instanceof RebelAngelEntity;
    }
}
