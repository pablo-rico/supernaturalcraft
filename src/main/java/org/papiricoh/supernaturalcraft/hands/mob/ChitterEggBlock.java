package org.papiricoh.supernaturalcraft.hands.mob;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.hands.HandsRegistry;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * A Chitter's egg sac (v0.42): on a nest, it hatches (a Chitter climbs out of it) while the Chitters are awake
 * ({@link ChittersRules#awake}) and a hunter is near, one random tick at a time; broken, it bursts with nothing inside.
 */
public class ChitterEggBlock extends Block {

    public static final MapCodec<ChitterEggBlock> CODEC = simpleCodec(ChitterEggBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);
    /** A hunter must be this close for an egg to hatch. */
    static final double HATCH_NEAR = 16;

    public ChitterEggBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

    @Override
    protected BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbour, net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        return !state.canSurvive(level, pos) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0 || !ChittersRules.awake(WorldClock.day(level.getServer()))) return;
        Player near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, HATCH_NEAR, false);
        if (near == null || near.isSpectator() || near.isCreative()) return;
        hatch(level, pos);
    }

    /** The egg bursts and a Chitter climbs out. */
    public static void hatch(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        level.removeBlock(pos, false);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, pos, SoundEvents.FROGSPAWN_HATCH, SoundSource.HOSTILE, 1.0f, 0.6f);
        ChitterEntity c = HandsRegistry.CHITTER.get().create(level);
        if (c == null) return;
        c.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0);
        c.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null);
        level.addFreshEntity(c);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity be,
                              net.minecraft.world.item.ItemStack tool) {
        super.playerDestroy(level, player, pos, state, be, tool);
        level.playSound(null, pos, SoundEvents.FROGSPAWN_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
    }
}
