package org.papiricoh.supernaturalcraft.entity.boss.alastair;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.RaphaelAttacks;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.network.AlastairFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
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
 * Alastair's attacks (v0.40), numbers in {@link AlastairBalance}. Each is shown on the ground in its windup and lands only in
 * ACTIVE (red: his blades, hooks, chains and the rack; yellow: his smoke).
 * <ul>
 *   <li>P1, bound: {@link BoundLash} (a red ring round the trap) and {@link Taunt}.</li>
 *   <li>P2: {@link Slash} (a short red cone), {@link Scalpels} (three red lanes), {@link Hook} (a red lane: whoever it bites is
 *   dragged to him), {@link ChainLash} (a red ring round him).</li>
 *   <li>P3: {@link Smoke} (yellow circle), {@link Rack} (red circle under a hunter: iron rises round whoever is caught),
 *   {@link CallDemons} (red rings at his spawn points), and the tormentor's blades.</li>
 * </ul>
 */
public final class AlastairAttacks {

    private AlastairAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(BoundLash::new, 3), opt(Taunt::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(opt(Slash::new, 3), opt(Scalpels::new, 2.5f),
            opt(Hook::new, 2), opt(ChainLash::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(opt(Rack::new, 3), opt(Smoke::new, 2),
            opt(CallDemons::new, 1.5f), opt(Hook::new, 2), opt(Scalpels::new, 1.5f), opt(Slash::new, 2));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            default -> P3;
        };
    }

    static AlastairEntity alastair(LuciferEntity boss) {
        return (AlastairEntity) boss;
    }

    // --- helpers -------------------------------------------------------------------------------------------------------------

    static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID())
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

    static List<LivingEntity> inRing(LuciferEntity boss, Vec3 at, float r) {
        return foes(boss, new AABB(at, at).inflate(r + 1, 3, r + 1)).stream().filter(e -> inCircle(e, at, r)).toList();
    }

    abstract static class Cued extends RaphaelAttacks.Cued {
        Cued(String id, String clip, int windup, int active, int recover) {
            super(id, clip, AlastairBalance.cue(clip), windup, active, recover);
        }
    }

    // --- P1, bound -----------------------------------------------------------------------------------------------------------

    /** The chains he is held by whip the floor round the trap. */
    public static class BoundLash extends Cued {
        public BoundLash() {
            super("bound_lash", "chain_lash", AlastairBalance.BOUND_LASH_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            TelegraphMarker.ring(level(boss), boss.position(), AlastairBalance.BOUND_LASH_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, boss.position(), AlastairBalance.BOUND_LASH_RADIUS)) {
                hit(boss, e, AllDamageTypes.HELLFIRE, AlastairBalance.BOUND_LASH_DAMAGE);
            }
            alastair(boss).fx(AlastairFxPayload.CHAINS, Math.round(AlastairBalance.BOUND_LASH_RADIUS * 10), 0, boss.position(), 14);
            sound(boss, AllSounds.apocalypse("alastair.chains"), 2f, 0.9f);
        }
    }

    /** A word for the hunters (no harm: he enjoys it). */
    public static class Taunt extends Cued {
        public Taunt() {
            super("taunt", "taunt", 10, 1, 20);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            alastair(boss).taunt();
        }
    }

    // --- P2, the tormentor ---------------------------------------------------------------------------------------------------

    public static class Slash extends Cued {
        private float yaw;

        public Slash() {
            super("slash", "scalpel_slash", AlastairBalance.SLASH_WINDUP, 2, 12);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 36 ? 1.5f : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, AlastairBalance.SLASH_REACH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inCone(boss, boss.position(), yaw, AlastairBalance.SLASH_REACH)) {
                hit(boss, e, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK, AlastairBalance.SLASH_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
            sound(boss, AllSounds.apocalypse("alastair.scalpel"), 1.5f, 1f);
        }
    }

    public static class Scalpels extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Scalpels() {
            super("scalpels", "scalpel_throw", AlastairBalance.THROW_WINDUP, 2, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            for (int k = -1; k <= 1; k++) {
                TelegraphMarker.line(level(boss), from, yaw + k * AlastairBalance.THROW_SPREAD, AlastairBalance.THROW_WIDTH, AlastairBalance.THROW_LENGTH,
                        TelegraphMarker.RED, windupLength(boss) + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (int k = -1; k <= 1; k++) {
                float y = yaw + k * AlastairBalance.THROW_SPREAD;
                for (LivingEntity e : inLane(boss, from, dirOf(y), AlastairBalance.THROW_LENGTH, AlastairBalance.THROW_WIDTH / 2)) {
                    hit(boss, e, net.minecraft.world.damagesource.DamageTypes.MOB_PROJECTILE, AlastairBalance.THROW_DAMAGE);
                }
                alastair(boss).fx(AlastairFxPayload.SCALPEL, Math.round(y), Math.round(AlastairBalance.THROW_LENGTH), from, 10);
            }
            sound(boss, AllSounds.apocalypse("alastair.scalpel"), 1.5f, 1.3f);
        }
    }

    /** The hook: a red lane; whoever it bites is hurt, slowed and dragged to his feet. */
    public static class Hook extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Hook() {
            super("hook", "hook_throw", AlastairBalance.HOOK_WINDUP, 2, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) > 25 ? 1.5f : 0.5f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            yaw = face(boss, target.position());
            from = boss.position();
            TelegraphMarker.line(level(boss), from, yaw, AlastairBalance.HOOK_WIDTH, AlastairBalance.HOOK_LENGTH, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            int caught = -1;
            for (LivingEntity e : inLane(boss, from, dir, AlastairBalance.HOOK_LENGTH, AlastairBalance.HOOK_WIDTH / 2)) {
                hit(boss, e, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK, AlastairBalance.HOOK_DAMAGE);
                Vec3 pull = boss.position().subtract(e.position()).multiply(1, 0, 1).normalize().scale(AlastairBalance.HOOK_PULL);
                e.setDeltaMovement(pull.x, 0.4, pull.z);
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AlastairBalance.HOOK_SLOW, 1));
                caught = e.getId();
                sound(boss, AllSounds.apocalypse("alastair.hook_hit"), 1.5f, 1f);
            }
            alastair(boss).fx(AlastairFxPayload.HOOK, Math.round(yaw), caught, from, 12);
            sound(boss, AllSounds.apocalypse("alastair.hook_throw"), 1.5f, 1f);
        }
    }

