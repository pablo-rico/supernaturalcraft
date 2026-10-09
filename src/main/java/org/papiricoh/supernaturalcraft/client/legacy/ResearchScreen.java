package org.papiricoh.supernaturalcraft.client.legacy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchMenu;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchOffer;
import org.papiricoh.supernaturalcraft.legacy.research.ResearchSlot;
import org.papiricoh.supernaturalcraft.network.ResearchActionPayload;
import org.papiricoh.supernaturalcraft.network.ResearchBoardPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * The research desk (v0.17), in the Men of Letters' style: a walnut frame, green leather, brass rules and the banker's lamp's
 * glow. Left, the board of topics on offer ({@link ResearchBoardPayload}); centre, the chosen topic's cost and time and START;
 * right, the research under way at the hunter's desks, each with its countdown and a CANCEL.
 */
public class ResearchScreen extends AbstractContainerScreen<ResearchMenu> {

    static final int W = 316, H = 204;
    static final int WALNUT_DARK = 0xFF1A0F08, WALNUT = 0xFF3B2414, WALNUT_LIGHT = 0xFF4E311C, LEATHER = 0xFF15281B,
            LEATHER_LIGHT = 0xFF1E3826, BRASS = 0xFFB08B36, BRASS_LIGHT = 0xFFD0AE55, PAPER = 0xFFF3E7C8, FADED = 0xFF9C9178,
            LAMP = 0xFF8FD08A, RED = 0xFFD07A6A;
    private static final int LIST_X = 8, LIST_Y = 48, LIST_W = 120, ROW = 20, LIST_H = 148;
    private static final int MID_X = 134, MID_W = 94;
    private static final int SLOT_X = 234, SLOT_W = 74, SLOT_H = 34;

    private List<ResearchOffer> offers = List.of();
    private int maxSlots = 1;
    private String selected = "";
    private double scroll;
    private int startX, startY, startW = 70, startH = 14;

    public ResearchScreen(ResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = W;
        imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        boardChanged();
    }

    /** A board arrived (or the screen opened after it did). */
    void boardChanged() {
        ResearchBoardPayload b = ClientLegacy.board(menu.containerId);
        if (b == null) return;
        offers = b.offers();
        maxSlots = Math.max(1, b.maxSlots());
        if (offers.stream().noneMatch(o -> o.topic().equals(selected))) selected = offers.isEmpty() ? "" : offers.get(0).topic();
    }

    private ResearchOffer chosen() {
        for (ResearchOffer o : offers) if (o.topic().equals(selected)) return o;
        return null;
    }

    private long now() {
        return minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
    }

