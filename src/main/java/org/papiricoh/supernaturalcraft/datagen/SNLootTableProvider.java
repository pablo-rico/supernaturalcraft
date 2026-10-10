package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.papiricoh.supernaturalcraft.hunter.DevilsTrapBlock;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SNLootTableProvider extends LootTableProvider {

    public SNLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK),
                new SubProviderEntry(SNEntityLoot::new, LootContextParamSets.ENTITY),
                new SubProviderEntry(SNChestLoot::new, LootContextParamSets.CHEST)), registries);
    }

    private static class BlockLoot extends BlockLootSubProvider {

        BlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            add(AllBlocks.ROCK_SALT_ORE.get(), b -> oreDrop(b, AllItems.SALT.get(), 2, 4));
            add(AllBlocks.DEEPSLATE_ROCK_SALT_ORE.get(), b -> oreDrop(b, AllItems.SALT.get(), 2, 4));
            add(AllBlocks.NETHER_SULFUR_ORE.get(), b -> oreDrop(b, AllItems.SULFUR.get(), 1, 3));
            dropSelf(AllBlocks.RITUAL_ALTAR.get());
            dropSelf(AllBlocks.MORNINGSTAR_TROPHY.get());
            dropSelf(AllBlocks.HELLFORGE.get());
            dropSelf(AllBlocks.ECLIPSE_TROPHY.get());
            dropSelf(AllBlocks.CHOIR_TROPHY.get());
            dropSelf(AllBlocks.AZAZEL_TROPHY.get());
            dropSelf(AllBlocks.LILITH_TROPHY.get());
            dropSelf(AllBlocks.METATRON_TROPHY.get());
            dropSelf(AllBlocks.WAR_TROPHY.get());
            dropSelf(AllBlocks.FAMINE_TROPHY.get());
            dropSelf(AllBlocks.PESTILENCE_TROPHY.get());
            dropSelf(AllBlocks.MICHAEL_TROPHY.get());
            dropSelf(AllBlocks.GABRIEL_TROPHY.get());
            dropSelf(AllBlocks.RAPHAEL_TROPHY.get());
            // v0.17: the bunker's furniture.
            add(AllBlocks.BUNKER_DOOR.get(), createDoorTable(AllBlocks.BUNKER_DOOR.get()));
            dropSelf(AllBlocks.RESEARCH_DESK.get());
            dropSelf(AllBlocks.MAP_TABLE.get());
            dropSelf(AllBlocks.ARCHIVE_SHELF.get());
            dropSelf(AllBlocks.MEN_OF_LETTERS_EMBLEM.get());
            // v0.18: Heaven and the wild crossroads.
            dropSelf(AllBlocks.HEARTH.get());
            dropSelf(AllBlocks.CLOUD_STONE.get());
            dropSelf(AllBlocks.CLOUD_BRICKS.get());
            dropSelf(AllBlocks.FILING_CABINET.get());
            dropOther(AllBlocks.CROSSROADS_SOIL.get(), net.minecraft.world.item.Items.DIRT);
            dropSelf(AllBlocks.NAOMI_TROPHY.get());
            dropSelf(AllBlocks.ZACHARIAH_TROPHY.get());
            dropSelf(AllBlocks.DEATH_TROPHY.get());
            // v0.8: the bowl and a curse bag keep what they hold when broken.
            add(AllBlocks.SPELL_BOWL.get(), LootTable.lootTable().withPool(applyExplosionCondition(AllItems.SPELL_BOWL.get(),
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.SPELL_BOWL.get())
                            .apply(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                    .copyComponents(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(org.papiricoh.supernaturalcraft.registry.AllDataComponents.BOWL_CONTENTS.get()))))));
            add(AllBlocks.CURSE_BAG.get(), LootTable.lootTable().withPool(applyExplosionCondition(AllItems.CURSE_BAG.get(),
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.CURSE_BAG.get())
                            .apply(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                    .copyComponents(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY)
                                    .include(org.papiricoh.supernaturalcraft.registry.AllDataComponents.HEX_BAG.get()))))));
            dropSelf(AllBlocks.GRAVE_HEADSTONE.get());
            add(AllBlocks.GRAVE_SOIL.get(), b -> createSingleItemTableWithSilkTouch(b, AllItems.GRAVE_DIRT.get()));
            for (var b : java.util.List.of(AllBlocks.HELLSTONE, AllBlocks.HELLSTONE_BRICKS, AllBlocks.RACK_STONE, AllBlocks.CONGEALED_BLOOD,
                    AllBlocks.ASH_BLOCK, AllBlocks.HELLFIRE_VENT, AllBlocks.CORRIDOR_STONE, AllBlocks.CORRIDOR_BRICKS, AllBlocks.ABYSSAL_STONE)) {
                dropSelf(b.get());
            }
            add(AllBlocks.BRIMSTONE_ORE.get(), b -> oreDrop(b, AllItems.BRIMSTONE.get(), 1, 3));
            add(AllBlocks.ABYSSAL_SHARD_ORE.get(), b -> oreDrop(b, AllItems.ABYSSAL_SHARD.get(), 1, 2));
            dropOther(AllBlocks.MEAT_HOOK.get(), AllItems.RACK_HOOK.get());
            dropOther(AllBlocks.SALT_LINE.get(), AllItems.SALT.get());
            dropOther(AllBlocks.CHALK_LINE.get(), AllItems.CHALK.get());
            dropOther(AllBlocks.BLOOD_CHALK_LINE.get(), AllItems.BLOOD_CHALK.get());
            // Only the centre ninth of a devil's trap gives the item back.
            add(AllBlocks.DEVILS_TRAP.get(), LootTable.lootTable().withPool(applyExplosionCondition(AllItems.DEVILS_TRAP.get(),
                    LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.DEVILS_TRAP.get()))
                            .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(AllBlocks.DEVILS_TRAP.get())
                                    .setProperties(StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(DevilsTrapBlock.PART, DevilsTrapBlock.CENTER))))));
        }

        private LootTable.Builder oreDrop(Block block, Item drop, int min, int max) {
            HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
            return createSilkTouchDispatchTable(block, applyExplosionDecay(block, LootItem.lootTableItem(drop)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                    .apply(ApplyBonusCount.addOreBonusCount(enchantments.getOrThrow(Enchantments.FORTUNE)))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            // Arena floor blocks have no loot table at all.
            return AllBlocks.BLOCKS.getEntries().stream().map(DeferredHolder::get).map(b -> (Block) b)
                    .filter(b -> b.getLootTable() != BuiltInLootTables.EMPTY).toList();
        }
    }
}
