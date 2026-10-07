package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.client.ClientHunterLog;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.BookButton;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.network.LibraryEditPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * The right page's "Library" leaf: the hunter's 24 saved designs (kept on the player, synced by
 * the server). Pick a slot; double-click or Load puts it on the table; Save writes the draft into
 * it (a second click to overwrite another design); Delete asks twice too.
 */
final class Library {

    private static final int COLS = 3, ROWS = 8;
    private static final int X = BookStyle.RIGHT_X + 2, TOP = ScriptoriumSection.RIGHT_TOP;
    private static final int STEP_X = 60, STEP_Y = 22, SLOT_W = 56, SLOT_H = 20;
    private static final int ACTIONS_Y = TOP + ROWS * STEP_Y + 2;

    private enum Pending {NONE, OVERWRITE, DELETE}

    private static int lastSlot = -1;

    private final ScriptoriumSection section;
    int slot = lastSlot;
    private Pending pending = Pending.NONE;
    private int lastClickSlot = -1;
    private long lastClickAt;
    private BookButton load, save, delete;

    Library(ScriptoriumSection section) {
        this.section = section;
    }

    void init() {
        int x = BookStyle.RIGHT_X, w = 56;
        load = section.book().add(new BookButton(x, ACTIONS_Y, w, 14,
                Component.translatable("screen.supernaturalcraft.book.scriptorium.library.load"), this::load));
        save = section.book().add(new BookButton(x + 62, ACTIONS_Y, w, 14,
                Component.translatable("screen.supernaturalcraft.book.scriptorium.library.save"), this::save));
        delete = section.book().add(new BookButton(x + 124, ACTIONS_Y, w, 14,
                Component.translatable("screen.supernaturalcraft.book.scriptorium.library.delete"), this::delete));
        pending = Pending.NONE;
    }

    void select(int s) {
        if (s != slot) pending = Pending.NONE;
        slot = s;
        lastSlot = s;
    }

    private static List<Spell> designs() {
        return ClientHunterLog.designs();
    }

    private Spell design(int i) {
        List<Spell> d = designs();
        return i >= 0 && i < d.size() ? d.get(i) : Spell.EMPTY;
    }

    private static boolean blank(Spell s) {
        return s.isEmpty() && s.modifiers().isEmpty();
    }

    private void load() {
        if (slot < 0 || blank(design(slot))) return;
        section.loadDraft(design(slot));
        section.status(Component.translatable("screen.supernaturalcraft.book.scriptorium.library.loaded", GrimoireItem.spellName(design(slot))),
                BookStyle.INK);
    }

    private void save() {
        if (slot < 0) return;
        Spell spell = section.draft.toSpell();
        Spell there = design(slot);
        if (!blank(there) && !there.equals(spell) && pending != Pending.OVERWRITE) {
            pending = Pending.OVERWRITE;
            return;
        }
        PacketDistributor.sendToServer(new LibraryEditPayload(slot, spell));
        pending = Pending.NONE;
        section.status(Component.translatable("screen.supernaturalcraft.book.scriptorium.library.saved", slot + 1), BookStyle.INK);
    }

    private void delete() {
        if (slot < 0) return;
        if (pending != Pending.DELETE) {
            pending = Pending.DELETE;
            return;
        }
        PacketDistributor.sendToServer(new LibraryEditPayload(slot, Spell.EMPTY));
        pending = Pending.NONE;
    }

    void render(GuiGraphics g, int mx, int my) {
        Font font = section.font();
        List<Spell> d = designs();
        int hover = slotAt(mx, my);
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = X + (i % COLS) * STEP_X, y = TOP + (i / COLS) * STEP_Y;
            (i == hover ? BookAtlas.LIBRARY_SLOT_HOVER : BookAtlas.LIBRARY_SLOT).draw(g, x, y);
            Spell s = i < d.size() ? d.get(i) : Spell.EMPTY;
            if (blank(s)) {
                Component empty = Component.translatable("screen.supernaturalcraft.book.scriptorium.library.empty");
                g.drawString(font, empty, x + (SLOT_W - font.width(empty)) / 2, y + 6, 0x887A6448, false);
            } else {
                String name = GrimoireItem.spellName(s).getString();
                if (font.width(name) > SLOT_W - 6) name = font.plainSubstrByWidth(name, SLOT_W - 6 - font.width("…")) + "…";
                g.drawString(font, name, x + 3, y + 2, BookStyle.INK, false);
                int gx = x + 3;
                for (ResourceLocation id : GrimoireItem.allSigils(s)) {
                    if (gx + 7 > x + SLOT_W - 2) break;
                    Sigils.glyph(g, id, gx, y + 11, 7, true);
                    gx += 7;
                }
            }
            if (i == slot) g.renderOutline(x - 1, y - 1, SLOT_W + 2, SLOT_H + 2, BookStyle.GOLD);
        }
        if (hover >= 0) section.book().tooltip(tooltip(hover));

