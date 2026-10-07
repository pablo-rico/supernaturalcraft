package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SigilKind;

import java.util.List;

/**
 * The right page's "Sigils" leaf: every sigil there is, by kind. Known ones in colour (a click
 * adds them to the draft), unknown ones as silhouettes. The card below reads the sigil under the
 * mouse, or the last one picked: what it does and costs, and where a page of it may be found.
 */
final class SigilIndex {

    private static final int X = BookStyle.RIGHT_X + 3, TOP = ScriptoriumSection.RIGHT_TOP;
    private static final int PER_ROW = 8, STEP = 22;
    private static final int BOTTOM = BookStyle.PAGE_Y + BookStyle.PAGE_H;

    private static @Nullable ResourceLocation lastSelected;

    private final ScriptoriumSection section;
    @Nullable ResourceLocation selected = lastSelected;
    private @Nullable ResourceLocation hovered;

    SigilIndex(ScriptoriumSection section) {
        this.section = section;
    }

    void select(@Nullable ResourceLocation id) {
        selected = id;
        lastSelected = id;
    }

    /** Lays out the grid: calls {@code cell} for every sigil and returns the y under the last row. */
    private int layout(Cell cell) {
        int y = TOP;
        for (SigilKind kind : SigilKind.values()) {
            List<ResourceLocation> ids = Sigils.byKind(kind);
            if (ids.isEmpty()) continue;
            cell.label(kind, ids, y);
            y += 10;
            for (int i = 0; i < ids.size(); i++) {
                cell.slot(ids.get(i), X + (i % PER_ROW) * STEP, y + (i / PER_ROW) * STEP);
            }
            y += ((ids.size() + PER_ROW - 1) / PER_ROW) * STEP + 3;
        }
        return y;
    }

    private interface Cell {
        void label(SigilKind kind, List<ResourceLocation> ids, int y);

        void slot(ResourceLocation id, int x, int y);
    }

    void render(GuiGraphics g, int mx, int my) {
        Font font = section.font();
        hovered = null;
        int cardY = layout(new Cell() {
            @Override
            public void label(SigilKind kind, List<ResourceLocation> ids, int y) {
                long known = ids.stream().filter(Sigils::known).count();
                Component text = Component.translatable("screen.supernaturalcraft.composer.known." + kind.getSerializedName());
                g.drawString(font, text, X, y, BookStyle.FADED, false);
                String count = known + "/" + ids.size();
                g.drawString(font, count, X + font.width(text) + 4, y, known == ids.size() ? BookStyle.GOLD : BookStyle.FADED, false);
            }

            @Override
            public void slot(ResourceLocation id, int x, int y) {
                boolean over = mx >= x && mx < x + 20 && my >= y && my < y + 20;
                if (over) hovered = id;
                (over || id.equals(selected) ? BookAtlas.SIGIL_SLOT_HOVER : BookAtlas.SIGIL_SLOT).draw(g, x, y);
                Sigils.glyph(g, id, x + 2, y + 2, 16, Sigils.known(id));
                if (id.equals(selected)) g.renderOutline(x - 1, y - 1, 22, 22, BookStyle.GOLD);
                if (section.draft.contains(id)) g.fill(x + 15, y + 2, x + 18, y + 5, BookStyle.GOLD);
            }
        });
        card(g, font, cardY + 1);
    }

