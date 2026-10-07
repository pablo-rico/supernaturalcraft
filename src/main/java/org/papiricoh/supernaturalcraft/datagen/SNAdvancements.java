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
        AdvancementHolder key = obtain(out, ember, "lock_and_key", AllItems.KEY_TO_THE_CAGE.get(), AdvancementType.GOAL, 50);
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
