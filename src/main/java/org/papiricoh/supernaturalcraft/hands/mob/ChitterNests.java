package org.papiricoh.supernaturalcraft.hands.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;

/**
 * The Chitters' nests (v0.42): where a pack rises somewhere new it weaves one on the cave floor (patches of hair and resin over
 * the stone, a few egg sacs on top), unless one already lies within {@link ChittersRules#NEST_SPACING}. Only stone and soil are
 * woven over; the eggs hatch on their own while the Chitters are awake ({@link ChitterEggBlock}).
 */
public final class ChitterNests {

    static final int PATCH = 8, EGGS_MAX = 3;

    private ChitterNests() {
    }

    /** Whether a nest block lies within {@link ChittersRules#NEST_SPACING} of {@code at}. */
    public static boolean nestNear(ServerLevel level, BlockPos at) {
        int r = ChittersRules.NEST_SPACING;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-r, -r / 2, -r), at.offset(r, r / 2, r))) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            if (s.is(HandsRegistry.CHITTER_NEST.get()) || s.is(HandsRegistry.CHITTER_EGG.get())) return true;
        }
        return false;
    }

    /** Weaves a nest on the floor round {@code at} (if none is near). @return how many eggs were laid */
    public static int weave(ServerLevel level, BlockPos at, RandomSource random) {
        if (nestNear(level, at)) return 0;
        BlockState nest = HandsRegistry.CHITTER_NEST.get().defaultBlockState(), egg = HandsRegistry.CHITTER_EGG.get().defaultBlockState();
        int eggs = 0;
        for (int i = 0; i < PATCH * 3; i++) {
            BlockPos p = at.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            BlockPos floor = floorUnder(level, p);
            if (floor == null) continue;
            level.setBlock(floor, nest, Block.UPDATE_CLIENTS);
            if (eggs < EGGS_MAX && random.nextInt(3) == 0 && level.getBlockState(floor.above()).isAir()) {
                level.setBlock(floor.above(), egg, Block.UPDATE_CLIENTS);
                eggs++;
            }
        }
        return eggs;
    }

    /** The stone or soil floor block a few cells under {@code p} with open air above it, or null. */
    private static BlockPos floorUnder(ServerLevel level, BlockPos p) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos q = p.above(dy);
            BlockState s = level.getBlockState(q);
            if (!level.getBlockState(q.relative(Direction.UP)).isAir()) continue;
            if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(net.minecraft.world.level.block.Blocks.GRAVEL)) return q;
        }
        return null;
    }
}
