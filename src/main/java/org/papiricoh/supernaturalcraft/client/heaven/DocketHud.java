package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.List;

/**
 * "It was already written" on the HUD (v0.18): Zachariah's docket of the next attacks as a clipboard at the left edge, the
 * next one marked, a revision struck through with a pen line that runs across it before the new entry is written in red.
 */
public final class DocketHud {

    private static final DocketView VIEW = new DocketView();
    private static int strikeTotal = 1;

    private DocketHud() {
    }

    public static void foretell(String ids) {
        VIEW.foretell(ids);
    }

    public static void revise(int index, String replacement, int ticks) {
        strikeTotal = Math.max(1, ticks);
        VIEW.revise(index, replacement, ticks);
    }

    public static DocketView view() {
        return VIEW;
    }

    public static void clear() {
        VIEW.clear();
    }

    static void tick() {
        VIEW.tick();
    }

    static void render(GuiGraphics g, float partial, int w, int h) {
        if (VIEW.empty()) return;
        List<DocketView.Entry> entries = VIEW.entries();
        int lineH = 12, x0 = 8, y0 = h / 2 - 20 - entries.size() * lineH / 2, pw = 132, ph = 22 + entries.size() * lineH + 4;
        float a = Mth.clamp((VIEW.age() + partial) / 6f, 0, 1);
        HeavenGui.panel(g, HeavenGuiAtlas.DOCKET, x0, y0, x0 + pw, y0 + ph, HeavenGui.Style.OFFICE, a * 0.95f);
        // The clipboard's clip.
        g.fill(x0 + pw / 2 - 14, y0 - 3, x0 + pw / 2 + 14, y0 + 3, HeavenGui.argb(a, 0x8A8F99));
        GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.docket"), x0 + 6, y0 + 6, 0.75f,
                HeavenGui.argb(a, 0x2A2A30), false);
        g.fill(x0 + 6, y0 + 15, x0 + pw - 6, y0 + 16, HeavenGui.argb(a * 0.6f, HeavenGui.CARBON));
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < entries.size(); i++) {
            DocketView.Entry e = entries.get(i);
            int y = y0 + 20 + i * lineH;
            // Each line is "typed" in a little after the one above it.
            float la = a * Mth.clamp((VIEW.age() + partial - i * 3) / 5f, 0, 1);
            if (la <= 0.01f) continue;
            if (i == 0) g.fill(x0 + 3, y - 2, x0 + pw - 3, y + lineH - 3, HeavenGui.argb(la * 0.18f, HeavenGui.GOLD));
            int ink = e.revised() ? HeavenGui.DENIED : 0x2A2A30;
            Component line = Component.literal((i + 1) + ". ").append(HeavenText.attack(e.id()));
            GuiDraw.text(g, line, x0 + 8, y, 0.8f, HeavenGui.argb(la, ink), false);
            if (i == 0) GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.docket_next"), x0 + pw - 30, y, 0.65f,
                    HeavenGui.argb(la, HeavenGui.GOLD_DEEP), false);
            if (e.revised()) GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.docket_revised"), x0 + pw - 30, y,
                    0.65f, HeavenGui.argb(la, HeavenGui.DENIED), false);
            if (e.striking()) {
                float k = Mth.clamp(1 - (e.strikeLeft() - partial) / strikeTotal, 0, 1);
                int tw = Math.round(font.width(line) * 0.8f);
                float end = x0 + 7 + (tw + 2) * Mth.clamp(k * 1.6f, 0, 1);
                if (!HeavenGui.sprite(g, HeavenGuiAtlas.STRIKE, x0 + 7, y + 2, Math.round(end - x0 - 7), 3, la, 0xFFFFFF)) {
                    GuiDraw.line(g, x0 + 7, y + 3.5f, end, y + 3f, 1.4f, HeavenGui.argb(la, HeavenGui.DENIED));
                }
            }
        }
    }
}
