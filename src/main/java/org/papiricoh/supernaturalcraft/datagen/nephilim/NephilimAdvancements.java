package org.papiricoh.supernaturalcraft.datagen.nephilim;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.ChangeDimensionTrigger;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.empty.EmptyDimension;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;

import java.util.function.Consumer;

/**
 * Seasons 12-14's advancements (v0.41), the whole tree under Amara's {@code main/dawn}. The kills are kill advancements (bosses
 * by registry id, so this never needs the slices' classes); the story steps are granted by code ({@code Nephilims.advance},
 * AU Michael's flight, the vessel's fall, the Hellforge); the two crossings are dimension triggers.
 */
public final class NephilimAdvancements {

    /** The Apocalypse World's level key, by id (the slice owns the class). */
    static final ResourceKey<Level> APOCALYPSE_WORLD = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            SupernaturalCraft.asResource("apocalypse_world"));

    private NephilimAdvancements() {
    }

    public static void generate(Consumer<AdvancementHolder> out, AdvancementHolder dawn) {
        AdvancementHolder ultimatum = impossible(out, dawn, "keep_calm_and_carry_on", BritishRegistry.HESS_TROPHY_ITEM.get(), AdvancementType.TASK);
        kill(out, ultimatum, "the_british_invasion", BritishRegistry.KETCH_TROPHY_ITEM.get(), "ketch", AdvancementType.GOAL, 600);
        kill(out, ultimatum, "who_we_are", BritishRegistry.HESS_TROPHY_ITEM.get(), "hess", AdvancementType.GOAL, 700);
        AdvancementHolder trace = impossible(out, ultimatum, "trace_the_nephilim", NephilimRegistry.NEPHILIM_BLOOD.get(), AdvancementType.TASK);
        AdvancementHolder dagon = kill(out, trace, "all_along_the_watchtower", BritishRegistry.DAGON_TROPHY_ITEM.get(), "dagon",
                AdvancementType.CHALLENGE, 900);
        AdvancementHolder born = impossible(out, dagon, "the_nephilim", NephilimRegistry.NEPHILIM_BLOOD.get(), AdvancementType.GOAL);
        AdvancementHolder asmodeus = kill(out, born, "bring_em_back_alive", NephilimRegistry.ASMODEUS_TROPHY_ITEM.get(), "asmodeus",
                AdvancementType.GOAL, 800);
        impossible(out, asmodeus, "lance_of_michael", NephilimRegistry.ARCHANGEL_BANE_SHARD.get(), AdvancementType.GOAL);
        AdvancementHolder au = Advancement.Builder.advancement().parent(born)
                .display(NephilimRegistry.NEPHILIM_BLOOD.get(), title("the_bad_place"), desc("the_bad_place"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("crossed", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(APOCALYPSE_WORLD))
                .save(out, id("the_bad_place"));
        impossible(out, au, "war_of_the_worlds", ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM.get(), AdvancementType.TASK);
        AdvancementHolder auMichael = impossible(out, au, "let_the_good_times_roll", ApocWorldRegistry.AU_MICHAEL_TROPHY_ITEM.get(),
                AdvancementType.CHALLENGE);
        AdvancementHolder offer = impossible(out, auMichael, "stranger_in_a_strange_land", NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM.get(),
                AdvancementType.TASK);
        AdvancementHolder spear = impossible(out, offer, "the_spear", NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM.get(), AdvancementType.CHALLENGE);
        AdvancementHolder absence = impossible(out, spear, "the_fall_of_the_son", NephilimRegistry.SOULLESS_JACK_TROPHY_ITEM.get(), AdvancementType.TASK);
        AdvancementHolder moriah = kill(out, absence, "moriah", NephilimRegistry.SOULLESS_JACK_TROPHY_ITEM.get(), "soulless_jack",
                AdvancementType.CHALLENGE, 1200);
        AdvancementHolder empty = Advancement.Builder.advancement().parent(moriah)
                .display(NephilimRegistry.NEPHILIM_GRACE.get(), title("into_the_empty"), desc("into_the_empty"), null, AdvancementType.GOAL, true, true, false)
                .addCriterion("entered", ChangeDimensionTrigger.TriggerInstance.changedDimensionTo(EmptyDimension.LEVEL))
                .save(out, id("into_the_empty"));
        kill(out, empty, "despair", NephilimRegistry.SHADOW_TROPHY_ITEM.get(), "shadow", AdvancementType.CHALLENGE, 1500);
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

    private static AdvancementHolder impossible(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemLike icon,
                                                AdvancementType type) {
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, title(name), desc(name), null, type, true, true, false)
                .addCriterion("done", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(out, id(name));
    }

    private static AdvancementHolder kill(Consumer<AdvancementHolder> out, AdvancementHolder parent, String name, ItemLike icon,
                                          String entity, AdvancementType type, int xp) {
        EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(SupernaturalCraft.asResource(entity));
        return Advancement.Builder.advancement().parent(parent)
                .display(icon, title(name), desc(name), null, type, true, true, type == AdvancementType.CHALLENGE)
                .addCriterion("killed", KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(t)))
                .rewards(AdvancementRewards.Builder.experience(xp))
                .save(out, id(name));
    }
}
