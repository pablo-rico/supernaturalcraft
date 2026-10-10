package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * A hunter's Heaven being written (v0.18, {@code HeavenFxPayload.PLOT_PROGRESS}): a thin bar of light at the top of the
 * screen with how much is done; at 100 it says the place is ready and fades.
 */
public final class PlotProgress {

    /** Ticks the bar stays after the last word, and after "ready". */
    static final int STALE = 200, READY = 80;

    private static int percent = -1, sinceUpdate, sinceDone = -1;
    private static float shown;

    private PlotProgress() {
    }

    public static void update(int pct) {
        int p = Mth.clamp(pct, 0, 100);
        if (p >= 100 && percent < 100 && percent >= 0) sinceDone = 0;
        if (p >= 100 && percent < 0) return;
        percent = p;
        sinceUpdate = 0;
    }

    public static int percent() {
        return percent;
    }

    public static void clear() {
        percent = -1;
        sinceDone = -1;
        shown = 0;
    }

    static void tick() {
        if (percent < 0) return;
        sinceUpdate++;
        if (sinceDone >= 0 && ++sinceDone > READY) clear();
        else if (percent < 100 && sinceUpdate > STALE) clear();
        if (percent >= 0) shown += (percent - shown) * 0.25f;
    }

    static void render(GuiGraphics g, float partial, int w, int h) {
        if (percent < 0) return;
        float a = sinceDone < 0 ? 1 : Mth.clamp(1 - (sinceDone + partial - READY + 30) / 30f, 0, 1);
        if (a <= 0.01f) return;
        int bw = Math.min(180, w - 40), x0 = (w - bw) / 2, y0 = 6;
        HeavenGui.panel(g, HeavenGuiAtlas.PLOT_BAR, x0 - 3, y0 - 3, x0 + bw + 3, y0 + 8, HeavenGui.Style.HEAVEN, a * 0.85f);
        HeavenGui.bar(g, HeavenGuiAtlas.PLOT_FILL, x0, y0, x0 + bw, y0 + 5, shown / 100f, HeavenGui.GOLD, a);
        Component text = sinceDone >= 0 || percent >= 100
                ? Component.translatable("hud.supernaturalcraft.heaven.plot_ready")
                : Component.translatable("hud.supernaturalcraft.heaven.plot_progress", Math.round(shown));
        GuiDraw.centred(g, text, w / 2f, y0 + 11, 0.85f, HeavenGui.argb(a, 0xFFF4DA), true);
    }
}
