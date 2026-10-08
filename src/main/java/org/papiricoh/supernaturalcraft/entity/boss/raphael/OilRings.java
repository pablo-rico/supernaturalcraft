package org.papiricoh.supernaturalcraft.entity.boss.raphael;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.HolyOilFireBlock;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseGround;
import org.papiricoh.supernaturalcraft.entity.boss.raphael.arena.HouseLayout;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

import java.util.Arrays;

/**
 * The four rings of holy oil on the floor of Raphael's house (v0.16): poured as {@code holy_oil_slick} through the arena (so
 * they come back with it), lit by fire into {@code holy_oil_fire} for {@link RaphaelBalance#RING_BURN_TICKS}, spent until the
 * next phase. Lighting a ring: flint and steel or a fire charge used on its oil ({@link HolyOilSlickBlock}), a burning arrow or
 * a fireball landing in it, or a fire lit beside it.
 */
public final class OilRings {

    public enum State { NONE, LAID, BURNING, SPENT }

    private final State[] states = new State[HouseLayout.RINGS.size()];
    private final long[] burnsUntil = new long[HouseLayout.RINGS.size()];

    public OilRings() {
        Arrays.fill(states, State.NONE);
    }

    public State state(int ring) {
        return states[ring];
    }

    public boolean anyBurning() {
        for (State s : states) if (s == State.BURNING) return true;
        return false;
    }

    /** Pours every ring afresh (the start of the fight and of each phase): any fire still burning is put out. */
    public void layAll(ServerLevel level, ArenaController arena, HouseGround ground) {
        BlockState slick = AllBlocks.HOLY_OIL_SLICK.get().defaultBlockState();
        for (int i = 0; i < states.length; i++) {
            int laid = 0;
            for (HouseLayout.Spot s : HouseLayout.ringCells(i)) {
                BlockPos pos = ground.onFloor(s);
                BlockState here = level.getBlockState(pos);
                if (here.is(AllBlocks.HOLY_OIL_SLICK.get()) || arena.mutate(level, pos, slick, 0)) laid++;
            }
            states[i] = laid > 0 ? State.LAID : State.NONE;
            burnsUntil[i] = 0;
        }
    }

    /** Lights ring {@code ring}, if its oil lies there. @return whether it caught */
    public boolean ignite(ServerLevel level, ArenaController arena, HouseGround ground, int ring) {
        if (ring < 0 || ring >= states.length || states[ring] != State.LAID) return false;
        BlockState fire = AllBlocks.HOLY_OIL_FIRE.get().defaultBlockState();
        int lit = 0;
        for (HouseLayout.Spot s : HouseLayout.ringCells(ring)) {
            BlockPos pos = ground.onFloor(s);
            if (!level.getBlockState(pos).is(AllBlocks.HOLY_OIL_SLICK.get())) continue;
            if (arena.mutate(level, pos, fire, RaphaelBalance.RING_BURN_TICKS)) lit++;
        }
        if (lit == 0) return false;
        states[ring] = State.BURNING;
        burnsUntil[ring] = level.getGameTime() + RaphaelBalance.RING_BURN_TICKS;
        Vec3 c = centre(ground, ring);
        level.playSound(null, BlockPos.containing(c), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.2, c.z, 40, 1.4, 0.1, 1.4, 0.02);
        return true;
    }

    /** Puts ring {@code ring} out (he broke free of it): spent until the next phase. */
    public void extinguish(ServerLevel level, ArenaController arena, HouseGround ground, int ring) {
        if (ring < 0 || ring >= states.length) return;
        for (HouseLayout.Spot s : HouseLayout.ringCells(ring)) {
            BlockPos pos = ground.onFloor(s);
            if (level.getBlockState(pos).getBlock() instanceof HolyOilFireBlock) arena.revert(level, pos);
        }
        states[ring] = State.SPENT;
        Vec3 c = centre(ground, ring);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.3, c.z, 30, 1.4, 0.2, 1.4, 0.02);
    }

    /** A burning ring whose time is up is spent. */
    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (int i = 0; i < states.length; i++) if (states[i] == State.BURNING && now >= burnsUntil[i]) states[i] = State.SPENT;
    }

    /** Fire near a laid ring lights it: a burning projectile or a fireball over it, a fire lit beside it. */
    public void checkSparks(ServerLevel level, ArenaController arena, HouseGround ground) {
        for (int i = 0; i < states.length; i++) {
            if (states[i] != State.LAID) continue;
            HouseLayout.Spot c = HouseLayout.RINGS.get(i);
            BlockPos lo = ground.onFloor(new HouseLayout.Spot(c.dx() - HouseLayout.RING_HALF, c.dz() - HouseLayout.RING_HALF));
            BlockPos hi = ground.onFloor(new HouseLayout.Spot(c.dx() + HouseLayout.RING_HALF, c.dz() + HouseLayout.RING_HALF));
            AABB box = new AABB(Vec3.atLowerCornerOf(lo), Vec3.atLowerCornerOf(hi).add(1, 1.5, 1)).inflate(0.3, 0, 0.3);
            boolean spark = !level.getEntitiesOfClass(Projectile.class, box, OilRings::burning).isEmpty();
            if (!spark) {
                for (BlockPos p : BlockPos.betweenClosed(lo.offset(-1, 0, -1), hi.offset(1, 0, 1))) {
                    if (level.getBlockState(p).getBlock() instanceof BaseFireBlock) {
                        spark = true;
                        break;
                    }
                }
            }
            if (spark) ignite(level, arena, ground, i);
        }
    }

    static boolean burning(Projectile p) {
        return p.isAlive() && (p.isOnFire() || p instanceof SmallFireball || p instanceof LargeFireball
                || p instanceof AbstractHurtingProjectile h && h.isOnFire());
    }

    /** The ring {@code pos} (its oil or the floor inside it) belongs to, or -1. */
    public static int ringAt(HouseGround ground, BlockPos pos) {
        BlockPos o = ground.origin();
        return HouseLayout.ringAt(pos.getX() - o.getX(), pos.getZ() - o.getZ());
    }

    /** The middle of ring {@code ring}'s floor (where one stands inside it). */
    public static Vec3 centre(HouseGround ground, int ring) {
        return Vec3.atBottomCenterOf(ground.onFloor(HouseLayout.RINGS.get(ring)));
    }

    /** The laid ring nearest {@code from} within {@code reach}, or -1. */
    public int nearestLaid(HouseGround ground, Vec3 from, double reach) {
        int best = -1;
        double bestD = reach * reach;
        for (int i = 0; i < states.length; i++) {
            if (states[i] != State.LAID) continue;
            double d = centre(ground, i).distanceToSqr(from);
            if (d <= bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    /** Test hook: what the rings hold now, by index. */
    public @Nullable State[] states() {
        return states.clone();
    }
}
