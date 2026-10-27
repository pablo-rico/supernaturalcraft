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
        // A ghost leaves ectoplasm when dispersed (by code); the crossroads demon's page drop is by code too.
        add(AllEntities.GHOST.get(), LootTable.lootTable());
        witches();
        // The Author drops nothing: his gifts are written for each hunter (AuthorRewards), once.
        add(AllEntities.CHUCK.get(), LootTable.lootTable());
        add(AllEntities.CROSSROADS_DEMON.get(), LootTable.lootTable()
                .withPool(drop(AllItems.DEMON_BLOOD.get(), 0, 1).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                // v0.25: the blood of a King of Hell (Crowley was the crossroads' king), one of the three for the righteous bone.
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.5f))
                        .add(LootItem.lootTableItem(AllItems.KINGS_BLOOD.get()))));
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
        // v0.12: Michael's spoils are left by code (MichaelSpoils), per hunter; a soldier of the Host may drop a shard.
        add(AllEntities.MICHAEL.get(), LootTable.lootTable());
        // Gabriel's spoils are dealt by code, per hunter (GabrielSpoils); his doubles leave nothing.
        add(AllEntities.GABRIEL.get(), LootTable.lootTable());
        add(AllEntities.GABRIEL_DOUBLE.get(), LootTable.lootTable());
        add(AllEntities.RAPHAEL.get(), LootTable.lootTable());
        // v0.21: Cain's spoils are his own (CainSpoils).
        add(AllEntities.CAIN.get(), LootTable.lootTable());
        // v0.21 (P5): Abaddon's spoils are her own (AbaddonSpoils); her knights carry scraps of their plate, her heralds blood.
        add(AllEntities.ABADDON.get(), LootTable.lootTable());
        add(AllEntities.ABADDON_KNIGHT.get(), LootTable.lootTable()
                .withPool(drop(AllItems.DEMON_BLOOD.get(), 0, 1).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(drop(net.minecraft.world.item.Items.IRON_NUGGET, 1, 4))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                        .add(LootItem.lootTableItem(net.minecraft.world.item.Items.NETHERITE_SCRAP))));
        add(AllEntities.ABADDON_HERALD.get(), LootTable.lootTable()
                .withPool(drop(AllItems.DEMON_BLOOD.get(), 0, 2).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                .withPool(drop(AllItems.SULFUR.get(), 1, 2)));
        // v0.21 (P6): the witch bosses' spoils are their own (RowenaSpoils, GrandWitchSpoils).
        add(AllEntities.ROWENA.get(), LootTable.lootTable());
        add(AllEntities.GRAND_WITCH.get(), LootTable.lootTable());
        add(AllEntities.GARRISON_ANGEL.get(), LootTable.lootTable());
        // v0.17: Henry leaves nothing; the monsters' field notes are dealt by code (members only). Agent A may add drops.
        // v0.25: an Alpha's blood (a vampire's will do), one of the three for the righteous bone.
        add(AllEntities.VAMPIRE.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.25f))
                        .add(LootItem.lootTableItem(AllItems.ALPHA_BLOOD.get()))));
        // v0.25: a Leviathan's pockets (what it took from the villager it ate), maybe a SucroCorp sample; its head is MISC and has no table.
        add(AllEntities.LEVIATHAN.get(), LootTable.lootTable()
                .withPool(drop(net.minecraft.world.item.Items.EMERALD, 0, 2))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.3f))
                        .add(LootItem.lootTableItem(AllItems.TURDUCKEN_SLAMMER.get()))));
        // v0.28: SucroCorp's people: an executive's gold, a guard's emeralds, a chemist's borax; any of them a SucroCorp sample.
        add(AllEntities.LEVIATHAN_EXECUTIVE.get(), LootTable.lootTable()
                .withPool(drop(net.minecraft.world.item.Items.GOLD_INGOT, 0, 2))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.3f))
                        .add(LootItem.lootTableItem(AllItems.TURDUCKEN_SLAMMER.get()))));
        add(AllEntities.LEVIATHAN_GUARD.get(), LootTable.lootTable()
                .withPool(drop(net.minecraft.world.item.Items.EMERALD, 0, 2))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.3f))
                        .add(LootItem.lootTableItem(AllItems.TURDUCKEN_SLAMMER.get()))));
        add(AllEntities.LEVIATHAN_CHEMIST.get(), LootTable.lootTable()
                .withPool(drop(AllItems.BORAX.get(), 0, 1))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.3f))
                        .add(LootItem.lootTableItem(AllItems.TURDUCKEN_SLAMMER.get()))));
        // Dick Roman's spoils are his own (DickSpoils), per hunter.
        add(AllEntities.DICK_ROMAN.get(), LootTable.lootTable());
        add(AllEntities.WEREWOLF.get(), LootTable.lootTable());
        add(AllEntities.SHAPESHIFTER.get(), LootTable.lootTable());
        // v0.18: Heaven's tables (spoils of Naomi and Zachariah are dealt by code, per hunter).
        org.papiricoh.supernaturalcraft.datagen.heaven.HeavenLoot.entities(this::add);
        // v0.13: a rival hunter's pockets: salt, maybe holy water, rarely a round for the Colt.
        add(AllEntities.RIVAL_HUNTER.get(), LootTable.lootTable()
                .withPool(drop(AllItems.SALT.get(), 1, 3))
                .withPool(drop(AllItems.HOLY_WATER.get(), 0, 1))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.05f))
                        .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()))));
        add(AllEntities.HOST_ANGEL.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.25f))
                        .add(LootItem.lootTableItem(AllItems.CHOIR_SHARD.get()))));
        // v0.22: a few feathers, shed in the fall (never their blades).
        add(AllEntities.ANGEL.get(), LootTable.lootTable().withPool(drop(Items.FEATHER, 0, 2)));
        add(AllEntities.FALLEN_ANGEL.get(), LootTable.lootTable().withPool(drop(Items.FEATHER, 1, 3)));
        // v0.26: the Chain's rank and file.
        for (var type : java.util.List.of(AllEntities.DEMON_BRUTE.get(), AllEntities.SULFUR_PRIEST.get(), AllEntities.DEMON_FLAYER.get(),
                AllEntities.DEMON_TORMENTOR.get()))
            add(type, LootTable.lootTable()
                    .withPool(drop(AllItems.DEMON_BLOOD.get(), 0, 1).when(LootItemKilledByPlayerCondition.killedByPlayer()))
                    .withPool(drop(AllItems.SULFUR.get(), 1, 3)));
        for (var type : java.util.List.of(AllEntities.ANGEL_TEMPLAR.get(), AllEntities.ANGEL_HERALD.get(), AllEntities.ANGEL_CHOIRMASTER.get(),
                AllEntities.ANGEL_DOMINION.get()))
            add(type, LootTable.lootTable().withPool(drop(Items.FEATHER, 1, 2)));
        add(AllEntities.GRIGORI.get(), LootTable.lootTable().withPool(drop(Items.FEATHER, 1, 3)));
        // v0.11: a Horseman's ring, trophy and horse are left by code (HorsemanEntity.dropSpoils), every victory.
        for (var type : java.util.List.of(AllEntities.WAR.get(), AllEntities.FAMINE.get(), AllEntities.PESTILENCE.get(),
                AllEntities.DEATH.get())) {
            add(type, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(AllItems.COLT_BULLET.get()).apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4))))));
        }
        add(AllEntities.HORSEMAN_STEED.get(), LootTable.lootTable().withPool(drop(Items.LEATHER, 0, 2)));
        add(AllEntities.WAR_MIRAGE.get(), LootTable.lootTable());
        add(AllEntities.HUNGRY_THRALL.get(), LootTable.lootTable().withPool(drop(Items.ROTTEN_FLESH, 0, 1)));
        add(AllEntities.FLY_SWARM.get(), LootTable.lootTable());
        add(AllEntities.REAPER.get(), LootTable.lootTable());
        azazel();
        lilith();
        metatron();
    }

    /** Metatron leaves the Angel Tablet, charged, his likeness, shards of the choir and a few consecrated rounds. */
    private void metatron() {
        LootTable.Builder table = LootTable.lootTable();
        for (Item item : new Item[]{AllItems.ANGEL_TABLET.get(), AllItems.METATRON_TROPHY.get()}) {
            table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(item)));
        }
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.CHOIR_SHARD.get())
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2, 4)))));
        table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(AllItems.COLT_BULLET.get())
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 5)))));
        add(AllEntities.METATRON.get(), table);
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

    /**
     * v0.21: a witch's satchel: blank taglocks, herbs, now and then a witchcraft page (tier 1-2); her blood and a coven witch's
     * calling card drop by code ({@code coven.CovenEvents}), a familiar's stolen taglock too. A hexed villager leaves nothing worth
     * having.
     */
    private void witches() {
        add(AllEntities.HEDGE_WITCH.get(), LootTable.lootTable()
                .withPool(drop(AllItems.TAGLOCK.get(), 0, 1))
                .withPool(herbs(0, 2))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer()).when(LootItemRandomChanceCondition.randomChance(0.08f))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction
                                .randomSpell(org.papiricoh.supernaturalcraft.magic.school.School.WITCHCRAFT, 2)))));
        add(AllEntities.COVEN_WITCH.get(), LootTable.lootTable()
                .withPool(drop(AllItems.TAGLOCK.get(), 1, 2))
                .withPool(herbs(1, 3))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer()).when(LootItemRandomChanceCondition.randomChance(0.25f))
                        .add(LootItem.lootTableItem(AllItems.SPELL_PAGE.get()).apply(org.papiricoh.supernaturalcraft.loot.RandomBowlSpellFunction
                                .randomSpell(org.papiricoh.supernaturalcraft.magic.school.School.WITCHCRAFT, 2)))));
        add(AllEntities.FAMILIAR.get(), LootTable.lootTable());
        add(AllEntities.HEXED_VILLAGER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(0.5f))
                        .add(LootItem.lootTableItem(Items.ROTTEN_FLESH))));
    }

    /** The herbs in a witch's satchel. */
    private static LootPool.Builder herbs(int min, int max) {
        return LootPool.lootPool().setRolls(UniformGenerator.between(min, max))
                .add(LootItem.lootTableItem(Items.SWEET_BERRIES).setWeight(4))
                .add(LootItem.lootTableItem(Items.GLOW_BERRIES).setWeight(2))
                .add(LootItem.lootTableItem(Items.RED_MUSHROOM).setWeight(3))
                .add(LootItem.lootTableItem(Items.BROWN_MUSHROOM).setWeight(3))
                .add(LootItem.lootTableItem(Items.FERN).setWeight(3))
                .add(LootItem.lootTableItem(Items.SPIDER_EYE).setWeight(1));
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
