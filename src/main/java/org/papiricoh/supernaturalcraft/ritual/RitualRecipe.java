package org.papiricoh.supernaturalcraft.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import org.papiricoh.supernaturalcraft.registry.AllRecipes;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.effect.RitualEffect;

import java.util.List;

/**
 * A ritual: a floor pattern, offerings on the altar (any order), something to set it off, the
 * conditions it needs and what happens when it completes.
 */
public record RitualRecipe(ResourceKey<RitualPattern> pattern, List<Ingredient> ingredients, Ingredient activator,
                           boolean consumeActivator, RitualConditions conditions, int duration, float manaCost,
                           RitualEffect effect) implements Recipe<RitualInput> {

    public static final int MAX_OFFERINGS = 8;

    public static final MapCodec<RitualRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(SNRegistries.RITUAL_PATTERN).fieldOf("pattern").forGetter(RitualRecipe::pattern),
            Ingredient.CODEC_NONEMPTY.listOf(0, MAX_OFFERINGS).fieldOf("ingredients").forGetter(RitualRecipe::ingredients),
            Ingredient.CODEC_NONEMPTY.fieldOf("activator").forGetter(RitualRecipe::activator),
            Codec.BOOL.optionalFieldOf("consume_activator", false).forGetter(RitualRecipe::consumeActivator),
            RitualConditions.CODEC.optionalFieldOf("conditions", RitualConditions.NONE).forGetter(RitualRecipe::conditions),
            Codec.intRange(1, 1200).optionalFieldOf("duration", 60).forGetter(RitualRecipe::duration),
            Codec.floatRange(0, 1000).optionalFieldOf("mana_cost", 0f).forGetter(RitualRecipe::manaCost),
            RitualEffect.CODEC.fieldOf("effect").forGetter(RitualRecipe::effect)
    ).apply(i, RitualRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    /** Offerings match shapelessly and exactly — no spare items on the altar. */
    @Override
    public boolean matches(RitualInput input, Level level) {
        if (!activator.test(input.activator())) return false;
        List<ItemStack> offered = input.offerings().stream().filter(s -> !s.isEmpty()).toList();
        if (offered.size() != ingredients.size()) return false;
        return RecipeMatcher.findMatches(offered, ingredients) != null;
    }

    @Override
    public ItemStack assemble(RitualInput input, HolderLookup.Provider registries) {
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
        return AllRecipes.RITUAL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return AllRecipes.RITUAL.get();
    }

    public static class Serializer implements RecipeSerializer<RitualRecipe> {
        @Override
        public MapCodec<RitualRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RitualRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
