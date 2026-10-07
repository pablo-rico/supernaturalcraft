package org.papiricoh.supernaturalcraft.entity.boss.horsemen.death;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.projectile.SoulCrescent;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAttacks.*;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Death's attacks: the cane up close, the scythe reaping a wide arc at range and thrown as crescents, his reapers, a step
 * through the shadows in the world of the dead, and from the pale horse the charge and waves of the scythe all round.
 */
public final class DeathAttacks {

    private DeathAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(CaneStrike::new, 3), opt(Reap::new, 3), opt(ScytheThrow::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(CaneStrike::new, 2), opt(Reap::new, 3), opt(ScytheThrow::new, 2), opt(SummonReapers::new, 3));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Reap::new, 3), opt(ScytheThrow::new, 2), opt(SummonReapers::new, 2), opt(ShadowStep::new, 3));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(
            opt(() -> new Charge(AllDamageTypes.REAPED, 14), 3), opt(MountedReap::new, 3), opt(SummonReapers::new, 1.5f),
            opt(ScytheThrow::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    /** A rap of the cane, close in. */
    public static class CaneStrike extends BossAttack<LuciferEntity> {
        private float yaw;

        public CaneStrike() {
            super("cane_strike", "cane_strike", 14, 4, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceTo(target) < 5 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 3.4f, TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.DEATH_REAP.get(), 1.5f, 1.4f);
            hitAll(boss, inCone(boss, boss.position(), yaw, 3.4, 55), DamageTypes.MOB_ATTACK, 10);
        }
    }

    /** The scythe reaps a wide arc, far beyond arm's reach. */
    public static class Reap extends BossAttack<LuciferEntity> {
        private float yaw;

        public Reap() {
            super("reap", "scythe_reap", 22, 4, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 7.5f, TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.DEATH_REAP.get(), 2.5f, 0.9f);
            ServerLevel level = level(boss);
            for (int i = -4; i <= 4; i++) {
                Vec3 d = dirOf(yaw + i * 14).scale(5.5);
                DeathEntity.reapFx(level, boss.position().add(d));
            }
            hitAll(boss, inCone(boss, boss.position(), yaw, 7.5, 65), AllDamageTypes.REAPED, 12);
        }
    }

    /** Crescents of soul flung from the scythe, fanned out toward his target. */
    public static class ScytheThrow extends BossAttack<LuciferEntity> {
        public ScytheThrow() {
            super("scythe_throw", "scythe_throw", 18, 4, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.line(level(boss), boss.position(), yawTo(boss.position(), target.position()), 1.6f, 16, TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            float yaw = yawTo(boss.position(), target.position());
            int n = boss.phase() >= 3 ? 5 : 3;
            for (int i = 0; i < n; i++) {
                SoulCrescent c = new SoulCrescent(level, boss);
                c.setPos(boss.getX(), boss.getY() + 1.2, boss.getZ());
                Vec3 d = dirOf(yaw + (i - (n - 1) / 2f) * 12);
                c.shoot(d.x, 0, d.z, 1.1f, 0);
                level.addFreshEntity(c);
            }
            sound(boss, AllSounds.DEATH_REAP.get(), 2f, 1.2f);
        }
    }

    /** He calls his reapers. You only see them when your time is nearly up. */
    public static class SummonReapers extends BossAttack<LuciferEntity> {
        public SummonReapers() {
            super("summon_reapers", "summon_reapers", 24, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return level(boss).getEntitiesOfClass(ReaperEntity.class, boss.getBoundingBox().inflate(48)).size() >= 6 ? 0 : 1;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int n = 2 + Math.min(3, boss.challengers().size());
            for (int i = 0; i < n; i++) {
                Vec3 at = randomArenaPoint(boss, 0.8);
                ReaperEntity reaper = AllEntities.REAPER.get().create(level);
                if (reaper == null) continue;
                reaper.moveTo(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360, 0);
                reaper.setMaster(boss.getUUID());
                reaper.finalizeSpawn(level, level.getCurrentDifficultyAt(reaper.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                level.addFreshEntity(reaper);
                boss.minions().add(reaper.getUUID());
                level.sendParticles(AllParticles.SOUL_WISP.get(), at.x, at.y + 1, at.z, 8, 0.3, 0.6, 0.3, 0.02);
            }
            sound(boss, AllSounds.DEATH_LIMBO_BELL.get(), 2.5f, 0.6f);
        }
    }

    /** He steps out of one shadow and into the one behind you, cane already falling. */
    public static class ShadowStep extends BossAttack<LuciferEntity> {
        public ShadowStep() {
            super("shadow_step", "shadow_step", 10, 8, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(ParticleTypes.SQUID_INK, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
            Vec3 at = floorAt(boss, behind);
            boss.teleportTo(at.x, at.y, at.z);
            boss.setYRot(yawTo(at, target.position()));
            level(boss).sendParticles(ParticleTypes.SQUID_INK, at.x, at.y + 1, at.z, 30, 0.4, 0.8, 0.4, 0.02);
            TelegraphMarker.circle(level(boss), target.position(), 1.6f, TelegraphMarker.VIOLET, active);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != active - 1) return;
            sound(boss, AllSounds.DEATH_REAP.get(), 2f, 1.1f);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(2.6, 1, 2.6))) hit(boss, e, AllDamageTypes.REAPED, 11);
        }
    }

    /** Mounted: the scythe swept round, and a wave of it rolling out across the arena. */
    public static class MountedReap extends BossAttack<LuciferEntity> {
        public MountedReap() {
            super("mounted_reap", "mounted_reap", 24, 6, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return horseman(boss).isMounted() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.circle(level(boss), boss.position(), 5.5f, TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            sound(boss, AllSounds.DEATH_REAP.get(), 3f, 0.7f);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(5.5, 2, 5.5))) {
                if (inCircle(e, boss.position(), 5.5)) hit(boss, e, AllDamageTypes.REAPED, 14);
            }
            for (int i = 0; i < 8; i++) {
                SoulCrescent c = new SoulCrescent(level, boss);
                c.setPos(boss.getX(), boss.getY() + 1.0, boss.getZ());
                Vec3 d = dirOf(i * 45f);
                c.shoot(d.x, 0, d.z, 0.9f, 0);
                level.addFreshEntity(c);
            }
        }
    }
}
