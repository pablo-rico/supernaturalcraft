package org.papiricoh.supernaturalcraft.entity.boss.rowena;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.witch.FamiliarEntity;
import org.papiricoh.supernaturalcraft.entity.witch.HexedVillagerEntity;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.network.RowenaFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
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
 * Rowena's attacks (v0.21, P6). Every one is shown on the ground in its windup and lands only in ACTIVE: violet for telekinesis
 * (the fling's lane), red for fire (Ignis; the whole arena before the recitation's hex), blue for ice (Glacies), yellow for her
 * smoke (where she will appear).
 * <ul>
 *   <li>P1, the Witch at Home: {@link Fling}, {@link Ignis}, {@link Glacies}.</li>
 *   <li>P2, the Familiar and the Hexed: {@link CallFamiliar} (forced while she has none), {@link SummonVictims},
 *   {@link SmokeBlink}, and the P1 spells.</li>
 *   <li>P3, the Recitation: {@link Recitation} (forced when its time comes), and the rest from the air.</li>
 * </ul>
 */
public final class RowenaAttacks {

    private RowenaAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Fling::new, 3), opt(Ignis::new, 3), opt(Glacies::new, 3));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(SummonVictims::new, 2.5f), opt(Ignis::new, 2.5f), opt(Glacies::new, 2.5f), opt(Fling::new, 2), opt(SmokeBlink::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(Ignis::new, 3), opt(Glacies::new, 3), opt(Fling::new, 2), opt(SmokeBlink::new, 2), opt(SummonVictims::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static RowenaEntity rowena(LuciferEntity boss) {
        return (RowenaEntity) boss;
    }

    // --- helpers ---------------------------------------------------------------------------------------------------------

    /** Whether her spells should land on {@code e}: not herself, her familiar or her victims, nobody in creative or spectating. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof RowenaEntity) && !(e instanceof FamiliarEntity)
                && !(e instanceof HexedVillagerEntity) && !boss.minions().contains(e.getUUID())
                && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    /** Everyone her spells may find in {@code box}, her tracked hunters included. */
    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        List<LivingEntity> out = new ArrayList<>(level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e)));
        for (LivingEntity h : rowena(boss).hunters()) if (!out.contains(h) && isFoe(boss, h) && box.contains(h.position())) out.add(h);
        return out;
    }

    static float face(LuciferEntity boss, Vec3 at) {
        float yaw = yawTo(boss.position(), at);
        boss.setYRot(yaw);
        boss.setYBodyRot(yaw);
        boss.setYHeadRot(yaw);
        return yaw;
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} along {@code dir}. */
    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, Vec3 dir, double length, double halfWidth) {
        return foes(boss, new AABB(from, from).inflate(length + 1, 6, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    static List<LivingEntity> inside(LuciferEntity boss, Vec3 at, float radius) {
        return foes(boss, new AABB(at, at).inflate(radius + 1, 4, radius + 1)).stream().filter(e -> inCircle(e, at, radius)).toList();
    }

    /** An attack whose clip is cued so its moment falls at the end of the windup ({@link RowenaAssets#HIT_TICKS}). */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, RowenaBalance.cue(clip), windup, active, recover);
        }
    }

    // --- P1 · the Witch at Home ------------------------------------------------------------------------------------------

    /** A flick of the wrist: a violet lane before her; whoever is in it is struck and thrown. */
    public static class Fling extends Cued {
        private Vec3 from = Vec3.ZERO;
        private float yaw;

        public Fling() {
            super("fling", "fling", RowenaBalance.FLING_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            from = floorAt(boss, boss.position());
            yaw = face(boss, target.position());
            TelegraphMarker.line(level(boss), from, yaw, RowenaBalance.FLING_WIDTH, RowenaBalance.FLING_LENGTH, TelegraphMarker.VIOLET,
                    windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : inLane(boss, from, dir, RowenaBalance.FLING_LENGTH, RowenaBalance.FLING_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.SPELL, RowenaBalance.FLING_DAMAGE);
                e.push(dir.x * RowenaBalance.FLING_PUSH, 0.6, dir.z * RowenaBalance.FLING_PUSH);
                e.hurtMarked = true;
            }
            rowena(boss).fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.FLING, Math.round(yaw), Math.round(RowenaBalance.FLING_LENGTH), from, 14));
            sound(boss, AllSounds.coven("rowena.fling"), 1.6f, 1f);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }
    }

    /** A palm thrust: a red circle at the hunter, then fire. */
    public static class Ignis extends Cued {
        private Vec3 spot = Vec3.ZERO;

        public Ignis() {
            super("ignis", "ignis", RowenaBalance.IGNIS_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, RowenaBalance.IGNIS_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inside(boss, spot, RowenaBalance.IGNIS_RADIUS)) {
                hit(boss, e, AllDamageTypes.HELLFIRE, RowenaBalance.IGNIS_DAMAGE);
                e.igniteForSeconds(RowenaBalance.IGNIS_FIRE_SECONDS);
            }
            level(boss).sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 0.3, spot.z, 60, RowenaBalance.IGNIS_RADIUS / 2, 0.4,
                    RowenaBalance.IGNIS_RADIUS / 2, 0.05);
            rowena(boss).fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.IGNIS, Math.round(RowenaBalance.IGNIS_RADIUS * 10), 0, spot, 30));
            sound(boss, AllSounds.coven("rowena.ignis"), 1.6f, 1f);
        }
    }

    /** Both hands pushed down: a blue circle at the hunter, then frost that slows. */
    public static class Glacies extends Cued {
        private Vec3 spot = Vec3.ZERO;

        public Glacies() {
            super("glacies", "glacies", RowenaBalance.GLACIES_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, RowenaBalance.GLACIES_RADIUS, TelegraphMarker.BLUE, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inside(boss, spot, RowenaBalance.GLACIES_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, RowenaBalance.GLACIES_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RowenaBalance.GLACIES_SLOW_TICKS, 3), boss);
                e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 40));
            }
            level(boss).sendParticles(ParticleTypes.SNOWFLAKE, spot.x, spot.y + 0.3, spot.z, 60, RowenaBalance.GLACIES_RADIUS / 2, 0.4,
                    RowenaBalance.GLACIES_RADIUS / 2, 0.05);
            rowena(boss).fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.GLACIES, Math.round(RowenaBalance.GLACIES_RADIUS * 10), 0, spot, 40));
            sound(boss, AllSounds.coven("rowena.glacies"), 1.6f, 1f);
        }
    }

    // --- P2 · the Familiar and the Hexed ---------------------------------------------------------------------------------

    /** Arms spread: a raven answers her, bound to her; it goes for a taglock. */
    public static class CallFamiliar extends Cued {
        public CallFamiliar() {
            super("call_familiar", "summon_victims", RowenaBalance.FAMILIAR_WINDUP, 2, 10);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return rowena(boss).mayCallFamiliar() ? 1 : 0;
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            if (rowena(boss).mayCallFamiliar()) rowena(boss).callFamiliar();
            sound(boss, AllSounds.coven("rowena.summon"), 1.4f, 1.2f);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }
    }

    /** Arms spread wide: hexed victims rise round her (each heals her if it is killed; free them instead). */
    public static class SummonVictims extends Cued {
        public SummonVictims() {
            super("summon_victims", "summon_victims", RowenaBalance.VICTIMS_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return rowena(boss).victims().size() < RowenaBalance.MAX_VICTIMS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            level(boss).sendParticles(ParticleTypes.WITCH, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 2, 0.3, 2, 0.05);
            sound(boss, AllSounds.coven("rowena.summon"), 1.5f, 1f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            rowena(boss).summonVictims(RowenaBalance.VICTIMS_PER_CALL);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }
    }

    /** A turn into yellow smoke: a yellow circle where she will appear (behind the hunter); whoever stands in it is struck. */
    public static class SmokeBlink extends Cued {
        private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;

        public SmokeBlink() {
            super("smoke_blink", "teleport", RowenaBalance.SMOKE_WINDUP, 2, 12);
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
            TelegraphMarker.circle(level(boss), to, RowenaBalance.SMOKE_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
            rowena(boss).fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.SMOKE, 0, 0, to, windupLength(boss)));
            sound(boss, AllSounds.coven("rowena.teleport"), 1.2f, 0.9f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Vec3 land = boss.isAerialPhase() ? to.add(0, RowenaBalance.SMOKE_LIFT, 0) : to;
            MagicFx.teleport(level, boss, School.WITCHCRAFT, from, land, MagicFxPayload.SMOKE_YELLOW, 30);
            boss.teleportTo(land.x, land.y, land.z);
            face(boss, target.position());
            for (LivingEntity e : inside(boss, to, RowenaBalance.SMOKE_RADIUS)) {
                hit(boss, e, AllDamageTypes.SPELL, RowenaBalance.SMOKE_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, RowenaBalance.SMOKE_BLIND, 0), boss);
            }
            sound(boss, AllSounds.coven("rowena.teleport"), 1.5f, 1.1f);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }
    }

    // --- P3 · the Recitation ---------------------------------------------------------------------------------------------

    /**
     * She floats over three bowls and recites ({@link RowenaBalance#RECITE_TICKS}); the letters climb with her progress. Break
     * all three bowls and she falls, stunned; otherwise, after a red warning over the whole arena for its last
     * {@link RowenaBalance#WIPE_WARNING} ticks, the hex sweeps it for {@link RowenaBalance#wipeDamage} per bowl still whole.
     */
    public static class Recitation extends BossAttack<LuciferEntity> {
        private boolean broken, spoken, warned;
        private int whole = RowenaBalance.BOWLS;

        public Recitation() {
            super("recitation", "-", 20, RowenaBalance.RECITE_TICKS, 24);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return rowena(boss).mayRecite() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            rowena(boss).beginRecitation();
            sound(boss, AllSounds.coven("rowena.recite"), 2f, 1f);
            rowena(boss).tell(level(boss), "message.supernaturalcraft.rowena.recitation");
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            RowenaEntity r = rowena(boss);
            whole = r.bowls().size();
            if (whole == 0) {
                if (!broken) {
                    broken = true;
                    r.recitationBroken();
                    r.fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.RECITE_END, 0, 0, centre(boss), 30));
                }
                return;
            }
            if (t % RowenaBalance.PROGRESS_EVERY == 0) {
                r.fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.RECITE, RowenaBalance.progress(t, RowenaBalance.RECITE_TICKS), whole,
                        boss.position(), RowenaBalance.PROGRESS_EVERY));
            }
            if (t % 60 == 0) sound(boss, AllSounds.coven("rowena.recite"), 1.6f, 1f + t / (float) RowenaBalance.RECITE_TICKS * 0.3f);
            if (!warned && t >= RowenaBalance.RECITE_TICKS - RowenaBalance.WIPE_WARNING) {
                warned = true;
                ArenaController arena = boss.arena();
                float radius = arena != null ? arena.radius() : 12;
                TelegraphMarker.circle(level(boss), floorAt(boss, centre(boss)), radius, TelegraphMarker.RED, RowenaBalance.WIPE_WARNING + 4);
            }
            if (t >= RowenaBalance.RECITE_TICKS - 1 && !spoken) {
                spoken = true;
                wipe(boss);
            }
        }

        static Vec3 centre(LuciferEntity boss) {
            ArenaController arena = boss.arena();
            return arena != null ? arena.centerVec() : boss.position();
        }

        private void wipe(LuciferEntity boss) {
            float damage = RowenaBalance.wipeDamage(whole);
            Vec3 c = centre(boss);
            ArenaController arena = boss.arena();
            double r = arena != null ? arena.radius() : 12;
            for (LivingEntity e : foes(boss, new AABB(c, c).inflate(r + 1, 12, r + 1))) {
                hit(boss, e, AllDamageTypes.SPELL, damage);
                MagicFx.hex(level(boss), e, e.position().add(0, 1, 0), MagicFxPayload.HEX_CURSE, 1.2f, 20);
            }
            rowena(boss).fx(new RowenaFxPayload(boss.getId(), RowenaFxPayload.RECITE_END, 1, Math.round(damage), c, 30));
            sound(boss, AllSounds.coven("rowena.laugh"), 3f, 0.9f);
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return broken;
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            rowena(boss).endRecitation();
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        /** Tests: whether the bowls were all broken, whether the hex was spoken. */
        public boolean broken() {
            return broken;
        }

        public boolean spoken() {
            return spoken;
        }
    }
}
