package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** Low-tier ascension currency; officers never substitute for boss progression keys. */
public final class ChainRewards {
    private ChainRewards() {}
    public static void drop(LivingEntity mob, ChainRank rank) {
        mob.spawnAtLocation(new ItemStack(AllItems.ASCENSION_SHARD_1.get(), rank.ordinal() + 1));
        if (rank == ChainRank.LORD) mob.spawnAtLocation(new ItemStack(AllItems.ASCENSION_SHARD_2.get()));
    }
}
