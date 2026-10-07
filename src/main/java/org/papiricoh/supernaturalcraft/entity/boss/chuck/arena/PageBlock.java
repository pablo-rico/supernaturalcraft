package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * A block of blank paper: the floor and walls of the Blank Page, and the ceiling gravity drops hunters onto. Now and
 * then a scrap of paper lifts off it.
 */
public class PageBlock extends Block {

    public static final MapCodec<PageBlock> CODEC = simpleCodec(PageBlock::new);

    public PageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(160) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(AllParticles.PAGE_SCRAP.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                    pos.getZ() + random.nextDouble(), 0, 0.02 + random.nextDouble() * 0.03, 0);
        }
    }
}
