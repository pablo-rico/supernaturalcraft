package org.papiricoh.supernaturalcraft.hell.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/** A black iron bowl of hellfire that never goes out: the lamps of the Pit. */
public class BrazierBlock extends Block {

    public static final MapCodec<BrazierBlock> CODEC = simpleCodec(BrazierBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 10, 11), Block.box(1, 10, 1, 15, 14, 15));

    public BrazierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.9, z = pos.getZ() + 0.5;
        for (int i = 0; i < 2; i++) {
            level.addParticle(AllParticles.HELLFIRE.get(), x + (random.nextDouble() - 0.5) * 0.6, y,
                    z + (random.nextDouble() - 0.5) * 0.6, 0, 0.04 + random.nextDouble() * 0.04, 0);
        }
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, x, y + 0.4, z, 0, 0.05, 0);
    }
}
