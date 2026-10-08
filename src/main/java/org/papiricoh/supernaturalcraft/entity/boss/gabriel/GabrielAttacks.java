package org.papiricoh.supernaturalcraft.entity.boss.gabriel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.arena.ChannelLayouts;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.dirOf;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.floorAt;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.inCircle;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.level;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.randomArenaPoint;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.sound;
import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.yawTo;

/**
 * Gabriel's attacks, one pool per channel. Each is written on the ground in its windup and lands only in ACTIVE.
 * <ul>
 *   <li>CH 2, the sitcom. While the LAUGH sign is lit: the pie in the face, banana peels, the falling piano, the extras
 *   through the doors. In the quiet: his own tricks (a snap that bursts under you, a slap).</li>
 *   <li>CH 5, the game show (between rounds): confetti cannons, the buzzer's zap, a swing of the microphone.</li>
 *   <li>CH 7, the hospital: the defibrillator's shocks, the microphone, the snap.</li>
 *   <li>CH 9, the commercial, from his podium: a rain of candy, a spotlight from any of the five podiums, the jingle.</li>
 * </ul>
 * {@link SnapStep} closes the gap when the hunter runs or hides (never in the commercial).
 */
public final class GabrielAttacks {

    private GabrielAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> SITCOM = List.of(
            opt(PieThrow::new, 3), opt(BananaPeels::new, 2), opt(Piano::new, 2), opt(Extras::new, 1.5f),
            opt(SnapBurst::new, 1.5f), opt(Slap::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> GAME_SHOW = List.of(
            opt(Confetti::new, 3), opt(BuzzerZap::new, 2.5f), opt(MicSwing::new, 2), opt(SnapBurst::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> HOSPITAL = List.of(
            opt(Defibrillator::new, 3), opt(MicSwing::new, 2), opt(SnapBurst::new, 1.5f));
    private static final List<AttackScheduler.Option<LuciferEntity>> COMMERCIAL = List.of(
            opt(CandyRain::new, 3), opt(Spotlight::new, 2.5f), opt(Jingle::new, 1.5f));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> SITCOM;
            case 2 -> GAME_SHOW;
            case 3 -> HOSPITAL;
            default -> COMMERCIAL;
        };
    }

    public static GabrielEntity gabriel(LuciferEntity boss) {
        return (GabrielEntity) boss;
    }

    // --- helpers -------------------------------------------------------------------------------------------------

    /** Whether his tricks should land on {@code e}: not himself, not his doubles, nobody in creative. */
    public static boolean isFoe(LuciferEntity boss, LivingEntity e) {
        return e != boss && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof GabrielEntity) && !(e instanceof GabrielDoubleEntity)
                && !(e instanceof ServerPlayer p && (p.isCreative() || p.isSpectator()));
    }

    public static List<LivingEntity> foes(LuciferEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(boss, e));
    }

    /** Strikes everyone within {@code r} of {@code at}. */
    static void burst(LuciferEntity boss, Vec3 at, double r, float damage) {
        for (LivingEntity e : foes(boss, new AABB(at, at).inflate(r + 1, 4, r + 1))) {
            if (inCircle(e, at, r)) gabriel(boss).strike(e, damage);
        }
    }

    /** Everyone in a lane {@code length} long and {@code halfWidth} either side, from {@code from} toward {@code yaw}. */
    static List<LivingEntity> inLane(LuciferEntity boss, Vec3 from, float yaw, double length, double halfWidth) {
        Vec3 dir = dirOf(yaw);
        return foes(boss, new AABB(from, from).inflate(length + 1, 4, length + 1)).stream().filter(e -> {
            Vec3 to = e.position().subtract(from).multiply(1, 0, 1);
            double along = to.dot(dir), across = to.subtract(dir.scale(along)).length();
            return along >= -0.5 && along <= length && across <= halfWidth && Math.abs(e.getY() - from.y) < 3.5;
        }).toList();
    }

