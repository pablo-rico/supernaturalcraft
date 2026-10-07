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
