package org.papiricoh.supernaturalcraft.crossroads.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.network.DealChoicePayload;
import org.papiricoh.supernaturalcraft.network.DealOfferPayload;

import java.util.List;

/**
 * The demon's offer, written on the journal's parchment: a smug line, the wishes it will grant
 * with their terms in days, and the fine print of the one you point at. Pick one and seal it, or
 * walk away. A wild bargain (v0.18, a box buried at a natural crossroads) comes in a bone-white frame, titled "A Wild Bargain",
 * with "Bind my soul" already ticked.
 */
public class DealScreen extends Screen {

    /** How many smug lines there are in the lang file ({@code screen.supernaturalcraft.deal.line.N}). */
    public static final int LINES = 6;
    /** The wild demon's own lines ({@code screen.supernaturalcraft.deal.wild.line.N}). */
    public static final int WILD_LINES = 4;
    private static final ResourceLocation BG = SupernaturalCraft.asResource("textures/gui/journal.png");
    private static final int W = 256, H = 196, INK = 0x3B2A1A, FADED = 0x7A6448, BLOOD = 0x8A1414;
    private static final int LIST_TOP = 52, ROW = 14, DESC_TOP = 140, SOUL_TOP = 160;
    /** The wild bargain's frame: bone, and the dirt of the road in its seams. */
    private static final int BONE = 0xFFE6DDC6, BONE_SHADOW = 0xFFA89C82, ROAD = 0xFF4A3A28;

    private final DealOfferPayload offer;
    private final Component line;
    private int left, top;
    private int selected = -1;
    private Button seal;
    /** "Bind my soul" (v0.13, only when the offer carries the clause): die to the hounds and rise a demon. */
    private boolean bindSoul;

    public DealScreen(DealOfferPayload offer, int lineIndex) {
        super(Component.translatable(offer.wild() ? "screen.supernaturalcraft.deal.wild" : "screen.supernaturalcraft.deal"));
        this.offer = offer;
        this.line = offer.wild() ? Component.translatable("screen.supernaturalcraft.deal.wild.line." + Math.floorMod(lineIndex, WILD_LINES))
                : Component.translatable("screen.supernaturalcraft.deal.line." + Math.floorMod(lineIndex, LINES));
        this.bindSoul = offer.soulClause() && DealTerms.soulClausePreTicked(offer.wild());
    }

    private int count() {
        return Math.min(offer.wishes().size(), Math.min(offer.args().size(), offer.days().size()));
    }

