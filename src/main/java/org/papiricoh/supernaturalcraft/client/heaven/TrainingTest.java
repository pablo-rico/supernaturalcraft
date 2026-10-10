package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * Naomi's training test (v0.18, {@code HeavenFxPayload.TRAINING_TEST}): a clinical strip under the boss bar with its rule
 * ("Do not harm the kneeling") and the time left; when it ends, the verdict under the crosshair.
 */
public final class TrainingTest {

    private static int left = -1, total = 1;

    private TrainingTest() {
    }

    public static void start(int duration) {
        total = Math.max(1, duration);
        left = total;
    }

    /** Over: passed ("Unexpected result") or not. */
    public static void end(boolean passed) {
        left = -1;
        HeavenOverlay.add(new HeavenOverlay.Notice(Component.translatable(passed ? "hud.supernaturalcraft.heaven.test_passed"
                : "hud.supernaturalcraft.heaven.test_over"), passed ? HeavenGui.CYAN : HeavenGui.ALARM, 60));
    }

    public static boolean active() {
        return left >= 0;
    }

    public static void clear() {
        left = -1;
    }

    static void tick() {
        if (left > 0) left--;
    }

    static void render(GuiGraphics g, float partial, int w, int h) {
        if (left < 0) return;
        float a = Mth.clamp((total - left + partial) / 8f, 0, 1);
        int bw = 200, x0 = (w - bw) / 2, y0 = 42;
        HeavenGui.panel(g, HeavenGuiAtlas.QTE_FRAME, x0, y0, x0 + bw, y0 + 30, HeavenGui.Style.CLINIC, a * 0.92f);
        GuiDraw.centred(g, Component.translatable("hud.supernaturalcraft.heaven.test"), w / 2f, y0 + 4, 1f,
                HeavenGui.argb(a, 0x1E3A44), false);
        GuiDraw.centred(g, Component.translatable("hud.supernaturalcraft.heaven.test_rule"), w / 2f, y0 + 14, 0.75f,
                HeavenGui.argb(a, 0x3D6C78), false);
        float f = Mth.clamp((left - partial) / total, 0, 1);
        boolean late = left < 60 && (left / 4) % 2 == 0;
        HeavenGui.bar(g, HeavenGuiAtlas.QTE_FILL, x0 + 6, y0 + 24, x0 + bw - 6, y0 + 27, f, late ? HeavenGui.ALARM : HeavenGui.CYAN, a);
    }
}
