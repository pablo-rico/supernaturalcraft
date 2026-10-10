package org.papiricoh.supernaturalcraft.heaven.home;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A seal of light across a doorway in Heaven (v0.18: the home's door, the clinical wing's, Naomi's lift): unbreakable, solid,
 * see-through; it opens when the plot says so ({@link Seals}). No block states.
 */
public class CelestialSealBlock extends Block {

    public CelestialSealBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
