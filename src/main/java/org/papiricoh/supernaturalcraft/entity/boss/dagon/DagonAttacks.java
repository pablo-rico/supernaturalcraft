package org.papiricoh.supernaturalcraft.entity.boss.dagon;

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
import static org.papiricoh.supernaturalcraft.british.BossKit.inBand;
import static org.papiricoh.supernaturalcraft.british.BossKit.inLane;
import static org.papiricoh.supernaturalcraft.british.BossKit.inRing;
import static org.papiricoh.supernaturalcraft.british.BossKit.opt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;

/**
 * Dagon's attacks (v0.41), numbers in {@link DagonBalance}: violet for her telekinesis, red for her hellfire. Each is shown on
 * the ground in its windup and lands only in ACTIVE.
 */
public final class DagonAttacks {

    private DagonAttacks() {
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Fling::new, 3), opt(Crush::new, 2), opt(Palm::new, 2.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Fling::new, 2), opt(Crush::new, 1.5f), opt(Palm::new, 2),
            opt(FireRing::new, 2.5f), opt(Pin::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Fling::new, 1.5f), opt(Crush::new, 1.5f), opt(Palm::new, 2),
            opt(FireRing::new, 2), opt(Pin::new, 1.5f), opt(Scream::new, 2.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static int lane(float length, int palette) {
        return Math.round(length * 10) + palette * 10000;
    }

    abstract static class Cued extends BossKit.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, DagonBalance.cue(clip), windup, active, recover);
        }
    }

    /** A flick of her hand: whoever is in the violet lane is hurled aside. */
    public static class Fling extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Fling() {
            super("fling", "fling", DagonBalance.FLING_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, DagonBalance.FLING_WIDTH, DagonBalance.FLING_LENGTH, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            for (LivingEntity e : inLane(boss, from, dir, DagonBalance.FLING_LENGTH, DagonBalance.FLING_WIDTH / 2)) {
                hit(boss, e, DamageTypes.MAGIC, DagonBalance.FLING_DAMAGE);
                Vec3 push = side.scale(boss.getRandom().nextBoolean() ? DagonBalance.FLING_PUSH : -DagonBalance.FLING_PUSH).add(dir.scale(0.6));
                e.setDeltaMovement(push.x, 0.55, push.z);
                e.hurtMarked = true;
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(yaw), lane(DagonBalance.FLING_LENGTH, BritishFxPayload.FORCE), from, 10);
            sound(boss, BritishRegistry.sound("dagon.fling"), 1.6f, 1f);
        }
    }

    /** A slowly closing fist: a violet circle under a hunter, hard when it closes. */
    public static class Crush extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Crush() {
            super("crush", "crush", DagonBalance.CRUSH_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, DagonBalance.CRUSH_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, DagonBalance.CRUSH_RADIUS)) {
                hit(boss, e, DamageTypes.MAGIC, DagonBalance.CRUSH_DAMAGE);
                e.setDeltaMovement(0, -0.6, 0);
                e.hurtMarked = true;
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(DagonBalance.CRUSH_RADIUS * 10), BritishFxPayload.FORCE, at, 10);
            sound(boss, BritishRegistry.sound("dagon.crush"), 1.6f, 1f);
        }
    }

    /** A palm of hellfire down a red lane. */
    public static class Palm extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Palm() {
            super("fire_palm", "fire_palm", DagonBalance.PALM_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, DagonBalance.PALM_WIDTH, DagonBalance.PALM_LENGTH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inLane(boss, from, dirOf(yaw), DagonBalance.PALM_LENGTH, DagonBalance.PALM_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.HELLFIRE, DagonBalance.PALM_DAMAGE);
                e.igniteForSeconds(DagonBalance.PALM_BURN);
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(yaw), lane(DagonBalance.PALM_LENGTH, BritishFxPayload.FIRE), from, 12);
            sound(boss, BritishRegistry.sound("dagon.fire"), 1.8f, 1f);
        }
    }

    /** Both arms swept down: a band of fire round her; only close to her is safe. */
    public static class FireRing extends Cued {
        private Vec3 at = Vec3.ZERO;

        public FireRing() {
            super("fire_ring", "fire_ring", DagonBalance.RING_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            at = boss.position();
            int life = windupLength(boss) + 4;
            TelegraphMarker.ring(level(boss), at, DagonBalance.RING_OUTER, TelegraphMarker.RED, life);
            TelegraphMarker.circle(level(boss), at, DagonBalance.RING_INNER, TelegraphMarker.SAFE, life);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inBand(boss, at, DagonBalance.RING_INNER, DagonBalance.RING_OUTER)) {
                hit(boss, e, AllDamageTypes.HELLFIRE, DagonBalance.RING_DAMAGE);
                e.igniteForSeconds(DagonBalance.RING_BURN);
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(DagonBalance.RING_OUTER * 10), BritishFxPayload.FIRE, at, 14);
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(DagonBalance.RING_OUTER * 5), BritishFxPayload.FIRE, at, 14);
            sound(boss, BritishRegistry.sound("dagon.fire"), 2f, 0.8f);
        }
    }

    /** A pointing finger: whoever stands in the small violet circle is pinned in the air and hurt. */
    public static class Pin extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Pin() {
            super("pin", "pin", DagonBalance.PIN_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, DagonBalance.PIN_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, DagonBalance.PIN_RADIUS)) {
                hit(boss, e, DamageTypes.MAGIC, DagonBalance.PIN_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, DagonBalance.PIN_HOLD / 2, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DagonBalance.PIN_HOLD, 3));
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(DagonBalance.PIN_RADIUS * 10), BritishFxPayload.FORCE, at, DagonBalance.PIN_HOLD);
            sound(boss, BritishRegistry.sound("dagon.fling"), 1.4f, 1.4f);
        }
    }

    /** Head back, mouth open: a shriek round her (red). */
    public static class Scream extends Cued {
        public Scream() {
            super("scream", "scream", DagonBalance.SCREAM_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            TelegraphMarker.circle(level(boss), boss.position(), DagonBalance.SCREAM_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, boss.position(), DagonBalance.SCREAM_RADIUS)) {
                hit(boss, e, DamageTypes.MAGIC, DagonBalance.SCREAM_DAMAGE);
                Vec3 out = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (out.lengthSqr() < 0.01) out = new Vec3(1, 0, 0);
                out = out.normalize().scale(DagonBalance.SCREAM_PUSH);
                e.setDeltaMovement(out.x, 0.5, out.z);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DagonBalance.SCREAM_DAZE, 0));
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.RING, Math.round(DagonBalance.SCREAM_RADIUS * 10), BritishFxPayload.FIRE, boss.position(), 12);
            sound(boss, BritishRegistry.sound("dagon.scream"), 2.4f, 1f);
        }
    }

    /** The hunter ran, or hid: she drags them back down a violet lane. */
    public static class Drag extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;
        private float length;

        public Drag() {
            super("drag", "fling", 16, 2, 12);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            length = (float) Math.min(40, boss.position().distanceTo(target.position()) + 2);
            TelegraphMarker.line(level(boss), from, yaw, 2.4f, length, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inLane(boss, from, dirOf(yaw), length, 1.2)) {
                Vec3 pull = boss.position().subtract(e.position()).multiply(1, 0, 1).normalize().scale(DagonBalance.PULL);
                e.setDeltaMovement(pull.x, 0.5, pull.z);
                e.hurtMarked = true;
            }
            BritishFxPayload.send(level(boss), boss.getId(), BritishFxPayload.LANE, Math.round(yaw), lane(length, BritishFxPayload.FORCE), from, 10);
            sound(boss, BritishRegistry.sound("dagon.fling"), 1.6f, 0.8f);
        }
    }
}
