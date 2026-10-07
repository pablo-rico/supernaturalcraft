package org.papiricoh.supernaturalcraft.grave;

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
import net.minecraft.world.phys.shapes.VoxelShape;

/** An old headstone. Its carved face (the model's north side) looks toward {@link #FACING}. */
public class GraveHeadstoneBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<GraveHeadstoneBlock> CODEC = simpleCodec(GraveHeadstoneBlock::new);
    /**
     * The stone stands at the back of its block (the model's south edge), its face looking out across
     * the block toward FACING: one shape per facing, matching the model's rotation.
     */
    private static final VoxelShape NORTH = Block.box(1.5, 0, 11.4, 14.5, 15.2, 15.6);
    private static final VoxelShape SOUTH = Block.box(1.5, 0, 0.4, 14.5, 15.2, 4.6);
    private static final VoxelShape EAST = Block.box(0.4, 0, 1.5, 4.6, 15.2, 14.5);
    private static final VoxelShape WEST = Block.box(11.4, 0, 1.5, 15.6, 15.2, 14.5);

    public GraveHeadstoneBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }
}
