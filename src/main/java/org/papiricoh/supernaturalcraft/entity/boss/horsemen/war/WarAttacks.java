package org.papiricoh.supernaturalcraft.entity.boss.horsemen.war;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAttacks.*;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * War's attacks. On foot: sword combos and a heavy cleave, each shown on the ground first (a shield raised just as a
 * blow lands parries it); with his fury up, a war cry and his illusion. On the red horse: charges across the field,
 * sweeps of the sword all round him, and fire along the trenches.
 */
public final class WarAttacks {

    /** Ticks of shield-raising that count as a parry rather than just blocking. */
    public static final int PARRY_WINDOW = 10;

    private WarAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(SwordCombo::new, 3), opt(HeavyCleave::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(SwordCombo::new, 3), opt(HeavyCleave::new, 2), opt(WarCry::new, 1.5f), opt(Illusion::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(() -> new Charge(DamageTypes.MOB_ATTACK, 12), 3), opt(MountedSweep::new, 2.5f), opt(TrenchFire::new, 1.5f),
            opt(Illusion::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static WarEntity war(LuciferEntity boss) {
        return (WarEntity) boss;
    }

    /**
     * One blow of his sword on everyone in the cone. A hunter who raised a shield within the last half second parries:
     * no harm, and he staggers. One who has been hiding behind it all along has it knocked aside and takes the blow.
     */
    static void strike(LuciferEntity boss, float yaw, double reach, double halfAngle, float damage) {
        level(boss).playSound(null, boss.blockPosition(), AllSounds.WAR_SWORD_CLASH.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.6f,
                0.9f + boss.getRandom().nextFloat() * 0.2f);
        Vec3 dir = dirOf(yaw);
        level(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + dir.x * 1.6, boss.getY() + 1.1, boss.getZ() + dir.z * 1.6, 2, 0.4, 0.1, 0.4, 0);
        for (LivingEntity e : inCone(boss, boss.position(), yaw, reach, halfAngle)) {
            if (e instanceof ServerPlayer p && p.isBlocking()) {
                if (p.getTicksUsingItem() <= PARRY_WINDOW && boss instanceof WarEntity war) {
                    war.parried(p);
                    return;
                }
                p.disableShield();
            }
            hit(boss, e, DamageTypes.MOB_ATTACK, damage);
        }
    }

    /** Three cuts, each shown as a cone a moment before it lands. */
    public static class SwordCombo extends BossAttack<LuciferEntity> {
        private static final int GAP = 12;
        private float yaw;

        public SwordCombo() {
            super("sword_combo", "sword_combo", 16, GAP * 3, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 4.2f, TelegraphMarker.RED, windup);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % GAP == 1) {
                yaw = yawTo(boss.position(), target.position());
                boss.setYRot(yaw);
                // Each cut steps in.
                Vec3 step = dirOf(yaw).scale(0.6);
                boss.setDeltaMovement(step.x, boss.getDeltaMovement().y, step.z);
                if (t > 1) TelegraphMarker.cone(level(boss), boss.position(), yaw, 4.2f, TelegraphMarker.RED, GAP - 2);
            }
            if (t % GAP == GAP - 1) strike(boss, yaw, 4.2, 55, 9);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }
    }

    /** One great overhead cleave down a long lane. */
    public static class HeavyCleave extends BossAttack<LuciferEntity> {
        private float yaw;

        public HeavyCleave() {
            super("sword_heavy", "sword_heavy", 28, 4, 22);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            boss.setYRot(yaw);
            TelegraphMarker.line(level(boss), boss.position(), yaw, 2.2f, 9, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            sound(boss, AllSounds.WAR_SWORD_CLASH.get(), 2.2f, 0.7f);
            for (int i = 1; i <= 9; i++) {
                level(boss).sendParticles(ParticleTypes.CRIT, boss.getX() + dir.x * i, boss.getY() + 0.3, boss.getZ() + dir.z * i, 4, 0.3, 0.1, 0.3, 0.1);
            }
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(9.5, 2, 9.5))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
                if (along < 0 || along > 9.5 || across > 1.4) continue;
                if (e instanceof ServerPlayer p && p.isBlocking()) {
                    if (p.getTicksUsingItem() <= PARRY_WINDOW && boss instanceof WarEntity war) {
                        war.parried(p);
                        return;
                    }
                    p.disableShield();
                }
                hit(boss, e, DamageTypes.MOB_ATTACK, 16);
            }
        }
    }

    /** A war cry: everyone close is thrown back, and his fury rises. */
    public static class WarCry extends BossAttack<LuciferEntity> {
        public WarCry() {
            super("war_cry", "rage_roar", 22, 4, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), boss.position(), 6f, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.WAR_RAGE.get(), 3f, 0.8f);
            level(boss).sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 1, boss.getZ(), 4, 1.5, 0.5, 1.5, 0);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(6, 2, 6))) {
                if (!inCircle(e, boss.position(), 6)) continue;
                Vec3 away = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
                away = away.normalize();
                hit(boss, e, DamageTypes.MOB_ATTACK, 6);
                e.push(away.x * 1.5, 0.6, away.z * 1.5);
                e.hurtMarked = true;
            }
            if (boss instanceof WarEntity war) war.fury().set(war.fury().value() + 10);
        }
    }

    /**
     * His illusion: every hunter is marked for ten seconds (to each of them, the others look like demons) and mirages
     * walk the field, some hostile, some innocent. The innocent kneel.
     */
    public static class Illusion extends BossAttack<LuciferEntity> {
        public Illusion() {
            super("illusion", "illusion_cast", 24, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return WarIllusions.mirages(level(boss), war(boss)) > 6 ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1.5, boss.getZ(), 30, 0.6, 0.8, 0.6, 0.03);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            WarIllusions.cast(level(boss), war(boss), boss.challengers());
        }
    }

    /** Mounted: the sword swept all the way round him. */
    public static class MountedSweep extends BossAttack<LuciferEntity> {
        public MountedSweep() {
            super("mounted_sweep", "mounted_sweep", 22, 6, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return horseman(boss).isMounted() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.circle(level(boss), boss.position(), 5f, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.WAR_SWORD_CLASH.get(), 2.5f, 0.8f);
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                level(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + Math.cos(a) * 3, boss.getY() + 1.2, boss.getZ() + Math.sin(a) * 3,
                        1, 0, 0, 0, 0);
            }
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(5, 2, 5))) {
                if (inCircle(e, boss.position(), 5)) hit(boss, e, DamageTypes.MOB_ATTACK, 13);
            }
        }
    }

    /** Fire runs along the trenches, and around whoever stands near him. */
    public static class TrenchFire extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public TrenchFire() {
            super("trench_fire", "rage_roar", 26, 4, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            List<BlockPos> floor = war(boss).trenchFloor();
            for (int i = 0; i < floor.size(); i += 3) spots.add(Vec3.atBottomCenterOf(floor.get(i).above()));
            for (ServerPlayer p : boss.challengers()) spots.add(floorAt(boss, p.position()));
            for (int i = 0; i < spots.size(); i += 6) TelegraphMarker.circle(level(boss), spots.get(i), 1.6f, TelegraphMarker.RED, windup);
            for (ServerPlayer p : boss.challengers()) TelegraphMarker.circle(level(boss), floorAt(boss, p.position()), 1.8f, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            sound(boss, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, 2.5f, 0.7f);
            for (Vec3 s : spots) level.addFreshEntity(new FlameTrail(level, boss, s.x, s.y, s.z));
        }
    }
}
