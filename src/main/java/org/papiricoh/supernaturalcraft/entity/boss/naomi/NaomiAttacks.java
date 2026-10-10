package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.HeavenFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.flatDistance;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Naomi's eight attacks (v0.18). Every one is shown on the floor first and lands only in ACTIVE: gold for her hand (the palm, the
 * restraint, the strap, her guards' alcoves, the console), red for the drill, white for the wipe with green lanes through it.
 * <ol>
 *   <li>{@link Palm} Palm of Correction: a heavy blow ahead of her; a raised shield parries it and she staggers.</li>
 *   <li>{@link Restraint} Restraint Field: a ring round the hunter; whoever is inside when it closes is held.</li>
 *   <li>{@link StrapIn} Strap In: a line from a chair to the most isolated hunter (an angel of rank II or more first): still on it
 *   when it closes, they are strapped in ({@link ReprogrammingChairEntity}).</li>
 *   <li>{@link DrillLance}: a cone and a lunge with the drill, bleeding; three in a row in phase 2.</li>
 *   <li>{@link MemoryWipe}: a white ring sweeps out from her; standing in a green lane is the only shelter.</li>
 *   <li>{@link CallGuards}: two or three guards from the alcoves (while two stand she takes less).</li>
 *   <li>{@link Test} Training Test: kneeling friends and hostile shapes on the test floor ({@link TrainingTest}).</li>
 *   <li>{@link Recalibration}: she heals at her console until six blows land on it.</li>
 * </ol>
 * {@link Approach} is the gap closer when the hunters run or hide.
 */
public final class NaomiAttacks {

