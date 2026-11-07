package org.papiricoh.supernaturalcraft.entity.boss.uriel;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocalypse.seal.RebelAngelEntity;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.UrielFxPayload;
import org.papiricoh.supernaturalcraft.pact.StoryNpcEntity;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;

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
 * Uriel's attacks (v0.40). Every one is shown on the ground in gold (holy) in its windup and lands only in ACTIVE.
 * <ul>
 *   <li>I, the Soldier: {@link Slash} (a short gold cone ahead), {@link Thrust} (a gold lane, a lunge), {@link Palm} (a gold circle
 *   where the ground cracks: it slows), {@link Smite} (a long gold cone), {@link Command} (his rebels come to his side; gold
 *   circles where they land).</li>
 *   <li>II, the Traitor, alone, wings out: {@link Flash} (a gold ring round him: blinding grace), and the rest harder
 *   ({@link UrielBalance#TRAITOR_DAMAGE}), the smite reaching farther.</li>
 * </ul>
 */
public final class UrielAttacks {

    private UrielAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(Slash::new, 3), opt(Thrust::new, 2.5f), opt(Palm::new, 2.5f), opt(Smite::new, 2), opt(Command::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(Slash::new, 2.5f), opt(Thrust::new, 2), opt(Palm::new, 2), opt(Smite::new, 2.5f), opt(Flash::new, 3));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return phase <= 1 ? P1 : P2;
    }

    static UrielEntity uriel(LuciferEntity boss) {
        return (UrielEntity) boss;
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    /** Whether his blows land on {@code e}: not him nor his rebels, nobody creative or spectating, no story NPC. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof StoryNpcEntity) && !(e instanceof RebelAngelEntity)
                && !boss.minions().contains(e.getUUID()) && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
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

    /** Everyone in a 120° cone {@code reach} deep (the shape of {@code TelegraphMarker.cone}). */
    static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(60));
        return foes(boss, new AABB(from, from).inflate(reach, 4, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 3.5 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

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

    static float dmg(LuciferEntity boss, float base) {
        return base * UrielBalance.phaseDamage(boss.phase());
    }

    /** An attack whose clip is cued so its moment falls at the end of the windup. */
    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, UrielBalance.cue(clip), windup, active, recover);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return uriel(boss).finishing() ? 0 : 1;
        }
    }

    // --- the attacks ---------------------------------------------------------------------------------------------------------

    /** A heavy cut of the blade: a short gold cone ahead. */
    public static class Slash extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Slash() {
            super("slash", "slash", UrielBalance.SLASH_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.cone(level(boss), from, yaw, UrielBalance.SLASH_REACH, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            uriel(boss).sound("slash", 1.6f, 1f);
            uriel(boss).fx(UrielFxPayload.SLASH, Math.round(yaw), 0, from.add(dirOf(yaw).scale(1.5)), 10);
            for (LivingEntity e : inCone(boss, from, yaw, UrielBalance.SLASH_REACH)) hit(boss, e, AllDamageTypes.SMITE, dmg(boss, UrielBalance.SLASH_DAMAGE));
        }
    }

    /** A lunge with the blade: a gold lane ahead; he crosses it. */
    public static class Thrust extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Thrust() {
            super("thrust", "thrust", UrielBalance.THRUST_WINDUP, 3, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, UrielBalance.THRUST_WIDTH, UrielBalance.THRUST_REACH, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            uriel(boss).sound("slash", 1.6f, 1.2f);
            for (LivingEntity e : inLane(boss, from, dir, UrielBalance.THRUST_REACH, UrielBalance.THRUST_WIDTH / 2)) {
                hit(boss, e, AllDamageTypes.SMITE, dmg(boss, UrielBalance.THRUST_DAMAGE));
                push(e, dir, 0.8, 0.3);
            }
            Vec3 to = floorAt(boss, from.add(dir.scale(UrielBalance.THRUST_REACH - 1)));
            boss.teleportTo(to.x, to.y, to.z);
        }
    }

    /** A palm slammed into the ground under the target: a gold circle; the ground cracks and slows. */
    public static class Palm extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Palm() {
            super("palm", "palm", UrielBalance.PALM_WINDUP, 2, 18);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, UrielBalance.PALM_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            uriel(boss).sound("palm", 2f, 0.9f);
            uriel(boss).fx(UrielFxPayload.PALM, Math.round(UrielBalance.PALM_RADIUS * 10), 0, at, 30);
            uriel(boss).crack(at, UrielBalance.PALM_RADIUS);
            for (LivingEntity e : inCircleOf(boss, at, UrielBalance.PALM_RADIUS)) {
                hit(boss, e, AllDamageTypes.SMITE, dmg(boss, UrielBalance.PALM_DAMAGE));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, UrielBalance.PALM_SLOW, 2));
            }
        }
    }

    /** The smite: a hand thrust forward, fingers spread; a long gold cone, light down it. */
    public static class Smite extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Smite() {
            super("smite", "smite", UrielBalance.SMITE_WINDUP, 2, 20);
        }

        float reach(LuciferEntity boss) {
            return boss.phase() >= 2 ? UrielBalance.TRAITOR_SMITE_REACH : UrielBalance.SMITE_REACH;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.cone(level(boss), from, yaw, reach(boss), TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            uriel(boss).sound("smite", 2f, 1f);
            uriel(boss).fx(UrielFxPayload.SMITE, Math.round(yaw), Math.round(reach(boss) * 10), from, 20);
            for (LivingEntity e : inCone(boss, from, yaw, reach(boss))) {
                hit(boss, e, AllDamageTypes.SMITE, dmg(boss, UrielBalance.SMITE_DAMAGE));
                push(e, dirOf(yaw), 1.0, 0.4);
            }
        }
    }

    /** He points: his rebels come down to his side (gold circles where they land), up to his cap. */
    public static class Command extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Command() {
            super("command", "command", UrielBalance.COMMAND_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return super.weight(boss, target) > 0 && uriel(boss).rebels() < boss.minionCount(UrielBalance.ESCORT_CAP) ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
            int room = boss.minionCount(UrielBalance.ESCORT_CAP) - uriel(boss).rebels();
            List<Vec3> all = uriel(boss).spawnPoints();
            for (int i = 0; i < Math.min(room, Math.min(2, all.size())); i++) spots.add(all.get(boss.getRandom().nextInt(all.size())));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.5f, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            uriel(boss).sound("wings", 1.8f, 1.1f);
            for (Vec3 s : spots) uriel(boss).raiseRebel(s);
        }
    }

    /** Blinding grace: arms flung wide, a gold ring round him; inside it at the moment, a flash that burns and blinds. */
    public static class Flash extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Flash() {
            super("flash", "flash", UrielBalance.FLASH_WINDUP, 2, 22);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            at = boss.position();
            TelegraphMarker.circle(level(boss), at, UrielBalance.FLASH_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
            super.onWindup(boss, target);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            uriel(boss).sound("flash", 2.5f, 1f);
            uriel(boss).fx(UrielFxPayload.FLASH, Math.round(UrielBalance.FLASH_RADIUS * 10), 0, at, 30);
            for (LivingEntity e : inCircleOf(boss, at, UrielBalance.FLASH_RADIUS)) {
                hit(boss, e, AllDamageTypes.GRACE, dmg(boss, UrielBalance.FLASH_DAMAGE));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, UrielBalance.FLASH_BLIND, 0));
            }
        }
    }
}
