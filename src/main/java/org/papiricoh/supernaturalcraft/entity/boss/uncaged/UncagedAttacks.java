package org.papiricoh.supernaturalcraft.entity.boss.uncaged;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferCinematics;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferAttacks.*;

/**
 * Lucifer Uncaged's pools: Lucifer's attacks, harder (his damage multiplier is twice Lucifer's),
 * plus ten of his own. Like every boss attack: warned on the ground in the wind-up, harmful only
 * when active.
 */
public final class UncagedAttacks {

    private UncagedAttacks() {
    }

    private static AttackScheduler.Option<LuciferEntity> opt(java.util.function.Supplier<BossAttack<LuciferEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<LuciferEntity>> P1 = List.of(
            opt(LuciferAttacks.Snap::new, 2), opt(LuciferAttacks.Grasp::new, 2), opt(LuciferAttacks.Fling::new, 2),
            opt(ChainLash::new, 3), opt(ShacklePull::new, 2));
    private static final List<AttackScheduler.Option<LuciferEntity>> P2 = List.of(
            opt(LuciferAttacks.Rain::new, 2), opt(LuciferAttacks.Fissure::new, 2), opt(LuciferAttacks.Leap::new, 2),
            opt(LuciferAttacks.WingSweep::new, 2), opt(BrimstonePillars::new, 3), opt(HoundPack::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P3 = List.of(
            opt(LuciferAttacks.Cage::new, 2), opt(LuciferAttacks.Collapse::new, 2), opt(LuciferAttacks.Beam::new, 2),
            opt(LuciferAttacks.Illusion::new, 1), opt(FrozenPrison::new, 2), opt(ChainLash::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P4 = List.of(
            opt(HellLegion::new, 1), opt(LuciferAttacks.Storm::new, 2), opt(LuciferAttacks.Drain::new, 2),
            opt(AbyssalPull::new, 2), opt(BrimstonePillars::new, 1), opt(HoundPack::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P5 = List.of(
            opt(LuciferAttacks.Judgement::new, 2), opt(LuciferAttacks.Storm::new, 2), opt(FallingStars::new, 3),
            opt(LuciferAttacks.Beam::new, 1), opt(LuciferAttacks.Drain::new, 1));
    private static final List<AttackScheduler.Option<LuciferEntity>> P6 = List.of(
            opt(LuciferAttacks.Judgement::new, 2), opt(LuciferAttacks.Storm::new, 2), opt(FallingStars::new, 2),
            opt(SixWingTempest::new, 3), opt(LuciferAttacks.Drain::new, 1), opt(Supernova::new, 1));

    public static List<AttackScheduler.Option<LuciferEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            case 4 -> P4;
            case 5 -> P5;
            default -> P6;
        };
    }

    /** Distance from {@code p} to the segment starting at {@code a} running {@code len} along {@code dir}; NaN if past its ends. */
    static double alongLine(Vec3 p, Vec3 a, Vec3 dir, double len) {
        Vec3 d = new Vec3(p.x - a.x, 0, p.z - a.z);
        double t = d.dot(dir);
        if (t < 0 || t > len) return Double.NaN;
        return d.subtract(dir.scale(t)).length();
    }

    // --- phase 1: the Prisoner -----------------------------------------------------------------------

    /** The chains still hanging from his wrists, cracked out like whips along the ground. */
    public static class ChainLash extends BossAttack<LuciferEntity> {
        private final List<Float> yaws = new ArrayList<>();
        private Vec3 from = Vec3.ZERO;
        static final float LENGTH = 22, WIDTH = 1.8f;

        public ChainLash() {
            super("chain_lash", "chain_lash", 24, 8, 22);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            from = boss.position();
            for (ServerPlayer p : boss.challengers()) yaws.add(yawTo(from, p.position()));
            if (yaws.isEmpty()) yaws.add(yawTo(from, target.position()));
            float base = yaws.getFirst();
            yaws.add(base + 35);
            yaws.add(base - 35);
            for (float y : yaws) TelegraphMarker.line(level(boss), from, y, WIDTH, LENGTH, TelegraphMarker.RED, windup + 6);
            sound(boss, AllSounds.UNCAGED_CHAINS.get(), 2.5f, 0.8f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (float y : yaws) {
                Vec3 dir = dirOf(y);
                for (int i = 1; i <= LENGTH; i++) {
                    Vec3 p = from.add(dir.scale(i));
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y + 0.3, p.z, 2, 0.15, 0.1, 0.15, 0.05);
                    if (i % 3 == 0) level.sendParticles(AllParticles.HELLFIRE.get(), p.x, p.y + 0.2, p.z, 1, 0.1, 0.05, 0.1, 0.02);
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(LENGTH + 1, 3, LENGTH + 1))) {
                    if (e == boss || e instanceof LuciferEntity) continue;
                    double off = alongLine(e.position(), from, dir, LENGTH);
                    if (!Double.isNaN(off) && off <= WIDTH / 2 + 0.4 && Math.abs(e.getY() - from.y) < 3) {
                        hit(boss, e, AllDamageTypes.HELLFIRE, 11);
                        e.push(dir.x * 1.2, 0.35, dir.z * 1.2);
                        e.hurtMarked = true;
                    }
                }
            }
            sound(boss, SoundEvents.CHAIN_BREAK, 3f, 0.5f);
        }
    }

    /** He hauls on his shackles: everyone is dragged toward him, into a burst of hellfire. */
    public static class ShacklePull extends BossAttack<LuciferEntity> {
        static final float BLAST = 3.5f;

        public ShacklePull() {
            super("shackle_pull", "shackle_pull", 20, 24, 24);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            TelegraphMarker.circle(level(boss), boss.position(), BLAST, TelegraphMarker.RED, windup + active + 2);
            sound(boss, AllSounds.UNCAGED_CHAINS.get(), 3f, 0.6f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            if (t < active - 4) {
                for (ServerPlayer p : boss.challengers()) {
                    Vec3 to = boss.position().subtract(p.position()).multiply(1, 0, 1);
                    if (to.length() > BLAST + 0.5 && to.length() < 18) {
                        Vec3 v = to.normalize().scale(0.22);
                        p.setDeltaMovement(p.getDeltaMovement().multiply(0.6, 1, 0.6).add(v));
                        p.hurtMarked = true;
                        if (t % 3 == 0) {
                            Vec3 mid = p.position().add(0, 1, 0);
                            level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 3, 0.2, 0.2, 0.2, 0.02);
                        }
                    }
                }
            } else if (t == active - 4) {
                level.sendParticles(AllParticles.HELLFIRE.get(), boss.getX(), boss.getY() + 0.5, boss.getZ(), 120, BLAST / 2, 0.5, BLAST / 2, 0.15);
                sound(boss, SoundEvents.BLAZE_SHOOT, 3f, 0.5f);
                for (ServerPlayer p : boss.challengers()) {
                    if (inCircle(p, boss.position(), BLAST)) {
                        hit(boss, p, AllDamageTypes.HELLFIRE, 12);
                        p.igniteForSeconds(4);
                    }
                }
            }
        }
    }

    // --- phase 2: Hellfire ----------------------------------------------------------------------------

    /** Columns of burning brimstone burst out of the island, under you and all around. */
    public static class BrimstonePillars extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();
        static final float R = 1.8f;

        public BrimstonePillars() {
            super("brimstone_pillars", "pillars", 30, 24, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) spots.add(p.position());
            Vec3 c = boss.arena() != null ? boss.arena().centerVec() : boss.position();
            double turn = boss.getRandom().nextDouble() * Math.PI;
            for (int ring = 1; ring <= 3; ring++) {
                for (int k = 0; k < 4 + ring * 2; k++) {
                    double a = turn + Math.PI * 2 * k / (4 + ring * 2) + ring * 0.4;
                    spots.add(floorAt(boss, c.add(Math.cos(a) * ring * 6.5, 0, Math.sin(a) * ring * 6.5)));
                }
            }
            for (int i = 0; i < spots.size(); i++) {
                TelegraphMarker.circle(level(boss), spots.get(i), R, TelegraphMarker.RED, windup + (i % 3) * 8 + 8);
            }
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 8 != 0) return;
            int wave = t / 8;
            ServerLevel level = level(boss);
            for (int i = 0; i < spots.size(); i++) {
                if (i % 3 != wave) continue;
                Vec3 s = spots.get(i);
                column(level, s, AllParticles.HELLFIRE.get(), 6, 18);
                level.sendParticles(ParticleTypes.LAVA, s.x, s.y + 0.5, s.z, 4, 0.5, 0.2, 0.5, 0.1);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(s, s).inflate(R, 6, R))) {
                    if (e instanceof LuciferEntity || !inCircle(e, s, R)) continue;
                    hit(boss, e, AllDamageTypes.HELLFIRE, 10);
                    e.igniteForSeconds(4);
                    e.push(0, 0.6, 0);
                    e.hurtMarked = true;
                }
            }
            sound(boss, SoundEvents.BLAZE_SHOOT, 2.5f, 0.6f);
        }
    }

