package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

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
 * AU Michael's attacks (v0.41), numbers in {@link AuMichaelBalance}. Each is shown on the ground in its windup and lands only in
 * ACTIVE: red for his blade, gold for the holy fire and where his soldiers come in, white for the gale, violet for the lightning.
 * <ul>
 *   <li>P1: {@link Slash} (a red cone), {@link Combo} (three red cones, a step each), {@link Smite} (a gold circle under a hunter).</li>
 *   <li>P2: the blade, {@link Gale} (a wide white cone: thrown far), {@link Command} (gold rings where his soldiers step in).</li>
 *   <li>P3: {@link Bolts} (a violet circle under every hunter, and strays), {@link Lance} (violet circles walked down a lane), the
 *   smite and the blade.</li>
 *   <li>P4, in the air: {@link HolyRain} (gold circles about the hunters), the bolts and the gale.</li>
 * </ul>
 * Forced: {@link WingStep} when a hunter runs or hides (a flash of wings and he stands behind them).
 */
public final class AuMichaelAttacks {

    private AuMichaelAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(Slash::new, 3), opt(Combo::new, 2), opt(Smite::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Slash::new, 2), opt(Combo::new, 1.5f), opt(Smite::new, 1.5f),
            opt(Gale::new, 2.5f), opt(Command::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Bolts::new, 3), opt(Lance::new, 2.5f), opt(Smite::new, 1.5f),
            opt(Slash::new, 1.5f), opt(Gale::new, 1), opt(Command::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(opt(HolyRain::new, 3), opt(Bolts::new, 2.5f), opt(Gale::new, 1.5f),
            opt(Lance::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    static AuMichaelEntity michael(LuciferEntity boss) {
        return (AuMichaelEntity) boss;
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof AuSoldierEntity) && !boss.minions().contains(e.getUUID())
                && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
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

    /** Foes in a cone of {@code halfAngle} degrees either side of {@code yaw}, out to {@code reach}. */
    static List<LivingEntity> inCone(LuciferEntity boss, Vec3 from, float yaw, double reach, double halfAngle) {
        Vec3 dir = dirOf(yaw);
        double cos = Math.cos(Math.toRadians(halfAngle));
        return foes(boss, new AABB(from, from).inflate(reach, 5, reach)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double d = to.length();
            return d <= reach && Math.abs(e.getY() - from.y) <= 4 && (d < 0.8 || to.normalize().dot(dir) >= cos);
        }).toList();
    }

    static List<LivingEntity> inRing(LuciferEntity boss, Vec3 at, float r) {
        return foes(boss, new AABB(at, at).inflate(r + 1, 4, r + 1)).stream().filter(e -> inCircle(e, at, r)).toList();
    }

    /** A visual bolt of the burned sky at {@code at}, and whoever stands in its circle is struck. */
    static void bolt(LuciferEntity boss, Vec3 at, float radius, float damage) {
        michael(boss).bolt(at);
        for (LivingEntity e : inRing(boss, at, radius)) hit(boss, e, DamageTypes.LIGHTNING_BOLT, damage);
    }

    /** The hunters a phase aims at (every challenger, the target first). */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>();
        out.add(target);
        for (ServerPlayer p : boss.challengers()) if (p != target) out.add(p);
        return out;
    }

    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, AuMichaelBalance.cue(clip), windup, active, recover);
        }
    }

    // --- P1, the general -----------------------------------------------------------------------------------------------------

    /** A heavy cut of his archangel blade: a red cone before him. */
    public static class Slash extends Cued {
        private float yaw;

        public Slash() {
            super("slash", "blade_slash", AuMichaelBalance.SLASH_WINDUP, 2, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 49 ? 1.5f : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, AuMichaelBalance.SLASH_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inCone(boss, boss.position(), yaw, AuMichaelBalance.SLASH_REACH, 60)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, AuMichaelBalance.SLASH_DAMAGE);
            }
            michael(boss).fx(AuMichaelFxPayload.SLASH, Math.round(yaw), Math.round(AuMichaelBalance.SLASH_REACH * 10), boss.position(), 8);
            sound(boss, ApocWorldRegistry.michael("slash"), 1.6f, 1f);
        }
    }

    /** Three cuts, a step forward each, a red cone shown for each before it falls. */
    public static class Combo extends Cued {
        private float yaw;
        private int cut;

        public Combo() {
            super("combo", "blade_combo", AuMichaelBalance.COMBO_WINDUP, 3 * AuMichaelBalance.COMBO_GAP, 12);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 64 ? 1.2f : 0.3f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            cut = 0;
            showCut(boss, 0, windupLength(boss) + 4);
        }

        private Vec3 cutFrom(LuciferEntity boss, int n) {
            return boss.position().add(dirOf(yaw).scale(AuMichaelBalance.COMBO_STEP * n));
        }

        private void showCut(LuciferEntity boss, int n, int life) {
            TelegraphMarker.cone(level(boss), floorAt(boss, cutFrom(boss, n)), yaw, AuMichaelBalance.COMBO_REACH, TelegraphMarker.RED, life);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            land(boss);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t > 0 && t % AuMichaelBalance.COMBO_GAP == 0 && cut < 3) land(boss);
        }

        private void land(LuciferEntity boss) {
            Vec3 from = floorAt(boss, cutFrom(boss, cut));
            for (LivingEntity e : inCone(boss, from, yaw, AuMichaelBalance.COMBO_REACH, 55)) {
                hit(boss, e, DamageTypes.MOB_ATTACK, AuMichaelBalance.COMBO_DAMAGE);
            }
            michael(boss).fx(AuMichaelFxPayload.SLASH, Math.round(yaw), Math.round(AuMichaelBalance.COMBO_REACH * 10), from, 6);
            sound(boss, ApocWorldRegistry.michael("slash"), 1.4f, 1.1f + 0.1f * cut);
            cut++;
            if (cut < 3) showCut(boss, cut, AuMichaelBalance.COMBO_GAP + 2);
        }
    }

    /** A raised palm: a gold circle under a hunter, holy fire when it closes. */
    public static class Smite extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Smite() {
            super("smite", "smite", AuMichaelBalance.SMITE_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, AuMichaelBalance.SMITE_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, AuMichaelBalance.SMITE_RADIUS)) {
                hit(boss, e, AllDamageTypes.SMITE, AuMichaelBalance.SMITE_DAMAGE);
                e.igniteForTicks(AuMichaelBalance.SMITE_FIRE);
            }
            michael(boss).fx(AuMichaelFxPayload.SMITE, Math.round(AuMichaelBalance.SMITE_RADIUS * 10), 0, at, 20);
            level(boss).sendParticles(AllParticles.GRACE.get(), at.x, at.y + 0.5, at.z, 40, 1.2, 1.2, 1.2, 0.05);
            sound(boss, ApocWorldRegistry.michael("smite"), 2f, 1f);
        }
    }

    // --- P2, the wings -------------------------------------------------------------------------------------------------------

    /** His wings beaten forward: a wide white cone; whoever is in it is hurt a little and thrown far. */
    public static class Gale extends Cued {
        private float yaw;

        public Gale() {
            super("gale", "wing_beat", AuMichaelBalance.GALE_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), floorAt(boss, boss.position()), yaw, AuMichaelBalance.GALE_REACH, TelegraphMarker.WHITE, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 from = floorAt(boss, boss.position());
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : inCone(boss, from, yaw, AuMichaelBalance.GALE_REACH, 70)) {
                hit(boss, e, AllDamageTypes.SPELL, AuMichaelBalance.GALE_DAMAGE);
                Vec3 away = e.position().subtract(from).multiply(1, 0, 1);
                away = away.lengthSqr() < 0.01 ? dir : away.normalize();
                e.push(away.x * AuMichaelBalance.GALE_SHOVE, 0.6, away.z * AuMichaelBalance.GALE_SHOVE);
                e.hurtMarked = true;
            }
            michael(boss).fx(AuMichaelFxPayload.GALE, Math.round(yaw), Math.round(AuMichaelBalance.GALE_REACH), from, 16);
            sound(boss, ApocWorldRegistry.michael("gale"), 2.5f, 1f);
        }
    }

    /** A pointing order: gold rings where his soldiers step in, and they come. */
    public static class Command extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Command() {
            super("command", "command", AuMichaelBalance.COMMAND_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return michael(boss).soldiers() < AuMichaelBalance.MAX_SOLDIERS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spots.clear();
            for (Vec3 s : boss.minionSpots(michael(boss).spawnPoints())) {
                if (spots.size() >= boss.minionCount(AuMichaelBalance.SOLDIERS_PER_CALL)) break;
                spots.add(s);
                TelegraphMarker.ring(level(boss), s, 1.2f, TelegraphMarker.GOLD, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int room = AuMichaelBalance.MAX_SOLDIERS - michael(boss).soldiers();
            for (Vec3 s : spots) {
                if (room-- <= 0) break;
                AuSoldierEntity m = ApocWorldRegistry.AU_ANGEL_SOLDIER.get().create(level);
                if (m == null) continue;
                m.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                m.setPersistenceRequired();
                m.setTarget(target);
                level.addFreshEntity(m);
                boss.minions().add(m.getUUID());
                level.sendParticles(AllParticles.GRACE.get(), s.x, s.y + 1, s.z, 24, 0.3, 0.9, 0.3, 0.05);
                level.sendParticles(ParticleTypes.FLASH, s.x, s.y + 1, s.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // --- P3, the burned sky --------------------------------------------------------------------------------------------------

    /** The sky answers him: a violet circle under every hunter (and strays), a bolt in each. */
    public static class Bolts extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Bolts() {
            super("bolts", "lightning", AuMichaelBalance.BOLT_WINDUP, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            spots.clear();
            for (LivingEntity e : marks(boss, target)) if (spots.size() < 4) spots.add(floorAt(boss, e.position()));
            for (int i = 0; i < AuMichaelBalance.STRAY_BOLTS; i++) spots.add(randomArenaPoint(boss, 0.7));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, AuMichaelBalance.BOLT_RADIUS, TelegraphMarker.VIOLET, windupLength(boss) + 4);
            sound(boss, ApocWorldRegistry.michael("thunder"), 1.2f, 1.4f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : spots) bolt(boss, s, AuMichaelBalance.BOLT_RADIUS, AuMichaelBalance.BOLT_DAMAGE);
            sound(boss, ApocWorldRegistry.michael("thunder"), 3f, 0.9f);
        }
    }

    /** Bolts walked down a lane toward a hunter: violet circles along it, struck one after another. */
    public static class Lance extends Cued {
        private final List<Vec3> spots = new ArrayList<>();
        private int struck;

        public Lance() {
            super("lance", "lightning", AuMichaelBalance.LANCE_WINDUP, AuMichaelBalance.LANCE_BOLTS * 3, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            float yaw = face(boss, target.position());
            Vec3 dir = dirOf(yaw);
            spots.clear();
            struck = 0;
            for (int i = 1; i <= AuMichaelBalance.LANCE_BOLTS; i++) {
                Vec3 s = floorAt(boss, boss.position().add(dir.scale(AuMichaelBalance.LANCE_LENGTH * i / AuMichaelBalance.LANCE_BOLTS)));
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, AuMichaelBalance.LANCE_WIDTH / 2 + 0.6f, TelegraphMarker.VIOLET, windupLength(boss) + 3 * i + 4);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 == 0 && struck < spots.size()) {
                bolt(boss, spots.get(struck++), AuMichaelBalance.LANCE_WIDTH / 2 + 0.6f, AuMichaelBalance.LANCE_DAMAGE);
                sound(boss, ApocWorldRegistry.michael("thunder"), 2f, 1.2f);
            }
        }
    }

    // --- P4, the archangel ---------------------------------------------------------------------------------------------------

    /** Holy fire rained from the air: gold circles about the hunters, each a burst of it. */
    public static class HolyRain extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public HolyRain() {
            super("holy_rain", "smite", AuMichaelBalance.RAIN_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spots.clear();
            List<LivingEntity> marks = marks(boss, target);
            for (int i = 0; i < AuMichaelBalance.RAIN_CIRCLES; i++) {
                LivingEntity m = marks.get(i % marks.size());
                double a = boss.getRandom().nextDouble() * Math.PI * 2, d = i < marks.size() ? 0 : 2 + boss.getRandom().nextDouble() * 4;
                spots.add(floorAt(boss, m.position().add(Math.cos(a) * d, 0, Math.sin(a) * d)));
            }
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, AuMichaelBalance.RAIN_RADIUS, TelegraphMarker.GOLD, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (Vec3 s : spots) {
                for (LivingEntity e : inRing(boss, s, AuMichaelBalance.RAIN_RADIUS)) {
                    hit(boss, e, AllDamageTypes.SMITE, AuMichaelBalance.RAIN_DAMAGE);
                    e.igniteForTicks(AuMichaelBalance.SMITE_FIRE / 2);
                }
                michael(boss).fx(AuMichaelFxPayload.SMITE, Math.round(AuMichaelBalance.RAIN_RADIUS * 10), 1, s, 16);
            }
            sound(boss, ApocWorldRegistry.michael("smite"), 2.5f, 0.9f);
        }
    }

    // --- forced --------------------------------------------------------------------------------------------------------------

    /** A beat of wings and he stands behind the hunter who ran. */
    public static class WingStep extends BossAttack<LuciferEntity> {
        public WingStep() {
            super("wing_step", "-", 10, 2, 10);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.4, 1, 0.4, 0.05);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(3));
            Vec3 at = floorAt(boss, behind);
            level(boss).sendParticles(ParticleTypes.FLASH, boss.getX(), boss.getY() + 1, boss.getZ(), 1, 0, 0, 0, 0);
            boss.teleportTo(at.x, at.y, at.z);
            level(boss).sendParticles(AllParticles.GRACE.get(), at.x, at.y + 1, at.z, 30, 0.4, 1, 0.4, 0.05);
            sound(boss, ApocWorldRegistry.michael("gale"), 1.5f, 1.4f);
        }
    }
}
