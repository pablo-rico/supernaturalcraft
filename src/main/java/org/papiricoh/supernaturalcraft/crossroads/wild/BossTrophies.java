package org.papiricoh.supernaturalcraft.crossroads.wild;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.balance.ProgressionScale;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Each great enemy's trophy, for the wild TROPHY wish ({@link DealTerms#TROPHY_BOSSES}): the bust it leaves, and one Ascension
 * Shard of the tier it drops. Lucifer Uncaged and the Author leave no bust of their own, so the demon cannot fetch one.
 */
public final class BossTrophies {

    private static final Map<Boss, Supplier<? extends Item>> TROPHIES = new EnumMap<>(Boss.class);

    static {
        TROPHIES.put(Boss.AZAZEL, AllItems.AZAZEL_TROPHY);
        TROPHIES.put(Boss.LILITH, AllItems.LILITH_TROPHY);
        TROPHIES.put(Boss.LUCIFER, AllItems.MORNINGSTAR_TROPHY);
        TROPHIES.put(Boss.GABRIEL, AllItems.GABRIEL_TROPHY);
        TROPHIES.put(Boss.WAR, AllItems.WAR_TROPHY);
        TROPHIES.put(Boss.FAMINE, AllItems.FAMINE_TROPHY);
        TROPHIES.put(Boss.PESTILENCE, AllItems.PESTILENCE_TROPHY);
        TROPHIES.put(Boss.RAPHAEL, AllItems.RAPHAEL_TROPHY);
        TROPHIES.put(Boss.BROKEN_CHORUS, AllItems.CHOIR_TROPHY);
        TROPHIES.put(Boss.METATRON, AllItems.METATRON_TROPHY);
        TROPHIES.put(Boss.NAOMI, AllItems.NAOMI_TROPHY);
        TROPHIES.put(Boss.ZACHARIAH, AllItems.ZACHARIAH_TROPHY);
        TROPHIES.put(Boss.AMARA, AllItems.ECLIPSE_TROPHY);
        TROPHIES.put(Boss.DEATH, AllItems.DEATH_TROPHY);
        TROPHIES.put(Boss.MICHAEL, AllItems.MICHAEL_TROPHY);
    }

    private BossTrophies() {
    }

    /** Whether the demon can fetch {@code boss}'s trophy. */
    public static boolean has(Boss boss) {
        return TROPHIES.containsKey(boss);
    }

    /** {@code boss}'s trophy, or an empty stack. */
    public static ItemStack trophy(@Nullable Boss boss) {
        Supplier<? extends Item> item = boss == null ? null : TROPHIES.get(boss);
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    /** One Ascension Shard of the tier {@code boss} leaves, or an empty stack. */
    public static ItemStack shard(@Nullable Boss boss) {
        if (boss == null) return ItemStack.EMPTY;
        int tier = ProgressionScale.of(boss).shardTier();
        return tier <= 0 ? ItemStack.EMPTY : new ItemStack(AllItems.shardOf(tier).get());
    }
}
