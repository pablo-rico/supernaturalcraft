package org.papiricoh.supernaturalcraft.entity.boss.eve;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.EveFxPayload;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatorySoul;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.randomArenaPoint;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Eve's attacks (v0.39). Every one is shown on the ground in its windup and lands only in ACTIVE: red for her blows (the lash's
 * cone, the roots' circles, the swarm's lane, the thorns' ring, the field), yellow where her children climb out, green for where
 * one is safe (at her feet from the thorns, the field's few circles), violet for the reach of her call.
 * <ul>
 *   <li>P1, the Mother: {@link Lash}, {@link RootGrasp}, {@link Birth}.</li>
 *   <li>P2, the Swarm: {@link Swarm}, and the Mother's three.</li>
 *   <li>P3, Mother's Call: {@link Call} (Purgatory's souls turn on the hunters), {@link ThornRing}, the swarm and the roots.</li>
 *   <li>P4, the Mother of All: {@link RootField}, the swarm down two lanes, the thorns, the call, births.</li>
 * </ul>
 */
public final class EveAttacks {

    private EveAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Lash::new, 3), opt(RootGrasp::new, 2.5f),
            opt(Birth::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Swarm::new, 3), opt(Lash::new, 2),
            opt(RootGrasp::new, 2), opt(Birth::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Call::new, 1.5f), opt(ThornRing::new, 2.5f),
            opt(Swarm::new, 2), opt(RootGrasp::new, 2), opt(Lash::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(opt(RootField::new, 2.5f), opt(Swarm::new, 2),
            opt(ThornRing::new, 2), opt(Call::new, 1), opt(Birth::new, 1), opt(RootGrasp::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    static EveEntity eve(LuciferEntity boss) {
        return (EveEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether her blows should land on {@code e}: not herself, her children or Purgatory's souls, nobody creative or watching. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof EveEntity) && !(e instanceof PurgatorySoul)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    /** Everyone her blows may find in {@code box}, her tracked hunters included. */
    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : eve(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    /** The hunters to spread an attack over: every challenger (and the tracked ones), or the target alone. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(eve(boss).hunters());
        if (!out.contains(target)) out.add(0, target);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(60));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 3.5 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

    static double reachInArena(LuciferEntity boss, Vec3 from, Vec3 dir, double max) {
        ArenaController arena = boss.arena();
        if (arena == null) return max;
        double limit = arena.radius() - 1.5;
        double d = max;
        while (d > 1 && arena.horizontalDistance(from.add(dir.scale(d))) > limit) d -= 0.5;
        return d;
    }

    /** Roots hold someone: rooted in place, and the roots show. */
    static void hold(LuciferEntity boss, LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6), boss);
        e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
        e.hurtMarked = true;
    }

    /**
     * Roots left standing round {@code c} for a while (through the arena, so they go when it does): mangrove roots on a ring of
     * air cells over the floor, never where someone stands.
     */
    static void rootsAround(LuciferEntity boss, Vec3 c, float radius, int ticks) {
        ArenaController arena = boss.arena();
        if (arena == null) return;
        BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
        int n = Math.max(4, Math.round(radius * 4));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + boss.getRandom().nextDouble() * 0.3;
            BlockPos p = BlockPos.containing(c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius);
            if (!level(boss).getBlockState(p).isAir() || level(boss).getBlockState(p.below()).isAir()) continue;
            if (!level(boss).getEntitiesOfClass(LivingEntity.class, new AABB(p)).isEmpty()) continue;
            if (!arena.contains(p)) continue;
            arena.mutate(level(boss), p, roots, ticks);
        }
    }

    /** An attack whose clip is cued so its blow falls at the end of the windup ({@link EveAssets#HIT_TICKS}). */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, EveBalance.cue(clip), windup, active, recover);
        }
    }

    // --- P1 · the Mother -------------------------------------------------------------------------------------------------

    /** A backhand of root-clawed fingers across a red cone before her. */
    public static class Lash extends Cued {
        private float yaw;

        public Lash() {
            super("lash", "lash", EveBalance.LASH_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double r = EveBalance.LASH_REACH + 2;
            return boss.distanceToSqr(target) < r * r ? 1 : 0.1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, EveBalance.LASH_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : inCone(boss, boss.position(), yaw, EveBalance.LASH_REACH)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, EveBalance.LASH_DAMAGE);
                e.push(dir.x * EveBalance.LASH_PUSH, 0.3, dir.z * EveBalance.LASH_PUSH);
                e.hurtMarked = true;
            }
            eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.LASH, Math.round(yaw), Math.round(EveBalance.LASH_REACH * 10), boss.position(), 8));
            sound(boss, AllSounds.purgatory("eve_roots"), 1.2f, 1.4f);
        }
    }

    /** She drives a hand into the earth: a red circle under each hunter; the roots rise there and hold whoever stands in it. */
    public static class RootGrasp extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public RootGrasp() {
            super("root_grasp", "root_grasp", EveBalance.GRASP_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) if (spots.size() < EveBalance.GRASP_MAX) spots.add(floorAt(boss, e.position()));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, EveBalance.GRASP_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : spots) {
                boolean held = false;
                for (LivingEntity e : foes(boss, new AABB(s, s).inflate(EveBalance.GRASP_RADIUS + 1, 3, EveBalance.GRASP_RADIUS + 1))) {
                    if (!inCircle(e, s, EveBalance.GRASP_RADIUS)) continue;
                    hit(boss, e, DamageTypes.MOB_ATTACK, EveBalance.GRASP_DAMAGE);
                    hold(boss, e, EveBalance.GRASP_HOLD);
                    held = true;
                }
                rootsAround(boss, s, EveBalance.GRASP_RADIUS + 0.6f, EveBalance.GRASP_ROOTS_LINGER);
                eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.ROOTS, Math.round(EveBalance.GRASP_RADIUS * 10), held ? 1 : 0, s,
                        EveBalance.GRASP_HOLD));
            }
            sound(boss, AllSounds.purgatory("eve_roots"), 1.8f, 0.8f);
        }
    }

    /** Arms spread, head back: the earth heaves at yellow circles and her children climb out. */
    public static class Birth extends Cued {
        private final List<Vec3> spots = new ArrayList<>();
        private final int base;

        public Birth() {
            this(EveBalance.BIRTH_BASE);
        }

        Birth(int base) {
            super("birth", "birth", EveBalance.BIRTH_WINDUP, 2, 18);
            this.base = base;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return eve(boss).children() >= EveBalance.CHILD_CAP ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            int n = base + (boss.phase() >= EveBalance.PHASES ? 1 : 0);
            for (int i = 0; i < n; i++) spots.add(eve(boss).birthSpot(i));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, EveBalance.BIRTH_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
            sound(boss, AllSounds.purgatory("eve_birth"), 1.4f, 1.0f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : boss.minionSpots(spots)) {
                Mob child = eve(boss).birth(s, target);
                if (child != null) eve(boss).fx(new EveFxPayload(child.getId(), EveFxPayload.BIRTH, 0, 0, s, 24));
            }
        }
    }

    // --- P2 · the Swarm --------------------------------------------------------------------------------------------------

    /** Her jaw drops and roaches pour out down a red lane (two lanes in the last phase), biting whoever stays in it. */
    public static class Swarm extends Cued {
        private final List<Float> yaws = new ArrayList<>();
        private final List<Double> lengths = new ArrayList<>();
        private Vec3 from = Vec3.ZERO;

        public Swarm() {
            super("swarm", "roach_swarm", EveBalance.SWARM_WINDUP, EveBalance.SWARM_TICKS, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            float first = face(boss, target.position());
            from = boss.position();
            int lanes = boss.phase() >= EveBalance.PHASES ? EveBalance.LAST_SWARMS : 1;
            List<LivingEntity> marks = marks(boss, target);
            for (int i = 0; i < lanes; i++) {
                float yaw = i == 0 ? first : marks.size() > i ? yawTo(from, marks.get(i).position()) : first + 70f;
                double len = reachInArena(boss, from, dirOf(yaw), EveBalance.SWARM_REACH);
                yaws.add(yaw);
                lengths.add(len);
                TelegraphMarker.line(level(boss), from, yaw, EveBalance.SWARM_WIDTH, (float) len, TelegraphMarker.RED, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < yaws.size(); i++) {
                eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.SWARM, Math.round(yaws.get(i)), (int) Math.round(lengths.get(i) * 10), from,
                        EveBalance.SWARM_TICKS));
            }
            sound(boss, AllSounds.purgatory("eve_roaches"), 1.8f, 1.0f);
            bite(boss);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t > 0 && t % EveBalance.SWARM_EVERY == 0) bite(boss);
        }

        private void bite(LuciferEntity boss) {
            for (int i = 0; i < yaws.size(); i++) {
                for (LivingEntity e : inLane(boss, from, dirOf(yaws.get(i)), lengths.get(i), EveBalance.SWARM_WIDTH / 2)) {
                    e.invulnerableTime = 0;
                    hit(boss, e, DamageTypes.MOB_ATTACK, EveBalance.SWARM_DAMAGE);
                    e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1), boss);
                }
            }
        }
    }

    // --- P3 · Mother's Call ----------------------------------------------------------------------------------------------

    /** Arms raised, a long cry (its reach in violet): every soul of Purgatory near turns on the hunters, and a child climbs out. */
    public static class Call extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Call() {
            super("call", "call", EveBalance.CALL_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ArenaController arena = boss.arena();
            float reach = arena != null ? Math.min(EveBalance.CALL_RANGE, arena.radius() - 1) : 12f;
            TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), reach, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            for (int i = 0; i < EveBalance.CALL_BIRTHS; i++) spots.add(eve(boss).birthSpot(i + 3));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, EveBalance.BIRTH_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
            sound(boss, AllSounds.purgatory("eve_call"), 3.0f, 1.0f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            int answered = eve(boss).callSouls(target);
            for (Vec3 s : boss.minionSpots(spots)) {
                Mob child = eve(boss).birth(s, target);
                if (child != null) eve(boss).fx(new EveFxPayload(child.getId(), EveFxPayload.BIRTH, 0, 0, s, 24));
            }
            eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.CALL, Math.round(EveBalance.CALL_RANGE), answered, boss.position(), 40));
        }
    }

    /** Both fists down: a red ring of thorns bursts round her; only at her feet (green) or beyond the ring is one safe. */
    public static class ThornRing extends Cued {
        private Vec3 c = Vec3.ZERO;

        public ThornRing() {
            super("thorn_ring", "thorn_ring", EveBalance.THORN_WINDUP, 2, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double d = Math.sqrt(boss.distanceToSqr(target));
            return d > EveBalance.THORN_INNER && d < EveBalance.THORN_OUTER + 2 ? 1 : 0.3f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            c = floorAt(boss, boss.position());
            int life = windupLength(boss) + 4;
            TelegraphMarker.ring(level(boss), c, EveBalance.THORN_OUTER, TelegraphMarker.RED, life);
            TelegraphMarker.ring(level(boss), c, (EveBalance.THORN_OUTER + EveBalance.THORN_INNER) / 2, TelegraphMarker.RED, life);
            TelegraphMarker.circle(level(boss), c, EveBalance.THORN_INNER, TelegraphMarker.SAFE, life);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : foes(boss, new AABB(c, c).inflate(EveBalance.THORN_OUTER + 1, 3, EveBalance.THORN_OUTER + 1))) {
                double d = Math.sqrt(e.position().subtract(c).multiply(1, 0, 1).lengthSqr());
                if (d < EveBalance.THORN_INNER || d > EveBalance.THORN_OUTER || Math.abs(e.getY() - c.y) > 3.5) continue;
                hit(boss, e, DamageTypes.MOB_ATTACK, EveBalance.THORN_DAMAGE);
                e.push(0, 0.5, 0);
                e.hurtMarked = true;
            }
            rootsAround(boss, c, EveBalance.THORN_OUTER - 0.5f, EveBalance.GRASP_ROOTS_LINGER / 2);
            eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.THORNS, Math.round(EveBalance.THORN_INNER * 10),
                    Math.round(EveBalance.THORN_OUTER * 10), c, 20));
            sound(boss, AllSounds.purgatory("eve_roots"), 2.0f, 0.6f);
        }
    }

    // --- P4 · the Mother of All ------------------------------------------------------------------------------------------

    /** She kneels, palms on the ground: red over the whole glade but a few green circles; then all of it erupts at once. */
    public static class RootField extends Cued {
        private final List<Vec3> safe = new ArrayList<>();
        private Vec3 c = Vec3.ZERO;
        private float radius;

        public RootField() {
            super("root_field", "root_field", EveBalance.FIELD_WINDUP, 2, 24);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ArenaController arena = boss.arena();
            c = floorAt(boss, arena != null ? arena.centerVec() : boss.position());
            radius = arena != null ? arena.radius() - 1 : 14;
            int life = windupLength(boss) + 4;
            TelegraphMarker.circle(level(boss), c, radius, TelegraphMarker.RED, life);
            for (int i = 0; i < EveBalance.FIELD_SAFE; i++) safe.add(randomArenaPoint(boss, 0.7));
            for (Vec3 s : safe) TelegraphMarker.circle(level(boss), s, EveBalance.FIELD_SAFE_RADIUS, TelegraphMarker.SAFE, life);
            eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.FIELD, Math.round(radius), 0, c, life));
        }

        /** Whether {@code e} stands in one of the green circles. */
        boolean sheltered(LivingEntity e) {
            for (Vec3 s : safe) if (inCircle(e, s, EveBalance.FIELD_SAFE_RADIUS)) return true;
            return false;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : foes(boss, new AABB(c, c).inflate(radius + 1, 4, radius + 1))) {
                if (!inCircle(e, c, radius) || sheltered(e)) continue;
                hit(boss, e, DamageTypes.MOB_ATTACK, EveBalance.FIELD_DAMAGE);
                hold(boss, e, EveBalance.GRASP_HOLD / 2);
            }
            for (int i = 0; i < 6; i++) rootsAround(boss, randomArenaPoint(boss, 0.85), 1.5f, EveBalance.GRASP_ROOTS_LINGER);
            eve(boss).fx(new EveFxPayload(boss.getId(), EveFxPayload.ROOTS, Math.round(radius * 10), 1, c, 30));
            sound(boss, AllSounds.purgatory("eve_roots"), 3.0f, 0.5f);
        }

        /** Tests: the safe circles chosen. */
        public List<Vec3> safeSpots() {
            return safe;
        }
    }
}
