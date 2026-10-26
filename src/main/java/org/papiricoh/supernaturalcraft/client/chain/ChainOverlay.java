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
import org.papiricoh.supernaturalcraft.network.ChainNoticePayload;

import java.util.Locale;

/**
 * The Chain of Command's title card: when an officer materializes near the player, a dark band across the screen with its rank,
 * its name and its epithet, inked in its side's colour (blood for the Pit, pale gold for the Host, ash for the Fallen). It is
 * let through camera sequences like the bosses' cards.
 */
public final class ChainOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("chain_title");
    private static final int LIFE = 80, FADE_IN = 8, FADE_OUT = 20;

    private static Card card;

    private record Card(Component rank, Component name, Component epithet, int rgb, int[] age) {
    }

    static void show(ChainNoticePayload p) {
        boolean fallen = p.key().endsWith("the_fallen");
        int rgb = fallen ? 0xB8B0A8 : "angel".equals(p.faction()) ? 0xF4E3A8 : 0xD8352A;
        card = new Card(Component.translatable("chain.supernaturalcraft.rank." + p.rank().toLowerCase(Locale.ROOT)),
                Component.literal(p.name()), Component.translatable(p.key()), rgb, new int[1]);
    }

    static void tick() {
        Card c = card;
        if (c == null) return;
        if (Minecraft.getInstance().level == null || ++c.age[0] >= LIFE) card = null;
    }

    static void clear() {
        card = null;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Card c = card;
        if (c == null || Minecraft.getInstance().player == null) return;
        float t = c.age[0] + delta.getGameTimeDeltaPartialTick(false);
        float a = Mth.clamp(Math.min(t / FADE_IN, (LIFE - t) / FADE_OUT), 0, 1);
        if (a <= 0.01f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float cy = Math.max(h * 0.28f, 70);
        int band = 50;
        for (int i = 0; i < band; i += 2) {
            float k = 1 - Math.abs(i - band / 2f) / (band / 2f);
            g.fill(0, (int) (cy - band / 2f + i), w, (int) (cy - band / 2f + i + 2), argb(a * 0.7f * k, 0x0A0606));
        }
        float rule = Math.min(1, t / 14f) * w * 0.22f;
        g.fill((int) (w / 2f - rule), (int) cy - 13, (int) (w / 2f + rule), (int) cy - 12, argb(a * 0.8f, c.rgb()));
        GuiDraw.centred(g, c.rank(), w / 2f, cy - 22, 1f, argb(a * 0.9f, 0xE8E0D8), true);
        float scale = Math.min(2.2f, (w - 40) / (float) Math.max(1, GuiDraw.width(c.name(), 1f)));
        GuiDraw.centred(g, c.name(), w / 2f, cy - 8, scale, argb(a, c.rgb()), true);
        GuiDraw.centred(g, c.epithet(), w / 2f, cy + 12, 1f, argb(a * 0.85f, 0xCFC6BC), true);
    }

    private static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
