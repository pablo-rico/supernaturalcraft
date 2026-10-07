package org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAttacks.*;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Pestilence's attacks: toxic clouds that settle and grow, a cough in your face, a swipe of the cane, the flies, and on
 * his horse a long spray of sickness and the charge. Every one of them gives the plague.
 */
public final class PestilenceAttacks {

    private PestilenceAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(CloudCast::new, 3), opt(Cough::new, 3), opt(CaneSwipe::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(CloudCast::new, 2), opt(Cough::new, 2), opt(CaneSwipe::new, 1.5f), opt(SwarmCall::new, 3));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(() -> new Charge(AllDamageTypes.PLAGUE, 10), 2.5f), opt(CoughCone::new, 3), opt(SwarmCall::new, 1.5f), opt(CloudCast::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static PestilenceEntity pestilence(LuciferEntity boss) {
        return (PestilenceEntity) boss;
    }

    /** Plague on everyone hit, beyond the blow. */
    static void sicken(LuciferEntity boss, List<LivingEntity> victims, float damage, int doses) {
        for (LivingEntity e : victims) {
            hit(boss, e, AllDamageTypes.PLAGUE, damage);
            Plague.infect(e, doses);
        }
    }

    /** He breathes out over the swamp: clouds settle where the hunters stand and keep growing. */
    public static class CloudCast extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public CloudCast() {
            super("cloud_cast", "cloud_cast", 24, 4, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return pestilence(boss).clouds() >= 6 ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) spots.add(floorAt(boss, p.position()));
            if (spots.isEmpty()) spots.add(floorAt(boss, target.position()));
            spots.add(randomArenaPoint(boss, 0.8));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, PestilenceEntity.CLOUD_START + 0.5f, TelegraphMarker.SAFE, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.PESTILENCE_COUGH.get(), 2f, 0.7f);
            for (Vec3 s : spots) pestilence(boss).addCloud(s);
        }
    }

    /** A wet cough, right in your face. */
    public static class Cough extends BossAttack<LuciferEntity> {
        private float yaw;

        public Cough() {
            super("cough", "cough", 16, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 5f, TelegraphMarker.SAFE, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.PESTILENCE_COUGH.get(), 2.5f, 1.0f);
            Vec3 dir = dirOf(yaw);
            level(boss).sendParticles(AllParticles.PLAGUE_SPORE.get(), boss.getX() + dir.x * 2, boss.getY() + 1.4, boss.getZ() + dir.z * 2, 40,
                    1.0, 0.4, 1.0, 0.04);
            sicken(boss, inCone(boss, boss.position(), yaw, 5, 45), 5, 1);
        }
    }

    /** A swipe of the cane. */
    public static class CaneSwipe extends BossAttack<LuciferEntity> {
        private float yaw;

        public CaneSwipe() {
            super("cane_swipe", "cough", 12, 4, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceTo(target) < 5 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 3.5f, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            hitAll(boss, inCone(boss, boss.position(), yaw, 3.5, 60), DamageTypes.MOB_ATTACK, 8);
        }
    }

    /** He calls the flies: swarms that chase, blind and sicken, and only burn. */
    public static class SwarmCall extends BossAttack<LuciferEntity> {
        public SwarmCall() {
            super("swarm_call", "swarm_call", 22, 4, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return level(boss).getEntitiesOfClass(FlySwarmEntity.class, boss.getBoundingBox().inflate(48)).size() >= 4 ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.FLY_SWARM_BUZZ.get(), 2.5f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int n = 2 + Math.min(2, boss.challengers().size() / 2);
            for (int i = 0; i < n; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Vec3 at = boss.position().add(Math.cos(a) * 2, 1.8, Math.sin(a) * 2);
                FlySwarmEntity swarm = AllEntities.FLY_SWARM.get().create(level);
                if (swarm == null) continue;
                swarm.moveTo(at.x, at.y, at.z, 0, 0);
                swarm.setOwner(boss.getUUID());
                swarm.finalizeSpawn(level, level.getCurrentDifficultyAt(swarm.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                level.addFreshEntity(swarm);
                boss.minions().add(swarm.getUUID());
            }
        }
    }

    /** Mounted: a long cough sprayed out in a cone. */
    public static class CoughCone extends BossAttack<LuciferEntity> {
        private float yaw;

        public CoughCone() {
            super("cough_cone", "mounted_spray", 24, 10, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 10f, TelegraphMarker.SAFE, windup);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 2 == 0) {
                Vec3 dir = dirOf(yaw);
                for (int i = 2; i <= 10; i += 2) {
                    level(boss).sendParticles(AllParticles.PLAGUE_SPORE.get(), boss.getX() + dir.x * i, boss.getY() + 1.2, boss.getZ() + dir.z * i,
                            4, i * 0.15, 0.3, i * 0.15, 0.01);
                }
            }
            if (t == 1) sound(boss, AllSounds.PESTILENCE_COUGH.get(), 3f, 0.8f);
            if (t == 5) sicken(boss, inCone(boss, boss.position(), yaw, 10, 35), 7, 2);
        }
    }
}