        Spell chosen = design(slot);
        Spell draft = section.draft.toSpell();
        load.active = slot >= 0 && !blank(chosen);
        save.active = slot >= 0 && !draft.isEmpty() && !chosen.equals(draft);
        delete.active = slot >= 0 && !blank(chosen);
        save.setMessage(Component.translatable(pending == Pending.OVERWRITE
                ? "screen.supernaturalcraft.book.scriptorium.library.overwrite" : "screen.supernaturalcraft.book.scriptorium.library.save"));
        delete.setMessage(Component.translatable(pending == Pending.DELETE
                ? "screen.supernaturalcraft.book.scriptorium.library.confirm" : "screen.supernaturalcraft.book.scriptorium.library.delete"));

        Component line;
        int color = BookStyle.FADED;
        if (pending == Pending.OVERWRITE) {
            line = Component.translatable("screen.supernaturalcraft.book.scriptorium.library.overwrite_hint", GrimoireItem.spellName(chosen));
            color = BookStyle.BLOOD;
        } else if (pending == Pending.DELETE) {
            line = Component.translatable("screen.supernaturalcraft.book.scriptorium.library.delete_hint", GrimoireItem.spellName(chosen));
            color = BookStyle.BLOOD;
        } else if (slot >= 0) {
            line = blank(chosen) ? Component.translatable("screen.supernaturalcraft.book.scriptorium.library.slot_empty", slot + 1)
                    : Component.translatable("screen.supernaturalcraft.book.scriptorium.library.slot", slot + 1, GrimoireItem.spellName(chosen));
            color = BookStyle.INK;
        } else {
            long used = d.stream().filter(s -> !blank(s)).count();
            line = Component.translatable("screen.supernaturalcraft.book.scriptorium.library.hint", used, COLS * ROWS);
        }
        int y = ACTIONS_Y + 18;
        for (FormattedCharSequence l : font.split(line, BookStyle.PAGE_W)) {
            if (y + 8 > BookStyle.PAGE_Y + BookStyle.PAGE_H) break;
            g.drawString(font, l, BookStyle.RIGHT_X, y, color, false);
            y += 9;
        }
    }

    private List<Component> tooltip(int i) {
        Spell s = design(i);
        List<Component> lines = new ArrayList<>();
        if (blank(s)) {
            lines.add(Component.translatable("screen.supernaturalcraft.book.scriptorium.library.slot_empty", i + 1).withStyle(ChatFormatting.GRAY));
            return lines;
        }
        lines.add(GrimoireItem.spellName(s).withStyle(ChatFormatting.GOLD));
        lines.add(GrimoireItem.sigilList(s).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("screen.supernaturalcraft.composer.mana", Sigils.num(Sigils.mana(s))).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("screen.supernaturalcraft.book.scriptorium.library.double_click").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        return lines;
    }

    private static int slotAt(double mx, double my) {
        if (mx < X || my < TOP) return -1;
        int c = (int) ((mx - X) / STEP_X), r = (int) ((my - TOP) / STEP_Y);
        if (c >= COLS || r >= ROWS) return -1;
        if (mx - X - c * STEP_X >= SLOT_W || my - TOP - r * STEP_Y >= SLOT_H) return -1;
        return r * COLS + c;
    }

    boolean mouseClicked(double mx, double my, int button) {
        int i = slotAt(mx, my);
        if (i < 0) return false;
        long now = Util.getMillis();
        boolean twice = i == lastClickSlot && now - lastClickAt < 350;
        lastClickSlot = i;
        lastClickAt = now;
        select(i);
        section.click();
        if (twice) load();
        return true;
    }
}
