package org.papiricoh.supernaturalcraft.client.book.journal;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.joml.Quaternionf;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlItem;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.journal.JournalBlock;
import org.papiricoh.supernaturalcraft.journal.JournalLayout;
import org.papiricoh.supernaturalcraft.ritual.RitualConditions;
import org.papiricoh.supernaturalcraft.ritual.RitualRecipe;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * An open entry's blocks, measured for {@link JournalLayout} and drawn on the page. Built once per
 * opened entry: wrapped text, captions, looked-up recipes and the creatures (created once and kept
 * while the entry is open) live here.
 */
final class PageBlocks {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Width of a page's text column: a little narrower than the page, for margins and the ribbon. */
    static final int W = 170;
    /** A line of body text, and the half line a blank one takes (between paragraphs). */
    static final int LINE = 10, BLANK = 5;
    private static final int CAPTION_LINE = 9, SLOT = 20;
    private static final int SLOT_SPACING = 20;
    /** Extra room under a ritual or a bowl spell, so two in a row read as two. */
    private static final int PAD = 6;

    /** Where tooltips go (the book shows them after the frame). */
    private final Consumer<List<Component>> tooltip;
    private final Minecraft mc = Minecraft.getInstance();
    private final Font font = mc.font;
    final List<Part> parts = new ArrayList<>();

    PageBlocks(List<JournalBlock> blocks, Consumer<List<Component>> tooltip) {
        this.tooltip = tooltip;
        for (JournalBlock b : blocks) parts.add(part(b));
    }

    List<JournalLayout.Measured> measured() {
        return parts.stream().map(Part::measured).toList();
    }

    void tick() {
        for (Part p : parts) if (p instanceof EntityPart e && e.entity != null) e.entity.tickCount++;
    }

    private Part part(JournalBlock b) {
        return switch (b) {
            case JournalBlock.Text t -> new TextPart(t);
            case JournalBlock.Entity e -> new EntityPart(e);
            case JournalBlock.Items i -> new ItemsPart(i);
            case JournalBlock.Recipe r -> recipePart(r);
            case JournalBlock.Image i -> new ImagePart(i);
        };
    }

    // --- parts -------------------------------------------------------------------------------------

    abstract class Part {
        /** The caption's wrapped lines (italic, faded, centred), or none. */
        final List<FormattedCharSequence> caption;

        Part(Optional<String> captionKey) {
            caption = captionKey.map(k -> font.split(Component.translatable(k).withStyle(ChatFormatting.ITALIC), W - 16))
                    .orElse(List.of());
        }

        int captionHeight() {
            return caption.isEmpty() ? 0 : 3 + caption.size() * CAPTION_LINE;
        }

        void drawCaption(GuiGraphics g, int x, int y) {
            if (caption.isEmpty()) return;
            y += 3;
            for (FormattedCharSequence line : caption) {
                g.drawString(font, line, x + (W - font.width(line)) / 2, y, BookStyle.FADED, false);
                y += CAPTION_LINE;
            }
        }

        abstract JournalLayout.Measured measured();

