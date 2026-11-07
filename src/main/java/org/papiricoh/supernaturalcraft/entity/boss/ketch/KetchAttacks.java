package org.papiricoh.supernaturalcraft.entity.boss.ketch;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.british.BossKit;
import org.papiricoh.supernaturalcraft.british.BritishFxPayload;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

import java.util.List;

import static org.papiricoh.supernaturalcraft.british.BossKit.face;
import static org.papiricoh.supernaturalcraft.british.BossKit.inCone;
import static org.papiricoh.supernaturalcraft.british.BossKit.inLane;
import static org.papiricoh.supernaturalcraft.british.BossKit.inRing;
import static org.papiricoh.supernaturalcraft.british.BossKit.opt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;

/**
 * Ketch's attacks (v0.41), numbers in {@link KetchBalance}. Each is shown on the ground in its windup and lands only in
 * ACTIVE: red for his rounds and his knife, violet for the hex of his grenades, yellow for his smoke.
 */
public final class KetchAttacks {

    private KetchAttacks() {
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Shot::new, 3), opt(Volley::new, 2), opt(Slash::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Shot::new, 2), opt(Volley::new, 2), opt(Slash::new, 2),
            opt(Lunge::new, 2.5f), opt(Grenade::new, 2.5f), opt(Taunt::new, 0.4f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Volley::new, 3), opt(Slash::new, 1.5f),
            opt(Lunge::new, 2.5f), opt(Grenade::new, 2.5f), opt(Vanish::new, 2.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static KetchEntity ketch(LuciferEntity boss) {
        return (KetchEntity) boss;
    }

    static int lane(float length, int palette) {
        return Math.round(length * 10) + palette * 10000;
    }

    abstract static class Cued extends BossKit.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, KetchBalance.cue(clip), windup, active, recover);
        }
    }

    /** One aimed shot down a red lane. */
    public static class Shot extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Shot() {
            super("shot", "shoot", KetchBalance.SHOT_WINDUP, 2, 12);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, KetchBalance.SHOT_WIDTH, KetchBalance.SHOT_LENGTH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inLane(boss, from, dirOf(yaw), KetchBalance.SHOT_LENGTH, KetchBalance.SHOT_WIDTH / 2)) {
                hit(boss, e, DamageTypes.MOB_PROJECTILE, KetchBalance.SHOT_DAMAGE);
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(yaw), lane(KetchBalance.SHOT_LENGTH, BritishFxPayload.GREY), from, 6);
            sound(boss, BritishRegistry.sound("ketch.shot"), 1.6f, 1f);
        }
    }

    /** Three quick shots, fanned. */
    public static class Volley extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Volley() {
            super("volley", "volley", KetchBalance.VOLLEY_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            for (int k = -1; k <= 1; k++) {
                TelegraphMarker.line(level(boss), from, yaw + k * KetchBalance.VOLLEY_SPREAD, KetchBalance.VOLLEY_WIDTH, KetchBalance.VOLLEY_LENGTH,
                        TelegraphMarker.RED, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int k = -1; k <= 1; k++) {
                float y = yaw + k * KetchBalance.VOLLEY_SPREAD;
                for (LivingEntity e : inLane(boss, from, dirOf(y), KetchBalance.VOLLEY_LENGTH, KetchBalance.VOLLEY_WIDTH / 2)) {
                    hit(boss, e, DamageTypes.MOB_PROJECTILE, KetchBalance.VOLLEY_DAMAGE);
                }
                BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(y), lane(KetchBalance.VOLLEY_LENGTH, BritishFxPayload.GREY), from, 6);
            }
            sound(boss, BritishRegistry.sound("ketch.shot"), 1.6f, 1.15f);
        }
    }

    /** The knife, for whoever comes close: a red cone. */
    public static class Slash extends Cued {
        private float yaw;

        public Slash() {
            super("knife_slash", "knife_slash", KetchBalance.SLASH_WINDUP, 2, 10);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 25 ? 2f : 0.1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, KetchBalance.SLASH_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inCone(boss, boss.position(), yaw, KetchBalance.SLASH_REACH)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, KetchBalance.SLASH_DAMAGE);
            }
            sound(boss, BritishRegistry.sound("ketch.knife"), 1.4f, 1.1f);
        }
    }

    /** A lunge with the knife along a red lane; he ends where it ends. */
    public static class Lunge extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Lunge() {
            super("knife_lunge", "knife_lunge", KetchBalance.LUNGE_WINDUP, 2, 14);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double d = boss.distanceToSqr(target);
            return d > 16 && d < KetchBalance.LUNGE_LENGTH * KetchBalance.LUNGE_LENGTH ? 2f : 0.3f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, KetchBalance.LUNGE_WIDTH, KetchBalance.LUNGE_LENGTH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : inLane(boss, from, dir, KetchBalance.LUNGE_LENGTH, KetchBalance.LUNGE_WIDTH / 2)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, KetchBalance.LUNGE_DAMAGE);
            }
            Vec3 end = floorAt(boss, from.add(dir.scale(KetchBalance.LUNGE_LENGTH - 1)));
            boss.teleportTo(end.x, end.y, end.z);
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(yaw), lane(KetchBalance.LUNGE_LENGTH, BritishFxPayload.SMOKE), from, 8);
            sound(boss, BritishRegistry.sound("ketch.knife"), 1.4f, 0.9f);
        }
    }

    /** A hex grenade: a violet circle where it bursts. */
    public static class Grenade extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Grenade() {
            super("grenade", "grenade_throw", KetchBalance.GRENADE_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, KetchBalance.GRENADE_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, KetchBalance.GRENADE_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, KetchBalance.GRENADE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KetchBalance.GRENADE_HEX, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, KetchBalance.GRENADE_HEX, 0));
            }
            level(boss).sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.5, at.z, 50, 1.5, 0.5, 1.5, 0.05);
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(KetchBalance.GRENADE_RADIUS * 10), BritishFxPayload.HEX, at, 10);
            sound(boss, BritishRegistry.sound("ketch.grenade"), 1.6f, 1f);
        }
    }

    /** Smoke round him (yellow): who is in it is blinded, and he steps out of it behind his target. */
    public static class Vanish extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Vanish() {
            super("vanish", "smoke_vanish", KetchBalance.SMOKE_WINDUP, 2, 12);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            at = boss.position();
            TelegraphMarker.circle(level(boss), at, KetchBalance.SMOKE_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, KetchBalance.SMOKE_RADIUS)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, KetchBalance.SMOKE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, KetchBalance.SMOKE_BLIND, 0));
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.CLOUD, Math.round(KetchBalance.SMOKE_RADIUS * 10), BritishFxPayload.SMOKE, at, 50);
            Vec3 behind = target.position().subtract(dirOf(target.getYRot()).scale(KetchBalance.VANISH_BEHIND));
            Vec3 to = floorAt(boss, behind);
            boss.teleportTo(to.x, to.y, to.z);
            face(boss, target.position());
            sound(boss, BritishRegistry.sound("ketch.smoke"), 1.6f, 1f);
        }
    }

    /** He adjusts his cuffs and says something polite (no harm). */
    public static class Taunt extends Cued {
        public Taunt() {
            super("taunt", "taunt", 12, 1, 16);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ketch(boss).taunt();
        }
    }
}
