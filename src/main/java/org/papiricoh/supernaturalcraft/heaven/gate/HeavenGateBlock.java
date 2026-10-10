package org.papiricoh.supernaturalcraft.heaven.gate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * A gate of light into a hunter's own Heaven (or out of it, or between its places): standing in it for
 * {@link HeavenGates#CROSSING_TICKS} carries a player where its {@link HeavenGate} says, like a Nether portal. Where it leads
 * and when it closes is kept in {@link HeavenGateSavedData}; a gate block nothing knows about (pasted, left by a crash, part
 * of a layout) fades on its own.
 * <p>State: {@link #AXIS} ({@code x|z}), the axis its face lies along, as a Nether portal's.
 */
public class HeavenGateBlock extends Block implements Portal {

    public static final MapCodec<HeavenGateBlock> CODEC = simpleCodec(HeavenGateBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);

    public HeavenGateBlock(Properties properties) {
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
        return entity instanceof Player p && p.getAbilities().invulnerable ? 1 : HeavenGates.CROSSING_TICKS;
    }

    @Override
    public @Nullable DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return HeavenGates.destination(level, entity, pos);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.NONE;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 60);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (HeavenGateSavedData.get(level).at(pos) == null) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        else level.scheduleTick(pos, this, 400);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(160) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, AllSounds.heaven("heaven.gate_hum"),
                    SoundSource.BLOCKS, 0.35f, 0.9f + random.nextFloat() * 0.2f, false);
        }
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        if (random.nextInt(2) == 0) {
            double out = (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.END_ROD, x ? pos.getX() + random.nextDouble() : pos.getX() + 0.5 + out,
                    pos.getY() + random.nextDouble(), x ? pos.getZ() + 0.5 + out : pos.getZ() + random.nextDouble(), 0, 0.015, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
