package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.arena.ZachariahOfficeLayout;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.flatDistance;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Zachariah's attacks (v0.18): ten, by phase, each shown on the floor through its windup and landing only in ACTIVE. Gold is
 * Heaven's (the stamp, the smite, the notice), violet his telekinesis (the paper, the shuffle), green where it is safe (the
 * approved desks, the smite's ring).
 * <ul>
 *   <li>I, Intake: Paper Storm, Rubber Stamp, Clerks, Precedent, Reassignment, Termination Notice.</li>
 *   <li>II, Review: and the Cubicle Shuffle.</li>
 *   <li>III, It Was Already Written: and the Wing Buffet; what he does next is on the docket.</li>
 *   <li>IV, Final Judgment: and the Smite of Heaven.</li>
 * </ul>
 * The Overdue Smite is never his choice: an overdue form calls it, and it cuts in before the docket. A foretold attack
 * ({@code lead} &gt; 0) shows its telegraph that much earlier; its clip still plays at the usual moment.
 */
public final class ZachariahAttacks {

    private ZachariahAttacks() {
    }

    public static final String PAPER_STORM = "paper_storm", RUBBER_STAMP = "rubber_stamp", CLERKS = "clerks", PRECEDENT = "precedent",
            CUBICLE_SHUFFLE = "cubicle_shuffle", TERMINATION = "termination_notice", REASSIGNMENT = "reassignment", WING_BUFFET = "wing_buffet",
            SMITE_OF_HEAVEN = "smite_of_heaven", OVERDUE_SMITE = "overdue_smite", BLINK = "blink";

    /** Every attack he chooses, by id, made with a lead (ticks of early warning). The Overdue Smite and the Blink are forced only. */
    private static final Map<String, IntFunction<BossAttack<LuciferEntity>>> MAKERS = new LinkedHashMap<>();

    static {
        MAKERS.put(PAPER_STORM, PaperStorm::new);
        MAKERS.put(RUBBER_STAMP, RubberStamp::new);
        MAKERS.put(CLERKS, Clerks::new);
        MAKERS.put(PRECEDENT, Precedent::new);
        MAKERS.put(CUBICLE_SHUFFLE, CubicleShuffle::new);
        MAKERS.put(TERMINATION, TerminationNotice::new);
        MAKERS.put(REASSIGNMENT, Reassignment::new);
        MAKERS.put(WING_BUFFET, WingBuffet::new);
        MAKERS.put(SMITE_OF_HEAVEN, SmiteOfHeaven::new);
    }

    /** The ten (the docket's nine and the forced Overdue Smite). */
    public static final List<String> ALL = List.of(PAPER_STORM, RUBBER_STAMP, CLERKS, PRECEDENT, CUBICLE_SHUFFLE, TERMINATION, REASSIGNMENT,
            WING_BUFFET, SMITE_OF_HEAVEN, OVERDUE_SMITE);

    /** Base weights by phase (an id missing from a phase's map is not in its pool). */
    public static Map<String, Float> weights(int phase) {
        Map<String, Float> w = new LinkedHashMap<>();
        w.put(PAPER_STORM, 3f);
        w.put(RUBBER_STAMP, 3f);
        w.put(CLERKS, 1.5f);
        w.put(PRECEDENT, 2f);
        w.put(REASSIGNMENT, 1.5f);
        w.put(TERMINATION, phase >= 2 ? 1.5f : 1f);
        if (phase >= 2) w.put(CUBICLE_SHUFFLE, 1.5f);
        if (phase >= 3) w.put(WING_BUFFET, 2.5f);
        if (phase >= 4) w.put(SMITE_OF_HEAVEN, 2.5f);
        return w;
    }

    /** A new attack of this id ({@code lead} ticks of early warning), or null for an unknown id. */
    public static @Nullable BossAttack<LuciferEntity> create(String id, int lead) {
        IntFunction<BossAttack<LuciferEntity>> maker = MAKERS.get(id);
        return maker == null ? null : maker.apply(Math.max(0, lead));
    }

