package org.papiricoh.supernaturalcraft.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.BowlSpells;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlItem;
import org.papiricoh.supernaturalcraft.client.ClientArcana;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hunter's Journal, bound into the back of every grimoire: the whole progression, from the
 * first pinch of salt to the Cage, with the counters to every trick Lucifer has. After the fixed
 * pages come the bowl spells the reader has learned, one page per recipe, written from the
 * recipes themselves: liquids, ingredients, mana and the words.
 */
public class JournalScreen extends Screen {

    /** Fixed pages, from lang ({@code journal.supernaturalcraft.page<N>.title/.body}). */
    public static final int PAGES = 27;
    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/journal.png");
    private static final int W = 256, H = 196, INK = 0x3B2A1A, FADED = 0x7A6448;

    /** A page written for one learned spell recipe. */
    private record SpellPage(ResourceLocation spell, BowlSpellRecipe recipe, int variant, int variants) {
    }

    private final Screen parent;
    private final List<SpellPage> spellPages = new ArrayList<>();
    private int page;
    private int left, top;

    public JournalScreen(Screen parent) {
        super(Component.translatable("screen.supernaturalcraft.journal"));
        this.parent = parent;
    }

    private int pageCount() {
        return PAGES + spellPages.size();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        collectSpellPages();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> page = Math.max(0, page - 1))
                .bounds(left + 10, top + 170, 30, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> page = Math.min(pageCount() - 1, page + 1))
                .bounds(left + W - 40, top + 170, 30, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> onClose())
                .bounds(left + W / 2 - 30, top + 170, 60, 18).build());
    }

    private void collectSpellPages() {
        spellPages.clear();
        if (minecraft == null || minecraft.level == null) return;
        var recipes = minecraft.level.getRecipeManager();
        for (ResourceLocation spell : BowlSpells.allSpells(recipes)) {
            if (!ClientArcana.rites().contains(spell)) continue;
            List<RecipeHolder<BowlSpellRecipe>> forSpell = BowlSpells.recipesFor(recipes, spell);
            for (int i = 0; i < forSpell.size(); i++) spellPages.add(new SpellPage(spell, forSpell.get(i).value(), i + 1, forSpell.size()));
        }
        page = Math.min(page, pageCount() - 1);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        g.blit(BG, left, top, 0, 0, W, H, 256, 256);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        Component title;
        List<Component> body;
        if (page < PAGES) {
            String key = "journal.supernaturalcraft.page" + (page + 1);
            title = Component.translatable(key + ".title");
            body = List.of(Component.translatable(key + ".body"));
        } else {
            SpellPage sp = spellPages.get(page - PAGES);
            Component name = Component.translatable(BowlSpells.nameKey(sp.spell()));
            title = sp.variants() > 1 ? Component.translatable("journal.supernaturalcraft.spell.variant", name, sp.variant(), sp.variants()) : name;
            body = spellBody(sp);
        }
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 10, INK, false);
        int y = top + 26;
        outer:
        for (Component paragraph : body) {
            for (FormattedCharSequence line : font.split(paragraph, W - 28)) {
                if (y > top + 160) break outer;
                g.drawString(font, line, left + 14, y, INK, false);
                y += 10;
            }
            y += 3;
        }
        Component n = Component.literal((page + 1) + " / " + pageCount());
        g.drawString(font, n, left + (W - font.width(n)) / 2, top + 160, FADED, false);
    }

    /** A learned spell's page: what it does, what goes in the bowl, the mana, and the words. */
    private static List<Component> spellBody(SpellPage sp) {
        BowlSpellRecipe r = sp.recipe();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(BowlSpells.nameKey(sp.spell()) + ".desc").withStyle(ChatFormatting.DARK_GRAY));
        MutableComponent liquids = Component.empty();
        for (int i = 0; i < r.liquids().size(); i++) {
            BowlLiquid l = r.liquids().get(i);
            if (i > 0) liquids.append(", ");
            liquids.append(Component.translatable(SpellBowlItem.liquidKey(l)));
        }
        lines.add(Component.translatable("journal.supernaturalcraft.spell.liquids",
                r.liquids().isEmpty() ? Component.translatable("journal.supernaturalcraft.spell.none") : liquids));
        MutableComponent items = Component.empty();
        for (int i = 0; i < r.ingredients().size(); i++) {
            if (i > 0) items.append(", ");
            items.append(name(r.ingredients().get(i)));
        }
        lines.add(Component.translatable("journal.supernaturalcraft.spell.ingredients",
                r.ingredients().isEmpty() ? Component.translatable("journal.supernaturalcraft.spell.none") : items));
        if (r.manaCost() > 0) lines.add(Component.translatable("journal.supernaturalcraft.spell.mana", Math.round(r.manaCost())));
        lines.add(Component.translatable("journal.supernaturalcraft.spell.words",
                Component.literal(r.incantation()).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_RED)));
        return lines;
    }

    private static Component name(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? Component.literal("?") : options[0].getHoverName();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