    /** The hunters to spread a gag over: every challenger, or the target alone. */
    static List<LivingEntity> marks(LuciferEntity boss, LivingEntity target) {
        List<LivingEntity> out = new ArrayList<>(boss.challengers());
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

    /**
     * An attack whose clip is cued so its moment (a snap, a slam, a shock) falls at the end of the windup: the scheduler's own
     * trigger is skipped ({@code animation} is no clip) and the clip starts {@code hitTicks} before the windup ends
     * (the art's timings: snap 9, buzzer_slam 9, defib 27, throw_pie 11 ticks).
     */
    public abstract static class Cued extends BossAttack<LuciferEntity> {
        protected final String clip;
        protected final int hitTicks;
        private boolean cued;

        protected Cued(String id, String clip, int hitTicks, int windup, int active, int recover) {
            super(id, "-", windup, active, recover);
            this.clip = clip;
            this.hitTicks = hitTicks;
        }

        protected int windupLength(LuciferEntity boss) {
            return Math.max(1, Math.round(windup * boss.windupScale()));
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            if (windupLength(boss) <= hitTicks) {
                cued = true;
                boss.triggerAnim("action", clip);
            }
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            if (!cued && t >= windupLength(boss) - hitTicks) {
                cued = true;
                boss.triggerAnim("action", clip);
            }
        }
    }

    /** The clips' moments, in ticks from their start. */
    static final int SNAP_HIT = 9, SLAM_HIT = 9, DEFIB_HIT = 27;
    /** Where the sounds' own moments fall: the piano's crash, the defibrillator's thump. */
    static final int PIANO_CRASH = 19, DEFIB_THUMP = 24;

    // --- shared: his own tricks ------------------------------------------------------------------------------------

    /** A snap of the fingers and he is behind the hunter. Forced when they run or hide. */
    public static class SnapStep extends BossAttack<LuciferEntity> {
        public SnapStep() {
            super("snap_step", "snap", 10, 2, 10);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 back = target.getLookAngle().multiply(1, 0, 1);
            if (back.lengthSqr() < 0.01) back = new Vec3(0, 0, 1);
            Vec3 at = floorAt(boss, target.position().subtract(back.normalize().scale(3)));
            ServerLevel level = level(boss);
            level.sendParticles(ParticleTypes.POOF, boss.getX(), boss.getY() + 1, boss.getZ(), 20, 0.4, 0.8, 0.4, 0.02);
            boss.teleportTo(at.x, at.y, at.z);
            face(boss, target.position());
            level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y + 1, at.z, 20, 0.4, 0.8, 0.4, 0.05);
            sound(boss, AllSounds.GABRIEL_SNAP.get(), 2f, 1.1f);
        }
    }

    /** A snap: something nasty bursts out of the floor under every hunter (shown first in red). */
    public static class SnapBurst extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public SnapBurst() {
            super("snap_burst", "snap", SNAP_HIT, 24, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            for (LivingEntity e : marks(boss, target)) spots.add(floorAt(boss, e.position()));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.8f, TelegraphMarker.RED, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            sound(boss, AllSounds.GABRIEL_SNAP.get(), 2.5f, 0.9f);
            ServerLevel level = level(boss);
            for (Vec3 s : spots) {
                level.sendParticles(ParticleTypes.FIREWORK, s.x, s.y + 0.3, s.z, 30, 0.5, 0.6, 0.5, 0.15);
                level.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
                burst(boss, s, 1.8, 8f);
            }
        }
    }

    /** A swing of his microphone (or a slap) at whoever is close, shown as a cone. */
    public static class MicSwing extends BossAttack<LuciferEntity> {
        private float yaw;

