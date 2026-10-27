package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

/** Chest loot: the Hymnal Spire (a Shattered Hymn always in its temple), the bunker, the coven, haunted houses, the Chain, Hell. */
public class SNChestLoot implements LootTableSubProvider {

    public SNChestLoot(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        // Older spires (before v0.31) keep their temple and its vault.
        out.accept(SpireBuilder.TEMPLE_LOOT, spireTemple());
        out.accept(SpireBuilder.VAULT_LOOT, spireVault().withPool(hymn()));
        // v0.31: the remade spire. Its temple has one chest, so the Shattered Hymn is always there; the vault's gold went down
        // to the choristers' crypt.
        out.accept(SpireBuilder.SPIRE_TEMPLE_LOOT, spireTemple().withPool(hymn()));
        out.accept(SpireBuilder.CRYPT_LOOT, spireVault());
        out.accept(SpireBuilder.MONASTERY_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(Items.BREAD).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.HONEY_BOTTLE).setWeight(3))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(AllItems.ENOCHIAN_INK.get()).setWeight(4))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(sigilPage("radiance", 2)).add(sigilPage("halo", 2)).add(sigilPage("mend", 3))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).setWeight(2)
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell()))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(4))));
        out.accept(SpireBuilder.SUMMIT_LOOT, LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(Items.WHITE_CANDLE).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(1))));
        bunker(out);
        coven(out);
        haunted(out);
        chain(out);
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
                        .add(sigilPage("brimstone", 2))
                        .add(sigilPage("hellchain", 2))
                        .add(sigilPage("soul_rend", 1))
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
                        .add(sigilPage("salt_shot", 1))
                        .add(sigilPage("hex", 1))
                        .add(sigilPage("pain", 1))
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

    /** What an abandoned church keeps (v0.21). */
    private static LootTable.Builder church() {
        return LootTable.lootTable()
                // Sigil pages of Heaven and of the hunt (tiers 1-3).
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                        .add(sigilPage("smite", 4)).add(sigilPage("mend", 4)).add(sigilPage("radiance", 3)).add(sigilPage("halo", 3))
                        .add(sigilPage("banishing", 1)).add(sigilPage("repel", 4)).add(sigilPage("reveal", 4))
                        .add(sigilPage("salt_shot", 3)).add(sigilPage("exorcise", 2)).add(sigilPage("silver_edge", 2))
                        .add(sigilPage("snare", 2)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.WHITE_CANDLE).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(AllItems.CHRISM.get()).setWeight(2))
                        .add(LootItem.lootTableItem(AllItems.ENOCHIAN_INK.get()).setWeight(3))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 8))))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(Items.BELL).setWeight(1)));
    }

    /** v0.21: what an abandoned church and a witches' coven keep. */
    private static void coven(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/abandoned_church")), church());
        // v0.25: the crypt keeps a nun's bones too — the bone of a righteous mortal, for the Leviathans' end.
        out.accept(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/abandoned_church_crypt")), church()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.RIGHTEOUS_BONE.get()))));
        out.accept(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/witch_coven")), LootTable.lootTable()
                // Bowl pages of witchcraft (tiers 1-3).
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction
                                .randomSpell(org.papiricoh.supernaturalcraft.magic.school.School.WITCHCRAFT, 3))))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                        .add(LootItem.lootTableItem(AllItems.TAGLOCK.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.HEXED_INK.get()).setWeight(4)
                                .apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                                        org.papiricoh.supernaturalcraft.registry.AllDataComponents.SCHOOL.get(),
                                        org.papiricoh.supernaturalcraft.magic.school.School.WITCHCRAFT)))
                        .add(LootItem.lootTableItem(AllItems.BLOOD_VIAL.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))))
                        .add(LootItem.lootTableItem(Items.GLOW_BERRIES).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.SWEET_BERRIES).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.SPIDER_EYE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.CURSE_BAG.get()).setWeight(2))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(sigilPage("hex", 3)).add(sigilPage("pain", 2)).add(sigilPage("frost", 3)).add(sigilPage("glamour", 1))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(6))));
    }

    /** The temple's treasures (the Shattered Hymn is added where it belongs). */
    private static LootTable.Builder spireTemple() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(4, 7))
                        .add(sigilPage("radiance", 2))
                        .add(sigilPage("halo", 2))
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
                                .apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell())));
    }

    /** The vault's gold: the old temple's vault, and since v0.31 the choristers' crypt. */
    private static LootTable.Builder spireVault() {
        return LootTable.lootTable()
                // v0.21: the Master's sigil of Heaven, now and then.
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(sigilPage("judgement", 1))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(2)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(2, 4))
                        .add(LootItem.lootTableItem(Items.GOLD_BLOCK).setWeight(4))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(4))
                        .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1))
                        .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8))))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))));
    }

    private static LootPool.Builder hymn() {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.SHATTERED_HYMN.get()));
    }

    private static ResourceKey<LootTable> haunted(String room) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                ResourceLocation.parse(org.papiricoh.supernaturalcraft.haunted.HauntedHouseLayout.LOOT_PREFIX + room));
    }

    /** What a haunted house's rooms keep: a family's leftovers, and some hunter's who came before you (v0.31). */
    private static LootTable.Builder hauntedRoom() {
        return LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 6))
                        .add(LootItem.lootTableItem(AllItems.SALT.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.CANDLE).setWeight(7).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(Items.STRING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.BONE).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                        .add(LootItem.lootTableItem(Items.GOLD_NUGGET).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 7))))
                        .add(LootItem.lootTableItem(Items.CLOCK).setWeight(2))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(3))
                        .add(LootItem.lootTableItem(AllItems.ECTOPLASM.get()).setWeight(3)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(sigilPage("repel", 3)).add(sigilPage("reveal", 3)).add(sigilPage("salt_shot", 2)).add(sigilPage("exorcise", 1))
                        .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(8)));
    }

    /** v0.31: the haunted house — four rooms, and the study that opens once the whole family rests. */
    private static void haunted(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(haunted("cellar"), hauntedRoom()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                        .add(LootItem.lootTableItem(Items.GLASS_BOTTLE).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.COAL).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))));
        out.accept(haunted("bedroom"), hauntedRoom()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(3).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(3))
                        .add(LootItem.lootTableItem(AllItems.TAGLOCK.get()).setWeight(2))));
        out.accept(haunted("nursery"), hauntedRoom()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(Items.MUSIC_DISC_11).setWeight(2))
                        .add(LootItem.lootTableItem(Items.MUSIC_DISC_13).setWeight(2))
                        .add(LootItem.lootTableItem(Items.WHITE_WOOL).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.CAKE).setWeight(1))));
        out.accept(haunted("attic"), hauntedRoom()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                        .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(3))
                        .add(LootItem.lootTableItem(Items.SPYGLASS).setWeight(2))
                        .add(LootItem.lootTableItem(AllItems.BLOOD_VIAL.get()).setWeight(2))));
        // The study: the family's keepsakes and the hunter's notes they hid, for whoever gave them peace.
        out.accept(haunted("study"), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction.randomSpell())))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1, 2))
                        .add(sigilPage("exorcise", 3)).add(sigilPage("banishing", 2)).add(sigilPage("reveal", 3)).add(sigilPage("repel", 3))
                        .add(sigilPage("silver_edge", 2)).add(sigilPage("snare", 2)))
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(3, 5))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))
                        .add(LootItem.lootTableItem(AllItems.ECTOPLASM.get()).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.CHRISM.get()).setWeight(2))
                        .add(LootItem.lootTableItem(AllItems.ENOCHIAN_INK.get()).setWeight(3))
                        .add(LootItem.lootTableItem(AllItems.SILVER_MACHETE.get()).setWeight(1))
                        .add(LootItem.lootTableItem(Items.WRITABLE_BOOK).setWeight(3))));
    }

    /** v0.26: the treasure in the vaults of the Chain's strongholds (the Pit Castle, the Monastery). */
    private static void chain(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        out.accept(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/chain_pit_castle_vault")), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(4, 7))
                        .add(LootItem.lootTableItem(AllItems.DEMON_BLOOD.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(AllItems.SULFUR.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(1))
                        .add(LootItem.lootTableItem(Items.BLAZE_POWDER).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 5))))
                        .add(LootItem.lootTableItem(AllItems.CURSE_BAG.get()).setWeight(2)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(sigilPage("hellfire", 3)).add(sigilPage("brimstone", 3)).add(sigilPage("hellchain", 2)).add(sigilPage("soul_rend", 1))));
        out.accept(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/chain_monastery_vault")), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(4, 7))
                        .add(LootItem.lootTableItem(AllItems.CHRISM.get()).setWeight(4))
                        .add(LootItem.lootTableItem(AllItems.ENOCHIAN_INK.get()).setWeight(5))
                        .add(LootItem.lootTableItem(AllItems.HOLY_WATER.get()).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(2))
                        .add(LootItem.lootTableItem(Items.BOOK).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(sigilPage("radiance", 3)).add(sigilPage("halo", 3)).add(sigilPage("smite", 2)).add(sigilPage("judgement", 1))));
        // v0.28: SucroCorp's vault: the company's product, the one thing that frightens it, and its money.
        out.accept(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource("chests/chain_sucrocorp_vault")), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(UniformGenerator.between(4, 7))
                        .add(LootItem.lootTableItem(AllItems.TURDUCKEN_SLAMMER.get()).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(AllItems.BORAX.get()).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(AllItems.BORAX_SPRAY.get()).setWeight(3))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 6))))
                        .add(LootItem.lootTableItem(Items.EMERALD).setWeight(6).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 9))))
                        .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                        .add(LootItem.lootTableItem(Items.PAPER).setWeight(4).apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 8)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(AllItems.LEVIATHAN_TABLET.get()).setWeight(1))
                        .add(LootItem.lootTableItem(AllItems.BORAX_SPRAYER.get()).setWeight(2))
                        .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(3))));
    }

    /** A sigil page of {@code sigil} (v0.21: the new sigils are placed in the loot of the places that fit them). */
    private static LootItem.Builder<?> sigilPage(String sigil, int weight) {
        return LootItem.lootTableItem(AllItems.SIGIL_PAGE.get()).setWeight(weight)
                .apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                        org.papiricoh.supernaturalcraft.registry.AllDataComponents.SIGIL_PAGE.get(),
                        org.papiricoh.supernaturalcraft.SupernaturalCraft.asResource(sigil)));
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
                        .add(sigilPage("chain", 1))
                        .add(sigilPage("glyph", 1))
                        .add(sigilPage("linger", 1))
                        .add(sigilPage("quicken", 1))
                        .add(sigilPage("silver_edge", 1))
                        .add(sigilPage("glamour", 1))
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
