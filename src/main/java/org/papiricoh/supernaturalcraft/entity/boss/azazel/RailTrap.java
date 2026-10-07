package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTerrain;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Samuel Colt's rails at the heart of Azazel's arena: laid when he rises, they hold him the moment
 * he is inside them while they are charged. Then the iron goes cold and lights up again, rail by
 * rail, until it can hold him once more. All of it goes through the arena, so it is put back after.
 */
public final class RailTrap {

    private final List<BlockPos> rails = new ArrayList<>();
    private final List<RailTrapLayout.Shape> shapes = new ArrayList<>();
    private boolean built;
    private boolean charged = true;
    private long heldUntil;
    private long spentAt, rechargeAt;
    private int lit = -1;

    public boolean built() {
        return built;
    }

    public boolean charged() {
        return built && charged;
    }

    public boolean holding(long now) {
        return now < heldUntil;
    }

    public List<BlockPos> rails() {
        return rails;
    }

    /** Lays the rails round the arena's centre, on whatever ground is there. */
    public void build(ServerLevel level, ArenaController arena) {
        if (built) return;
        built = true;
        charged = true;
        BlockPos c = arena.center();
        for (RailTrapLayout.Cell cell : RailTrapLayout.cells()) {
            BlockPos floor = ArenaTerrain.surface(level, arena, c.getX() + cell.dx(), c.getZ() + cell.dz());
            if (floor == null) continue;
            BlockPos at = floor.above();
            BlockState rail = AllBlocks.COLT_RAIL.get().defaultBlockState().setValue(ColtRailBlock.SHAPE, cell.shape());
            if (arena.mutate(level, at, rail, 0)) {
                rails.add(at);
                shapes.add(cell.shape());
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.05);
            }
        }
        level.playSound(null, c, AllSounds.RAIL_TRAP_SNAP.get(), SoundSource.HOSTILE, 2.5f, 0.7f);
    }

    /** Whether {@code at} stands inside the trap's circle (and near its height). */
    public static boolean inside(ArenaController arena, Vec3 at) {
        Vec3 c = arena.centerVec();
        return RailTrapLayout.inside(at.x - c.x, at.z - c.z) && Math.abs(at.y - c.y) < 4;
    }

    /**
     * Snaps shut on him: the iron goes cold and starts to charge again.
     *
     * @return false if the rails are not charged
     */
    public boolean spring(ServerLevel level, ArenaController arena, long now, int holdTicks, int rechargeTicks) {
        if (!charged()) return false;
        charged = false;
        heldUntil = now + holdTicks;
        spentAt = now + holdTicks;
        rechargeAt = spentAt + rechargeTicks;
        lit = 0;
        setAll(level, arena, false, rails.size());
        Vec3 c = arena.centerVec();
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x + Math.cos(a) * RailTrapLayout.RADIUS, c.y + 0.2,
                    c.z + Math.sin(a) * RailTrapLayout.RADIUS, 2, 0.1, 0.3, 0.1, 0.05);
        }
        level.playSound(null, arena.center(), AllSounds.RAIL_TRAP_SNAP.get(), SoundSource.HOSTILE, 3.0f, 0.6f);
        return true;
    }

    /** Lights the rails back up, one after another, over the recharge; all at once when it is done. */
    public void tick(ServerLevel level, ArenaController arena, long now) {
        if (!built || charged) return;
        if (now >= rechargeAt) {
            charged = true;
            lit = -1;
            setAll(level, arena, true, rails.size());
            level.playSound(null, arena.center(), AllSounds.RAIL_TRAP_RECHARGE.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            Vec3 c = arena.centerVec();
            level.sendParticles(AllParticles.SIGIL.get(), c.x, c.y + 0.3, c.z, 60, RailTrapLayout.RADIUS * 0.6, 0.1, RailTrapLayout.RADIUS * 0.6, 0.02);
            return;
        }
        if (now < spentAt) return;
        // While charging, the lit rails are only a sign of progress: the trap holds nothing until it is whole.
        int want = (int) (rails.size() * (now - spentAt) / Math.max(1, rechargeAt - spentAt));
        while (lit < want && lit < rails.size()) {
            BlockPos at = rails.get(lit);
            arena.mutate(level, at, railState(lit, true), 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.02);
            lit++;
        }
    }

    /** Test hook: finishes the recharge at once. */
    public void forceRecharge(ServerLevel level, ArenaController arena) {
        heldUntil = 0;
        spentAt = 0;
        rechargeAt = 0;
        tick(level, arena, level.getGameTime());
    }

    private BlockState railState(int i, boolean on) {
        return AllBlocks.COLT_RAIL.get().defaultBlockState().setValue(ColtRailBlock.SHAPE, shapes.get(i)).setValue(ColtRailBlock.CHARGED, on);
    }

    private void setAll(ServerLevel level, ArenaController arena, boolean on, int count) {
        for (int i = 0; i < count; i++) {
            if (level.getBlockState(rails.get(i)).is(AllBlocks.COLT_RAIL.get())) arena.mutate(level, rails.get(i), railState(i, on), 0);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Built", built);
        tag.putBoolean("Charged", charged);
        tag.putLong("HeldUntil", heldUntil);
        tag.putLong("SpentAt", spentAt);
        tag.putLong("RechargeAt", rechargeAt);
        tag.putInt("Lit", lit);
        long[] pos = new long[rails.size()];
        int[] shape = new int[rails.size()];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = rails.get(i).asLong();
            shape[i] = shapes.get(i).ordinal();
        }
        tag.putLongArray("Rails", pos);
        tag.putIntArray("Shapes", shape);
        return tag;
    }

    public void load(CompoundTag tag) {
        built = tag.getBoolean("Built");
        charged = !tag.contains("Charged") || tag.getBoolean("Charged");
        heldUntil = tag.getLong("HeldUntil");
        spentAt = tag.getLong("SpentAt");
        rechargeAt = tag.getLong("RechargeAt");
        lit = tag.getInt("Lit");
        rails.clear();
        shapes.clear();
        long[] pos = tag.getLongArray("Rails");
        int[] shape = tag.getIntArray("Shapes");
        for (int i = 0; i < pos.length && i < shape.length; i++) {
            rails.add(BlockPos.of(pos[i]));
            shapes.add(RailTrapLayout.Shape.values()[shape[i]]);
        }
    }
}
