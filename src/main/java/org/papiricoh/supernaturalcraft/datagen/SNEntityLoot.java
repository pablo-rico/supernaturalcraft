package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.stream.Stream;

public class SNEntityLoot extends EntityLootSubProvider {

    public SNEntityLoot(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    public void generate() {
        add(AllEntities.BLACK_EYED_DEMON.get(), LootTable.lootTable()
                .withPool(drop(AllItems.DEMON_BLOOD.get(), 0, 1).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(drop(AllItems.SULFUR.get(), 0, 2)));
        add(AllEntities.DEMON_OCCULTIST.get(), LootTable.lootTable()
                .withPool(drop(AllItems.DEMON_BLOOD.get(), 0, 1).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(drop(AllItems.SULFUR.get(), 1, 3)));
        lucifer();
        add(AllEntities.LUCIFER_ILLUSION.get(), LootTable.lootTable());
        amara();
        add(AllEntities.AMARA_SHADE.get(), LootTable.lootTable());
        chorus();
        add(AllEntities.CHOIR_ECHO.get(), LootTable.lootTable());
    }

    /** The Broken Chorus leaves its wings, its likeness and shards of its wheels, every time. */
    private void chorus() {
        LootTable.Builder table = LootTable.lootTable();
        for (Item item : new Item[]{AllItems.SERAPH_WINGS.get(), AllItems.CHOIR_TROPHY.get()}) {
            table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(item)));
        }
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.CHOIR_SHARD.get())
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 5)))));
        table.withPool(rareBullets());
        add(AllEntities.BROKEN_CHORUS.get(), table);
    }

    /** The Darkness leaves Penumbra, her sight, her essence and her likeness, every time. */
    private void amara() {
        LootTable.Builder table = LootTable.lootTable();
        for (Item item : new Item[]{AllItems.PENUMBRA.get(), AllItems.ECLIPSE_SIGHT.get(), AllItems.ECLIPSE_TROPHY.get()}) {
            table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(item)));
        }
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.VOID_ESSENCE.get())
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 5)))));
        table.withPool(rareBullets());
        add(AllEntities.AMARA.get(), table);
    }

    /** Lucifer drops all of his spoils, every time, plus the tier-three Echo sigil. */
    private void lucifer() {
        LootTable.Builder table = LootTable.lootTable();
        for (Item item : new Item[]{AllItems.ARCHANGEL_BLADE.get(), AllItems.LUCIFERS_GRACE.get(), AllItems.MORNINGSTAR_TROPHY.get(),
                Items.NETHER_STAR}) {
            table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(item)));
        }
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.THE_COLT.get())
                .apply(SetComponentsFunction.setComponent(AllDataComponents.COLT_AMMO.get(), ColtItem.CAPACITY))));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.COLT_BULLET.get())
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 5)))));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.SIGIL_PAGE.get())
                .apply(SetComponentsFunction.setComponent(AllDataComponents.SIGIL_PAGE.get(), SupernaturalCraft.asResource("echo")))));
        add(AllEntities.LUCIFER.get(), table);
    }

    /** Half the time, a few consecrated rounds: the Colt's ammunition is otherwise made by ritual. */
    private static LootPool.Builder rareBullets() {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(0.5f))
                .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))));
    }

    private LootPool.Builder drop(Item item, int min, int max) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0, 1))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return AllEntities.ENTITY_TYPES.getEntries().stream().map(DeferredHolder::get)
                .filter(t -> t.getCategory() != MobCategory.MISC)
                .map(t -> (EntityType<?>) t);
    }
}
