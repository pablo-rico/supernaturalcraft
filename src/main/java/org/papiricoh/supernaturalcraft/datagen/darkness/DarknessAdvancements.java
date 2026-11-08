package org.papiricoh.supernaturalcraft.datagen.darkness;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.function.Consumer;

/**
 * Season 11's advancements (v0.42), hung from the Mark taken off ({@code main/unmarked}); every step is granted by code
 * ({@code Darknesses.advance}, {@code Darknesses.handClaimed}). Returns the last one (Amara takes Casifer), the parent of her
 * kill advancement {@code main/dawn}.
 */
public final class DarknessAdvancements {

    private DarknessAdvancements() {
    }

    public static AdvancementHolder generate(Consumer<AdvancementHolder> out, AdvancementHolder unmarked) {
        AdvancementHolder released = impossible(out, unmarked, "the_darkness", AllItems.VOID_ESSENCE.get(), AdvancementType.GOAL);
        AdvancementHolder superior = impossible(out, released, "form_and_void", AllItems.VOID_ESSENCE.get(), AdvancementType.TASK);
        AdvancementHolder cage = impossible(out, superior, "o_brother_where_art_thou", AllItems.CAGE_SHACKLE.get(), AdvancementType.TASK);
        AdvancementHolder casifer = impossible(out, cage, "the_devil_in_the_details", AllItems.ARCHANGEL_BLADE.get(), AdvancementType.GOAL);
        AdvancementHolder hand = impossible(out, casifer, "hand_of_god", AllItems.VOID_ESSENCE.get(), AdvancementType.GOAL);
        AdvancementHolder revealed = impossible(out, hand, "dont_call_me_shurley", AllItems.TYPEWRITER.get(), AdvancementType.GOAL);
        return impossible(out, revealed, "we_happy_few", AllItems.ECLIPSE_SIGHT.get(), AdvancementType.TASK);
    }

    private static AdvancementHolder impossible(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemLike icon,
                                                AdvancementType type) {
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, Component.translatable("advancement.supernaturalcraft." + name),
                        Component.translatable("advancement.supernaturalcraft." + name + ".desc"), null, type, true, true, false)
                .addCriterion("done", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(out, SupernaturalCraft.MODID + ":main/" + name);
    }
}
