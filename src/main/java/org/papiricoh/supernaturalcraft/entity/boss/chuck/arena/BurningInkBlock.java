package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * Ink that burns whoever stands on it ("the floor is lava", rewritten in the Author's hand): every half second a
 * hunter on it is {@code rewritten} for {@link #DAMAGE}. Bosses walk on it unharmed.
 */
public class BurningInkBlock extends Block {

    public static final MapCodec<BurningInkBlock> CODEC = simpleCodec(BurningInkBlock::new);
    public static final float DAMAGE = 2.0f;
    public static final int INTERVAL = 10;

    public BurningInkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && burns(living)
                && living.tickCount % INTERVAL == 0) {
            if (living.hurt(AllDamageTypes.source(level, AllDamageTypes.REWRITTEN, null), DAMAGE)) {
                server.sendParticles(AllParticles.INK_LETTER.get(), living.getX(), living.getY() + 0.3, living.getZ(),
                        3, 0.3, 0.2, 0.3, 0.04);
                server.sendParticles(ParticleTypes.SMALL_FLAME, living.getX(), living.getY() + 0.1, living.getZ(),
                        4, 0.3, 0.05, 0.3, 0.01);
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Whether the ink burns this creature (never a boss). */
    public static boolean burns(LivingEntity living) {
        return !living.getType().is(AllTags.Entities.BOSSES) && !living.isSpectator();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) == 0 && level.getBlockState(pos.above()).isAir()) {
            double x = pos.getX() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE,
                    x, pos.getY() + 1.02, z, 0, 0.02, 0);
        }
    }
}
