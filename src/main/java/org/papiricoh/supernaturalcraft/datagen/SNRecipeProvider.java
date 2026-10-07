package org.papiricoh.supernaturalcraft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.concurrent.CompletableFuture;

/** Vanilla-format recipes. Ritual recipes are hand-written JSON under data/.../recipe/ritual/. */
public class SNRecipeProvider extends RecipeProvider {

    public SNRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.CHALK.get(), 4)
                .requires(Items.CALCITE).requires(Items.BONE_MEAL)
                .unlockedBy("has_calcite", has(Items.CALCITE)).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.CHALK.get(), 2)
                .requires(Items.BONE_MEAL).requires(Items.BONE_MEAL).requires(Items.CLAY_BALL)
                .unlockedBy("has_bone_meal", has(Items.BONE_MEAL))
                .save(out, SupernaturalCraft.asResource("chalk_from_clay"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.BLOOD_CHALK.get(), 4)
                .requires(AllItems.CHALK.get(), 4).requires(AllItems.DEMON_BLOOD.get())
                .unlockedBy("has_demon_blood", has(AllItems.DEMON_BLOOD.get())).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.ENOCHIAN_INK.get(), 2)
                .requires(Items.INK_SAC).requires(AllItems.SULFUR.get()).requires(AllItems.SALT.get()).requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_sulfur", has(AllItems.SULFUR.get())).save(out);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, AllItems.GRIMOIRE.get())
                .requires(Items.BOOK).requires(AllItems.ENOCHIAN_INK.get()).requires(AllItems.CHALK.get()).requires(Items.LEATHER)
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, AllItems.HUNTERS_AMULET.get())
                .pattern("s s").pattern(" g ").pattern(" b ")
                .define('s', Items.STRING).define('g', Items.GOLD_INGOT).define('b', AllItems.DEMON_BLOOD.get())
                .unlockedBy("has_demon_blood", has(AllItems.DEMON_BLOOD.get())).save(out);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, AllItems.RITUAL_ALTAR.get())
                .pattern(" c ").pattern("bgb").pattern("bbb")
                .define('c', ItemTags.CANDLES).define('g', Items.GOLD_INGOT).define('b', Items.POLISHED_BLACKSTONE_BRICKS)
                .unlockedBy("has_chalk", has(AllItems.CHALK.get())).save(out);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.RUNE_BLANK.get(), 2)
                .requires(Items.SMOOTH_STONE).requires(AllItems.CHALK.get()).requires(AllItems.ENOCHIAN_INK.get())
                .unlockedBy("has_ink", has(AllItems.ENOCHIAN_INK.get())).save(out);

        Ingredient saltOres = Ingredient.of(AllItems.ROCK_SALT_ORE.get(), AllItems.DEEPSLATE_ROCK_SALT_ORE.get());
        SimpleCookingRecipeBuilder.smelting(saltOres, RecipeCategory.MISC, AllItems.SALT.get(), 0.2f, 200)
                .unlockedBy("has_ore", has(AllItems.ROCK_SALT_ORE.get())).save(out, SupernaturalCraft.asResource("salt_from_smelting"));
        SimpleCookingRecipeBuilder.blasting(saltOres, RecipeCategory.MISC, AllItems.SALT.get(), 0.2f, 100)
                .unlockedBy("has_ore", has(AllItems.ROCK_SALT_ORE.get())).save(out, SupernaturalCraft.asResource("salt_from_blasting"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(AllItems.NETHER_SULFUR_ORE.get()), RecipeCategory.MISC, AllItems.SULFUR.get(), 0.3f, 200)
                .unlockedBy("has_ore", has(AllItems.NETHER_SULFUR_ORE.get())).save(out, SupernaturalCraft.asResource("sulfur_from_smelting"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.WATER_BUCKET), RecipeCategory.MISC, AllItems.SALT.get(), 0.1f, 400)
                .unlockedBy("has_water_bucket", has(Items.WATER_BUCKET))
                .save(out, SupernaturalCraft.asResource("salt_from_evaporation"));
    }
}
