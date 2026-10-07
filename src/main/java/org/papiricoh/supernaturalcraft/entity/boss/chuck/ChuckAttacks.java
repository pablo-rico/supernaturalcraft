package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
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
 * Everything the Author does to the hunters. As a man (chapters 1-2): a shove, a backhand, a thrown whiskey glass; the
 * Snap; and from Hell on the typewriter (keys raining, lines of text sweeping the floor, Backspace), rewritten rules and
 * floating words. As the light (3-5): the same, plus his two hands (slam, grab, sweep) and the ink echoes of old
 * enemies; in chapter 5 he narrates the hunters (only ever forced, see {@link ChuckEntity#forcedAttack}).
 *
 * <p>Every attack warns during its wind-up and only hurts in its active stage. Damage is {@code ERASED} (the snap),
 * {@code INK} (keys, words, lines, echoes) or {@code REWRITTEN} (his hands and body, broken rules), scaled by his
 * damage multiplier through {@code LuciferAttacks.hit}.
 */
public final class ChuckAttacks {

    private ChuckAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        int snap = ChuckBalance.snapCountdown(phase);
        return switch (phase) {
            case 1 -> List.of(opt(Shove::new, 3), opt(Backhand::new, 3), opt(ThrowGlass::new, 2.5f), opt(() -> new Snap(snap), 1.5f),
                    opt(TypewriterRain::new, 1.5f));
            case 2 -> List.of(opt(Shove::new, 1.5f), opt(Backhand::new, 2), opt(ThrowGlass::new, 2), opt(() -> new Snap(snap), 2),
                    opt(TypewriterRain::new, 2), opt(LineSweep::new, 2), opt(Backspace::new, 1.5f), opt(RewriteRules::new, 1.5f),
                    opt(FloatingWords::new, 1));
            case 3 -> List.of(opt(HandSlam::new, 2), opt(HandSweep::new, 2), opt(HandGrab::new, 1.5f), opt(TypewriterRain::new, 2),
                    opt(LineSweep::new, 1.5f), opt(() -> new Snap(snap), 2), opt(InkEcho::new, 2), opt(RewriteRules::new, 1.5f),
                    opt(FloatingWords::new, 1.5f), opt(Backspace::new, 1));
            default -> List.of(opt(HandSlam::new, 1.5f), opt(HandSweep::new, 1.5f), opt(HandGrab::new, 1.5f), opt(TypewriterRain::new, 2),
                    opt(LineSweep::new, 2), opt(() -> new Snap(snap), 2), opt(InkEcho::new, 2.5f), opt(RewriteRules::new, 1.5f),
                    opt(FloatingWords::new, 1.5f), opt(Backspace::new, 1.5f));
        };
    }

    // --- helpers --------------------------------------------------------------------------------------------------

    static ChuckEntity chuck(LuciferEntity boss) {
        return (ChuckEntity) boss;
    }

    static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !boss.minions().contains(e.getUUID());
    }

    static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** Whether nothing solid stands between {@code from} and {@code victim}'s eyes. */
    public static boolean sees(ServerLevel level, Vec3 from, LivingEntity victim, LuciferEntity boss) {
        return level.clip(new ClipContext(from, victim.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss))
                .getType() == HitResult.Type.MISS;
    }

    /** Hunters to aim at: the challengers, or the target if there are none (tests, dummies). */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(boss.challengers());
        if (out.isEmpty()) out.add(target);
        return out;
    }

    static void shove(LivingEntity e, Vec3 dir, double strength, double up) {
        e.setDeltaMovement(dir.x * strength, up, dir.z * strength);
        e.hurtMarked = true;
    }

    // --- the man ----------------------------------------------------------------------------------------------------

    /** Two hands to the chest: whoever is in front of him goes flying. */
    public static class Shove extends BossAttack<LuciferEntity> {
        private static final double REACH = 3.6;
        private float yaw;

        public Shove() {
            super("shove", "shove", 10, 4, 14);
        }

        @Override
        public boolean movesBoss() {
            return false;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 5 * 5 ? 1 : 0.05f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, (float) REACH, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(REACH, 2, REACH))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (to.length() > REACH || (to.length() > 0.6 && to.normalize().dot(dir) < 0.4)) continue;
                hit(boss, e, AllDamageTypes.REWRITTEN, 8);
                shove(e, dir, 2.2, 0.6);
            }
            sound(boss, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5f, 0.8f);
        }
    }

    /** The back of his hand, bored: a cuff that throws you sideways and leaves you slow. */
    public static class Backhand extends BossAttack<LuciferEntity> {
        private static final double REACH = 3.3;
        private float yaw;

        public Backhand() {
            super("backhand", "backhand", 12, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 5 * 5 ? 1 : 0.05f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = yawTo(boss.position(), target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, (float) REACH, TelegraphMarker.RED, windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw), side = dirOf(yaw - 90);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(REACH, 2, REACH))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (to.length() > REACH || (to.length() > 0.6 && to.normalize().dot(dir) < 0.3)) continue;
                hit(boss, e, AllDamageTypes.REWRITTEN, 10);
                shove(e, side, 1.4, 0.4);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), boss);
            }
            sound(boss, SoundEvents.PLAYER_ATTACK_STRONG, 1.5f, 0.7f);
        }
    }

    /** He finishes his whiskey and throws the glass where you stand; it bursts in ink. */
    public static class ThrowGlass extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 1.9f;
        private static final int FLIGHT = 12;
        private Vec3 spot = Vec3.ZERO, from = Vec3.ZERO;
        private boolean burst;

        public ThrowGlass() {
            super("throw_glass", "throw_glass", 14, FLIGHT + 2, 12);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            double d = boss.distanceTo(target);
            return d > 4 && d < 26 ? 1 : 0.2f;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position().add(target.getDeltaMovement().multiply(8, 0, 8)));
            TelegraphMarker.circle(level(boss), spot, RADIUS, TelegraphMarker.GOLD, windup + FLIGHT + 2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            from = boss.position().add(0, 1.6, 0);
            sound(boss, SoundEvents.WITCH_THROW, 1.5f, 0.8f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            if (t < FLIGHT) {
                double f = t / (double) FLIGHT;
                Vec3 p = from.lerp(spot, f).add(0, Math.sin(f * Math.PI) * 3, 0);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_BOTTLE)), p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
            } else if (!burst) {
                burst = true;
                for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(RADIUS, 2.5, RADIUS))) {
                    if (!inCircle(e, spot, RADIUS)) continue;
                    hit(boss, e, AllDamageTypes.INK, 9);
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), boss);
                }
                level.sendParticles(AllParticles.INK.get(), spot.x, spot.y + 0.3, spot.z, 30, RADIUS / 2, 0.2, RADIUS / 2, 0.1);
                level.playSound(null, BlockPos.containing(spot), SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.HOSTILE, 2f, 0.8f);
            }
        }
    }

    /**
     * The Snap: a frame is drawn around a hunter and counts down; at zero whoever is inside it AND still in his sight is
     * erased. Leave the frame, or put something solid between you and him. As the light, a hand hangs over the frame and
     * snaps.
     */
    public static class Snap extends BossAttack<LuciferEntity> {
        private Vec3 center = Vec3.ZERO;

        public Snap(int countdown) {
            super("snap", "snap", countdown, 2, 20);
        }

        public Vec3 center() {
            return center;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 40 * 40 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            center = floorAt(boss, target.position());
            frame(boss, center, windup);
            ChuckEntity c = chuck(boss);
            AuthorHandEntity hand = c.hand(true);
            if (hand != null) {
                hand.order(center.add(0, 7, 0), windup / 2);
                hand.face(boss.position());
            }
        }

        /** The frame on the floor and the countdown on screen. */
        public static void frame(LuciferEntity boss, Vec3 center, int ticks) {
            double r = ChuckBalance.SNAP_RADIUS;
            ServerLevel level = level(boss);
            // Four thin lines: the edges of the square.
            TelegraphMarker.line(level, center.add(-r, 0, -r), -90, 0.35f, (float) (2 * r), TelegraphMarker.WHITE, ticks);
            TelegraphMarker.line(level, center.add(-r, 0, r), -90, 0.35f, (float) (2 * r), TelegraphMarker.WHITE, ticks);
            TelegraphMarker.line(level, center.add(-r, 0, -r), 0, 0.35f, (float) (2 * r), TelegraphMarker.WHITE, ticks);
            TelegraphMarker.line(level, center.add(r, 0, -r), 0, 0.35f, (float) (2 * r), TelegraphMarker.WHITE, ticks);
            ChuckFx.snapCount(chuck(boss), center, r, ticks);
            sound(boss, AllSounds.CHUCK_WRITE.get(), 1.5f, 1.2f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            AuthorHandEntity hand = chuck(boss).hand(true);
            if (hand != null) hand.triggerAnim("action", "snap");
            sound(boss, AllSounds.CHUCK_SNAP.get(), 3f, 1f);
            for (LivingEntity e : marks(boss, target)) erase(boss, e, center);
            for (LivingEntity e : foes(boss, new AABB(center, center).inflate(ChuckBalance.SNAP_RADIUS, 4, ChuckBalance.SNAP_RADIUS))) {
                if (!(e instanceof ServerPlayer)) erase(boss, e, center);
            }
            ServerLevel level = level(boss);
            level.sendParticles(AllParticles.INK_LETTER.get(), center.x, center.y + 1, center.z, 60, ChuckBalance.SNAP_RADIUS / 2, 1, ChuckBalance.SNAP_RADIUS / 2, 0.05);
        }

        /** Whether the snap at {@code center} erases {@code victim}: inside the frame, and seen. */
        public static boolean catches(LuciferEntity boss, LivingEntity victim, Vec3 center) {
            Vec3 d = victim.position().subtract(center);
            if (!ChuckBalance.inSnapFrame(d.x, d.z, ChuckBalance.SNAP_RADIUS) || Math.abs(d.y) > 4) return false;
            return sees(level(boss), boss.getEyePosition(), victim, boss);
        }

        /** Erases {@code victim} if it is caught; true if it was. */
        public static boolean erase(LuciferEntity boss, LivingEntity victim, Vec3 center) {
            if (!isFoe(boss, victim) || !catches(boss, victim, center)) return false;
            hit(boss, victim, AllDamageTypes.ERASED, ChuckBalance.SNAP_DAMAGE);
            level(boss).sendParticles(AllParticles.INK_LETTER.get(), victim.getX(), victim.getY() + 1, victim.getZ(), 30, 0.4, 0.8, 0.4, 0.08);
            return true;
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            AuthorHandEntity hand = chuck(boss).hand(true);
            if (hand != null) hand.release();
        }
    }

    /** He types on nothing: giant keys rain onto marked spots, a few on every hunter. */
    public static class TypewriterRain extends BossAttack<LuciferEntity> {
        private static final String LETTERS = "SUPERNATURALCHUCKTHEEND";
        private final List<Vec3> spots = new ArrayList<>();

        public TypewriterRain() {
            super("typewriter_rain", "type_rain", 24, 50, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (LivingEntity m : marks(boss, target)) {
                spots.add(floorAt(boss, m.position()));
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                spots.add(floorAt(boss, m.position().add(Math.cos(a) * 3.2, 0, Math.sin(a) * 3.2)));
            }
            while (spots.size() < 9) spots.add(randomArenaPoint(boss, 0.8));
            for (int i = 0; i < spots.size(); i++) {
                Vec3 s = spots.get(i);
                int lands = windup + i * 4;
                TelegraphMarker.circle(level, s, (float) TypewriterKeyEntity.RADIUS, TelegraphMarker.RED, lands + 2);
                TypewriterKeyEntity key = AllEntities.TYPEWRITER_KEY.get().create(level);
                if (key == null) continue;
                key.moveTo(s.x, s.y + 18, s.z, boss.getRandom().nextFloat() * 360, 0);
                key.setLetter(LETTERS.charAt((i + boss.getRandom().nextInt(LETTERS.length())) % LETTERS.length()));
                key.drop(chuck(boss), s, lands);
                level.addFreshEntity(key);
            }
            sound(boss, AllSounds.CHUCK_TYPE.get(), 2.5f, 0.9f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 4 == 0) sound(boss, AllSounds.CHUCK_TYPE.get(), 1.5f, 0.8f + boss.getRandom().nextFloat() * 0.4f);
            if (t == spots.size() * 4) sound(boss, AllSounds.CHUCK_BELL.get(), 2f, 1f);
        }
    }

    /**
     * A line of his text sweeps the whole arena like a blade: a low line (jump it) or a high one (kneel under it). The
     * words are scenery; the line judges its own touch ({@link ChuckBalance#lineHits}).
     */
    public static class LineSweep extends BossAttack<LuciferEntity> {
        private static final String[] LINES = {"AND THE HUNTER FELL", "THEN THERE WAS NOTHING", "NOBODY WAS LEFT TO SAVE THEM",
                "AND IT WAS NOT GOOD", "THE END THE END THE END"};
        public static final int ACTIVE = 120;
        private final Set<UUID> struck = new HashSet<>();
        private Vec3 start = Vec3.ZERO, dir = Vec3.ZERO;
        private double halfWidth, speed, floorY;
        private boolean low;

        public LineSweep() {
            super("line_sweep", "line_sweep", 30, ACTIVE, 16);
        }

        public boolean low() {
            return low;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            Vec3 c = arena != null ? arena.centerVec() : boss.position();
            double r = arena != null ? arena.radius() - 2 : 20;
            float yaw = yawTo(c, target.position()) + (boss.getRandom().nextFloat() - 0.5f) * 60;
            dir = dirOf(yaw);
            low = boss.phase() < 3 || boss.getRandom().nextBoolean();
            halfWidth = r;
            floorY = floorAt(boss, c).y;
            start = new Vec3(c.x, floorY, c.z).subtract(dir.scale(r));
            speed = 2 * r / ACTIVE;
            ServerLevel level = level(boss);
            Vec3 across = dirOf(yaw + 90);
            TelegraphMarker.line(level, start.subtract(across.scale(r)), yaw + 90, 1.2f, (float) (2 * r),
                    low ? TelegraphMarker.RED : TelegraphMarker.VIOLET, windup + 6);
            // The words, along the starting line, waiting.
            String line = LINES[boss.getRandom().nextInt(LINES.length)];
            String[] words = line.split(" ");
            int n = (int) Math.max(4, 2 * r / 3.2);
            for (int i = 0; i < n; i++) {
                FloatingWordEntity w = AllEntities.FLOATING_WORD.get().create(level);
                if (w == null) continue;
                double along = -r + (i + 0.5) * (2 * r / n);
                Vec3 at = start.add(across.scale(along)).add(0, low ? 0.1 : ChuckBalance.HIGH_LINE_BOTTOM, 0);
                w.moveTo(at.x, at.y, at.z, 0, 0);
                w.setText(words[i % words.length]);
                w.setSize(0.8f);
                w.setColor(low ? 0x2A0E08 : 0x1A1030);
                w.bind(chuck(boss), 0, windup + ACTIVE + 4);
                level.addFreshEntity(w);
                w.launch(Vec3.ZERO);
                chuck(boss).track(w);
            }
            sound(boss, AllSounds.CHUCK_CARRIAGE.get(), 2f, 1f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (FloatingWordEntity w : chuck(boss).trackedWords()) w.launch(dir.scale(speed));
            sound(boss, AllSounds.CHUCK_CARRIAGE.get(), 2.5f, 0.7f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            Vec3 lineAt = start.add(dir.scale(speed * t));
            for (LivingEntity e : foes(boss, new AABB(lineAt, lineAt).inflate(halfWidth + 1, 4, halfWidth + 1))) {
                if (struck.contains(e.getUUID())) continue;
                if (touches(e, lineAt, dir, halfWidth, floorY, low)) {
                    struck.add(e.getUUID());
                    hit(boss, e, AllDamageTypes.INK, 12);
                    shove(e, dir, 0.9, 0.3);
                }
            }
        }

        /** Whether the line, now at {@code lineAt} sweeping along {@code dir}, touches {@code e}. */
        public static boolean touches(LivingEntity e, Vec3 lineAt, Vec3 dir, double halfWidth, double floorY, boolean low) {
            Vec3 to = e.position().subtract(lineAt).multiply(1, 0, 1);
            double ahead = to.dot(dir);
            double across = Math.abs(to.x * -dir.z + to.z * dir.x);
            if (Math.abs(ahead) > 0.7 || across > halfWidth) return false;
            return ChuckBalance.lineHits(low, e.getY() - floorY, e.getBbHeight());
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            chuck(boss).dropTrackedWords();
        }
    }

    /** Backspace: every hunter is sent back to where they stood five seconds ago. */
    public static class Backspace extends BossAttack<LuciferEntity> {
        public Backspace() {
            super("backspace", "backspace", 20, 2, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.challengers().isEmpty() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity m : marks(boss, target)) {
                TelegraphMarker.ring(level(boss), floorAt(boss, m.position()), 1.2f, TelegraphMarker.VIOLET, windup);
            }
            sound(boss, AllSounds.CHUCK_TYPE.get(), 2f, 0.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ChuckEntity c = chuck(boss);
            long now = level(boss).getGameTime();
            for (LivingEntity m : marks(boss, target)) pullBack(c, m, c.trail(m), now);
            sound(boss, AllSounds.CHUCK_BACKSPACE.get(), 3f, 1f);
        }

        /**
         * Sends {@code who} back {@link ChuckBalance#BACKSPACE_TICKS} along {@code trail}: the newest sample old enough
         * that is inside the arena and where they fit (else an older one, else a newer one). False if none will do.
         */
        public static boolean pullBack(ChuckEntity boss, LivingEntity who, PositionTrail trail, long now) {
            if (trail.size() == 0) return false;
            int first = trail.backIndex(now, ChuckBalance.BACKSPACE_TICKS);
            List<Integer> order = new ArrayList<>();
            for (int i = first; i < trail.size(); i++) order.add(i);
            for (int i = first - 1; i >= 0; i--) order.add(i);
            ArenaController arena = boss.arena();
            Vec3 from = who.position();
            for (int i : order) {
                PositionTrail.Sample s = trail.newest(i);
                Vec3 to = new Vec3(s.x(), s.y(), s.z());
                if (arena != null && !arena.contains(to)) continue;
                AABB box = who.getBoundingBox().move(to.subtract(from));
                if (!who.level().noCollision(who, box)) continue;
                if (who instanceof ServerPlayer p) p.connection.teleport(to.x, to.y, to.z, p.getYRot(), p.getXRot());
                if (who.position().distanceToSqr(to) > 1e-4) who.teleportTo(to.x, to.y, to.z);
                if (who.position().distanceToSqr(to) > 1e-4) who.setPos(to.x, to.y, to.z);
                who.setDeltaMovement(Vec3.ZERO);
                who.fallDistance = 0;
                ChuckFx.backspace(boss, who, from);
                level(boss).sendParticles(AllParticles.INK_LETTER.get(), from.x, from.y + 1, from.z, 20, 0.3, 0.8, 0.3, 0.03);
                return true;
            }
            return false;
        }
    }

    /** "And the water burned." He rewrites one rule of the world for a while. */
    public static class RewriteRules extends BossAttack<LuciferEntity> {
        public RewriteRules() {
            super("rewrite", "rewrite", 20, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return chuck(boss).rules() == 0 && ChuckBalance.rules(boss.phase()) != 0 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.INK_LETTER.get(), boss.getX(), boss.getEyeY(), boss.getZ(), 30, 1, 1, 1, 0.05);
            sound(boss, AllSounds.CHUCK_TYPE.get(), 2f, 1.1f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ChuckEntity c = chuck(boss);
            int rule = ChuckBalance.pickRule(boss.phase(), c.lastRule(), boss.getRandom().nextInt(16));
            if (rule != 0) c.applyRule(rule, ChuckBalance.ruleTicks(rule), marks(boss, target));
        }
    }

    /** He points, and words he writes in the air drift after the hunters; they burn on touch. */
    public static class FloatingWords extends BossAttack<LuciferEntity> {
        private static final String[] WORDS = {"AND THEN", "THE END", "FELL", "SAID", "HUNTER", "NO", "ALONE", "DIED", "GOD"};

        public FloatingWords() {
            super("floating_words", "point", 16, 4, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.INK_LETTER.get(), boss.getX(), boss.getEyeY(), boss.getZ(), 16, 0.6, 0.6, 0.6, 0.04);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            List<LivingEntity> marks = marks(boss, target);
            int n = 3 + marks.size();
            for (int i = 0; i < n; i++) {
                FloatingWordEntity w = AllEntities.FLOATING_WORD.get().create(level);
                if (w == null) continue;
                double a = i * Math.PI * 2 / n;
                double height = Math.min(boss.getBbHeight() * 0.7, 6);
                Vec3 at = boss.position().add(Math.cos(a) * 2.5, height, Math.sin(a) * 2.5);
                w.moveTo(at.x, at.y, at.z, 0, 0);
                w.setText(WORDS[boss.getRandom().nextInt(WORDS.length)]);
                w.setSize(0.9f);
                w.setColor(0x1A1410);
                w.bind(chuck(boss), 7, 160);
                level.addFreshEntity(w);
                w.hunt(marks.get(i % marks.size()), 0.2);
            }
            sound(boss, AllSounds.CHUCK_WRITE.get(), 2f, 0.9f);
        }
    }

    // --- the hands (the light) --------------------------------------------------------------------------------------

    /** A hand rises over its target and comes down flat. */
    public static class HandSlam extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 3.5f;
        private Vec3 spot = Vec3.ZERO;
        private boolean left;

        public HandSlam() {
            super("hand_slam", "narrate", 24, 8, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return chuck(boss).hand(false) != null || chuck(boss).hand(true) != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, RADIUS, TelegraphMarker.RED, windup + 4);
            left = boss.getRandom().nextBoolean();
            AuthorHandEntity hand = chuck(boss).hand(left);
            if (hand == null) hand = chuck(boss).hand(left = !left);
            if (hand != null) {
                hand.order(spot.add(0, 6, 0), windup - 4);
                hand.face(boss.position());
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            AuthorHandEntity hand = chuck(boss).hand(left);
            if (hand != null) {
                hand.order(spot.add(0, 0.2, 0), 3);
                hand.triggerAnim("action", "slam");
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t != 3) return;
            ServerLevel level = level(boss);
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(RADIUS, 3, RADIUS))) {
                if (!inCircle(e, spot, RADIUS)) continue;
                hit(boss, e, AllDamageTypes.REWRITTEN, 16);
                Vec3 away = e.position().subtract(spot).multiply(1, 0, 1);
                shove(e, away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize(), 1.2, 0.8);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, spot.x, spot.y + 0.3, spot.z, 3, 1, 0.1, 1, 0);
            level.sendParticles(AllParticles.GOLDEN_MOTE.get(), spot.x, spot.y + 0.3, spot.z, 40, RADIUS / 2, 0.2, RADIUS / 2, 0.15);
            sound(boss, AllSounds.SCRIBE_HAND_SLAM.get(), 3f, 0.6f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            AuthorHandEntity hand = chuck(boss).hand(left);
            if (hand != null) hand.release();
        }
    }

    /** A hand closes on its target and flings them into the air. */
    public static class HandGrab extends BossAttack<LuciferEntity> {
        private static final float RADIUS = 2.2f;
        private Vec3 spot = Vec3.ZERO;

        public HandGrab() {
            super("hand_grab", "narrate", 22, 6, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return chuck(boss).hand(true) != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, RADIUS, TelegraphMarker.VIOLET, windup + 2);
            AuthorHandEntity hand = chuck(boss).hand(true);
            if (hand != null) {
                hand.order(spot.add(0, 2.5, 0), windup - 2);
                hand.face(spot);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            AuthorHandEntity hand = chuck(boss).hand(true);
            if (hand != null) hand.triggerAnim("action", "grab");
            for (LivingEntity e : foes(boss, new AABB(spot, spot).inflate(RADIUS, 3, RADIUS))) {
                if (!inCircle(e, spot, RADIUS)) continue;
                hit(boss, e, AllDamageTypes.REWRITTEN, 8);
                Vec3 out = e.position().subtract(boss.position()).multiply(1, 0, 1);
                shove(e, out.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : out.normalize(), 0.8, 1.5);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), boss);
            }
            sound(boss, AllSounds.CHUCK_ERASE.get(), 2f, 0.7f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            AuthorHandEntity hand = chuck(boss).hand(true);
            if (hand != null) hand.release();
        }
    }

    /** A hand sweeps the floor in an arc, low: jump it. */
    public static class HandSweep extends BossAttack<LuciferEntity> {
        private static final double RADIUS = 10;
        private final Set<UUID> struck = new HashSet<>();
        private Vec3 from = Vec3.ZERO;
        private float yaw;

        public HandSweep() {
            super("hand_sweep", "narrate", 26, 10, 18);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return chuck(boss).hand(false) != null ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            Vec3 back = boss.position().subtract(target.position()).multiply(1, 0, 1);
            from = floorAt(boss, target.position().add(back.lengthSqr() < 0.01 ? Vec3.ZERO : back.normalize().scale(4)));
            yaw = yawTo(from, target.position());
            TelegraphMarker.cone(level(boss), from, yaw, (float) RADIUS, TelegraphMarker.RED, windup + 4);
            AuthorHandEntity hand = chuck(boss).hand(false);
            if (hand != null) hand.order(from.add(dirOf(yaw - 60).scale(6)).add(0, 0.3, 0), windup);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            AuthorHandEntity hand = chuck(boss).hand(false);
            if (hand != null) hand.triggerAnim("action", "sweep");
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            float a = yaw - 60 + 120f * Math.min(1, t / 8f);
            AuthorHandEntity hand = chuck(boss).hand(false);
            if (hand != null) {
                hand.order(from.add(dirOf(a).scale(6)).add(0, 0.3, 0), 1);
                hand.faceYaw(a + 90);
            }
            if (t != 4) return;
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : foes(boss, new AABB(from, from).inflate(RADIUS, 3, RADIUS))) {
                Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
                if (to.length() > RADIUS || (to.length() > 0.8 && to.normalize().dot(dir) < 0.5) || !e.onGround()) continue;
                if (struck.add(e.getUUID())) {
                    hit(boss, e, AllDamageTypes.INK, 12);
                    shove(e, dirOf(a + 90), 1.6, 0.4);
                }
            }
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 3f, 0.5f);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            AuthorHandEntity hand = chuck(boss).hand(false);
            if (hand != null) hand.release();
        }
    }

    // --- ink echoes -------------------------------------------------------------------------------------------------

    /**
     * An old enemy (one the hunters have beaten) written back in ink beside a hunter: it does a short version of its most
     * famous attack and runs down the page. Azazel's smoke dash, Lilith's white light, Lucifer's hellfire, the Chorus's
     * hymn, Metatron's Fall, Amara's hunger, the Uncaged's falling stars.
     */
    public static class InkEcho extends BossAttack<LuciferEntity> {
        public static final int STRIKE = 10;
        private @Nullable UUID echo;
        private String boss = "lucifer";
        private Vec3 at = Vec3.ZERO;
        private final List<Vec3> spots = new ArrayList<>();
        private float yaw;

        public InkEcho() {
            super("ink_echo", "echo_call", 30, STRIKE + 6, 20);
        }

        public String echoOf() {
            return boss;
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return ChuckBalance.echoes(boss.phase()) ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity host, LivingEntity target) {
            ChuckEntity c = chuck(host);
            List<String> beaten = c.beatenBosses();
            boss = beaten.get(host.getRandom().nextInt(beaten.size()));
            ServerLevel level = level(host);
            Vec3 away = host.position().subtract(target.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
            at = floorAt(host, target.position().add(away.scale(7)));
            yaw = yawTo(at, target.position());
            InkEchoEntity e = AllEntities.INK_ECHO.get().create(level);
            if (e != null) {
                e.moveTo(at.x, at.y, at.z, yaw, 0);
                e.setBoss(boss);
                e.bind(c);
                level.addFreshEntity(e);
                e.face(target.position());
                echo = e.getUUID();
                c.track(e);
            }
            int warn = windup + STRIKE + 2;
            switch (boss) {
                case "azazel" -> TelegraphMarker.line(level, at, yaw, 2.6f, 16, TelegraphMarker.YELLOW, warn);
                case "lilith" -> TelegraphMarker.ring(level, at, 12, TelegraphMarker.WHITE, warn);
                case "broken_chorus" -> TelegraphMarker.ring(level, at, 7, TelegraphMarker.GOLD, warn);
                case "amara" -> TelegraphMarker.circle(level, at, 5, TelegraphMarker.VIOLET, warn);
                case "metatron", "lucifer_uncaged" -> {
                    for (LivingEntity m : marks(host, target)) spots.add(floorAt(host, m.position()));
                    for (int i = 0; i < 3; i++) spots.add(floorAt(host, target.position().add(
                            (host.getRandom().nextDouble() - 0.5) * 10, 0, (host.getRandom().nextDouble() - 0.5) * 10)));
                    for (Vec3 s : spots) TelegraphMarker.circle(level, s, 2.3f, boss.equals("metatron") ? TelegraphMarker.GOLD : TelegraphMarker.RED, warn);
                }
                default -> {
                    spots.add(floorAt(host, target.position()));
                    TelegraphMarker.circle(level, spots.getFirst(), 3.2f, TelegraphMarker.RED, warn);
                }
            }
            sound(host, AllSounds.CHUCK_ECHO.get(), 2.5f, 1f);
        }

        @Override
        public void tickActive(LuciferEntity host, LivingEntity target, int t) {
            if (t != STRIKE) return;
            ServerLevel level = level(host);
            switch (boss) {
                case "azazel" -> {
                    Vec3 dir = dirOf(yaw), end = at.add(dir.scale(16));
                    for (LivingEntity e : foes(host, new AABB(at, end).inflate(1.5, 2, 1.5))) {
                        if (segment(e.position(), at, end) <= 1.3) hit(host, e, AllDamageTypes.INK, 12);
                    }
                    for (double d = 0; d <= 16; d += 0.8) {
                        Vec3 p = at.add(dir.scale(d));
                        level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 1, p.z, 2, 0.3, 0.4, 0.3, 0.01);
                    }
                    if (echo != null && level.getEntity(echo) instanceof InkEchoEntity e) e.setPos(end.x, end.y, end.z);
                }
                case "lilith" -> {
                    Vec3 eye = at.add(0, 1.6, 0);
                    for (LivingEntity e : foes(host, new AABB(at, at).inflate(12, 5, 12))) {
                        if (e.position().distanceTo(at) <= 12 && sees(level, eye, e, host)) hit(host, e, AllDamageTypes.INK, 14);
                    }
                    level.sendParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 2, 0, 0, 0, 0);
                }
                case "broken_chorus" -> {
                    for (LivingEntity e : foes(host, new AABB(at, at).inflate(9, 4, 9))) {
                        double d = flatDistance(e.position(), at);
                        if (d >= 5.5 && d <= 8.5) hit(host, e, AllDamageTypes.INK, 12);
                    }
                    for (int i = 0; i < 36; i++) {
                        double a = i * Math.PI * 2 / 36;
                        level.sendParticles(AllParticles.INK.get(), at.x + Math.cos(a) * 7, at.y + 0.5, at.z + Math.sin(a) * 7, 2, 0.1, 0.4, 0.1, 0.01);
                    }
                }
                case "amara" -> {
                    for (LivingEntity e : foes(host, new AABB(at, at).inflate(9, 4, 9))) {
                        Vec3 to = at.subtract(e.position()).multiply(1, 0, 1);
                        if (to.length() > 9) continue;
                        if (to.length() <= 5) hit(host, e, AllDamageTypes.INK, 10);
                        shove(e, to.lengthSqr() < 0.01 ? Vec3.ZERO : to.normalize(), 0.9, 0.2);
                    }
                    level.sendParticles(AllParticles.INK.get(), at.x, at.y + 1, at.z, 60, 2.5, 1, 2.5, 0.2);
                }
                case "metatron", "lucifer_uncaged" -> {
                    for (Vec3 s : spots) {
                        for (LivingEntity e : foes(host, new AABB(s, s).inflate(2.3, 3, 2.3))) {
                            if (inCircle(e, s, 2.3)) hit(host, e, AllDamageTypes.INK, 12);
                        }
                        level.sendParticles(AllParticles.INK.get(), s.x, s.y + 0.4, s.z, 25, 1.1, 0.2, 1.1, 0.12);
                        column(level, s, AllParticles.INK_LETTER.get(), 10, 8);
                    }
                }
                default -> {
                    Vec3 s = spots.getFirst();
                    for (LivingEntity e : foes(host, new AABB(s, s).inflate(3.2, 3, 3.2))) {
                        if (!inCircle(e, s, 3.2)) continue;
                        hit(host, e, AllDamageTypes.INK, 14);
                        e.igniteForSeconds(3);
                    }
                    level.sendParticles(AllParticles.HELLFIRE.get(), s.x, s.y + 0.3, s.z, 40, 1.6, 0.4, 1.6, 0.1);
                }
            }
            sound(host, AllSounds.CHUCK_ECHO.get(), 2f, 0.6f);
        }

        static double segment(Vec3 p, Vec3 a, Vec3 b) {
            double dx = b.x - a.x, dz = b.z - a.z, len2 = dx * dx + dz * dz;
            double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.z - a.z) * dz) / len2));
            double cx = a.x + dx * t - p.x, cz = a.z + dz * t - p.z;
            return Math.sqrt(cx * cx + cz * cz);
        }

        @Override
        public void onEnd(LuciferEntity host) {
            if (echo != null && level(host).getEntity(echo) instanceof InkEchoEntity e) e.dissolve();
        }
    }

    // --- chapter 5: narration -------------------------------------------------------------------------------------

    /**
     * "And the hunter ran." He narrates what a hunter does next, and for three and a half seconds watches. Whoever does
     * the opposite makes him a liar: the script cracks ({@link ChuckEntity#contradicted}). Whoever obeys is punished.
     */
    public static class Narration extends BossAttack<LuciferEntity> {
        private NarrationJudge.Order order = NarrationJudge.Order.RUN;
        private final Map<UUID, Vec3> stood = new HashMap<>();
        private final Set<UUID> jumped = new HashSet<>();

        public Narration() {
            super("narration", "narrate", ChuckBalance.NARRATION_WINDUP, ChuckBalance.NARRATION_JUDGED, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return 0;  // only ever forced
        }

        public NarrationJudge.Order order() {
            return order;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ChuckEntity c = chuck(boss);
            order = NarrationJudge.next(c.lastOrder(), boss.getRandom().nextInt(16));
            c.narrate(order);
            ChuckFx.narrate(c, NarrationJudge.lineKey(order), windup + active);
            sound(boss, AllSounds.CHUCK_TYPE.get(), 2.5f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (LivingEntity m : marks(boss, target)) stood.put(m.getUUID(), m.position());
            sound(boss, AllSounds.CHUCK_BELL.get(), 2f, 1.2f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ChuckEntity c = chuck(boss);
            for (LivingEntity m : marks(boss, target)) {
                Vec3 start = stood.computeIfAbsent(m.getUUID(), k -> m.position());
                if (!m.onGround() && m.getY() > start.y + 0.35) jumped.add(m.getUUID());
                if (t >= active - 1) {
                    NarrationJudge.Verdict v = judge(c, m, order, start, jumped.contains(m.getUUID()));
                    if (v == NarrationJudge.Verdict.CONTRADICTED) c.contradicted(m);
                    else if (v == NarrationJudge.Verdict.OBEYED) punish(c, m, order);
                }
            }
        }

        /** The verdict on {@code m} at the end of a line, from where they stood when it was spoken. */
        public static NarrationJudge.Verdict judge(ChuckEntity boss, LivingEntity m, NarrationJudge.Order order, Vec3 start, boolean jumped) {
            double moved = m.position().subtract(start).multiply(1, 0, 1).length();
            Vec3 toHim = boss.getEyePosition().subtract(m.getEyePosition()).normalize();
            double look = m.getLookAngle().dot(toHim);
            return NarrationJudge.judge(order, moved, look, jumped, m.isShiftKeyDown());
        }

        /** The price of doing as he wrote. */
        public static void punish(ChuckEntity boss, LivingEntity m, NarrationJudge.Order order) {
            hit(boss, m, AllDamageTypes.REWRITTEN, 10);
            switch (order) {
                case RUN -> m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), boss);
                case STAND_STILL -> m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4), boss);
                case LOOK_AWAY -> m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), boss);
                case JUMP -> m.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1), boss);
                case KNEEL -> m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), boss);
            }
            level(boss).sendParticles(AllParticles.INK_LETTER.get(), m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
        }

        @Override
        public void onEnd(LuciferEntity boss) {
            chuck(boss).narrate(null);
        }
    }

    /** A gap closer for the man: he is simply somewhere else, as if he had always been. */
    public static class Elsewhere extends BossAttack<LuciferEntity> {
        public Elsewhere() {
            super("elsewhere", "point", 10, 2, 10);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.PAGE_SCRAP.get(), boss.getX(), boss.getY() + 1, boss.getZ(), 20, 0.4, 0.8, 0.4, 0.04);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 behind = target.position().subtract(dirOf(target.getYRot()).scale(3));
            Vec3 at = floorAt(boss, behind);
            ArenaController arena = boss.arena();
            if (arena != null && !arena.contains(at)) at = floorAt(boss, target.position());
            boss.teleportTo(at.x, at.y, at.z);
            level(boss).sendParticles(AllParticles.PAGE_SCRAP.get(), at.x, at.y + 1, at.z, 20, 0.4, 0.8, 0.4, 0.04);
            sound(boss, AllSounds.CHUCK_WRITE.get(), 1.5f, 1.3f);
        }
    }
}
