package org.papiricoh.supernaturalcraft.entity.boss.dick;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.DickFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
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
 * Dick Roman's attacks (v0.25). Every one is shown on the ground in its windup and lands only in ACTIVE: red for his blows
 * (the downsizing ring, the bite's cone, the swallow's lane, the goo's circles, the tentacles' lanes), violet for the
 * contract's lanes (it binds), yellow for where his staff walk in and for the goo's surge.
 * <ul>
 *   <li>P1, the Executive: {@link Downsizing}, {@link SignHere}, {@link StaffMeeting}.</li>
 *   <li>P2, the Maw: {@link Bite}, {@link Swallow}, {@link GooSpew}, and the contract still.</li>
 *   <li>P3, the Copy: {@link Copy} (he hides among doubles in a hunter's face; borax finds him), and the Maw's blows.</li>
 *   <li>P4, the Flood: {@link Tentacles}, {@link Surge} (the goo rolls back over the dried islands), the bite and the spew.</li>
 * </ul>
 */
public final class DickAttacks {

    private DickAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Downsizing::new, 3), opt(SignHere::new, 2.5f),
            opt(StaffMeeting::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Bite::new, 3), opt(Swallow::new, 2),
            opt(GooSpew::new, 2), opt(SignHere::new, 1), opt(Downsizing::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Copy::new, 1.5f), opt(Bite::new, 3),
            opt(GooSpew::new, 2), opt(Swallow::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(opt(Tentacles::new, 3), opt(Surge::new, 1.2f),
            opt(Bite::new, 2), opt(GooSpew::new, 1.5f), opt(Swallow::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    static DickRomanEntity dick(LuciferEntity boss) {
        return (DickRomanEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether his blows should land on {@code e}: not himself or his own, nobody in creative or spectating. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof DickRomanEntity) && !(e instanceof LeviathanEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    /** Everyone his blows may find in {@code box}, his tracked hunters included. */
    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : dick(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    /** The hunters to spread an attack over: every challenger (and the tracked ones), or the target alone. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(dick(boss).hunters());
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

    /** The floor under {@code p} in his arena. */
    static Vec3 floor(LuciferEntity boss, Vec3 p) {
        return floorAt(boss, p);
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} along {@code dir}. */
    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
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

    /** How far from {@code from} along {@code dir} stays inside his arena (at most {@code max}). */
    static double reachInArena(LuciferEntity boss, Vec3 from, Vec3 dir, double max) {
        ArenaController arena = boss.arena();
        if (arena == null) return max;
        double limit = arena.radius() - 1.5;
        double d = max;
        while (d > 1 && arena.horizontalDistance(from.add(dir.scale(d))) > limit) d -= 0.5;
        return d;
    }

    /** An attack whose clip is cued so its blow falls at the end of the windup ({@link DickAssets#HIT_TICKS}). */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, DickBalance.cue(clip), windup, active, recover);
        }
    }

    // --- P1 · the Executive ----------------------------------------------------------------------------------------------

    /** "We're making some cuts": a red ring round him, then a shockwave that throws everyone in it out. */
    public static class Downsizing extends Cued {
        public Downsizing() {
            super("downsizing", "downsizing", DickBalance.DOWNSIZE_WINDUP, 2, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < DickBalance.DOWNSIZE_RADIUS * DickBalance.DOWNSIZE_RADIUS ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            TelegraphMarker.circle(level(boss), boss.position(), DickBalance.DOWNSIZE_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 c = boss.position();
            for (LivingEntity e : foes(boss, new AABB(c, c).inflate(DickBalance.DOWNSIZE_RADIUS + 1, 3, DickBalance.DOWNSIZE_RADIUS + 1))) {
                if (!inCircle(e, c, DickBalance.DOWNSIZE_RADIUS)) continue;
                hit(boss, e, DamageTypes.MOB_ATTACK, DickBalance.DOWNSIZE_DAMAGE);
                Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0e-4 ? dirOf(boss.getYRot()) : away.normalize();
                e.push(away.x * DickBalance.DOWNSIZE_PUSH, 0.4, away.z * DickBalance.DOWNSIZE_PUSH);
                e.hurtMarked = true;
            }
            dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.DOWNSIZE, Math.round(DickBalance.DOWNSIZE_RADIUS * 10), 0, c, 12));
            sound(boss, AllSounds.dick("laugh"), 1.5f, 0.9f);
        }
    }

    /** "Sign here": a violet lane to each hunter; whoever stands in one when the pen comes down is bound (rooted) and stung. */
    public static class SignHere extends Cued {
        private final List<Float> yaws = new ArrayList<>();
        private final List<Double> lengths = new ArrayList<>();
        private Vec3 from = Vec3.ZERO;

        public SignHere() {
            super("sign_here", "sign_here", DickBalance.SIGN_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            from = boss.position();
            for (LivingEntity e : marks(boss, target)) {
                if (yaws.size() >= DickBalance.SIGN_MAX) break;
                float yaw = yawTo(from, e.position());
                double len = reachInArena(boss, from, dirOf(yaw), DickBalance.SIGN_REACH);
                yaws.add(yaw);
                lengths.add(len);
                TelegraphMarker.line(level(boss), from, yaw, DickBalance.SIGN_WIDTH, (float) len, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < yaws.size(); i++) {
                for (LivingEntity e : inLane(boss, from, dirOf(yaws.get(i)), lengths.get(i), DickBalance.SIGN_WIDTH / 2)) {
                    hit(boss, e, DamageTypes.MAGIC, DickBalance.SIGN_DAMAGE);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DickBalance.SIGN_ROOT, 6), boss);
                    dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.CONTRACT, e.getId(), 0, e.position(), DickBalance.SIGN_ROOT));
                }
            }
        }
    }

    /** "Staff meeting": his employees walk in at yellow circles near the edge — Leviathans, in villagers' faces, for a moment. */
    public static class StaffMeeting extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public StaffMeeting() {
            super("staff_meeting", "staff_meeting", DickBalance.STAFF_WINDUP, 2, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return dick(boss).staff() >= DickBalance.STAFF * 2 ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            for (int i = 0; i < DickBalance.STAFF; i++) spots.add(randomArenaPoint(boss, 0.85));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, DickBalance.STAFF_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : boss.minionSpots(spots)) {
                LeviathanEntity l = AllEntities.LEVIATHAN.get().create(level(boss));
                if (l == null) continue;
                l.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                l.disguiseAsVillager(boss.getRandom());
                l.setPersistenceRequired();
                level(boss).addFreshEntity(l);
                l.reveal();
                l.setTarget(target);
                boss.minions().add(l.getUUID());
            }
        }
    }

    // --- P2 · the Maw ----------------------------------------------------------------------------------------------------

    /** The maw snaps shut on a red cone before him. */
    public static class Bite extends Cued {
        private float yaw;

        public Bite() {
            super("bite", "bite", DickBalance.BITE_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < (DickBalance.BITE_REACH + 2) * (DickBalance.BITE_REACH + 2) ? 1 : 0.1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, DickBalance.BITE_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inCone(boss, boss.position(), yaw, DickBalance.BITE_REACH)) hit(boss, e, DamageTypes.MOB_ATTACK, DickBalance.BITE_DAMAGE);
            dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.BITE, Math.round(yaw), Math.round(DickBalance.BITE_REACH * 10), boss.position(), 8));
            sound(boss, AllSounds.dick("bite"), 1.6f, 1.0f);
        }
    }

    /** A red lane: he lunges down it; the first hunter in it is held in his maw, chewed, then spat out back down the lane. */
    public static class Swallow extends Cued {
        private float yaw;
        private double length;
        private Vec3 from = Vec3.ZERO;
        private @Nullable LivingEntity held;

        public Swallow() {
            super("swallow", "swallow", DickBalance.SWALLOW_WINDUP, DickBalance.SWALLOW_HOLD, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            length = reachInArena(boss, from, dirOf(yaw), DickBalance.SWALLOW_REACH);
            TelegraphMarker.line(level(boss), from, yaw, DickBalance.SWALLOW_WIDTH, (float) length, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            List<LivingEntity> caught = new ArrayList<>(inLane(boss, from, dir, length, DickBalance.SWALLOW_WIDTH / 2));
            caught.sort((a, b) -> Double.compare(a.distanceToSqr(from), b.distanceToSqr(from)));
            held = caught.isEmpty() ? null : caught.getFirst();
            Vec3 end = held != null ? held.position().subtract(dir.scale(1.2)) : from.add(dir.scale(length));
            end = floorAt(boss, end);
            boss.teleportTo(end.x, end.y, end.z);
            boss.getNavigation().stop();
            if (held != null) {
                hit(boss, held, DamageTypes.MOB_ATTACK, DickBalance.SWALLOW_DAMAGE);
                dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.SWALLOW, held.getId(), 0, boss.getEyePosition(), DickBalance.SWALLOW_HOLD));
                sound(boss, AllSounds.dick("bite"), 1.6f, 0.7f);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (held == null || !held.isAlive()) return;
            Vec3 mouth = boss.position().add(dirOf(boss.getYRot()).scale(0.9)).add(0, 0.6, 0);
            held.teleportTo(mouth.x, mouth.y, mouth.z);
            held.setDeltaMovement(Vec3.ZERO);
            held.hurtMarked = true;
            if (t > 0 && t % 20 == 0) hit(boss, held, DamageTypes.MOB_ATTACK, DickBalance.SWALLOW_DAMAGE);
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return held == null;
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            if (held == null || !held.isAlive()) return;
            Vec3 back = dirOf(yaw).scale(-1);
            hit(boss, held, DamageTypes.MOB_ATTACK, DickBalance.SPIT_DAMAGE);
            held.push(back.x * DickBalance.SPIT_PUSH, 0.5, back.z * DickBalance.SPIT_PUSH);
            held.hurtMarked = true;
            boss.triggerAnim("action", "spit");
            dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.SWALLOW, held.getId(), 1, boss.getEyePosition(), 0));
            held = null;
        }
    }

    /** He retches goo: a red circle on each hunter (and one more); goo splashes there, and stays a while (slow, burning). */
    public static class GooSpew extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public GooSpew() {
            super("goo_spew", "goo_spew", DickBalance.GOO_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) if (spots.size() < DickBalance.GOO_MAX) spots.add(floorAt(boss, e.position()));
            spots.add(randomArenaPoint(boss, 0.7));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, DickBalance.GOO_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            BlockState goo = AllBlocks.LEVIATHAN_GOO.get().defaultBlockState();
            int r = (int) Math.ceil(DickBalance.GOO_RADIUS);
            for (Vec3 s : spots) {
                for (LivingEntity e : foes(boss, new AABB(s, s).inflate(DickBalance.GOO_RADIUS + 1, 3, DickBalance.GOO_RADIUS + 1))) {
                    if (inCircle(e, s, DickBalance.GOO_RADIUS)) hit(boss, e, DamageTypes.MOB_ATTACK, DickBalance.GOO_DAMAGE);
                }
                if (arena != null) {
                    BlockPos c = BlockPos.containing(s);
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dz * dz > DickBalance.GOO_RADIUS * DickBalance.GOO_RADIUS) continue;
                            BlockPos p = c.offset(dx, 0, dz);
                            if (level(boss).getBlockState(p).isAir() && !level(boss).getBlockState(p.below()).isAir()) {
                                arena.mutate(level(boss), p, goo, DickBalance.GOO_TICKS);
                            }
                        }
                    }
                }
                dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.GOO_SPLASH, Math.round(DickBalance.GOO_RADIUS * 10), 0, s, 14));
            }
            sound(boss, AllSounds.dick("goo"), 1.6f, 0.9f);
        }
    }

    // --- P3 · the Copy ---------------------------------------------------------------------------------------------------

    /** He shudders into a hunter's face, and doubles in the same skin step out of the goo round them (yellow circles first). */
    public static class Copy extends Cued {
        private @Nullable LivingEntity copied;

        public Copy() {
            super("copy", "copy", DickBalance.COPY_WINDUP, 2, 10);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return dick(boss).hidden() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            List<LivingEntity> marks = marks(boss, target);
            copied = marks.get(boss.getRandom().nextInt(marks.size()));
            TelegraphMarker.circle(level(boss), boss.position(), 2.5f, TelegraphMarker.YELLOW, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            LivingEntity who = copied != null && copied.isAlive() ? copied : target;
            ArenaController arena = boss.arena();
            dick(boss).beginCopy(level(boss), who, arena != null ? arena.centerVec() : boss.position());
        }
    }

    // --- P4 · the Flood --------------------------------------------------------------------------------------------------

    /** Tentacles of goo rise behind each hunter and lash down red lanes through them. */
    public static class Tentacles extends Cued {
        private final List<Vec3> froms = new ArrayList<>();
        private final List<Float> yaws = new ArrayList<>();

        public Tentacles() {
            super("tentacles", "tentacle_slam", DickBalance.TENTACLE_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            for (LivingEntity e : marks(boss, target)) {
                if (froms.size() >= DickBalance.TENTACLE_MAX) break;
                Vec3 away = e.position().subtract(boss.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0e-4 ? dirOf(boss.getYRot()) : away.normalize();
                Vec3 from = floorAt(boss, e.position().add(away.scale(DickBalance.TENTACLE_REACH / 2)));
                ArenaController arena = boss.arena();
                if (arena != null && arena.horizontalDistance(from) > arena.radius() - 2) from = floorAt(boss, e.position().add(away.scale(2)));
                float yaw = yawTo(from, e.position());
                froms.add(from);
                yaws.add(yaw);
                TelegraphMarker.line(level(boss), from, yaw, DickBalance.TENTACLE_WIDTH, DickBalance.TENTACLE_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < froms.size(); i++) {
                Vec3 dir = dirOf(yaws.get(i));
                for (LivingEntity e : inLane(boss, froms.get(i), dir, DickBalance.TENTACLE_REACH, DickBalance.TENTACLE_WIDTH / 2)) {
                    hit(boss, e, DamageTypes.MOB_ATTACK, DickBalance.TENTACLE_DAMAGE);
                    e.push(0, 0.6, 0);
                    e.hurtMarked = true;
                }
                dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.TENTACLE, Math.round(yaws.get(i)),
                        Math.round(DickBalance.TENTACLE_REACH * 10), froms.get(i), 14));
            }
            sound(boss, AllSounds.dick("goo"), 1.8f, 0.6f);
        }
    }

    /** A yellow warning over the whole floor; then the goo rolls back over every patch borax dried. */
    public static class Surge extends Cued {
        public Surge() {
            super("surge", "goo_spew", DickBalance.SURGE_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ArenaController arena = boss.arena();
            if (arena == null) return;
            TelegraphMarker.ring(level(boss), floorAt(boss, arena.centerVec()), arena.radius() - 1, TelegraphMarker.YELLOW, windupLength(boss) + 4);
            dick(boss).fx(new DickFxPayload(boss.getId(), DickFxPayload.SURGE, arena.radius(), 0, floorAt(boss, arena.centerVec()), windupLength(boss)));
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            dick(boss).surge(level(boss));
            sound(boss, AllSounds.dick("goo"), 2.0f, 0.5f);
        }
    }
}
