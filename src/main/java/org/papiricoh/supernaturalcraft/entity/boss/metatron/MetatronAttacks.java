package org.papiricoh.supernaturalcraft.entity.boss.metatron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.hazard.FlameTrail;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.projectile.BossShard;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Metatron's attacks: his own (an angel blade, his grace; ink from the lectern), the Hand's (the quill
 * writes and the words burn, the palm, the sweep), the Book's (it falls, it shuts, it storms pages) and
 * the Tablet's (the Fall, the Word). Construct attacks order the Hand or the Book about and release it
 * when they end.
 */
public final class MetatronAttacks {

    private MetatronAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(opt(BladeRush::new, 3), opt(Smite::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(BladeRush::new, 2), opt(Smite::new, 1.5f), opt(QuillScript::new, 2.5f), opt(PalmSlam::new, 2), opt(Sweep::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(InkBolt::new, 2), opt(QuillScript::new, 2), opt(PalmSlam::new, 1.5f), opt(Sweep::new, 1.5f),
            opt(TomeCrush::new, 2), opt(BoundShut::new, 1.5f), opt(PageStorm::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(
            opt(InkBolt::new, 1.5f), opt(QuillScript::new, 1.5f), opt(PalmSlam::new, 1), opt(Sweep::new, 1),
            opt(TomeCrush::new, 1.5f), opt(BoundShut::new, 1), opt(PageStorm::new, 1.5f), opt(TheFall::new, 2.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    // --- helpers --------------------------------------------------------------------------

    private static MetatronEntity metatron(LuciferEntity boss) {
        return (MetatronEntity) boss;
    }

    private static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID());
    }

    private static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** Distance from {@code p} to the segment {@code a}-{@code b}, on the ground plane. */
    static double segmentDistance(Vec3 p, Vec3 a, Vec3 b) {
        double dx = b.x - a.x, dz = b.z - a.z, len2 = dx * dx + dz * dz;
        double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.z - a.z) * dz) / len2));
        double cx = a.x + dx * t - p.x, cz = a.z + dz * t - p.z;
        return Math.sqrt(cx * cx + cz * cz);
    }

    // --- his own -----------------------------------------------------------------------------

    /** An angel blade: a rush and two cuts. */
    public static class BladeRush extends BossAttack<LuciferEntity> {
        private Vec3 dash = Vec3.ZERO;
        private final Set<UUID> struck = new HashSet<>();

