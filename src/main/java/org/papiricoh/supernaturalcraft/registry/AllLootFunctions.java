package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction;

public class AllLootFunctions {

    public static final DeferredRegister<LootItemFunctionType<?>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, SupernaturalCraft.MODID);

    /** A spell page with a random bowl spell on it. */
    public static final DeferredHolder<LootItemFunctionType<?>, LootItemFunctionType<RandomBowlSpellFunction>> RANDOM_BOWL_SPELL =
            LOOT_FUNCTIONS.register("random_bowl_spell", () -> new LootItemFunctionType<>(RandomBowlSpellFunction.CODEC));

    public static void init() {
    }
}
