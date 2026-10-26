package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.function.Consumer;

/** The hunter's progression as advancements: salt, demons, rituals, the key, the Cage. */
public class SNAdvancements implements AdvancementProvider.AdvancementGenerator {

    private HolderLookup.Provider registries;

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> out, ExistingFileHelper existing) {
        this.registries = registries;
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(AllItems.SALT.get(), title("root"), desc("root"),
                        SupernaturalCraft.asResource("textures/block/ritual_altar_bottom.png"), AdvancementType.TASK, false, false, false)
                .addCriterion("salt", InventoryChangeTrigger.TriggerInstance.hasItems(AllItems.SALT.get()))
                .save(out, id("root"));

        AdvancementHolder demon = kill(out, root, "black_eyes", AllItems.DEMON_BLOOD.get(), AllEntities.BLACK_EYED_DEMON.get(), AdvancementType.TASK, 0);
        AdvancementHolder grimoire = obtain(out, root, "fine_print", AllItems.GRIMOIRE.get(), AdvancementType.TASK, 0);
        AdvancementHolder holyWater = obtain(out, grimoire, "christo", AllItems.HOLY_WATER.get(), AdvancementType.TASK, 0);
        AdvancementHolder trap = obtain(out, demon, "caught_in_the_trap", AllItems.DEVILS_TRAP.get(), AdvancementType.TASK, 0);
        AdvancementHolder ember = obtain(out, trap, "back_to_hell", AllItems.HELLFIRE_EMBER.get(), AdvancementType.GOAL, 20);
        obtain(out, ember, "the_knife", AllItems.RUBYS_KNIFE.get(), AdvancementType.TASK, 0);
        obtain(out, holyWater, "angel_blade", AllItems.ANGEL_BLADE.get(), AdvancementType.TASK, 0);
        // Azazel comes first: the Key to the Cage is forged with his blood.
        AdvancementHolder azazel = kill(out, trap, "yellow_eyed", AllItems.AZAZEL_BLOOD.get(), AllEntities.AZAZEL.get(), AdvancementType.GOAL, 150);
        Advancement.Builder.advancement().parent(trap)
                .display(AllItems.AZAZEL_TROPHY.get(), title("railroaded"), desc("railroaded"), null, AdvancementType.TASK, true, true, false)
                .addCriterion("held", net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE.createCriterion(new net.minecraft.advancements.critereon.ImpossibleTrigger.TriggerInstance()))
                .save(out, id("railroaded"));
        AdvancementHolder key = obtain(out, azazel, "lock_and_key", AllItems.KEY_TO_THE_CAGE.get(), AdvancementType.GOAL, 50);
        // Lilith: her fall breaks the last seal Lucifer's summoning needs.
        AdvancementHolder lilith = kill(out, azazel, "lucifer_rising", AllItems.LAST_SEAL.get(), AllEntities.LILITH.get(), AdvancementType.GOAL, 250);
        Advancement.Builder.advancement().parent(lilith)
                .display(AllItems.HOUND_WHISTLE.get(), title("no_deal"), desc("no_deal"), null, AdvancementType.TASK, true, true, false)
                .addCriterion("burned", net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE.createCriterion(new net.minecraft.advancements.critereon.ImpossibleTrigger.TriggerInstance()))
                .save(out, id("no_deal"));
        AdvancementHolder lucifer = kill(out, key, "devil_went_down", AllItems.ARCHANGEL_BLADE.get(), AllEntities.LUCIFER.get(), AdvancementType.CHALLENGE, 500);
        // The arsenal and the Darkness.
        AdvancementHolder forge = obtain(out, grimoire, "hellforge", AllItems.HELLFORGE.get(), AdvancementType.TASK, 0);
        obtain(out, forge, "graven", AllItems.RUNES.get(org.papiricoh.supernaturalcraft.weapon.Rune.EDGE).get(), AdvancementType.TASK, 0);
        AdvancementHolder cain = obtain(out, ember, "mark_of_cain", AllItems.FIRST_BLADE.get(), AdvancementType.GOAL, 30);
        obtain(out, cain, "whispers", AllItems.WHISPERING_CODEX.get(), AdvancementType.GOAL, 30);
        AdvancementHolder amara = kill(out, lucifer, "dawn", AllItems.ECLIPSE_TROPHY.get(), AllEntities.AMARA.get(), AdvancementType.CHALLENGE, 800);
        obtain(out, amara, "penumbra", AllItems.PENUMBRA.get(), AdvancementType.GOAL, 0);
        obtain(out, amara, "void_rune", AllItems.RUNES.get(org.papiricoh.supernaturalcraft.weapon.Rune.VOID).get(), AdvancementType.GOAL, 50);
        obtain(out, lucifer, "grace", AllItems.LUCIFERS_GRACE.get(), AdvancementType.TASK, 0);
        obtain(out, lucifer, "nothing_it_cant_kill", AllItems.THE_COLT.get(), AdvancementType.GOAL, 0);
        // v0.11: the Horsemen, after Lucifer; each victory leaves his ring.
        kill(out, lucifer, "war", AllItems.RING_OF_WAR.get(), AllEntities.WAR.get(), AdvancementType.GOAL, 300);
        kill(out, lucifer, "famine", AllItems.RING_OF_FAMINE.get(), AllEntities.FAMINE.get(), AdvancementType.GOAL, 300);
        kill(out, lucifer, "pestilence", AllItems.RING_OF_PESTILENCE.get(), AllEntities.PESTILENCE.get(), AdvancementType.GOAL, 300);
        // v0.14: the Trickster, a side road after Lucifer (his third prank noticed is granted by code).
        AdvancementHolder sighted = impossible(out, lucifer, "trickster_sighted", AllItems.CANDY_WRAPPER.get(), AdvancementType.TASK);
        // v0.27: a hunter sworn into a chain of command (awarded by code, ChainLife.join).
        impossible(out, azazel, "chain_sworn", AllItems.SEAL_OF_SUMMONS.get(), AdvancementType.TASK);
        kill(out, sighted, "changing_channels", AllItems.TRICKSTER_REMOTE.get(), AllEntities.GABRIEL.get(), AdvancementType.CHALLENGE, 400);
        // v0.16: Raphael, a side road once the three Horsemen are down (his rite asks for all three).
        AdvancementHolder freeToBe = kill(out, lucifer, "free_to_be_you_and_me", AllItems.RAPHAELS_STORMCALLER.get(), AllEntities.RAPHAEL.get(), AdvancementType.CHALLENGE, 500);
        // v0.21 (P4): Cain at his farm, a side road after Lucifer; the Mark (granted by code: met him, beaten him, bear it).
        AdvancementHolder metCain = impossible(out, lucifer, "met_cain", AllItems.CAINS_HONEY.get(), AdvancementType.TASK);
        // v0.25: Dick Roman, called with the righteous bone once Raphael is down.
        // v0.25: the milestones of the hunt for the Leviathans (granted by code: leviathan.LeviathanRules).
        AdvancementHolder helloCruelWorld = impossible(out, freeToBe, "hello_cruel_world", AllItems.BORAX.get(), AdvancementType.TASK);
        impossible(out, helloCruelWorld, "meet_the_new_boss", AllItems.TURDUCKEN_SLAMMER.get(), AdvancementType.GOAL);
        AdvancementHolder slashFiction = impossible(out, helloCruelWorld, "slash_fiction", net.minecraft.world.item.Items.IRON_AXE, AdvancementType.TASK);
        impossible(out, slashFiction, "out_with_the_old", AllItems.BORAX_SPRAY.get(), AdvancementType.GOAL);
        kill(out, freeToBe, "survival_of_the_fittest", AllItems.LEVIATHAN_TABLET.get(), AllEntities.DICK_ROMAN.get(), AdvancementType.CHALLENGE, 500);
        AdvancementHolder firstBorn = kill(out, metCain, "first_born", AllItems.CAIN_TROPHY.get(), AllEntities.CAIN.get(), AdvancementType.CHALLENGE, 400);
        AdvancementHolder marked = impossible(out, firstBorn, "marked", AllItems.FIRST_BLADE.get(), AdvancementType.GOAL);
        // v0.21 (P5): Abaddon, called up once Cain and the three Horsemen are down; her Book (read: granted by code).
        AdvancementHolder kingOfTheDamned = kill(out, firstBorn, "king_of_the_damned", AllItems.ABADDON_TROPHY.get(), AllEntities.ABADDON.get(),
                AdvancementType.CHALLENGE, 500);
        AdvancementHolder bookOfTheDamned = impossible(out, kingOfTheDamned, "book_of_the_damned", AllItems.BOOK_OF_THE_DAMNED.get(), AdvancementType.GOAL);
        // v0.21 (P6): the choice with the Book (granted by code), the witch boss of each road, and the Mark taken off (by code).
        AdvancementHolder gaveBook = impossible(out, bookOfTheDamned, "gave_the_book", AllItems.ROWENAS_CALLING_CARD.get(), AdvancementType.GOAL);
        AdvancementHolder keptBook = impossible(out, bookOfTheDamned, "kept_the_book", AllItems.BOOK_OF_THE_DAMNED.get(), AdvancementType.GOAL);
        kill(out, gaveBook, "grand_witch_defeated", AllItems.GRAND_WITCH_TROPHY.get(), AllEntities.GRAND_WITCH.get(), AdvancementType.CHALLENGE, 500);
        kill(out, keptBook, "rowena_defeated", AllItems.ROWENA_TROPHY.get(), AllEntities.ROWENA.get(), AdvancementType.CHALLENGE, 500);
        impossible(out, marked, "unmarked", AllItems.CAINS_HONEY.get(), AdvancementType.CHALLENGE);
        // v0.8: the spell bowl, ghosts and the crossroads (granted by code).
        AdvancementHolder firstSpell = impossible(out, grimoire, "first_spell", AllItems.SPELL_BOWL.get(), AdvancementType.TASK);
        impossible(out, firstSpell, "salt_and_burn", AllItems.ECTOPLASM.get(), AdvancementType.GOAL);
        // v0.21: the witchcraft and mastery (granted by code: the bowl's first aimed spell, a school's ranks).
        impossible(out, firstSpell, "first_targeted_spell", AllItems.TAGLOCK.get(), AdvancementType.TASK);
        // v0.21 (P3): the witches and Rowena, the abandoned churches (granted by code).
        AdvancementHolder metRowena = impossible(out, firstSpell, "met_rowena", AllItems.ROWENAS_CALLING_CARD.get(), AdvancementType.GOAL);
        impossible(out, metRowena, "rowenas_clue", net.minecraft.world.item.Items.HONEYCOMB, AdvancementType.TASK);
        impossible(out, firstSpell, "hexed_villager_saved", net.minecraft.world.item.Items.EMERALD, AdvancementType.TASK);
        impossible(out, holyWater, "consecrated_church", net.minecraft.world.item.Items.BELL, AdvancementType.GOAL);
        AdvancementHolder apprentice = impossible(out, grimoire, "mastery_apprentice", AllItems.SIGIL_PAGE.get(), AdvancementType.TASK);
        AdvancementHolder adept = impossible(out, apprentice, "mastery_adept", AllItems.GRIMOIRE.get(), AdvancementType.GOAL);
        impossible(out, adept, "mastery_master", AllItems.HEXED_INK.get(), AdvancementType.CHALLENGE);
        // The rites of the well (granted by code, every tenth rite).
        AdvancementHolder well10 = impossible(out, grimoire, "well_10", net.minecraft.world.item.Items.AMETHYST_SHARD, AdvancementType.TASK);
        AdvancementHolder well20 = impossible(out, well10, "well_20", AllItems.CHOIR_SHARD.get(), AdvancementType.GOAL);
        impossible(out, well20, "well_30", AllItems.LUCIFERS_GRACE.get(), AdvancementType.CHALLENGE);
        AdvancementHolder deal = impossible(out, demon, "deal_with_the_devil", AllItems.CROSSROADS_CONTRACT.get(), AdvancementType.TASK);
        impossible(out, deal, "debt_paid", AllItems.HELLHOUND_FANG.get(), AdvancementType.GOAL);
        impossible(out, deal, "wild_bargain", AllItems.CROSSROADS_BOX.get(), AdvancementType.GOAL);
        // The Hymnal Spire and the Broken Chorus.
        AdvancementHolder spire = Advancement.Builder.advancement().parent(holyWater)
                .display(AllItems.CHOIR_ALTAR.get(), title("hymnal_spire"), desc("hymnal_spire"), null, AdvancementType.TASK, true, true, false)
                .addCriterion("found", net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance.located(
                        net.minecraft.advancements.critereon.LocationPredicate.Builder.inStructure(
                                registries.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(SNStructures.HYMNAL_SPIRE))))
                .save(out, id("hymnal_spire"));
        AdvancementHolder hymn = obtain(out, spire, "shattered_hymn", AllItems.SHATTERED_HYMN.get(), AdvancementType.TASK, 0);
        AdvancementHolder chorus = kill(out, hymn, "silence_falls", AllItems.CHOIR_TROPHY.get(), AllEntities.BROKEN_CHORUS.get(), AdvancementType.CHALLENGE, 700);
        // Heaven's road (v0.12): Metatron after the Broken Chorus, then Michael.
        AdvancementHolder metatron = kill(out, chorus, "scribe_of_god", AllItems.ANGEL_TABLET.get(), AllEntities.METATRON.get(), AdvancementType.CHALLENGE, 800);
        Advancement.Builder.advancement().parent(metatron)
                .display(AllItems.METATRON_TROPHY.get(), title("obeyed"), desc("obeyed"), null, AdvancementType.TASK, true, true, false)
                .addCriterion("kept", net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE.createCriterion(new net.minecraft.advancements.critereon.ImpossibleTrigger.TriggerInstance()))
                .save(out, id("obeyed"));
        // v0.18: a Heaven of one's own, a side road once Metatron has fallen (its gate's rite asks for him).
        AdvancementHolder heavensDoor = Advancement.Builder.advancement().parent(metatron)
                .display(AllItems.CLOUD_STONE.get(), title("heavens_door"), desc("heavens_door"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("entered", net.minecraft.advancements.critereon.ChangeDimensionTrigger.TriggerInstance
                        .changedDimensionTo(org.papiricoh.supernaturalcraft.heaven.HeavenDimension.LEVEL))
                .rewards(AdvancementRewards.Builder.experience(100))
                .save(out, id("heavens_door"));
        impossible(out, heavensDoor, "memory_lane", AllItems.CLOUD_BRICKS.get(), AdvancementType.TASK);
        impossible(out, heavensDoor, "met_ash", net.minecraft.world.item.Items.JUKEBOX, AdvancementType.TASK);
        AdvancementHolder naomi = kill(out, heavensDoor, "deprogrammed", AllItems.NAOMIS_DRILL.get(), AllEntities.NAOMI.get(),
                AdvancementType.CHALLENGE, 600);
        AdvancementHolder zachariah = kill(out, naomi, "out_of_office", AllItems.ZACHARIAHS_BLADE.get(), AllEntities.ZACHARIAH.get(),
                AdvancementType.CHALLENGE, 800);
        impossible(out, zachariah, "home_sweet_heaven", AllItems.HEARTH.get(), AdvancementType.GOAL);
        AdvancementHolder michael = kill(out, metatron, "sword_of_heaven", AllItems.MICHAEL_LANCE.get(), AllEntities.MICHAEL.get(),
                AdvancementType.CHALLENGE, 1000);
        impossible(out, michael, "wings_of_heaven", AllItems.MICHAELS_GRACE.get(), AdvancementType.GOAL);
        Advancement.Builder.advancement().parent(michael)
                .display(AllItems.GENERAL_HELMET.get(), title("general"), desc("general"), null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("armour", InventoryChangeTrigger.TriggerInstance.hasItems(AllItems.GENERAL_HELMET.get(),
                        AllItems.GENERAL_CHESTPLATE.get(), AllItems.GENERAL_LEGGINGS.get(), AllItems.GENERAL_BOOTS.get()))
                .rewards(AdvancementRewards.Builder.experience(300))
                .save(out, id("general"));

        // Allegiance (v0.13): every rank, awarded by code (Allegiances / AllegianceRites).
        AdvancementHolder heeded = impossible(out, azazel, "heeded_the_call", AllItems.VIAL_OF_GRACE.get(), AdvancementType.TASK);
        AdvancementHolder angel1 = impossible(out, heeded, "angel_1", AllItems.VIAL_OF_GRACE.get(), AdvancementType.GOAL);
        AdvancementHolder angel2 = impossible(out, angel1, "angel_2", AllItems.SERAPH_WINGS.get(), AdvancementType.GOAL);
        AdvancementHolder angel3 = impossible(out, angel2, "angel_3", AllItems.ARCHANGEL_BLADE.get(), AdvancementType.GOAL);
        impossible(out, angel3, "angel_4", AllItems.MICHAEL_LANCE.get(), AdvancementType.CHALLENGE);
        AdvancementHolder soulBound = impossible(out, deal, "soul_bound", AllItems.CROSSROADS_CONTRACT.get(), AdvancementType.TASK);
        AdvancementHolder demon1 = impossible(out, soulBound, "demon_1", AllItems.DEMON_BLOOD.get(), AdvancementType.GOAL);
        AdvancementHolder demon2 = impossible(out, demon1, "demon_2", AllItems.AZAZEL_BLOOD.get(), AdvancementType.GOAL);
        AdvancementHolder demon3 = impossible(out, demon2, "demon_3", AllItems.FIRST_BLADE.get(), AdvancementType.GOAL);
        impossible(out, demon3, "demon_4", AllItems.FALLEN_STAR.get(), AdvancementType.CHALLENGE);
        AdvancementHolder hunter1 = impossible(out, grimoire, "hunter_1", AllItems.SALT.get(), AdvancementType.GOAL);
        AdvancementHolder hunter2 = impossible(out, hunter1, "hunter_2", AllItems.THE_COLT.get(), AdvancementType.GOAL);
        impossible(out, hunter2, "hunter_3", AllItems.SILVER_MACHETE.get(), AdvancementType.CHALLENGE);
        // The Men of Letters (v0.17): every rank of the Legacy, awarded by code (Henry's offer, then research).
        AdvancementHolder legacy1 = impossible(out, lucifer, "legacy_1", AllItems.BUNKER_KEY.get(), AdvancementType.GOAL);
        AdvancementHolder legacy2 = impossible(out, legacy1, "legacy_2", AllItems.MEN_OF_LETTERS_RING.get(), AdvancementType.GOAL);
        AdvancementHolder legacy3 = impossible(out, legacy2, "legacy_3", AllItems.SPELLWRIGHTS_SPECTACLES.get(), AdvancementType.GOAL);
        AdvancementHolder legacy4 = impossible(out, legacy3, "legacy_4", AllItems.HENRYS_CASE.get(), AdvancementType.GOAL);
        impossible(out, legacy4, "legacy_5", AllItems.AQUARIAN_STAR.get(), AdvancementType.CHALLENGE);
        Advancement.Builder.advancement().parent(hymn)
                .display(AllItems.CHOIR_BELLS.getFirst().get(), title("silence"), desc("silence"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("rang", net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE.createCriterion(new net.minecraft.advancements.critereon.ImpossibleTrigger.TriggerInstance()))
                .save(out, id("silence"));
        obtain(out, chorus, "seraph_wings", AllItems.SERAPH_WINGS.get(), AdvancementType.GOAL, 0);
        obtain(out, chorus, "hymn_rune", AllItems.RUNES.get(org.papiricoh.supernaturalcraft.weapon.Rune.HYMN).get(), AdvancementType.GOAL, 50);
        // Hell, the Horsemen and the Cage.
        AdvancementHolder hell = Advancement.Builder.advancement().parent(lucifer)
                .display(AllItems.HELLSTONE.get(), title("highway_to_hell"), desc("highway_to_hell"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("entered", net.minecraft.advancements.critereon.ChangeDimensionTrigger.TriggerInstance
                        .changedDimensionTo(org.papiricoh.supernaturalcraft.hell.HellDimension.LEVEL))
                .rewards(AdvancementRewards.Builder.experience(100))
                .save(out, id("highway_to_hell"));
        obtain(out, hell, "hellhound_heel", AllItems.HELLHOUND_FANG.get(), AdvancementType.TASK, 0);
        kill(out, hell, "pale_rider", AllItems.RING_OF_DEATH.get(), AllEntities.DEATH.get(), AdvancementType.CHALLENGE, 600);
        AdvancementHolder rings = Advancement.Builder.advancement().parent(hell)
                .display(AllItems.RING_OF_DEATH.get(), title("four_horsemen"), desc("four_horsemen"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("rings", InventoryChangeTrigger.TriggerInstance.hasItems(AllItems.RING_OF_WAR.get(), AllItems.RING_OF_FAMINE.get(),
                        AllItems.RING_OF_PESTILENCE.get(), AllItems.RING_OF_DEATH.get()))
                .rewards(AdvancementRewards.Builder.experience(200))
                .save(out, id("four_horsemen"));
        AdvancementHolder uncaged = kill(out, rings, "back_in_the_box", AllItems.FALLEN_STAR.get(), AllEntities.LUCIFER_UNCAGED.get(),
                AdvancementType.CHALLENGE, 1000);
        // v0.10: the Author. Meeting him is granted by code (the cabin's dialogue).
        AdvancementHolder author = impossible(out, uncaged, "the_author", AllItems.TYPEWRITER.get(), AdvancementType.GOAL);
        kill(out, author, "the_end", AllItems.THE_END_MANUSCRIPT.get(), AllEntities.CHUCK.get(), AdvancementType.CHALLENGE, 2000);
    }

    private static String id(String name) {
        return SupernaturalCraft.MODID + ":main/" + name;
    }

    private static Component title(String name) {
        return Component.translatable("advancement.supernaturalcraft." + name);
    }

    private static Component desc(String name) {
        return Component.translatable("advancement.supernaturalcraft." + name + ".desc");
    }

    /** An advancement only code grants (ChorusRewards.award). */
    private AdvancementHolder impossible(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemLike icon,
                                         AdvancementType type) {
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, title(name), desc(name), null, type, true, true, false)
                .addCriterion("done", net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE.createCriterion(
                        new net.minecraft.advancements.critereon.ImpossibleTrigger.TriggerInstance()))
                .save(out, id(name));
    }

    private AdvancementHolder obtain(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemLike item,
                                     AdvancementType type, int xp) {
        return Advancement.Builder.advancement().parent(parent)
                .display(item, title(name), desc(name), null, type, true, true, false)
                .addCriterion("has", InventoryChangeTrigger.TriggerInstance.hasItems(item))
                .rewards(AdvancementRewards.Builder.experience(xp))
                .save(out, id(name));
    }

    private AdvancementHolder kill(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemLike icon,
                                   EntityType<?> entity, AdvancementType type, int xp) {
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, title(name), desc(name), null, type, true, true, type == AdvancementType.CHALLENGE)
                .addCriterion("killed", KilledTrigger.TriggerInstance.playerKilledEntity(
                        EntityPredicate.Builder.entity().of(entity)))
                .rewards(AdvancementRewards.Builder.experience(xp))
                .save(out, id(name));
    }
}