    public static boolean known(String id) {
        return MAKERS.containsKey(id);
    }

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        List<AttackScheduler.Option<LuciferEntity>> out = new ArrayList<>();
        for (Map.Entry<String, Float> e : weights(phase).entrySet()) {
            String id = e.getKey();
            Supplier<BossAttack<LuciferEntity>> f = () -> create(id, 0);
            out.add(new AttackScheduler.Option<>(f, e.getValue()));
        }
        return out;
    }

    /** A weighted pick for the docket: never {@code previous}, and only what can be used against {@code target} now. */
    public static String pick(ZachariahEntity boss, @Nullable LivingEntity target, @Nullable String previous) {
        List<String> ids = new ArrayList<>();
        List<Float> ws = new ArrayList<>();
        float total = 0;
        for (Map.Entry<String, Float> e : weights(boss.phase()).entrySet()) {
            if (e.getKey().equals(previous)) continue;
            BossAttack<LuciferEntity> a = create(e.getKey(), 0);
            float w = e.getValue() * (target != null && a != null ? a.weight(boss, target) : 1f);
            if (w <= 0) continue;
            ids.add(e.getKey());
            ws.add(w);
            total += w;
        }
        if (ids.isEmpty()) return PAPER_STORM.equals(previous) ? RUBBER_STAMP : PAPER_STORM;
        float roll = boss.getRandom().nextFloat() * total;
        for (int i = 0; i < ids.size(); i++) {
            roll -= ws.get(i);
            if (roll <= 0) return ids.get(i);
        }
        return ids.getLast();
    }

    public static ZachariahEntity zach(LuciferEntity boss) {
        return (ZachariahEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether his office should land on {@code e}: not himself, not his clerks, nobody in creative. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof ZachariahEntity) && !(e instanceof ClerkAngelEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    /** Everyone in {@code box} his blows may land on (the hunters he tracks too: fake players are not in the level's lists). */
    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : zach(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    /** The hunters, the target first if it is not one of them. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(zach(boss).hunters());
        if (!out.contains(target) && (isFoe(boss, target) || out.isEmpty())) out.add(target);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    static void play(LuciferEntity boss, String event, float volume, float pitch) {
        boss.level().playSound(null, boss.blockPosition(), AllSounds.heaven(event), net.minecraft.sounds.SoundSource.HOSTILE, volume, pitch);
    }

    /** DENIED: no natural healing for a while. */
    public static void deny(LivingEntity e) {
        e.addEffect(new MobEffectInstance(AllMobEffects.PAPERWORK, ZachariahBalance.DENIED_TICKS, 0, false, true, true));
    }

    /**
     * An attack of his: its clip plays its hit tick ({@code HeavenAssets.ZACHARIAH_HIT_TICKS}) before the end of the windup, so
     * the moment of the clip is the moment it lands (a foretold one waits out its lead
     * first); the scheduler's own trigger is skipped ({@code animation} is no clip).
     */
    public abstract static class Paper extends BossAttack<LuciferEntity> {
        protected final String clip;
        protected final int lead, clipAt;
        private boolean cued;

        protected Paper(String id, String clip, int windup, int active, int recover, int lead) {
            super(id, "-", windup + lead, active, recover);
            this.clip = clip;
            this.lead = lead;
            // The clip's own moment (the art's hit tick) lands as the windup ends.
            this.clipAt = Math.min(windup, org.papiricoh.supernaturalcraft.heaven.HeavenAssets.ZACHARIAH_HIT_TICKS.getOrDefault(clip, windup));
        }

        /** Where to show it, and whatever it decides at the start (the windup includes the lead). */
        protected abstract void begin(ZachariahEntity boss, LivingEntity target);

        @Override
        public final void onWindup(LuciferEntity boss, LivingEntity target) {
            begin(zach(boss), target);
            if (windupLength(boss) <= clipAt) cue(boss);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (!cued && t >= windupLength(boss) - clipAt) cue(boss);
        }

        private void cue(LuciferEntity boss) {
            cued = true;
            if (!clip.isEmpty()) boss.triggerAnim("action", clip);
        }

        protected int windupLength(LuciferEntity boss) {
            return Math.max(1, Math.round(windup * boss.windupScale()));
        }

        /** How long a telegraph shown now must last. */
        protected int show() {
            return windup + 4;
        }
    }

    // --- I · Intake ------------------------------------------------------------------------------------------------------

    /** A cone of memos thrown at the target (violet: his telekinesis), loosed through the active stage. */
    public static class PaperStorm extends Paper {
        private float yaw;

        public PaperStorm(int lead) {
            super(PAPER_STORM, ZachariahAnimations.PAPER_STORM, 20, 20, 16, lead);
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, ZachariahBalance.STORM_REACH, TelegraphMarker.VIOLET, show());
            play(boss, "zachariah.paper_storm", 1.5f, 1.0f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 2 != 0) return;
            int per = (int) Math.ceil(ZachariahBalance.STORM_MEMOS / Math.max(1.0, active / 2.0));
            for (int i = 0; i < per; i++) {
                double off = (boss.getRandom().nextDouble() * 2 - 1) * ZachariahBalance.STORM_HALF_ANGLE;
                Vec3 dir = dirOf(yaw + (float) off).add(0, (boss.getRandom().nextDouble() - 0.5) * 0.1, 0).normalize();
                MemoProjectile.throwFrom(zach(boss), boss.position().add(0, 1.4, 0).add(dir.scale(0.6)), dir);
            }
        }
    }

    /** A rubber stamp the size of a room comes down on each hunter: a 3×3 square in gold, and whoever stays under it is DENIED. */
    public static class RubberStamp extends Paper {
        private final List<Vec3> spots = new ArrayList<>();

        public RubberStamp(int lead) {
            super(RUBBER_STAMP, ZachariahAnimations.STAMP, 22, 2, 18, lead);
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) if (spots.size() < ZachariahBalance.STAMP_TARGETS) spots.add(boss.onFloor(e.position()));
            for (Vec3 s : spots) square(level(boss), s, ZachariahBalance.STAMP_HALF, TelegraphMarker.GOLD, show());
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            stamp(zach(boss), spots);
        }
    }

    /** A square of half-side {@code half} on the floor round {@code centre}. */
    static void square(ServerLevel level, Vec3 centre, float half, int color, int lifetime) {
        TelegraphMarker.line(level, centre.add(0, 0, -half), 0f, half * 2, half * 2, color, lifetime);
    }

    /** The stamps land on {@code spots}. @return who was stamped */
    public static List<LivingEntity> stamp(ZachariahEntity boss, List<Vec3> spots) {
        ServerLevel level = level(boss);
        List<LivingEntity> struck = new ArrayList<>();
        for (Vec3 s : spots) {
            level.sendParticles(ParticleTypes.CLOUD, s.x, s.y + 0.2, s.z, 20, 1.2, 0.1, 1.2, 0.05);
            level.sendParticles(ParticleTypes.CRIT, s.x, s.y + 0.4, s.z, 12, 1.2, 0.2, 1.2, 0.1);
            for (LivingEntity e : foes(boss, new AABB(s, s).inflate(ZachariahBalance.STAMP_HALF + 0.5, 3, ZachariahBalance.STAMP_HALF + 0.5))) {
                if (struck.contains(e) || Math.abs(e.getY() - s.y) > 2.5 || !ZachariahBalance.underStamp(e.getX() - s.x, e.getZ() - s.z)) continue;
                hit(boss, e, AllDamageTypes.SMITE, ZachariahBalance.STAMP_DAMAGE);
                deny(e);
                struck.add(e);
                if (e instanceof ServerPlayer p) boss.say(p, "denied");
            }
        }
        play(boss, "zachariah.stamp", 2.5f, 0.9f);
        if (!struck.isEmpty()) play(boss, "zachariah.denied", 1.5f, 1.0f);
        return struck;
    }

    /** Angels of his office come out of the cubicles (their doors shown in gold): stamps for whoever puts them down. */
    public static class Clerks extends Paper {
        public Clerks(int lead) {
            super(CLERKS, ZachariahAnimations.SUMMON_CLERKS, 30, 4, 16, lead);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return zach(boss).clerks().size() < ZachariahBalance.CLERK_CAP ? 1 : 0;
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            for (LayoutPoint p : ZachariahOfficeLayout.CLERK_SPAWNS) {
                TelegraphMarker.circle(level(boss), boss.at(p), 1.2f, TelegraphMarker.GOLD, show());
            }
            play(boss, "zachariah.docket", 1.5f, 1.2f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            zach(boss).callClerks(level(boss));
        }
    }

    /**
     * Precedent: what you did before, you will do again. Gold circles where each hunter stood {@link ZachariahBalance#PRECEDENT_DELAY}
     * ticks ago and where they stand now; both are struck.
     */
    public static class Precedent extends Paper {
        private final List<Vec3> spots = new ArrayList<>();

        public Precedent(int lead) {
            super(PRECEDENT, ZachariahAnimations.PRECEDENT, 30, 2, 16, lead);
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            face(boss, target.position());
            spots.addAll(spots(boss, marks(boss, target), boss.level().getGameTime()));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, ZachariahBalance.PRECEDENT_RADIUS, TelegraphMarker.GOLD, show());
            play(boss, "zachariah.docket", 1.5f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            strike(zach(boss), spots);
        }
    }

    /** Where Precedent strikes: each hunter's spot of {@link ZachariahBalance#PRECEDENT_DELAY} ticks ago (if known) and now. */
    public static List<Vec3> spots(ZachariahEntity boss, List<LivingEntity> hunters, long now) {
        List<Vec3> out = new ArrayList<>();
        for (LivingEntity h : hunters) {
            Vec3 then = boss.whereWas(h, now, ZachariahBalance.PRECEDENT_DELAY);
            if (then != null && flatDistance(then, h.position()) > 1.0) out.add(boss.onFloor(then));
            out.add(boss.onFloor(h.position()));
        }
        return out;
    }

    /** Precedent lands. @return who it struck */
    public static List<LivingEntity> strike(ZachariahEntity boss, List<Vec3> spots) {
        ServerLevel level = level(boss);
        List<LivingEntity> struck = new ArrayList<>();
        float r = ZachariahBalance.PRECEDENT_RADIUS;
        for (Vec3 s : spots) {
            level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 0.5, s.z, 16, 0.6, 0.6, 0.6, 0.05);
            for (LivingEntity e : foes(boss, new AABB(s, s).inflate(r + 1, 3, r + 1))) {
                if (struck.contains(e) || flatDistance(e.position(), s) > r || Math.abs(e.getY() - s.y) > 2.5) continue;
                hit(boss, e, AllDamageTypes.SMITE, ZachariahBalance.PRECEDENT_DAMAGE);
                struck.add(e);
            }
        }
        play(boss, "zachariah.stamp", 1.5f, 1.4f);
        return struck;
    }

    /** Reassignment: a hunter marked in gold is moved one tile of the office, with a slap of paperwork. */
    public static class Reassignment extends Paper {
        private @Nullable LivingEntity marked;
        private Vec3 at = Vec3.ZERO;

        public Reassignment(int lead) {
            super(REASSIGNMENT, ZachariahAnimations.REASSIGN, 24, 2, 14, lead);
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            List<LivingEntity> hunters = marks(boss, target);
            marked = hunters.get(boss.getRandom().nextInt(hunters.size()));
            at = boss.onFloor(marked.position());
            face(boss, at);
            TelegraphMarker.circle(level(boss), at, ZachariahBalance.REASSIGN_RADIUS, TelegraphMarker.GOLD, show());
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (marked != null && marked.isAlive() && flatDistance(marked.position(), at) <= ZachariahBalance.REASSIGN_RADIUS) {
                reassign(zach(boss), marked);
            }
        }
    }

    /** Moves {@code e} one tile of the office along a random axis (folded back into the window). @return the offset */
    public static Vec3 reassign(ZachariahEntity boss, LivingEntity e) {
        int tile = ZachariahOfficeLayout.TILE;
        int k = boss.getRandom().nextInt(4);
        Vec3 off = new Vec3(k == 0 ? tile : k == 1 ? -tile : 0, 0, k == 2 ? tile : k == 3 ? -tile : 0);
        Vec3 c = boss.officeCentre();
        double nx = c.x + ZachariahBalance.fold(e.getX() + off.x - c.x), nz = c.z + ZachariahBalance.fold(e.getZ() + off.z - c.z);
        Vec3 moved = new Vec3(nx - e.getX(), 0, nz - e.getZ());
        ServerLevel level = level(boss);
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.8, 0.3, 0.05);
        OfficeWrap.move(e, moved, false);
        hit(boss, e, AllDamageTypes.SPELL, ZachariahBalance.REASSIGN_DAMAGE);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        level.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.8, 0.3, 0.05);
        play(boss, "zachariah.file", 1.5f, 1.3f);
        if (e instanceof ServerPlayer p) boss.say(p, "reassigned");
        return moved;
    }

    /**
     * A Termination Notice served on one hunter: due in {@link ZachariahBalance#TERMINATION_TICKS}. Standing at an Approved desk
     * (green) voids it; otherwise it takes {@link ZachariahBalance#TERMINATION_SHARE} of their health, split with whoever stands
     * with them. He keeps throwing paper while it runs.
     */
    public static class TerminationNotice extends Paper {
        private @Nullable LivingEntity marked;

        public TerminationNotice(int lead) {
            super(TERMINATION, ZachariahAnimations.TERMINATION, ZachariahBalance.TERMINATION_TICKS, 2, 16, lead);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return zach(boss).noticeServed() ? 0 : 1;
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            List<LivingEntity> hunters = marks(boss, target);
            marked = hunters.get(boss.getRandom().nextInt(hunters.size()));
            face(boss, marked.position());
            for (LayoutPoint p : ZachariahOfficeLayout.DESK_SAFE) {
                TelegraphMarker.circle(level(boss), boss.at(p), ZachariahBalance.DESK_RADIUS, TelegraphMarker.SAFE, show());
            }
            boss.serveNotice(marked, show() - 4);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            if (marked == null || !marked.isAlive()) return;
            if (t % 20 == 1) {
                TelegraphMarker.ring(level(boss), zach(boss).onFloor(marked.position()), ZachariahBalance.SHARE_RADIUS, TelegraphMarker.GOLD, 22);
            }
            if (t % 30 == 15) {
                Vec3 dir = marked.position().subtract(boss.position()).multiply(1, 0, 1);
                if (dir.lengthSqr() > 0.01) MemoProjectile.throwFrom(zach(boss), boss.position().add(0, 1.4, 0), dir.normalize());
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (marked != null) terminate(zach(boss), marked);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            zach(boss).serveNotice(null, 0);
        }
    }

    /**
     * The notice comes due on {@code marked}: nothing at an Approved desk; otherwise the share of their health, split between them
     * and everyone standing with them. @return who paid it (empty if voided)
     */
    public static List<LivingEntity> terminate(ZachariahEntity boss, LivingEntity marked) {
        ServerLevel level = level(boss);
        play(boss, "zachariah.termination", 2.5f, 1.0f);
        if (boss.atApprovedDesk(marked.position())) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, marked.getX(), marked.getY() + 1, marked.getZ(), 16, 0.4, 0.6, 0.4, 0.05);
            if (marked instanceof ServerPlayer p) boss.say(p, "notice_void");
            return List.of();
        }
        List<LivingEntity> sharers = new ArrayList<>();
        sharers.add(marked);
        float r = ZachariahBalance.SHARE_RADIUS;
        for (LivingEntity e : foes(boss, marked.getBoundingBox().inflate(r + 1, 2, r + 1))) {
            if (e != marked && e.distanceTo(marked) <= r && (e instanceof ServerPlayer || boss.hunters().contains(e))) sharers.add(e);
        }
        float each = ZachariahBalance.terminationEach(marked.getMaxHealth(), sharers.size());
        for (LivingEntity e : sharers) {
            BossStrike.deal(boss, e, AllDamageTypes.DIVINE_WRATH, each);
            level.sendParticles(ParticleTypes.FLASH, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
            if (e instanceof ServerPlayer p) boss.say(p, sharers.size() > 1 ? "notice_shared" : "notice_served");
        }
        return sharers;
    }

    // --- II · Review -----------------------------------------------------------------------------------------------------

    /** The partitions shuffle: cubicle walls rise and fall round the office for a while (his telekinesis, shown in violet). */
    public static class CubicleShuffle extends Paper {
        public CubicleShuffle(int lead) {
            super(CUBICLE_SHUFFLE, ZachariahAnimations.SHUFFLE, 20, 4, 14, lead);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return zach(boss).officeReady() ? 1 : 0;
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), boss.position(), 6f, TelegraphMarker.VIOLET, show());
            play(boss, "zachariah.file", 2f, 0.7f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            zach(boss).shuffleCubicles();
        }
    }

    // --- III · It Was Already Written ------------------------------------------------------------------------------------

    /** His wings beat once: a cone before him, and whoever is in it is thrown. */
    public static class WingBuffet extends Paper {
        private float yaw;

        public WingBuffet(int lead) {
            super(WING_BUFFET, ZachariahAnimations.WING_BUFFET, 18, 2, 16, lead);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 12 * 12 ? 1 : 0.2f;
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, ZachariahBalance.BUFFET_REACH, TelegraphMarker.GOLD, show());
            play(boss, "zachariah.wings", 1.5f, 1.2f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            buffet(zach(boss), boss.position(), yaw);
        }
    }

    /** The buffet lands. @return who it threw */
    public static List<LivingEntity> buffet(ZachariahEntity boss, Vec3 from, float yaw) {
        Vec3 dir = dirOf(yaw);
        double reach = ZachariahBalance.BUFFET_REACH, cos = Math.cos(Math.toRadians(ZachariahBalance.BUFFET_HALF_ANGLE));
        List<LivingEntity> thrown = new ArrayList<>();
        for (LivingEntity e : foes(boss, new AABB(from, from).inflate(reach, 4, reach))) {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            if (d > reach || Math.abs(e.getY() - from.y) > 3.5 || (d > 0.8 && to.normalize().dot(dir) < cos)) continue;
            hit(boss, e, AllDamageTypes.SMITE, ZachariahBalance.BUFFET_DAMAGE);
            Vec3 away = d < 0.01 ? dir : to.normalize();
            e.push(away.x * ZachariahBalance.BUFFET_SHOVE, 0.5, away.z * ZachariahBalance.BUFFET_SHOVE);
            e.hurtMarked = true;
            thrown.add(e);
        }
        Vec3 c = from.add(dir.scale(reach / 2)).add(0, 1.2, 0);
        level(boss).sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 40, reach / 3, 0.6, reach / 3, 0.15);
        play(boss, "zachariah.wings", 3f, 0.8f);
        return thrown;
    }

    // --- IV · Final Judgment ---------------------------------------------------------------------------------------------

    /** Golden lines run out from him across the open sky; only the green ring halfway out is spared. */
    public static class SmiteOfHeaven extends Paper {
        private float offset;
        private Vec3 centre = Vec3.ZERO;

        public SmiteOfHeaven(int lead) {
            super(SMITE_OF_HEAVEN, ZachariahAnimations.SMITE, 40, 4, 22, lead);
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            centre = boss.position();
            offset = boss.getRandom().nextFloat() * 360f / ZachariahBalance.SMITE_LINES;
            ServerLevel level = level(boss);
            for (int i = 0; i < ZachariahBalance.SMITE_LINES; i++) {
                float yaw = offset + i * 360f / ZachariahBalance.SMITE_LINES;
                TelegraphMarker.line(level, centre, yaw, ZachariahBalance.SMITE_HALF_WIDTH * 2, ZachariahBalance.SMITE_LENGTH, TelegraphMarker.GOLD, show());
            }
            TelegraphMarker.ring(level, centre, ZachariahBalance.SMITE_SAFE_INNER, TelegraphMarker.SAFE, show());
            TelegraphMarker.ring(level, centre, ZachariahBalance.SMITE_SAFE_OUTER, TelegraphMarker.SAFE, show());
            play(boss, "zachariah.approved", 2f, 0.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            smite(zach(boss), centre, offset);
        }
    }

    /** The smite lands: everyone on a line but off the ring. @return who it struck */
    public static List<LivingEntity> smite(ZachariahEntity boss, Vec3 centre, float offset) {
        ServerLevel level = level(boss);
        List<LivingEntity> struck = new ArrayList<>();
        double reach = ZachariahBalance.SMITE_LENGTH;
        List<LivingEntity> near = foes(boss, new AABB(centre, centre).inflate(reach + 1, 4, reach + 1));
        for (int i = 0; i < ZachariahBalance.SMITE_LINES; i++) {
            Vec3 dir = dirOf(offset + i * 360f / ZachariahBalance.SMITE_LINES);
            for (int k = 2; k <= reach; k += 2) {
                Vec3 p = centre.add(dir.scale(k));
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.3, p.z, 2, 0.2, 0.4, 0.2, 0.02);
            }
            for (LivingEntity e : near) {
                if (struck.contains(e) || Math.abs(e.getY() - centre.y) > 3.5) continue;
                Vec3 to = e.position().subtract(centre).multiply(1, 0, 1);
                double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
                if (!ZachariahBalance.smiteHits(along, across)) continue;
                hit(boss, e, AllDamageTypes.SMITE, ZachariahBalance.SMITE_DAMAGE);
                struck.add(e);
            }
        }
        level.sendParticles(ParticleTypes.FLASH, centre.x, centre.y + 1, centre.z, 2, 0, 0, 0, 0);
        play(boss, "zachariah.termination", 3f, 0.7f);
        return struck;
    }

    // --- forced ----------------------------------------------------------------------------------------------------------

    /** An overdue form: Heaven collects where its holder stands (gold, a moment to step out). */
    public static class OverdueSmite extends Paper {
        private final @Nullable LivingEntity debtor;
        private Vec3 at = Vec3.ZERO;

        public OverdueSmite(@Nullable LivingEntity debtor) {
            super(OVERDUE_SMITE, ZachariahAnimations.SMITE, 24, 2, 14, 0);
            this.debtor = debtor;
        }

        @Override
        protected void begin(ZachariahEntity boss, LivingEntity target) {
            LivingEntity who = debtor != null && debtor.isAlive() ? debtor : target;
            at = boss.onFloor(who.position());
            face(boss, at);
            TelegraphMarker.circle(level(boss), at, ZachariahBalance.OVERDUE_RADIUS, TelegraphMarker.GOLD, show());
            play(boss, "zachariah.denied", 2f, 0.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 40, 0.8, 1.5, 0.8, 0.1);
            float r = ZachariahBalance.OVERDUE_RADIUS;
            for (LivingEntity e : foes(boss, new AABB(at, at).inflate(r + 1, 3, r + 1))) {
                if (flatDistance(e.position(), at) <= r && Math.abs(e.getY() - at.y) < 2.5) hit(boss, e, AllDamageTypes.SMITE, ZachariahBalance.OVERDUE_DAMAGE);
            }
            play(boss, "zachariah.termination", 2f, 1.2f);
        }
    }

    /** He is simply elsewhere: a step behind his target (he is no part of the office's wrap). */
    public static class Blink extends BossAttack<LuciferEntity> {
        private Vec3 to = Vec3.ZERO;

        public Blink() {
            super(BLINK, "-", 12, 2, 10);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            Vec3 back = target.getLookAngle().multiply(1, 0, 1);
            if (back.lengthSqr() < 0.01) back = new Vec3(0, 0, 1);
            to = zach(boss).onFloor(target.position().subtract(back.normalize().scale(3)));
            TelegraphMarker.circle(level(boss), to, 0.9f, TelegraphMarker.GOLD, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            level.sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY() + 1, boss.getZ(), 20, 0.4, 0.8, 0.4, 0.05);
            boss.teleportTo(to.x, to.y, to.z);
            boss.getNavigation().stop();
            face(boss, target.position());
            play(boss, "zachariah.wings", 1.5f, 1.4f);
        }
    }

    /** Where {@code p} of his office is (tests and the attacks), for an origin {@code origin}. */
    public static Vec3 at(BlockPos origin, LayoutPoint p) {
        return Vec3.atBottomCenterOf(origin.offset(p.x(), p.y(), p.z()));
    }
}