    /** He whistles, and the hounds of Hell come up out of the stone, every one of them visible. */
    public static class HoundPack extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public HoundPack() {
            super("hound_pack", "hounds", 30, 10, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return LuciferAttacks.Legion.livingMinions(boss) < 4 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < 3; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Vec3 s = floorAt(boss, boss.position().add(Math.cos(a) * 6, 0, Math.sin(a) * 6));
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, 1.2f, TelegraphMarker.RED, windup + 6);
            }
            sound(boss, AllSounds.HELLHOUND_BARK.get(), 3f, 0.7f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (Vec3 s : spots) {
                HellhoundEntity hound = AllEntities.HELLHOUND.get().create(level);
                if (hound == null) continue;
                hound.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                hound.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                hound.reveal(Integer.MAX_VALUE / 2);
                hound.setTarget(target);
                level.addFreshEntity(hound);
                boss.minions().add(hound.getUUID());
                column(level, s, AllParticles.HELLFIRE.get(), 2, 10);
            }
        }
    }

    // --- phase 3: the cold of the Cage ----------------------------------------------------------------

    /** Cage ice closes over one challenger. The others can break them out; inside, the cold bites. */
    public static class FrozenPrison extends BossAttack<LuciferEntity> {
        private LivingEntity prisoner;
        private Vec3 at = Vec3.ZERO;

        public FrozenPrison() {
            super("frozen_prison", "frozen_prison", 20, 80, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            List<ServerPlayer> all = boss.challengers();
            prisoner = all.isEmpty() ? target : all.get(boss.getRandom().nextInt(all.size()));
            at = prisoner.position();
            TelegraphMarker.ring(level(boss), at, 1.6f, TelegraphMarker.BLUE, windup + 4);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            ServerLevel level = level(boss);
            at = prisoner.position();
            if (arena != null) {
                BlockPos c = BlockPos.containing(at);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int dy = 0; dy <= 2; dy++) {
                            boolean shell = Math.abs(dx) == 1 || Math.abs(dz) == 1 || dy == 2;
                            BlockPos p = c.offset(dx, dy, dz);
                            if (shell && level.getBlockState(p).canBeReplaced()) {
                                arena.mutate(level, p, AllBlocks.CAGE_ICE.get().defaultBlockState(), 140);
                            }
                        }
                    }
                }
            }
            level.sendParticles(AllParticles.FROST.get(), at.x, at.y + 1, at.z, 60, 1, 1, 1, 0.05);
            sound(boss, SoundEvents.GLASS_BREAK, 2f, 0.4f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (prisoner == null || t % 20 != 10 || !prisoner.isAlive()) return;
            if (prisoner.position().distanceTo(at) < 1.6) {
                hit(boss, prisoner, AllDamageTypes.SPELL, 4);
                prisoner.setTicksFrozen(Math.min(prisoner.getTicksRequiredToFreeze() + 100, prisoner.getTicksFrozen() + 60));
                prisoner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            }
        }
    }

    // --- phase 4: Legion -------------------------------------------------------------------------------

    /** Demons and hellhounds together, called up out of the Pit. */
    public static class HellLegion extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();

        public HellLegion() {
            super("hell_legion", "summon", 30, 10, 20);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return LuciferAttacks.Legion.livingMinions(boss) < 4 ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + boss.getRandom().nextDouble();
                Vec3 s = floorAt(boss, boss.position().add(Math.cos(a) * 5, 0, Math.sin(a) * 5));
                spots.add(s);
                TelegraphMarker.circle(level(boss), s, 1.0f, TelegraphMarker.RED, windup + 6);
            }
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (int i = 0; i < spots.size(); i++) {
                Vec3 s = spots.get(i);
                var mob = i % 2 == 0 ? AllEntities.BLACK_EYED_DEMON.get().create(level) : AllEntities.HELLHOUND.get().create(level);
                if (mob == null) continue;
                mob.moveTo(s.x, s.y, s.z, boss.getRandom().nextFloat() * 360, 0);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(s)), MobSpawnType.MOB_SUMMONED, null);
                if (mob instanceof HellhoundEntity h) h.reveal(Integer.MAX_VALUE / 2);
                if (mob instanceof BlackEyedDemon d) d.setTarget(target);
                mob.setTarget(target);
                level.addFreshEntity(mob);
                boss.minions().add(mob.getUUID());
                column(level, s, AllParticles.DEMON_SMOKE.get(), 3, 12);
            }
            sound(boss, AllSounds.DEMON_SMOKE.get(), 2f, 0.5f);
        }
    }

    /** A well of the Pit's own dark opens at the island's edge and drags everything toward the fall. */
    public static class AbyssalPull extends BossAttack<LuciferEntity> {
        private Vec3 well = Vec3.ZERO;
        static final double REACH = 13, CORE = 2.2;

        public AbyssalPull() {
            super("abyssal_pull", "abyssal_pull", 30, 70, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ArenaController arena = boss.arena();
            Vec3 c = arena != null ? arena.centerVec() : boss.position();
            double r = (arena != null ? Math.max(6, arena.floorRadius() > 0 ? arena.floorRadius() : arena.radius()) : 12) * 0.8;
            double a = yawTo(c, target.position()) * Math.PI / 180 + Math.PI / 2;
            well = floorAt(boss, c.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            TelegraphMarker.ring(level(boss), well, (float) REACH, TelegraphMarker.VIOLET, windup + active);
            TelegraphMarker.circle(level(boss), well, (float) CORE, TelegraphMarker.VIOLET, windup + active);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            level.sendParticles(AllParticles.VOID_MOTE.get(), well.x, well.y + 0.6, well.z, 6, CORE, 0.4, CORE, 0.02);
            for (ServerPlayer p : boss.challengers()) {
                Vec3 to = well.subtract(p.position()).multiply(1, 0, 1);
                double d = to.length();
                if (d > REACH || d < 0.3) continue;
                p.setDeltaMovement(p.getDeltaMovement().add(to.normalize().scale(0.055)));
                p.hurtMarked = true;
                if (d < CORE && t % 10 == 0) hit(boss, p, AllDamageTypes.VOID, 4);
            }
        }
    }

    // --- phase 5: the Morning Star ----------------------------------------------------------------------

    /** Stars fall on the island: each one marked in gold where it will strike. */
    public static class FallingStars extends BossAttack<LuciferEntity> {
        private final List<Vec3> spots = new ArrayList<>();
        static final float R = 2.4f;

        public FallingStars() {
            super("falling_stars", "falling_stars", 36, 44, 20);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            for (ServerPlayer p : boss.challengers()) {
                spots.add(p.position());
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                spots.add(floorAt(boss, p.position().add(Math.cos(a) * 3.5, 0, Math.sin(a) * 3.5)));
            }
            for (int i = 0; i < 6; i++) spots.add(randomArenaPoint(boss, 0.9));
            for (int i = 0; i < spots.size(); i++) {
                TelegraphMarker.circle(level(boss), spots.get(i), R, TelegraphMarker.GOLD, windup + i * 4 + 6);
            }
            sound(boss, AllSounds.LUCIFER_CHARGE.get(), 3f, 1.4f);
        }

        @Override
        public void tickActive(LuciferEntity boss, LivingEntity target, int t) {
            if (t % 4 != 0 || t / 4 >= spots.size()) return;
            ServerLevel level = level(boss);
            Vec3 s = spots.get(t / 4);
            for (int i = 0; i < 12; i++) {
                level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 12 - i, s.z, 2, 0.1, 0.1, 0.1, 0.0);
            }
            level.sendParticles(ParticleTypes.FLASH, s.x, s.y + 0.5, s.z, 1, 0, 0, 0, 0);
            level.sendParticles(AllParticles.GRACE.get(), s.x, s.y + 0.5, s.z, 40, R / 2, 0.3, R / 2, 0.2);
            level.playSound(null, BlockPos.containing(s), AllSounds.UNCAGED_STAR.get(), net.minecraft.sounds.SoundSource.HOSTILE, 2.5f, 1.2f);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(s, s).inflate(R, 3, R))) {
                if (e instanceof LuciferEntity || !inCircle(e, s, R)) continue;
                hit(boss, e, AllDamageTypes.GRACE, 14);
            }
        }
    }

    // --- phase 6: the Light-Bringer -------------------------------------------------------------------

    /** All six wings sweep at once: six lanes of light around him, with gaps between them. */
    public static class SixWingTempest extends BossAttack<LuciferEntity> {
        private float turn;
        private Vec3 from = Vec3.ZERO;
        static final float LENGTH = 26, WIDTH = 4.5f;

        public SixWingTempest() {
            super("six_wing_tempest", "tempest", 34, 10, 26);
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            from = floorAt(boss, boss.position());
            turn = yawTo(from, target.position()) + 30;
            for (int k = 0; k < 6; k++) {
                TelegraphMarker.line(level(boss), from, turn + k * 60, WIDTH, LENGTH, TelegraphMarker.WHITE, windup + 6);
            }
            sound(boss, AllSounds.LUCIFER_WINGS.get(), 4f, 0.6f);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (int k = 0; k < 6; k++) {
                Vec3 dir = dirOf(turn + k * 60);
                for (int i = 1; i <= LENGTH; i += 2) {
                    Vec3 p = from.add(dir.scale(i));
                    level.sendParticles(AllParticles.GRACE.get(), p.x, p.y + 0.6, p.z, 3, WIDTH / 4, 0.3, WIDTH / 4, 0.05);
                }
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(LENGTH + 1, 5, LENGTH + 1))) {
                    if (e instanceof LuciferEntity) continue;
                    double off = alongLine(e.position(), from, dir, LENGTH);
                    if (!Double.isNaN(off) && off <= WIDTH / 2 && Math.abs(e.getY() - from.y) < 4) {
                        hit(boss, e, AllDamageTypes.GRACE, 16);
                        e.push(dir.x * 1.6, 0.5, dir.z * 1.6);
                        e.hurtMarked = true;
                    }
                }
            }
            sound(boss, AllSounds.LUCIFER_SMITE.get(), 4f, 1.3f);
        }
    }

    /**
     * His last light. It only comes when he is nearly done: a long charge, then everything not standing in
     * a green circle or under a Ward is burned white.
     */
    public static class Supernova extends BossAttack<LuciferEntity> {
        private final List<Vec3> safe = new ArrayList<>();

        public Supernova() {
            super("supernova", "supernova", 90, 10, 50);
        }

        @Override
        public float weight(LuciferEntity boss, LivingEntity target) {
            return boss.isEnraged() ? 1 : 0;
        }

        @Override
        public void onWindup(LuciferEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            for (ServerPlayer p : boss.challengers()) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                safe.add(floorAt(boss, p.position().add(Math.cos(a) * 8, 0, Math.sin(a) * 8)));
            }
            if (safe.isEmpty()) safe.add(randomArenaPoint(boss, 0.6));
            for (Vec3 s : safe) TelegraphMarker.circle(level, s, 2.2f, TelegraphMarker.SAFE, windup + 10);
            ArenaController arena = boss.arena();
            if (arena != null) TelegraphMarker.ring(level, arena.centerVec(), arena.radius() - 0.5f, TelegraphMarker.WHITE, windup + 10);
            sound(boss, AllSounds.LUCIFER_CHARGE.get(), 5f, 0.5f);
            LuciferCinematics.smiteCharge(boss, windup);
        }

        @Override
        public void tickWindup(LuciferEntity boss, LivingEntity target, int t) {
            level(boss).sendParticles(ParticleTypes.END_ROD, boss.getX(), boss.getY() + boss.getBbHeight() * 0.6, boss.getZ(),
                    3 + t / 5, 3, 3, 3, 0.2);
        }

        @Override
        public void onActive(LuciferEntity boss, LivingEntity target) {
            LuciferCinematics.smiteRelease(boss);
            sound(boss, AllSounds.UNCAGED_STAR.get(), 6f, 0.5f);
            for (ServerPlayer p : boss.challengers()) {
                boolean sheltered = WardEntity.isSheltered(p) || safe.stream().anyMatch(s -> inCircle(p, s, 2.4));
                if (!sheltered) {
                    hit(boss, p, AllDamageTypes.GRACE, 30);
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                }
            }
            ArenaController arena = boss.arena();
            if (arena != null) {
                Vec3 c = arena.centerVec();
                level(boss).sendParticles(ParticleTypes.END_ROD, c.x, c.y + 3, c.z, 600, arena.radius() / 2.0, 4, arena.radius() / 2.0, 0.2);
            }
        }
    }
}