        public BladeRush() {
            super("blade_rush", "blade_rush", 14, 20, 18);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 14 * 14 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            float yaw = yawTo(boss.position(), target.position());
            dash = dirOf(yaw);
            TelegraphMarker.line(level(boss), boss.position(), yaw, 2f, (float) Math.min(13, boss.distanceTo(target) + 2), TelegraphMarker.GOLD, windup + 6);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t < 6) {
                boss.setDeltaMovement(dash.x * 1.05, boss.getDeltaMovement().y, dash.z * 1.05);
                level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 3, 0.3, 0.5, 0.3, 0.01);
            } else {
                boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
                boss.getLookControl().setLookAt(target);
            }
            if (t == 8 || t == 15) {
                struck.clear();
                Vec3 facing = dirOf(boss.getYRot());
                for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(3.4))) {
                    Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                    if (to.length() > 3.4 || (to.length() > 0.5 && to.normalize().dot(facing) < 0.3)) continue;
                    if (struck.add(e.getUUID())) {
                        hit(boss, e, AllDamageTypes.SPELL, 9);
                        e.knockback(0.5, -facing.x, -facing.z);
                    }
                }
                sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 1.2f);
            }
        }
    }

    /** His grace, laid on everything in front of him. */
    public static class Smite extends BossAttack<LuciferEntity> {
        private static final double RADIUS = 5.5;
        private float yaw;

        public Smite() {
            super("smite", "smite", 18, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 8 * 8 ? 1 : 0.1f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, (float) RADIUS, TelegraphMarker.GOLD, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(RADIUS, 3, RADIUS))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (to.length() > RADIUS || (to.length() > 0.8 && to.normalize().dot(dir) < 0.5)) continue;
                hit(boss, e, AllDamageTypes.GRACE, 10);
                e.setDeltaMovement(dir.x * 1.2, 0.5, dir.z * 1.2);
                e.hurtMarked = true;
            }
            level(boss).sendParticles(AllParticles.GRACE.get(), boss.getX() + dir.x * 2, boss.getY() + 1, boss.getZ() + dir.z * 2, 40, 1.5, 0.6, 1.5, 0.1);
            sound(boss, SoundEvents.BEACON_POWER_SELECT, 2f, 1.6f);
        }
    }

    /** From the lectern: slow, seeking drops of ink. */
    public static class InkBolt extends BossAttack<LuciferEntity> {
        public InkBolt() {
            super("ink_bolt", "ink_bolt", 16, 4, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.INK.get(), boss.getX(), boss.getEyeY(), boss.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Vec3 from = boss.getEyePosition();
            Vec3 dir = target.getEyePosition().subtract(from).normalize();
            double base = Math.atan2(dir.z, dir.x);
            for (int i = -1; i <= 1; i++) {
                double a = base + Math.toRadians(i * 14);
                BossShard s = new BossShard(level, boss, BossShard.Kind.VOID, 7f * boss.attackDamageMultiplier(), 0.03f, target);
                s.setPos(from.x, from.y, from.z);
                s.shoot(Math.cos(a), dir.y, Math.sin(a), 0.7f, 1f);
                level.addFreshEntity(s);
            }
            sound(boss, AllSounds.SCRIBE_HAND_WRITE.get(), 1.5f, 0.6f);
        }
    }

    // --- the Hand -----------------------------------------------------------------------------

    /** The quill writes a word across the floor; a moment later every stroke of it burns. */
    public static class QuillScript extends BossAttack<LuciferEntity> {
        private static final int STROKE = 10, LENGTH = 7;
        private final List<Vec3[]> strokes = new ArrayList<>();
        private final Set<UUID> struck = new HashSet<>();
        private boolean burned;

        public QuillScript() {
            super("quill_script", "command", 26, 4 * STROKE + 10, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return metatron(boss).hand() != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            List<Vec3> marks = new ArrayList<>();
            for (ServerPlayer p : boss.challengers()) marks.add(p.position());
            while (marks.size() < 3) marks.add(randomArenaPoint(boss, 0.7));
            for (Vec3 m : marks.subList(0, Math.min(4, marks.size()))) {
                float yaw = boss.getRandom().nextFloat() * 360;
                Vec3 d = dirOf(yaw).scale(LENGTH / 2.0);
                Vec3 a = floorAt(boss, m.subtract(d)), b = floorAt(boss, m.add(d));
                strokes.add(new Vec3[]{a, b});
                TelegraphMarker.line(level, a, yaw, 2.0f, LENGTH, TelegraphMarker.GOLD, windup + strokes.size() * STROKE + 10);
            }
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) hand.order(strokes.getFirst()[0].add(0, 0.05, 0), windup);
            sound(boss, AllSounds.SCRIBE_HAND_WRITE.get(), 2f, 0.7f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ScribeHandEntity hand = metatron(boss).hand();
            ServerLevel level = level(boss);
            int k = t / STROKE;
            if (k < strokes.size()) {
                Vec3[] s = strokes.get(k);
                double f = (t % STROKE) / (double) (STROKE - 1);
                Vec3 tip = s[0].lerp(s[1], f);
                if (hand != null) {
                    hand.order(tip.add(0, 0.05, 0), 1);
                    hand.face(s[1]);
                    if (t % STROKE == 0) hand.triggerAnim("action", "write");
                }
                level.sendParticles(AllParticles.INK.get(), tip.x, tip.y + 0.15, tip.z, 4, 0.1, 0.02, 0.1, 0.0);
                if (t % 3 == 0) sound(boss, AllSounds.SCRIBE_HAND_WRITE.get(), 1f, 1.2f);
            } else if (!burned && t >= strokes.size() * STROKE + 8) {
                burned = true;
                burn(boss, level);
            }
        }

        private void burn(LuciferEntity boss, ServerLevel level) {
            for (Vec3[] s : strokes) {
                double len = s[0].distanceTo(s[1]);
                for (double d = 0; d <= len; d += 0.7) {
                    Vec3 p = s[0].lerp(s[1], d / len);
                    level.sendParticles(AllParticles.HELLFIRE.get(), p.x, p.y + 0.2, p.z, 3, 0.2, 0.3, 0.2, 0.04);
                    level.sendParticles(AllParticles.INK.get(), p.x, p.y + 0.4, p.z, 2, 0.2, 0.3, 0.2, 0.02);
                    if (((int) (d / 0.7)) % 3 == 0) level.addFreshEntity(new FlameTrail(level, boss, p.x, p.y, p.z));
                }
                for (LivingEntity e : foes(boss, new AABB(s[0], s[1]).inflate(1.5, 2, 1.5))) {
                    if (segmentDistance(e.position(), s[0], s[1]) <= 1.1 && struck.add(e.getUUID())) {
                        hit(boss, e, AllDamageTypes.HELLFIRE, 10);
                        e.igniteForSeconds(3);
                    }
                }
            }
            sound(boss, SoundEvents.FIRECHARGE_USE, 2f, 0.6f);
        }

        @Override
        public boolean endEarly(LuciferEntity boss, LivingEntity target) {
            return burned;
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) hand.release();
        }
    }

    /** The Hand rises over its target and comes down flat. */
    public static class PalmSlam extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 3f;
        private Vec3 spot = Vec3.ZERO;

        public PalmSlam() {
            super("palm_slam", "command", 24, 8, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return metatron(boss).hand() != null ? 1 : 0;
        }

        private Vec3 from = Vec3.ZERO;

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, RADIUS, TelegraphMarker.RED, windup + 4);
            float yaw = yawTo(boss.position(), spot);
            // The palm turns flat ahead of the nib: stand back from the spot so it lands on it.
            from = spot.subtract(dirOf(yaw).scale(ScribeHandEntity.SLAM_REACH));
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) {
                hand.faceYaw(yaw);
                hand.order(from.add(0, 2.0, 0), windup - 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) {
                hand.order(from.add(0, -ScribeHandEntity.SLAM_DROP, 0), 3);
                hand.triggerAnim("action", "slam");
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != 3) return;
            ServerLevel level = level(boss);
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(RADIUS, 3, RADIUS))) {
                if (!inCircle(e, spot, RADIUS)) continue;
                hit(boss, e, AllDamageTypes.SPELL, 14);
                Vec3 away = e.position().subtract(spot).multiply(1, 0, 1);
                away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
                e.setDeltaMovement(away.x * 1.2, 0.8, away.z * 1.2);
                e.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.3, spot.z, 3, 1, 0.1, 1, 0);
            level.sendParticles(AllParticles.GRACE.get(), spot.x, spot.y + 0.3, spot.z, 40, RADIUS / 2, 0.2, RADIUS / 2, 0.15);
            sound(boss, AllSounds.SCRIBE_HAND_SLAM.get(), 3f, 0.7f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) hand.release();
        }
    }

    /** The Hand sweeps the floor in front of him, low: jump it, or be elsewhere. */
    public static class Sweep extends BossAttack<LuciferEntity> {
        private static final double RADIUS = 9;
        private float yaw;
        private Vec3 from = Vec3.ZERO;
        private final Set<UUID> struck = new HashSet<>();

        public Sweep() {
            super("sweep", "command", 26, 10, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return metatron(boss).hand() != null && boss.distanceToSqr(target) < RADIUS * RADIUS * 1.6 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            from = floorAt(boss, target.position().add(boss.position().subtract(target.position()).normalize().scale(4)));
            yaw = yawTo(from, target.position());
            TelegraphMarker.cone(level(boss), from, yaw, (float) RADIUS, TelegraphMarker.RED, windup + 4);
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) hand.order(from.add(dirOf(yaw - 60).scale(5)).add(0, -ScribeHandEntity.SWEEP_SINK, 0), windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) hand.triggerAnim("action", "sweep");
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            float a = yaw - 60 + 120f * Math.min(1, t / 8f);
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) {
                hand.order(from.add(dirOf(a).scale(5.5)).add(0, -ScribeHandEntity.SWEEP_SINK, 0), 1);
                hand.faceYaw(a + 90);
            }
            if (t == 4) {
                Vec3 dir = dirOf(yaw);
                for (LivingEntity e : foes(boss, new AABB(from, from).inflate(RADIUS, 3, RADIUS))) {
                    Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
                    // Off the ground (jumping) clears it.
                    if (to.length() > RADIUS || (to.length() > 0.8 && to.normalize().dot(dir) < 0.5) || !e.onGround()) continue;
                    if (struck.add(e.getUUID())) {
                        hit(boss, e, AllDamageTypes.SPELL, 12);
                        Vec3 side = dirOf(a + 90);
                        e.setDeltaMovement(side.x * 1.6, 0.4, side.z * 1.6);
                        e.hurtMarked = true;
                    }
                }
                sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.5f);
            }
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ScribeHandEntity hand = metatron(boss).hand();
            if (hand != null) hand.release();
        }
    }

    // --- the Book -----------------------------------------------------------------------------

    /** The Book rises and falls on its edge across its target, splintering shelves where it lands. */
    public static class TomeCrush extends BossAttack<LuciferEntity> {
        private static final double HALF_LENGTH = 2.5, HALF_WIDTH = 1.4;
        private Vec3 spot = Vec3.ZERO;
        private float yaw;

        public TomeCrush() {
            super("tome_crush", "command", 30, 8, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return metatron(boss).book() != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position());
            yaw = boss.getRandom().nextFloat() * 180;
            Vec3 d = dirOf(yaw).scale(HALF_LENGTH);
            TelegraphMarker.line(level(boss), spot.subtract(d), yaw, (float) (HALF_WIDTH * 2), (float) (HALF_LENGTH * 2), TelegraphMarker.RED, windup + 4);
            ScribeBookEntity book = metatron(boss).book();
            if (book != null) {
                book.order(spot.add(0, 9, 0), windup - 6);
                book.faceYaw(yaw);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ScribeBookEntity book = metatron(boss).book();
            if (book != null) {
                book.order(spot.add(0, 0.1, 0), 4);
                book.triggerAnim("action", "slam");
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != 4) return;
            ServerLevel level = level(boss);
            Vec3 along = dirOf(yaw), across = dirOf(yaw + 90);
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(HALF_LENGTH + 1, 3, HALF_LENGTH + 1))) {
                Vec3 to = e.position().subtract(spot).multiply(1, 0, 1);
                if (Math.abs(to.dot(along)) > HALF_LENGTH || Math.abs(to.dot(across)) > HALF_WIDTH) continue;
                hit(boss, e, AllDamageTypes.SPELL, 16);
                e.setDeltaMovement(e.getDeltaMovement().add(0, -0.5, 0));
                e.hurtMarked = true;
            }
            // Shelves splinter under it (the library is put back with the arena).
            ArenaController arena = boss.arena();
            if (arena != null) {
                for (double l = -HALF_LENGTH; l <= HALF_LENGTH; l += 0.5) {
                    for (double w = -HALF_WIDTH; w <= HALF_WIDTH; w += 0.5) {
                        Vec3 p = spot.add(along.scale(l)).add(across.scale(w));
                        for (int y = 0; y < 3; y++) {
                            BlockPos at = BlockPos.containing(p.x, spot.y + y, p.z);
                            if (level.getBlockState(at).is(Blocks.BOOKSHELF)) {
                                level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.BOOKSHELF.defaultBlockState()),
                                        at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
                                arena.mutate(level, at, Blocks.AIR.defaultBlockState(), 0);
                            }
                        }
                    }
                }
            }
            level.sendParticles(AllParticles.PAGE.get(), spot.x, spot.y + 0.5, spot.z, 40, 2, 0.4, 2, 0.15);
            level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.3, spot.z, 2, 1, 0.1, 1, 0);
            sound(boss, AllSounds.SCRIBE_BOOK_SLAM.get(), 3f, 0.6f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ScribeBookEntity book = metatron(boss).book();
            if (book != null) {
                book.triggerAnim("action", "open");
                book.release();
            }
        }
    }

    /** The Book opens beside its target and shuts on them: held, in the dark, until the others wound him enough. */
    public static class BoundShut extends BossAttack<LuciferEntity> {
        private static final double REACH = 1.9;
        private Vec3 spot = Vec3.ZERO;
        private boolean caught;

        public BoundShut() {
            super("bound_shut", "command", 24, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return metatron(boss).book() != null && metatron(boss).trapped() == null && target instanceof ServerPlayer ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, (float) REACH, TelegraphMarker.WHITE, windup + 2);
            ScribeBookEntity book = metatron(boss).book();
            if (book != null) {
                book.order(spot.add(0, 1.0, 0), windup - 4);
                book.triggerAnim("action", "open");
            }
            sound(boss, AllSounds.SCRIBE_BOOK_PAGES.get(), 2f, 0.7f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            MetatronEntity m = metatron(boss);
            ScribeBookEntity book = m.book();
            if (target instanceof ServerPlayer p && inCircle(p, spot, REACH)) {
                caught = true;
                m.trapInBook(p);
                if (book != null) {
                    book.triggerAnim("action", "close");
                    book.order(p.position().add(0, 0.2, 0), 2);
                }
                sound(boss, AllSounds.SCRIBE_BOOK_SLAM.get(), 2f, 1.2f);
            } else if (book != null) {
                book.triggerAnim("action", "slam");
            }
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ScribeBookEntity book = metatron(boss).book();
            if (book != null && !caught) {
                book.triggerAnim("action", "open");
                book.release();
            }
        }
    }

    /** The Book opens and its pages storm out, sweeping back and forth. */
    public static class PageStorm extends BossAttack<LuciferEntity> {
        private float yaw;

        public PageStorm() {
            super("page_storm", "command", 20, 30, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return metatron(boss).book() != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ScribeBookEntity book = metatron(boss).book();
            Vec3 at = boss.position().add(target.position().subtract(boss.position()).multiply(1, 0, 1).normalize().scale(3)).add(0, 2.5, 0);
            yaw = yawTo(at, target.position());
            TelegraphMarker.cone(level(boss), floorAt(boss, at), yaw, 12, TelegraphMarker.WHITE, windup + 30);
            if (book != null) {
                book.order(at, windup);
                book.face(target.position());
                book.triggerAnim("action", "storm");
            }
            sound(boss, AllSounds.SCRIBE_BOOK_PAGES.get(), 2.5f, 1.0f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0) return;
            ScribeBookEntity book = metatron(boss).book();
            Vec3 from = book != null ? book.position().add(0, 0.8, 0) : boss.getEyePosition();
            ServerLevel level = level(boss);
            float sweep = (float) Math.sin(t * 0.25) * 35;
            for (int i = -1; i <= 1; i++) {
                Vec3 d = dirOf(yaw + sweep + i * 9);
                BossShard s = new BossShard(level, boss, BossShard.Kind.PAGE, 5f * boss.attackDamageMultiplier(), 0f, null);
                s.setPos(from.x, from.y, from.z);
                s.shoot(d.x, -0.08, d.z, 1.0f, 2f);
                level.addFreshEntity(s);
            }
            if (t % 9 == 0) sound(boss, AllSounds.SCRIBE_BOOK_PAGES.get(), 1.5f, 1.3f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            ScribeBookEntity book = metatron(boss).book();
            if (book != null) book.release();
        }
    }

    // --- the Tablet ----------------------------------------------------------------------------

    /** The Fall: angels cast out of Heaven, coming down burning wherever he points. */
    public static class TheFall extends BossAttack<LuciferEntity> {
        private static final float R = 2.4f;
        private final List<Vec3> spots = new ArrayList<>();

        public TheFall() {
            super("the_fall", "the_fall", 36, 48, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) {
                spots.add(p.position());
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                spots.add(floorAt(boss, p.position().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5)));
            }
            for (int i = 0; i < 7; i++) spots.add(randomArenaPoint(boss, 0.9));
            for (int i = 0; i < spots.size(); i++) {
                TelegraphMarker.circle(level(boss), spots.get(i), R, TelegraphMarker.GOLD, windup + i * 4 + 6);
            }
            sound(boss, AllSounds.METATRON_FALL.get(), 3f, 0.6f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 4 != 0 || t / 4 >= spots.size()) return;
            ServerLevel level = level(boss);
            Vec3 s = spots.get(t / 4);
            for (int i = 0; i < 14; i++) {
                level.sendParticles(AllParticles.HELLFIRE.get(), s.x, s.y + 14 - i, s.z, 2, 0.15, 0.1, 0.15, 0.0);
                if (i % 3 == 0) level.sendParticles(AllParticles.GRACE.get(), s.x, s.y + 14 - i, s.z, 2, 0.4, 0.1, 0.4, 0.02);
            }
            level.sendParticles(ParticleTypes.FLASH, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
            level.sendParticles(AllParticles.HELLFIRE.get(), s.x, s.y + 0.5, s.z, 40, R / 2, 0.3, R / 2, 0.2);
            level.playSound(null, BlockPos.containing(s), AllSounds.UNCAGED_STAR.get(), net.minecraft.sounds.SoundSource.HOSTILE, 2.5f, 0.9f);
            for (LivingEntity e : foes(boss, new AABB(s, s).inflate(R, 3, R))) {
                if (!inCircle(e, s, R)) continue;
                hit(boss, e, AllDamageTypes.GRACE, 14);
                e.igniteForSeconds(3);
            }
        }
    }

    /**
     * The Word of God: an order written in the air for everyone to see, then three seconds in which it
     * is enforced. BE STILL, LOOK AWAY or KNEEL; whoever breaks it is struck, and carries the mark.
     */
    public static class TheWord extends BossAttack<LuciferEntity> {
        public static final int ENFORCED = 60;
        private WordJudge.Order order = WordJudge.Order.BE_STILL;
        private final Map<UUID, Vec3> stood = new HashMap<>();
        private final Set<UUID> punished = new HashSet<>();

        public TheWord() {
            super("the_word", "the_word", 30, ENFORCED, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return 0;  // only ever forced
        }

        public WordJudge.Order order() {
            return order;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            MetatronEntity m = metatron(boss);
            order = WordJudge.next(m.lastWord(), boss.getRandom().nextInt(3));
            m.setLastWord(order);
            announce(boss, order);
        }

        /** Writes {@code order} across every challenger's sight. */
        public static void announce(LuciferEntity boss, WordJudge.Order order) {
            for (ServerPlayer p : boss.challengers()) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 85, 10));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("word.supernaturalcraft.subtitle")));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("word.supernaturalcraft." + order.key())
                        .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD)));
            }
            sound(boss, AllSounds.METATRON_WORD.get(), 4f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) stood.put(p.getUUID(), p.position());
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            boolean ending = t >= ENFORCED - 1;
            for (ServerPlayer p : boss.challengers()) {
                if (punished.contains(p.getUUID())) continue;
                Vec3 start = stood.getOrDefault(p.getUUID(), p.position());
                double moved = p.position().subtract(start).multiply(1, 0, 1).length();
                Vec3 toHim = boss.getEyePosition().subtract(p.getEyePosition()).normalize();
                double look = p.getLookAngle().dot(toHim);
                if (WordJudge.disobeys(order, moved, look, p.isShiftKeyDown(), ending)) {
                    punished.add(p.getUUID());
                    punish(boss, p, order);
                } else if (ending) {
                    metatron(boss).kept(p, order);
                }
            }
        }

        /** The price of disobeying {@code order}. */
        public static void punish(LuciferEntity boss, LivingEntity p, WordJudge.Order order) {
            hit(boss, p, AllDamageTypes.GRACE, SNConfig.METATRON_WORD_DAMAGE.get().floatValue());
            switch (order) {
                case BE_STILL -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), boss);
                case LOOK_AWAY -> p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), boss);
                case KNEEL -> {
                    Vec3 away = p.position().subtract(boss.position()).multiply(1, 0, 1);
                    away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
                    p.setDeltaMovement(away.x * 1.8, 0.7, away.z * 1.8);
                    p.hurtMarked = true;
                    p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), boss);
                }
            }
            level(boss).sendParticles(AllParticles.GRACE.get(), p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
        }
    }

    /** Rewrites a cell of floor for the Rewrite (columns rise, holes open), avoiding the dais and anyone standing there. */
    static boolean free(ServerLevel level, ArenaController arena, BlockPos at) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(0.5, 2, 0.5)).isEmpty()
                && ArenaTerrain.surface(level, arena, at.getX(), at.getZ()) != null;
    }
}
