package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.CrowleyFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.hit;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Crowley's attacks (v0.35). He is a schemer: every attack is shown on the ground in its windup and lands only in ACTIVE — red
 * for hellfire and his hounds, violet for the telekinetic snap, yellow for his smoke.
 * <ul>
 *   <li>P1, the Hall: {@link Snap} (behind a hunter, a violet shove), {@link PaperStorm} (red cone of contracts),
 *   {@link RedSmoke} (yellow zone: blinds, shuffles his people), {@link CallTheOffice} (suits through the doors),
 *   {@link ReleaseTheHounds} (red rings).</li>
 *   <li>P2, the Cell Wing: the hounds and the smoke above all; the patients come in waves on their own ({@link CrowleyEntity}).</li>
 *   <li>P3, the Throne Room: {@link Hellfire} (red lines across the floor) and {@link Collection} (a hunter with an open
 *   crossroads deal is marked and hunted), and the rest. While he sits, nothing that moves him.</li>
 * </ul>
 */
public final class CrowleyAttacks {

    private CrowleyAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Snap::new, 3), opt(PaperStorm::new, 3), opt(RedSmoke::new, 2), opt(CallTheOffice::new, 1.5f), opt(ReleaseTheHounds::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(ReleaseTheHounds::new, 3), opt(RedSmoke::new, 2.5f), opt(Snap::new, 2), opt(PaperStorm::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Hellfire::new, 3), opt(Collection::new, 2), opt(ReleaseTheHounds::new, 2), opt(PaperStorm::new, 2),
            opt(CallTheOffice::new, 1.5f), opt(Snap::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static CrowleyEntity crowley(LuciferEntity boss) {
        return (CrowleyEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether his blows should land on {@code e}: not him or his people, nobody in creative or spectating. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !CrowleyRetinue.ours(e)
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

    /** An attack whose clip is cued so its moment falls at the end of the windup ({@link CrowleyAssets#HIT_TICKS}). */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, CrowleyBalance.cue(clip), windup, active, recover);
        }
    }

    /** Hounds through red rings at {@code spots}, set on {@code quarry} (visible a while: their paws scorch the floor). */
    public static int callHounds(CrowleyEntity boss, List<Vec3> spots, @Nullable LivingEntity quarry) {
        ServerLevel level = level(boss);
        int room = CrowleyBalance.MAX_HOUNDS - boss.people(HellhoundEntity.class);
        int n = 0;
        for (Vec3 s : spots) {
            for (int i = 0; i < CrowleyBalance.HOUNDS_PER_CALL && n < Math.min(room, boss.minionCount(spots.size() * CrowleyBalance.HOUNDS_PER_CALL)); i++) {
                HellhoundEntity hound = AllEntities.HELLHOUND.get().create(level);
                if (hound == null) continue;
                hound.getPersistentData().putBoolean(CrowleyEntity.CALLED_TAG, true);
                hound.moveTo(s.x + (i - 0.5) * 0.8, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                hound.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                hound.setPersistenceRequired();
                if (quarry instanceof ServerPlayer p) hound.setQuarry(p);
                if (quarry != null) hound.setTarget(quarry);
                hound.reveal(CrowleyBalance.HOUNDS_REVEAL);
                level.addFreshEntity(hound);
                boss.minions().add(hound.getUUID());
                n++;
            }
            boss.fx(new CrowleyFxPayload(boss.getId(), CrowleyFxPayload.HOUNDS, CrowleyBalance.HOUNDS_PER_CALL, 0, s, 40));
            for (LivingEntity e : foes(boss, new AABB(s, s).inflate(CrowleyBalance.HOUND_RING_RADIUS + 1, 3, CrowleyBalance.HOUND_RING_RADIUS + 1))) {
                if (inCircle(e, s, CrowleyBalance.HOUND_RING_RADIUS)) hit(boss, e, AllDamageTypes.HELLFIRE, CrowleyBalance.HOUND_SCORCH_DAMAGE);
            }
        }
        if (n > 0) sound(boss, AllSounds.crowley("hounds"), 2f, 1f);
        return n;
    }

    /** {@code count} of his people (suits, or patients) through the room's doors; @return how many came */
    public static int callPeople(CrowleyEntity boss, boolean suits, int count) {
        ServerLevel level = level(boss);
        int cap = suits ? CrowleyBalance.MAX_SUITS : CrowleyBalance.MAX_PATIENTS;
        int have = boss.people(suits ? CrowleySuitEntity.class : PossessedPatientEntity.class);
        int n = Math.min(boss.minionCount(count), cap - have);
        List<Vec3> spots = boss.minionSpots(boss.spawnPoints());
        for (int i = 0; i < n && !spots.isEmpty(); i++) {
            Vec3 s = spots.get(i % spots.size());
            Mob m = suits ? AllEntities.CROWLEY_SUIT.get().create(level) : AllEntities.POSSESSED_PATIENT.get().create(level);
            if (m == null) continue;
            m.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
            m.setPersistenceRequired();
            LivingEntity t = boss.attackTarget();
            if (t != null) m.setTarget(t);
            level.addFreshEntity(m);
            boss.minions().add(m.getUUID());
            level.sendParticles(AllParticles.DEMON_SMOKE.get(), s.x, s.y + 1, s.z, 20, 0.3, 0.8, 0.3, 0.04);
        }
        return n;
    }

    /** Crossroads demons for the throne room (red eyes, out of smoke). */
    public static void callCrossroads(CrowleyEntity boss, int count) {
        ServerLevel level = level(boss);
        List<Vec3> spots = boss.spawnPoints();
        LivingEntity t = boss.attackTarget();
        for (int i = 0; i < boss.minionCount(count) && !spots.isEmpty(); i++) {
            CrossroadsDemonEntity d = CrossroadsDemonEntity.called(level, spots.get(spots.size() - 1 - i % spots.size()),
                    t instanceof net.minecraft.world.entity.player.Player p ? p : null, CrowleyEntity.CALLED_TAG);
            if (d != null) boss.minions().add(d.getUUID());
        }
    }

    // --- every phase ------------------------------------------------------------------------------------------------------

    /**
     * The snap: he vanishes and stands behind a hunter; a violet line from him through them shows the shove, and whoever is
     * on it at the moment is thrown along it.
     */
    public static class Snap extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Snap() {
            super("snap", "snap", CrowleyBalance.SNAP_WINDUP, 2, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return crowley(boss).seated() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
            Vec3 at = floorAt(boss, behind);
            if (!level(boss).noCollision(boss, boss.getBoundingBox().move(at.subtract(boss.position())))) at = boss.position();
            crowley(boss).blink(at);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, CrowleyBalance.SNAP_WIDTH, CrowleyBalance.SNAP_REACH, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            int shoved = -1;
            for (LivingEntity e : inLane(boss, from, dir, CrowleyBalance.SNAP_REACH, CrowleyBalance.SNAP_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.SPELL, CrowleyBalance.SNAP_DAMAGE);
                e.setDeltaMovement(dir.x * CrowleyBalance.SNAP_PUSH, 0.45, dir.z * CrowleyBalance.SNAP_PUSH);
                e.hurtMarked = true;
                shoved = e.getId();
            }
            crowley(boss).fx(new CrowleyFxPayload(boss.getId(), CrowleyFxPayload.SNAP, shoved, 0, boss.getEyePosition(), 12));
            sound(boss, AllSounds.crowley("snap"), 2f, 1f);
        }
    }

    /** "You're fired": a backhanded wave, a red cone of flying contracts. */
    public static class PaperStorm extends Cued {
        private float yaw;

        public PaperStorm() {
            super("paper_storm", "dismiss", CrowleyBalance.PAPER_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < CrowleyBalance.PAPER_REACH * CrowleyBalance.PAPER_REACH ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, CrowleyBalance.PAPER_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 from = boss.position();
            for (LivingEntity e : inCone(boss, from, yaw, CrowleyBalance.PAPER_REACH)) hit(boss, e, AllDamageTypes.SPELL, CrowleyBalance.PAPER_DAMAGE);
            crowley(boss).fx(new CrowleyFxPayload(boss.getId(), CrowleyFxPayload.PAPER_STORM, Math.round(yaw),
                    Math.round(CrowleyBalance.PAPER_REACH), from, 24));
            sound(boss, AllSounds.crowley("paper_storm"), 2f, 1f);
        }
    }

    /** Red smoke: a yellow zone on a hunter; inside it they are blinded, and his people are shuffled about the room. */
    public static class RedSmoke extends Cued {
        private Vec3 at = Vec3.ZERO;

        public RedSmoke() {
            super("red_smoke", "snap", CrowleyBalance.SMOKE_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, CrowleyBalance.SMOKE_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            float r = CrowleyBalance.SMOKE_RADIUS;
            for (LivingEntity e : foes(boss, new AABB(at, at).inflate(r + 1, 3, r + 1))) {
                if (!inCircle(e, at, r)) continue;
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CrowleyBalance.SMOKE_BLIND, 0));
            }
            List<Vec3> spots = crowley(boss).spawnPoints();
            int i = 0;
            for (Entity m : crowley(boss).people()) {
                if (spots.isEmpty() || !(m instanceof CrowleySuitEntity || m instanceof PossessedPatientEntity)) continue;
                Vec3 s = spots.get((i++ + boss.getRandom().nextInt(spots.size())) % spots.size());
                m.teleportTo(s.x, s.y, s.z);
            }
            crowley(boss).fx(new CrowleyFxPayload(boss.getId(), CrowleyFxPayload.RED_SMOKE, Math.round(r * 10), 0, at, CrowleyBalance.SMOKE_TICKS));
            sound(boss, AllSounds.crowley("smoke_out"), 2f, 0.8f);
        }
    }

    /** "Call the office": red circles at the room's doors, suits come through. */
    public static class CallTheOffice extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public CallTheOffice() {
            super("call_office", "snap", CrowleyBalance.CALL_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return crowley(boss).people(CrowleySuitEntity.class) < CrowleyBalance.MAX_SUITS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spots.clear();
            for (Vec3 s : crowley(boss).spawnPoints()) {
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, 1.2f, TelegraphMarker.RED, windupLength(boss) + 4);
                if (spots.size() >= CrowleyBalance.SUITS_PER_CALL) break;
            }
            sound(boss, AllSounds.crowley("laugh"), 1.5f, 1f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            callPeople(crowley(boss), true, CrowleyBalance.SUITS_PER_CALL);
        }
    }

    /** "Release the hounds": red rings by the hunters; whoever stands in one is scorched as the hounds come through. */
    public static class ReleaseTheHounds extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public ReleaseTheHounds() {
            super("hounds", "point", CrowleyBalance.HOUNDS_WINDUP, 2, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return crowley(boss).people(HellhoundEntity.class) < CrowleyBalance.MAX_HOUNDS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spots.clear();
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            for (int i = 0; i < 2; i++) {
                Vec3 s = floorAt(boss, target.position().add(Math.cos(a + i * Math.PI) * 3, 0, Math.sin(a + i * Math.PI) * 3));
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, CrowleyBalance.HOUND_RING_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            callHounds(crowley(boss), spots, target);
        }
    }

    // --- P3 · the Throne Room --------------------------------------------------------------------------------------------

    /** Hellfire: red lines across the floor, through the hunters; they burn a while. */
    public static class Hellfire extends Cued {
        private final List<Vec3> starts = new ArrayList<>();
        private final List<Float> yaws = new ArrayList<>();

        public Hellfire() {
            super("hellfire", "point", CrowleyBalance.FIRE_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            starts.clear();
            yaws.clear();
            List<LivingEntity> marks = new ArrayList<>(boss.challengers());
            if (!marks.contains(target)) marks.add(target);
            marks.sort(Comparator.comparingDouble(e -> e.distanceToSqr(boss)));
            float half = CrowleyBalance.FIRE_LENGTH / 2;
            for (int i = 0; i < CrowleyBalance.FIRE_LINES; i++) {
                LivingEntity through = marks.get(i % marks.size());
                float yaw = boss.getRandom().nextFloat() * 360;
                Vec3 start = floorAt(boss, through.position().subtract(dirOf(yaw).scale(half)));
                starts.add(start);
                yaws.add(yaw);
                TelegraphMarker.line(level(boss), start, yaw, CrowleyBalance.FIRE_WIDTH, CrowleyBalance.FIRE_LENGTH, TelegraphMarker.RED, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < starts.size(); i++) {
                Vec3 dir = dirOf(yaws.get(i));
                for (LivingEntity e : inLane(boss, starts.get(i), dir, CrowleyBalance.FIRE_LENGTH, CrowleyBalance.FIRE_WIDTH / 2)) {
                    hit(boss, e, AllDamageTypes.HELLFIRE, CrowleyBalance.FIRE_DAMAGE);
                    e.igniteForSeconds(3);
                }
                Vec3 d = dir.scale(CrowleyBalance.FIRE_LENGTH);
                crowley(boss).fx(new CrowleyFxPayload(boss.getId(), CrowleyFxPayload.HELLFIRE_LINE, (int) Math.round(d.x * 10),
                        (int) Math.round(d.z * 10), starts.get(i), CrowleyBalance.FIRE_TICKS));
            }
            sound(boss, AllSounds.crowley("hellfire"), 2f, 1f);
        }
    }

    /**
     * Collection: a hunter with an open crossroads deal is marked (a red ring follows them); the hounds about him go for them,
     * and two more come through. Never offered when nobody owes anything.
     */
    public static class Collection extends Cued {
        private @Nullable ServerPlayer debtor;

        public Collection() {
            super("collection", "point", CrowleyBalance.COLLECT_WINDUP, 2, 16);
        }

        static @Nullable ServerPlayer debtor(LuciferEntity boss) {
            for (ServerPlayer p : boss.challengers()) if (Debts.get(p).active()) return p;
            return null;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return debtor(boss) != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            debtor = debtor(boss);
            if (debtor == null) return;
            face(boss, debtor.position());
            TelegraphMarker.ring(level(boss), floorAt(boss, debtor.position()), 1.6f, TelegraphMarker.RED, windupLength(boss) + 4);
            debtor.displayClientMessage(Component.translatable("message.supernaturalcraft.crowley.collection")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (debtor == null || !debtor.isAlive()) return;
            crowley(boss).fx(new CrowleyFxPayload(boss.getId(), CrowleyFxPayload.COLLECT, debtor.getId(), 0, debtor.position(), CrowleyBalance.COLLECT_TICKS));
            crowley(boss).markCollected(debtor, CrowleyBalance.COLLECT_TICKS);
            Vec3 a = floorAt(boss, debtor.position().add(4, 0, 0)), b = floorAt(boss, debtor.position().add(-4, 0, 0));
            callHounds(crowley(boss), List.of(a, b), debtor);
        }
    }
}
