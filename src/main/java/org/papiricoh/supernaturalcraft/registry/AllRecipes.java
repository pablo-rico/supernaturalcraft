package org.papiricoh.supernaturalcraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

public class AllRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, SupernaturalCraft.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SupernaturalCraft.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<RitualRecipe>> RITUAL = RECIPE_TYPES.register("ritual",
            () -> RecipeType.simple(SupernaturalCraft.asResource("ritual")));
    public static final DeferredHolder<RecipeSerializer<?>, RitualRecipe.Serializer> RITUAL_SERIALIZER =
            RECIPE_SERIALIZERS.register("ritual", RitualRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe>> BOWL_SPELL = RECIPE_TYPES.register("bowl_spell",
            () -> RecipeType.simple(SupernaturalCraft.asResource("bowl_spell")));
    public static final DeferredHolder<RecipeSerializer<?>, org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe.Serializer> BOWL_SPELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("bowl_spell", org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe.Serializer::new);

    public static final DeferredRegister<net.neoforged.neoforge.common.crafting.IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.INGREDIENT_TYPES, SupernaturalCraft.MODID);
    /** A book and quill (or a written book) with a name in it: Metatron's summoning needs his. */
    public static final DeferredHolder<net.neoforged.neoforge.common.crafting.IngredientType<?>,
            net.neoforged.neoforge.common.crafting.IngredientType<org.papiricoh.supernaturalcraft.ritual.WrittenNameIngredient>> WRITTEN_NAME =
            INGREDIENT_TYPES.register("written_name", () -> new net.neoforged.neoforge.common.crafting.IngredientType<>(
                    org.papiricoh.supernaturalcraft.ritual.WrittenNameIngredient.CODEC));

    public static void init() {
    }
}
