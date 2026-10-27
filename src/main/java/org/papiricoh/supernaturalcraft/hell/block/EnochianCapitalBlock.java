package org.papiricoh.supernaturalcraft.hell.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * The carved end of an Enochian pillar. {@link #HALF} TOP is a capital (the flare at the top, the shaft running down
 * from it); BOTTOM is the base (flare at the bottom). Placed against a ceiling or high on a wall it is a capital.
 */
public class EnochianCapitalBlock extends Block {

    public static final MapCodec<EnochianCapitalBlock> CODEC = simpleCodec(EnochianCapitalBlock::new);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    public EnochianCapitalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, Half.BOTTOM));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace();
        boolean top = face == Direction.DOWN
                || (face != Direction.UP && ctx.getClickLocation().y - ctx.getClickedPos().getY() > 0.5);
        return defaultBlockState().setValue(HALF, top ? Half.TOP : Half.BOTTOM);
    }
}
