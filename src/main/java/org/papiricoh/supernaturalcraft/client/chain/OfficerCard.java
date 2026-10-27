package org.papiricoh.supernaturalcraft.client.chain;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;
import org.papiricoh.supernaturalcraft.network.ChainSyncPayload.Entry;

import java.util.Locale;

import static org.papiricoh.supernaturalcraft.client.chain.ChainOverlay.*;

/**
 * An officer's title card ({@link ChainNoticePayload#TITLE}, sent when it materializes near the player): a framed card in its
 * side's metal ({@code card_<side>.png}) high on the screen, its rank medallion set in the top rim, the rank to the left of
 * the medallion and the level to the right, the name large under it and the epithet under a rule. It drops in with the
 * medallion settling into its socket, holds, and rises away. While it is up the officer bars wait ({@link #fade}); it is
 * let through camera sequences like the bosses' cards.
 */
public final class OfficerCard implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("chain_title");
    private static final int LIFE = 100, IN = 12, OUT = 20;

    private static Card card;

    private record Card(int side, int rank, Component rankName, Component name, Component epithet, int level, int[] age) {
    }

    private OfficerCard() {
    }

    public static OfficerCard layer() {
        return new OfficerCard();
    }

    static void show(ChainNoticePayload p) {
        int side = p.key().endsWith("the_fallen") ? FALLEN : ChainOverlay.side(p.faction(), "LOYAL");
        Entry e = find(p.faction(), p.rank(), p.name());
        card = new Card(side, rank(p.rank()), rankName(p.faction(), p.rank()), Component.literal(p.name()), Component.translatable(p.key()),
                e == null ? -1 : e.level(), new int[1]);
    }

    static void tick() {
        Card c = card;
        if (c == null) return;
        if (Minecraft.getInstance().level == null || ++c.age[0] >= LIFE) card = null;
    }

    static void clear() {
        card = null;
    }

    static boolean active() {
        return card != null;
    }

    /** How much of the card is up (0..1): the officer bars give way to it. */
    static float fade(float partial) {
        Card c = card;
        if (c == null) return 0;
        float t = c.age[0] + partial;
        return Mth.clamp(Math.min(t / 6f, (LIFE - t) / 10f), 0, 1);
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Card c = card;
        Minecraft mc = Minecraft.getInstance();
        if (c == null || mc.player == null || mc.options.hideGui) return;
        float t = c.age[0] + delta.getGameTimeDeltaPartialTick(false);
        float in = smooth(t / IN), out = smooth((LIFE - t) / OUT);
        float a = Math.min(in, out);
        if (a <= 0.02f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        // Small screens: the whole card shrinks to fit (it is never wider than the screen less a margin).
        float scale = Math.min(1f, (w - 12) / (float) CARD_W);
        float top = Math.max(4, Math.min(h * 0.12f, h / 2f - 24 - CARD_H * scale));
        top = Math.max(top, OfficerBar.bossBottom() + 2) + (1 - in) * -10 + (1 - out) * -6;
        g.pose().pushPose();
        g.pose().translate(w / 2f - CARD_W * scale / 2f, top, 0);
        g.pose().scale(scale, scale, 1);
        drawCard(g, c, t, a);
        g.pose().popPose();
    }

    private static void drawCard(GuiGraphics g, Card c, float t, float a) {
        int side = c.side;
        if (has(card(side))) blit(g, side, card(side), 0, 0, CARD_W, CARD_H, 0, 0, CARD_W, CARD_H, CARD_W, CARD_H, a);
        else plate(g, 16, 16, 240, CARD_H, side, a);
        // The medallion settles into its socket (centre 128, 18) from a little larger, with a flash of its side's light.
        float settle = smooth((t - 3) / 10f);
        float size = MEDALLION * (1.35f - 0.35f * settle);
        medallion(g, side, c.rank, 128 - size / 2, 18 - size / 2, Math.round(size), a * Math.min(1, t / 5f));
        ResourceLocation medallions = sheet(MEDALLIONS, MEDALLIONS_LEVIATHAN, side);
        if (settle < 1 && has(medallions)) {
            MichaelGui.glow(g, medallions, 128 - size / 2, 18 - size / 2, Math.round(size), Math.round(size),
                    c.rank * MEDALLION, row(medallions, side) * MEDALLION, MEDALLION, MEDALLION, 180, 108, (1 - settle) * a * 0.8f, accent(side));
        }
        // The rank, left of the socket; the level and its pips, right of it (both on the top band, y 20..38).
        Component rank = Component.literal(c.rankName.getString().toUpperCase(Locale.ROOT));
        centred(g, rank, 64, 25, 1f, a, accent(side));
        if (c.level >= 0) {
            centred(g, Component.translatable("gui.supernaturalcraft.chain.level", c.level), 192, 21, 1f, a, ink(side));
            pips(g, side, c.level, 192 + 2.5f * PIP, 31, a, false);
        }
        // The name, as large as fits.
        float ns = Math.min(2f, 196f / Math.max(1, GuiDraw.width(c.name, 1f)));
        float nameIn = smooth((t - 4) / 10f);
        centred(g, c.name, 128, 41 + (16 - 8 * ns) / 2f, ns, a * nameIn, ink(side));
        // A rule that opens from the middle, then the epithet.
        float rule = smooth((t - 6) / 14f) * 84;
        if (rule > 0.5f) {
            g.fill(Math.round(128 - rule), 60, Math.round(128 + rule), 61, argb(a * 0.85f, accent(side)));
            g.fill(Math.round(128 - rule * 0.6f), 61, Math.round(128 + rule * 0.6f), 62, argb(a * 0.35f, accent(side)));
        }
        centred(g, c.epithet, 128, 66, 1f, a * smooth((t - 10) / 10f), 0xD8D0C8);
    }
}