    /** The reading card under the grid. */
    private void card(GuiGraphics g, Font font, int y) {
        int x = BookStyle.RIGHT_X, w = BookStyle.PAGE_W, h = BOTTOM - y;
        if (h < 30) return;
        BookAtlas.panel(g, BookAtlas.CARD, x, y, w, h);
        int ix = x + 6, iy = y + 6, iw = w - 12, bottom = y + h - 6;
        ResourceLocation id = hovered != null ? hovered : selected;
        SigilComponent s = Sigils.get(id);
        if (s == null) {
            for (FormattedCharSequence line : font.split(Component.translatable("screen.supernaturalcraft.book.scriptorium.index_hint"), iw)) {
                g.drawString(font, line, ix, iy, BookStyle.FADED, false);
                iy += 10;
            }
            return;
        }
        boolean known = Sigils.known(id);
        Sigils.glyph(g, id, ix, iy, 16, known);
        Component name = known ? Sigils.name(id) : Component.translatable("screen.supernaturalcraft.book.scriptorium.unknown");
        g.drawString(font, name, ix + 20, iy, known ? BookStyle.INK : BookStyle.FADED, false);
        Component kind = Sigils.kindName(s.kind()).copy().append(" · ")
                .append(Component.translatable("screen.supernaturalcraft.book.scriptorium.tier", Sigils.tier(s.tier())));
        g.drawString(font, kind, ix + 20, iy + 9, BookStyle.FADED, false);
        iy += 20;

        int footer = 0;
        if (known) {
            String stats = s.kind() == SigilKind.MODIFIER
                    ? Component.translatable("screen.supernaturalcraft.composer.multiplier", Sigils.num(Sigils.manaMultiplier(s))).getString()
                    : Component.translatable("screen.supernaturalcraft.composer.mana", Sigils.num(s.manaCost())).getString();
            if (s.cooldown() > 0) {
                stats += "  " + Component.translatable("screen.supernaturalcraft.book.scriptorium.cooldown", Sigils.num(s.cooldown() / 20f)).getString();
            }
            int rowH = s.reagents().isEmpty() ? 11 : 17;
            g.drawString(font, stats, ix, iy + (rowH - 8) / 2, BookStyle.MANA, false);
            int rx = ix + iw - 16;
            for (SigilComponent.Reagent r : s.reagents()) {
                ItemStack stack = r.stack();
                g.renderItem(stack, rx, iy);
                g.renderItemDecorations(font, stack, rx, iy);
                rx -= 17;
            }
            if (!s.reagents().isEmpty()) {
                Component burns = Component.translatable("screen.supernaturalcraft.book.scriptorium.burns");
                g.drawString(font, burns, rx + 15 - font.width(burns), iy + 4, BookStyle.BLOOD, false);
            }
            iy += rowH + 1;
        } else {
            g.drawString(font, Component.translatable("screen.supernaturalcraft.book.scriptorium.not_learned"), ix, iy, BookStyle.BLOOD, false);
            iy += 11;
        }

        Component desc = Component.translatable(SigilComponent.translationKey(id) + ".desc");
        Component found = Component.translatable("screen.supernaturalcraft.book.scriptorium.found",
                Component.translatable(SigilComponent.translationKey(id) + ".source"));
        int text = (known ? font.split(desc, iw).size() * 9 + 3 : 0) + font.split(found, iw).size() * 9;
        // The "click to add" footer only when the reading fits above it.
        if (known && !section.draft.contains(id) && iy + text + 10 <= bottom) footer = 10;
        int limit = bottom - footer;
        if (known) iy = lines(g, font, desc, ix, iy, iw, limit, BookStyle.INK) + 3;
        lines(g, font, found, ix, iy, iw, limit, BookStyle.FADED);
        if (footer > 0) {
            Component add = Component.translatable("screen.supernaturalcraft.book.scriptorium.click_add");
            g.drawString(font, add, ix + iw - font.width(add), bottom - 8, BookStyle.GOLD, false);
        }
    }

    /** Wrapped text, stopping at {@code limit}; returns the y under it. */
    private static int lines(GuiGraphics g, Font font, Component text, int x, int y, int w, int limit, int color) {
        for (FormattedCharSequence line : font.split(text, w)) {
            if (y + 8 > limit) break;
            g.drawString(font, line, x, y, color, false);
            y += 9;
        }
        return y;
    }

    boolean mouseClicked(double mx, double my, int button) {
        ResourceLocation[] hit = new ResourceLocation[1];
        layout(new Cell() {
            @Override
            public void label(SigilKind kind, List<ResourceLocation> ids, int y) {
            }

            @Override
            public void slot(ResourceLocation id, int x, int y) {
                if (mx >= x && mx < x + 20 && my >= y && my < y + 20) hit[0] = id;
            }
        });
        if (hit[0] == null) return false;
        select(hit[0]);
        if (button == 0 && Sigils.known(hit[0])) section.addSigil(hit[0]);
        else section.click();
        return true;
    }
}
