package org.papiricoh.supernaturalcraft.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SigilKind;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/** One entry per sigil: its page, glyph, cost and reagents. */
public class SigilCategory implements IRecipeCategory<SigilCategory.Entry> {

    public record Entry(ResourceLocation id, SigilComponent sigil) {
    }

    private static final int W = 150, H = 54;
    private final IDrawable icon;

    public SigilCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(AllItems.GRIMOIRE.get()));
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return SNJeiPlugin.SIGIL;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.supernaturalcraft.sigils");
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 2).setStandardSlotBackground()
                .addItemStack(SigilPageItem.of(AllItems.SIGIL_PAGE.get(), entry.id()));
        int x = 2;
        for (SigilComponent.Reagent r : entry.sigil().reagents()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, x, 34).setStandardSlotBackground().addItemStack(r.stack());
            x += 20;
        }
    }

    @Override
    public void draw(Entry entry, IRecipeSlotsView slots, GuiGraphics g, double mx, double my) {
        Font font = Minecraft.getInstance().font;
        SigilComponent s = entry.sigil();
        g.blit(SigilComponent.glyphTexture(entry.id()), 24, 3, 0, 0, 16, 16, 16, 16);
        g.drawString(font, Component.translatable(SigilComponent.translationKey(entry.id())), 44, 3, 0xFF3B2A1A, false);
        g.drawString(font, Component.translatable("screen.supernaturalcraft.composer.kind." + s.kind().getSerializedName())
                .append(" · T" + s.tier()), 44, 13, 0xFF808080, false);
        Component cost = s.kind() == SigilKind.MODIFIER
                ? Component.translatable("screen.supernaturalcraft.composer.multiplier", String.format("%.1f", s.param("mana_multiplier", 1f)))
                : Component.translatable("screen.supernaturalcraft.composer.mana", Math.round(s.manaCost()));
        g.drawString(font, cost, 44, 23, 0xFF6327B3, false);
        g.drawWordWrap(font, Component.translatable(SigilComponent.translationKey(entry.id()) + ".desc"), 64, 36, 84, 0xFF555555);
    }
}
