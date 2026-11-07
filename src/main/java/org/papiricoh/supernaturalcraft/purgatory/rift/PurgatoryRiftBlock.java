package org.papiricoh.supernaturalcraft.purgatory.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A tear between the world and Purgatory (v0.39): grey mist with souls drifting in it. Standing in it for
 * {@link PurgatoryRifts#CROSSING_TICKS} carries a hunter through, like a Nether portal; where it leads is worked out for them
 * ({@link PurgatoryRifts#destination}). A rift block nothing knows about seals itself.
 */
public class PurgatoryRiftBlock extends Block implements Portal {

    public static final MapCodec<PurgatoryRiftBlock> CODEC = simpleCodec(PurgatoryRiftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);

    public PurgatoryRiftBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof Player && entity.canUsePortal(false)) entity.setAsInsidePortal(this, pos);
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player p ? (p.getAbilities().invulnerable ? 1 : PurgatoryRifts.CROSSING_TICKS) : 0;
    }

    @Override
    public @Nullable DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return PurgatoryRifts.destination(level, entity, pos);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 40);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (PurgatoryRiftSavedData.get(level).at(pos) == null) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        else level.scheduleTick(pos, this, 200);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.35f, 0.5f + random.nextFloat() * 0.1f, false);
        }
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        double out = (random.nextDouble() - 0.5) * 0.6;
        double px = pos.getX() + random.nextDouble(), py = pos.getY() + random.nextDouble(), pz = pos.getZ() + random.nextDouble();
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SOUL, x ? px : pos.getX() + 0.5 + out, py, x ? pos.getZ() + 0.5 + out : pz, 0, 0.02, 0);
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.WHITE_ASH, px, py, pz, 0, 0.01, 0);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
