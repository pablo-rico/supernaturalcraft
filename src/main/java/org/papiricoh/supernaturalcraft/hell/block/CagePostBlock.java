package org.papiricoh.supernaturalcraft.hell.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A heavy post of the Cage: a thick Enochian iron column that the Cage's bars run into. It is a kind of
 * iron bars (so bars and posts join each other like vanilla bars do), with its thicker core added to the shape.
 */
public class CagePostBlock extends IronBarsBlock {

    public static final MapCodec<CagePostBlock> CODEC = simpleCodec(CagePostBlock::new);
    private static final VoxelShape CORE = Block.box(4, 0, 4, 12, 16, 12);

    public CagePostBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.or(super.getShape(state, level, pos, ctx), CORE);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.or(super.getCollisionShape(state, level, pos, ctx), CORE);
    }
}
