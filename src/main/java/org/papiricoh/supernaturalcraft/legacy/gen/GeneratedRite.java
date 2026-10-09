package org.papiricoh.supernaturalcraft.legacy.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;

import java.util.List;

/**
 * A bowl spell the Men of Letters' research has worked out for one hunter (v0.17), kept whole in their archive. The spell bowl
 * tries a hunter's generated rites when no recipe matches.
 *
 * @param index its place in the hunter's sequence
 * @param name its Latin name
 * @param liquids liquid ids ({@code bowl.BowlLiquid#id}), as a multiset
 * @param ingredients item ids, as a multiset
 * @param incantation the Latin words to recite
 * @param manaCost mana it costs
 * @param effect what it does (any registered {@link BowlSpellEffect})
 */
public record GeneratedRite(int index, String name, List<String> liquids, List<ResourceLocation> ingredients, String incantation,
                            float manaCost, BowlSpellEffect effect) {

    public static final String PREFIX = "rite/";

    public static final Codec<GeneratedRite> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("index").forGetter(GeneratedRite::index),
            Codec.STRING.fieldOf("name").forGetter(GeneratedRite::name),
            Codec.STRING.listOf().fieldOf("liquids").forGetter(GeneratedRite::liquids),
            ResourceLocation.CODEC.listOf().fieldOf("ingredients").forGetter(GeneratedRite::ingredients),
            Codec.STRING.fieldOf("incantation").forGetter(GeneratedRite::incantation),
            Codec.FLOAT.fieldOf("mana_cost").forGetter(GeneratedRite::manaCost),
            BowlSpellEffect.CODEC.fieldOf("effect").forGetter(GeneratedRite::effect)
    ).apply(i, GeneratedRite::new));

    public GeneratedRite {
        liquids = List.copyOf(liquids);
        ingredients = List.copyOf(ingredients);
    }

    /** The spell id it is learned as ({@code supernaturalcraft:rite/<n>}), like a bowl recipe's {@code spell}. */
    public ResourceLocation id() {
        return id(index);
    }

    public static ResourceLocation id(int index) {
        return SupernaturalCraft.asResource(PREFIX + index);
    }

    /** @return the index of a rite's spell id, or -1 if {@code id} is not one */
    public static int indexOf(ResourceLocation id) {
        if (!SupernaturalCraft.MODID.equals(id.getNamespace()) || !id.getPath().startsWith(PREFIX)) return -1;
        try {
            return Integer.parseInt(id.getPath().substring(PREFIX.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
