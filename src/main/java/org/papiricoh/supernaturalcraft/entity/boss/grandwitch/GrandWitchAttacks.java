package org.papiricoh.supernaturalcraft.entity.boss.grandwitch;

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
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.bowl.witch.CurseEffect;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.witch.WitchMagic;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.GrandWitchFxPayload;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.flatDistance;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * The Grand Witch's attacks (v0.21, P6). Every one is shown on the ground in its windup and lands only in ACTIVE: blue for her
 * frost, red for fire (the inferno round her; the circles that follow every hunter in the mass taglock), violet for telekinesis
 * (the crush), green for Rowena's safe circles, yellow for her smoke.
 * <ul>
 *   <li>P1, the Coven's Shield: {@link FrostHex} and {@link Inferno}, seldom (her coven does the work).</li>
 *   <li>P2, the Matriarch's Hexes: {@link FrostHex}, {@link Inferno}, {@link Crush}; {@link SmokeVeil} when the hunter runs.</li>
 *   <li>P3, the Mass Taglock: {@link MassTaglock} (with Rowena's green circles), {@link SmokeVeil}, and the rest.</li>
 * </ul>
 */
public final class GrandWitchAttacks {

    private GrandWitchAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(FrostHex::new, 2), opt(Inferno::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(FrostHex::new, 3), opt(Inferno::new, 3), opt(Crush::new, 3));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(MassTaglock::new, 3), opt(SmokeVeil::new, 2), opt(Crush::new, 2), opt(FrostHex::new, 1.5f), opt(Inferno::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static GrandWitchEntity witch(LuciferEntity boss) {
        return (GrandWitchEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether her hexes should land on {@code e}: not herself, her coven or Rowena, nobody in creative or spectating. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !WitchMagic.kin(e) && !(e instanceof RowenaAllyEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : witch(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    static List<LivingEntity> inside(LuciferEntity boss, Vec3 at, float radius) {
        return foes(boss, new AABB(at, at).inflate(radius + 1, 4, radius + 1)).stream().filter(e -> inCircle(e, at, radius)).toList();
    }

    /** Her hunters, nearest first, {@code target} among them. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(witch(boss).hunters());
        out.removeIf(e -> !isFoe(boss, e));
        if (!out.contains(target)) out.add(target);
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(boss)));
        return out;
    }

    static void face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
    }

    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, GrandWitchBalance.cue(clip), windup, active, recover);
        }
    }

    // --- frost, fire, telekinesis ----------------------------------------------------------------------------------------

    /** The staff swept low: a blue circle under each hunter; frost and a heavy slow on whoever is still in one. */
    public static class FrostHex extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public FrostHex() {
            super("frost_hex", "frost_hex", GrandWitchBalance.FROST_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) {
                if (spots.size() >= GrandWitchBalance.FROST_MAX) break;
                Vec3 s = floorAt(boss, e.position());
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, GrandWitchBalance.FROST_RADIUS, TelegraphMarker.BLUE, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            List<LivingEntity> struck = new ArrayList<>();
            for (Vec3 s : spots) {
                for (LivingEntity e : inside(boss, s, GrandWitchBalance.FROST_RADIUS)) {
                    if (struck.contains(e)) continue;
                    struck.add(e);
                    hit(boss, e, AllDamageTypes.SPELL, GrandWitchBalance.FROST_DAMAGE);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GrandWitchBalance.FROST_SLOW_TICKS, 4), boss);
                    e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 40));
                }
                level(boss).sendParticles(ParticleTypes.SNOWFLAKE, s.x, s.y + 0.3, s.z, 30, 1, 0.4, 1, 0.04);
                witch(boss).fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.FROST_HEX, Math.round(GrandWitchBalance.FROST_RADIUS * 10),
                        0, s, 40));
            }
            sound(boss, AllSounds.coven("grand_witch.frost_hex"), 1.8f, 1f);
        }
    }

    /** The staff raised: a red circle round her; fire. */
    public static class Inferno extends Cued {
        private Vec3 centre = Vec3.ZERO;

        public Inferno() {
            super("inferno", "inferno", GrandWitchBalance.INFERNO_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double r = GrandWitchBalance.INFERNO_RADIUS + 2;
            return boss.distanceToSqr(target) < r * r ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            centre = floorAt(boss, boss.position());
            TelegraphMarker.circle(level(boss), centre, GrandWitchBalance.INFERNO_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inside(boss, centre, GrandWitchBalance.INFERNO_RADIUS)) {
                hit(boss, e, AllDamageTypes.HELLFIRE, GrandWitchBalance.INFERNO_DAMAGE);
                e.igniteForSeconds(GrandWitchBalance.INFERNO_FIRE_SECONDS);
            }
            level(boss).sendParticles(ParticleTypes.FLAME, centre.x, centre.y + 0.4, centre.z, 120, GrandWitchBalance.INFERNO_RADIUS / 2, 0.5,
                    GrandWitchBalance.INFERNO_RADIUS / 2, 0.06);
            witch(boss).fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.INFERNO, Math.round(GrandWitchBalance.INFERNO_RADIUS * 10),
                    0, centre, 30));
            sound(boss, AllSounds.coven("grand_witch.inferno"), 2f, 1f);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }
    }

    /** A clenching fist: a violet circle on the hunter; whoever is still in it is crushed and held a moment. */
    public static class Crush extends Cued {
        private Vec3 spot = Vec3.ZERO;
        private final List<LivingEntity> held = new ArrayList<>();
        private final List<Vec3> holds = new ArrayList<>();

        public Crush() {
            super("crush", "crush", GrandWitchBalance.CRUSH_WINDUP, GrandWitchBalance.CRUSH_PIN_TICKS, 12);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, GrandWitchBalance.CRUSH_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            int first = -1;
            for (LivingEntity e : inside(boss, spot, GrandWitchBalance.CRUSH_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, GrandWitchBalance.CRUSH_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GrandWitchBalance.CRUSH_PIN_TICKS, 6), boss);
                held.add(e);
                holds.add(e.position());
                if (first < 0) first = e.getId();
            }
            witch(boss).fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.CRUSH, first, 0, spot, GrandWitchBalance.CRUSH_PIN_TICKS));
            sound(boss, AllSounds.coven("grand_witch.crush"), 1.8f, 1f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            for (int i = 0; i < held.size(); i++) {
                LivingEntity e = held.get(i);
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
        public List<LivingEntity> held() {
            return held;
        }
    }

    // --- P3 · the Mass Taglock -------------------------------------------------------------------------------------------

    /**
     * Arms spread: every hunter's taglock at once. A red circle follows each of them ({@link GrandWitchBalance#MASS_FOLLOW}),
     * holds, and the curse lands on each one who is not standing in one of the green circles Rowena draws
     * ({@link GrandWitchBalance#SAFE_CIRCLES}, near the hunters). Without Rowena (a test, a command) the circles still come.
     */
    public static class MassTaglock extends Cued {
        private final List<LivingEntity> marked = new ArrayList<>();
        private final List<TelegraphMarker> markers = new ArrayList<>();
        private final List<Vec3> safe = new ArrayList<>();

        public MassTaglock() {
            super("mass_taglock", "mass_taglock", GrandWitchBalance.MASS_FOLLOW + GrandWitchBalance.MASS_HOLD, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ServerLevel level = level(boss);
            int show = windupLength(boss) + 4;
            for (LivingEntity e : marks(boss, target)) {
                marked.add(e);
                markers.add(TelegraphMarker.circle(level, floorAt(boss, e.position()), GrandWitchBalance.MASS_RADIUS, TelegraphMarker.RED, show));
                witch(boss).fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.MASS_TAGLOCK, e.getId(),
                        Math.round(GrandWitchBalance.MASS_RADIUS * 10), e.position(), windupLength(boss)));
            }
            ArenaController arena = boss.arena();
            for (int i = 0; i < GrandWitchBalance.SAFE_CIRCLES; i++) {
                LivingEntity near = marked.get(i % marked.size());
                Vec3 at = null;
                for (int tries = 0; tries < 8 && at == null; tries++) {
                    double a = boss.getRandom().nextDouble() * Math.PI * 2, d = 3 + boss.getRandom().nextDouble() * (GrandWitchBalance.SAFE_REACH - 3);
                    Vec3 p = near.position().add(Math.cos(a) * d, 0, Math.sin(a) * d);
                    if (arena == null || arena.horizontalDistance(p) < arena.radius() - 2) at = p;
                }
                if (at == null) at = near.position();
                Vec3 s = floorAt(boss, at);
                safe.add(s);
                TelegraphMarker.circle(level, s, GrandWitchBalance.SAFE_RADIUS, TelegraphMarker.SAFE, show + 10);
                RowenaAllyEntity r = witch(boss).ally();
                witch(boss).fx(new GrandWitchFxPayload(r != null ? r.getId() : -1, GrandWitchFxPayload.SAFE_CIRCLE,
                        Math.round(GrandWitchBalance.SAFE_RADIUS * 10), 0, s, show + 10));
            }
            RowenaAllyEntity r = witch(boss).ally();
            if (r != null) r.drawCircles(safe);
            sound(boss, AllSounds.coven("grand_witch.taglock"), 2f, 1f);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            if (t > Math.round(GrandWitchBalance.MASS_FOLLOW * boss.windupScale())) return;
            for (int i = 0; i < marked.size(); i++) {
                LivingEntity e = marked.get(i);
                if (!e.isAlive()) continue;
                Vec3 s = floorAt(boss, e.position());
                markers.get(i).setPos(s.x, s.y + 0.02, s.z);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : marked) {
                if (!e.isAlive() || !isFoe(boss, e)) continue;
                double nearest = Double.MAX_VALUE;
                for (Vec3 s : safe) if (Math.abs(e.getY() - s.y) < 3.5) nearest = Math.min(nearest, flatDistance(e.position(), s));
                boolean spared = GrandWitchBalance.spared(nearest);
                if (!spared) {
                    hit(boss, e, AllDamageTypes.SPELL, GrandWitchBalance.MASS_DAMAGE);
                    WitchMagic.curse(boss, e, CurseEffect.Curse.FRAILTY, GrandWitchBalance.MASS_CURSE_TICKS);
                } else {
                    MagicFx.hex(level(boss), e, e.position().add(0, 1, 0), MagicFxPayload.HEX_PROTECT, 1.2f, 20);
                }
                witch(boss).fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.MASS_CURSE, e.getId(), spared ? 0 : 1, e.position(), 20));
            }
            sound(boss, AllSounds.coven("grand_witch.laugh"), 2f, 0.9f);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        /** Tests: the green circles' centres. */
        public List<Vec3> safe() {
            return safe;
        }
    }

    /** A turn into yellow smoke: a yellow circle where she lands (behind the hunter); whoever stands in it is struck. */
    public static class SmokeVeil extends Cued {
        private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;

        public SmokeVeil() {
            super("smoke_veil", "smoke_veil", GrandWitchBalance.VEIL_WINDUP, 2, 12);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return witch(boss).shielded() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            from = boss.position();
            Vec3 behind = target.position().subtract(boss.position()).multiply(1, 0, 1);
            behind = behind.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : behind.normalize();
            Vec3 want = target.position().add(behind.scale(2.5));
            ArenaController arena = boss.arena();
            if (arena != null && arena.horizontalDistance(want) > arena.radius() - 2) want = target.position();
            to = floorAt(boss, want);
            TelegraphMarker.circle(level(boss), to, GrandWitchBalance.VEIL_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
            witch(boss).fx(new GrandWitchFxPayload(boss.getId(), GrandWitchFxPayload.VEIL, 0, 0, to, windupLength(boss)));
            sound(boss, AllSounds.coven("grand_witch.smoke_veil"), 1.4f, 1f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            MagicFx.teleport(level(boss), boss, School.WITCHCRAFT, from, to, MagicFxPayload.SMOKE_YELLOW, 30);
            boss.teleportTo(to.x, to.y, to.z);
            face(boss, target.position());
            for (LivingEntity e : inside(boss, to, GrandWitchBalance.VEIL_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, GrandWitchBalance.VEIL_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, GrandWitchBalance.VEIL_BLIND, 0), boss);
            }
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        public @Nullable Vec3 destination() {
            return to;
        }
    }
}