    // --- drawing ---------------------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        int x = leftPos, y = topPos;
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0x90000000);
        g.fill(x, y, x + W, y + H, WALNUT_DARK);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, WALNUT);
        // Wood grain.
        for (int i = 0; i < H; i += 3) g.fill(x + 1, y + i, x + W - 1, y + i + 1, (i * 7 % 5 == 0) ? 0x14000000 : 0x0AFFFFFF);
        // The leather writing surface, with a brass rule.
        g.fill(x + 5, y + 31, x + W - 5, y + H - 5, BRASS);
        g.fill(x + 6, y + 32, x + W - 6, y + H - 6, LEATHER);
        // The banker's lamp's pool of green light over the middle.
        int cx = x + MID_X + MID_W / 2;
        for (int r = 0; r < 6; r++) {
            int a = 0x12 - r * 2;
            g.fill(cx - 30 - r * 8, y + 32, cx + 30 + r * 8, y + 32 + 40 + r * 12, (a << 24) | 0x40A556);
        }
        // The lamp itself above the leather: a green shade on a brass stem.
        g.fill(cx - 22, y + 8, cx + 22, y + 18, 0xFF1A5E2C);
        g.fill(cx - 20, y + 9, cx + 20, y + 13, 0xFF40A556);
        g.fill(cx - 23, y + 17, cx + 23, y + 19, BRASS_LIGHT);
        g.fill(cx - 1, y + 19, cx + 1, y + 31, BRASS);
        g.fill(cx - 22, y + 19, cx + 22, y + 22, 0x5540A556);
        // Column rules.
        g.fill(x + LIST_X + LIST_W + 2, y + 34, x + LIST_X + LIST_W + 3, y + H - 8, 0x60B08B36);
        g.fill(x + SLOT_X - 4, y + 34, x + SLOT_X - 3, y + H - 8, 0x60B08B36);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 8, 7, BRASS_LIGHT, false);
        Component rank = Component.translatable("screen.supernaturalcraft.research.rank", LegacyText.rank(ClientLegacy.legacy().rank()));
        g.drawString(font, rank, 8, 18, FADED, false);
        int used = ClientLegacy.archive().slots().size();
        Component desks = Component.translatable("screen.supernaturalcraft.research.desks", used, maxSlots);
        g.drawString(font, desks, W - 8 - font.width(desks), 7, used >= maxSlots ? RED : LAMP, false);
        g.drawString(font, Component.translatable("screen.supernaturalcraft.research.under_way"), SLOT_X, 35, BRASS_LIGHT, false);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        drawOffers(g, mx, my);
        drawChosen(g, mx, my);
        drawSlots(g, mx, my);
    }

    private void drawOffers(GuiGraphics g, int mx, int my) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        g.drawString(font, Component.translatable("screen.supernaturalcraft.research.board"), x, y - 13, BRASS_LIGHT, false);
        if (offers.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.translatable("screen.supernaturalcraft.research.empty")
                    .withStyle(ChatFormatting.ITALIC), LIST_W - 4)) {
                g.drawString(font, line, x + 2, y + 4, FADED, false);
                y += 10;
            }
            return;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
        g.enableScissor(x, y, x + LIST_W, y + LIST_H);
        for (int i = 0; i < offers.size(); i++) {
            ResearchOffer o = offers.get(i);
            int ry = y + i * ROW - (int) scroll;
            if (ry + ROW < y || ry > y + LIST_H) continue;
            boolean sel = o.topic().equals(selected), over = in(mx, my, x, ry, LIST_W, ROW - 1) && my >= y && my < y + LIST_H;
            if (sel) {
                g.fill(x, ry, x + LIST_W, ry + ROW - 1, 0x5040A556);
                g.fill(x, ry, x + 2, ry + ROW - 1, LAMP);
            } else if (over) {
                g.fill(x, ry, x + LIST_W, ry + ROW - 1, 0x30FFFFFF);
            }
            g.renderFakeItem(LegacyText.icon(o.topic()), x + 3, ry + 1);
            String tier = LegacyText.roman(o.tier());
            int tw = font.width(tier);
            Component t = LegacyText.title(o.titleKey(), o.args());
            g.drawString(font, fit(t, LIST_W - 26 - tw - 4), x + 22, ry + 6, o.affordable() ? PAPER : FADED, false);
            g.drawString(font, tier, x + LIST_W - tw - 3, ry + 6, BRASS, false);
        }
        g.disableScissor();
        int max = maxScroll();
        if (max > 0) {
            int thumb = Math.max(10, LIST_H * LIST_H / (offers.size() * ROW));
            int ty = y + (int) ((LIST_H - thumb) * (scroll / max));
            g.fill(x + LIST_W + 1, ty, x + LIST_W + 2, ty + thumb, BRASS);
        }
    }

    private int maxScroll() {
        return Math.max(0, offers.size() * ROW - LIST_H);
    }

    private void drawChosen(GuiGraphics g, int mx, int my) {
        ResearchOffer o = chosen();
        int x = leftPos + MID_X, y = topPos + LIST_Y;
        if (o == null) return;
        g.renderFakeItem(LegacyText.icon(o.topic()), x + (MID_W - 16) / 2, y - 2);
        y += 18;
        for (FormattedCharSequence line : font.split(LegacyText.title(o.titleKey(), o.args()), MID_W)) {
            g.drawString(font, line, x + (MID_W - font.width(line)) / 2, y, PAPER, false);
            y += 10;
        }
        Component kind = Component.translatable("screen.supernaturalcraft.research.kind_tier", LegacyText.kind(o.topic()), LegacyText.roman(o.tier()));
        g.drawString(font, kind, x + (MID_W - font.width(kind)) / 2, y + 1, FADED, false);
        y += 14;
        g.drawString(font, Component.translatable("screen.supernaturalcraft.research.cost"), x, y, BRASS_LIGHT, false);
        y += 11;
        int col = 0;
        ItemStack hover = ItemStack.EMPTY;
        for (ItemStack s : o.cost()) {
            int ix = x + col * 19, iy = y;
            g.fill(ix, iy, ix + 18, iy + 18, o.affordable() ? 0x40000000 : 0x40D07A6A);
            g.renderItem(s, ix + 1, iy + 1);
            g.renderItemDecorations(font, s, ix + 1, iy + 1);
            if (in(mx, my, ix, iy, 18, 18)) hover = s;
            if (++col == 5) {
                col = 0;
                y += 19;
            }
        }
        y += col == 0 ? 2 : 21;
        Component time = Component.translatable("screen.supernaturalcraft.research.time", LegacyText.clock(o.ticks()));
        g.drawString(font, time, x, y, PAPER, false);
        y += 14;
        boolean free = ClientLegacy.archive().slots().size() < maxSlots;
        boolean can = o.affordable() && free;
        startX = x + (MID_W - startW) / 2;
        startY = y;
        button(g, startX, startY, startW, startH, Component.translatable("screen.supernaturalcraft.research.start"), can,
                in(mx, my, startX, startY, startW, startH));
        if (!can) {
            Component why = Component.translatable(free ? "screen.supernaturalcraft.research.missing" : "screen.supernaturalcraft.research.busy");
            for (FormattedCharSequence line : font.split(why.copy().withStyle(ChatFormatting.ITALIC), MID_W)) {
                g.drawString(font, line, x + (MID_W - font.width(line)) / 2, startY + startH + 4, RED, false);
                y += 10;
            }
        }
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    private void drawSlots(GuiGraphics g, int mx, int my) {
        List<ResearchSlot> slots = ClientLegacy.archive().slots();
        int x = leftPos + SLOT_X, y = topPos + LIST_Y;
        long now = now();
        for (int i = 0; i < maxSlots; i++) {
            int sy = y + i * (SLOT_H + 4);
            g.fill(x, sy, x + SLOT_W, sy + SLOT_H, 0x50000000);
            g.fill(x, sy, x + SLOT_W, sy + 1, 0x80B08B36);
            if (i >= slots.size()) {
                Component empty = Component.translatable("screen.supernaturalcraft.research.empty_desk").withStyle(ChatFormatting.ITALIC);
                g.drawString(font, empty, x + (SLOT_W - font.width(empty)) / 2, sy + 13, FADED, false);
                continue;
            }
            ResearchSlot s = slots.get(i);
            g.renderFakeItem(LegacyText.icon(s.topic()), x + 2, sy + 3);
            g.drawString(font, fit(LegacyText.topic(s.topic()), SLOT_W - 22), x + 20, sy + 4, PAPER, false);
            float p = s.progress(now);
            int bx = x + 3, by = sy + 18, bw = SLOT_W - 18;
            g.fill(bx, by, bx + bw, by + 4, 0xFF0B1A10);
            g.fill(bx, by, bx + Math.round(bw * p), by + 4, s.done(now) ? BRASS_LIGHT : LAMP);
            String left = s.done(now) ? Component.translatable("screen.supernaturalcraft.research.finishing").getString() : LegacyText.clock(s.end() - now);
            g.drawString(font, left, x + 3, sy + 24, FADED, false);
            // CANCEL: a small brass x.
            int cx = x + SLOT_W - 12, cy = sy + 17;
            boolean over = in(mx, my, cx, cy, 10, 10);
            g.fill(cx, cy, cx + 10, cy + 10, over ? 0xFF7A2A22 : 0xFF3A1A12);
            g.drawString(font, "x", cx + 3, cy + 1, over ? PAPER : BRASS_LIGHT, false);
            if (over) g.renderTooltip(font, Component.translatable("screen.supernaturalcraft.research.cancel"), mx, my);
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, Component label, boolean active, boolean over) {
        g.fill(x, y, x + w, y + h, active ? (over ? BRASS_LIGHT : BRASS) : 0xFF4A4030);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, active ? (over ? 0xFF2E5A36 : LEATHER_LIGHT) : 0xFF2A2620);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, active ? PAPER : FADED, false);
    }

    private FormattedCharSequence fit(Component text, int w) {
        if (font.width(text) <= w) return text.getVisualOrderText();
        String s = text.getString();
        while (!s.isEmpty() && font.width(s + "…") > w) s = s.substring(0, s.length() - 1);
        return Component.literal(s + "…").getVisualOrderText();
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // --- input ---------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        if (in(mx, my, x, y, LIST_W, LIST_H)) {
            int i = (int) Math.floor((my - y + scroll) / ROW);
            if (i >= 0 && i < offers.size()) {
                selected = offers.get(i).topic();
                click();
                return true;
            }
        }
        ResearchOffer o = chosen();
        if (o != null && in(mx, my, startX, startY, startW, startH) && o.affordable() && ClientLegacy.archive().slots().size() < maxSlots) {
            PacketDistributor.sendToServer(new ResearchActionPayload(menu.containerId, ResearchActionPayload.START, o.topic()));
            click();
            return true;
        }
        List<ResearchSlot> slots = new ArrayList<>(ClientLegacy.archive().slots());
        for (int i = 0; i < slots.size() && i < maxSlots; i++) {
            int cx = leftPos + SLOT_X + SLOT_W - 12, cy = topPos + LIST_Y + i * (SLOT_H + 4) + 17;
            if (in(mx, my, cx, cy, 10, 10)) {
                PacketDistributor.sendToServer(new ResearchActionPayload(menu.containerId, ResearchActionPayload.CANCEL, slots.get(i).topic()));
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(mx, my, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H)) {
            scroll = Mth.clamp(scroll - sy * ROW, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
    }

    /** For previews: show a board without a server. */
    public void previewBoard(int slots, List<ResearchOffer> board) {
        offers = board;
        maxSlots = slots;
        selected = board.isEmpty() ? "" : board.get(0).topic();
    }
}
