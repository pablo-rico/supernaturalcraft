package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * The seals' blocks (v0.40, the Seals slice), as classes the registry already names so the slice can grow them freely
 * (keep each constructor taking {@code Properties}).
 */
public final class SealBlocks {

    private SealBlocks() {
    }

    /** The demons' hex at a seal: break it and the rite (or the plague) stops ({@code SealEvents} sees it go). */
    public static class HexFocus extends Block {
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

        public HexFocus(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(AllParticles.HEX_MOTE.get(), pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 0.4 + r.nextDouble() * 0.5,
                        pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.02, 0);
            }
        }

        @Override
        public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            if (level instanceof ServerLevel server) SealEvents.focusBroken(server, pos, player);
            return super.playerWillDestroy(level, pos, state, player);
        }
    }

    /** A flat decal on the ground: no collision, gone when what holds it goes. */
    abstract static class Decal extends Block {
        private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

        Decal(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return SHAPE;
        }

        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        }

        @Override
        protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
            return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, dir, other, level, pos, otherPos);
        }
    }

    /** A puddle of the water turned to blood (a plague; reverted by the ledger). It drips, and it stains whoever wades in. */
    public static class BloodPool extends Decal {
        public BloodPool(Properties properties) {
            super(properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
            if (r.nextInt(6) == 0) {
                level.addParticle(AllParticles.BLOOD_DROP.get(), pos.getX() + r.nextDouble(), pos.getY() + 0.08, pos.getZ() + r.nextDouble(), 0, 0.01, 0);
            }
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide && entity instanceof LivingEntity l && level.getGameTime() % 40 == 0) {
                l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 60, 0));
            }
        }
    }

    /** The burnt shape of wings where an angel died at a seal. */
    public static class WingScorch extends Decal {
        public WingScorch(Properties properties) {
            super(properties);
        }
    }
}
