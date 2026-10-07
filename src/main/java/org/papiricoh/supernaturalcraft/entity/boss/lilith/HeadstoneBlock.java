package org.papiricoh.supernaturalcraft.entity.boss.lilith;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One half of a headstone raised by Lilith's arena: cover from her white light. Each burst it stops
 * adds a crack ({@link #CRACKS}); at {@link LilithBalance#HEADSTONE_CRACKS} it falls. Only ever placed
 * (and taken away) by the fight; unbreakable, with no item.
 */
public class HeadstoneBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<HeadstoneBlock> CODEC = simpleCodec(HeadstoneBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, LilithBalance.HEADSTONE_CRACKS);
    private static final VoxelShape LOWER_NS = Block.box(1, 0, 5, 15, 16, 11), LOWER_EW = Block.box(5, 0, 1, 11, 16, 15);
    private static final VoxelShape UPPER_NS = Block.box(2, 0, 5, 14, 13, 11), UPPER_EW = Block.box(5, 0, 2, 11, 13, 14);

    public HeadstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(CRACKS, 0));
    }

    @Override
    protected MapCodec<? extends HeadstoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, CRACKS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) return ns ? LOWER_NS : LOWER_EW;
        return ns ? UPPER_NS : UPPER_EW;
    }
}
