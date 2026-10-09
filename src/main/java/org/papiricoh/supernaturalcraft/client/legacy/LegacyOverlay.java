package org.papiricoh.supernaturalcraft.client.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * A new rank's title card (v0.17): the order's mark (the eye in the Aquarian star) in brass, "The Men of Letters" over it and
 * the rank's name under it, fading in and out over the top third of the screen.
 */
public final class LegacyOverlay {

    private static final ResourceLocation EMBLEM = SupernaturalCraft.asResource("textures/block/men_of_letters_emblem.png");
    private static final int LENGTH = 120;
    private static int rank, age = -1;

    private LegacyOverlay() {
    }

    public static void rankUp(int r) {
        rank = r;
        age = 0;
    }

    /** For previews: show at once, past the fade-in. */
    public static void showNow(int r) {
        rank = r;
        age = 24;
    }

    public static void tick() {
        if (age >= 0 && ++age > LENGTH) age = -1;
    }

    public static void clear() {
        age = -1;
    }

    public static void render(GuiGraphics g, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (age < 0 || mc.options.hideGui && mc.screen == null) return;
        float t = age + partial;
        float alpha = Math.min(1, Math.min(t / 14f, (LENGTH - t) / 20f));
        if (alpha <= 0.02f) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float cx = w / 2f, cy = h * 0.3f + (1 - Math.min(1, t / 16f)) * 6;
        int bw = Math.min(w - 20, 300), bh = 70;
        int x0 = Math.round(cx - bw / 2f), y0 = Math.round(cy - bh / 2f);
        g.fillGradient(x0, y0, x0 + bw, y0 + bh, argb(alpha * 0.82f, 0x1A0F08), argb(alpha * 0.82f, 0x2A190D));
        g.fill(x0, y0, x0 + bw, y0 + 1, argb(alpha, 0xB08B36));
        g.fill(x0, y0 + bh - 1, x0 + bw, y0 + bh, argb(alpha, 0xB08B36));
        RenderSystem.enableBlend();
        g.setColor(1, 1, 1, alpha);
        int s = 48;
        g.blit(EMBLEM, x0 + 12, y0 + (bh - s) / 2, 0, 0, s, s, s, s);
        g.blit(EMBLEM, x0 + bw - 12 - s, y0 + (bh - s) / 2, 0, 0, s, s, s, s);
        g.setColor(1, 1, 1, 1);
        GuiDraw.centred(g, Component.translatable("title.supernaturalcraft.legacy.order"), cx, y0 + 10, 1.0f, argb(alpha * 0.9f, 0xD0AE55), true);
        GuiDraw.centred(g, LegacyText.rank(rank), cx, y0 + 24, 2.0f, argb(alpha, 0xF3E7C8), true);
        GuiDraw.centred(g, Component.translatable("title.supernaturalcraft.legacy.rank", LegacyText.roman(rank)), cx, y0 + 50, 1.0f,
                argb(alpha * 0.85f, 0x8FD08A), true);
    }

    static int argb(float a, int rgb) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }
}
