package org.papiricoh.supernaturalcraft.datagen.heaven;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * v0.18's crafting recipes (the crossroads box; owned by the crossroads work), called from {@code SNRecipeProvider}. Ritual
 * recipes stay hand-written JSON under {@code data/supernaturalcraft/recipe/ritual/}.
 */
public final class HeavenRecipes {

    private HeavenRecipes() {
    }

    public static void build(RecipeOutput out) {
        // The crossroads box, the old way: a bone, graveyard dirt, a photo, sulphur and a scrap of iron.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, AllItems.CROSSROADS_BOX.get())
                .requires(Items.BONE).requires(AllItems.GRAVE_SOIL.get()).requires(Items.PAPER).requires(AllItems.SULFUR.get())
                .requires(Items.IRON_NUGGET)
                .unlockedBy("has_grave_soil", InventoryChangeTrigger.TriggerInstance.hasItems(AllItems.GRAVE_SOIL.get()))
                .save(out);
    }
}
