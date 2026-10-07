package org.papiricoh.supernaturalcraft.hell.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * A crack in the Ash Wastes that breathes hellfire: a lazy plume most of the time, and now and then a
 * gout that sets whoever stands on it alight (only every few seconds, keyed to the world clock, so
 * a careful hunter can time the crossing).
 */
public class HellfireVentBlock extends Block {

    public static final MapCodec<HellfireVentBlock> CODEC = simpleCodec(HellfireVentBlock::new);
    /** One gout every {@code PERIOD} ticks, lasting {@code BURST} ticks; each vent has its own offset. */
    public static final int PERIOD = 100, BURST = 20;

    public HellfireVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static boolean erupting(Level level, BlockPos pos) {
        long phase = (level.getGameTime() + (pos.asLong() * 31L & 0xFF)) % PERIOD;
        return phase < BURST;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !living.fireImmune() && erupting(level, pos)) {
            living.igniteForSeconds(3);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 1.02, z = pos.getZ() + 0.5;
        if (erupting(level, pos)) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(AllParticles.HELLFIRE.get(), x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5,
                        0, 0.25 + random.nextDouble() * 0.2, 0);
            }
            if (random.nextInt(6) == 0) level.playLocalSound(x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.4f, 0.6f, false);
        } else if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y, z, 0, 0.05, 0);
        }
    }
}
