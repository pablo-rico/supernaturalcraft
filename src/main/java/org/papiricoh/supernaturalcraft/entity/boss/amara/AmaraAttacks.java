package org.papiricoh.supernaturalcraft.entity.boss.amara;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.boss.BossStrike;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.entity.boss.AttackScheduler;
import org.papiricoh.supernaturalcraft.entity.boss.BossAttack;
import org.papiricoh.supernaturalcraft.entity.hazard.VoidZone;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;
import org.papiricoh.supernaturalcraft.entity.projectile.BossShard;
import org.papiricoh.supernaturalcraft.light.LightWellBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Everything the Darkness does to those who stand against her, by the phase that brings it.
 * Violet telegraphs are her void, green marks a safe place; every attack warns before it lands.
 */
public final class AmaraAttacks {

    private AmaraAttacks() {
    }

    private static AttackScheduler.Option<AmaraEntity> opt(Supplier<BossAttack<AmaraEntity>> f, float w) {
        return new AttackScheduler.Option<>(f, w);
    }

    private static final List<AttackScheduler.Option<AmaraEntity>> P1 = List.of(
            opt(UmbralOrbit::new, 3), opt(GravityWell::new, 2), opt(EclipseLance::new, 2), opt(ShadowRain::new, 2),
            opt(SpawnShades::new, 1), opt(Snuff::new, 1));
    private static final List<AttackScheduler.Option<AmaraEntity>> P2 = List.of(
            opt(TentacleSlam::new, 3), opt(TentacleSweep::new, 2), opt(Grasp::new, 2), opt(BurrowingSpikes::new, 2),
            opt(VoidBloom::new, 2), opt(Snuff::new, 1));
    private static final List<AttackScheduler.Option<AmaraEntity>> P3 = List.of(
            opt(CoronaFlare::new, 3), opt(Totality::new, 2), opt(BlackSunCollapse::new, 2), opt(EclipseLance::new, 1),
            opt(ShadowRain::new, 1), opt(Snuff::new, 1));
    private static final List<AttackScheduler.Option<AmaraEntity>> P4 = List.of(
            opt(VoidStep::new, 3), opt(Unmaking::new, 2), opt(UmbralOrbit::new, 1), opt(GravityWell::new, 1),
            opt(SpawnShades::new, 1), opt(Snuff::new, 1));

    public static List<AttackScheduler.Option<AmaraEntity>> pool(int phase) {
        return switch (phase) {
            case 1 -> P1;
            case 2 -> P2;
            case 3 -> P3;
            default -> P4;
        };
    }

    // --- helpers --------------------------------------------------------------------------

    static ServerLevel level(AmaraEntity boss) {
        return (ServerLevel) boss.level();
    }

    static void hit(AmaraEntity boss, Entity victim, float amount) {
        if (victim.getType().is(AllTags.Entities.DARKNESS) || victim instanceof AmaraPart) return;
        BossStrike.deal(boss, victim, AllDamageTypes.VOID, amount * boss.attackDamageMultiplier());
    }

    static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    static Vec3 floorAt(AmaraEntity boss, Vec3 p) {
        ArenaController arena = boss.arena();
        if (arena != null) {
            BlockPos s = ArenaTerrain.surface(level(boss), arena, Mth.floor(p.x), Mth.floor(p.z));
            if (s != null) return new Vec3(p.x, s.getY() + 1, p.z);
        }
        return p;
    }

    static Vec3 mass(AmaraEntity boss) {
        return boss.position().add(0, AmaraEntity.MASS_CENTER, 0);
    }

