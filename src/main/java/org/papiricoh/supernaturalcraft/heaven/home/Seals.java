package org.papiricoh.supernaturalcraft.heaven.home;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The celestial seals across Heaven's doorways (v0.18): the clinical wing's door, Naomi's lift and the home's door.
 * <p><b>Rule</b> (shared with the layouts): a doorway's seal is any group of face-connected {@code celestial_seal} blocks that
 * touches its anchor point (the anchor or a block next to it, up to two above). A layout may build the seal into the doorway in
 * any shape; if it has none, the plot writer puts a 1x2 seal on the anchor. Opening it clears the whole group.
 */
public final class Seals {

    /** The most blocks one seal may span. */
    public static final int MAX_BLOCKS = 128;

    private Seals() {
    }

    /** A seal block touching {@code anchor}, or null. */
    public static @Nullable BlockPos near(ServerLevel level, BlockPos anchor) {
        for (int dy = 0; dy <= 2; dy++) {
            for (BlockPos p : BlockPos.betweenClosed(anchor.offset(-1, dy, -1), anchor.offset(1, dy, 1))) {
                if (level.getBlockState(p).is(AllBlocks.CELESTIAL_SEAL.get())) return p.immutable();
            }
        }
        return null;
    }

    /** Puts a 1x2 seal on {@code anchor} unless the doorway already has one. */
    public static void ensure(ServerLevel level, BlockPos anchor) {
        if (near(level, anchor) != null) return;
        BlockState seal = AllBlocks.CELESTIAL_SEAL.get().defaultBlockState();
        level.setBlock(anchor, seal, Block.UPDATE_CLIENTS);
        level.setBlock(anchor.above(), seal, Block.UPDATE_CLIENTS);
    }

    /** Every block of the seal across {@code anchor}'s doorway (empty if there is none). */
    public static List<BlockPos> find(ServerLevel level, BlockPos anchor) {
        BlockPos start = near(level, anchor);
        List<BlockPos> out = new ArrayList<>();
        if (start == null) return out;
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        seen.add(start);
        while (!todo.isEmpty() && out.size() < MAX_BLOCKS) {
            BlockPos p = todo.poll();
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n) && level.getBlockState(n).is(AllBlocks.CELESTIAL_SEAL.get())) todo.add(n);
            }
        }
        return out;
    }

    /** Opens the seal across {@code anchor}'s doorway: its blocks melt into light. @return the blocks it had */
    public static List<BlockPos> open(ServerLevel level, BlockPos anchor) {
        List<BlockPos> blocks = find(level, anchor);
        for (BlockPos p : blocks) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        }
        if (!blocks.isEmpty()) level.playSound(null, anchor, AllSounds.heaven("heaven.seal_open"), SoundSource.BLOCKS, 1.2f, 1.0f);
        return blocks;
    }
}
