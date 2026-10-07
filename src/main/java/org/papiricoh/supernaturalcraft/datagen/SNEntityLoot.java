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
                .withPool(drop(AllItems.SULFUR.get(), 1, 3))
                // In Hell an occultist may carry one of Crowley's contracts.
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.3f))
                        .when(net.minecraft.world.level.storage.loot.predicates.LocationCheck.checkLocation(
                                net.minecraft.advancements.critereon.LocationPredicate.Builder.inDimension(
                                        org.papiricoh.supernaturalcraft.hell.HellDimension.LEVEL)))
                        .add(LootItem.lootTableItem(AllItems.DAMNED_CONTRACT.get()))));
        lucifer();
        add(AllEntities.LUCIFER_ILLUSION.get(), LootTable.lootTable());
        amara();
        add(AllEntities.AMARA_SHADE.get(), LootTable.lootTable());
        chorus();
        add(AllEntities.CHOIR_ECHO.get(), LootTable.lootTable());
        add(AllEntities.HELLHOUND.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition
                                .randomChanceAndLootingBoost(registries, 0.35f, 0.1f))
                        .add(LootItem.lootTableItem(AllItems.HELLHOUND_FANG.get()))));
        luciferUncaged();
        azazel();
        lilith();
    }

    /** Lilith leaves the last seal (Lucifer's summoning needs it), her likeness, her whistle, and Crowley's kind of paper. */
    private void lilith() {
        LootTable.Builder table = LootTable.lootTable();
        for (Item item : new Item[]{AllItems.LAST_SEAL.get(), AllItems.LILITH_TROPHY.get(), AllItems.HOUND_WHISTLE.get()}) {
            table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(item)));
        }
        table.withPool(drop(AllItems.DAMNED_CONTRACT.get(), 2, 4));
        table.withPool(drop(AllItems.DEMON_BLOOD.get(), 1, 3));
        add(AllEntities.LILITH.get(), table);
    }

    /** Azazel leaves two vials of his blood (each Key to the Cage needs both), his likeness, and what any demon leaves. */
    private void azazel() {
        LootTable.Builder table = LootTable.lootTable();
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.AZAZEL_BLOOD.get())
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2)))));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.AZAZEL_TROPHY.get())));
        table.withPool(drop(AllItems.DEMON_BLOOD.get(), 1, 3));
        table.withPool(drop(AllItems.SULFUR.get(), 2, 4));
        add(AllEntities.AZAZEL.get(), table);
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

    /** Lucifer Uncaged leaves a Fallen Star, two nether stars, a full load of rounds and a page of the Smite sigil. */
    private void luciferUncaged() {
        LootTable.Builder table = LootTable.lootTable();
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.FALLEN_STAR.get())));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(Items.NETHER_STAR)
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2)))));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.COLT_BULLET.get())
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(8)))));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.SIGIL_PAGE.get())
                .apply(SetComponentsFunction.setComponent(AllDataComponents.SIGIL_PAGE.get(), SupernaturalCraft.asResource("smite")))));
        add(AllEntities.LUCIFER_UNCAGED.get(), table);
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