    public static class ChainLash extends Cued {
        public ChainLash() {
            super("chain_lash", "chain_lash", AlastairBalance.LASH_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return inRing(boss, boss.position(), AlastairBalance.LASH_RADIUS).isEmpty() ? 0.3f : 1.5f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            TelegraphMarker.circle(level(boss), boss.position(), AlastairBalance.LASH_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, boss.position(), AlastairBalance.LASH_RADIUS)) {
                hit(boss, e, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK, AlastairBalance.LASH_DAMAGE);
                Vec3 out = e.position().subtract(boss.position()).multiply(1, 0, 1).normalize();
                e.setDeltaMovement(out.x * 0.9, 0.35, out.z * 0.9);
                e.hurtMarked = true;
            }
            alastair(boss).fx(AlastairFxPayload.CHAINS, Math.round(AlastairBalance.LASH_RADIUS * 10), 1, boss.position(), 14);
            sound(boss, AllSounds.apocalypse("alastair.chains"), 2f, 1f);
        }
    }

    // --- P3, the rack --------------------------------------------------------------------------------------------------------

    public static class Smoke extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Smoke() {
            super("smoke", "smoke", AlastairBalance.SMOKE_WINDUP, 2, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, AlastairBalance.SMOKE_RADIUS, TelegraphMarker.YELLOW, windupLength(boss) + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity e : inRing(boss, at, AlastairBalance.SMOKE_RADIUS)) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, AlastairBalance.SMOKE_BLIND, 0));
                hit(boss, e, AllDamageTypes.HELLFIRE, AlastairBalance.SMOKE_DAMAGE);
            }
            level(boss).sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, at.x, at.y + 1, at.z, 60, 2, 1, 2, 0.02);
            alastair(boss).fx(AlastairFxPayload.SMOKE, Math.round(AlastairBalance.SMOKE_RADIUS * 10), 0, at, 60);
            sound(boss, AllSounds.apocalypse("alastair.smoke"), 2f, 1f);
        }
    }

    /** The rack rises under a hunter: iron closes round whoever is caught, and holds them while it hurts. */
    public static class Rack extends Cued {
        private Vec3 at = Vec3.ZERO;

        public Rack() {
            super("rack", "rack_call", AlastairBalance.RACK_WINDUP, 2, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            face(boss, target.position());
            at = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), at, AlastairBalance.RACK_RADIUS, TelegraphMarker.RED, windupLength(boss) + 4);
            sound(boss, AllSounds.apocalypse("alastair.rack_rise"), 2f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            ArenaController arena = boss.arena();
            for (LivingEntity e : inRing(boss, at, AlastairBalance.RACK_RADIUS)) {
                hit(boss, e, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK, AlastairBalance.RACK_DAMAGE);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AlastairBalance.RACK_HOLD, 6));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, AlastairBalance.RACK_HOLD, 1));
                // Iron bars round the one caught, gone again when the hold is over.
                if (arena != null) {
                    BlockPos c = e.blockPosition();
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            for (int dy = 0; dy <= 1; dy++) {
                                BlockPos p = c.offset(dx, dy, dz);
                                if (level.getBlockState(p).canBeReplaced()) arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), AlastairBalance.RACK_HOLD);
                            }
                        }
                }
            }
            alastair(boss).fx(AlastairFxPayload.RACK, Math.round(AlastairBalance.RACK_RADIUS * 10), 0, at, AlastairBalance.RACK_HOLD);
            sound(boss, AllSounds.apocalypse("alastair.rack_clamp"), 2f, 1f);
        }
    }

    public static class CallDemons extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public CallDemons() {
            super("call_demons", "summon", AlastairBalance.SUMMON_WINDUP, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return alastair(boss).demons() < AlastairBalance.MAX_DEMONS ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spots.clear();
            List<Vec3> all = boss.minionSpots(alastair(boss).spawnPoints());
            for (Vec3 s : all) {
                spots.add(s);
                TelegraphMarker.ring(level(boss), s, 1.2f, TelegraphMarker.RED, windupLength(boss) + 4);
                if (spots.size() >= boss.minionCount(AlastairBalance.DEMONS_PER_CALL)) break;
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            int room = AlastairBalance.MAX_DEMONS - alastair(boss).demons();
            for (Vec3 s : spots) {
                if (room-- <= 0) break;
                Mob m = AllEntities.BLACK_EYED_DEMON.get().create(level);
                if (m == null) continue;
                m.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                m.setPersistenceRequired();
                m.setTarget(target);
                level.addFreshEntity(m);
                boss.minions().add(m.getUUID());
                level.sendParticles(AllParticles.DEMON_SMOKE.get(), s.x, s.y + 1, s.z, 20, 0.3, 0.8, 0.3, 0.04);
            }
        }
    }
}
