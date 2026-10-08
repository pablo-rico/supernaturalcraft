package org.papiricoh.supernaturalcraft.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquids;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlItem;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.client.ClientArcana;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;

import java.util.ArrayList;
import java.util.List;

/**
 * One page per bowl spell recipe: the bottles to pour (top row) and the ingredients (below) on the
 * left, the spell's name and its outcome on the right, then mana, conditions and the incantation,
 * which stays hidden ("?????") until the spell's page has been read.
 */
public class BowlSpellCategory implements IRecipeCategory<RecipeHolder<BowlSpellRecipe>> {

    private static final int W = 176, H = 118, WORDS_Y = 88;
    private static final int INK = 0xFF3B2A1A, GOLD = 0xFF8A6A10, PURPLE = 0xFF6327B3, RED = 0xFF7A1A1A, FADED = 0xFF808080;
    private final IDrawable icon;

    public BowlSpellCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(AllItems.SPELL_BOWL.get()));
    }

    @Override
    public RecipeType<RecipeHolder<BowlSpellRecipe>> getRecipeType() {
        return SNJeiPlugin.BOWL_SPELL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.supernaturalcraft.bowl_spell");
    }

    @Override
    public int getWidth() {
        return W;
    }

    @Override
    public int getHeight() {
        return H;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /** What stands for a liquid in its slot: the bottle it is poured from. */
    public static ItemStack bottle(BowlLiquid liquid) {
        return BowlLiquids.bottle(Dose.of(liquid));
    }

    private static boolean known(ResourceLocation spell) {
        return ClientArcana.rites().contains(spell);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BowlSpellRecipe> holder, IFocusGroup focuses) {
        BowlSpellRecipe recipe = holder.value();
        List<BowlLiquid> liquids = recipe.liquids();
        for (int i = 0; i < liquids.size(); i++) {
            BowlLiquid liquid = liquids.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, 2 + i * 18, 2).setStandardSlotBackground().addItemStack(bottle(liquid))
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable(SpellBowlItem.liquidKey(liquid)).withStyle(ChatFormatting.AQUA));
                        if (liquid == BowlLiquid.POTION) tooltip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.any_potion").withStyle(ChatFormatting.GRAY));
                        if (liquid == BowlLiquid.BLOOD) tooltip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.any_blood").withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.liquid").withStyle(ChatFormatting.DARK_GRAY));
                    });
        }
        List<Ingredient> ins = recipe.ingredients();
        for (int i = 0; i < ins.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 2 + (i % 4) * 18, 24 + (i / 4) * 18).setStandardSlotBackground().addIngredients(ins.get(i));
        }
        ItemStack result = recipe.effect().displayResult();
        if (!result.isEmpty()) builder.addSlot(RecipeIngredientRole.OUTPUT, 154, 30).setOutputSlotBackground().addItemStack(result);
        // The page that teaches the spell: looking up its uses finds the recipes.
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(SpellPageItem.of(recipe.spell(holder.id())));
    }

    @Override
    public void draw(RecipeHolder<BowlSpellRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        BowlSpellRecipe recipe = holder.value();
        ResourceLocation spell = recipe.spell(holder.id());
        Font font = Minecraft.getInstance().font;
        g.drawWordWrap(font, Component.translatable(BowlSpells.nameKey(spell)), 78, 4, 96, GOLD);
        if (recipe.effect().displayResult().isEmpty()) {
            g.drawWordWrap(font, Component.translatable("jei.supernaturalcraft.effect." + recipe.effect().type().toLanguageKey()), 78, 26, 96, INK);
        }
        if (recipe.manaCost() > 0) {
            g.drawString(font, Component.translatable("jei.supernaturalcraft.bowl_spell.mana", Math.round(recipe.manaCost())), 2, 64, PURPLE, false);
        }
        int y = 64;
        for (Component line : conditions(recipe.conditions())) {
            g.drawString(font, line, 78, y, RED, false);
            y += 10;
        }
        Component words = known(spell)
                ? Component.literal("“" + recipe.incantation() + "”").withStyle(ChatFormatting.ITALIC)
                : Component.translatable("jei.supernaturalcraft.bowl_spell.unknown_words");
        g.drawWordWrap(font, words, 2, WORDS_Y, W - 4, known(spell) ? RED : FADED);
    }

    private static List<Component> conditions(RitualConditions cond) {
        List<Component> lines = new ArrayList<>();
        if (cond.time() != RitualConditions.Time.ANY) lines.add(Component.translatable("jei.supernaturalcraft.ritual." + cond.time().getSerializedName()));
        if (cond.weather() != RitualConditions.Weather.ANY) lines.add(Component.translatable("jei.supernaturalcraft.ritual.weather." + cond.weather().getSerializedName()));
        if (cond.eclipse()) lines.add(Component.translatable("jei.supernaturalcraft.ritual.eclipse"));
        cond.dimension().ifPresent(d -> lines.add(Component.translatable("jei.supernaturalcraft.ritual.dimension",
                Component.translatableWithFallback("jei.supernaturalcraft.dimension." + d.location().getNamespace() + "." + d.location().getPath(),
                        d.location().getPath()))));
        cond.requirementNames().ifPresent(names -> lines.add(Component.translatable("jei.supernaturalcraft.ritual.requires", names)));
        return lines;
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<BowlSpellRecipe> holder, IRecipeSlotsView slots, double mx, double my) {
        ResourceLocation spell = holder.value().spell(holder.id());
        if (my >= WORDS_Y && my < WORDS_Y + 30) {
            if (!known(spell)) tooltip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.words_hidden").withStyle(ChatFormatting.GRAY));
        } else if (mx >= 78 && my < 24) {
            tooltip.add(Component.translatable(BowlSpells.nameKey(spell) + ".desc").withStyle(ChatFormatting.GRAY));
        }
    }
}
