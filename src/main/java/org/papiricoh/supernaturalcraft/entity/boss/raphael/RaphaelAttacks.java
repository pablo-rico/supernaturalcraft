package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseGround;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseLayout;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.flatDistance;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.randomArenaPoint;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Raphael's attacks (v0.16). Lightning is always shown in violet first and lands only in ACTIVE (a bolt that is only light, and
 * his own blow); the smite's burst in gold; the snap's safe band in green.
 * <ul>
 *   <li>P1, the Storm: directed bolts, the thunderclap, the smite (interrupted by a heavy blow or a shield raised in time), the
 *   blink, lightning in lines through the broken windows.</li>
 *   <li>P2, the Healer: his garrison is called first ({@link CallGarrison}, forced); laying on hands raises one fallen angel;
 *   the storm goes on round them.</li>
 *   <li>P3, the Wrath of Heaven (no roof): the lightning field, chain lightning, the snap, and the smite and blink still.</li>
 * </ul>
 * The smite and the blink take him to places the hunters can learn: the middle of a ring of holy oil, often (the bait).
 */
public final class RaphaelAttacks {

    private RaphaelAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(DirectedBolts::new, 3), opt(Thunderclap::new, 2), opt(Smite::new, 2), opt(Blink::new, 1.5f), opt(WindowLightning::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(DirectedBolts::new, 2.5f), opt(Thunderclap::new, 2), opt(Smite::new, 2), opt(Blink::new, 1.5f), opt(WindowLightning::new, 1.5f),
            opt(LayingOnHands::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(LightningField::new, 3), opt(ChainLightning::new, 2.5f), opt(Snap::new, 2), opt(Smite::new, 1.5f), opt(Blink::new, 1),
            opt(DirectedBolts::new, 1.5f), opt(Thunderclap::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    public static RaphaelEntity raphael(LuciferEntity boss) {
        return (RaphaelEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether his storm should land on {@code e}: not himself, not his garrison, nobody in creative. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof RaphaelEntity) && !(e instanceof GarrisonAngelEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** The hunters to spread the storm over: every challenger (and the tracked ones), or the target alone. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(raphael(boss).hunters());
        if (!out.contains(target)) out.add(target);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    /** A bolt of his on everyone within {@code r} of {@code at}. */
    static void strikeCircle(LuciferEntity boss, Vec3 at, double r, float damage) {
        for (LivingEntity e : foes(boss, new AABB(at, at).inflate(r + 1, 4, r + 1))) {
            if (inCircle(e, at, r)) hit(boss, e, DamageTypes.LIGHTNING_BOLT, damage);
        }
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} along {@code dir}. */
    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    /**
     * An attack whose clip is cued so its moment (the snap, the clap, the arms raised to call the bolt) falls at the end of the
     * windup: the scheduler's own trigger is skipped ({@code animation} is no clip).
     */
    public abstract static class Cued extends BossAttack<LuciferEntity> {
        protected final String clip;
        protected final int cue;
        private boolean cued;

        protected Cued(String id, String clip, int cue, int windup, int active, int recover) {
            super(id, "-", windup, active, recover);
            this.clip = clip;
            this.cue = cue;
        }

        protected int windupLength(LuciferEntity boss) {
            return Math.max(1, Math.round(windup * boss.windupScale()));
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            if (windupLength(boss) <= cue) {
                cued = true;
                boss.triggerAnim("action", clip);
            }
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (!cued && t >= windupLength(boss) - cue) {
                cued = true;
                boss.triggerAnim("action", clip);
            }
        }
    }

    // --- P1 · the Storm --------------------------------------------------------------------------------------------------

    /** A bolt called down on every hunter (and one stray), each shown in violet first. */
    public static class DirectedBolts extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public DirectedBolts() {
            super("directed_bolts", "call_lightning", RaphaelBalance.CALL_CUE, RaphaelBalance.BOLT_WINDUP, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) if (spots.size() < 4) spots.add(floorAt(boss, e.position()));
            spots.add(randomArenaPoint(boss, 0.5));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, RaphaelBalance.BOLT_RADIUS, TelegraphMarker.VIOLET, windup + 4);
            sound(boss, AllSounds.RAPHAEL_THUNDER.get(), 1.2f, 1.4f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            RaphaelEntity r = raphael(boss);
            for (Vec3 s : spots) {
                r.bolt(s);
                strikeCircle(boss, s, RaphaelBalance.BOLT_RADIUS, RaphaelBalance.BOLT_DAMAGE);
            }
        }
    }

    /** A clap of thunder: a cone before him, and whoever is in it is thrown. */
    public static class Thunderclap extends Cued {
        private float yaw;

        public Thunderclap() {
            super("thunderclap", "thunderclap", RaphaelBalance.CLAP_CUE, 18, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < RaphaelBalance.CLAP_REACH * RaphaelBalance.CLAP_REACH ? 1 : 0.1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, RaphaelBalance.CLAP_REACH, TelegraphMarker.VIOLET, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            clap(boss, boss.position(), yaw, RaphaelBalance.CLAP_REACH, RaphaelBalance.CLAP_DAMAGE);
        }
    }

    /** The clap itself (also how he breaks out of a ring): everyone in the cone takes a blow and is thrown. */
    static void clap(LuciferEntity boss, Vec3 from, float yaw, double reach, float damage) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(RaphaelBalance.CLAP_HALF_ANGLE));
        ServerLevel level = level(boss);
        for (LivingEntity e : foes(boss, new AABB(from, from).inflate(reach, 4, reach))) {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach || Math.abs(e.getY() - from.y) > 3.5 || (d > 0.8 && to.normalize().dot(dir) < cos)) continue;
            if (damage > 0) hit(boss, e, AllDamageTypes.SPELL, damage);
            Vec3 away = d < 0.01 ? dir : to.normalize();
            e.push(away.x * RaphaelBalance.CLAP_SHOVE, 0.5, away.z * RaphaelBalance.CLAP_SHOVE);
            e.hurtMarked = true;
        }
        Vec3 c = from.add(dir.scale(reach / 2)).add(0, 1, 0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 30, reach / 3, 0.5, reach / 3, 0.1);
        sound(boss, AllSounds.RAPHAEL_THUNDER.get(), 3f, 0.8f);
    }

    /**
     * The smite: his hand raised for {@link RaphaelBalance#SMITE_WINDUP} ticks, then a burst of light round him (what burst
     * Castiel). Often he first steps into a laid ring of holy oil. A heavy enough blow while his hand is up stops it; so does a
     * shield raised just before the hand closes. Either way he reels.
     */
    public static class Smite extends BossAttack<LuciferEntity> {
        /** True damage taken during the windup so far. */
        float taken;
        boolean broken;

        public Smite() {
            // The clip is started in onWindup, after the step into a ring (whose flinch would cut it short).
            super("smite", "-", RaphaelBalance.SMITE_WINDUP, 4, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 10 * 10 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            RaphaelEntity r = raphael(boss);
            HouseGround g = r.ground();
            if (g != null && boss.getRandom().nextFloat() < RaphaelBalance.SMITE_BAIT_CHANCE) {
                int ring = r.rings().nearestLaid(g, boss.position(), RaphaelBalance.BAIT_REACH * 0.85);
                if (ring >= 0) r.blinkTo(OilRings.centre(g, ring));
            }
            face(boss, target.position());
            boss.triggerAnim("action", "smite");
            TelegraphMarker.circle(level(boss), boss.position(), RaphaelBalance.SMITE_RADIUS, TelegraphMarker.GOLD, windup + 4);
            sound(boss, AllSounds.RAPHAEL_SMITE.get(), 1.4f, 1.5f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (broken) return;
            Vec3 hand = boss.position().add(0, 2.4, 0);
            level(boss).sendParticles(AllParticles.GRACE.get(), hand.x, hand.y, hand.z, 2 + t / 6, 0.15, 0.15, 0.15, 0.01);
        }

        /** Damage he took with his hand up: enough of it stops the smite. */
        public boolean wound(RaphaelEntity raphael, float trueDamage, LivingEntity by) {
            if (broken) return false;
            taken += trueDamage;
            if (!RaphaelBalance.interrupts(taken, raphael.trueMaxHealth())) return false;
            broken = true;
            raphael.stagger(by instanceof ServerPlayer p ? p : null, "message.supernaturalcraft.raphael.smite_broken");
            return true;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (!broken) smite(raphael(boss), raphael(boss).hunters());
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return broken;
        }
    }

    /**
     * The hand closes: a shield raised in time by anyone within the burst turns it (he reels); otherwise everyone within it is
     * struck and thrown, and a shield held up all along is beaten aside. {@code hunters} are those who might parry (the burst
     * itself finds its victims). @return whether it landed
     */
    public static boolean smite(RaphaelEntity boss, List<? extends LivingEntity> hunters) {
        Vec3 at = boss.position();
        for (LivingEntity h : hunters) {
            if (!inCircle(h, at, RaphaelBalance.SMITE_RADIUS)) continue;
            int up = h.isBlocking() ? h.getTicksUsingItem() : -1;
            if (RaphaelBalance.parries(up)) {
                boss.stagger(h instanceof ServerPlayer p ? p : null, "message.supernaturalcraft.raphael.parry");
                return false;
            }
        }
        ServerLevel level = level(boss);
        sound(boss, AllSounds.RAPHAEL_SMITE.get(), 3f, 1.0f);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1.5, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.2, at.z, 80, RaphaelBalance.SMITE_RADIUS / 2, 0.6, RaphaelBalance.SMITE_RADIUS / 2, 0.2);
        List<LivingEntity> victims = new ArrayList<>(foes(boss, boss.getBoundingBox().inflate(RaphaelBalance.SMITE_RADIUS + 1, 3, RaphaelBalance.SMITE_RADIUS + 1)));
        for (LivingEntity h : hunters) if (!victims.contains(h) && isFoe(boss, h)) victims.add(h);
        for (LivingEntity e : victims) {
            if (!inCircle(e, at, RaphaelBalance.SMITE_RADIUS)) continue;
            if (e.isBlocking() && e instanceof ServerPlayer p) p.disableShield();
            hit(boss, e, AllDamageTypes.SMITE, RaphaelBalance.SMITE_DAMAGE);
            Vec3 away = e.position().subtract(at).multiply(1, 0, 1);
            if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
            away = away.normalize();
            e.push(away.x * 1.4, 0.6, away.z * 1.4);
            e.hurtMarked = true;
        }
        return true;
    }

    /** A flinch of wings and he is elsewhere: behind the hunter, or (the bait) in the middle of a ring of holy oil. */
    public static class Blink extends BossAttack<LuciferEntity> {
        private Vec3 to = Vec3.ZERO;

        public Blink() {
            // The flinch is cued as he goes (blinkTo), not as the windup starts.
            super("blink", "-", 12, 2, 10);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            RaphaelEntity r = raphael(boss);
            HouseGround g = r.ground();
            int ring = g == null ? -1 : r.rings().nearestLaid(g, target.position(), RaphaelBalance.BAIT_REACH);
            if (ring >= 0 && boss.getRandom().nextFloat() < 0.5f) {
                to = OilRings.centre(g, ring);
            } else {
                Vec3 back = target.getLookAngle().multiply(1, 0, 1);
                if (back.lengthSqr() < 0.01) back = new Vec3(0, 0, 1);
                to = floorAt(boss, target.position().subtract(back.normalize().scale(RaphaelBalance.BLINK_BEHIND)));
            }
            TelegraphMarker.circle(level(boss), to, 0.9f, TelegraphMarker.VIOLET, windup + 4);
            sound(boss, AllSounds.RAPHAEL_WINGS.get(), 1.5f, 1.2f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            raphael(boss).blinkTo(to);
            face(boss, target.position());
        }
    }

    /** Through the broken windows: lightning runs in lines across the house (each shown in violet first). */
    public static class WindowLightning extends Cued {
        private final List<HouseLayout.Window> windows = new ArrayList<>();
        private final List<Vec3> froms = new ArrayList<>();

        public WindowLightning() {
            super("window_lightning", "call_lightning", RaphaelBalance.CALL_CUE, 30, 4, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return raphael(boss).ground() != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            HouseGround g = raphael(boss).ground();
            if (g == null) return;
            // The windows whose lanes pass nearest the hunters first, then any.
            List<HouseLayout.Window> all = new ArrayList<>(HouseLayout.WINDOWS);
            Collections.shuffle(all, new java.util.Random(boss.getRandom().nextLong()));
            List<LivingEntity> marks = marks(boss, target);
            all.sort((a, b) -> Double.compare(laneMiss(g, a, marks), laneMiss(g, b, marks)));
            for (int i = 0; i < RaphaelBalance.WINDOW_VOLLEY && i < all.size(); i++) {
                HouseLayout.Window w = all.get(i);
                windows.add(w);
                Vec3 from = Vec3.atBottomCenterOf(g.onFloor(new HouseLayout.Spot(w.dx(), w.dz())));
                froms.add(from);
                float yaw = yawTo(from, from.add(w.inX(), 0, w.inZ()));
                TelegraphMarker.line(level(boss), from, yaw, RaphaelBalance.WINDOW_HALF_WIDTH * 2, w.length(), TelegraphMarker.VIOLET, windup + 4);
            }
        }

        private static double laneMiss(HouseGround g, HouseLayout.Window w, List<LivingEntity> marks) {
            Vec3 from = Vec3.atBottomCenterOf(g.onFloor(new HouseLayout.Spot(w.dx(), w.dz())));
            Vec3 dir = new Vec3(w.inX(), 0, w.inZ());
            double best = Double.MAX_VALUE;
            for (LivingEntity e : marks) {
                Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
                double along = to.dot(dir);
                if (along < 0 || along > w.length()) continue;
                best = Math.min(best, to.subtract(dir.scale(along)).length());
            }
            return best;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            RaphaelEntity r = raphael(boss);
            ServerLevel level = level(boss);
            for (int i = 0; i < windows.size(); i++) {
                HouseLayout.Window w = windows.get(i);
                Vec3 from = froms.get(i), dir = new Vec3(w.inX(), 0, w.inZ());
                for (int k = 1; k <= w.length(); k++) {
                    Vec3 p = from.add(dir.scale(k));
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.8, p.z, 3, 0.3, 0.4, 0.3, 0.1);
                }
                r.bolt(from.add(dir.scale(w.length() / 2.0)));
                for (LivingEntity e : inLane(boss, from, dir, w.length(), RaphaelBalance.WINDOW_HALF_WIDTH)) {
                    hit(boss, e, DamageTypes.LIGHTNING_BOLT, RaphaelBalance.WINDOW_DAMAGE);
                }
            }
        }
    }

    // --- P2 · the Healer -------------------------------------------------------------------------------------------------

    /** He calls his garrison: an angel at each post, each holding a thread of grace to him. Forced once, as the phase opens. */
    public static class CallGarrison extends BossAttack<LuciferEntity> {
        public CallGarrison() {
            super("call_garrison", "heal_channel", 30, 4, 20);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + 2.5, boss.getZ(), 4, 1.5, 0.5, 1.5, 0.05);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            raphael(boss).callGarrison(level(boss));
        }
    }

    /** Laying on hands: one fallen angel of his garrison gets up again (once a phase). */
    public static class LayingOnHands extends BossAttack<LuciferEntity> {
        public LayingOnHands() {
            super("laying_on_hands", "heal_channel", 26, 4, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return raphael(boss).canRaise() ? 1 : 0;
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + 1.4, boss.getZ(), 3, 0.5, 0.5, 0.5, 0.03);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            raphael(boss).raiseFallen(level(boss));
        }
    }

    // --- P3 · the Wrath of Heaven ----------------------------------------------------------------------------------------

    /** The lightning field: half the floor's grid at once, every other cell, shown in violet first. */
    public static class LightningField extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public LightningField() {
            super("lightning_field", "call_lightning", RaphaelBalance.CALL_CUE, 36, 4, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            var arena = boss.arena();
            Vec3 c = arena != null ? arena.centerVec() : boss.position();
            int parity = boss.getRandom().nextInt(2);
            int n = RaphaelBalance.FIELD_REACH / RaphaelBalance.FIELD_SPACING;
            for (int i = -n; i <= n; i++) {
                for (int j = -n; j <= n; j++) {
                    if (!RaphaelBalance.fieldStrikes(i, j, parity)) continue;
                    spots.add(floorAt(boss, c.add(i * RaphaelBalance.FIELD_SPACING, 0, j * RaphaelBalance.FIELD_SPACING)));
                }
            }
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, RaphaelBalance.FIELD_RADIUS, TelegraphMarker.VIOLET, windup + 4);
            sound(boss, AllSounds.RAPHAEL_THUNDER.get(), 2f, 0.7f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            RaphaelEntity r = raphael(boss);
            ServerLevel level = level(boss);
            List<Vec3> shuffled = new ArrayList<>(spots);
            Collections.shuffle(shuffled, new java.util.Random(boss.getRandom().nextLong()));
            for (int i = 0; i < shuffled.size(); i++) {
                Vec3 s = shuffled.get(i);
                if (i < RaphaelBalance.FIELD_BOLTS) r.bolt(s);
                else level.sendParticles(ParticleTypes.ELECTRIC_SPARK, s.x, s.y + 0.5, s.z, 20, 0.8, 0.8, 0.8, 0.2);
                strikeCircle(boss, s, RaphaelBalance.FIELD_RADIUS, RaphaelBalance.FIELD_DAMAGE);
            }
        }
    }

    /** Lightning that leaps from hunter to hunter, weaker at each leap. */
    public static class ChainLightning extends Cued {
        public ChainLightning() {
            super("chain_lightning", "call_lightning", RaphaelBalance.CALL_CUE, 22, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            float yaw = face(boss, target.position());
            TelegraphMarker.line(level(boss), boss.position(), yaw, 1.6f, (float) Math.max(2, flatDistance(boss.position(), target.position())),
                    TelegraphMarker.VIOLET, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            List<LivingEntity> pool = new ArrayList<>(marks(boss, target));
            pool.addAll(foes(boss, boss.getBoundingBox().inflate(24, 6, 24)));
            chain(raphael(boss), target, pool);
        }
    }

    /** The chain from {@code first} through {@code pool}. @return everyone it struck, in order */
    public static List<LivingEntity> chain(RaphaelEntity boss, LivingEntity first, List<LivingEntity> pool) {
        List<LivingEntity> struck = new ArrayList<>();
        ServerLevel level = level(boss);
        LivingEntity at = first;
        Vec3 from = boss.position().add(0, 2, 0);
        float damage = RaphaelBalance.CHAIN_DAMAGE;
        while (at != null && struck.size() < RaphaelBalance.CHAIN_TARGETS) {
            struck.add(at);
            Vec3 to = at.position().add(0, at.getBbHeight() / 2, 0);
            arc(level, from, to);
            hit(boss, at, DamageTypes.LIGHTNING_BOLT, damage);
            boss.flash(at.position());
            damage *= RaphaelBalance.CHAIN_DECAY;
            from = to;
            LivingEntity next = null;
            double best = RaphaelBalance.CHAIN_REACH * RaphaelBalance.CHAIN_REACH;
            for (LivingEntity e : pool) {
                if (struck.contains(e) || !isFoe(boss, e)) continue;
                double d = e.distanceToSqr(at);
                if (d <= best) {
                    best = d;
                    next = e;
                }
            }
            at = next;
        }
        sound(boss, AllSounds.RAPHAEL_THUNDER.get(), 2f, 1.3f);
        return struck;
    }

    static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        int n = Math.max(2, (int) (d.length() * 2));
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(d.scale(i / (double) n));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.02);
        }
    }

    /** The snap of his fingers: a great burst round him, but for a band of green halfway out. */
    public static class Snap extends Cued {
        private Vec3 centre = Vec3.ZERO;

        public Snap() {
            super("snap", "snap", RaphaelBalance.SNAP_CUE, RaphaelBalance.SNAP_WINDUP, 4, 22);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            centre = boss.position();
            ServerLevel level = level(boss);
            int r = RaphaelBalance.SNAP_RADIUS, w = RaphaelBalance.SNAP_SAFE_WIDTH;
            TelegraphMarker.ring(level, centre, r, TelegraphMarker.VIOLET, windup + 4);
            TelegraphMarker.ring(level, centre, (float) RaphaelBalance.snapSafeInner(r, w), TelegraphMarker.SAFE, windup + 4);
            TelegraphMarker.ring(level, centre, (float) RaphaelBalance.snapSafeOuter(r, w), TelegraphMarker.SAFE, windup + 4);
            raphael(boss).snapFx(centre, windup);
            sound(boss, AllSounds.RAPHAEL_THUNDER.get(), 2.5f, 0.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            snap(raphael(boss), centre, marks(boss, target));
        }
    }

    /** The snap lands: everyone within its radius but outside the safe band is struck. @return who was struck */
    public static List<LivingEntity> snap(RaphaelEntity boss, Vec3 centre, List<? extends LivingEntity> also) {
        int r = RaphaelBalance.SNAP_RADIUS, w = RaphaelBalance.SNAP_SAFE_WIDTH;
        ServerLevel level = level(boss);
        sound(boss, AllSounds.RAPHAEL_SNAP.get(), 4f, 1.0f);
        level.sendParticles(ParticleTypes.FLASH, centre.x, centre.y + 1, centre.z, 2, 0, 0, 0, 0);
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2 / 36;
            for (double d : new double[]{2, r * 0.25, r - 1}) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, centre.x + Math.cos(a) * d, centre.y + 0.4, centre.z + Math.sin(a) * d, 2, 0.2, 0.3, 0.2, 0.05);
            }
        }
        for (int i = 0; i < 4; i++) {
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            boss.bolt(floorAt(boss, centre.add(Math.cos(a) * (r - 2), 0, Math.sin(a) * (r - 2))));
        }
        List<LivingEntity> victims = new ArrayList<>(foes(boss, new AABB(centre, centre).inflate(r + 1, 4, r + 1)));
        for (LivingEntity e : also) if (!victims.contains(e) && isFoe(boss, e)) victims.add(e);
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity e : victims) {
            double d = flatDistance(e.position(), centre);
            if (Math.abs(e.getY() - centre.y) > 4 || !RaphaelBalance.snapHits(d, r, w)) continue;
            hit(boss, e, AllDamageTypes.SMITE, RaphaelBalance.SNAP_DAMAGE);
            struck.add(e);
        }
        return struck;
    }
}
