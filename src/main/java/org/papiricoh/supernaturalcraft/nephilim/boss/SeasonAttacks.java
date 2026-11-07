package org.papiricoh.supernaturalcraft.nephilim.boss;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * v0.41 core: the shapes the core's bosses attack in, each shown on the ground through its whole windup
 * ({@link TelegraphMarker}) and landing only in ACTIVE, its clip cued so its moment falls on the hit (the contract's
 * {@code HIT_TICKS}). A boss builds its pool from these with its own numbers ({@code *Balance}), colours (CLAUDE.md: red
 * fire, blue ice, violet telekinesis, gold holy, yellow smoke) and particles.
 */
public final class SeasonAttacks {

    private SeasonAttacks() {
    }

    public static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    /** One blow's shape and numbers. */
    public record Spec(String id, String clip, Map<String, Integer> cues, int windup, int recover, ResourceKey<DamageType> type,
                       float damage, int colour, ParticleOptions particle, @Nullable SoundEvent sound) {
        int cue() {
            return cues.getOrDefault(clip, 0);
        }
    }

    // --- helpers -----------------------------------------------------------------------------------------------------------

    static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID())
                && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : boss.challengers()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    public static List<LivingEntity> inRing(LuciferEntity boss, Vec3 at, float r) {
        return foes(boss, new AABB(at, at).inflate(r + 1, 3, r + 1)).stream().filter(e -> inCircle(e, at, r)).toList();
    }

    public static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    public static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(60));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 3.5 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

    public static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    static void burst(ServerLevel level, ParticleOptions p, Vec3 at, int n, double spread) {
        level.sendParticles(p, at.x, at.y + 0.4, at.z, n, spread, 0.3, spread, 0.04);
    }

    abstract static class Base extends RaphaelAttacks.Cued {
        final Spec s;

        Base(Spec s, int active) {
            super(s.id(), s.clip(), s.cue(), s.windup(), active, s.recover());
            this.s = s;
        }

        void land(LuciferEntity boss, LivingEntity e) {
            hit(boss, e, s.type(), s.damage());
        }

        void noise(LuciferEntity boss, Vec3 at) {
            if (s.sound() != null) level(boss).playSound(null, at.x, at.y, at.z, s.sound(), net.minecraft.sounds.SoundSource.HOSTILE, 2f, 1f);
        }
    }

    /** A cone before it, toward the target. */
    public static class Cone extends Base {
        private final float reach;
        private float yaw;

        public Cone(Spec s, float reach) {
            super(s, 2);
            this.reach = reach;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < (reach + 2) * (reach + 2) ? 1.5f : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, reach, s.colour(), windupLength(boss) + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inCone(boss, boss.position(), yaw, reach)) land(boss, e);
            burst(level(boss), s.particle(), boss.position().add(dirOf(yaw).scale(reach / 2)), 30, reach / 3);
            noise(boss, boss.position());
        }
    }

    /** A ring round itself ({@code onTarget}: round the target's feet instead). */
    public static class Ring extends Base {
        private final float radius;
        private final boolean onTarget;
        private Vec3 at = Vec3.ZERO;

        public Ring(Spec s, float radius, boolean onTarget) {
            super(s, 2);
            this.radius = radius;
            this.onTarget = onTarget;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            at = onTarget ? floorAt(boss, target.position()) : boss.position();
            TelegraphMarker.circle(level(boss), at, radius, s.colour(), windupLength(boss) + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, radius)) land(boss, e);
            burst(level(boss), s.particle(), at, 60, radius / 2);
            noise(boss, at);
        }
    }

    /** {@code count} lanes fanned {@code spread} degrees apart from itself toward the target. */
    public static class Lanes extends Base {
        private final float length, width, spread;
        private final int count;
        private float yaw;

        public Lanes(Spec s, int count, float spread, float length, float width) {
            super(s, 2);
            this.count = count;
            this.spread = spread;
            this.length = length;
            this.width = width;
        }

        private float yawOf(int i) {
            return yaw + (i - (count - 1) / 2f) * spread;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            for (int i = 0; i < count; i++) TelegraphMarker.line(level(boss), boss.position(), yawOf(i), width, length, s.colour(), windupLength(boss) + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            java.util.Set<LivingEntity> struck = new java.util.HashSet<>();
            for (int i = 0; i < count; i++) {
                Vec3 dir = dirOf(yawOf(i));
                for (LivingEntity e : inLane(boss, boss.position(), dir, length, width / 2)) if (struck.add(e)) land(boss, e);
                for (int k = 1; k <= length; k += 2) {
                    Vec3 p = boss.position().add(dir.scale(k));
                    level(boss).sendParticles(s.particle(), p.x, p.y + 0.6, p.z, 2, 0.15, 0.2, 0.15, 0.01);
                }
            }
            noise(boss, boss.position());
        }
    }

    /** A circle under the target that leaves an effect on whoever stood in it (and a little harm). */
    public static class Zone extends Base {
        private final float radius;
        private final Function<LivingEntity, MobEffectInstance> effect;
        private Vec3 at = Vec3.ZERO;

        public Zone(Spec s, float radius, Function<LivingEntity, MobEffectInstance> effect) {
            super(s, 2);
            this.radius = radius;
            this.effect = effect;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, radius, s.colour(), windupLength(boss) + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, radius)) {
                if (s.damage() > 0) land(boss, e);
                e.addEffect(effect.apply(e), boss);
            }
            burst(level(boss), s.particle(), at, 50, radius / 2);
            noise(boss, at);
        }
    }

    /** Calls minions at spots round the arena (red rings first), up to a cap. */
    public static class Summon extends Base {
        private final Supplier<EntityType<? extends Mob>> type;
        private final int perCall, cap;
        private final List<Vec3> spots = new ArrayList<>();

        public Summon(Spec s, Supplier<EntityType<? extends Mob>> type, int perCall, int cap) {
            super(s, 2);
            this.type = type;
            this.perCall = perCall;
            this.cap = cap;
        }

        private int standing(LuciferEntity boss) {
            int n = 0;
            for (java.util.UUID id : boss.minions()) if (level(boss).getEntity(id) instanceof Mob m && m.isAlive()) n++;
            return n;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return standing(boss) >= boss.minionCount(cap) ? 0f : 1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spots.clear();
            int n = Math.min(boss.minionCount(perCall), boss.minionCount(cap) - standing(boss));
            for (int i = 0; i < n; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, r = 5 + boss.getRandom().nextDouble() * 5;
                Vec3 p = floorAt(boss, boss.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
                spots.add(p);
                TelegraphMarker.ring(level(boss), p, 1.2f, TelegraphMarker.RED, windupLength(boss) + 2);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (Vec3 p : spots) {
                Mob m = type.get().create(level);
                if (m == null) continue;
                m.moveTo(p.x, p.y, p.z, boss.getRandom().nextFloat() * 360, 0);
                m.setTarget(target);
                m.setPersistenceRequired();
                level.addFreshEntity(m);
                boss.minions().add(m.getUUID());
                burst(level, s.particle(), p, 20, 0.5);
            }
            noise(boss, boss.position());
        }
    }

    /** A lane that drags whoever it catches toward the boss (telekinesis, a tendril). */
    public static class Pull extends Base {
        private final float length, width, pull;
        private float yaw;

        public Pull(Spec s, float length, float width, float pull) {
            super(s, 2);
            this.length = length;
            this.width = width;
            this.pull = pull;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.line(level(boss), boss.position(), yaw, width, length, s.colour(), windupLength(boss) + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inLane(boss, boss.position(), dirOf(yaw), length, width / 2)) {
                land(boss, e);
                Vec3 to = boss.position().subtract(e.position()).normalize().scale(pull);
                e.setDeltaMovement(to.x, 0.35, to.z);
                e.hurtMarked = true;
            }
            noise(boss, boss.position());
        }
    }

    /** It vanishes and steps out behind the target (forced when the target runs). */
    public static class Blink extends Base {
        public Blink(Spec s) {
            super(s, 1);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) > 100 ? 2f : 0.3f;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            burst(level(boss), s.particle(), boss.position(), 30, 0.4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 behind = floorAt(boss, target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(3)));
            boss.teleportTo(behind.x, behind.y, behind.z);
            face(boss, target.position());
            burst(level(boss), s.particle(), behind, 30, 0.4);
            noise(boss, behind);
        }
    }
}
