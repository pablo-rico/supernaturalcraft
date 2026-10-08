package org.papiricoh.supernaturalcraft.entity.boss.lucifer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.projectile.BossShard;
import org.papiricoh.supernaturalcraft.entity.projectile.HellfireBolt;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Every attack Lucifer knows, grouped by the phase that introduces it. Each one warns on the
 * ground during its wind-up and only hurts in its active stage.
 */
public final class LuciferAttacks {

    private LuciferAttacks() {
    }

    // --- pools ----------------------------------------------------------------------------

    private static AttackScheduler.Option<LuciferEntity> opt(java.util.function.Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Snap::new, 3), opt(Fling::new, 2), opt(Grasp::new, 3), opt(Legion::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Snap::new, 1.5f), opt(WingSweep::new, 3), opt(Rain::new, 2), opt(Fissure::new, 2), opt(Leap::new, 2), opt(Legion::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Cage::new, 2), opt(Beam::new, 2), opt(Illusion::new, 1), opt(Collapse::new, 2), opt(Fissure::new, 1), opt(Leap::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(
            opt(Storm::new, 3), opt(Judgement::new, 2), opt(Drain::new, 2), opt(Rain::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    // --- helpers --------------------------------------------------------------------------

    public static ServerLevel level(LuciferEntity boss) {
        return (ServerLevel) boss.level();
    }

    /** One of his blows: {@code amount} times his multiplier, part of it as Divine Wrath ({@link BossStrike}). */
    public static boolean hit(LuciferEntity boss, Entity victim, ResourceKey<DamageType> type, float amount) {
        return BossStrike.deal(boss, victim, type, amount * boss.attackDamageMultiplier());
    }

    public static double flatDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static boolean inCircle(Entity e, Vec3 c, double r) {
        return flatDistance(e.position(), c) <= r && Math.abs(e.getY() - c.y) < 3.5;
    }

    /** Minecraft yaw that faces from {@code from} toward {@code to}. */
    public static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }

    public static Vec3 dirOf(float yaw) {
        return new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
    }

    public static Vec3 floorAt(LuciferEntity boss, Vec3 p) {
        ArenaController arena = boss.arena();
        if (arena != null) {
            BlockPos s = ArenaTerrain.surface(level(boss), arena, Mth.floor(p.x), Mth.floor(p.z));
            if (s != null) return new Vec3(p.x, s.getY() + 1, p.z);
        }
        return p;
    }

    public static Vec3 randomArenaPoint(LuciferEntity boss, double maxFraction) {
        ArenaController arena = boss.arena();
        Vec3 c = arena != null ? arena.centerVec() : boss.position();
        double r = (arena != null ? arena.radius() : 12) * maxFraction * Math.sqrt(boss.getRandom().nextDouble());
        double a = boss.getRandom().nextDouble() * Math.PI * 2;
        return floorAt(boss, c.add(Math.cos(a) * r, 0, Math.sin(a) * r));
    }

    public static void column(ServerLevel level, Vec3 at, net.minecraft.core.particles.ParticleOptions p, double height, int count) {
        for (int i = 0; i < count; i++) {
            double y = height * i / count;
            level.sendParticles(p, at.x, at.y + y, at.z, 2, 0.3, 0.1, 0.3, 0.02);
        }
    }

    public static void sound(LuciferEntity boss, net.minecraft.sounds.SoundEvent e, float volume, float pitch) {
        boss.level().playSound(null, boss.blockPosition(), e, SoundSource.HOSTILE, volume, pitch);
    }

    // --- shared: gap closer ---------------------------------------------------------------

    /** Steps out of the dark behind the target. Forced when the target runs or hides. */
    public static class Teleport extends BossAttack<LuciferEntity> {
        public Teleport() {
            super("teleport", "teleport", 10, 2, 10);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 1, 0.4, 0.02);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
            Vec3 at = floorAt(boss, behind);
            level(boss).sendParticles(AllParticles.HELLFIRE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 1, 0.4, 0.05);
            boss.teleportTo(at.x, at.y, at.z);
            level(boss).sendParticles(AllParticles.HELLFIRE.get(), at.x, at.y + 1, at.z, 30, 0.4, 1, 0.4, 0.05);
            sound(boss, AllSounds.DEMON_SMOKE.get(), 2f, 0.6f);
        }
    }

    // --- phase 1: the Vessel ---------------------------------------------------------------

    /** A snap of the fingers: hellfire erupts under every challenger. */
    public static class Snap extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public Snap() {
            super("snap", "snap", 30, 10, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) {
                spots.add(p.position());
                if (boss.phase() >= 2) {
                    double a = boss.getRandom().nextDouble() * Math.PI * 2;
                    spots.add(floorAt(boss, p.position().add(Math.cos(a) * 3, 0, Math.sin(a) * 3)));
                }
            }
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.8f, TelegraphMarker.RED, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.LUCIFER_SNAP.get(), 2.5f, 1.0f);
            ServerLevel level = level(boss);
            for (Vec3 s : spots) {
                column(level, s, AllParticles.HELLFIRE.get(), 6, 24);
                level.sendParticles(ParticleTypes.LAVA, s.x, s.y + 0.2, s.z, 6, 0.6, 0.1, 0.6, 0);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(s, s).inflate(2, 4, 2))) {
                    if (e != boss && inCircle(e, s, 1.8)) {
                        hit(boss, e, AllDamageTypes.HELLFIRE, 10);
                        e.igniteForSeconds(4);
                    }
                }
            }
        }
    }

    /** He lifts a hand at his target; if he can still see them a moment later, they are thrown. */
    public static class Fling extends BossAttack<LuciferEntity> {
        public Fling() {
            super("fling", "fling", 20, 10, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.hasLineOfSight(target) && boss.distanceToSqr(target) < 18 * 18 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.line(level(boss), boss.position(), yaw, 1.6f, (float) Math.min(18, boss.distanceTo(target) + 4),
                    TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (boss.distanceToSqr(target) > 20 * 20 || !boss.hasLineOfSight(target)) return;
            Vec3 dir = target.position().subtract(boss.position()).multiply(1, 0, 1).normalize();
            target.setDeltaMovement(dir.x * 2.4, 0.95, dir.z * 2.4);
            target.hurtMarked = true;
            hit(boss, target, AllDamageTypes.SPELL, 4);
            level(boss).sendParticles(AllParticles.SIGIL.get(), target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
            sound(boss, AllSounds.LUCIFER_SNAP.get(), 1.5f, 0.6f);
        }
    }

    /** A burning dash and three blows. */
    public static class Grasp extends BossAttack<LuciferEntity> {
        private Vec3 dash = Vec3.ZERO;
        private final Set<UUID> struck = new HashSet<>();

        public Grasp() {
            super("grasp", "grasp", 12, 30, 25);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 14 * 14 ? 1 : 0.2f;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = yawTo(boss.position(), target.position());
            dash = dirOf(yaw);
            TelegraphMarker.line(level(boss), boss.position(), yaw, 2.2f, (float) Math.min(14, boss.distanceTo(target) + 2),
                    TelegraphMarker.RED, windup + 8);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t < 8) {
                boss.setDeltaMovement(dash.x * 0.95, boss.getDeltaMovement().y, dash.z * 0.95);
                level(boss).sendParticles(AllParticles.HELLFIRE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 4, 0.3, 0.6, 0.3, 0.01);
            } else {
                boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
                boss.getLookControl().setLookAt(target);
            }
            if (t == 8 || t == 16 || t == 24) {
                struck.clear();
                Vec3 facing = dirOf(boss.getYRot());
                for (LivingEntity e : level(boss).getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(3.4))) {
                    Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                    if (e == boss || to.length() > 3.4 || (to.length() > 0.5 && to.normalize().dot(facing) < 0.3)) continue;
                    if (struck.add(e.getUUID())) {
                        hit(boss, e, AllDamageTypes.HELLFIRE, 8);
                        e.igniteForSeconds(3);
                        e.knockback(0.6, -facing.x, -facing.z);
                    }
                }
                sound(boss, net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
                Vec3 f = boss.position().add(facing.scale(1.8));
                level(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, f.x, boss.getY() + 1.2, f.z, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Calls black-eyed demons up through the floor. */
    public static class Legion extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public Legion() {
            super("legion", "summon", 30, 10, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return livingMinions(boss) < 3 ? 1 : 0;
        }

        public static int livingMinions(LuciferEntity boss) {
            boss.minions().removeIf(id -> {
                Entity e = level(boss).getEntity(id);
                return e == null || !e.isAlive();
            });
            return boss.minions().size();
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            int n = 2 + boss.getRandom().nextInt(2);
            for (int i = 0; i < n; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Vec3 s = floorAt(boss, boss.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, 1.0f, TelegraphMarker.RED, windup + 6);
            }
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            for (Vec3 s : spots) level(boss).sendParticles(AllParticles.DEMON_SMOKE.get(), s.x, s.y + 0.2, s.z, 2, 0.2, 0.1, 0.2, 0.05);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : spots) {
                BlackEyedDemon demon = AllEntities.BLACK_EYED_DEMON.get().create(level(boss));
                if (demon == null) continue;
                demon.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                demon.finalizeSpawn(level(boss), level(boss).getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                demon.setTarget(target);
                level(boss).addFreshEntity(demon);
                boss.minions().add(demon.getUUID());
                column(level(boss), s, AllParticles.DEMON_SMOKE.get(), 3, 12);
            }
            sound(boss, AllSounds.DEMON_SMOKE.get(), 2f, 0.5f);
        }
    }

    // --- phase 2: the Fallen ---------------------------------------------------------------

    /** The ash wings sweep everything in front of him. */
    public static class WingSweep extends BossAttack<LuciferEntity> {
        private float yaw;

        public WingSweep() {
            super("wing_sweep", "wing_sweep", 18, 8, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 8 * 8 ? 1.5f : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            boss.setYBodyRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 6.5f, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 facing = dirOf(yaw);
            for (LivingEntity e : level(boss).getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(7))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (e == boss || to.length() > 6.5 || (to.length() > 0.5 && to.normalize().dot(facing) < 0.5)) continue;
                hit(boss, e, AllDamageTypes.HELLFIRE, 12);
                Vec3 push = to.normalize();
                e.setDeltaMovement(push.x * 1.5, 0.45, push.z * 1.5);
                e.hurtMarked = true;
            }
            for (int i = -6; i <= 6; i++) {
                Vec3 d = dirOf(yaw + i * 10).scale(4);
                level(boss).sendParticles(AllParticles.ASH.get(), boss.getX() + d.x, boss.getY() + 1.2, boss.getZ() + d.z, 4, 0.4, 0.4, 0.4, 0.05);
            }
            sound(boss, AllSounds.LUCIFER_WINGS.get(), 3f, 0.8f);
        }
    }

    /** Arms raised: hellfire falls from the sky into marked circles, one after another. */
    public static class Rain extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public Rain() {
            super("rain", "rain", 30, 44, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) {
                spots.add(p.position());
                for (int i = 0; i < 2; i++) {
                    double a = boss.getRandom().nextDouble() * Math.PI * 2, r = 2 + boss.getRandom().nextDouble() * 4;
                    spots.add(floorAt(boss, p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r)));
                }
            }
            for (int i = 0; i < 3; i++) spots.add(randomArenaPoint(boss, 0.9));
            for (int i = 0; i < spots.size(); i++) {
                TelegraphMarker.circle(level(boss), spots.get(i), 2.2f, TelegraphMarker.RED, windup + i * 4 + 14);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 4 != 0 || t / 4 >= spots.size()) return;
            Vec3 s = spots.get(t / 4);
            HellfireBolt bolt = new HellfireBolt(level(boss), boss, new Vec3(0, -1, 0), 9f * boss.attackDamageMultiplier(), 2.2f);
            bolt.setPos(s.x, s.y + 18, s.z);
            bolt.setDeltaMovement(0, -1.2, 0);
            if (SNConfig.REAL_DESTRUCTION.get() && level(boss).getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
                bolt.breaking(1.5f);
            }
            level(boss).addFreshEntity(bolt);
        }
    }

    /** A crack of hellfire crawls from his feet toward the target. */
    public static class Fissure extends BossAttack<LuciferEntity> {
        private Vec3 origin = Vec3.ZERO, dir = Vec3.ZERO;
        private final Set<UUID> burned = new HashSet<>();

        public Fissure() {
            super("fissure", "fissure", 20, 30, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = yawTo(boss.position(), target.position());
            origin = boss.position();
            dir = dirOf(yaw);
            TelegraphMarker.line(level(boss), origin, yaw, 2f, 21, TelegraphMarker.RED, windup + 30);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            Vec3 head = floorAt(boss, origin.add(dir.scale(1 + t * 0.7)));
            ServerLevel level = level(boss);
            ArenaController arena = boss.arena();
            if (arena != null) {
                BlockPos s = ArenaTerrain.surface(level, arena, Mth.floor(head.x), Mth.floor(head.z));
                if (s != null) arena.mutate(level, s, AllBlocks.HELLFIRE_CRACK.get().defaultBlockState(), 300);
            }
            level.sendParticles(AllParticles.HELLFIRE.get(), head.x, head.y + 0.3, head.z, 8, 0.4, 0.3, 0.4, 0.05);
            level.sendParticles(ParticleTypes.LAVA, head.x, head.y + 0.2, head.z, 1, 0.3, 0, 0.3, 0);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(head, head).inflate(1.3, 2, 1.3))) {
                if (e != boss && burned.add(e.getUUID())) {
                    hit(boss, e, AllDamageTypes.HELLFIRE, 8);
                    e.igniteForSeconds(4);
                    e.push(0, 0.5, 0);
                    e.hurtMarked = true;
                }
            }
        }
    }

    /** He leaps onto his target; the landing sends out a shockwave you have to jump. */
    public static class Leap extends BossAttack<LuciferEntity> {
        private static final int AIR = 12, WAVE = 12;
        private Vec3 start = Vec3.ZERO, landing = Vec3.ZERO;
        private final Set<UUID> waved = new HashSet<>();

        public Leap() {
            super("leap", "leap", 16, AIR + WAVE, 25);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) > 6 * 6 ? 1.5f : 0.4f;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            landing = target.position();
            TelegraphMarker.circle(level(boss), landing, 2.5f, TelegraphMarker.RED, windup + AIR);
            TelegraphMarker.ring(level(boss), landing, 9f, TelegraphMarker.RED, windup + AIR + WAVE);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            start = boss.position();
            boss.setNoGravity(true);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            if (t <= AIR) {
                double f = t / (double) AIR;
                Vec3 p = start.lerp(landing, f).add(0, Math.sin(Math.PI * f) * 5, 0);
                boss.moveTo(p.x, p.y, p.z, boss.getYRot(), boss.getXRot());
                boss.setDeltaMovement(Vec3.ZERO);
                if (t == AIR) {
                    if (!boss.isAerialPhase()) boss.setNoGravity(false);
                    sound(boss, AllSounds.LUCIFER_SMITE.get(), 2.5f, 1.2f);
                    level.sendParticles(ParticleTypes.EXPLOSION, landing.x, landing.y + 0.5, landing.z, 3, 0.8, 0.2, 0.8, 0);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, boss.getBoundingBox().inflate(2.5))) {
                        if (e != boss && inCircle(e, landing, 2.5)) hit(boss, e, AllDamageTypes.HELLFIRE, 12);
                    }
                }
                return;
            }
            double r = 1 + (t - AIR) * (8.0 / WAVE);
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                level.sendParticles(AllParticles.HELLFIRE.get(), landing.x + Math.cos(a) * r, landing.y + 0.2, landing.z + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
            }
            for (ServerPlayer p : boss.challengers()) {
                double d = flatDistance(p.position(), landing);
                if (Math.abs(d - r) < 0.9 && p.onGround() && waved.add(p.getUUID())) {
                    hit(boss, p, AllDamageTypes.HELLFIRE, 9);
                    p.setDeltaMovement(p.getDeltaMovement().add(0, 0.7, 0));
                    p.hurtMarked = true;
                }
            }
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            if (!boss.isAerialPhase()) boss.setNoGravity(false);
        }
    }

    // --- phase 3: Cage Wrath ---------------------------------------------------------------

    /** A ring of Cage ice slams up around the target. Break out before it freezes you. */
    public static class Cage extends BossAttack<LuciferEntity> {
        private Vec3 center = Vec3.ZERO;

        public Cage() {
            super("cage", "cage", 25, 60, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            center = target.position();
            TelegraphMarker.ring(level(boss), center, 2.6f, TelegraphMarker.BLUE, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            ServerLevel level = level(boss);
            if (arena != null) {
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI * 2 * k / 8;
                    BlockPos base = BlockPos.containing(center.x + Math.cos(a) * 2.6, center.y - 1, center.z + Math.sin(a) * 2.6);
                    ArenaTerrain.pillar(level, arena, base, AllBlocks.CAGE_ICE.get().defaultBlockState(), 3, 200);
                }
            }
            sound(boss, net.minecraft.sounds.SoundEvents.GLASS_BREAK, 2f, 0.5f);
            level.sendParticles(AllParticles.FROST.get(), center.x, center.y + 1, center.z, 60, 2, 1, 2, 0.05);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 20 != 10) return;
            for (LivingEntity e : level(boss).getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(2.4, 3, 2.4))) {
                if (e != boss && inCircle(e, center, 2.3)) {
                    hit(boss, e, AllDamageTypes.SPELL, 2);
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 80, e.getTicksFrozen() + 60));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                }
            }
        }
    }

    /** A beam of grace from his chest sweeps half the arena. Ice pillars give cover. */
    public static class Beam extends BossAttack<LuciferEntity> {
        private static final double LENGTH = 22;
        private float startYaw;
        private int sign;

        public Beam() {
            super("beam", "beam", 30, 60, 30);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            startYaw = yawTo(boss.position(), target.position()) - 45;
            sign = boss.getRandom().nextBoolean() ? 1 : -1;
            if (sign < 0) startYaw += 90;
            TelegraphMarker.line(level(boss), boss.position(), startYaw, 2.4f, (float) LENGTH, TelegraphMarker.VIOLET, windup);
            sound(boss, AllSounds.LUCIFER_CHARGE.get(), 3f, 1.2f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            float yaw = startYaw + sign * t * 3f;
            boss.setYRot(yaw);
            boss.setYBodyRot(yaw);
            boss.setYHeadRot(yaw);
            Vec3 eye = boss.position().add(0, boss.getBbHeight() * 0.55, 0);
            Vec3 dir = dirOf(yaw);
            for (double d = 1; d < LENGTH; d += 0.75) {
                Vec3 p = eye.add(dir.scale(d));
                level.sendParticles(t % 2 == 0 ? ParticleTypes.END_ROD : AllParticles.GRACE.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            }
            if (t % 3 != 0) return;
            for (ServerPlayer p : boss.challengers()) {
                Vec3 to = p.getEyePosition().subtract(eye);
                double along = to.dot(dir);
                if (along < 0 || along > LENGTH) continue;
                Vec3 closest = eye.add(dir.scale(along));
                double off = p.position().add(0, 1, 0).distanceTo(closest);
                if (off > 1.6) continue;
                var clip = level.clip(new ClipContext(eye, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
                if (clip.getType() == HitResult.Type.BLOCK) continue;  // hiding behind ice
                hit(boss, p, AllDamageTypes.GRACE, 5);
            }
        }
    }

    /** Three illusions walk out of him; he hides among them. Reveal tells them apart. */
    public static class Illusion extends BossAttack<LuciferEntity> {
        public Illusion() {
            super("illusion", "illusion", 20, 10, 15);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return level(boss).getEntitiesOfClass(LuciferIllusion.class, boss.getBoundingBox().inflate(40)).isEmpty() ? 1 : 0;
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(AllParticles.FROST.get(), boss.getX(), boss.getY() + 1.5, boss.getZ(), 6, 1, 1, 1, 0.1);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            List<Vec3> spots = new ArrayList<>();
            for (int i = 0; i < 3; i++) spots.add(randomArenaPoint(boss, 0.6));
            spots.add(boss.position());
            int real = boss.getRandom().nextInt(spots.size());
            for (int i = 0; i < spots.size(); i++) {
                Vec3 s = spots.get(i);
                if (i == real) {
                    boss.teleportTo(s.x, s.y, s.z);
                } else {
                    LuciferIllusion ill = AllEntities.LUCIFER_ILLUSION.get().create(level);
                    if (ill == null) continue;
                    ill.moveTo(s.x, s.y, s.z, boss.getYRot(), 0);
                    ill.setTarget(target);
                    level.addFreshEntity(ill);
                }
                level.sendParticles(AllParticles.FROST.get(), s.x, s.y + 1, s.z, 30, 0.5, 1, 0.5, 0.05);
            }
            sound(boss, AllSounds.LUCIFER_TRANSFORM.get(), 2f, 1.6f);
        }
    }

    /** Pillars of ice burst from the floor and shatter into shards that hunt the living. */
    public static class Collapse extends BossAttack<LuciferEntity> {
        private final List<BlockPos> bases = new ArrayList<>();
        private final List<BlockPos> blocks = new ArrayList<>();

        public Collapse() {
            super("collapse", "collapse", 25, 20, 25);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            if (arena == null) return;
            bases.addAll(ArenaTerrain.pillarSites(level(boss), arena, boss.getRandom(), 6));
            for (BlockPos b : bases) TelegraphMarker.circle(level(boss), Vec3.atBottomCenterOf(b.above()), 1.3f, TelegraphMarker.BLUE, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            if (arena == null) return;
            for (BlockPos b : bases) {
                blocks.addAll(ArenaTerrain.pillar(level(boss), arena, b, AllBlocks.CAGE_ICE.get().defaultBlockState(), 3, 40));
            }
            sound(boss, net.minecraft.sounds.SoundEvents.GLASS_PLACE, 2f, 0.5f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != 10) return;
            ServerLevel level = level(boss);
            ArenaController arena = boss.arena();
            List<ServerPlayer> players = boss.challengers();
            for (BlockPos b : bases) {
                Vec3 from = Vec3.atCenterOf(b.above(2));
                ServerPlayer aim = players.stream().min((a, c) -> Double.compare(a.distanceToSqr(from), c.distanceToSqr(from))).orElse(null);
                for (int i = 0; i < 3 && aim != null; i++) {
                    BossShard shard = new BossShard(level, boss, BossShard.Kind.ICE, 6f * boss.attackDamageMultiplier(), 0.03f, aim);
                    shard.setPos(from.x, from.y + i * 0.6, from.z);
                    Vec3 dir = aim.getEyePosition().subtract(from).normalize();
                    shard.shoot(dir.x, dir.y + 0.05, dir.z, 0.9f, 6f);
                    level.addFreshEntity(shard);
                }
                level.sendParticles(AllParticles.FROST.get(), from.x, from.y, from.z, 30, 0.5, 1, 0.5, 0.1);
            }
            if (arena != null) blocks.forEach(p -> arena.revert(level, p));
            sound(boss, net.minecraft.sounds.SoundEvents.GLASS_BREAK, 3f, 0.6f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ArenaController arena = boss.arena();
            if (arena != null) blocks.forEach(p -> arena.revert(level(boss), p));
        }
    }

    // --- phase 4: Archangel Unbound --------------------------------------------------------

    /** Volleys of grace feathers, fanned at every challenger. */
    public static class Storm extends BossAttack<LuciferEntity> {
        public Storm() {
            super("storm", "storm", 20, 50, 20);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 8 != 0) return;
            ServerLevel level = level(boss);
            Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
            List<ServerPlayer> players = boss.challengers();
            for (ServerPlayer p : players.subList(0, Math.min(4, players.size()))) {
                Vec3 dir = p.getEyePosition().subtract(from).normalize();
                float baseYaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG);
                for (int i = -2; i <= 2; i++) {
                    double a = (baseYaw + i * 9) * Mth.DEG_TO_RAD;
                    BossShard f = new BossShard(level, boss, BossShard.Kind.FEATHER, 5f * boss.attackDamageMultiplier(), 0.04f, p);
                    f.setPos(from.x, from.y, from.z);
                    f.shoot(Math.cos(a), dir.y, Math.sin(a), 1.1f, 1f);
                    level.addFreshEntity(f);
                }
            }
            sound(boss, AllSounds.LUCIFER_WINGS.get(), 2f, 1.3f);
        }
    }

    /** Lanes of holy light strike across the arena, then strike again at right angles. */
    public static class Judgement extends BossAttack<LuciferEntity> {
        private static final int LANE = 3;

        public Judgement() {
            super("judgement", "judgement", 30, 40, 20);
        }

        private void telegraph(LuciferEntity boss, boolean alongX, int parity, int lifetime) {
            ArenaController arena = boss.arena();
            if (arena == null) return;
            int r = arena.radius();
            Vec3 c = arena.centerVec();
            for (int k = -r / LANE; k <= r / LANE; k++) {
                if (Math.floorMod(k, 2) != parity) continue;
                double off = k * LANE;
                Vec3 from = alongX ? c.add(-r, 0, off) : c.add(off, 0, -r);
                TelegraphMarker.line(level(boss), floorAt(boss, from), alongX ? -90 : 0, LANE, r * 2, TelegraphMarker.GOLD, lifetime);
            }
        }

        private void strike(LuciferEntity boss, boolean alongX, int parity) {
            ArenaController arena = boss.arena();
            if (arena == null) return;
            ServerLevel level = level(boss);
            Vec3 c = arena.centerVec();
            int r = arena.radius();
            for (int k = -r / LANE; k <= r / LANE; k++) {
                if (Math.floorMod(k, 2) != parity) continue;
                double off = k * LANE;
                for (int s = -r; s <= r; s += 3) {
                    Vec3 p = alongX ? c.add(s, 0, off) : c.add(off, 0, s);
                    if (arena.horizontalDistance(p) > r) continue;
                    Vec3 f = floorAt(boss, p);
                    level.sendParticles(ParticleTypes.END_ROD, f.x, f.y + 3, f.z, 6, 0.4, 3, 0.4, 0.01);
                }
            }
            for (ServerPlayer p : boss.challengers()) {
                double off = alongX ? p.getZ() - c.z : p.getX() - c.x;
                int lane = (int) Math.round(off / LANE);
                if (Math.floorMod(lane, 2) == parity && Math.abs(off - lane * LANE) <= LANE / 2.0 + 0.2) {
                    hit(boss, p, AllDamageTypes.GRACE, 14);
                }
            }
            sound(boss, AllSounds.LUCIFER_SMITE.get(), 3f, 1.4f);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            telegraph(boss, true, 0, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            strike(boss, true, 0);
            telegraph(boss, false, 1, 25);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t == 25) strike(boss, false, 1);
        }
    }

    /**
     * Archangel's Smite: he gathers light for three seconds, then unmakes everything not standing
     * in a safe sigil or a Ward. The longest punish window of the fight follows.
     */
    public static class Smite extends BossAttack<LuciferEntity> {
        private final List<Vec3> safe = new ArrayList<>();

        public Smite() {
            super("smite", "smite_charge", 72, 10, 40);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (ServerPlayer p : boss.challengers()) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                safe.add(floorAt(boss, p.position().add(Math.cos(a) * 6, 0, Math.sin(a) * 6)));
            }
            if (safe.isEmpty()) safe.add(randomArenaPoint(boss, 0.7));
            for (Vec3 s : safe) TelegraphMarker.circle(level, s, 2.4f, TelegraphMarker.SAFE, windup + 10);
            ArenaController arena = boss.arena();
            if (arena != null) TelegraphMarker.ring(level, arena.centerVec(), arena.radius() - 0.5f, TelegraphMarker.GOLD, windup + 10);
            sound(boss, AllSounds.LUCIFER_CHARGE.get(), 4f, 0.7f);
            LuciferCinematics.smiteCharge(boss, windup);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ(),
                    4 + t / 6, 2, 2, 2, 0.15);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.LUCIFER_SMITE.get(), 5f, 0.6f);
            LuciferCinematics.smiteRelease(boss);
            for (ServerPlayer p : boss.challengers()) {
                boolean sheltered = WardEntity.isSheltered(p) || safe.stream().anyMatch(s -> inCircle(p, s, 2.6));
                if (!sheltered) hit(boss, p, AllDamageTypes.GRACE, 24);
            }
            ArenaController arena = boss.arena();
            if (arena != null) {
                Vec3 c = arena.centerVec();
                level(boss).sendParticles(ParticleTypes.END_ROD, c.x, c.y + 2, c.z, 400, arena.radius() / 2.0, 3, arena.radius() / 2.0, 0.1);
            }
        }
    }

    /** A tether of grace that feeds him. Break it by running or by hitting him three times. */
    public static class Drain extends BossAttack<LuciferEntity> {
        int hits;

        public Drain() {
            super("drain", "drain", 15, 80, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 14 * 14 && boss.hasLineOfSight(target) && boss.getHealth() < boss.getMaxHealth() * 0.22f ? 1 : 0.3f;
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return hits >= 3 || boss.distanceToSqr(target) > 15 * 15 || !boss.hasLineOfSight(target);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            Vec3 a = target.position().add(0, 1, 0), b = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
            for (int i = 0; i <= 12; i++) {
                Vec3 p = a.lerp(b, i / 12.0);
                level.sendParticles(AllParticles.GRACE.get(), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            }
            if (t % 10 == 0) {
                hit(boss, target, AllDamageTypes.GRACE, 2);
                boss.heal(4);
            }
        }
    }
}
