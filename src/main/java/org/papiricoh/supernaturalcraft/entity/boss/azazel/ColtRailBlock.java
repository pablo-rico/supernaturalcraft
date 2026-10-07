package org.papiricoh.supernaturalcraft.entity.boss.azazel;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * One length of Samuel Colt's iron rail, set flat in the ground of Azazel's arena. Charged, the iron
 * glows and no demon will path across it; spent, it is cold iron until it charges again. Placed and
 * taken away only by the fight ({@link RailTrap}); unbreakable, with no item.
 */
public class ColtRailBlock extends Block {

    public static final MapCodec<ColtRailBlock> CODEC = simpleCodec(ColtRailBlock::new);
    public static final EnumProperty<RailTrapLayout.Shape> SHAPE = EnumProperty.create("shape", RailTrapLayout.Shape.class);
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 1, 16);

    public ColtRailBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHAPE, RailTrapLayout.Shape.NS).setValue(CHARGED, true));
    }

    @Override
    protected MapCodec<? extends ColtRailBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, CHARGED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** Azazel will not path across charged iron; spent rails, and anyone else, are left to the defaults. */
    @Override
    public @Nullable PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return state.getValue(CHARGED) && mob instanceof AzazelEntity ? PathType.BLOCKED : null;
    }
}
