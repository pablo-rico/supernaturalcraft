package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Inside a staged memory (v0.18): the scene's tint over Heaven's sky and fog, faded in as the visitor steps through the veil
 * and back out as they leave ({@code HeavenFxPayload.MEMORY_ENTER}/{@code MEMORY_LEAVE}). {@link HeavenSky} and the fog read
 * {@link #strength}; the HUD adds a soft vignette in the same colour.
 */
public final class MemoryTint {

    private static int rgb = 0xFFFFFF;
    private static float alphaOfTint = 1f;
    /** 0-1 now and a tick ago; where it heads and how fast. */
    private static float value, previous, target;
    private static float step = 0.05f;

    private MemoryTint() {
    }

    /** Into a memory: its ARGB tint (alpha = how strongly), in over {@code fade} ticks. */
    public static void enter(int argb, int fade) {
        rgb = argb & 0xFFFFFF;
        int a = argb >>> 24;
        alphaOfTint = a == 0 ? 1f : a / 255f;
        target = 1;
        step = 1f / Math.max(1, fade);
    }

    /** Out of it, over {@code fade} ticks. */
    public static void leave(int fade) {
        target = 0;
        step = 1f / Math.max(1, fade);
    }

    public static void clear() {
        value = previous = target = 0;
    }

    static void tick() {
        previous = value;
        if (value < target) value = Math.min(target, value + step);
        else if (value > target) value = Math.max(target, value - step);
    }

    /** How much of the tint shows (0 = none), eased. */
    public static float strength(float partial) {
        float v = Mth.lerp(partial, previous, value);
        return v * v * (3 - 2 * v) * alphaOfTint;
    }

    public static int rgb() {
        return rgb;
    }

    public static boolean active() {
        return value > 0 || target > 0;
    }

    /** {@code base} pulled toward the tint by {@code k}: r, g, b in 0-1. */
    public static float[] mix(float r, float g, float b, float k) {
        float tr = ((rgb >> 16) & 255) / 255f, tg = ((rgb >> 8) & 255) / 255f, tb = (rgb & 255) / 255f;
        return new float[]{Mth.lerp(k, r, tr), Mth.lerp(k, g, tg), Mth.lerp(k, b, tb)};
    }

    /** The edges of the screen, softly, in the memory's colour: remembering, not seeing. */
    static void renderVignette(GuiGraphics g, float partial, int w, int h) {
        float k = strength(partial);
        if (k <= 0.01f || Minecraft.getInstance().options.hideGui && k < 0.02f) return;
        int steps = 10;
        for (int i = 0; i < steps; i++) {
            float a = k * 0.05f * (steps - i) / steps;
            int ix = w * i / 60, iy = h * i / 60;
            g.fill(0, iy, w, iy + h / 60 + 1, HeavenGui.argb(a, rgb));
            g.fill(0, h - iy - h / 60 - 1, w, h - iy, HeavenGui.argb(a, rgb));
            g.fill(ix, 0, ix + w / 60 + 1, h, HeavenGui.argb(a, rgb));
            g.fill(w - ix - w / 60 - 1, 0, w - ix, h, HeavenGui.argb(a, rgb));
        }
        g.fill(0, 0, w, h, HeavenGui.argb(k * 0.05f, rgb));
    }
}
