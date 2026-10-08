package org.papiricoh.supernaturalcraft.compat.jei;

import com.mojang.serialization.JsonOps;
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
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;
import org.papiricoh.supernaturalcraft.ritual.PatternGeometry;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualPattern;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * One page per ritual: the floor pattern seen from above on the left (hover a cell to see which
 * block goes there), offerings and the activator in the middle, the outcome on the right.
 */
public class RitualCategory implements IRecipeCategory<RecipeHolder<RitualRecipe>> {

    private static final int W = 176, H = 124, GRID = 81, GX = 2, GY = 4;
    private final IDrawable icon;

    public RitualCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(AllItems.RITUAL_ALTAR.get()));
    }

    @Override
    public RecipeType<RecipeHolder<RitualRecipe>> getRecipeType() {
        return SNJeiPlugin.RITUAL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.supernaturalcraft.ritual");
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

    private static @Nullable RitualPattern pattern(RitualRecipe recipe) {
        var level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess().registryOrThrow(SNRegistries.RITUAL_PATTERN).get(recipe.pattern());
    }

    private static int cellSize(RitualPattern pattern) {
        return Math.max(4, Math.min(16, GRID / Math.max(1, pattern.size())));
    }

    private static ItemStack icon(BlockPredicate predicate) {
        return predicate.blocks().flatMap(set -> set.stream().findFirst()).map(Holder::value).map(Block::asItem)
                .map(ItemStack::new).orElse(new ItemStack(Items.BARRIER));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<RitualRecipe> holder, IFocusGroup focuses) {
        RitualRecipe recipe = holder.value();
        List<Ingredient> ins = recipe.ingredients();
        for (int i = 0; i < ins.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 90 + (i % 2) * 18, 4 + (i / 2) * 18)
                    .setStandardSlotBackground().addIngredients(ins.get(i));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 90, 84).setStandardSlotBackground().addIngredients(recipe.activator())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(recipe.consumeActivator()
                        ? "jei.supernaturalcraft.ritual.consumed" : "jei.supernaturalcraft.ritual.activator").withStyle(ChatFormatting.GRAY)));
        ItemStack result = recipe.effect().displayResult();
        if (!result.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 40).setOutputSlotBackground().addItemStack(result);
        }
        // Every distinct block in the pattern is also an input, so "uses" lookups find the ritual.
        RitualPattern pattern = pattern(recipe);
        if (pattern != null) {
            for (BlockPredicate p : pattern.key().values()) {
                List<ItemStack> stacks = new ArrayList<>();
                p.blocks().ifPresent(set -> set.forEach(h -> stacks.add(new ItemStack(h.value().asItem()))));
                if (!stacks.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStacks(stacks);
            }
        }
    }

    @Override
    public void draw(RecipeHolder<RitualRecipe> holder, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        RitualRecipe recipe = holder.value();
        Font font = Minecraft.getInstance().font;
        RitualPattern pattern = pattern(recipe);
        g.fill(GX - 1, GY - 1, GX + GRID + 1, GY + GRID + 1, 0xFF2A2420);
        if (pattern != null) {
            int cell = cellSize(pattern);
            int ox = GX + (GRID - cell * pattern.size()) / 2, oy = GY + (GRID - cell * pattern.size()) / 2;
            for (int z = 0; z < pattern.pattern().size(); z++) {
                String row = pattern.pattern().get(z);
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    if (c == PatternGeometry.ANY) continue;
                    ItemStack stack = c == PatternGeometry.ALTAR ? new ItemStack(AllItems.RITUAL_ALTAR.get()) : icon(pattern.key().get(c));
                    g.pose().pushPose();
                    g.pose().translate(ox + x * cell, oy + z * cell, 0);
                    g.pose().scale(cell / 16f, cell / 16f, 1);
                    g.renderItem(stack, 0, 0);
                    g.pose().popPose();
                }
            }
        }
        int y = 92;
        RitualConditions cond = recipe.conditions();
        if (cond.time() != RitualConditions.Time.ANY) {
            g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual." + cond.time().getSerializedName()), 112, y, 0xFF5A3C8C, false);
            y += 10;
        }
        if (cond.weather() != RitualConditions.Weather.ANY) {
            g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual.weather." + cond.weather().getSerializedName()), 112, y, 0xFF5A3C8C, false);
            y += 10;
        }
        if (cond.eclipse()) {
            g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual.eclipse"), 112, y, 0xFF5A3C8C, false);
            y += 10;
        }
        if (cond.dimension().isPresent()) {
            var dim = cond.dimension().get().location();
            g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual.dimension",
                    Component.translatableWithFallback("jei.supernaturalcraft.dimension." + dim.getNamespace() + "." + dim.getPath(), dim.getPath())),
                    112, y, 0xFF8A2A1A, false);
            y += 10;
        }
        if (cond.requirementNames().isPresent()) {
            g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual.requires", cond.requirementNames().get()), 112, y, 0xFF6A5A2A, false);
        }
        if (recipe.manaCost() > 0) {
            g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual.mana", Math.round(recipe.manaCost())), 112, 84, 0xFF6327B3, false);
        }
        g.drawString(font, Component.translatable("jei.supernaturalcraft.ritual.duration", Math.round(recipe.duration() / 20f)), 150, 62, 0xFF808080, false);
        if (recipe.effect().displayResult().isEmpty()) {
            Component text = Component.translatable("jei.supernaturalcraft.effect." + recipe.effect().type().toLanguageKey());
            g.drawWordWrap(font, text, 128, 8, 46, 0xFF3B2A1A);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<RitualRecipe> holder, IRecipeSlotsView slots, double mx, double my) {
        tooltip.addAll(cellTooltip(holder, mx, my));
    }

    private static List<Component> cellTooltip(RecipeHolder<RitualRecipe> holder, double mx, double my) {
        RitualPattern pattern = pattern(holder.value());
        if (pattern == null) return List.of();
        int cell = cellSize(pattern);
        int ox = GX + (GRID - cell * pattern.size()) / 2, oy = GY + (GRID - cell * pattern.size()) / 2;
        int x = (int) Math.floor((mx - ox) / cell), z = (int) Math.floor((my - oy) / cell);
        if (z < 0 || z >= pattern.pattern().size() || x < 0 || x >= pattern.pattern().get(z).length()) return List.of();
        char c = pattern.pattern().get(z).charAt(x);
        if (c == PatternGeometry.ANY) return List.of();
        if (c == PatternGeometry.ALTAR) return List.of(new ItemStack(AllItems.RITUAL_ALTAR.get()).getHoverName());
        BlockPredicate p = pattern.key().get(c);
        List<Component> lines = new ArrayList<>();
        p.blocks().ifPresent(set -> set.stream().limit(6).forEach(h -> lines.add(h.value().getName())));
        p.properties().ifPresent(props -> StatePropertiesPredicate.CODEC.encodeStart(JsonOps.INSTANCE, props).result()
                .ifPresent(json -> lines.add(Component.literal(json.toString()).withStyle(ChatFormatting.GRAY))));
        return lines;
    }
}
