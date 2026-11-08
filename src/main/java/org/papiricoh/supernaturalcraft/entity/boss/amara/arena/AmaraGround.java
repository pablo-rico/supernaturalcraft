package org.papiricoh.supernaturalcraft.entity.boss.amara.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;

import java.util.ArrayList;
import java.util.List;

/**
 * Amara's ground (v0.42), the runtime side of {@link FogTownLayout} and {@link VoidIslands}: the town written over her arena
 * while she rises, then, in phase V, the ground crumbling ring by ring into the Void and the islands sinking. Every block goes
 * through {@link ArenaController#mutate} (the arena gives it all back), a few hundred a tick ({@link HorsemenGround}). The floor
 * is the block under the arena's centre ({@link #origin}).
 */
public final class AmaraGround {

    /** Ticks between two rings crumbling once the Void begins. */
    public static final int RING_TICKS = 12;

    private final BlockPos origin;
    private final int radius;
    private @Nullable HorsemenGround writing;
    private final List<HorsemenGround> queue = new ArrayList<>();
    private boolean town, crumbling;
    private int ring, ringTimer;
    private final int[] sunk = new int[VoidIslands.ISLANDS.size()];

    private AmaraGround(BlockPos origin, int radius) {
        this.origin = origin;
        this.radius = radius;
    }

    public static AmaraGround pin(ArenaController arena) {
        return new AmaraGround(arena.center().below(), arena.radius() - 1);
    }

    public BlockPos origin() {
        return origin;
    }

    private HorsemenGround cells(List<ArenaCell> cells) {
        List<BlockPos> positions = new ArrayList<>(cells.size());
        List<String> blocks = new ArrayList<>(cells.size());
        for (ArenaCell c : cells) {
            positions.add(origin.offset(c.dx(), c.dy(), c.dz()));
            blocks.add(c.block());
        }
        return HorsemenGround.fixed(positions, blocks);
    }

    /**
     * Whether the town is written at all: not in the GameTest server, whose fights stand on small templates side by side (a test
     * that wants it turns it on).
     */
    public static boolean townEnabled = !net.neoforged.neoforge.gametest.GameTestHooks.isGametestServer();

    /** Starts writing the town (once). */
    public void buildTown() {
        if (town || !townEnabled) return;
        town = true;
        queue.add(cells(FogTownLayout.plan()));
    }

    /** Phase V: the ground starts to come undone, from the edge inward. */
    public void beginVoid() {
        if (crumbling) return;
        crumbling = true;
        ring = 0;
        ringTimer = 0;
        // The islands are cut loose: whatever stood on them goes, and so does the ground under their few blocks of rock.
        List<ArenaCell> loose = new ArrayList<>();
        for (int i = 0; i < VoidIslands.ISLANDS.size(); i++) {
            for (int[] c : VoidIslands.columns(i)) {
                if (c[0] * c[0] + c[1] * c[1] > radius * radius) continue;
                for (int y = FogTownLayout.TOP; y >= 1; y--) loose.add(new ArenaCell(c[0], y, c[1], FogTownLayout.AIR));
                for (int y = -VoidIslands.THICKNESS - 1; y >= -VoidIslands.DEPTH; y--) loose.add(new ArenaCell(c[0], y, c[1], FogTownLayout.AIR));
            }
        }
        queue.add(0, cells(loose));
    }

    public boolean crumbling() {
        return crumbling;
    }

    /** One tick of writing. @return the centre of the ring that crumbled this tick (for its FX), or null */
    public @Nullable Integer tick(ServerLevel level, ArenaController arena) {
        Integer fell = null;
        if (crumbling && ring < VoidIslands.rings(radius) && --ringTimer <= 0) {
            ringTimer = RING_TICKS;
            List<ArenaCell> air = new ArrayList<>();
            for (int[] c : VoidIslands.falling(ring, radius)) {
                for (int y = FogTownLayout.TOP; y >= -VoidIslands.DEPTH; y--) air.add(new ArenaCell(c[0], y, c[1], FogTownLayout.AIR));
            }
            if (!air.isEmpty()) queue.add(cells(air));
            fell = ring;
            ring++;
        }
        if (writing == null || writing.done()) writing = queue.isEmpty() ? null : queue.remove(0);
        if (writing != null) writing.tick(level, arena);
        return fell;
    }

    /** Tests and previews: everything queued, at once. */
    public void finish(ServerLevel level, ArenaController arena) {
        while (crumbling && ring < VoidIslands.rings(radius)) {
            ringTimer = 0;
            tick(level, arena);
        }
        if (writing != null) writing.finish(level, arena);
        for (HorsemenGround g : queue) g.finish(level, arena);
        queue.clear();
        writing = null;
    }

    public boolean idle() {
        return (writing == null || writing.done()) && queue.isEmpty();
    }

    /** The island a living thing stands on that may still sink, or -1. */
    public int sinkableUnder(LivingEntity e) {
        int x = e.getBlockX() - origin.getX(), z = e.getBlockZ() - origin.getZ();
        int i = VoidIslands.islandAt(x, z);
        return i >= 0 && crumbling && VoidIslands.maySink(i, sunk[i]) ? i : -1;
    }

    public Vec3 islandCentre(int i) {
        VoidIslands.Island is = VoidIslands.ISLANDS.get(i);
        return new Vec3(origin.getX() + is.cx() + 0.5, origin.getY() + 1 - sunk[i], origin.getZ() + is.cz() + 0.5);
    }

    /**
     * Island {@code i} sinks one step: each column's top block goes and a block is added under its bottom (so it keeps its
     * thickness and moves down). Past {@link VoidIslands#SINK_STEPS} it crumbles away entirely.
     */
    public void sink(ServerLevel level, @Nullable ArenaController arena, int i) {
        if (arena == null || !VoidIslands.maySink(i, sunk[i])) return;
        int s = sunk[i];
        boolean last = s + 1 >= VoidIslands.SINK_STEPS;
        for (int[] c : VoidIslands.columns(i)) {
            BlockPos top = origin.offset(c[0], -s, c[1]);
            BlockPos bottom = origin.offset(c[0], -s - VoidIslands.THICKNESS - 1, c[1]);
            BlockState under = level.getBlockState(top);
            if (last) {
                for (int y = 0; y < VoidIslands.THICKNESS + 1; y++) arena.mutate(level, top.below(y), Blocks.AIR.defaultBlockState(), 0);
                continue;
            }
            BlockState stone = under.isAir() ? Blocks.BLACKSTONE.defaultBlockState() : under;
            arena.mutate(level, bottom, Blocks.BLACKSTONE.defaultBlockState(), 0);
            arena.mutate(level, top.below(), stone, 0);
            arena.mutate(level, top, Blocks.AIR.defaultBlockState(), 0);
        }
        sunk[i] = s + 1;
    }

    public int sunk(int i) {
        return sunk[i];
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("Town", town);
        tag.putBoolean("Crumbling", crumbling);
        tag.putInt("Ring", ring);
        tag.putIntArray("Sunk", sunk);
    }

    /** After a reload: what was written stays written; what was still queued is dropped (the arena restores it all anyway). */
    public void load(CompoundTag tag) {
        town = tag.getBoolean("Town");
        crumbling = tag.getBoolean("Crumbling");
        ring = tag.getInt("Ring");
        int[] s = tag.getIntArray("Sunk");
        System.arraycopy(s, 0, sunk, 0, Math.min(s.length, sunk.length));
    }
}