        /** Draws lines {@code first..last} (a paragraph) or the whole block, its top at y. */
        abstract void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my);
    }

    final class TextPart extends Part {
        final List<FormattedCharSequence> lines;

        TextPart(JournalBlock.Text t) {
            super(Optional.empty());
            lines = font.split(ArchivePages.parse(t.text()), W);
        }

        int height(int i) {
            return font.width(lines.get(i)) == 0 ? BLANK : LINE;
        }

        @Override
        JournalLayout.Measured measured() {
            List<Integer> h = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) h.add(height(i));
            return JournalLayout.Measured.text(h);
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            for (int i = first; i <= last && i < lines.size(); i++) {
                g.drawString(font, lines.get(i), x, y, BookStyle.INK, false);
                y += height(i);
            }
        }
    }

    /** A creature on the page, turning slowly. */
    final class EntityPart extends Part {
        final int box;
        /** Below 1 the creature shrinks inside the usual box (models bigger than their hitbox); above 1 the box grows. */
        final float modelScale;
        final Entity entity;

        EntityPart(JournalBlock.Entity b) {
            super(b.caption());
            box = Math.max(32, Math.min(200, Math.round(90 * Math.max(1f, b.scale()))));
            modelScale = Math.min(1f, b.scale());
            entity = create(b.entity());
        }

        private Entity create(ResourceLocation id) {
            if (mc.level == null) return null;
            try {
                Entity e = BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> (Entity) t.create(mc.level)).orElse(null);
                // Hounds and ghosts hide from the naked eye; on the page they stay shown.
                if (e instanceof org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity hound) hound.reveal(Integer.MAX_VALUE);
                if (e instanceof org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity ghost) {
                    ghost.clientAlpha = ghost.clientAlphaO = org.papiricoh.supernaturalcraft.entity.ghost.GhostBalance.SEEN_ALPHA;
                }
                return e;
            } catch (RuntimeException e) {
                LOGGER.warn("The journal could not draw {}", id, e);
                return null;
            }
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(box + captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            int feet = y + box - 6;
            // A soft shadow on the parchment for it to stand on.
            g.fill(x + W / 2 - 26, feet, x + W / 2 + 26, feet + 1, 0x1E2A1A0A);
            g.fill(x + W / 2 - 18, feet + 1, x + W / 2 + 18, feet + 2, 0x162A1A0A);
            if (entity != null) {
                if (!drawEntity(g, entity, x + W / 2f, feet, (box - 12) * modelScale, (W - 30) * modelScale)) broken.add(entity);
            }
            drawCaption(g, x, y + box);
        }
    }

    /** Creatures whose renderer threw once: not drawn again. */
    private final java.util.Set<Entity> broken = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * Draws a creature standing at (cx, feetY), fitted into maxH × maxW book pixels by its
     * bounding box, turning slowly. Like {@code InventoryScreen.renderEntityInInventory}, but for
     * any entity and without its screen-space scissor.
     *
     * @return false if its renderer failed
     */
    boolean drawEntity(GuiGraphics g, Entity e, float cx, float feetY, float maxH, float maxW) {
        if (broken.contains(e)) return true;
        float h = Math.max(0.3f, e.getBbHeight()), w = Math.max(0.3f, e.getBbWidth());
        float s = Math.min(maxH / h, maxW / (w * 1.4f));
        float yaw = 200 + (Util.getMillis() % 36000L) / 1000f * 18f;
        if (e instanceof LivingEntity l) {
            l.yBodyRot = l.yBodyRotO = yaw;
            l.yHeadRot = l.yHeadRotO = yaw;
        }
        e.setYRot(yaw);
        e.yRotO = yaw;
        e.setXRot(0);
        e.xRotO = 0;
        Quaternionf tilt = new Quaternionf().rotateX(-8 * (float) (Math.PI / 180));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
        g.flush();
        var stack = g.pose();
        stack.pushPose();
        var dispatcher = mc.getEntityRenderDispatcher();
        boolean ok = true;
        try {
            stack.translate(cx, feetY, 120);
            stack.scale(s, s, -s);
            stack.mulPose(pose);
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(tilt.conjugate(new Quaternionf()).rotateY((float) Math.PI));
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(e, 0, 0, 0, 0, 1, stack, g.bufferSource(), 0xF000F0));
            g.flush();
        } catch (RuntimeException ex) {
            LOGGER.warn("The journal could not draw {}", e.getType(), ex);
            ok = false;
        } finally {
            dispatcher.setRenderShadow(true);
            stack.popPose();
            Lighting.setupFor3DItems();
        }
        return ok;
    }

    final class ItemsPart extends Part {
        final List<ItemStack> items;

        ItemsPart(JournalBlock.Items b) {
            super(b.caption());
            items = b.items().stream().map(Ink::item).toList();
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(SLOT + captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            int x0 = x + (W - items.size() * SLOT_SPACING) / 2;
            for (int i = 0; i < items.size(); i++) slot(g, items.get(i), x0 + i * SLOT_SPACING, y, mx, my, List.of());
            drawCaption(g, x, y + SLOT);
        }
    }

    final class ImagePart extends Part {
        final JournalBlock.Image image;
        final int w, h;

        ImagePart(JournalBlock.Image b) {
            super(b.caption());
            image = b;
            float s = Math.min(1f, Math.min(W / (float) b.width(), 200f / b.height()));
            w = Math.max(1, Math.round(b.width() * s));
            h = Math.max(1, Math.round(b.height() * s));
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(h + captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            g.blit(image.texture(), x + (W - w) / 2, y, w, h, 0, 0, image.width(), image.height(), image.width(), image.height());
            drawCaption(g, x, y + h);
        }
    }

    // --- recipes -----------------------------------------------------------------------------------

    private Part recipePart(JournalBlock.Recipe b) {
        Optional<RecipeHolder<?>> holder = mc.level == null ? Optional.empty() : mc.level.getRecipeManager().byKey(b.recipe());
        if (holder.isEmpty()) return new CaptionOnly(b.caption());
        Recipe<?> recipe = holder.get().value();
        if (recipe instanceof RitualRecipe r) return new RitualPart(r, b.caption());
        if (recipe instanceof BowlSpellRecipe r) return new BowlPart(r, b.caption());
        if (recipe.getIngredients().isEmpty()) return new CaptionOnly(b.caption());
        return new CraftingPart(recipe, b.caption());
    }

    /** A recipe the game does not know (a missing datapack, say): just its caption. */
    final class CaptionOnly extends Part {
        CaptionOnly(Optional<String> caption) {
            super(caption);
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            drawCaption(g, x, y - 3);
        }
    }

    /** A crafting grid (shaped, or the ingredients three to a row), an arrow and what it makes. */
    final class CraftingPart extends Part {
        final List<Ingredient> grid;
        final int cols, rows;
        final ItemStack result;

        CraftingPart(Recipe<?> recipe, Optional<String> caption) {
            super(caption);
            List<Ingredient> ins = recipe.getIngredients();
            if (recipe instanceof ShapedRecipe shaped) {
                cols = shaped.getWidth();
                rows = shaped.getHeight();
            } else {
                cols = Math.min(3, ins.size());
                rows = Math.min(3, (ins.size() + 2) / 3);
            }
            grid = ins.subList(0, Math.min(ins.size(), cols * rows));
            result = mc.level == null ? ItemStack.EMPTY : recipe.getResultItem(mc.level.registryAccess());
        }

        int height() {
            return Math.max(rows * SLOT, 24);
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(height() + captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            int total = cols * SLOT + 8 + 18 + 8 + 24;
            int x0 = x + (W - total) / 2, gy = y + (height() - rows * SLOT) / 2;
            for (int i = 0; i < grid.size(); i++) {
                slot(g, cycle(grid.get(i)), x0 + (i % cols) * SLOT, gy + (i / cols) * SLOT, mx, my, List.of());
            }
            int ax = x0 + cols * SLOT + 8;
            BookAtlas.ARROW_NEXT.draw(g, ax, y + (height() - 12) / 2);
            resultSlot(g, result, ax + 26, y + (height() - 24) / 2, mx, my);
            drawCaption(g, x, y + height());
        }
    }

    /** A ritual: the offerings in a ring around the activator, what it makes, the pattern and its cost. */
    final class RitualPart extends Part {
        static final int RING = 80, R = 30;
        final RitualRecipe recipe;
        final List<FormattedCharSequence> effectText, notes;

        RitualPart(RitualRecipe recipe, Optional<String> caption) {
            super(caption);
            this.recipe = recipe;
            effectText = recipe.effect().displayResult().isEmpty()
                    ? font.split(Component.translatable("jei.supernaturalcraft.effect." + recipe.effect().type().toLanguageKey()), 72)
                    : List.of();
            List<FormattedCharSequence> n = new ArrayList<>();
            for (Component c : conditions(recipe.conditions())) n.addAll(font.split(c, W - 10));
            notes = n;
        }

        int body() {
            return Math.max(RING, effectText.size() * 9);
        }

        int height() {
            return body() + 2 + LINE + notes.size() * 9 + PAD;
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(height() + captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            boolean makes = effectText.isEmpty();
            int total = RING + 6 + (makes ? 18 + 6 + 24 : 74);
            int x0 = x + (W - total) / 2;
            int cx = x0 + RING / 2, cy = y + body() / 2;
            // A faint chalk circle under the offerings.
            ring(g, cx, cy, R + 1, 0x403B2A1A);
            List<Ingredient> ins = recipe.ingredients();
            for (int i = 0; i < ins.size(); i++) {
                double a = -Math.PI / 2 + i * 2 * Math.PI / ins.size();
                int sx = (int) Math.round(cx + Math.cos(a) * R) - SLOT / 2, sy = (int) Math.round(cy + Math.sin(a) * R) - SLOT / 2;
                slot(g, cycle(ins.get(i)), sx, sy, mx, my, List.of());
            }
            slot(g, cycle(recipe.activator()), cx - SLOT / 2, cy - SLOT / 2, mx, my, List.of(Component.translatable(recipe.consumeActivator()
                    ? "jei.supernaturalcraft.ritual.consumed" : "jei.supernaturalcraft.ritual.activator").withStyle(ChatFormatting.GRAY)));
            int rx = x0 + RING + 6;
            if (makes) {
                BookAtlas.ARROW_NEXT.draw(g, rx, cy - 6);
                resultSlot(g, recipe.effect().displayResult(), rx + 24, cy - 12, mx, my);
            } else {
                int ty = cy - effectText.size() * 9 / 2;
                for (FormattedCharSequence line : effectText) {
                    g.drawString(font, line, rx, ty, BookStyle.INK, false);
                    ty += 9;
                }
            }
            int ly = y + body() + 2;
            Component pattern = patternName(recipe.pattern().location());
            Component line = recipe.manaCost() > 0
                    ? Component.translatable("screen.supernaturalcraft.book.journal.ritual_line", pattern,
                    Component.translatable("screen.supernaturalcraft.book.journal.mana", Math.round(recipe.manaCost()))
                            .withStyle(s -> s.withColor(BookStyle.MANA & 0xFFFFFF)))
                    : pattern;
            Ink.centred(g, font, line, x + W / 2, ly, W, BookStyle.INK);
            ly += LINE;
            for (FormattedCharSequence n : notes) {
                g.drawString(font, n, x + (W - font.width(n)) / 2, ly, BookStyle.BLOOD, false);
                ly += 9;
            }
            drawCaption(g, x, y + height() - PAD);
        }
    }

    /** A bowl spell: the liquids to pour, the ingredients, the mana, and the words in dark red. */
    final class BowlPart extends Part {
        final BowlSpellRecipe recipe;
        final List<List<BowlLiquid>> liquidRows = new ArrayList<>();
        final List<FormattedCharSequence> words, notes;

        BowlPart(BowlSpellRecipe recipe, Optional<String> caption) {
            super(caption);
            this.recipe = recipe;
            // Liquids flow in rows across the page: a drop and a name each.
            List<BowlLiquid> row = new ArrayList<>();
            int rowW = 0;
            for (BowlLiquid l : recipe.liquids()) {
                int w = liquidWidth(l);
                if (!row.isEmpty() && rowW + w > W) {
                    liquidRows.add(row);
                    row = new ArrayList<>();
                    rowW = 0;
                }
                row.add(l);
                rowW += w;
            }
            if (!row.isEmpty()) liquidRows.add(row);
            words = font.split(Component.literal("“" + recipe.incantation() + "”").withStyle(ChatFormatting.ITALIC), W - 10);
            List<FormattedCharSequence> n = new ArrayList<>();
            for (Component c : conditions(recipe.conditions())) n.addAll(font.split(c, W - 10));
            notes = n;
        }

        int liquidWidth(BowlLiquid l) {
            return 10 + font.width(Component.translatable(SpellBowlItem.liquidKey(l))) + 8;
        }

        int ingredientsHeight() {
            return recipe.ingredients().isEmpty() ? 0 : SLOT + 3;
        }

        int height() {
            return liquidRows.size() * 11 + (liquidRows.isEmpty() ? 0 : 3) + ingredientsHeight()
                    + (recipe.manaCost() > 0 ? LINE : 0) + notes.size() * 9 + 3 + words.size() * 9 + PAD;
        }

        @Override
        JournalLayout.Measured measured() {
            return JournalLayout.Measured.fixed(height() + captionHeight());
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int first, int last, int mx, int my) {
            int cy = y;
            for (List<BowlLiquid> row : liquidRows) {
                int rowW = row.stream().mapToInt(this::liquidWidth).sum() - 8;
                int lx = x + (W - rowW) / 2;
                for (BowlLiquid l : row) {
                    Component name = Component.translatable(SpellBowlItem.liquidKey(l));
                    drop(g, lx, cy + 1, l.color);
                    g.drawString(font, name, lx + 10, cy, BookStyle.INK, false);
                    if (Ink.over(mx, my, lx, cy - 1, liquidWidth(l) - 8, 10)) {
                        List<Component> tip = new ArrayList<>();
                        tip.add(name.copy().withStyle(ChatFormatting.AQUA));
                        if (l == BowlLiquid.POTION) tip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.any_potion").withStyle(ChatFormatting.GRAY));
                        if (l == BowlLiquid.BLOOD) tip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.any_blood").withStyle(ChatFormatting.GRAY));
                        tip.add(Component.translatable("jei.supernaturalcraft.bowl_spell.liquid").withStyle(ChatFormatting.DARK_GRAY));
                        tooltip.accept(tip);
                    }
                    lx += liquidWidth(l);
                }
                cy += 11;
            }
            if (!liquidRows.isEmpty()) cy += 3;
            List<Ingredient> ins = recipe.ingredients();
            if (!ins.isEmpty()) {
                int x0 = x + (W - ins.size() * SLOT_SPACING) / 2;
                for (int i = 0; i < ins.size(); i++) slot(g, cycle(ins.get(i)), x0 + i * SLOT_SPACING, cy, mx, my, List.of());
                cy += SLOT + 3;
            }
            if (recipe.manaCost() > 0) {
                Ink.centred(g, font, Component.translatable("screen.supernaturalcraft.book.journal.mana", Math.round(recipe.manaCost())),
                        x + W / 2, cy, W, BookStyle.MANA);
                cy += LINE;
            }
            for (FormattedCharSequence n : notes) {
                g.drawString(font, n, x + (W - font.width(n)) / 2, cy, BookStyle.BLOOD, false);
                cy += 9;
            }
            cy += 3;
            for (FormattedCharSequence w : words) {
                g.drawString(font, w, x + (W - font.width(w)) / 2, cy, BookStyle.BLOOD, false);
                cy += 9;
            }
            drawCaption(g, x, y + height() - PAD);
        }
    }

    // --- pieces ------------------------------------------------------------------------------------

    /** One ingredient of several: they take turns, a second each. */
    private static ItemStack cycle(Ingredient ing) {
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        return items[(int) (Util.getMillis() / 1000 % items.length)];
    }

    private void slot(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my, List<Component> extra) {
        boolean over = Ink.over(mx, my, x, y, SLOT, SLOT);
        (over ? BookAtlas.SIGIL_SLOT_HOVER : BookAtlas.SIGIL_SLOT).draw(g, x, y);
        if (stack.isEmpty()) return;
        g.renderItem(stack, x + 2, y + 2);
        g.renderItemDecorations(font, stack, x + 2, y + 2);
        if (over) itemTooltip(stack, extra);
    }

    private void resultSlot(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my) {
        BookAtlas.panel(g, BookAtlas.CARD, x, y, 24, 24);
        if (stack.isEmpty()) return;
        g.renderItem(stack, x + 4, y + 4);
        g.renderItemDecorations(font, stack, x + 4, y + 4);
        if (Ink.over(mx, my, x, y, 24, 24)) itemTooltip(stack, List.of());
    }

    void itemTooltip(ItemStack stack, List<Component> extra) {
        List<Component> lines = new ArrayList<>(stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.Default.NORMAL));
        lines.addAll(extra);
        tooltip.accept(lines);
    }

    /** A drop of liquid: a little square of its colour with a darker rim. */
    private static void drop(GuiGraphics g, int x, int y, int rgb) {
        int dark = 0xFF000000 | ((rgb >> 1) & 0x7F7F7F);
        g.fill(x, y, x + 7, y + 7, dark);
        g.fill(x + 1, y + 1, x + 6, y + 6, 0xFF000000 | rgb);
        g.fill(x + 2, y + 2, x + 3, y + 3, 0x80FFFFFF);
    }

    /** A dotted circle, as if drawn in chalk. */
    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        int steps = (int) (r * 2 * Math.PI / 3);
        for (int i = 0; i < steps; i++) {
            double a = i * 2 * Math.PI / steps;
            int px = (int) Math.round(cx + Math.cos(a) * r), py = (int) Math.round(cy + Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    static Component patternName(ResourceLocation pattern) {
        String path = pattern.getPath();
        StringBuilder pretty = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (!pretty.isEmpty()) pretty.append(' ');
            pretty.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.translatableWithFallback("screen.supernaturalcraft.book.journal.pattern." + path, pretty.toString());
    }

    /** A ritual's or bowl spell's conditions (night, eclipse, a dimension, an advancement), as the JEI pages word them. */
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
}
