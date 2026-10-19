package org.papiricoh.supernaturalcraft.bowl.page;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.magic.school.SchoolCodecs;

import java.util.Optional;

/**
 * A spell page that can be found (v0.21), from the {@code bowl_spell_page} datapack registry
 * ({@code data/<ns>/supernaturalcraft/bowl_spell_page/<id>.json}). Its id is what the page item carries and what reading it
 * teaches ({@code ArcanaData.learnRite}); a recipe is known when the player knows its group (its {@code spell} field) or its own
 * id ({@code bowl.BowlSpells#knows}), so a page may teach a whole group or a single recipe.
 *
 * @param school what the page's spells belong to (learning gate)
 * @param tier   1-3, 4 for a Master's: learning it needs that rank in {@code school} ({@code magic.mastery.MasteryRules})
 * @param group  the rite group it belongs with in books and lists (its own id if unset)
 * @param weight how often it turns up as a random page (0 = never: pages handed out on purpose)
 */
public record SpellPage(School school, int tier, Optional<ResourceLocation> group, int weight) {

    public static final Codec<SpellPage> CODEC = RecordCodecBuilder.create(i -> i.group(
            SchoolCodecs.CODEC.optionalFieldOf("school", School.WITCHCRAFT).forGetter(SpellPage::school),
            Codec.intRange(1, 4).optionalFieldOf("tier", 1).forGetter(SpellPage::tier),
            ResourceLocation.CODEC.optionalFieldOf("group").forGetter(SpellPage::group),
            Codec.intRange(0, 1000).optionalFieldOf("weight", 10).forGetter(SpellPage::weight)
    ).apply(i, SpellPage::new));

    /** The group, defaulting to the page's own id. */
    public ResourceLocation group(ResourceLocation id) {
        return group.orElse(id);
    }
}
