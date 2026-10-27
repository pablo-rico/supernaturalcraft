package org.papiricoh.supernaturalcraft.hell.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A facing ornament of the Cage. The model's front (its north side) looks toward {@link #FACING}.
 * <ul>
 *   <li>{@link Kind#ANCHOR}: an iron plate bolted to the wall behind it, with a ring and a stub of chain. Placed against
 *   a wall it faces out of it.</li>
 *   <li>{@link Kind#WATCHER}: a statue of a bound angel kneeling on a plinth, facing whoever placed it.</li>
 * </ul>
 */
public class CageOrnamentBlock extends HorizontalDirectionalBlock {

    public enum Kind {
        ANCHOR(Block.box(3, 1, 10, 13, 13, 16)),
        WATCHER(Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 16, 14)));

        private final VoxelShape[] shapes = new VoxelShape[4];

        Kind(VoxelShape north) {
            for (Direction d : Direction.Plane.HORIZONTAL) shapes[d.get2DDataValue()] = rotate(north, d);
        }
    }

    public static final MapCodec<CageOrnamentBlock> ANCHOR_CODEC = simpleCodec(p -> new CageOrnamentBlock(p, Kind.ANCHOR));
    public static final MapCodec<CageOrnamentBlock> WATCHER_CODEC = simpleCodec(p -> new CageOrnamentBlock(p, Kind.WATCHER));

    private final Kind kind;

    public CageOrnamentBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return kind == Kind.ANCHOR ? ANCHOR_CODEC : WATCHER_CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        if (kind == Kind.ANCHOR && face.getAxis().isHorizontal()) return defaultBlockState().setValue(FACING, face);
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return kind.shapes[state.getValue(FACING).get2DDataValue()];
    }

    /** A shape drawn facing north, turned to face {@code to} (the same turn as the blockstate's y rotation). */
    private static VoxelShape rotate(VoxelShape north, Direction to) {
        VoxelShape[] out = {Shapes.empty()};
        int turns = switch (to) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        north.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double ax0 = x0, az0 = z0, ax1 = x1, az1 = z1;
            for (int i = 0; i < turns; i++) {
                // Clockwise seen from above: (x, z) -> (1 - z, x).
                double nx0 = 1 - az1, nz0 = ax0, nx1 = 1 - az0, nz1 = ax1;
                ax0 = nx0;
                az0 = nz0;
                ax1 = nx1;
                az1 = nz1;
            }
            out[0] = Shapes.or(out[0], Shapes.box(ax0, y0, az0, ax1, y1, az1));
        });
        return out[0].optimize();
    }
}
