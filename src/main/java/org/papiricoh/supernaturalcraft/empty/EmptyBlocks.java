package org.papiricoh.supernaturalcraft.empty;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Empty's blocks (v0.41): only {@link Sleeper} has behaviour of its own. */
public final class EmptyBlocks {

    private EmptyBlocks() {
    }

    /**
     * One of the Empty's sleepers: an angel's or a demon's body wrapped in black tar, lying across two blocks ({@link #PART}:
     * head and foot, the head toward {@link #FACING}). Written only by code; it drops nothing and is never placed by hand.
     */
    public static class Sleeper extends HorizontalDirectionalBlock {
        public static final MapCodec<Sleeper> CODEC = simpleCodec(Sleeper::new);
        public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 7, 16);

        public Sleeper(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING, PART);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }
    }
}