    private NaomiAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Palm::new, 3), opt(Restraint::new, 2), opt(StrapIn::new, 2), opt(() -> new DrillLance(1), 2.5f), opt(MemoryWipe::new, 1.5f),
            opt(CallGuards::new, 1.5f), opt(Test::new, 1.5f), opt(Recalibration::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Palm::new, 2), opt(Restraint::new, 2), opt(StrapIn::new, 2.5f), opt(() -> new DrillLance(2), 3), opt(MemoryWipe::new, 2.5f),
            opt(CallGuards::new, 1.5f), opt(Test::new, 2), opt(Recalibration::new, 1.2f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return phase <= 1 ? P1 : P2;
    }

    public static NaomiEntity naomi(LuciferEntity boss) {
        return (NaomiEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether her blows should land on {@code e}: not herself, her guards or her copies, nobody in creative. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof NaomiEntity) && !(e instanceof HeavenGuardEntity)
                && !(e instanceof TrainingCopyEntity) && !boss.minions().contains(e.getUUID())
                && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** Foes in {@code box} and the hunters she tracks (fake players are not in the level's lists). */
    static List<LivingEntity> victims(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(foes(boss, box));
        for (LivingEntity h : naomi(boss).hunters()) if (!out.contains(h) && isFoe(boss, h)) out.add(h);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    /** One of the drill's blows: deeper into a demon, and it bleeds. */
    public static void drillHit(LuciferEntity boss, LivingEntity e, float damage) {
        hit(boss, e, DamageTypes.MOB_ATTACK, damage * (Kin.isDemon(e) ? NaomiBalance.DEMON_DRILL : 1f));
        e.addEffect(new MobEffectInstance(AllMobEffects.BLEEDING, NaomiBalance.BLEED_TICKS, NaomiBalance.BLEED_LEVEL), boss);
    }

    private static void shove(LivingEntity e, Vec3 from, double strength) {
        Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        away = away.normalize();
        e.push(away.x * strength, 0.45, away.z * strength);
        e.hurtMarked = true;
    }

    // --- 1 · Palm of Correction -----------------------------------------------------------------------------------------

    /** An open hand ahead of her: a heavy blow, unless a raised shield turns it (she staggers). */
    public static class Palm extends BossAttack<LuciferEntity> {
        private Vec3 at = Vec3.ZERO;

        public Palm() {
            super("palm", NaomiAnimations.PALM_STRIKE, NaomiBalance.PALM_WINDUP, 3, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 6 * 6 ? 1 : 0.15f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = face(boss, target.position());
            at = floorAt(boss, boss.position().add(dirOf(yaw).scale(NaomiBalance.PALM_AHEAD)));
            TelegraphMarker.circle(level(boss), at, NaomiBalance.PALM_RADIUS, TelegraphMarker.GOLD, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            palm(naomi(boss), at);
        }
    }

    /** The palm lands at {@code at}: a shield raised in it parries (she staggers); otherwise everyone in it is struck. */
    public static boolean palm(NaomiEntity n, Vec3 at) {
        List<LivingEntity> inside = victims(n, new AABB(at, at).inflate(NaomiBalance.PALM_RADIUS + 1, 3, NaomiBalance.PALM_RADIUS + 1))
                .stream().filter(e -> inCircle(e, at, NaomiBalance.PALM_RADIUS)).toList();
        for (LivingEntity e : inside) {
            if (e.isBlocking()) {
                n.stagger(e instanceof ServerPlayer p ? p : null, "message.supernaturalcraft.naomi.parry");
                return false;
            }
        }
        ServerLevel level = level(n);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.8, at.z, 30, NaomiBalance.PALM_RADIUS / 2, 0.4, NaomiBalance.PALM_RADIUS / 2, 0.1);
        sound(n, AllSounds.heaven("naomi.console"), 1.5f, 0.6f);
        for (LivingEntity e : inside) {
            hit(n, e, AllDamageTypes.SMITE, NaomiBalance.PALM_DAMAGE);
            shove(e, n.position(), NaomiBalance.PALM_SHOVE);
        }
        return true;
    }

    // --- 2 · Restraint Field --------------------------------------------------------------------------------------------

    /** A gold ring round the hunter; whoever is inside when it closes is held. */
    public static class Restraint extends BossAttack<LuciferEntity> {
        private Vec3 centre = Vec3.ZERO;

        public Restraint() {
            super("restraint", NaomiAnimations.RESTRAINT, NaomiBalance.RESTRAINT_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            centre = floorAt(boss, target.position());
            TelegraphMarker.ring(level(boss), centre, NaomiBalance.RESTRAINT_RADIUS, TelegraphMarker.GOLD, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            restrain(naomi(boss), centre);
        }
    }

    /** The field closes at {@code centre}: everyone inside is held. @return how many */
    public static int restrain(NaomiEntity n, Vec3 centre) {
        float r = NaomiBalance.RESTRAINT_RADIUS;
        ServerLevel level = level(n);
        int held = 0;
        for (LivingEntity e : victims(n, new AABB(centre, centre).inflate(r + 1, 3, r + 1))) {
            if (!inCircle(e, centre, r)) continue;
            hit(n, e, AllDamageTypes.SPELL, NaomiBalance.RESTRAINT_DAMAGE);
            e.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, NaomiBalance.RESTRAINT_HOLD, 0), n);
            level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 16, 0.4, 0.8, 0.4, 0.01);
            held++;
        }
        sound(n, AllSounds.heaven("naomi.chair_strap"), 1.5f, 0.8f);
        return held;
    }

    // --- 3 · Strap In ---------------------------------------------------------------------------------------------------

    /** A line from a free chair to her chosen hunter: still on it when it closes, they are strapped in. */
    public static class StrapIn extends BossAttack<LuciferEntity> {
        private @Nullable Player victim;
        private @Nullable ReprogrammingChairEntity chair;
        private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;

        public StrapIn() {
            // The clip is cued so its point lands as the line closes (tickWindup).
            super("strap_in", "-", NaomiBalance.STRAP_WINDUP, 2, 20);
        }

        private boolean cued;

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            int length = Math.max(1, Math.round(windup * boss.windupScale()));
            if (!cued && t >= length - NaomiBalance.STRAP_CUE) {
                cued = true;
                boss.triggerAnim("action", NaomiAnimations.STRAP_IN);
            }
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            NaomiEntity n = naomi(boss);
            Player p = n.strapCandidate();
            return p != null && n.freeChairNear(p) != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            NaomiEntity n = naomi(boss);
            victim = n.strapCandidate();
            chair = victim == null ? null : n.freeChairNear(victim);
            if (victim == null || chair == null) return;
            from = chair.position();
            to = victim.position();
            face(boss, chair.position());
            float yaw = yawTo(from, to);
            float length = (float) flatDistance(from, to) + 1.5f;
            TelegraphMarker.line(level(boss), floorAt(boss, from), yaw, NaomiBalance.STRAP_HALF_WIDTH * 2, length, TelegraphMarker.GOLD, windup + 4);
            n.announceStrap(victim);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (victim == null || chair == null || !victim.isAlive()) return;
            if (NaomiBalance.onLane(victim.getX(), victim.getZ(), from.x, from.z, to.x, to.z, NaomiBalance.STRAP_HALF_WIDTH)) {
                naomi(boss).strapInto(chair, victim);
            } else {
                sound(boss, AllSounds.heaven("naomi.chair_free"), 1f, 1.3f);
            }
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return victim == null || chair == null;
        }
    }

    // --- 4 · Drill Lance ------------------------------------------------------------------------------------------------

    /** A red cone and a lunge with the drill; in phase 2, three in a row, each shown before it lands. */
    public static class DrillLance extends BossAttack<LuciferEntity> {
        private final int lunges;
        private float yaw;

        public DrillLance(int phase) {
            super("drill_lance", NaomiAnimations.DRILL_LANCE, NaomiBalance.LANCE_WINDUP, NaomiBalance.lanceActive(phase), 18);
            this.lunges = NaomiBalance.lunges(phase);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 10 * 10 ? 1 : 0.3f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            naomi(boss).setDrill(true);
            aim(boss, target, windup + 4);
        }

        private void aim(LuciferEntity boss, LivingEntity target, int ticks) {
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, NaomiBalance.LANCE_REACH, TelegraphMarker.RED, ticks);
            sound(boss, AllSounds.heaven("naomi.drill"), 0.8f, 1.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            lunge(naomi(boss), yaw);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            for (int k = 1; k < lunges; k++) {
                if (t == k * NaomiBalance.LANCE_GAP - NaomiBalance.LANCE_WARN) {
                    boss.triggerAnim("action", NaomiAnimations.DRILL_LANCE);
                    aim(boss, target, NaomiBalance.LANCE_WARN + 2);
                } else if (t == k * NaomiBalance.LANCE_GAP) {
                    lunge(naomi(boss), yaw);
                }
            }
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            naomi(boss).setDrill(false);
        }
    }

    /** One lunge along {@code yaw}: everyone in the cone takes the drill, then she steps through. @return how many */
    public static int lunge(NaomiEntity n, float yaw) {
        Vec3 from = n.position(), dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(NaomiBalance.LANCE_HALF_ANGLE));
        float reach = NaomiBalance.LANCE_REACH;
        int struck = 0;
        for (LivingEntity e : victims(n, new AABB(from, from).inflate(reach + 1, 3, reach + 1))) {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach + 0.4 || Math.abs(e.getY() - from.y) > 3 || (d > 0.8 && to.normalize().dot(dir) < cos)) continue;
            drillHit(n, e, NaomiBalance.LANCE_DAMAGE);
            struck++;
        }
        n.setDeltaMovement(dir.x * NaomiBalance.LANCE_STEP, n.getDeltaMovement().y, dir.z * NaomiBalance.LANCE_STEP);
        n.hurtMarked = true;
        Vec3 tip = from.add(dir.scale(reach * 0.6)).add(0, 1.1, 0);
        level(n).sendParticles(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 18, 0.4, 0.3, 0.4, 0.2);
        sound(n, AllSounds.heaven("naomi.drill"), 2f, 1.0f);
        return struck;
    }

    // --- 5 · Memory Wipe ------------------------------------------------------------------------------------------------

    /** A white ring sweeps out from her; the green lanes through it are the only shelter. */
    public static class MemoryWipe extends BossAttack<LuciferEntity> {
        private Vec3 centre = Vec3.ZERO;
        private List<Float> gaps = List.of();
        private final Set<UUID> struck = new HashSet<>();

        public MemoryWipe() {
            super("memory_wipe", NaomiAnimations.WIPE, NaomiBalance.WIPE_WINDUP, NaomiBalance.WIPE_TICKS, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            centre = boss.position();
            gaps = NaomiBalance.wipeGaps(NaomiBalance.WIPE_GAPS, boss.getRandom().nextFloat() * 360f);
            ServerLevel level = level(boss);
            int life = windup + NaomiBalance.WIPE_TICKS;
            TelegraphMarker.ring(level, centre, NaomiBalance.WIPE_RADIUS, TelegraphMarker.WHITE, life);
            for (float yaw : gaps) {
                TelegraphMarker.line(level, centre, yaw, NaomiBalance.WIPE_GAP_HALF_WIDTH * 2, NaomiBalance.WIPE_RADIUS, TelegraphMarker.SAFE, life);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.heaven("naomi.wipe"), 3f, 1.0f);
            sweep(boss, 0);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            sweep(boss, t);
        }

        private void sweep(LuciferEntity boss, int t) {
            double r = NaomiBalance.wipeRadius(t);
            ServerLevel level = level(boss);
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                level.sendParticles(ParticleTypes.END_ROD, centre.x + Math.cos(a) * r, centre.y + 0.6, centre.z + Math.sin(a) * r, 1, 0, 0.2, 0, 0);
            }
            for (LivingEntity e : victims(boss, new AABB(centre, centre).inflate(r + 1, 3, r + 1))) {
                if (struck.contains(e.getUUID())) continue;
                double dx = e.getX() - centre.x, dz = e.getZ() - centre.z;
                if (Math.sqrt(dx * dx + dz * dz) > r || Math.abs(e.getY() - centre.y) > 3.5) continue;
                if (NaomiBalance.inGap(dx, dz, gaps, NaomiBalance.WIPE_GAP_HALF_WIDTH)) continue;
                struck.add(e.getUUID());
                wipe(naomi(boss), e);
            }
        }
    }

    /** Caught by the wipe: a blow, slowness, and (a player) the screen goes white. */
    public static void wipe(NaomiEntity n, LivingEntity e) {
        hit(n, e, AllDamageTypes.SPELL, NaomiBalance.WIPE_DAMAGE);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NaomiBalance.WIPE_SLOW_TICKS, 1), n);
        if (e instanceof ServerPlayer p) {
            n.fxTo(p, new HeavenFxPayload(n.getId(), HeavenFxPayload.WHITEOUT, 0, 0, p.position(), NaomiBalance.WHITEOUT_TICKS, ""));
        }
    }

    // --- 6 · Call the Guards --------------------------------------------------------------------------------------------

    /** Guards step out of the alcoves (their spots shown first); none while enough of them stand. */
    public static class CallGuards extends BossAttack<LuciferEntity> {
        private List<Vec3> spots = List.of();

        public CallGuards() {
            super("call_guards", NaomiAnimations.CALL_GUARDS, NaomiBalance.GUARDS_WINDUP, 2, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return naomi(boss).guardsStanding() >= NaomiBalance.WARD_GUARDS ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            NaomiEntity n = naomi(boss);
            spots = n.guardSpots(NaomiBalance.guards(n.phase()));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.0f, TelegraphMarker.GOLD, windup + 4);
            sound(boss, AllSounds.heaven("naomi.guards"), 2f, 1.0f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            naomi(boss).callGuards(spots);
        }
    }

    // --- 7 · Training Test ----------------------------------------------------------------------------------------------

    /** Copies rise on the test floor (their spots shown in white first). */
    public static class Test extends BossAttack<LuciferEntity> {
        public Test() {
            super("training_test", NaomiAnimations.TEST, NaomiBalance.TEST_WINDUP, 2, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return naomi(boss).testRunning() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            for (Vec3 s : naomi(boss).copySpots()) TelegraphMarker.circle(level(boss), s, 0.8f, TelegraphMarker.WHITE, windup + 4);
            sound(boss, AllSounds.heaven("naomi.test_bell"), 1.5f, 1.2f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            naomi(boss).beginTest(target);
        }
    }

    // --- 8 · Recalibration ----------------------------------------------------------------------------------------------

    /** She goes to her console and heals there until six blows land on it (or the most it gives). */
    public static class Recalibration extends BossAttack<LuciferEntity> {
        public Recalibration() {
            // Her typing is the base controller's loop while the RECALIBRATING flag is up.
            super("recalibration", "-", NaomiBalance.RECAL_WINDUP, NaomiBalance.RECAL_TICKS, 20);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return naomi(boss).worthRecalibrating() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            NaomiEntity n = naomi(boss);
            Vec3 stand = n.consoleStand();
            TelegraphMarker.circle(level(boss), stand, 1.2f, TelegraphMarker.GOLD, windup + 4);
            n.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.4);
            sound(boss, AllSounds.heaven("naomi.console"), 1.5f, 1.0f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            naomi(boss).beginRecalibration();
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            NaomiEntity n = naomi(boss);
            n.getNavigation().stop();
            n.setDeltaMovement(0, Math.min(0, n.getDeltaMovement().y), 0);
            if (t % 20 == 0) n.recalibrationSecond();
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return naomi(boss).recalibrationOver();
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            naomi(boss).endRecalibration();
        }
    }

    // --- the gap closer -------------------------------------------------------------------------------------------------

    /** A flicker of white and she stands behind the hunter. */
    public static class Approach extends BossAttack<LuciferEntity> {
        private Vec3 to = Vec3.ZERO;

        public Approach() {
            super("approach", "-", 10, 2, 8);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            Vec3 back = target.getLookAngle().multiply(1, 0, 1);
            if (back.lengthSqr() < 0.01) back = new Vec3(0, 0, 1);
            to = floorAt(boss, target.position().subtract(back.normalize().scale(2.5)));
            TelegraphMarker.circle(level(boss), to, 0.9f, TelegraphMarker.WHITE, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            naomi(boss).blinkTo(to);
            face(boss, target.position());
        }
    }

    /** Her line as she straps an angel in. */
    static Component oneOfOurs() {
        return Component.translatable("message.supernaturalcraft.naomi.one_of_ours").withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
    }
}
