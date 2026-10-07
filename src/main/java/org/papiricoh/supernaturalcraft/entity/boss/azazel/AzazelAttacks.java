package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Azazel's attacks. His mind throws, drags and shoves (telekinesis); the fire that took Mary runs
 * across the ground and, once the vessel cracks, falls from above; his yellow eyes sicken and mark;
 * and his smoke rides other creatures and, at last, himself.
 */
public final class AzazelAttacks {

    /** How long a possessed creature stays his: longer than any fight should last. */
    public static final int POSSESSION_TICKS = 20 * 60 * 5;

    private AzazelAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Shove::new, 3), opt(Hurl::new, 3), opt(Drag::new, 2), opt(Gaze::new, 2), opt(CeilingFire::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Shove::new, 2), opt(Hurl::new, 3), opt(Drag::new, 1.5f), opt(Gaze::new, 2), opt(CeilingFire::new, 2),
            opt(FallingFire::new, 3), opt(Possession::new, 2), opt(SmokeDash::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return phase <= 1 ? P1 : P2;
    }

    // --- helpers --------------------------------------------------------------------------

    private static AzazelEntity azazel(LuciferEntity boss) {
        return (AzazelEntity) boss;
    }

    /** Whether {@code e} is someone he should hurt (not himself, nor the creatures fighting for him). */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        if (e == boss || !e.isAlive() || e instanceof ArmorStand) return false;
        if (boss.minions().contains(e.getUUID())) return false;
        return !(boss instanceof AzazelEntity a && a.possessed().contains(e.getUUID()));
    }

    private static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    private static boolean inCone(LuciferEntity boss, LivingEntity e, Vec3 dir, double radius) {
        Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
        double len = to.length();
        if (len > radius || Math.abs(e.getY() - boss.getY()) > 4) return false;
        return len < 0.8 || to.normalize().dot(dir) >= 0.5;
    }

    private static void push(LivingEntity e, Vec3 dir, double strength, double up) {
        e.setDeltaMovement(dir.x * strength, up, dir.z * strength);
        e.hurtMarked = true;
    }

    // --- telekinesis ------------------------------------------------------------------------

    /** A shove with his mind: everything in front of him is thrown back. */
    public static class Shove extends BossAttack<LuciferEntity> {
        private static final double RADIUS = 7;
        private float yaw;

        public Shove() {
            super("shove", "shove", 16, 4, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 8 * 8 ? 1.5f : 0.3f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, (float) RADIUS, TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(RADIUS, 3, RADIUS))) {
                if (!inCone(boss, e, dir, RADIUS)) continue;
                hit(boss, e, AllDamageTypes.SPELL, 6);
                push(e, dir, 1.8, 0.55);
            }
            ServerLevel level = level(boss);
            for (int i = 1; i <= 6; i++) {
                Vec3 p = boss.position().add(dir.scale(i));
                level.sendParticles(AllParticles.SIGIL.get(), p.x, p.y + 1, p.z, 4, 0.6, 0.4, 0.6, 0.02);
            }
            sound(boss, SoundEvents.EVOKER_CAST_SPELL, 2f, 0.6f);
        }
    }

    /** He tears blocks out of the ground, holds them up, and throws them one after another. */
    public static class Hurl extends BossAttack<LuciferEntity> {
        private final List<HurledDebris> stones = new ArrayList<>();
        private int thrown;

        public Hurl() {
            super("hurl", "hurl", 24, 16, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.hasLineOfSight(target) ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            ArenaController arena = boss.arena();
            if (arena == null) return;
            int want = boss.phase() >= 2 ? 5 : 3;
            for (int tries = 0; tries < want * 4 && stones.size() < want; tries++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, r = 2 + boss.getRandom().nextDouble() * 2.5;
                int x = (int) Math.floor(boss.getX() + Math.cos(a) * r), z = (int) Math.floor(boss.getZ() + Math.sin(a) * r);
                if (RailTrap.inside(arena, new Vec3(x + 0.5, arena.center().getY(), z + 0.5))) continue;
                BlockPos floor = ArenaTerrain.surface(level, arena, x, z);
                if (floor == null) continue;
                BlockState state = level.getBlockState(floor);
                if (state.getDestroySpeed(level, floor) < 0 || state.is(AllTags.Blocks.ARENA_IMMUNE)) continue;
                if (!arena.mutate(level, floor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 0)) continue;
                HurledDebris d = HurledDebris.of(level, boss, state, Vec3.atBottomCenterOf(floor), 6);
                d.setNoGravity(true);
                d.setDeltaMovement(0, 0.09, 0);
                level.addFreshEntity(d);
                stones.add(d);
                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state),
                        floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.05);
            }
            TelegraphMarker.line(level, boss.position(), yawTo(boss.position(), target.position()), 2f,
                    (float) Math.min(20, boss.distanceTo(target) + 3), TelegraphMarker.VIOLET, windup + 4);
            sound(boss, SoundEvents.GRAVEL_BREAK, 2f, 0.5f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            for (HurledDebris d : stones) {
                if (t > 14) d.setDeltaMovement(0, 0, 0);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0 || thrown >= stones.size()) return;
            HurledDebris d = stones.get(thrown++);
            if (d.isRemoved()) return;
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(d.position());
            d.setNoGravity(false);
            d.setDeltaMovement(aim.normalize().scale(1.15).add(0, Math.min(0.25, aim.length() * 0.012), 0));
            sound(boss, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.6f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            for (int i = thrown; i < stones.size(); i++) {
                HurledDebris d = stones.get(i);
                if (!d.isRemoved()) {
                    d.setNoGravity(false);
                    d.setDeltaMovement(0, -0.2, 0);
                }
            }
        }
    }

    /** He reaches out a hand and pulls his target across the ground to him, then strikes them aside. */
    public static class Drag extends BossAttack<LuciferEntity> {
        public Drag() {
            super("drag", "drag", 20, 14, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double d = boss.distanceTo(target);
            return d > 5 && d < 18 && boss.hasLineOfSight(target) ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.line(level(boss), boss.position(), yawTo(boss.position(), target.position()), 1.4f,
                    (float) boss.distanceTo(target) + 1, TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0) return;
            Vec3 a = boss.position().add(0, 1.3, 0), b = target.position().add(0, 1, 0);
            for (int i = 0; i < 8; i++) {
                Vec3 p = a.lerp(b, i / 8.0);
                level(boss).sendParticles(AllParticles.SIGIL.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t == 1 && boss.hasLineOfSight(target) && boss.distanceTo(target) < 20) {
                Vec3 to = boss.position().subtract(target.position()).multiply(1, 0, 1);
                double d = to.length();
                push(target, to.normalize(), Math.min(2.4, d * 0.22), 0.35);
                sound(boss, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5f, 0.6f);
            }
            if (t == 9 && boss.distanceTo(target) < 3.8 && isFoe(boss, target)) {
                Vec3 side = dirOf(boss.getYRot() + 90);
                hit(boss, target, AllDamageTypes.SPELL, 7);
                push(target, side, 1.2, 0.4);
                sound(boss, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5f, 0.6f);
            }
        }
    }

    // --- the yellow eyes --------------------------------------------------------------------

    /** His eyes burn yellow: whoever meets them is sickened, half-blinded and marked. */
    public static class Gaze extends BossAttack<LuciferEntity> {
        private float yaw;

        public Gaze() {
            super("gaze", "gaze", 30, 6, 16);
        }

        private static double radius(LuciferEntity boss) {
            return boss.phase() >= 2 ? 12 : 10;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, (float) radius(boss), TelegraphMarker.YELLOW, windup);
            sound(boss, AllSounds.AZAZEL_GAZE.get(), 2f, 0.8f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 4 == 0) {
                level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), boss.getX(), boss.getEyeY(), boss.getZ(), 2, 0.15, 0.05, 0.15, 0.0);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            double r = radius(boss);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(r, 4, r))) {
                if (!inCone(boss, e, dir, r)) continue;
                hit(boss, e, AllDamageTypes.SPELL, 3);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), boss);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), boss);
                e.addEffect(new MobEffectInstance(AllMobEffects.MARKED, 160, 0), boss);
                level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), e.getX(), e.getEyeY(), e.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
            }
            sound(boss, AllSounds.AZAZEL_LAUGH.get(), 1.5f, 1.1f);
        }
    }

    // --- fire --------------------------------------------------------------------------------

    /** The fire on the nursery ceiling, brought down: lines of flame that race out across the ground. */
    public static class CeilingFire extends BossAttack<LuciferEntity> {
        private static final double LENGTH = 16, SPEED = 0.6;
        private final List<Vec3> dirs = new ArrayList<>();
        private final Set<UUID> struck = new HashSet<>();
        private Vec3 origin = Vec3.ZERO;

        public CeilingFire() {
            super("ceiling_fire", "fire", 26, 28, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            origin = boss.position();
            int lines = boss.phase() >= 2 ? 5 : 3;
            float base = yawTo(boss.position(), target.position());
            for (int i = 0; i < lines; i++) {
                float yaw = base + (i - (lines - 1) / 2f) * (lines > 3 ? 22f : 28f);
                dirs.add(dirOf(yaw));
                TelegraphMarker.line(level(boss), origin, yaw, 1.8f, (float) LENGTH, TelegraphMarker.RED, windup + 28);
            }
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0) return;
            for (Vec3 d : dirs) {
                Vec3 p = origin.add(d.scale(boss.getRandom().nextDouble() * LENGTH));
                level(boss).sendParticles(AllParticles.HELLFIRE.get(), p.x, p.y + 5, p.z, 1, 0.3, 0.2, 0.3, 0.0);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            double front = Math.min(LENGTH, t * SPEED);
            ServerLevel level = level(boss);
            for (Vec3 d : dirs) {
                Vec3 p = floorAt(boss, origin.add(d.scale(front)));
                if (t % 2 == 0) level.addFreshEntity(new FlameTrail(level, boss, p.x, p.y, p.z));
                level.sendParticles(AllParticles.HELLFIRE.get(), p.x, p.y + 0.3, p.z, 4, 0.3, 0.4, 0.3, 0.03);
                for (LivingEntity e : foes(boss, new AABB(p, p).inflate(1.2, 2, 1.2))) {
                    if (struck.add(e.getUUID())) {
                        hit(boss, e, AllDamageTypes.HELLFIRE, 7);
                        e.igniteForSeconds(3);
                    }
                }
            }
            if (t % 6 == 0) sound(boss, SoundEvents.FIRECHARGE_USE, 1.2f, 0.7f);
        }
    }

    /** Once the vessel cracks: fire falls out of the sky onto every hunter, and around them. */
    public static class FallingFire extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 2.0f;
        private final List<Vec3> spots = new ArrayList<>();

        public FallingFire() {
            super("falling_fire", "fire", 30, 8, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) spots.add(p.position());
            int extra = 4 + boss.getRandom().nextInt(3) - Math.min(3, spots.size());
            for (int i = 0; i < extra; i++) spots.add(randomArenaPoint(boss, 0.85));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, RADIUS, TelegraphMarker.RED, windup + 4);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 2 != 0) return;
            for (Vec3 s : spots) {
                double y = 12 - t * 0.35;
                level(boss).sendParticles(AllParticles.HELLFIRE.get(), s.x, s.y + y, s.z, 2, 0.3, 0.2, 0.3, 0.0);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (Vec3 s : spots) {
                column(level, s, AllParticles.HELLFIRE.get(), 7, 20);
                level.sendParticles(ParticleTypes.LAVA, s.x, s.y + 0.2, s.z, 5, 0.6, 0.1, 0.6, 0);
                for (LivingEntity e : foes(boss, new AABB(s, s).inflate(RADIUS, 4, RADIUS))) {
                    if (inCircle(e, s, RADIUS)) {
                        hit(boss, e, AllDamageTypes.HELLFIRE, 9);
                        e.igniteForSeconds(4);
                    }
                }
            }
            sound(boss, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 1.3f);
        }
    }

    // --- smoke -------------------------------------------------------------------------------

    /** His smoke goes into the nearest monsters and makes them his; where there are none, he calls demons up. */
    public static class Possession extends BossAttack<LuciferEntity> {
        private static final int MAX = 3;
        private final List<LivingEntity> hosts = new ArrayList<>();
        private final List<Vec3> spawns = new ArrayList<>();

        public Possession() {
            super("possession", "possess", 30, 10, 20);
        }

        private static int servants(LuciferEntity boss) {
            AzazelEntity a = azazel(boss);
            ServerLevel level = level(boss);
            a.possessed().removeIf(id -> !(level.getEntity(id) instanceof LivingEntity l) || !l.isAlive() || !l.hasEffect(AllMobEffects.POSSESSED));
            boss.minions().removeIf(id -> level.getEntity(id) == null || !level.getEntity(id).isAlive());
            return a.possessed().size() + boss.minions().size();
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return servants(boss) < MAX ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int room = MAX - servants(boss);
            ArenaController arena = boss.arena();
            List<Monster> near = level.getEntitiesOfClass(Monster.class, boss.getBoundingBox().inflate(16, 6, 16), m ->
                    m.isAlive() && !(m instanceof LuciferEntity) && !m.getType().is(AllTags.Entities.BOSSES)
                            && !m.hasEffect(AllMobEffects.POSSESSED) && (arena == null || arena.contains(m.position())));
            for (Monster m : near) {
                if (hosts.size() >= Math.min(2, room)) break;
                hosts.add(m);
                TelegraphMarker.circle(level, m.position(), 1.0f, TelegraphMarker.YELLOW, windup + 4);
            }
            for (int i = hosts.size(); i < room; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Vec3 s = floorAt(boss, boss.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
                if (arena != null && RailTrap.inside(arena, s)) s = floorAt(boss, boss.position().add(-Math.cos(a) * 4, 0, -Math.sin(a) * 4));
                spawns.add(s);
                TelegraphMarker.circle(level, s, 1.0f, TelegraphMarker.YELLOW, windup + 4);
            }
            sound(boss, AllSounds.AZAZEL_SMOKE.get(), 2f, 0.7f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0) return;
            Vec3 from = boss.position().add(0, 1.6, 0);
            List<Vec3> to = new ArrayList<>(spawns);
            for (LivingEntity h : hosts) to.add(h.position().add(0, h.getBbHeight() * 0.8, 0));
            double k = Math.min(1, t / (double) windup);
            for (Vec3 p : to) {
                Vec3 at = from.lerp(p, k);
                level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.01);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            AzazelEntity a = azazel(boss);
            for (LivingEntity h : hosts) {
                if (!h.isAlive()) continue;
                h.addEffect(new MobEffectInstance(AllMobEffects.POSSESSED, POSSESSION_TICKS, 0, false, false, true), boss);
                a.possessed().add(h.getUUID());
                if (h instanceof net.minecraft.world.entity.Mob m) m.setTarget(target);
                level.sendParticles(AllParticles.YELLOW_SMOKE.get(), h.getX(), h.getEyeY(), h.getZ(), 20, 0.2, 0.3, 0.2, 0.05);
            }
            for (Vec3 s : spawns) {
                BlackEyedDemon demon = AllEntities.BLACK_EYED_DEMON.get().create(level);
                if (demon == null) continue;
                demon.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                demon.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                demon.addEffect(new MobEffectInstance(AllMobEffects.POSSESSED, POSSESSION_TICKS, 0, false, false, true), boss);
                demon.setTarget(target);
                level.addFreshEntity(demon);
                boss.minions().add(demon.getUUID());
                column(level, s, AllParticles.YELLOW_SMOKE.get(), 3, 12);
            }
        }
    }

    /**
     * He goes to smoke and rushes straight through his target, untouchable. He goes in a straight
     * line and does not swerve: a rush across charged rails ends with him held in them.
     */
    public static class SmokeDash extends BossAttack<LuciferEntity> {
        private static final double SPEED = 0.9;
        private final Set<UUID> struck = new HashSet<>();
        private Vec3 dir = Vec3.ZERO;
        private double length, travelled;

        public SmokeDash() {
            super("smoke_dash", "smoke_dash", 24, 40, 24);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) > 5 * 5 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = yawTo(boss.position(), target.position());
            dir = dirOf(yaw);
            length = Math.min(24, boss.distanceTo(target) + 6);
            TelegraphMarker.line(level(boss), boss.position(), yaw, 2.0f, (float) length, TelegraphMarker.YELLOW, windup + 6);
            sound(boss, AllSounds.AZAZEL_SMOKE.get(), 2f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            azazel(boss).setSmoke(true);
            level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
            sound(boss, AllSounds.AZAZEL_SMOKE.get(), 2.5f, 0.5f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (!azazel(boss).isSmoke()) return;
            boss.setDeltaMovement(dir.x * SPEED, boss.getDeltaMovement().y, dir.z * SPEED);
            boss.setYRot(yawTo(Vec3.ZERO, dir));
            travelled += SPEED;
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(1.2, 0.5, 1.2))) {
                if (struck.add(e.getUUID())) {
                    hit(boss, e, AllDamageTypes.SPELL, 8);
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0), boss);
                }
            }
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return travelled >= length || !azazel(boss).isSmoke();
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            AzazelEntity a = azazel(boss);
            if (a.isSmoke()) {
                a.setSmoke(false);
                level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            }
            boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
        }
    }

    /** He is simply somewhere else: behind his target, or anywhere out of the rails. */
    public static class Blink extends BossAttack<LuciferEntity> {
        public Blink() {
            super("blink", "blink", 10, 2, 10);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 1, 0.4, 0.02);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            Vec3 at = floorAt(boss, target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(3)));
            for (int i = 0; i < 8 && arena != null && (RailTrap.inside(arena, at) || !arena.contains(at)); i++) {
                at = randomArenaPoint(boss, 0.8);
            }
            if (arena != null && RailTrap.inside(arena, at)) return;
            level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 1, 0.4, 0.05);
            boss.teleportTo(at.x, at.y, at.z);
            level(boss).sendParticles(AllParticles.YELLOW_SMOKE.get(), at.x, at.y + 1, at.z, 30, 0.4, 1, 0.4, 0.05);
            sound(boss, AllSounds.AZAZEL_SMOKE.get(), 2f, 0.9f);
        }
    }
}
