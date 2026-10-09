package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.structure.SpireBuilder;

import java.util.function.BiConsumer;

/** What the Hymnal Spire's temple keeps: its treasures, and in the vault, always, a Shattered Hymn. */
public class SNChestLoot implements LootTableSubProvider {

    public SNChestLoot(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(SpireBuilder.TEMPLE_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(4, 7))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))))
                        .add(LootItem.lootTableItem(AllItems.ENOCHIAN_INK.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(3))
                        .add(LootItem.lootTableItem(Items.BELL).setWeight(2))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        // v0.8 (content agent): a bowl spell page.
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).setWeight(3)
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell()))));
        out.accept(SpireBuilder.VAULT_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.SHATTERED_HYMN.get())))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(4))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(4))
                        .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))));
        bunker(out);
        // A cell in Crowley's Corridors: contracts, rounds, the odd sigil page, and what the damned left behind.
        out.accept(org.papiricoh.supernaturalcraft.hell.worldgen.CorridorsStructure.CELL_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 5))
                        .add(LootItem.lootTableItem(AllItems.DAMNED_CONTRACT.get()).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(AllItems.DEMON_BLOOD.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.BRIMSTONE.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                        .add(LootItem.lootTableItem(AllItems.HELLFIRE_EMBER.get()).setWeight(4))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(3))
                        .add(LootItem.lootTableItem(AllItems.SIGIL_PAGE.get()).setWeight(2)
                                .apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                                        org.papiricoh.supernaturalcraft.registry.AllDataComponents.SIGIL_PAGE.get(),
                                        org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("hellfire"))))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        // v0.8 (content agent): a bowl spell page.
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).setWeight(3)
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell()))));
        // v0.8 (content agent): rolled into vanilla chests by SNLootModifiers; about one chest in eight gets a spell page.
        out.accept(org.papiricoh.supernaturalcraft.bowl.page.SpellPageDrops.CHANCE_TABLE, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(0.12f))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get())
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell()))));
        // v0.8: what was buried with the dead: bones and earth, candles, a few coins of iron, and now and then a spell page.
        out.accept(org.papiricoh.supernaturalcraft.grave.GraveBuilder.GRAVE_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(12).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(AllItems.GRAVE_DIRT.get()).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 7))))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(AllItems.ECTOPLASM.get()).setWeight(2)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).setWeight(1)
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell()))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(1))));
    }

    // --- v0.17.1: the Men of Letters' bunker ------------------------------------------------------------------------------------

    public static final ResourceKey<LootTable> BUNKER_LIBRARY = bunkerKey("library"), BUNKER_ARCHIVE = bunkerKey("archive"),
            BUNKER_LAB = bunkerKey("lab"), BUNKER_ARMORY = bunkerKey("armory"), BUNKER_KITCHEN = bunkerKey("kitchen"),
            BUNKER_VAULT = bunkerKey("vault");

    private static ResourceKey<LootTable> bunkerKey(String room) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/bunker_" + room));
    }

    /** Field notes on a topic, as loot. */
    private static LootItem.Builder<?> notes(String topic, int weight, int min, int max) {
        return LootItem.lootTableItem(AllItems.FIELD_NOTES.get()).setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                        org.papiricoh.supernaturalcraft.registry.AllDataComponents.NOTE_TOPIC.get(), topic));
    }

    /**
     * What the bunker keeps, room by room: paper, ink and notes for the research (the library and the archive hold the "place" and
     * "arcane" notes the lore asks for), the lab's reagents, the armoury's silver, the kitchen's tins, and the vault's once-a-member
     * starter kit.
     */
    private void bunker(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(BUNKER_LIBRARY, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(12).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                        .add(LootItem.lootTableItem(Items.INK_SAC).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(notes("place", 8, 1, 3))
                        .add(notes("arcane", 6, 1, 2))
                        .add(LootItem.lootTableItem(Items.FEATHER).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).setWeight(2)
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell()))));
        out.accept(BUNKER_ARCHIVE, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 5))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                        .add(notes("place", 12, 1, 4))
                        .add(notes("relic", 5, 1, 2))
                        .add(LootItem.lootTableItem(Items.MAP).setWeight(4))
                        .add(LootItem.lootTableItem(Items.WRITABLE_BOOK).setWeight(3))
                        .add(LootItem.lootTableItem(Items.INK_SAC).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))));
        out.accept(BUNKER_LAB, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 5))
                        .add(LootItem.lootTableItem(AllItems.DEAD_MANS_BLOOD.get()).setWeight(4))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.GLASS_BOTTLE).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(notes("arcane", 6, 1, 2))));
        out.accept(BUNKER_ARMORY, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.ARROW).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(4, 12))))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(AllItems.SILVER_MACHETE.get()).setWeight(1))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(1))
                        .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))));
        out.accept(BUNKER_KITCHEN, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 5))
                        .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.BAKED_POTATO).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.PUMPKIN_PIE).setWeight(5))
                        .add(LootItem.lootTableItem(Items.COOKED_BEEF).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.HONEY_BOTTLE).setWeight(3))
                        .add(LootItem.lootTableItem(Items.COOKIE).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))));
        // The vault: each member opens it once with the Bunker Key (vanilla vaults remember who has).
        out.accept(BUNKER_VAULT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(notes("place", 1, 4, 6)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(notes("arcane", 1, 2, 4)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.PAPER).apply(SetItemCountFunction.setCount(ConstantValue.exactly(16))))
                        .add(LootItem.lootTableItem(Items.INK_SAC).apply(SetItemCountFunction.setCount(ConstantValue.exactly(8)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(AllItems.DEAD_MANS_BLOOD.get()).setWeight(3))
                        .add(LootItem.lootTableItem(AllItems.SILVER_MACHETE.get()).setWeight(1))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(1))));
    }
}
