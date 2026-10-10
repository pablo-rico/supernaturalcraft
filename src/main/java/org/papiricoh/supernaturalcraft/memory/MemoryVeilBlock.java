package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The veil in a memory shrine's doorway: stepping through stages that shrine's memory on the plot's stage
 * ({@link MemoryStage#veilTouched}). The same veil, standing behind a visitor in a staged memory, is the way back out.
 * Registered in {@code AllBlocks} with the {@code (Properties)} constructor.
 */
public class MemoryVeilBlock extends Block {

    /** The axis the pane of light spans (X: a doorway facing north-south; Z: one facing east-west). */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10), Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);

    public MemoryVeilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /** A veil whose pane faces someone walking along {@code walk} (spans the other horizontal axis). */
    public BlockState facing(Direction.Axis walk) {
        return defaultBlockState().setValue(AXIS, walk == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof ServerPlayer player && !player.isSpectator() && player.isAlive()) {
            MemoryStage.veilTouched(server, pos, player);
        }
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }
}
