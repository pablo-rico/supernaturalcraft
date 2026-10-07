package org.papiricoh.supernaturalcraft.bowl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import org.papiricoh.supernaturalcraft.bowl.effect.BowlSpellEffect;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;

import java.util.List;
import java.util.Optional;

/**
 * A bowl spell: the liquids and ingredients the bowl must hold (any order, nothing spare), the
 * Latin to recite once it is lit, and what happens then. Several recipes may teach one
 * {@link #spell} (the Locating spell has one per kind of target): a spell page teaches the spell,
 * and with it every recipe for it.
 *
 * <p>The incantation is diegetic, the same in every language, so it lives in the JSON rather than
 * in the lang files. The spell's name and description do come from lang:
 * {@code bowl_spell.<namespace>.<spell path>} and {@code .desc}.
 */
public record BowlSpellRecipe(Optional<ResourceLocation> spellId, List<BowlLiquid> liquids, List<Ingredient> ingredients,
                              String incantation, float manaCost, RitualConditions conditions, int smokeColor,
                              float difficulty, int pageWeight, BowlSpellEffect effect) implements Recipe<BowlInput> {

    public static final MapCodec<BowlSpellRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(BowlSpellRecipe::spellId),
            Dose.LIQUID_CODEC.listOf(0, BowlContents.MAX_DOSES).optionalFieldOf("liquids", List.of()).forGetter(BowlSpellRecipe::liquids),
            Ingredient.CODEC_NONEMPTY.listOf(0, BowlContents.MAX_ITEMS).optionalFieldOf("ingredients", List.of()).forGetter(BowlSpellRecipe::ingredients),
            Codec.STRING.fieldOf("incantation").forGetter(BowlSpellRecipe::incantation),
            Codec.floatRange(0, 1000).optionalFieldOf("mana_cost", 0f).forGetter(BowlSpellRecipe::manaCost),
            RitualConditions.CODEC.optionalFieldOf("conditions", RitualConditions.NONE).forGetter(BowlSpellRecipe::conditions),
            Codec.INT.optionalFieldOf("smoke_color", 0x9FB7FF).forGetter(BowlSpellRecipe::smokeColor),
            Codec.floatRange(0.25f, 4f).optionalFieldOf("difficulty", 1f).forGetter(BowlSpellRecipe::difficulty),
            Codec.intRange(0, 1000).optionalFieldOf("page_weight", 10).forGetter(BowlSpellRecipe::pageWeight),
            BowlSpellEffect.CODEC.fieldOf("effect").forGetter(BowlSpellRecipe::effect)
    ).apply(i, BowlSpellRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BowlSpellRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    /**
     * The spell this recipe teaches: its {@code spell} field, or else the recipe's file name in its
     * namespace ({@code ns:bowl_spell/second_sight} teaches {@code ns:second_sight}).
     */
    public ResourceLocation spell(ResourceLocation recipeId) {
        return spellId.orElseGet(() -> recipeId.withPath(p -> p.substring(p.lastIndexOf('/') + 1)));
    }

    /** Liquids as a multiset, ingredients shapelessly and exactly: nothing spare in the bowl. */
    @Override
    public boolean matches(BowlInput input, Level level) {
        if (!BowlMix.sameLiquids(liquids, input.liquids())) return false;
        List<ItemStack> held = input.items().stream().filter(s -> !s.isEmpty()).toList();
        if (held.size() != ingredients.size()) return false;
        return held.isEmpty() || RecipeMatcher.findMatches(held, ingredients) != null;
    }

    @Override
    public ItemStack assemble(BowlInput input, HolderLookup.Provider registries) {
        return effect.displayResult().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return effect.displayResult();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AllRecipes.BOWL_SPELL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AllRecipes.BOWL_SPELL.get();
    }

    public static class Serializer implements RecipeSerializer<BowlSpellRecipe> {
        @Override
        public MapCodec<BowlSpellRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BowlSpellRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
