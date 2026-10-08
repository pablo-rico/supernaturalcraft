package org.papiricoh.supernaturalcraft.entity.boss.michael;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.host.HostAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.LightSpearEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.projectile.SteelFeatherEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Michael's attacks, phase by phase.
 * <ul>
 *   <li>I The Vessel: his angel blade in three-cut combos, the touch to the forehead (parried or broken, or it smites),
 *   a shove of light.</li>
 *   <li>II The General: he commands the Host from behind it and only crosses blades with whoever comes close.</li>
 * </ul>
 * Every blow is shown on the ground first and lands only when the clip says it does (michael contract's timings).
 */
public final class MichaelAttacks {

    private MichaelAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(() -> new BladeCombo(1), 2), opt(() -> new BladeCombo(2), 2), opt(() -> new BladeCombo(3), 1.5f),
            opt(ForeheadTouch::new, 2), opt(LightShove::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Command::new, 3), opt(() -> new BladeCombo(1), 1.5f), opt(() -> new BladeCombo(3), 1.5f),
            opt(ForeheadTouch::new, 2), opt(LightShove::new, 1.5f), opt(SummonHost::new, 2));

    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(LanceThrow::new, 2), opt(LanceThrust::new, 2), opt(LanceSweep::new, 2), opt(LanceRecall::new, 1),
            opt(() -> new BladeCombo(1), 1), opt(() -> new BladeCombo(2), 1), opt(() -> new BladeCombo(3), 1),
            opt(ForeheadTouch::new, 1), opt(WingBuffet::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(
            opt(FeatherStorm::new, 3), opt(Dive::new, 2.5f), opt(WingBuffet::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P5 = List.of(
            opt(LanceSweepBig::new, 3), opt(LanceThrust::new, 2), opt(LanceThrow::new, 1.5f), opt(LanceRecall::new, 1),
            opt(HaloVolley::new, 2.5f), opt(Command::new, 2), opt(SummonHost::new, 1.5f), opt(() -> new FeatherStorm(2, 9), 2),
            opt(() -> new WingBuffet(8), 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P6 = List.of(
            opt(LanceSweepBig::new, 3), opt(LanceThrust::new, 2), opt(LanceThrow::new, 3), opt(LanceRecall::new, 1),
            opt(HaloVolley::new, 2.5f), opt(Command::new, 1), opt(() -> new FeatherStorm(2, 11), 2.5f), opt(() -> new WingBuffet(8), 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            case 4 -> P4;
            case 5 -> P5;
            default -> P6;
        };
    }

    /** Lance attacks need the lance in his hand; without it the blade is all he has (and he is quicker with it). */
    static float lanceWeight(LuciferEntity boss) {
        return michael(boss).lanceHeld() ? 1 : 0;
    }

    static float bladeWeight(LuciferEntity boss) {
        int phase = boss.phase();
        if (phase < MichaelBalance.SHADOW_WINGS_PHASE) return 1;
        return michael(boss).lanceHeld() ? 0.4f : 2.5f;
    }

    public static MichaelEntity michael(LuciferEntity boss) {
        return (MichaelEntity) boss;
    }

    // --- helpers -------------------------------------------------------------------------------------------------

    /** Whether Michael's blows should land on {@code e}: not himself, not his Host, nothing already gone. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof HostAngelEntity) && !(e instanceof MichaelEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** Everyone in a cone {@code reach} long and {@code halfAngle} degrees either side of {@code yaw}, from {@code from}. */
    public static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach, double halfAngle) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(halfAngle));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && (d < 0.8 || to.normalize().dot(dir) >= cos) && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} toward {@code yaw}. */
    public static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, float yaw, double length, double halfWidth) {
        Vec3 dir = dirOf(yaw);
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    /** Throws {@code e} away from {@code from}, {@code strength} blocks a tick across and {@code lift} up. */
    public static void knock(LivingEntity e, Vec3 from, double strength, double lift) {
        Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        away = away.normalize();
        e.push(away.x * strength, lift, away.z * strength);
        e.hurtMarked = true;
    }

    /** Turns him to face {@code target} (body and head), and returns that yaw. */
    public static float face(LuciferEntity boss, Vec3 target) {
        float yaw = yawTo(boss.position(), target);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    /**
     * An attack whose clip is cued so its blow falls at the end of the windup: the scheduler's own trigger is skipped
     * ({@code animation} is no clip) and the clip starts {@code hitTicks} before the windup ends. Longer windups show
     * their warning on the ground first.
     */
    public abstract static class Cued extends BossAttack<LuciferEntity> {
        protected final String clip;
        protected final int hitTicks;
        private boolean cued;

        protected Cued(String id, String clip, int hitTicks, int windup, int active, int recover) {
            super(id, "-", windup, active, recover);
            this.clip = clip;
            this.hitTicks = hitTicks;
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            int length = Math.max(1, Math.round(windup * boss.windupScale()));
            if (!cued && t >= length - hitTicks) {
                cued = true;
                boss.triggerAnim("action", clip);
            }
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            if (Math.max(1, Math.round(windup * boss.windupScale())) <= hitTicks) {
                cued = true;
                boss.triggerAnim("action", clip);
            }
        }
    }

    // --- I · the Vessel ------------------------------------------------------------------------------------------

    /**
     * A combo of his angel blade: each of the three clips is a different string of cuts (one, two or three), each cut
     * shown as a gold cone the moment before it falls and stepping in after the hunter.
     */
    public static class BladeCombo extends Cued {
        /** The blow lands this long into each clip (0.35 s). */
        static final int HIT = 7, GAP = 14;
        private final int cuts;
        private float yaw;

        public BladeCombo(int n) {
            super("blade_combo_" + n, "blade_combo_" + n, HIT, 16, GAP * (n - 1) + 2, 14);
            this.cuts = n;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return (boss.distanceToSqr(target) < 10 * 10 ? 1 : 0.2f) * bladeWeight(boss);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 4.0f, TelegraphMarker.GOLD, windup);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            // Closing in while he draws.
            Vec3 to = target.position().subtract(boss.position()).multiply(1, 0, 1);
            if (to.length() > 2.5) {
                Vec3 step = to.normalize().scale(0.22);
                boss.setDeltaMovement(step.x, boss.getDeltaMovement().y, step.z);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            cut(boss);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            for (int k = 1; k < cuts; k++) {
                if (t == k * GAP - HIT) {
                    // The next cut: step in, turn, show it and swing (each cut its own clip).
                    yaw = face(boss, target.position());
                    Vec3 step = dirOf(yaw).scale(0.5);
                    boss.setDeltaMovement(step.x, boss.getDeltaMovement().y, step.z);
                    TelegraphMarker.cone(level(boss), boss.position(), yaw, 4.0f, TelegraphMarker.GOLD, HIT);
                    boss.triggerAnim("action", "blade_combo_" + ((cuts + k - 1) % 3 + 1));
                }
                if (t == k * GAP) cut(boss);
            }
        }

        private void cut(LuciferEntity boss) {
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 0.6f, 1.8f);
            Vec3 dir = dirOf(yaw);
            level(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + dir.x * 1.6, boss.getY() + 1.1, boss.getZ() + dir.z * 1.6,
                    2, 0.4, 0.1, 0.4, 0);
            for (LivingEntity e : inCone(boss, boss.position(), yaw, 4.0, 55)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, 9);
            }
        }
    }

    /**
     * The touch to the forehead: his palm glows for 1.2 s as he reaches for one hunter. Break it with 30 true damage
     * before the hand closes, or turn it with a shield raised in its last half second (he reels for two seconds). If it
     * lands: a smiting of light, blindness, and the hunter thrown away.
     */
    public static class ForeheadTouch extends BossAttack<LuciferEntity> {
        /** Reach of the hand, in blocks. */
        public static final double REACH = 2.8;
        public static final float SMITE_DAMAGE = 20;
        /** True damage taken during the windup so far. */
        float taken;
        private boolean broken;

        public ForeheadTouch() {
            super("forehead_touch", "forehead_touch", MichaelBalance.TOUCH_WINDUP_TICKS, 4, 22);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 8 * 8 ? 1 : 0;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            TelegraphMarker.circle(level(boss), target.position(), 1.4f, TelegraphMarker.GOLD, windup);
            sound(boss, AllSounds.MICHAEL_SMITE.get(), 1.4f, 1.6f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (broken) return;
            float yaw = face(boss, target.position());
            Vec3 to = target.position().subtract(boss.position()).multiply(1, 0, 1);
            if (to.length() > REACH - 0.8) {
                Vec3 step = to.normalize().scale(0.2);
                boss.setDeltaMovement(step.x, boss.getDeltaMovement().y, step.z);
            }
            // The glowing palm, held out.
            Vec3 hand = boss.position().add(dirOf(yaw).scale(0.7)).add(0, 1.4, 0);
            level(boss).sendParticles(AllParticles.GRACE.get(), hand.x, hand.y, hand.z, 2 + t / 6, 0.1, 0.1, 0.1, 0.01);
        }

        /** Damage he took while reaching: enough of it breaks the touch. */
        public boolean wound(MichaelEntity michael, float trueDamage, LivingEntity by) {
            if (broken) return false;
            taken += trueDamage;
            if (taken < MichaelBalance.TOUCH_BREAK_SHARE * michael.trueMaxHealth()) return false;
            broken = true;
            michael.stagger(MichaelBalance.PARRY_STUN_TICKS, by instanceof ServerPlayer p ? p : null,
                    "message.supernaturalcraft.michael.touch_broken");
            return true;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (!broken) contact(michael(boss), target, parryTicks(target));
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return broken;
        }
    }

    /** How long {@code target} has held a shield up, or -1 if it is not blocking. */
    public static int parryTicks(LivingEntity target) {
        return target.isBlocking() ? target.getTicksUsingItem() : -1;
    }

    /** Whether a shield raised {@code ticksUp} ago turns his hand (-1: no shield up). */
    public static boolean parries(int ticksUp) {
        return ticksUp >= 0 && ticksUp <= MichaelBalance.TOUCH_PARRY_WINDOW;
    }

    /**
     * The hand closes on {@code target}: a shield raised in time turns it (he reels), one held up all along is struck
     * aside; out of reach, nothing. Returns whether the touch landed.
     */
    public static boolean contact(MichaelEntity boss, LivingEntity target, int shieldTicks) {
        if (flatDistance(boss.position(), target.position()) > ForeheadTouch.REACH + 0.6 || Math.abs(boss.getY() - target.getY()) > 2.5) {
            return false;
        }
        if (parries(shieldTicks)) {
            boss.stagger(MichaelBalance.PARRY_STUN_TICKS, target instanceof ServerPlayer p ? p : null, "message.supernaturalcraft.michael.parry");
            return false;
        }
        if (shieldTicks >= 0 && target instanceof ServerPlayer p) p.disableShield();
        boss.triggerAnim("action", "smite");
        sound(boss, AllSounds.MICHAEL_SMITE.get(), 2.5f, 1.0f);
        Vec3 head = target.getEyePosition();
        level(boss).sendParticles(ParticleTypes.END_ROD, head.x, head.y, head.z, 40, 0.3, 0.3, 0.3, 0.2);
        hit(boss, target, AllDamageTypes.SMITE, ForeheadTouch.SMITE_DAMAGE);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        knock(target, boss.position(), 2.2, 0.8);
        return true;
    }

    /** A shove of light all round him: everyone close is thrown back. */
    public static class LightShove extends BossAttack<LuciferEntity> {
        public LightShove() {
            super("light_shove", "smite", 18, 4, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return foes(boss, boss.getBoundingBox().inflate(4.5, 2, 4.5)).size() >= 1 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), boss.position(), 5f, TelegraphMarker.GOLD, windup);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 3, 0.6, 0.8, 0.6, 0.02);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.MICHAEL_CHOIR.get(), 2f, 1.4f);
            level(boss).sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY() + 1, boss.getZ(), 60, 2.5, 0.5, 2.5, 0.15);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(5, 2, 5))) {
                if (!inCircle(e, boss.position(), 5)) continue;
                hit(boss, e, AllDamageTypes.SMITE, 5);
                knock(e, boss.position(), 1.6, 0.6);
            }
        }
    }

    // --- II · the General ----------------------------------------------------------------------------------------

    /** He raises his blade and gives the Host its next order (charge, shield wall, encircle). */
    public static class Command extends BossAttack<LuciferEntity> {
        public Command() {
            super("command", "command", 20, 2, 10);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return michael(boss).livingHost() > 0 ? 1 : 0;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            michael(boss).orderHost(target);
            sound(boss, AllSounds.MICHAEL_TRUMPET.get(), 1.6f, 1.3f);
        }
    }

    /** The Host comes down again, once a phase, when it has been cut down. */
    public static class SummonHost extends BossAttack<LuciferEntity> {
        public SummonHost() {
            super("summon_host", "summon_host", 30, 4, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return michael(boss).livingHost() == 0 && michael(boss).hostReinforcements() > 0 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.MICHAEL_TRUMPET.get(), 3f, 1.0f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + 2.5, boss.getZ(), 4, 1.5, 0.5, 1.5, 0.05);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            MichaelEntity m = michael(boss);
            m.useReinforcement();
            m.summonHost(level(boss), m.phase() >= MichaelBalance.ARCHANGEL_PHASE ? 2 : 1);
        }
    }

    // --- III · the Lance ------------------------------------------------------------------------------------------

    /** He hurls the Lance: a line of light shows its flight; whoever it strikes is pinned, and it stays in the ground. */
    public static class LanceThrow extends Cued {
        private Vec3 at = Vec3.ZERO;

        public LanceThrow() {
            super("lance_throw", "lance_throw", 11, 26, 2, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return lanceWeight(boss) * (boss.distanceToSqr(target) > 5 * 5 ? 1.5f : 0.5f);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            float yaw = face(boss, target.position());
            TelegraphMarker.line(level(boss), boss.position(), yaw, 1.6f, (float) Math.max(4, flatDistance(boss.position(), target.position()) + 2),
                    TelegraphMarker.GOLD, windup);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            face(boss, target.position());
            // Lead the target a little: where it will be when the lance arrives.
            at = target.position().add(target.getDeltaMovement().multiply(6, 0, 6)).add(0, target.getBbHeight() * 0.5, 0);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (!michael(boss).lanceHeld()) return;
            michael(boss).throwLance(at.equals(Vec3.ZERO) ? target.position() : at);
        }
    }

    /** A straight thrust down a long lane. */
    public static class LanceThrust extends Cued {
        public static final double LENGTH = 7, ARCHANGEL_LENGTH = 10;
        private float yaw;
        private double length = LENGTH;

        public LanceThrust() {
            super("lance_thrust", "lance_thrust", 9, 22, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double reach = michael(boss).isArchangel() ? ARCHANGEL_LENGTH : LENGTH;
            return lanceWeight(boss) * (boss.distanceToSqr(target) < reach * reach ? 1 : 0.2f);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            length = michael(boss).isArchangel() ? ARCHANGEL_LENGTH : LENGTH;
            yaw = face(boss, target.position());
            TelegraphMarker.line(level(boss), boss.position(), yaw, 2.2f, (float) length, TelegraphMarker.GOLD, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.MICHAEL_LANCE_THROW.get(), 1.4f, 1.4f);
            Vec3 dir = dirOf(yaw);
            for (int i = 1; i <= length; i++) {
                level(boss).sendParticles(ParticleTypes.END_ROD, boss.getX() + dir.x * i, boss.getY() + 1.2, boss.getZ() + dir.z * i, 2, 0.1, 0.1, 0.1, 0.01);
            }
            for (LivingEntity e : inLane(boss, boss.position(), yaw, length, 1.1)) {
                hit(boss, e, AllDamageTypes.LANCE, 14);
                knock(e, boss.position(), 1.0, 0.3);
            }
        }
    }

    /** The Lance swept all round him. */
    public static class LanceSweep extends Cued {
        private final double radius;
        private final float damage;

        public LanceSweep() {
            this("lance_sweep", "lance_sweep", 4.5, 12);
        }

        protected LanceSweep(String id, String clip, double radius, float damage) {
            super(id, clip, 12, 26, 2, 18);
            this.radius = radius;
            this.damage = damage;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return lanceWeight(boss) * (boss.distanceToSqr(target) < (radius + 1) * (radius + 1) ? 1.2f : 0.2f);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            TelegraphMarker.circle(level(boss), boss.position(), (float) radius, TelegraphMarker.GOLD, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 1.8f, 1.2f);
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                level(boss).sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX() + Math.cos(a) * radius * 0.7, boss.getY() + 1.1,
                        boss.getZ() + Math.sin(a) * radius * 0.7, 1, 0, 0, 0, 0);
            }
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(radius, 2, radius))) {
                if (!inCircle(e, boss.position(), radius)) continue;
                hit(boss, e, AllDamageTypes.LANCE, damage);
                knock(e, boss.position(), 1.3, 0.4);
            }
        }
    }

    /** His true form's sweep: the Lance, four blocks long, all the way round. */
    public static class LanceSweepBig extends LanceSweep {
        public LanceSweepBig() {
            super("lance_sweep_big", "lance_sweep_big", 6.5, 16);
        }
    }

    /** He calls the Lance back to his hand (it tears out of the ground and flies to him). */
    public static class LanceRecall extends BossAttack<LuciferEntity> {
        public LanceRecall() {
            super("lance_recall", "lance_recall", 16, 40, 8);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            MichaelEntity m = michael(boss);
            return m.lanceHeld() || m.lanceStolen() ? 0 : 0.5f + m.lanceAwayTicks() / 80f;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            michael(boss).recallLance();
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return michael(boss).lanceHeld();
        }
    }

    /** His wings beat once: everyone near him is thrown away. */
    public static class WingBuffet extends BossAttack<LuciferEntity> {
        private final double radius;

        public WingBuffet() {
            this(6);
        }

        public WingBuffet(double radius) {
            super("wing_buffet", "wing_buffet", 18, 4, 16);
            this.radius = radius;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return flatDistance(boss.position(), target.position()) < radius + 2 ? 1 : 0.1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), (float) radius, TelegraphMarker.WHITE, windup);
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 1.5f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 3f, 0.6f);
            Vec3 c = floorAt(boss, boss.position());
            level(boss).sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, 50, radius / 2, 0.2, radius / 2, 0.15);
            for (LivingEntity e : foes(boss, new AABB(c, c).inflate(radius, 8, radius))) {
                if (flatDistance(e.position(), c) > radius) continue;
                hit(boss, e, DamageTypes.MOB_ATTACK, 4);
                knock(e, c, 2.0, 0.6);
            }
        }
    }

    // --- IV · the Wings ------------------------------------------------------------------------------------------

    /** Fans of steel feathers from his wings, three volleys at every hunter. */
    public static class FeatherStorm extends BossAttack<LuciferEntity> {
        private final int volleys, perFan;

        public FeatherStorm() {
            this(3, 7);
        }

        public FeatherStorm(int volleys, int perFan) {
            super("feather_storm", "feather_storm", 24, 8 * volleys, 18);
            this.volleys = volleys;
            this.perFan = perFan;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.MICHAEL_FEATHER_STORM.get(), 2.5f, 1.0f);
            for (ServerPlayer p : targets(boss, target)) {
                float yaw = yawTo(boss.position(), p.position());
                TelegraphMarker.cone(level(boss), floorAt(boss, p.position().subtract(dirOf(yaw).scale(3))), yaw, 5f, TelegraphMarker.WHITE, windup);
            }
        }

        private static List<ServerPlayer> targets(LuciferEntity boss, LivingEntity target) {
            List<ServerPlayer> all = boss.challengers();
            if (all.isEmpty() && target instanceof ServerPlayer p) return List.of(p);
            return all;
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 8 != 1 || t / 8 >= volleys) return;
            Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.75, 0);
            List<LivingEntity> aims = new java.util.ArrayList<>(targets(boss, target));
            if (aims.isEmpty()) aims.add(target);
            for (LivingEntity p : aims) {
                SteelFeatherEntity.fan(boss, from, p.position().add(0, p.getBbHeight() * 0.5, 0), perFan, 50f);
            }
            michael(boss).fx(level(boss), new org.papiricoh.supernaturalcraft.network.MichaelFxPayload(boss.getId(),
                    org.papiricoh.supernaturalcraft.network.MichaelFxPayload.FEATHER_BURST, perFan * aims.size(), 0, from, 20));
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 1.4f, 1.4f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
        }
    }

    /** From the sky onto a hunter: the spot is shown on the ground, then he falls on it. */
    public static class Dive extends Cued {
        public static final double RADIUS = 3.5;
        private Vec3 spot = Vec3.ZERO, from = Vec3.ZERO;

        public Dive() {
            super("dive", "dive", 18, 30, 6, 22);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.isAerialPhase() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, (float) RADIUS, TelegraphMarker.GOLD, windup + active);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            from = boss.position();
            sound(boss, AllSounds.MICHAEL_DIVE.get(), 2.5f, 1.0f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            double k = Math.min(1, t / (double) active);
            Vec3 at = from.lerp(spot, k);
            boss.teleportTo(at.x, at.y, at.z);
            boss.setDeltaMovement(Vec3.ZERO);
            if (t >= active) impact(boss);
        }

        private void impact(LuciferEntity boss) {
            sound(boss, AllSounds.MICHAEL_SMITE.get(), 3f, 0.7f);
            level(boss).sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 3, 1, 0.2, 1, 0);
            level(boss).sendParticles(ParticleTypes.END_ROD, spot.x, spot.y + 0.3, spot.z, 60, RADIUS / 2, 0.3, RADIUS / 2, 0.2);
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(RADIUS, 3, RADIUS))) {
                if (!inCircle(e, spot, RADIUS)) continue;
                hit(boss, e, AllDamageTypes.SMITE, 14);
                knock(e, spot, 1.8, 0.7);
            }
        }
    }

    /** He comes down to the ground to gather himself: the window of the aerial phase. */
    public static class Land extends BossAttack<LuciferEntity> {
        public Land() {
            super("land", "land", 24, 2, MichaelBalance.LAND_RECOVER_TICKS);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            michael(boss).setLanded(true);
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 2f, 0.7f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 c = floorAt(boss, boss.position());
            level(boss).sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.2, c.z, 30, 1.5, 0.1, 1.5, 0.05);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            MichaelEntity m = michael(boss);
            if (m.phase() != MichaelBalance.AERIAL_PHASE) return;
            m.setLanded(false);
            m.triggerAnim("action", "takeoff");
            m.setDeltaMovement(0, 0.6, 0);
            sound(boss, AllSounds.MICHAEL_WINGS.get(), 2.5f, 1.0f);
        }
    }

    // --- V · the Archangel, VI · the Sword of Heaven ----------------------------------------------------------

    /**
     * The spears of his halo leave it one by one, hang a moment pointing at the hunters (their marks shown on the ground)
     * and fly. Twelve of them; eight once the halo is broken.
     */
    public static class HaloVolley extends BossAttack<LuciferEntity> {
        public HaloVolley() {
            super("halo_volley", "halo_volley", 26, 24, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return michael(boss).isArchangel() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            MichaelEntity m = michael(boss);
            List<LivingEntity> aims = new java.util.ArrayList<>(boss.challengers());
            if (aims.isEmpty()) aims.add(target);
            int spears = m.haloBroken() ? MichaelBones.HALO_SPEARS - MichaelBones.BROKEN_SPEARS.size() : MichaelBones.HALO_SPEARS;
            Vec3 halo = boss.position().add(0, boss.getBbHeight() + 0.4, 0);
            sound(boss, AllSounds.MICHAEL_CHOIR.get(), 2.5f, 1.3f);
            for (int i = 0; i < spears; i++) {
                double a = i * Math.PI * 2 / spears;
                Vec3 from = halo.add(Math.cos(a) * 1.6, Math.sin(a) * 1.6 * 0.4, Math.sin(a) * 1.6);
                LivingEntity aim = aims.get(i % aims.size());
                Vec3 at = aim.position().add((boss.getRandom().nextDouble() - 0.5) * 2.5, aim.getBbHeight() * 0.5,
                        (boss.getRandom().nextDouble() - 0.5) * 2.5);
                level(boss).addFreshEntity(LightSpearEntity.raise(boss, from, at, windup + i * 2));
                TelegraphMarker.circle(level(boss), floorAt(boss, at), 1.2f, TelegraphMarker.WHITE, windup + i * 2 + 10);
            }
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 4 == 0) {
                level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + boss.getBbHeight() + 0.4, boss.getZ(), 6, 1.2, 0.3, 1.2, 0.03);
            }
        }
    }

    /** Tells the hunters something, in his voice. */
    public static void say(MichaelEntity boss, ServerLevel level, String key) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 64 * 64) p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), false);
        }
    }
}
