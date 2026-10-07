package org.papiricoh.supernaturalcraft.author;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashMap;
import java.util.Map;

/** Puts {@link CabinLayout} into the world, touching only what lies inside the box it is given. */
public final class CabinBuilder {

    private static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};
    private static final Map<String, BlockState> PARSED = new HashMap<>();

    private CabinBuilder() {
    }

    public static Rotation rotation(int quarter) {
        return ROTATIONS[Math.floorMod(quarter, 4)];
    }

    /** A plan point in the world. */
    public static BlockPos at(BlockPos origin, int quarter, int[] local) {
        int[] w = CabinLayout.toWorld(local, origin.getX(), origin.getY(), origin.getZ(), quarter);
        return new BlockPos(w[0], w[1], w[2]);
    }

    public static BoundingBox box(BlockPos origin, int quarter) {
        int[] b = CabinLayout.box(origin.getX(), origin.getY(), origin.getZ(), quarter);
        return new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    /** The floor centre of a cabin standing on the ground at column ({@code x}, {@code z}). */
    public static BlockPos originOn(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Builds the whole cabin now (commands, the Author's own repair, previews and tests). */
    public static void placeAt(ServerLevel level, BlockPos origin, int quarter) {
        BoundingBox box = box(origin, quarter);
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        build(level, box, origin, quarter);
    }

    public static void build(WorldGenLevel level, BoundingBox box, BlockPos origin, int quarter) {
        Rotation rot = rotation(quarter);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // Clear the footprint above the floor (grass, leaves, snow) and stand the floor on a foundation down to the ground.
        for (int x = CabinLayout.MIN_X; x <= CabinLayout.MAX_X; x++) {
            for (int z = CabinLayout.MIN_Z; z <= CabinLayout.MAX_Z; z++) {
                int[] w = CabinLayout.toWorld(new int[]{x, 0, z}, origin.getX(), origin.getY(), origin.getZ(), quarter);
                for (int y = 1; y <= CabinLayout.CLEAR_TO; y++) {
                    p.set(w[0], w[1] + y, w[2]);
                    if (box.isInside(p) && !level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                boolean under = Math.abs(x) <= 4 && z >= -5 && z <= 9 || x <= -5 && z >= -1 && z <= 0;
                if (!under) continue;
                for (int d = 1; d <= CabinLayout.FOUNDATION; d++) {
                    p.set(w[0], w[1] - d, w[2]);
                    if (!box.isInside(p)) break;
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced()) break;
                    level.setBlock(p, (Math.abs(x) == 4 || z == -5 || z == 5 || z == 8) ? Blocks.COBBLESTONE.defaultBlockState()
                            : Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (CabinLayout.Cell c : CabinLayout.cells()) {
            int[] w = CabinLayout.toWorld(new int[]{c.x(), c.y(), c.z()}, origin.getX(), origin.getY(), origin.getZ(), quarter);
            p.set(w[0], w[1], w[2]);
            if (!box.isInside(p)) continue;
            level.setBlock(p, state(c.state()).rotate(level, p, rot), Block.UPDATE_CLIENTS);
        }
    }

    /** A block-state string of the plan, parsed once. */
    public static BlockState state(String s) {
        synchronized (PARSED) {
            return PARSED.computeIfAbsent(s, k -> {
                try {
                    return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), k, false).blockState();
                } catch (CommandSyntaxException e) {
                    throw new IllegalStateException("Bad block state in the cabin's plan: " + k, e);
                }
            });
        }
    }
}