    private String key(int i) {
        DealTerms.Wish wish = DealTerms.Wish.byId(offer.wishes().get(i));
        return wish == null ? DealTerms.keyOf(offer.wishes().get(i) + "." + offer.args().get(i)) : wish.key(offer.args().get(i));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        seal = addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.deal.seal"), b -> choose())
                .bounds(left + 24, top + 172, 96, 18).build());
        seal.active = selected >= 0;
        addRenderableWidget(Button.builder(Component.translatable("screen.supernaturalcraft.deal.walk_away"), b -> onClose())
                .bounds(left + W - 120, top + 172, 96, 18).build());
    }

    private void choose() {
        if (selected < 0 || selected >= count()) return;
        PacketDistributor.sendToServer(new DealChoicePayload(offer.entityId(), offer.wishes().get(selected), offer.args().get(selected),
                offer.soulClause() && bindSoul));
        minecraft.setScreen(null);
    }

    @Override
    public void tick() {
        super.tick();
        // The demon left (or you did): the offer is off the table.
        Entity demon = minecraft.level == null ? null : minecraft.level.getEntity(offer.entityId());
        if (demon == null || !demon.isAlive() || minecraft.player == null || minecraft.player.distanceToSqr(demon) > 10 * 10) onClose();
    }

    private int rowAt(double mx, double my) {
        if (mx < left + 14 || mx > left + W - 14) return -1;
        int i = (int) Math.floor((my - (top + LIST_TOP)) / ROW);
        return my >= top + LIST_TOP && i >= 0 && i < count() ? i : -1;
    }

    private boolean overSoul(double mx, double my) {
        return offer.soulClause() && mx >= left + 16 && mx <= left + 16 + 12 + font.width(soulLabel()) && my >= top + SOUL_TOP - 1
                && my <= top + SOUL_TOP + 9;
    }

    private Component soulLabel() {
        return Component.translatable("screen.supernaturalcraft.deal.bind_soul");
    }

    /** For previews. */
    public void tickSoul(boolean on) {
        bindSoul = on;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overSoul(mx, my)) {
            bindSoul = !bindSoul;
            return true;
        }
        int row = rowAt(mx, my);
        if (button == 0 && row >= 0) {
            selected = row;
            seal.active = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        if (offer.wild()) frame(g);
        g.blit(BG, left, top, 0, 0, W, H, 256, 256);
    }

    /** Bone-white slats round the parchment, knotted at the corners. */
    private void frame(GuiGraphics g) {
        int x0 = left - 6, y0 = top - 6, x1 = left + W + 6, y1 = top + H + 6;
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, ROAD);
        g.fill(x0, y0, x1, y1, BONE_SHADOW);
        g.fill(x0, y0, x1 - 1, y1 - 1, BONE);
        g.fill(x0 + 4, y0 + 4, x1 - 4, y1 - 4, ROAD);
        for (int[] c : new int[][]{{x0 - 3, y0 - 3}, {x1 - 7, y0 - 3}, {x0 - 3, y1 - 7}, {x1 - 7, y1 - 7}}) {
            g.fill(c[0], c[1], c[0] + 10, c[1] + 10, ROAD);
            g.fill(c[0] + 1, c[1] + 1, c[0] + 9, c[1] + 9, BONE);
            g.fill(c[0] + 4, c[1] + 2, c[0] + 6, c[1] + 8, BONE_SHADOW);
            g.fill(c[0] + 2, c[1] + 4, c[0] + 8, c[1] + 6, BONE_SHADOW);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 10, INK, false);
        int y = top + 24;
        for (FormattedCharSequence l : font.split(line.copy().withStyle(s -> s.withItalic(true)), W - 32)) {
            if (y > top + 44) break;
            g.drawString(font, l, left + 16, y, BLOOD, false);
            y += 10;
        }
        int hover = rowAt(mx, my);
        for (int i = 0; i < count(); i++) {
            int ry = top + LIST_TOP + i * ROW;
            if (i == selected) g.fill(left + 12, ry - 2, left + W - 12, ry + ROW - 3, 0x40801010);
            else if (i == hover) g.fill(left + 12, ry - 2, left + W - 12, ry + ROW - 3, 0x20402010);
            Component name = Component.translatable(key(i));
            g.drawString(font, name, left + 16, ry, INK, false);
            Component days = Component.translatable("screen.supernaturalcraft.deal.days", offer.days().get(i));
            g.drawString(font, days, left + W - 16 - font.width(days), ry, BLOOD, false);
        }
        if (count() == 0) {
            Component none = Component.translatable("screen.supernaturalcraft.deal.nothing");
            g.drawString(font, none, left + (W - font.width(none)) / 2, top + LIST_TOP, FADED, false);
        }
        int shown = selected >= 0 ? selected : hover;
        if (shown >= 0) {
            List<FormattedCharSequence> desc = font.split(FormattedText.of(Component.translatable(key(shown) + ".desc").getString()), W - 32);
            int dy = top + DESC_TOP;
            for (FormattedCharSequence l : desc) {
                if (dy > top + DESC_TOP + (offer.soulClause() ? 10 : 20)) break;
                g.drawString(font, l, left + 16, dy, FADED, false);
                dy += 10;
            }
        } else {
            Component hint = Component.translatable("screen.supernaturalcraft.deal.hint");
            g.drawString(font, hint, left + (W - font.width(hint)) / 2, top + DESC_TOP, FADED, false);
        }
        if (offer.soulClause()) {
            // The clause in the margin: a box to tick in blood.
            int bx = left + 16, by = top + SOUL_TOP;
            boolean hot = overSoul(mx, my);
            g.fill(bx, by, bx + 9, by + 9, hot ? BLOOD : INK);
            g.fill(bx + 1, by + 1, bx + 8, by + 8, 0xFFE9DCC0);
            if (bindSoul) g.drawString(font, "x", bx + 2, by, BLOOD, false);
            g.drawString(font, soulLabel(), bx + 13, by + 1, bindSoul || hot ? BLOOD : INK, false);
            if (hot) g.renderTooltip(font, font.split(Component.translatable("screen.supernaturalcraft.deal.bind_soul.desc"), 200), mx, my);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
