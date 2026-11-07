package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.GodstielFxPayload;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Godstiel's attacks (v0.39). Every one is shown on the ground in its windup and lands only in ACTIVE: gold for the light he
 * swallowed, violet for the Leviathans clawing out of him.
 * <ul>
 *   <li>I, The New God: {@link Smite} (gold circles under every hunter), {@link Palm} (a gold lane), {@link Radiance} (a gold
 *   ring round him: blinds and burns).</li>
 *   <li>II, Judgment: {@link Judgment} (heretics kneel in gold rings and burst when judged: kill them first) and {@link Pulpit}
 *   (bolts down a fan of gold lanes), and the light.</li>
 *   <li>III, The Cracks: {@link Bite} (the chest mouth: a violet cone close in) and {@link Retch} (goo in a violet cone: it pools
 *   and burns), and some light.</li>
 *   <li>IV, The Voices: {@link Sweep} (the tentacles all round him: a violet ring) and {@link Slam} (a violet lane).</li>
 *   <li>V, The Overflow, on the reservoir: {@link Surge} (violet geysers) and {@link Doubles} (Leviathans out of the black
 *   water), and the rest of the Leviathans' repertoire.</li>
 * </ul>
 */
public final class GodstielAttacks {

    private GodstielAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Smite::new, 3), opt(Palm::new, 3), opt(Radiance::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Judgment::new, 2.5f), opt(Pulpit::new, 3), opt(Smite::new, 2), opt(Palm::new, 2), opt(Radiance::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Bite::new, 3), opt(Retch::new, 3), opt(Smite::new, 1.5f), opt(Palm::new, 1.5f), opt(Pulpit::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(
            opt(Sweep::new, 3), opt(Slam::new, 3), opt(Bite::new, 2), opt(Retch::new, 2), opt(Judgment::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P5 = List.of(
            opt(Surge::new, 3), opt(Doubles::new, 1.5f), opt(Slam::new, 2.5f), opt(Sweep::new, 2.5f), opt(Bite::new, 2), opt(Retch::new, 2));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            case 4 -> P4;
            default -> P5;
        };
    }

    static GodstielEntity god(LuciferEntity boss) {
        return (GodstielEntity) boss;
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    /** Whether his blows should land on {@code e}: not him nor anything of his, nobody in creative or spectating, no story NPC. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof StoryNpcEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : boss.challengers()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    static List<LivingEntity> inCircleOf(LuciferEntity boss, Vec3 c, double r) {
        return foes(boss, new AABB(c, c).inflate(r + 1, 4, r + 1)).stream().filter(e -> inCircle(e, c, r)).toList();
    }

    /** Everyone in a 120° cone {@code reach} deep from {@code from} facing {@code yaw} (the shape of {@code TelegraphMarker.cone}). */
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

    static void push(Entity e, Vec3 dir, double strength, double up) {
        e.setDeltaMovement(dir.x * strength, up, dir.z * strength);
        e.hurtMarked = true;
    }

    /** An attack whose clip is cued so its moment falls at the end of the windup ({@link GodstielAssets#HIT_TICKS}). */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, GodstielBalance.cue(clip), windup, active, recover);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return god(boss).struggling() ? 0 : 1;
        }
    }

    // --- I · The New God -----------------------------------------------------------------------------------------------------

    /** A snap of the fingers: a gold circle under every hunter, then the light comes down. */
    public static class Smite extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Smite() {
            super("smite_snap", "snap", GodstielBalance.SMITE_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            for (ServerPlayer p : boss.challengers()) spots.add(floorAt(boss, p.position()));
            if (spots.isEmpty()) spots.add(floorAt(boss, target.position()));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, GodstielBalance.SMITE_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("snap", 2f, 1f);
            for (Vec3 s : spots) {
                god(boss).fx(GodstielFxPayload.SMITE, Math.round(GodstielBalance.SMITE_RADIUS * 10), 0, s, 20);
                for (LivingEntity e : inCircleOf(boss, s, GodstielBalance.SMITE_RADIUS)) hit(boss, e, AllDamageTypes.SMITE, GodstielBalance.SMITE_DAMAGE);
            }
        }
    }

    /** A palm thrust: a gold lane ahead, grace down it, and whoever stands in it thrown. */
    public static class Palm extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Palm() {
            super("palm", "smite", GodstielBalance.PALM_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, GodstielBalance.PALM_WIDTH, GodstielBalance.PALM_REACH, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            god(boss).sound("smite", 2f, 1f);
            god(boss).fx(GodstielFxPayload.PALM, Math.round(yaw), Math.round(GodstielBalance.PALM_REACH), from, 16);
            for (LivingEntity e : inLane(boss, from, dir, GodstielBalance.PALM_REACH, GodstielBalance.PALM_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.SMITE, GodstielBalance.PALM_DAMAGE);
                push(e, dir, GodstielBalance.PALM_PUSH, 0.5);
            }
        }
    }

    /** Radiance: arms flung wide, a gold ring round him; inside it at the moment, light that burns and blinds. */
    public static class Radiance extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Radiance() {
            super("radiance", "radiance", GodstielBalance.RADIANCE_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            at = boss.position();
            TelegraphMarker.circle(level(boss), at, GodstielBalance.RADIANCE_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("radiance", 2.5f, 1f);
            god(boss).fx(GodstielFxPayload.RADIANCE, Math.round(GodstielBalance.RADIANCE_RADIUS * 10), 0, at, 30);
            for (LivingEntity e : inCircleOf(boss, at, GodstielBalance.RADIANCE_RADIUS)) {
                hit(boss, e, AllDamageTypes.GRACE, GodstielBalance.RADIANCE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, GodstielBalance.RADIANCE_BLIND, 0));
            }
        }
    }

    // --- II · Judgment -------------------------------------------------------------------------------------------------------

    /** Heretics dragged to kneel in gold rings round the hall; judged at the moment, each one left bursts. */
    public static class Judgment extends Cued {
        private final List<Mob> heretics = new ArrayList<>();

        public Judgment() {
            super("judgment", "judgment", GodstielBalance.JUDGMENT_WINDUP, 2, 24);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return super.weight(boss, target) > 0 && god(boss).heretics() == 0 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            List<Vec3> spots = boss.minionSpots(god(boss).spawnPoints());
            int n = Math.min(spots.size(), boss.minionCount(GodstielBalance.HERETICS));
            for (int i = 0; i < n; i++) {
                Vec3 s = spots.get(i);
                Mob m = (i % 2 == 0 ? AllEntities.ANGEL.get() : AllEntities.BLACK_EYED_DEMON.get()).create(level);
                if (m == null) continue;
                m.moveTo(s.x, s.y, s.z, yawTo(s, boss.position()), 0);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                m.setNoAi(true);
                m.setPersistenceRequired();
                var hp = m.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(GodstielBalance.HERETIC_HEALTH);
                m.setHealth(GodstielBalance.HERETIC_HEALTH);
                m.getPersistentData().putBoolean(GodstielEntity.HERETIC_TAG, true);
                level.addFreshEntity(m);
                heretics.add(m);
                boss.minions().add(m.getUUID());
                TelegraphMarker.ring(level, s, GodstielBalance.HERETIC_BURST_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
                level.sendParticles(AllParticles.GRACE.get(), s.x, s.y + 1, s.z, 20, 0.3, 0.8, 0.3, 0.05);
            }
            god(boss).sound("wings", 2f, 0.8f);
            god(boss).tell("message.supernaturalcraft.godstiel.judgment");
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("judgment", 2.5f, 1f);
            for (Mob m : heretics) {
                if (!m.isAlive()) continue;
                Vec3 s = m.position();
                god(boss).fx(GodstielFxPayload.JUDGED, m.getId(), 0, s, 24);
                for (LivingEntity e : inCircleOf(boss, s, GodstielBalance.HERETIC_BURST_RADIUS)) {
                    if (e != m) hit(boss, e, AllDamageTypes.JUDGMENT, GodstielBalance.HERETIC_BURST_DAMAGE);
                }
                level(boss).sendParticles(AllParticles.GRACE.get(), s.x, s.y + 1, s.z, 60, 0.5, 1.0, 0.5, 0.15);
                m.discard();
            }
            heretics.clear();
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            // Cut short (a transition, the inner fight): whoever is left kneeling goes too.
            for (Mob m : heretics) if (m.isAlive()) m.discard();
            heretics.clear();
        }
    }

    /** The pulpit: bolts of light down a fan of gold lanes from him. */
    public static class Pulpit extends Cued {
        private final List<Vec3> dirs = new ArrayList<>();
        private Vec3 from = Vec3.ZERO;

        public Pulpit() {
            super("pulpit", "judgment", GodstielBalance.PULPIT_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = face(boss, target.position());
            from = boss.position();
            int n = GodstielBalance.PULPIT_LANES;
            for (int i = 0; i < n; i++) {
                float a = yaw + (i - (n - 1) / 2f) * 28f;
                dirs.add(dirOf(a));
                TelegraphMarker.line(level(boss), from, a, GodstielBalance.PULPIT_WIDTH, GodstielBalance.PULPIT_LENGTH, TelegraphMarker.GOLD, windupLength(boss) + 4);
            }
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("smite", 2.5f, 0.8f);
            List<LivingEntity> struck = new ArrayList<>();
            for (Vec3 dir : dirs) {
                Vec3 end = from.add(dir.scale(GodstielBalance.PULPIT_LENGTH));
                god(boss).fx(GodstielFxPayload.BOLT, Math.round((float) (end.x - from.x) * 10), Math.round((float) (end.z - from.z) * 10), from, 14);
                for (LivingEntity e : inLane(boss, from, dir, GodstielBalance.PULPIT_LENGTH, GodstielBalance.PULPIT_WIDTH / 2)) {
                    if (struck.contains(e)) continue;
                    struck.add(e);
                    hit(boss, e, AllDamageTypes.SMITE, GodstielBalance.PULPIT_DAMAGE);
                }
            }
        }
    }

    // --- III · The Cracks ----------------------------------------------------------------------------------------------------

    /** The mouth in his chest lunges: a violet cone close in front. */
    public static class Bite extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Bite() {
            super("bite", "bite", GodstielBalance.BITE_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.cone(level(boss), from, yaw, GodstielBalance.BITE_REACH, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("bite", 2f, 1f);
            for (LivingEntity e : inCone(boss, from, yaw, GodstielBalance.BITE_REACH)) hit(boss, e, AllDamageTypes.SPELL, GodstielBalance.BITE_DAMAGE);
        }
    }

    /** He retches goo in a violet cone: it pools on the floor (sticky, it burns) and dries in a while. */
    public static class Retch extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Retch() {
            super("retch", "retch", GodstielBalance.RETCH_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.cone(level(boss), from, yaw, GodstielBalance.RETCH_REACH, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("goo", 2f, 0.9f);
            god(boss).fx(GodstielFxPayload.GOO, Math.round(yaw), Math.round(GodstielBalance.RETCH_REACH), from, 16);
            for (LivingEntity e : inCone(boss, from, yaw, GodstielBalance.RETCH_REACH)) hit(boss, e, AllDamageTypes.SPELL, GodstielBalance.RETCH_DAMAGE);
            god(boss).spillGoo(from, yaw, GodstielBalance.RETCH_REACH);
        }
    }

    // --- IV · The Voices -----------------------------------------------------------------------------------------------------

    /** The tentacles sweep all round him: a violet ring. Whoever is in it is thrown out. */
    public static class Sweep extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Sweep() {
            super("sweep", "sweep", GodstielBalance.SWEEP_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            at = boss.position();
            TelegraphMarker.circle(level(boss), at, GodstielBalance.SWEEP_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("goo", 2f, 0.7f);
            god(boss).fx(GodstielFxPayload.SWEEP, Math.round(GodstielBalance.SWEEP_RADIUS * 10), 0, at, 16);
            for (LivingEntity e : inCircleOf(boss, at, GodstielBalance.SWEEP_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, GodstielBalance.SWEEP_DAMAGE);
                Vec3 out = e.position().subtract(at).multiply(1, 0, 1);
                push(e, out.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : out.normalize(), GodstielBalance.SWEEP_PUSH, 0.4);
            }
        }
    }

    /** The tentacles rise and slam down a violet lane. */
    public static class Slam extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Slam() {
            super("slam", "slam", GodstielBalance.SLAM_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, GodstielBalance.SLAM_WIDTH, GodstielBalance.SLAM_REACH, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("goo", 2.5f, 0.6f);
            Vec3 dir = dirOf(yaw);
            Vec3 end = from.add(dir.scale(GodstielBalance.SLAM_REACH));
            god(boss).fx(GodstielFxPayload.BOLT, Math.round((float) (end.x - from.x) * 10), Math.round((float) (end.z - from.z) * 10), from, -14);
            for (LivingEntity e : inLane(boss, from, dir, GodstielBalance.SLAM_REACH, GodstielBalance.SLAM_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.SPELL, GodstielBalance.SLAM_DAMAGE);
                e.setDeltaMovement(e.getDeltaMovement().x, 0.6, e.getDeltaMovement().z);
                e.hurtMarked = true;
            }
        }
    }

    // --- V · The Overflow ----------------------------------------------------------------------------------------------------

    /** Goo erupts round him: violet circles on the deck, near the hunters, then black geysers. */
    public static class Surge extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Surge() {
            super("surge", "surge", GodstielBalance.SURGE_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) spots.add(floorAt(boss, p.position()));
            Vec3 c = god(boss).arenaCentre();
            while (spots.size() < GodstielBalance.SURGE_SPOTS) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, r = 2 + boss.getRandom().nextDouble() * 5;
                spots.add(floorAt(boss, c.add(Math.cos(a) * r, 0, Math.sin(a) * r)));
            }
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, GodstielBalance.SURGE_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            god(boss).sound("goo", 2.5f, 0.5f);
            for (Vec3 s : spots) {
                god(boss).fx(GodstielFxPayload.SURGE, Math.round(GodstielBalance.SURGE_RADIUS * 10), 0, s, 24);
                for (LivingEntity e : inCircleOf(boss, s, GodstielBalance.SURGE_RADIUS)) {
                    hit(boss, e, AllDamageTypes.SPELL, GodstielBalance.SURGE_DAMAGE);
                    e.setDeltaMovement(e.getDeltaMovement().x, 0.9, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
            }
        }
    }

    /** Leviathans climb out of the black water onto the deck: doubles that serve him (borax bursts them). */
    public static class Doubles extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Doubles() {
            super("doubles", "surge", GodstielBalance.DOUBLES_WINDUP, 2, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return super.weight(boss, target) > 0 && god(boss).doubles() < GodstielBalance.MAX_DOUBLES ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            List<Vec3> all = boss.minionSpots(god(boss).spawnPoints());
            int n = Math.min(all.size(), Math.min(boss.minionCount(GodstielBalance.DOUBLES_PER_CALL),
                    GodstielBalance.MAX_DOUBLES - god(boss).doubles()));
            for (int i = 0; i < n; i++) {
                spots.add(all.get(i));
                TelegraphMarker.ring(level(boss), all.get(i), 1.4f, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            }
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (Vec3 s : spots) {
                LeviathanEntity l = AllEntities.LEVIATHAN.get().create(level);
                if (l == null) continue;
                l.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                l.serve(boss.getUUID());
                l.setPersistenceRequired();
                level.addFreshEntity(l);
                l.reveal();
                l.setTarget(target);
                boss.minions().add(l.getUUID());
                god(boss).fx(GodstielFxPayload.SURGE, 14, 0, s, 20);
            }
            god(boss).sound("voices", 2f, 0.8f);
        }
    }
}
