package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealEvents;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealKind;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRules;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.client.book.BookSection;
import org.papiricoh.supernaturalcraft.client.book.BookStyle;
import org.papiricoh.supernaturalcraft.network.SealSitePayload;
import org.papiricoh.supernaturalcraft.network.SealStatePayload;

import java.util.List;

/**
 * The Seals in the Hunter's Book (v0.40): on the left page the twelve seals (broken in blood, standing in ink; hover one for its
 * name), how many have broken, the stage, the days to the next and those the hunters have won; on the right the hunter's own
 * share and the seal they are called to now (its kind, which way and how far, the rite). Drawn in book space ({@link BookStyle}).
 * Text: {@code screen.supernaturalcraft.book.seals.*}.
 */
public class SealsSection extends BookSection {

    private static final int CELL = 44, ROW = 30, GLYPH = 26;

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        SealStatePayload s = ClientSeals.state();
        int x = BookStyle.LEFT_X, y = BookStyle.PAGE_Y;
        title(g, Component.translatable("screen.supernaturalcraft.book.seals.title"), x, y);
        y += 16;
        if (!s.started()) {
            wrap(g, Component.translatable("screen.supernaturalcraft.book.seals.unstarted"), x, y, BookStyle.FADED);
            return;
        }
        // The twelve seals.
        for (int i = 0; i < SealRules.TOTAL; i++) {
            int gx = x + (i % 4) * CELL + 9, gy = y + (i / 4) * ROW;
            boolean broken = i < s.broken();
            int ink = broken ? BookStyle.BLOOD : BookStyle.INK;
            g.fill(gx, gy, gx + GLYPH, gy + GLYPH, (broken ? 0x40 : 0x18) << 24 | ink & 0xFFFFFF);
            outline(g, gx, gy, GLYPH, ink);
            if (broken) {
                // The crack across a broken seal.
                for (int k = 0; k < GLYPH - 4; k++) g.fill(gx + 2 + k, gy + 2 + (k * 7 / 9) % (GLYPH - 4), gx + 3 + k, gy + 3 + (k * 7 / 9) % (GLYPH - 4), 0xFF000000 | ink);
            }
            String n = String.valueOf(i + 1);
            g.drawString(font, n, gx + (GLYPH - font.width(n)) / 2, gy + (GLYPH - 8) / 2, 0xFF000000 | (broken ? 0xF2E6D0 : BookStyle.INK), false);
            if (mx >= gx && mx < gx + GLYPH && my >= gy && my < gy + GLYPH) {
                book.tooltip(List.of(broken ? Component.translatable(SealWorld.sealName(i + 1))
                        : Component.translatable("screen.supernaturalcraft.book.seals.standing", i + 1)));
            }
        }
        y += 3 * ROW + 4;
        line(g, Component.translatable("screen.supernaturalcraft.book.seals.broken", s.broken(), SealRules.TOTAL), x, y, BookStyle.BLOOD);
        y += 11;
        line(g, Component.translatable("screen.supernaturalcraft.book.seals.stage." + Mth.clamp(s.stage(), 0, SealRules.MAX_STAGE)), x, y, BookStyle.INK);
        y += 11;
        Component next = s.risen() ? Component.translatable(s.echo() ? "screen.supernaturalcraft.book.seals.echo" : "screen.supernaturalcraft.book.seals.risen")
                : s.broken() >= SealRules.PASSIVE_MAX ? Component.translatable("screen.supernaturalcraft.book.seals.lilith")
                : Component.translatable("screen.supernaturalcraft.book.seals.next", s.daysLeft());
        y = wrap(g, next, x, y, BookStyle.FADED);
        line(g, Component.translatable("screen.supernaturalcraft.book.seals.delayed", s.delayed(), SealRules.DELAY_CAP), x, y + 2, BookStyle.GOLD);

        // The right page: the hunter's share, and the call.
        int rx = BookStyle.RIGHT_X, ry = BookStyle.PAGE_Y;
        title(g, Component.translatable("screen.supernaturalcraft.book.seals.share"), rx, ry);
        ry += 16;
        line(g, Component.translatable("screen.supernaturalcraft.book.seals.held", s.defended()), rx, ry, BookStyle.INK);
        ry += 11;
        line(g, Component.translatable("screen.supernaturalcraft.book.seals.let_break", s.letBreak()), rx, ry, BookStyle.INK);
        ry += 11;
        line(g, Component.translatable("screen.supernaturalcraft.book.seals.angels", s.angelsDefended(), SealRules.URIEL_AFTER), rx, ry, BookStyle.MANA);
        ry += 20;
        title(g, Component.translatable("screen.supernaturalcraft.book.seals.call"), rx, ry);
        ry += 16;
        SealSitePayload site = ClientSeals.site();
        if (!site.active()) {
            wrap(g, Component.translatable("screen.supernaturalcraft.book.seals.no_call"), rx, ry, BookStyle.FADED);
            return;
        }
        SealKind kind = SealKind.byOrdinal(site.kind());
        line(g, Component.translatable(kind.nameKey()), rx, ry, BookStyle.BLOOD);
        ry += 11;
        if (mc.player != null) {
            Vec3 at = Vec3.atCenterOf(site.pos());
            int dist = (int) Math.round(ClientSeals.distance(mc.player.position()));
            line(g, Component.translatable("screen.supernaturalcraft.book.seals.where", dist,
                    Component.translatable(SealEvents.direction(mc.player.position(), at))), rx, ry, BookStyle.INK);
            ry += 11;
        }
        int w = BookStyle.PAGE_W - 8;
        g.fill(rx, ry, rx + w, ry + 6, 0x30000000 | BookStyle.INK & 0xFFFFFF);
        g.fill(rx, ry, rx + Math.round(w * Mth.clamp(site.progress() / SealRules.RITE_MAX, 0, 1)), ry + 6, BookStyle.BLOOD);
        ry += 10;
        int secs = site.ticksLeft() / 20;
        line(g, Component.translatable("hud.supernaturalcraft.seal.rite", site.progress(), secs / 60, String.format("%02d", secs % 60)), rx, ry, BookStyle.FADED);
        ry += 13;
        wrap(g, Component.translatable(kind.hintKey()), rx, ry, BookStyle.FADED);
    }

    private void title(GuiGraphics g, Component text, int x, int y) {
        g.drawString(font, text, x, y, BookStyle.INK, false);
        g.fill(x, y + 10, x + BookStyle.PAGE_W - 8, y + 11, 0x60000000 | BookStyle.INK & 0xFFFFFF);
    }

    private void line(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    /** Wrapped text; returns the y under it. */
    private int wrap(GuiGraphics g, Component text, int x, int y, int color) {
        for (FormattedCharSequence l : font.split(text, BookStyle.PAGE_W - 6)) {
            g.drawString(font, l, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private static void outline(GuiGraphics g, int x, int y, int s, int color) {
        int c = 0xFF000000 | color;
        g.fill(x, y, x + s, y + 1, c);
        g.fill(x, y + s - 1, x + s, y + s, c);
        g.fill(x, y, x + 1, y + s, c);
        g.fill(x + s - 1, y, x + s, y + s, c);
    }
}
