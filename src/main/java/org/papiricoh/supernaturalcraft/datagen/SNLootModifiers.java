package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageDrops;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Spell pages in vanilla chests: each listed table also rolls {@link SpellPageDrops#CHANCE_TABLE}. */
public class SNLootModifiers extends GlobalLootModifierProvider {

    private static final List<ResourceKey<LootTable>> CHESTS = List.of(BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.ABANDONED_MINESHAFT, BuiltInLootTables.DESERT_PYRAMID, BuiltInLootTables.JUNGLE_TEMPLE,
            BuiltInLootTables.STRONGHOLD_LIBRARY, BuiltInLootTables.WOODLAND_MANSION, BuiltInLootTables.ANCIENT_CITY);

    public SNLootModifiers(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, SupernaturalCraft.MODID);
    }

    @Override
    protected void start() {
        for (ResourceKey<LootTable> chest : CHESTS) {
            add("spell_page_in_" + chest.location().getPath().replace('/', '_'), new AddTableLootModifier(
                    new LootItemCondition[]{LootTableIdCondition.builder(chest.location()).build()}, SpellPageDrops.CHANCE_TABLE));
        }
        // v0.17: field notes and, rarely, a cursed artifact in any chest.
        add("archive_loot", new org.papiricoh.supernaturalcraft.legacy.research.ArchiveLootModifier(new LootItemCondition[0], 0.25f, 0.03f));
    }
}
