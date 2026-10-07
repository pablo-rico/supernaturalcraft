package org.papiricoh.supernaturalcraft.entity.boss.horsemen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;

import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/** What every Horseman can do: close the distance, and (mounted) charge straight across the arena. */
public final class HorsemenAttacks {

    private HorsemenAttacks() {
    }

    public static HorsemanEntity horseman(LuciferEntity boss) {
        return (HorsemanEntity) boss;
    }

    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID())
                && !(e instanceof HorsemanEntity) && !(e instanceof HorsemanSteedEntity);
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** Everyone in a cone {@code reach} long and {@code halfAngle} degrees either side of {@code yaw}, from {@code from}. */
    public static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach, double halfAngle) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(halfAngle));
        return foes(boss, new AABB(from, from).inflate(reach, 3, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && (d < 0.8 || to.normalize().dot(dir) >= cos) && Math.abs(e.getY() - from.y) < 3;
        }).toList();
    }

    public static void hitAll(LuciferEntity boss, List<LivingEntity> victims, ResourceKey<DamageType> type, float amount) {
        for (LivingEntity e : victims) hit(boss, e, type, amount);
    }

    /** Crosses the arena to a hunter who ran or hid: a few long strides in a cloud of dust. */
    public static class Close extends BossAttack<LuciferEntity> {
        public Close() {
            super("close", "mounted_charge", 8, 2, 8);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 0.5, boss.getZ(), 20, 0.5, 0.4, 0.5, 0.02);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 toward = target.position().subtract(boss.position()).multiply(1, 0, 1);
            if (toward.lengthSqr() < 0.01) return;
            Vec3 at = floorAt(boss, target.position().subtract(toward.normalize().scale(3)));
            level(boss).sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 1, 0.4, 0.03);
            boss.teleportTo(at.x, at.y, at.z);
            boss.setYRot(yawTo(boss.position(), target.position()));
            level(boss).sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 30, 0.4, 1, 0.4, 0.03);
        }
    }

    /**
     * Mounted: he lines up and charges straight through, trampling whatever is in the lane. The lane is shown on the
     * ground first.
     */
    public static class Charge extends BossAttack<LuciferEntity> {
        private final ResourceKey<DamageType> type;
        private final float damage;
        private Vec3 from = Vec3.ZERO, dir = Vec3.ZERO;
        private final java.util.Set<java.util.UUID> struck = new java.util.HashSet<>();

        public Charge(ResourceKey<DamageType> type, float damage) {
            super("charge", "mounted_charge", 26, 22, 18);
            this.type = type;
            this.damage = damage;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return horseman(boss).isMounted() ? 1 : 0;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            boss.getNavigation().stop();
            boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            from = boss.position();
            float yaw = yawTo(from, target.position());
            dir = dirOf(yaw);
            boss.setYRot(yaw);
            TelegraphMarker.line(level(boss), from, yaw, 2.4f, 22, TelegraphMarker.RED, windup + active);
            sound(boss, org.papiricoh.supernaturalcraft.registry.AllSounds.STEED_NEIGH.get(), 2.5f, 0.8f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            boss.setDeltaMovement(dir.x * 1.05, boss.getDeltaMovement().y, dir.z * 1.05);
            boss.hurtMarked = true;
            if (t % 4 == 0) sound(boss, org.papiricoh.supernaturalcraft.registry.AllSounds.STEED_GALLOP.get(), 1.6f, 1.0f);
            level(boss).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boss.getX(), boss.getY() + 0.2, boss.getZ(), 2, 0.4, 0.1, 0.4, 0.01);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(0.8))) {
                if (!struck.add(e.getUUID())) continue;
                hit(boss, e, type, damage);
                e.push(dir.x * 1.4, 0.5, dir.z * 1.4);
                e.hurtMarked = true;
            }
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
        }
    }
}
