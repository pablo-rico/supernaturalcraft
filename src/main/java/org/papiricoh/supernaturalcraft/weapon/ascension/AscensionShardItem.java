package org.papiricoh.supernaturalcraft.weapon.ascension;

import net.minecraft.world.item.Item;

/**
 * An Ascension Shard (v0.15): at the Hellforge it raises a weapon or a piece of armour from Ascension
 * {@code tier - 1} to {@code tier}. The first is made by a rite; the rest are left by the great enemies
 * ({@code ProgressionScale.BossStats.shardTier}).
 */
public class AscensionShardItem extends Item {

    private final int tier;

    public AscensionShardItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }
}