    static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }

    static List<LivingEntity> victims(AmaraEntity boss, AABB box) {
        return level(boss).getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.getType().is(AllTags.Entities.DARKNESS) && !(e instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()))
                        && !org.papiricoh.supernaturalcraft.allegiance.BossTwists.amaraIgnores(e, boss.level().getGameTime()));
    }

    static void sound(AmaraEntity boss, SoundEvent e, float volume, float pitch) {
        boss.level().playSound(null, boss.blockPosition(), e, SoundSource.HOSTILE, volume, pitch);
    }

    static int blockLight(ServerLevel level, Vec3 at) {
        return level.getBrightness(LightLayer.BLOCK, BlockPos.containing(at));
    }

    // --- all phases -----------------------------------------------------------------------

    /**
     * She breathes in the light: torches, lanterns, candles and her own wells around the target go
     * out (the arena gives them back when the fight ends). Campfires hold.
     */
    public static class Snuff extends BossAttack<AmaraEntity> {
        public static final int RADIUS = 10;
        Vec3 center;

        public Snuff() {
            super("snuff", "snuff", 30, 2, 30);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            // Only worth doing when there is light about to snuff.
            return boss.litWells() > 1 || blockLight(level(boss), target.getEyePosition()) >= AmaraBalance.LIT ? 1f : 0.2f;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            center = floorAt(boss, target.position());
            TelegraphMarker.ring(level(boss), center, RADIUS, TelegraphMarker.VIOLET, windup);
            sound(boss, AllSounds.AMARA_AMBIENT.get(), 3f, 0.4f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            snuff(boss, center, RADIUS);
        }

        /** Puts out every light in reach. Public for tests. */
        public static int snuff(AmaraEntity boss, Vec3 center, int radius) {
            ServerLevel level = level(boss);
            ArenaController arena = boss.arena();
            int out = 0;
            BlockPos c = BlockPos.containing(center);
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-radius, -3, -radius), c.offset(radius, 6, radius))) {
                if (pos.distToCenterSqr(center) > radius * radius) continue;
                BlockState s = level.getBlockState(pos);
                BlockState dark = null;
                if (s.is(AllBlocks.LIGHT_WELL.get()) && s.getValue(LightWellBlock.LIT)) dark = s.setValue(LightWellBlock.LIT, false);
                else if (s.getBlock() instanceof AbstractCandleBlock && s.hasProperty(AbstractCandleBlock.LIT) && s.getValue(AbstractCandleBlock.LIT)) {
                    dark = s.setValue(AbstractCandleBlock.LIT, false);
                } else if (s.is(AllTags.Blocks.SNUFFABLE)) dark = Blocks.AIR.defaultBlockState();
                if (dark == null) continue;
                BlockPos at = pos.immutable();
                boolean done = arena != null ? arena.mutate(level, at, dark, 0) : level.setBlock(at, dark, 3);
                if (done) {
                    out++;
                    level.sendParticles(ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.01);
                }
            }
            if (out > 0) level.playSound(null, BlockPos.containing(center), AllSounds.AMARA_SNUFF.get(), SoundSource.HOSTILE, 3f, 0.6f);
            return out;
        }
    }

    // --- phase one ------------------------------------------------------------------------

    /** A spiral of void shards unwinding from her mass. */
    public static class UmbralOrbit extends BossAttack<AmaraEntity> {
        float angle;

        public UmbralOrbit() {
            super("umbral_orbit", "orbit", 20, 60, 40);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), 7f, TelegraphMarker.VIOLET, windup + active);
            angle = boss.getRandom().nextFloat() * Mth.TWO_PI;
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0) return;
            angle += 0.42f;
            for (int arm = 0; arm < 2; arm++) {
                float a = angle + arm * Mth.PI;
                Vec3 dir = new Vec3(Mth.cos(a), -0.06, Mth.sin(a));
                Vec3 from = mass(boss).add(dir.scale(3.6));
                BossShard shard = new BossShard(boss.level(), boss, BossShard.Kind.VOID, 5f * boss.attackDamageMultiplier(), 0f, null);
                shard.setPos(from.x, Math.max(from.y, target.getY() + 1.1), from.z);
                shard.shoot(dir.x, 0, dir.z, 0.55f, 0f);
                boss.level().addFreshEntity(shard);
            }
        }
    }

    /** A point of crushing gravity: pulls everyone in, then bursts. */
    public static class GravityWell extends BossAttack<AmaraEntity> {
        public static final float PULL_RADIUS = 9, BURST_RADIUS = 2.8f, DAMAGE = 9;
        Vec3 center;

        public GravityWell() {
            super("gravity_well", "well", 30, 40, 40);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            center = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), center, BURST_RADIUS, TelegraphMarker.VIOLET, windup + active);
            TelegraphMarker.ring(level(boss), center, PULL_RADIUS, TelegraphMarker.VIOLET, windup + active);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            ServerLevel level = level(boss);
            level.sendParticles(AllParticles.VOID_MOTE.get(), center.x, center.y + 0.6, center.z, 6, PULL_RADIUS * 0.4, 0.3, PULL_RADIUS * 0.4, 0.0);
            for (LivingEntity e : victims(boss, new AABB(center, center).inflate(PULL_RADIUS, 3, PULL_RADIUS))) {
                Vec3 to = center.subtract(e.position()).multiply(1, 0, 1);
                double d = to.length();
                if (d > PULL_RADIUS || d < 0.3) continue;
                Vec3 pull = to.normalize().scale(0.09 * (1.2 - d / PULL_RADIUS));
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurtMarked = true;
            }
            if (t == active - 1) {
                level.sendParticles(AllParticles.VOID_MOTE.get(), center.x, center.y + 0.6, center.z, 60, 1.2, 0.5, 1.2, 0.2);
                sound(boss, AllSounds.AMARA_LANCE.get(), 2f, 1.4f);
                for (LivingEntity e : victims(boss, new AABB(center, center).inflate(BURST_RADIUS, 2.5, BURST_RADIUS))) {
                    if (flat(e.position(), center) <= BURST_RADIUS) hit(boss, e, DAMAGE);
                }
            }
        }
    }

    /**
     * A line of black fire from her core along the ground. Strong light breaks it: anyone standing
     * where the block light is 12 or more is spared.
     */
    public static class EclipseLance extends BossAttack<AmaraEntity> {
        public static final float LENGTH = 30, HALF_WIDTH = 1.2f, DAMAGE = 14;
        Vec3 from;
        float yaw;

        public EclipseLance() {
            super("eclipse_lance", "lance", 35, 6, 50);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            from = floorAt(boss, boss.position());
            yaw = yawTo(from, target.position());
            TelegraphMarker.line(level(boss), from, yaw, HALF_WIDTH, LENGTH, TelegraphMarker.VIOLET, windup + 4);
            sound(boss, AllSounds.LUCIFER_CHARGE.get(), 2f, 0.5f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
            sound(boss, AllSounds.AMARA_LANCE.get(), 3f, 0.7f);
            for (int i = 0; i < LENGTH; i += 2) {
                Vec3 p = from.add(dir.scale(i));
                level.sendParticles(AllParticles.VOID_MOTE.get(), p.x, p.y + 0.8, p.z, 3, 0.3, 0.4, 0.3, 0.02);
            }
            Vec3 end = from.add(dir.scale(LENGTH));
            for (LivingEntity e : victims(boss, new AABB(from, end).inflate(HALF_WIDTH + 1, 3, HALF_WIDTH + 1))) {
                Vec3 rel = e.position().subtract(from);
                double along = rel.dot(dir), across = Math.abs(rel.x * dir.z - rel.z * dir.x);
                if (along < 0 || along > LENGTH || across > HALF_WIDTH + e.getBbWidth() / 2) continue;
                if (blockLight(level, e.getEyePosition()) >= AmaraBalance.LANCE_PROOF) {
                    level.sendParticles(AllParticles.GRACE.get(), e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                    continue;
                }
                hit(boss, e, DAMAGE);
            }
        }
    }

    /** Darkness falls in drops: circles around the target that leave pools of void behind. */
    public static class ShadowRain extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 2.0f, DAMAGE = 8;
        public static final int VOID_TICKS = 300;
        final List<Vec3> spots = new ArrayList<>();

        public ShadowRain() {
            super("shadow_rain", "rain", 25, 10, 30);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            spots.add(floorAt(boss, target.position()));
            for (int i = 0; i < 6; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, d = 3 + boss.getRandom().nextDouble() * 6;
                spots.add(floorAt(boss, target.position().add(Math.cos(a) * d, 0, Math.sin(a) * d)));
            }
            for (Vec3 s : spots) TelegraphMarker.circle(level(boss), s, RADIUS, TelegraphMarker.VIOLET, windup + 2);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            ServerLevel level = level(boss);
            Set<LivingEntity> struck = new HashSet<>();
            for (Vec3 s : spots) {
                level.sendParticles(AllParticles.VOID_MOTE.get(), s.x, s.y + 4, s.z, 30, 0.4, 2.0, 0.4, 0.05);
                for (LivingEntity e : victims(boss, new AABB(s, s).inflate(RADIUS, 2.5, RADIUS))) {
                    if (flat(e.position(), s) <= RADIUS && struck.add(e)) hit(boss, e, DAMAGE);
                }
                if (boss.getRandom().nextFloat() < 0.5f) VoidZone.spawn(level, s, RADIUS + 0.5f, VOID_TICKS);
            }
            sound(boss, AllSounds.AMARA_HURT.get(), 2f, 0.5f);
        }
    }

    /** Shades step out of her: a few, more for more challengers, never a crowd. */
    public static class SpawnShades extends BossAttack<AmaraEntity> {
        public static final int MAX_ALIVE = 6;
        final List<Vec3> spots = new ArrayList<>();

        public SpawnShades() {
            super("spawn_shades", "spawn", 30, 2, 30);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return alive(boss) >= MAX_ALIVE ? 0 : 1;
        }

        static int alive(AmaraEntity boss) {
            return level(boss).getEntitiesOfClass(AmaraShade.class, boss.getBoundingBox().inflate(48)).size();
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            int n = Math.min(MAX_ALIVE - alive(boss), 2 + boss.challengers().size());
            for (int i = 0; i < n; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2, d = 4 + boss.getRandom().nextDouble() * 5;
                Vec3 at = floorAt(boss, target.position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
                spots.add(at);
                TelegraphMarker.circle(level(boss), at, 1.0f, TelegraphMarker.VIOLET, windup);
            }
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            for (Vec3 at : spots) {
                AmaraShade shade = org.papiricoh.supernaturalcraft.registry.AllEntities.AMARA_SHADE.get().create(level(boss));
                if (shade == null) continue;
                shade.moveTo(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360, 0);
                shade.setTarget(target);
                level(boss).addFreshEntity(shade);
                level(boss).sendParticles(AllParticles.VOID_MOTE.get(), at.x, at.y + 1, at.z, 20, 0.3, 0.8, 0.3, 0.05);
            }
        }
    }

    // --- phase two ------------------------------------------------------------------------

    /** A tentacle rises over the target and crashes down. */
    public static class TentacleSlam extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 2.5f, DAMAGE = 12;
        Vec3 spot;

        public TentacleSlam() {
            super("tentacle_slam", "slam", 25, 4, 30);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            spot = floorAt(boss, target.position());
            TelegraphMarker.circle(level(boss), spot, RADIUS, TelegraphMarker.VIOLET, windup + 1);
            // The tentacle's slam lands at 0.6-0.7 of the effect's run: line that up with ACTIVE.
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SLAM, spot, 0, 0, Math.round(windup / 0.65f));
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            level(boss).sendParticles(AllParticles.VOID_MOTE.get(), spot.x, spot.y + 0.3, spot.z, 40, RADIUS * 0.5, 0.2, RADIUS * 0.5, 0.15);
            sound(boss, net.minecraft.sounds.SoundEvents.WARDEN_ATTACK_IMPACT, 3f, 0.6f);
            for (LivingEntity e : victims(boss, new AABB(spot, spot).inflate(RADIUS, 2.5, RADIUS))) {
                if (flat(e.position(), spot) > RADIUS) continue;
                hit(boss, e, DAMAGE);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                e.hurtMarked = true;
            }
        }
    }

    /** A tentacle sweeps the floor all the way round her. Jump it. */
    public static class TentacleSweep extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 11, DAMAGE = 10;
        float start;
        final Set<LivingEntity> struck = new HashSet<>();

        public TentacleSweep() {
            super("tentacle_sweep", "sweep", 30, 40, 30);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), RADIUS, TelegraphMarker.VIOLET, windup);
            start = boss.getRandom().nextFloat() * 360f;
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SWEEP, floorAt(boss, boss.position()), start, RADIUS, active);
        }

        /** The sweep's angle (degrees, Minecraft's x/z circle) at active tick {@code t}. */
        float angle(int t) {
            return start + 360f * t / active;
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            Vec3 c = boss.position();
            float a0 = angle(t), a1 = angle(t + 1);
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RADIUS + 1, 4, RADIUS + 1))) {
                if (struck.contains(e) || flat(e.position(), c) > RADIUS + 0.5) continue;
                double ea = Math.toDegrees(Math.atan2(e.getZ() - c.z, e.getX() - c.x));
                double rel = Mth.positiveModulo(ea - a0, 360);
                // Airborne (jumping) victims let it pass under them.
                if (rel <= a1 - a0 + 4 && e.onGround()) {
                    struck.add(e);
                    hit(boss, e, DAMAGE);
                    Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize().scale(1.2);
                    e.setDeltaMovement(push.x, 0.4, push.z);
                    e.hurtMarked = true;
                }
            }
        }
    }

    /** A tentacle seizes the target and drinks from it. Light makes it let go, and so does a struck cyst. */
    public static class Grasp extends BossAttack<AmaraEntity> {
        public static final int HOLD = 100;
        public static final float DRAIN = 2;
        Vec3 at;
        boolean released;
        float cystsBefore;

        public Grasp() {
            super("grasp", "grasp", 20, HOLD, 20);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.line(level(boss), floorAt(boss, boss.position()), yawTo(boss.position(), target.position()), 1.2f,
                    (float) flat(boss.position(), target.position()), TelegraphMarker.VIOLET, windup);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            at = target.position();
            cystsBefore = cystHealth(boss);
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.GRASP, at, 0, 0, active);
            sound(boss, net.minecraft.sounds.SoundEvents.WARDEN_TENDRIL_CLICKS, 3f, 0.6f);
        }

        static float cystHealth(AmaraEntity boss) {
            float h = 0;
            for (int i = 0; i < AmaraEntity.TENTACLES; i++) h += boss.partAlive(AmaraEntity.FIRST_TENTACLE + i) ? boss.partHealth(AmaraEntity.FIRST_TENTACLE + i) : 0;
            return h;
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (released) return;
            if (blockLight(level(boss), target.getEyePosition()) >= AmaraBalance.LANCE_PROOF || cystHealth(boss) < cystsBefore || !target.isAlive()) {
                released = true;
                level(boss).sendParticles(AllParticles.GRACE.get(), target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
                return;
            }
            target.teleportTo(at.x, target.getY(), at.z);
            target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
            target.hurtMarked = true;
            if (t % 10 == 0) hit(boss, target, DRAIN / 2);
        }

        @Override
        public boolean endEarly(AmaraEntity boss, LivingEntity target) {
            return released;
        }
    }

    /** Spikes of void burst from the ground in a line toward the target, one after another. */
    public static class BurrowingSpikes extends BossAttack<AmaraEntity> {
        public static final float DAMAGE = 8, STEP = 2.2f, RADIUS = 1.3f;
        final List<Vec3> line = new ArrayList<>();

        public BurrowingSpikes() {
            super("burrowing_spikes", "spikes", 30, 36, 25);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            Vec3 from = boss.position(), dir = target.position().subtract(from).multiply(1, 0, 1).normalize();
            for (int i = 0; i < 12; i++) {
                Vec3 p = floorAt(boss, from.add(dir.scale(4 + i * STEP)));
                line.add(p);
                TelegraphMarker.circle(level(boss), p, RADIUS, TelegraphMarker.VIOLET, windup + i * 3);
            }
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            if (t % 3 != 0 || t / 3 >= line.size()) return;
            Vec3 p = line.get(t / 3);
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.SPIKE, p, 0, 0, 20);
            for (LivingEntity e : victims(boss, new AABB(p, p).inflate(RADIUS, 3, RADIUS))) {
                if (flat(e.position(), p) > RADIUS) continue;
                hit(boss, e, DAMAGE);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0));
                e.hurtMarked = true;
            }
        }
    }

    /** Pools of void open under the challengers and spit shards into the air. */
    public static class VoidBloom extends BossAttack<AmaraEntity> {
        final List<Vec3> spots = new ArrayList<>();

        public VoidBloom() {
            super("void_bloom", "bloom", 30, 4, 30);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            List<LivingEntity> marks = new ArrayList<>(boss.challengers());
            if (marks.isEmpty()) marks.add(target);
            for (LivingEntity m : marks) {
                Vec3 p = floorAt(boss, m.position());
                spots.add(p);
                TelegraphMarker.circle(level(boss), p, 3f, TelegraphMarker.VIOLET, windup);
            }
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            for (Vec3 p : spots) {
                VoidZone.spawn(level(boss), p, 3f, 200);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    BossShard shard = new BossShard(boss.level(), boss, BossShard.Kind.VOID, 5f * boss.attackDamageMultiplier(), 0f, null);
                    shard.setPos(p.x, p.y + 0.5, p.z);
                    shard.shoot(Math.cos(a), 0.9, Math.sin(a), 0.5f, 0f);
                    boss.level().addFreshEntity(shard);
                }
            }
        }
    }

    // --- phase three ----------------------------------------------------------------------

    /** Her core's corona becomes a beam that turns a full circle at shin height. Jump it, or be far. */
    public static class CoronaFlare extends BossAttack<AmaraEntity> {
        public static final float LENGTH = 20, DAMAGE = 7;
        float start;
        final Set<LivingEntity> struck = new HashSet<>();

        public CoronaFlare() {
            super("corona_flare", "flare", 30, 80, 40);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            start = (float) Math.toDegrees(Math.atan2(target.getZ() - boss.getZ(), target.getX() - boss.getX())) + 90f;
            TelegraphMarker.ring(level(boss), floorAt(boss, boss.position()), LENGTH, TelegraphMarker.GOLD, windup);
            sound(boss, AllSounds.LUCIFER_CHARGE.get(), 3f, 0.6f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.FLARE, floorAt(boss, boss.position()), start, LENGTH, active);
        }

        @Override
        public void tickActive(AmaraEntity boss, LivingEntity target, int t) {
            float a = start + 360f * t / active;
            Vec3 dir = new Vec3(Math.cos(Math.toRadians(a)), 0, Math.sin(Math.toRadians(a)));
            Vec3 c = boss.position();
            if (t % 10 == 0) struck.clear();
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(LENGTH, 4, LENGTH))) {
                Vec3 rel = e.position().subtract(c).multiply(1, 0, 1);
                double along = rel.dot(dir), across = Math.abs(rel.x * dir.z - rel.z * dir.x);
                if (along < 0 || along > LENGTH || across > 1.0 + e.getBbWidth() / 2 || !e.onGround() || !struck.add(e)) continue;
                hit(boss, e, DAMAGE);
                e.igniteForSeconds(2);
            }
        }
    }

    /**
     * Totality: the whole arena goes dark at once. Only the lit wells hold it back; anyone further
     * than {@link #SAFE} blocks from a burning well is struck.
     */
    public static class Totality extends BossAttack<AmaraEntity> {
        public static final float SAFE = 4, DAMAGE = 16;

        public Totality() {
            super("totality", "totality", 60, 4, 50);
        }

        @Override
        public float weight(AmaraEntity boss, LivingEntity target) {
            return boss.litWells() > 0 ? 1 : 0.3f;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            for (BlockPos w : boss.wells()) {
                var s = level(boss).getBlockState(w);
                if (s.is(AllBlocks.LIGHT_WELL.get()) && s.getValue(LightWellBlock.LIT)) {
                    TelegraphMarker.circle(level(boss), Vec3.atBottomCenterOf(w), SAFE, TelegraphMarker.SAFE, windup);
                }
            }
            for (var p : boss.challengers()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.supernaturalcraft.amara.totality")
                        .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
            sound(boss, AllSounds.AMARA_ROAR.get(), 4f, 0.4f);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.RING_OUT, floorAt(boss, boss.position()), 0,
                    boss.arena() != null ? boss.arena().radius() : 24, 20);
            strike(boss);
        }

        /** Strikes everyone not sheltered by a lit well. Public for tests; returns who was hit. */
        public static List<LivingEntity> strike(AmaraEntity boss) {
            ServerLevel level = level(boss);
            List<Vec3> lit = new ArrayList<>();
            for (BlockPos w : boss.wells()) {
                var s = level.getBlockState(w);
                if (s.is(AllBlocks.LIGHT_WELL.get()) && s.getValue(LightWellBlock.LIT)) lit.add(Vec3.atBottomCenterOf(w));
            }
            List<LivingEntity> hit = new ArrayList<>();
            double r = boss.arena() != null ? boss.arena().radius() + 2 : 26;
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(r, 8, r))) {
                boolean safe = lit.stream().anyMatch(w -> flat(w, e.position()) <= SAFE);
                if (safe) continue;
                hit(boss, e, DAMAGE);
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 100, 0));
                hit.add(e);
            }
            return hit;
        }
    }

    /** The black sun pulls everything to it, then bursts. Get far away while you still can. */
    public static class BlackSunCollapse extends BossAttack<AmaraEntity> {
        public static final float RADIUS = 9, PULL = 20, DAMAGE = 15;

        public BlackSunCollapse() {
            super("black_sun_collapse", "collapse", 50, 4, 50);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            TelegraphMarker.circle(level(boss), floorAt(boss, boss.position()), RADIUS, TelegraphMarker.VIOLET, windup);
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.RING_IN, floorAt(boss, boss.position()), 0, PULL, windup);
        }

        @Override
        public void tickWindup(AmaraEntity boss, LivingEntity target, int t) {
            Vec3 c = boss.position();
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(PULL, 4, PULL))) {
                Vec3 to = c.subtract(e.position()).multiply(1, 0, 1);
                double d = to.length();
                if (d > PULL || d < 0.5) continue;
                e.setDeltaMovement(e.getDeltaMovement().add(to.normalize().scale(0.035)));
                e.hurtMarked = true;
            }
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            Vec3 c = boss.position();
            level(boss).sendParticles(ParticleTypes.FLASH, c.x, c.y + AmaraEntity.MASS_CENTER, c.z, 2, 0, 0, 0, 0);
            level(boss).sendParticles(AllParticles.VOID_MOTE.get(), c.x, c.y + 2, c.z, 120, RADIUS * 0.5, 1.5, RADIUS * 0.5, 0.3);
            sound(boss, AllSounds.AMARA_LANCE.get(), 4f, 0.5f);
            for (LivingEntity e : victims(boss, boss.getBoundingBox().inflate(RADIUS, 5, RADIUS))) {
                if (flat(e.position(), c) > RADIUS) continue;
                hit(boss, e, DAMAGE);
                Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize().scale(1.6);
                e.setDeltaMovement(push.x, 0.6, push.z);
                e.hurtMarked = true;
            }
        }
    }

    // --- her final form -------------------------------------------------------------------

    /** She steps through the dark and strikes from behind. */
    public static class VoidStep extends BossAttack<AmaraEntity> {
        public static final float DAMAGE = 12, REACH = 3.5f;
        Vec3 behind;

        public VoidStep() {
            super("void_step", "step", 15, 6, 25);
        }

        @Override
        public boolean movesBoss() {
            return true;
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
            behind = floorAt(boss, target.position().subtract(look.scale(2.5)));
            TelegraphMarker.circle(level(boss), behind, 1.2f, TelegraphMarker.VIOLET, windup);
            level(boss).sendParticles(AllParticles.VOID_MOTE.get(), boss.getX(), boss.getY() + 2.5, boss.getZ(), 30, 0.5, 1.5, 0.5, 0.05);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            boss.teleportTo(behind.x, behind.y, behind.z);
            level(boss).sendParticles(AllParticles.VOID_MOTE.get(), behind.x, behind.y + 2.5, behind.z, 30, 0.5, 1.5, 0.5, 0.05);
            if (boss.distanceTo(target) <= REACH + 1) hit(boss, target, DAMAGE);
        }
    }

    /**
     * Unmaking: the floor of her arena turns to void wherever it is darker than light level 8.
     * Stand in the light, or in your own.
     */
    public static class Unmaking extends BossAttack<AmaraEntity> {
        public static final int HOLD = 100;
        public static final float HARM = 3, WELL_SHELTER = 6;

        public Unmaking() {
            super("unmaking", "unmake", 60, 4, 60);
        }

        @Override
        public void onWindup(AmaraEntity boss, LivingEntity target) {
            for (var p : boss.challengers()) {
                p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.supernaturalcraft.amara.unmaking")
                        .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
            AmaraFx.send(boss, org.papiricoh.supernaturalcraft.network.AmaraFxPayload.RING_OUT, floorAt(boss, boss.position()), 0,
                    boss.arena() != null ? boss.arena().radius() : 24, windup);
        }

        @Override
        public void onActive(AmaraEntity boss, LivingEntity target) {
            unmake(boss);
        }

        /** Lays void over every dark stretch of floor. Public for tests; returns how many pools. */
        public static int unmake(AmaraEntity boss) {
            ServerLevel level = level(boss);
            ArenaController arena = boss.arena();
            if (arena == null) return 0;
            int n = 0, r = arena.radius() - 1;
            Vec3 c = arena.centerVec();
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (dx * dx + dz * dz > r * r) continue;
                    Vec3 p = floorAt(boss, c.add(dx, 0, dz));
                    // A burning well holds its ground, and so does any other light of 8 or more.
                    if (boss.nearLitWell(p, WELL_SHELTER) || blockLight(level, p.add(0, 0.5, 0)) >= AmaraBalance.LIT) continue;
                    VoidZone.spawn(level, p, 2.4f, HOLD, HARM);
                    n++;
                }
            }
            return n;
        }
    }
}
