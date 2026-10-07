package org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine;

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
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAttacks.*;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Famine's attacks. From his wheelchair: a wave of hunger and a beam that drinks a hunter's life. Standing: his
 * starving thralls, and a lunge that seizes. On the black horse: charges, and he lifts a hunter to feed on them.
 */
public final class FamineAttacks {

    private FamineAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(HungerWave::new, 2), opt(DrainBeam::new, 3));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(HungerWave::new, 1.5f), opt(DrainBeam::new, 2), opt(Thralls::new, 3), opt(Gnaw::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(() -> new Charge(AllDamageTypes.STARVED, 11), 2.5f), opt(Grab::new, 3), opt(Thralls::new, 1.5f), opt(DrainBeam::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static FamineEntity famine(LuciferEntity boss) {
        return (FamineEntity) boss;
    }

    /** A wave of hunger rolls out from him: it empties the stomachs it reaches and gnaws at those already empty. */
    public static class HungerWave extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 9;

        public HungerWave() {
            super("hunger_wave", "devour", 24, 4, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), boss.position(), RADIUS, TelegraphMarker.YELLOW, windup);
            sound(boss, AllSounds.FAMINE_HUNGER.get(), 2f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                level.sendParticles(ParticleTypes.SMOKE, boss.getX() + Math.cos(a) * RADIUS * 0.6, boss.getY() + 0.4,
                        boss.getZ() + Math.sin(a) * RADIUS * 0.6, 2, 0.4, 0.1, 0.4, 0.02);
            }
            for (ServerPlayer p : boss.challengers()) {
                if (!inCircle(p, boss.position(), RADIUS)) continue;
                int food = p.getFoodData().getFoodLevel();
                p.getFoodData().setFoodLevel(Math.max(0, food - 6));
                p.getFoodData().setSaturation(0);
                hit(boss, p, AllDamageTypes.STARVED, food <= 6 ? 7 : 3);
            }
        }
    }

    /** He fixes on one hunter and drinks: a thread of life runs from them to him while they stay in his sight. */
    public static class DrainBeam extends BossAttack<LuciferEntity> {
        public DrainBeam() {
            super("drain", "drain", 18, 50, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceTo(target) < 16 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), target.position(), 1.2f, TelegraphMarker.YELLOW, windup);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            boss.getLookControl().setLookAt(target);
            if (!boss.hasLineOfSight(target) || boss.distanceTo(target) > 18) return;
            if (t % 3 == 0) famine(boss).soulBeam(level(boss), target.position().add(0, 1, 0));
            if (t % 10 == 0) {
                hit(boss, target, AllDamageTypes.STARVED, 2.5f);
                if (target instanceof ServerPlayer p) p.getFoodData().addExhaustion(4f);
            }
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return !boss.hasLineOfSight(target);
        }
    }

    /** He calls his thralls: starving husks shuffle in from the edge of the field to be eaten. Kill them on the way. */
    public static class Thralls extends BossAttack<LuciferEntity> {
        public Thralls() {
            super("thralls", "devour", 20, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            int alive = level(boss).getEntitiesOfClass(HungryThrallEntity.class, boss.getBoundingBox().inflate(48)).size();
            return alive >= 6 ? 0 : 1;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int n = 3 + Math.min(3, boss.challengers().size());
            Vec3 c = boss.arena() != null ? boss.arena().centerVec() : boss.position();
            double r = boss.arena() != null ? boss.arena().radius() - 4 : 14;
            for (int i = 0; i < n; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Vec3 at = floorAt(boss, c.add(Math.cos(a) * r, 0, Math.sin(a) * r));
                HungryThrallEntity thrall = AllEntities.HUNGRY_THRALL.get().create(level);
                if (thrall == null) continue;
                thrall.moveTo(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360, 0);
                thrall.finalizeSpawn(level, level.getCurrentDifficultyAt(thrall.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                thrall.setMaster(boss.getUUID());
                level.addFreshEntity(thrall);
                boss.minions().add(thrall.getUUID());
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.5, at.z, 6, 0.3, 0.3, 0.3, 0.02);
            }
            sound(boss, AllSounds.FAMINE_HUNGER.get(), 2.5f, 0.7f);
        }
    }

    /** On his feet, he lunges and bites. */
    public static class Gnaw extends BossAttack<LuciferEntity> {
        private float yaw;

        public Gnaw() {
            super("gnaw", "grab", 16, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 3.6f, TelegraphMarker.YELLOW, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.FAMINE_DEVOUR.get(), 1.8f, 1.1f);
            hitAll(boss, inCone(boss, boss.position(), yaw, 3.6, 50), DamageTypes.MOB_ATTACK, 9);
        }
    }

    /**
     * He reaches for the nearest hunter and, if they are still there, lifts them and feeds: health and hunger drain
     * until the others hurt him enough (alone, until there is little left).
     */
    public static class Grab extends BossAttack<LuciferEntity> {
        private boolean caught;

        public Grab() {
            super("grab", "mounted_grab", 18, FamineEntity.GRAB_MAX_TICKS, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return target instanceof ServerPlayer && famine(boss).grabbed() == null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.circle(level(boss), target.position(), 1.6f, TelegraphMarker.YELLOW, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (target instanceof ServerPlayer p && boss.distanceTo(p) < 7 && boss.hasLineOfSight(p)) {
                caught = famine(boss).grab(p);
                sound(boss, AllSounds.FAMINE_DEVOUR.get(), 2f, 0.8f);
            }
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return !caught || famine(boss).grabbed() == null;
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            famine(boss).release();
        }
    }
}
