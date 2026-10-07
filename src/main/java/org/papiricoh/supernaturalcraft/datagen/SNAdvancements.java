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
        // Metatron, after Lucifer.
        AdvancementHolder metatron = kill(out, lucifer, "scribe_of_god", AllItems.ANGEL_TABLET.get(), AllEntities.METATRON.get(), AdvancementType.CHALLENGE, 800);
        Advancement.Builder.advancement().parent(metatron)
                .display(AllItems.METATRON_TROPHY.get(), title("obeyed"), desc("obeyed"), null, AdvancementType.TASK, true, true, false)
                .addCriterion("kept", net.minecraft.advancements.CriteriaTriggers.IMPOSSIBLE.createCriterion(new net.minecraft.advancements.critereon.ImpossibleTrigger.TriggerInstance()))
                .save(out, id("obeyed"));
        obtain(out, lucifer, "nothing_it_cant_kill", AllItems.THE_COLT.get(), AdvancementType.GOAL, 0);
        // v0.11: the Horsemen, after Lucifer; each victory leaves his ring.
        kill(out, lucifer, "war", AllItems.RING_OF_WAR.get(), AllEntities.WAR.get(), AdvancementType.GOAL, 300);
        kill(out, lucifer, "famine", AllItems.RING_OF_FAMINE.get(), AllEntities.FAMINE.get(), AdvancementType.GOAL, 300);
        kill(out, lucifer, "pestilence", AllItems.RING_OF_PESTILENCE.get(), AllEntities.PESTILENCE.get(), AdvancementType.GOAL, 300);
        // v0.8: the spell bowl, ghosts and the crossroads (granted by code).
        AdvancementHolder firstSpell = impossible(out, grimoire, "first_spell", AllItems.SPELL_BOWL.get(), AdvancementType.TASK);
        impossible(out, firstSpell, "salt_and_burn", AllItems.ECTOPLASM.get(), AdvancementType.GOAL);
        AdvancementHolder deal = impossible(out, demon, "deal_with_the_devil", AllItems.CROSSROADS_CONTRACT.get(), AdvancementType.TASK);
        impossible(out, deal, "debt_paid", AllItems.HELLHOUND_FANG.get(), AdvancementType.GOAL);
        // The Hymnal Spire and the Broken Chorus.
        AdvancementHolder spire = Advancement.Builder.advancement().parent(holyWater)
                .display(AllItems.CHOIR_ALTAR.get(), title("hymnal_spire"), desc("hymnal_spire"), null, AdvancementType.TASK, true, true, false)
                .addCriterion("found", net.minecraft.advancements.critereon.PlayerTrigger.TriggerInstance.located(
                        net.minecraft.advancements.critereon.LocationPredicate.Builder.inStructure(
                                registries.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getOrThrow(SNStructures.HYMNAL_SPIRE))))
                .save(out, id("hymnal_spire"));
        AdvancementHolder hymn = obtain(out, spire, "shattered_hymn", AllItems.SHATTERED_HYMN.get(), AdvancementType.TASK, 0);
        AdvancementHolder chorus = kill(out, hymn, "silence_falls", AllItems.CHOIR_TROPHY.get(), AllEntities.BROKEN_CHORUS.get(), AdvancementType.CHALLENGE, 700);
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
