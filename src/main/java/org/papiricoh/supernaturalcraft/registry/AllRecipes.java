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

    public static void init() {
    }
}