        public MicSwing() {
            super("mic_swing", "host_gesture", 12, 2, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.distanceToSqr(target) < 6 * 6 ? 1 : 0.1f;
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            yaw = face(boss, target.position());
            TelegraphMarker.cone(level(boss), boss.position(), yaw, 3.4f, TelegraphMarker.RED, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            Vec3 dir = dirOf(yaw);
            for (LivingEntity e : foes(boss, boss.getBoundingBox().inflate(4, 2, 4))) {
                Vec3 to = e.position().subtract(boss.position()).multiply(1, 0, 1);
                if (to.length() <= 3.4 && (to.length() < 0.8 || to.normalize().dot(dir) > 0.5)) {
                    gabriel(boss).strike(e, 8f);
                    e.push(dir.x * 0.8, 0.3, dir.z * 0.8);
                    e.hurtMarked = true;
                }
            }
            sound(boss, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 1.2f);
        }
    }

    // --- CH 2 · the sitcom -----------------------------------------------------------------------------------------

    static float litWeight(LuciferEntity boss) {
        return gabriel(boss).laughingNow() ? 1 : 0;
    }

    /** The pie in the face: one for the target and one for everyone else near. */
    public static class PieThrow extends BossAttack<LuciferEntity> {
        public PieThrow() {
            super("pie_throw", "throw_pie", 11, 2, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return litWeight(boss);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            face(boss, target.position());
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            int n = 0;
            for (LivingEntity e : marks(boss, target)) {
                if (n++ >= 3 || boss.distanceToSqr(e) > 20 * 20) continue;
                PieProjectile.throwAt(level(boss), gabriel(boss), e);
            }
            sound(boss, SoundEvents.SNOWBALL_THROW, 1.5f, 0.7f);
        }
    }

    /** Banana peels round the hunters' feet (shown first as small yellow rings): step on one and you go down. */
    public static class BananaPeels extends Cued {
        private final List<BlockPos> spots = new ArrayList<>();

        public BananaPeels() {
            super("banana_peels", "snap", SNAP_HIT, 16, 2, 12);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return litWeight(boss) * (gabriel(boss).peels() < GabrielBalance.PEELS * 2 ? 1 : 0);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ServerLevel level = level(boss);
            for (int i = 0; i < GabrielBalance.PEELS; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, d = 1.5 + boss.getRandom().nextDouble() * 3;
                Vec3 at = floorAt(boss, target.position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
                BlockPos pos = BlockPos.containing(at);
                if (spots.contains(pos)) continue;
                spots.add(pos);
                TelegraphMarker.circle(level, Vec3.atBottomCenterOf(pos), 0.6f, TelegraphMarker.YELLOW, windup + 4);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            for (BlockPos pos : spots) gabriel(boss).dropPeel(level(boss), pos);
            sound(boss, AllSounds.GABRIEL_SNAP.get(), 1.5f, 1.3f);
        }
    }

    /** A grand piano falls out of the flies onto the hunter: its shadow (red) first, then the crash. */
    public static class Piano extends Cued {
        private Vec3 spot = Vec3.ZERO;

        public Piano() {
            super("piano", "snap", SNAP_HIT, GabrielBalance.PIANO_WARNING, 2, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return litWeight(boss);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, GabrielBalance.PIANO_RADIUS, TelegraphMarker.RED, windup + 4);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            // The sound whistles down first: its crash lands with the piano.
            if (t == windupLength(boss) - PIANO_CRASH) {
                level(boss).playSound(null, spot.x, spot.y, spot.z, AllSounds.GABRIEL_PIANO.get(), net.minecraft.sounds.SoundSource.HOSTILE, 3f, 1.0f);
            }
            if (t % 4 == 0) {
                level(boss).sendParticles(ParticleTypes.NOTE, spot.x, spot.y + 6 + (windup - t) * 0.15, spot.z, 2, 0.5, 0.2, 0.5, 1.0);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DARK_OAK_PLANKS.defaultBlockState()),
                    spot.x, spot.y + 0.5, spot.z, 80, 1.2, 0.5, 1.2, 0.2);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_CONCRETE.defaultBlockState()),
                    spot.x, spot.y + 0.5, spot.z, 30, 1.0, 0.4, 1.0, 0.2);
            level.sendParticles(ParticleTypes.NOTE, spot.x, spot.y + 1, spot.z, 12, 1.2, 0.5, 1.2, 1.0);
            burst(boss, spot, GabrielBalance.PIANO_RADIUS, GabrielBalance.PIANO_DAMAGE);
        }
    }

    /** Extras walk in through the back flat's doors and join in. */
    public static class Extras extends BossAttack<LuciferEntity> {
        public Extras() {
            super("extras", "host_gesture", 20, 2, 10);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            long extras = gabriel(boss).doubles().stream().filter(d -> d.role() == GabrielDoubleEntity.Role.EXTRA).count();
            return litWeight(boss) * (extras < GabrielBalance.EXTRAS ? 1 : 0);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            GabrielEntity g = gabriel(boss);
            ArenaController arena = g.arena();
            if (g.ground() == null || arena == null) return;
            ServerLevel level = level(boss);
            for (ChannelLayouts.Spot s : ChannelLayouts.BACKSTAGE) {
                BlockPos at = g.ground().anywhere(level, arena, s);
                if (at != null) g.spawnDouble(level, GabrielDoubleEntity.Role.EXTRA, Vec3.atBottomCenterOf(at));
            }
            sound(boss, SoundEvents.WOODEN_DOOR_OPEN, 1.5f, 0.9f);
        }
    }

    /** In the quiet he fights for himself: a slap, close in. */
    public static class Slap extends MicSwing {
        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return (1 - litWeight(boss)) * super.weight(boss, target);
        }
    }

