package org.papiricoh.supernaturalcraft.entity.boss.cain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.CainFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.flatDistance;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.randomArenaPoint;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Cain's attacks (v0.21). Every one is shown on the ground in its windup and lands only in ACTIVE: red for his blows (lanes,
 * the furrow's cone, Abel's blood ring, the execution's circle, the Mark's flare), yellow for the swarm, violet for the tools
 * he hurls with his mind, green for the shelters from the flare.
 * <ul>
 *   <li>P1, the Beekeeper: {@link Lunge} (three dashes down red lanes), {@link Swarm} (yellow circles: bees that sting,
 *   poison and blind).</li>
 *   <li>P2, the Mark awakens: {@link Furrow} (a ploughing swing, red cone), {@link AbelsBlood} (spikes ring out of the earth
 *   round him, red ring, the ground by him safe), {@link ThrowTools} (violet lanes), and the lunge still.</li>
 *   <li>P3, the Father of Murder: {@link Execution} (a red circle where you stand; he is behind it and the blade comes down),
 *   {@link MarkFlare} (the whole yard burns red but for green shelters by his hives), and the P2 blows.</li>
 * </ul>
 */
public final class CainAttacks {

    private CainAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Lunge::new, 3), opt(Swarm::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Furrow::new, 3), opt(AbelsBlood::new, 2.5f), opt(ThrowTools::new, 2.5f), opt(Lunge::new, 1.5f), opt(Swarm::new, 0.8f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Execution::new, 3), opt(MarkFlare::new, 2), opt(Furrow::new, 2), opt(AbelsBlood::new, 1.5f), opt(ThrowTools::new, 1.5f),
            opt(Lunge::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static CainEntity cain(LuciferEntity boss) {
        return (CainEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether his blows should land on {@code e}: not himself, nobody in creative or spectating. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof CainEntity)
                && !(e instanceof org.papiricoh.supernaturalcraft.cain.CainNpcEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    /** Everyone his blows may find in {@code box}, his tracked hunters included (tests' fake players are not found by a search). */
    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : cain(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    /** The hunters to spread an attack over: every challenger (and the tracked ones), or the target alone. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(cain(boss).hunters());
        if (!out.contains(target)) out.add(target);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} along {@code dir}. */
    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    /** Everyone in a 120° cone {@code reach} deep from {@code from} facing {@code yaw}. */
    static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(60));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 3.5 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

    /** How far from {@code from} along {@code dir} he can go and stay well inside his arena (at most {@code max}). */
    static double reachInArena(LuciferEntity boss, Vec3 from, Vec3 dir, double max) {
        ArenaController arena = boss.arena();
        if (arena == null) return max;
        double limit = arena.radius() - 2.5;
        double d = max;
        while (d > 1 && arena.horizontalDistance(from.add(dir.scale(d))) > limit) d -= 0.5;
        return d;
    }

    /**
     * An attack whose clip is cued so its blow falls at the end of the windup (Raphael's {@link RaphaelAttacks.Cued}, with
     * Cain's cue from {@link CainAssets#HIT_TICKS}).
     */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, CainBalance.cue(clip), windup, active, recover);
        }
    }

    // --- P1 · the Beekeeper ----------------------------------------------------------------------------------------------

    /**
     * Three dashes, each down a red lane to where the hunter stood when it was drawn. The first lane shows through the windup;
     * each next one is drawn as the dash before it lands, {@link CainBalance#LUNGE_SPACING} ticks ahead of its own.
     */
    public static class Lunge extends Cued {
        private final float[] yaws = new float[CainBalance.LUNGES];
        private final Vec3[] froms = new Vec3[CainBalance.LUNGES];
        private final double[] lengths = new double[CainBalance.LUNGES];

        public Lunge() {
            super("lunge", "lunge", CainBalance.LUNGE_WINDUP, (CainBalance.LUNGES - 1) * CainBalance.LUNGE_SPACING + 2, 22);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            draw(boss, target, 0, boss.position(), windupLength(boss) + 2);
            sound(boss, AllSounds.cain("lunge"), 1.2f, 0.8f);
        }

        /** Draws dash {@code k}'s lane from {@code from} toward the target. */
        void draw(LuciferEntity boss, LivingEntity target, int k, Vec3 from, int show) {
            float yaw = yawTo(from, target.position());
            Vec3 dir = dirOf(yaw);
            double len = reachInArena(boss, from, dir, CainBalance.LUNGE_REACH);
            yaws[k] = yaw;
            froms[k] = from;
            lengths[k] = len;
            TelegraphMarker.line(level(boss), from, yaw, CainBalance.LUNGE_WIDTH, (float) len, TelegraphMarker.RED, show);
            if (k == 0) face(boss, target.position());
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            dash(boss, 0);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            int s = CainBalance.LUNGE_SPACING;
            for (int k = 1; k < CainBalance.LUNGES; k++) {
                int at = k * s;
                if (t == at - s + 1) draw(boss, target, k, boss.position(), s);
                if (t == at - CainBalance.cue("lunge")) boss.triggerAnim("action", "lunge");
                if (t == at) dash(boss, k);
            }
        }

        void dash(LuciferEntity boss, int k) {
            if (froms[k] == null) return;
            Vec3 dir = dirOf(yaws[k]);
            for (LivingEntity e : inLane(boss, froms[k], dir, lengths[k], CainBalance.LUNGE_WIDTH / 2)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, CainBalance.LUNGE_DAMAGE);
                e.push(dir.x * 0.8, 0.35, dir.z * 0.8);
                e.hurtMarked = true;
            }
            Vec3 end = floorAt(boss, froms[k].add(dir.scale(lengths[k])));
            boss.setYRot(yaws[k]);
            boss.setYBodyRot(yaws[k]);
            boss.teleportTo(end.x, end.y, end.z);
            boss.getNavigation().stop();
            cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.LUNGE, k, 0, end, 10));
            sound(boss, AllSounds.cain("lunge"), 1.5f, 1.0f + k * 0.1f);
        }
    }

    /** The bees rise: a yellow circle on each hunter (and one more), then the swarm stings everyone in them every second. */
    public static class Swarm extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Swarm() {
            super("swarm", "swarm", CainBalance.SWARM_WINDUP, CainBalance.SWARM_TICKS, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) if (spots.size() < CainBalance.SWARM_MAX) spots.add(floorAt(boss, e.position()));
            spots.add(randomArenaPoint(boss, 0.6));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, CainBalance.SWARM_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
            sound(boss, AllSounds.cain("swarm"), 1.2f, 1.0f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : spots) {
                cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.SWARM, Math.round(CainBalance.SWARM_RADIUS), 0, s, CainBalance.SWARM_TICKS));
            }
            sting(boss);
            sound(boss, AllSounds.cain("bees"), 1.5f, 1.0f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 20 == 0) sting(boss);
        }

        void sting(LuciferEntity boss) {
            for (Vec3 s : spots) {
                for (LivingEntity e : foes(boss, new AABB(s, s).inflate(CainBalance.SWARM_RADIUS + 1, 4, CainBalance.SWARM_RADIUS + 1))) {
                    if (!inCircle(e, s, CainBalance.SWARM_RADIUS)) continue;
                    hit(boss, e, DamageTypes.STING, CainBalance.SWARM_DAMAGE);
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, CainBalance.SWARM_POISON, 0), boss);
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CainBalance.SWARM_BLIND, 0), boss);
                }
            }
        }
    }

    // --- P2 · the Mark awakens -------------------------------------------------------------------------------------------

    /** A ploughing swing: a red cone before him, the earth torn up in it (for a while) and whoever stands there thrown. */
    public static class Furrow extends Cued {
        private float yaw;

        public Furrow() {
            super("furrow", "furrow", CainBalance.FURROW_WINDUP, 2, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < CainBalance.FURROW_REACH * CainBalance.FURROW_REACH ? 1 : 0.15f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, CainBalance.FURROW_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 from = boss.position();
            for (LivingEntity e : inCone(boss, from, yaw, CainBalance.FURROW_REACH)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, CainBalance.FURROW_DAMAGE);
                Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
                away = away.lengthSqr() < 0.01 ? dirOf(yaw) : away.normalize();
                e.push(away.x * 0.6, CainBalance.FURROW_LIFT, away.z * 0.6);
                e.hurtMarked = true;
            }
            scar(boss, from, yaw);
            cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.FURROW, Math.round(yaw), Math.round(CainBalance.FURROW_REACH), from, 20));
            sound(boss, AllSounds.cain("furrow"), 2f, 0.9f);
        }

        /** The torn earth: grass and farmland in the cone turned over for a while (the arena puts it back). */
        static int scar(LuciferEntity boss, Vec3 from, float yaw) {
            ArenaController arena = boss.arena();
            if (arena == null) return 0;
            ServerLevel level = level(boss);
            Vec3 dir = dirOf(yaw);
            double cos = Math.cos(Math.toRadians(60));
            int n = 0, r = Mth.ceil(CainBalance.FURROW_REACH);
            for (int dx = -r; dx <= r && n < 60; dx++) {
                for (int dz = -r; dz <= r && n < 60; dz++) {
                    if ((dx + dz) % 2 != 0) continue;
                    Vec3 to = new Vec3(dx, 0, dz);
                    double d = to.length();
                    if (d < 1 || d > CainBalance.FURROW_REACH || to.normalize().dot(dir) < cos) continue;
                    BlockPos floor = ArenaTerrain.surface(level, arena, Mth.floor(from.x) + dx, Mth.floor(from.z) + dz);
                    if (floor == null) continue;
                    BlockState s = level.getBlockState(floor);
                    if (!s.is(Blocks.GRASS_BLOCK) && !s.is(Blocks.FARMLAND) && !s.is(Blocks.DIRT) && !s.is(Blocks.DIRT_PATH)) continue;
                    if (arena.mutate(level, floor, Blocks.ROOTED_DIRT.defaultBlockState(), CainBalance.FURROW_SCAR_TICKS)) n++;
                }
            }
            return n;
        }
    }

    /**
     * Abel's blood cries out of the ground: a red ring round him, the ground right by him (green) safe; spikes burst up between
     * the two radii (they sink again after a while: the arena reverts them) and whoever stands there is run through.
     */
    public static class AbelsBlood extends Cued {
        private Vec3 centre;

        public AbelsBlood() {
            super("abels_blood", "abels_blood", CainBalance.ABEL_WINDUP, 2, 22);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            centre = boss.position();
            face(boss, target.position());
            int show = windupLength(boss) + 4;
            TelegraphMarker.ring(level(boss), centre, CainBalance.ABEL_OUTER, TelegraphMarker.RED, show);
            TelegraphMarker.circle(level(boss), centre, CainBalance.ABEL_INNER, TelegraphMarker.SAFE, show);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : foes(boss, new AABB(centre, centre).inflate(CainBalance.ABEL_OUTER + 1, 4, CainBalance.ABEL_OUTER + 1))) {
                if (Math.abs(e.getY() - centre.y) > 3.5 || !CainBalance.inAbelsRing(flatDistance(e.position(), centre))) continue;
                hit(boss, e, DamageTypes.MOB_ATTACK, CainBalance.ABEL_DAMAGE);
                e.push(0, 0.7, 0);
                e.hurtMarked = true;
            }
            spikes(boss, centre);
            cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.SPIKES, Math.round(CainBalance.ABEL_OUTER * 10),
                    Math.round(CainBalance.ABEL_INNER * 10), centre, CainBalance.ABEL_SPIKE_TICKS));
            sound(boss, AllSounds.cain("abels_blood"), 2f, 0.8f);
        }

        /** The spikes: pointed dripstone out of reddened earth, every other block of the ring. @return how many */
        static int spikes(LuciferEntity boss, Vec3 centre) {
            ArenaController arena = boss.arena();
            if (arena == null) return 0;
            ServerLevel level = level(boss);
            BlockState spike = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.UP).setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
            int n = 0, r = Mth.ceil(CainBalance.ABEL_OUTER);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.floorMod(dx + dz, 2) != 0) continue;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (!CainBalance.inAbelsRing(d)) continue;
                    BlockPos floor = ArenaTerrain.surface(level, arena, Mth.floor(centre.x) + dx, Mth.floor(centre.z) + dz);
                    if (floor == null) continue;
                    BlockPos above = floor.above();
                    BlockState over = level.getBlockState(above);
                    if (!over.isAir() && !over.canBeReplaced()) continue;
                    if (arena.mutate(level, above, spike, CainBalance.ABEL_SPIKE_TICKS)) {
                        arena.mutate(level, floor, Blocks.ROOTED_DIRT.defaultBlockState(), CainBalance.ABEL_SPIKE_TICKS);
                        n++;
                    }
                }
            }
            level.sendParticles(AllParticles.HELLFIRE.get(), centre.x, centre.y + 0.3, centre.z, 30, r / 2.0, 0.2, r / 2.0, 0.02);
            return n;
        }
    }

    /** He hurls his farm tools with his mind: a violet lane to each hunter, and the tools fly down them. */
    public static class ThrowTools extends Cued {
        private final List<Vec3> froms = new ArrayList<>();
        private final List<Float> yaws = new ArrayList<>();
        private final List<Double> lengths = new ArrayList<>();

        public ThrowTools() {
            super("throw_tools", "throw_tools", CainBalance.TOOLS_WINDUP, CainBalance.TOOLS_FLIGHT + 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            Vec3 from = boss.position();
            List<LivingEntity> marks = new ArrayList<>(marks(boss, target));
            marks.sort(Comparator.comparingDouble(e -> e.distanceToSqr(boss)));
            for (LivingEntity e : marks) {
                if (froms.size() >= CainBalance.TOOLS_MAX) break;
                float yaw = yawTo(from, e.position());
                double len = Math.min(CainBalance.TOOLS_MAX_REACH, flatDistance(from, e.position()) + 3);
                froms.add(from);
                yaws.add(yaw);
                lengths.add(len);
                TelegraphMarker.line(level(boss), from, yaw, CainBalance.TOOLS_WIDTH, (float) len, TelegraphMarker.VIOLET, windupLength(boss) + CainBalance.TOOLS_FLIGHT);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < froms.size(); i++) {
                Vec3 land = froms.get(i).add(dirOf(yaws.get(i)).scale(lengths.get(i)));
                cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.TOOLS, i % 4, 0, land, CainBalance.TOOLS_FLIGHT));
            }
            sound(boss, AllSounds.cain("throw"), 1.6f, 1.0f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != CainBalance.TOOLS_FLIGHT) return;
            List<LivingEntity> struck = new ArrayList<>();
            for (int i = 0; i < froms.size(); i++) {
                for (LivingEntity e : inLane(boss, froms.get(i), dirOf(yaws.get(i)), lengths.get(i), CainBalance.TOOLS_WIDTH / 2)) {
                    if (struck.contains(e)) continue;
                    struck.add(e);
                    hit(boss, e, AllDamageTypes.SPELL, CainBalance.TOOLS_DAMAGE);
                }
            }
        }
    }

    // --- P3 · the Father of Murder ---------------------------------------------------------------------------------------

    /** A red circle where the hunter stands; then he is behind that spot and the First Blade comes down on it. */
    public static class Execution extends Cued {
        private Vec3 spot;
        private Vec3 behind;
        private int victim = -1;

        public Execution() {
            super("execution", "execution", CainBalance.EXECUTION_WINDUP, 2, 24);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spot = floorAt(boss, target.position());
            victim = target.getId();
            Vec3 look = target.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1e-4) look = spot.subtract(boss.position()).multiply(1, 0, 1);
            look = look.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look.normalize();
            behind = floorAt(boss, spot.subtract(look.scale(CainBalance.EXECUTION_BEHIND)));
            ArenaController arena = boss.arena();
            if (arena != null && arena.horizontalDistance(behind) > arena.radius() - 1.5) behind = spot;
            TelegraphMarker.circle(level(boss), spot, CainBalance.EXECUTION_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
            sound(boss, AllSounds.cain("execution"), 1.0f, 0.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1, boss.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            boss.teleportTo(behind.x, behind.y, behind.z);
            boss.getNavigation().stop();
            face(boss, spot);
            cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.EXECUTION, victim, 0, behind, 16));
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(CainBalance.EXECUTION_RADIUS + 1, 4, CainBalance.EXECUTION_RADIUS + 1))) {
                if (inCircle(e, spot, CainBalance.EXECUTION_RADIUS)) hit(boss, e, DamageTypes.MOB_ATTACK, CainBalance.EXECUTION_DAMAGE);
            }
            sound(boss, AllSounds.cain("execution"), 2f, 1.0f);
        }
    }

    /**
     * The Mark blazes: the whole yard (a red ring at its edge) burns, but for green shelters at its edge toward his hives (or
     * scattered, away from his farm).
     */
    public static class MarkFlare extends Cued {
        private Vec3 centre;
        private double radius;
        private final List<Vec3> shelters = new ArrayList<>();

        public MarkFlare() {
            super("mark_flare", "mark_flare", CainBalance.FLARE_WINDUP, 2, 26);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ArenaController arena = boss.arena();
            centre = arena != null ? floorAt(boss, arena.centerVec()) : boss.position();
            radius = CainBalance.flareRadius(arena != null ? arena.radius() : 0);
            shelters.addAll(shelters(boss, centre, radius));
            int show = windupLength(boss) + 4;
            TelegraphMarker.ring(level(boss), centre, (float) radius, TelegraphMarker.RED, show);
            for (Vec3 s : shelters) TelegraphMarker.circle(level(boss), s, CainBalance.FLARE_SAFE_RADIUS, TelegraphMarker.SAFE, show);
            cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.MARK_FLARE, (int) Math.round(radius * 10), shelters.size(), centre, show));
            for (int i = 0; i < shelters.size(); i++) {
                cain(boss).fx(new CainFxPayload(boss.getId(), CainFxPayload.FLARE_SHELTER, i, Math.round(CainBalance.FLARE_SAFE_RADIUS * 10),
                        shelters.get(i), show));
            }
            sound(boss, AllSounds.cain("mark_flare"), 2f, 0.7f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : foes(boss, new AABB(centre, centre).inflate(radius + 1, 6, radius + 1))) {
                double nearest = Double.MAX_VALUE;
                for (Vec3 s : shelters) nearest = Math.min(nearest, flatDistance(e.position(), s));
                if (CainBalance.flareHits(flatDistance(e.position(), centre), nearest, radius)) {
                    hit(boss, e, AllDamageTypes.HELLFIRE, CainBalance.FLARE_DAMAGE);
                }
            }
            ServerLevel level = level(boss);
            level.sendParticles(AllParticles.HELLFIRE.get(), centre.x, centre.y + 0.5, centre.z, 200, radius / 2, 0.4, radius / 2, 0.05);
            sound(boss, AllSounds.cain("mark_flare"), 3f, 1.0f);
        }
    }

    /** The flare's shelters: at the arena's edge toward his nearest hives, or spread round the yard if he has none. */
    public static List<Vec3> shelters(LuciferEntity boss, Vec3 centre, double radius) {
        List<Vec3> out = new ArrayList<>();
        double reach = Math.max(3, radius - 3.5);
        List<BlockPos> hives = new ArrayList<>(cain(boss).hives());
        hives.sort(Comparator.comparingDouble(h -> flatDistance(Vec3.atCenterOf(h), centre)));
        for (BlockPos h : hives) {
            if (out.size() >= CainBalance.FLARE_SHELTERS) break;
            Vec3 dir = Vec3.atCenterOf(h).subtract(centre).multiply(1, 0, 1);
            if (dir.lengthSqr() < 1e-4) continue;
            Vec3 at = floorAt(boss, centre.add(dir.normalize().scale(reach)));
            if (out.stream().noneMatch(o -> flatDistance(o, at) < CainBalance.FLARE_SAFE_RADIUS * 2)) out.add(at);
        }
        double base = boss.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; out.size() < CainBalance.FLARE_SHELTERS && i < CainBalance.FLARE_SHELTERS * 2; i++) {
            double a = base + i * Math.PI * 2 / CainBalance.FLARE_SHELTERS;
            double r = radius * CainBalance.FLARE_SHELTER_REACH;
            Vec3 at = floorAt(boss, centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (out.stream().noneMatch(o -> flatDistance(o, at) < CainBalance.FLARE_SAFE_RADIUS * 2)) out.add(at);
        }
        return out;
    }
}
