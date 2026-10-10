package org.papiricoh.supernaturalcraft.crossroads.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * The trodden earth at the very centre of a natural crossroads: bury a crossroads box in it to call the demon, no bowl needed
 * ({@link CrossroadsBoxItem}). The demon comes on the block's scheduled tick ({@link WildCrossroads#arrive}), so a box buried
 * just before a save is still answered. Dug up, it is only dirt: the crossroads is the place, not the soil.
 */
public class CrossroadsSoilBlock extends Block {

    public CrossroadsSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        WildCrossroads.arrive(level, pos);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.DIRT));
    }
}