    // --- CH 5 · the game show --------------------------------------------------------------------------------------

    /** Confetti cannons: bursts round every hunter, shown first in yellow. */
    public static class Confetti extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Confetti() {
            super("confetti", "snap", SNAP_HIT, 26, 4, 14);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return gabriel(boss).quizActive() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            for (LivingEntity e : marks(boss, target)) {
                spots.add(floorAt(boss, e.position()));
                for (int i = 0; i < 2; i++) {
                    double a = boss.getRandom().nextDouble() * Math.PI * 2;
                    spots.add(floorAt(boss, e.position().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5)));
                }
            }
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.6f, TelegraphMarker.YELLOW, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            sound(boss, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2.5f, 1.0f);
            for (Vec3 s : spots) {
                level.sendParticles(ParticleTypes.FIREWORK, s.x, s.y + 0.5, s.z, 25, 0.6, 1.0, 0.6, 0.12);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), s.x, s.y + 1.5, s.z, 15, 0.6, 0.6, 0.6, 0.1);
                burst(boss, s, 1.6, 7f);
            }
        }
    }

    /** He slams the buzzer: a zap runs down a lane at the hunter (shown in violet). */
    public static class BuzzerZap extends Cued {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public BuzzerZap() {
            super("buzzer_zap", "buzzer_slam", SLAM_HIT, 22, 4, 16);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return gabriel(boss).quizActive() ? 0 : 1;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            from = boss.position();
            yaw = face(boss, target.position());
            TelegraphMarker.line(level(boss), from, yaw, 2.4f, 16f, TelegraphMarker.VIOLET, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Vec3 dir = dirOf(yaw);
            for (int i = 0; i < 16; i++) {
                Vec3 p = from.add(dir.scale(i));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.5, p.z, 4, 0.3, 0.3, 0.3, 0.1);
            }
            sound(boss, AllSounds.GABRIEL_BUZZER.get(), 2.5f, 1.2f);
            for (LivingEntity e : inLane(boss, from, yaw, 16, 1.2)) gabriel(boss).strike(e, 9f);
        }
    }

    // --- CH 7 · the hospital ---------------------------------------------------------------------------------------

    /** "Clear!": the defibrillator shocks zones round the hunters (violet first). */
    public static class Defibrillator extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public Defibrillator() {
            super("defib", "defib", DEFIB_HIT, 30, 4, 18);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            super.tickWindup(boss, target, t);
            // The paddles whine, then thump as the zones go off.
            if (t == windupLength(boss) - DEFIB_THUMP) sound(boss, AllSounds.GABRIEL_DEFIB.get(), 2.5f, 1.0f);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            for (LivingEntity e : marks(boss, target)) spots.add(floorAt(boss, e.position()));
            for (int i = 0; i < 2; i++) spots.add(randomArenaPoint(boss, 0.7));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 2.2f, TelegraphMarker.VIOLET, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (Vec3 s : spots) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, s.x, s.y + 0.4, s.z, 40, 1.0, 0.4, 1.0, 0.2);
                for (LivingEntity e : foes(boss, new AABB(s, s).inflate(3.2, 4, 3.2))) {
                    if (!inCircle(e, s, 2.2)) continue;
                    gabriel(boss).strike(e, 9f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                }
            }
        }
    }

    // --- CH 9 · the commercial -------------------------------------------------------------------------------------

    /** Trickster Treats rain on the stage: the spots are shown in yellow first. */
    public static class CandyRain extends Cued {
        private final List<Vec3> spots = new ArrayList<>();

        public CandyRain() {
            super("candy_rain", "snap", SNAP_HIT, 30, 4, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            for (LivingEntity e : marks(boss, target)) spots.add(floorAt(boss, e.position()));
            for (int i = 0; i < 3; i++) spots.add(randomArenaPoint(boss, 0.7));
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, 1.8f, TelegraphMarker.YELLOW, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            sound(boss, AllSounds.GABRIEL_JINGLE_COMMERCIAL.get(), 0.8f, 1.6f);
            for (Vec3 s : spots) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SUGAR)), s.x, s.y + 2, s.z, 30, 0.6, 1.0, 0.6, 0.15);
                level.sendParticles(ParticleTypes.FIREWORK, s.x, s.y + 0.3, s.z, 10, 0.5, 0.3, 0.5, 0.05);
                burst(boss, s, 1.8, 7f);
            }
        }
    }

    /** A spotlight sweeps from one of the five podiums (not always his own) down a lane at the hunter, in gold. */
    public static class Spotlight extends BossAttack<LuciferEntity> {
        private float yaw;
        private Vec3 from = Vec3.ZERO;

        public Spotlight() {
            super("spotlight", "spokesman_pose", 26, 6, 14);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            GabrielEntity g = gabriel(boss);
            List<LivingEntity> men = new ArrayList<>(g.spokesmen());
            men.add(g);
            LivingEntity source = men.get(boss.getRandom().nextInt(men.size()));
            from = source.position();
            yaw = yawTo(from, target.position());
            TelegraphMarker.line(level(boss), from, yaw, 2.6f, 24f, TelegraphMarker.GOLD, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Vec3 dir = dirOf(yaw);
            for (int i = 0; i < 24; i += 2) {
                Vec3 p = from.add(dir.scale(i));
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.6, p.z, 3, 0.4, 0.4, 0.4, 0.02);
            }
            sound(boss, SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
            for (LivingEntity e : inLane(boss, from, yaw, 24, 1.3)) gabriel(boss).strike(e, 10f);
        }
    }

    /** The jingle: a ring of sound rolls out from the middle of the stage (shown as a ring). */
    public static class Jingle extends Cued {
        private Vec3 centre = Vec3.ZERO;

        public Jingle() {
            super("jingle", "snap", SNAP_HIT, 28, 4, 16);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            super.onWindup(boss, target);
            ArenaController arena = boss.arena();
            centre = floorAt(boss, arena != null ? arena.centerVec() : boss.position());
            TelegraphMarker.ring(level(boss), centre, 7.5f, TelegraphMarker.VIOLET, windup + 4);
            TelegraphMarker.circle(level(boss), centre, 4.5f, TelegraphMarker.SAFE, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            sound(boss, AllSounds.GABRIEL_JINGLE_COMMERCIAL.get(), 2.5f, 1.0f);
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                level.sendParticles(ParticleTypes.NOTE, centre.x + Math.cos(a) * 6, centre.y + 0.5, centre.z + Math.sin(a) * 6, 1, 0, 0, 0, 1.0);
            }
            for (LivingEntity e : foes(boss, new AABB(centre, centre).inflate(8.5, 4, 8.5))) {
                double d = Math.sqrt(e.distanceToSqr(centre.x, e.getY(), centre.z));
                if (d >= 4.5 && d <= 7.5 && Math.abs(e.getY() - centre.y) < 3.5) gabriel(boss).strike(e, 9f);
            }
        }
    }
}
