package org.papiricoh.supernaturalcraft.entity.boss.abaddon;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.AbaddonFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
 * Abaddon's attacks (v0.21). Every one is shown on the ground in its windup and lands only in ACTIVE: red for hellfire (the
 * herald's mark that follows its target, the sweep's cone, the Brand's rings), yellow for the smoke she becomes, violet for
 * telekinesis (the pin), green for the Brand's gaps.
 * <ul>
 *   <li>P1, the Knight of Hell: {@link HeraldMark} (a red circle follows a hunter, holds, and the strike lands in it; her knights
 *   converge on it), {@link HellfireSweep} (red cone), {@link SummonRetinue} (knights and a herald rise).</li>
 *   <li>P2, Possession: {@link SmokeOut} (a yellow lane to a knight; she rides it: see {@link AbaddonEntity}), and the P1 blows.</li>
 *   <li>P3, the Brand of the Knights: {@link TelekineticPin} (violet circles: whoever is still in one is held), {@link Brand}
 *   (nested red rings with green gaps), and the rest.</li>
 * </ul>
 */
public final class AbaddonAttacks {

    private AbaddonAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(HeraldMark::new, 3), opt(HellfireSweep::new, 3), opt(SummonRetinue::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(SmokeOut::new, 4), opt(HellfireSweep::new, 2.5f), opt(HeraldMark::new, 2), opt(SummonRetinue::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Brand::new, 3), opt(TelekineticPin::new, 3), opt(HellfireSweep::new, 2), opt(HeraldMark::new, 1.5f),
            opt(SmokeOut::new, 1), opt(SummonRetinue::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static AbaddonEntity abaddon(LuciferEntity boss) {
        return (AbaddonEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether her blows should land on {@code e}: not herself or her retinue, nobody in creative or spectating. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof AbaddonEntity)
                && !(e instanceof AbaddonKnightEntity) && !(e instanceof AbaddonHeraldEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    /** Everyone her blows may find in {@code box}, her tracked hunters included. */
    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : abaddon(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(abaddon(boss).hunters());
        if (!out.contains(target)) out.add(target);
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(boss)));
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    /** Everyone in a 120° cone {@code reach} deep from {@code from} facing {@code yaw}. */
    static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(60));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 3.5 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} along {@code dir}. */
    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    /** An attack whose clip is cued so its moment falls at the end of the windup ({@link AbaddonAssets#HIT_TICKS}). */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, AbaddonBalance.cue(clip), windup, active, recover);
        }
    }

    /**
     * Calls {@code knights} knights (up to {@link AbaddonBalance#MAX_KNIGHTS} about her) and, with {@code herald}, a herald if she
     * has none, out of hellfire round her. @return how many rose
     */
    public static int summonRetinue(AbaddonEntity boss, int knights, boolean herald) {
        if (!(boss.level() instanceof ServerLevel level)) return 0;
        int n = 0;
        int room = AbaddonBalance.MAX_KNIGHTS - boss.retinue(true, false).size();
        List<Mob> risen = new ArrayList<>();
        for (int i = 0; i < Math.min(knights, room); i++) {
            AbaddonKnightEntity k = AllEntities.ABADDON_KNIGHT.get().create(level);
            if (k != null) risen.add(k);
        }
        if (herald && boss.retinue(false, true).size() < AbaddonBalance.MAX_HERALDS) {
            AbaddonHeraldEntity h = AllEntities.ABADDON_HERALD.get().create(level);
            if (h != null) risen.add(h);
        }
        double base = boss.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < risen.size(); i++) {
            Mob m = risen.get(i);
            double a = base + i * Math.PI * 2 / risen.size();
            Vec3 s = floorAt(boss, boss.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
            m.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
            m.setPersistenceRequired();
            if (m instanceof AbaddonKnightEntity k) k.serve(boss.getUUID());
            if (m instanceof AbaddonHeraldEntity h) h.serve(boss.getUUID());
            LivingEntity t = boss.attackTarget();
            if (t != null) m.setTarget(t);
            level.addFreshEntity(m);
            boss.minions().add(m.getUUID());
            level.sendParticles(AllParticles.HELLFIRE.get(), s.x, s.y + 0.3, s.z, 30, 0.4, 0.6, 0.4, 0.05);
            boss.fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.SUMMON, m.getId(), m instanceof AbaddonHeraldEntity ? 1 : 0, s, 30));
            n++;
        }
        boss.summoned();
        return n;
    }

    // --- P1 · the Knight of Hell -----------------------------------------------------------------------------------------

    /**
     * Her herald marks a hunter: a red circle follows them ({@link AbaddonBalance#MARK_FOLLOW}), holds still
     * ({@link AbaddonBalance#MARK_HOLD}), and the strike lands in it; her knights run at the spot.
     */
    public static class HeraldMark extends Cued {
        private @Nullable TelegraphMarker marker;
        private Vec3 spot = Vec3.ZERO;
        private int victim = -1;

        public HeraldMark() {
            super("herald_mark", "herald_mark", AbaddonBalance.MARK_FOLLOW + AbaddonBalance.MARK_HOLD, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spot = floorAt(boss, target.position());
            victim = target.getId();
            marker = TelegraphMarker.circle(level(boss), spot, AbaddonBalance.MARK_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
            for (Entity h : abaddon(boss).retinue(false, true)) if (h instanceof AbaddonHeraldEntity herald) herald.ring(target);
            abaddon(boss).fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.HERALD_MARK, victim,
                    Math.round(AbaddonBalance.MARK_RADIUS * 10), spot, windupLength(boss)));
            sound(boss, AllSounds.abaddon("herald_mark"), 1.5f, 1f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            if (t > Math.round(AbaddonBalance.MARK_FOLLOW * boss.windupScale()) || marker == null || !target.isAlive()) return;
            spot = floorAt(boss, target.position());
            marker.setPos(spot.x, spot.y + 0.02, spot.z);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(AbaddonBalance.MARK_RADIUS + 1, 4, AbaddonBalance.MARK_RADIUS + 1))) {
                if (inCircle(e, spot, AbaddonBalance.MARK_RADIUS)) {
                    hit(boss, e, AllDamageTypes.HELLFIRE, AbaddonBalance.MARK_DAMAGE);
                    e.igniteForSeconds(2);
                }
            }
            for (Entity k : abaddon(boss).retinue(true, false)) {
                if (k instanceof AbaddonKnightEntity knight && !knight.possessed()) {
                    knight.setTarget(target);
                    knight.getNavigation().moveTo(spot.x, spot.y, spot.z, AbaddonBalance.MARK_CONVERGE_SPEED);
                }
            }
            level(boss).sendParticles(AllParticles.HELLFIRE.get(), spot.x, spot.y + 0.3, spot.z, 50, 1.0, 0.4, 1.0, 0.05);
            abaddon(boss).fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.MARK_STRIKE, Math.round(AbaddonBalance.MARK_RADIUS * 10), 1, spot, 16));
            sound(boss, AllSounds.abaddon("sweep"), 1.5f, 1.3f);
        }
    }

    /** A sweeping arm: a red cone of hellfire before her. */
    public static class HellfireSweep extends Cued {
        private float yaw;

        public HellfireSweep() {
            super("hellfire_sweep", "hellfire_sweep", AbaddonBalance.SWEEP_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < AbaddonBalance.SWEEP_REACH * AbaddonBalance.SWEEP_REACH ? 1 : 0.15f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, AbaddonBalance.SWEEP_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 from = boss.position();
            for (LivingEntity e : inCone(boss, from, yaw, AbaddonBalance.SWEEP_REACH)) {
                hit(boss, e, AllDamageTypes.HELLFIRE, AbaddonBalance.SWEEP_DAMAGE);
                e.igniteForSeconds(AbaddonBalance.SWEEP_FIRE_SECONDS);
            }
            abaddon(boss).fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.SWEEP, Math.round(yaw), Math.round(AbaddonBalance.SWEEP_REACH), from, 20));
            sound(boss, AllSounds.abaddon("sweep"), 2f, 0.9f);
        }
    }

    /** Knights (and a herald) rise out of red circles round her. */
    public static class SummonRetinue extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public SummonRetinue() {
            super("summon_retinue", "herald_mark", AbaddonBalance.SUMMON_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return abaddon(boss).retinue(true, false).size() < AbaddonBalance.MAX_KNIGHTS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            for (int i = 0; i < 3; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Vec3 s = floorAt(boss, boss.position().add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, 1.2f, TelegraphMarker.RED, windupLength(boss) + 4);
            }
            sound(boss, AllSounds.abaddon("laugh"), 1.5f, 1f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            for (Vec3 s : spots) level(boss).sendParticles(AllParticles.DEMON_SMOKE.get(), s.x, s.y + 0.2, s.z, 2, 0.2, 0.1, 0.2, 0.05);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            summonRetinue(abaddon(boss), AbaddonBalance.KNIGHTS_PER_CALL, true);
        }
    }

    // --- P2 · Possession -------------------------------------------------------------------------------------------------

    /**
     * She throws her head back and leaves her vessel as black smoke: a yellow lane to the knight she will ride (anyone in it is
     * struck and blinded as the smoke passes), then the possession ({@link AbaddonEntity#beginPossession}).
     */
    public static class SmokeOut extends Cued {
        private @Nullable AbaddonKnightEntity knight;
        private Vec3 from = Vec3.ZERO;
        private float yaw;
        private double length;

        public SmokeOut() {
            super("smoke_out", "smoke_out", AbaddonBalance.SMOKE_WINDUP, AbaddonBalance.SMOKE_FLIGHT, 4);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return abaddon(boss).mayPossess() && vessel(boss) != null ? 1 : 0;
        }

        static @Nullable AbaddonKnightEntity vessel(LuciferEntity boss) {
            AbaddonKnightEntity best = null;
            double bestD = Double.MAX_VALUE;
            for (Entity e : abaddon(boss).retinue(true, false)) {
                if (!(e instanceof AbaddonKnightEntity k) || k.possessed()) continue;
                double d = k.distanceToSqr(boss);
                if (d < bestD && d < 30 * 30) {
                    bestD = d;
                    best = k;
                }
            }
            return best;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            knight = vessel(boss);
            if (knight == null) return;
            from = boss.position();
            yaw = face(boss, knight.position());
            length = Math.max(1, flatDistance(from, knight.position()));
            TelegraphMarker.line(level(boss), from, yaw, AbaddonBalance.SMOKE_WIDTH, (float) length, TelegraphMarker.YELLOW,
                    windupLength(boss) + AbaddonBalance.SMOKE_FLIGHT);
            sound(boss, AllSounds.abaddon("smoke"), 1.5f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (knight == null || !knight.isAlive()) return;
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : inLane(boss, from, dir, length, AbaddonBalance.SMOKE_WIDTH / 2)) {
                hit(boss, e, DamageTypes.MAGIC, AbaddonBalance.SMOKE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, AbaddonBalance.SMOKE_BLIND, 0), boss);
            }
            boss.triggerAnim("action", "possess");
            abaddon(boss).fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.SMOKE_OUT, knight.getId(), 0,
                    boss.position().add(0, 1.3, 0), AbaddonBalance.SMOKE_FLIGHT));
            level(boss).sendParticles(AllParticles.DEMON_SMOKE.get(), boss.getX(), boss.getY() + 1.3, boss.getZ(), 12, 0.3, 0.6, 0.3, 0.05);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != AbaddonBalance.SMOKE_FLIGHT - 1 || knight == null || !knight.isAlive() || !abaddon(boss).mayPossess()) return;
            abaddon(boss).beginPossession(knight);
        }
    }

    // --- P3 · the Brand of the Knights -----------------------------------------------------------------------------------

    /** A clenched fist held out: a violet circle on each hunter; whoever is still in one when it closes is held fast. */
    public static class TelekineticPin extends Cued {
        private final List<Vec3> spots = new ArrayList<>();
        private final List<LivingEntity> pinned = new ArrayList<>();
        private final List<Vec3> holds = new ArrayList<>();

        public TelekineticPin() {
            super("telekinetic_pin", "telekinetic_pin", AbaddonBalance.PIN_WINDUP, AbaddonBalance.PIN_TICKS, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) {
                if (spots.size() >= AbaddonBalance.PIN_MAX) break;
                Vec3 s = floorAt(boss, e.position());
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, AbaddonBalance.PIN_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            }
            sound(boss, AllSounds.abaddon("pin"), 1.2f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : spots) {
                for (LivingEntity e : foes(boss, new AABB(s, s).inflate(AbaddonBalance.PIN_RADIUS + 1, 4, AbaddonBalance.PIN_RADIUS + 1))) {
                    if (pinned.contains(e) || !inCircle(e, s, AbaddonBalance.PIN_RADIUS)) continue;
                    pinned.add(e);
                    holds.add(e.position());
                    hit(boss, e, AllDamageTypes.SPELL, AbaddonBalance.PIN_DAMAGE);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AbaddonBalance.PIN_TICKS, 6), boss);
                    abaddon(boss).fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.PIN, e.getId(), 0, e.position(), AbaddonBalance.PIN_TICKS));
                }
            }
            sound(boss, AllSounds.abaddon("pin"), 2f, 1.1f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            for (int i = 0; i < pinned.size(); i++) {
                LivingEntity e = pinned.get(i);
                if (!e.isAlive()) continue;
                Vec3 h = holds.get(i);
                if (e.position().distanceToSqr(h) > 0.04) e.teleportTo(h.x, h.y, h.z);
                e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                e.hurtMarked = true;
            }
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        /** Tests: whom it holds. */
        public List<LivingEntity> pinned() {
            return pinned;
        }
    }

    /**
     * Her palm pressed down: the Brand of the Knights burns into the ground round her — nested red rings
     * ({@link AbaddonBalance#BRAND_RINGS}), each broken by green gaps; whoever stands on a ring outside a gap burns.
     */
    public static class Brand extends Cued {
        private Vec3 centre = Vec3.ZERO;
        private final List<List<Vec3>> gaps = new ArrayList<>();

        public Brand() {
            super("brand", "brand", AbaddonBalance.BRAND_WINDUP, 2, 24);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            centre = floorAt(boss, boss.position());
            int show = windupLength(boss) + 4;
            for (float r : AbaddonBalance.BRAND_RINGS) {
                TelegraphMarker.ring(level(boss), centre, r + AbaddonBalance.BRAND_BAND, TelegraphMarker.RED, show);
                TelegraphMarker.ring(level(boss), centre, Math.max(0.5f, r - AbaddonBalance.BRAND_BAND), TelegraphMarker.RED, show);
                List<Vec3> ring = new ArrayList<>();
                double base = boss.getRandom().nextDouble() * Math.PI * 2;
                for (int g = 0; g < AbaddonBalance.BRAND_GAPS; g++) {
                    double a = base + g * Math.PI * 2 / AbaddonBalance.BRAND_GAPS + (boss.getRandom().nextDouble() - 0.5) * 0.8;
                    Vec3 at = floorAt(boss, centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
                    ring.add(at);
                    TelegraphMarker.circle(level(boss), at, AbaddonBalance.BRAND_GAP_RADIUS, TelegraphMarker.SAFE, show);
                }
                gaps.add(ring);
            }
            abaddon(boss).fx(new AbaddonFxPayload(boss.getId(), AbaddonFxPayload.BRAND, AbaddonBalance.BRAND_RINGS.length,
                    AbaddonBalance.BRAND_GAPS, centre, show));
            sound(boss, AllSounds.abaddon("brand"), 2f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            float outer = AbaddonBalance.BRAND_RINGS[AbaddonBalance.BRAND_RINGS.length - 1] + AbaddonBalance.BRAND_BAND + 1;
            for (LivingEntity e : foes(boss, new AABB(centre, centre).inflate(outer, 4, outer))) {
                if (Math.abs(e.getY() - centre.y) > 3.5) continue;
                int ring = AbaddonBalance.brandRing(flatDistance(e.position(), centre));
                if (ring < 0) continue;
                double nearest = Double.MAX_VALUE;
                for (Vec3 g : gaps.get(ring)) nearest = Math.min(nearest, flatDistance(e.position(), g));
                if (AbaddonBalance.brandHits(ring, nearest)) {
                    hit(boss, e, AllDamageTypes.HELLFIRE, AbaddonBalance.BRAND_DAMAGE);
                    e.igniteForSeconds(3);
                }
            }
            ServerLevel level = level(boss);
            level.sendParticles(AllParticles.HELLFIRE.get(), centre.x, centre.y + 0.4, centre.z, 160, outer / 2, 0.3, outer / 2, 0.05);
            sound(boss, AllSounds.abaddon("brand"), 3f, 1.1f);
        }

        /** Tests: the gaps of each ring. */
        public List<List<Vec3>> gaps() {
            return gaps;
        }
    }
}
