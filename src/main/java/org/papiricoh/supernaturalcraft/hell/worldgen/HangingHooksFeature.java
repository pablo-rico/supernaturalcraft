package org.papiricoh.supernaturalcraft.hell.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/**
 * The Rack: chains hanging from the cavern roof with a meat hook at the end, a few blocks to a
 * dozen long, sometimes in a cluster.
 */
public class HangingHooksFeature extends Feature<NoneFeatureConfiguration> {

    public HangingHooksFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int placed = 0;
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            BlockPos at = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            BlockPos roof = findRoof(level, at);
            if (roof == null) continue;
            int length = 4 + random.nextInt(11);
            BlockPos p = roof.below();
            int n = 0;
            while (n < length && level.isEmptyBlock(p) && level.isEmptyBlock(p.below())) {
                level.setBlock(p, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2);
                p = p.below();
                n++;
            }
            if (n >= 2 && level.isEmptyBlock(p)) {
                level.setBlock(p, AllBlocks.MEAT_HOOK.get().defaultBlockState(), 2);
                placed++;
            }
        }
        return placed > 0;
    }

    /** The cavern roof above {@code at}: the first solid block within 32 above an air column. */
    private static BlockPos findRoof(WorldGenLevel level, BlockPos at) {
        if (!level.isEmptyBlock(at)) return null;
        BlockPos p = at;
        for (int i = 0; i < 32; i++) {
            p = p.above();
            if (!level.isEmptyBlock(p)) {
                return level.getBlockState(p).isFaceSturdy(level, p, Direction.DOWN) ? p : null;
            }
        }
        return null;
    }
}
