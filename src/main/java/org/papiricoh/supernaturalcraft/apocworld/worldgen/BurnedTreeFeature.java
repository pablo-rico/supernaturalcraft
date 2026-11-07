package org.papiricoh.supernaturalcraft.apocworld.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;

/**
 * A tree the fire took (v0.41): a charred trunk, three to nine blocks, a few bare broken branches, now and then a stump or a
 * trunk fallen on its side. Only on the burned world's own ground.
 */
public class BurnedTreeFeature extends Feature<NoneFeatureConfiguration> {

    public BurnedTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos at = ctx.origin();
        if (!ground(level.getBlockState(at.below())) || !level.getBlockState(at).canBeReplaced()) return false;
        BlockState log = ApocWorldRegistry.BURNED_LOG.get().defaultBlockState();
        int kind = r.nextInt(10);
        if (kind == 0) {
            // A trunk fallen on its side.
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockState lying = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
            int len = 3 + r.nextInt(4);
            for (int i = 0; i < len; i++) {
                BlockPos p = at.relative(d, i);
                if (!level.getBlockState(p).canBeReplaced()) break;
                if (level.getBlockState(p.below()).canBeReplaced()) break;
                level.setBlock(p, lying, Block.UPDATE_CLIENTS);
            }
            return true;
        }
        int height = kind < 3 ? 1 + r.nextInt(2) : 4 + r.nextInt(6);
        for (int y = 0; y < height; y++) {
            BlockPos p = at.above(y);
            if (!level.getBlockState(p).canBeReplaced()) return y > 0;
            level.setBlock(p, log, Block.UPDATE_CLIENTS);
        }
        if (height < 4) return true;
        int branches = 1 + r.nextInt(3);
        for (int b = 0; b < branches; b++) {
            Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
            int y = height / 2 + r.nextInt(Math.max(1, height / 2));
            BlockPos base = at.above(Math.min(height - 1, y));
            BlockState arm = log.setValue(RotatedPillarBlock.AXIS, d.getAxis());
            int len = 1 + r.nextInt(3);
            for (int i = 1; i <= len; i++) {
                BlockPos p = base.relative(d, i).above(i > 1 && r.nextBoolean() ? 1 : 0);
                if (!level.getBlockState(p).canBeReplaced()) break;
                level.setBlock(p, arm, Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }

    private static boolean ground(BlockState s) {
        return s.is(ApocWorldRegistry.SCORCHED_SOIL.get()) || s.is(ApocWorldRegistry.ASH_BLOCK.get())
                || s.is(ApocWorldRegistry.SCORCHED_STONE.get()) || s.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT);
    }
}
