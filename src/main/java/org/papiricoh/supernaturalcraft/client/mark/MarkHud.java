package org.papiricoh.supernaturalcraft.client.mark;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;

/**
 * The local bearer's Thirst (v0.21), small and to the right of the hotbar: the Mark of Cain in scar red, a thin vial beside it
 * filling from the foot as the Thirst climbs. The rune glows with {@link MarkArm#glow}; from one half (bloodlust) it beats like a
 * heart -- swelling on each thump, the vial's top pulsing -- and at full Thirst it burns hot and ragged. The red at the screen's
 * edges is the magic overlay's ({@code MagicOverlay}).
 */
public final class MarkHud implements LayeredDraw.Layer {

    static final int H = 18, VIAL_W = 3, SCAR = 0x3A0606, RED = 0xD0141E, HOT = 0xFFB070, GLASS = 0x140808;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || CameraDirector.active() || mc.player.isSpectator()) return;
        if (!MarkArm.marked(mc.player)) return;
        float thirst = MarkArm.thirst(mc.player);
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float t = mc.player.tickCount + partial;
        // To the right of the hotbar; past the off-hand slot when it sits on that side.
        int cx = g.guiWidth() / 2, x = cx + 98 + (mc.player.getMainArm() == HumanoidArm.LEFT ? 29 : 0), y = g.guiHeight() - H - 3;
        draw(g, x, y, thirst, t);
    }

    /** The rune and its vial with their top-left at (x, y) (previews call this too). */
    public static void draw(GuiGraphics g, int x, int y, float thirst, float t) {
        float glow = MarkArm.glow(thirst, t, 0);
        boolean lust = thirst >= 0.5f;
        float swell = lust ? 1 + 0.12f * (glow - 0.4f) : 1;
        float rh = H * swell, rw = rh * MarkGlyph.ASPECT;
        float rx = x + (H * MarkGlyph.ASPECT - rw) / 2, ry = y + (H - rh) / 2;
        // A dark halo, then the rune: scar under, light over at the glow.
        int halo = argb(0.35f + 0.35f * glow * thirst, 0x200404);
        g.fill(x - 2, y - 2, x + Math.round(H * MarkGlyph.ASPECT) + 2, y + H + 2, halo);
        MarkGlyph.gui(g, rx, ry, rh, 1, 1.6f, argb(0.95f, SCAR), 0);
        int hot = thirst >= 0.99f ? HOT : lerp(RED, HOT, glow * thirst);
        MarkGlyph.gui(g, rx, ry, rh, 1, 1.0f, argb(0.25f + 0.75f * glow, hot), argb(0.3f * glow, RED));
        // The vial: glass, the Thirst filling it from the foot, a line at one half.
        int vx = x + Math.round(H * MarkGlyph.ASPECT) + 4;
        g.fill(vx - 1, y - 1, vx + VIAL_W + 1, y + H + 1, argb(0.9f, 0x5A3A30));
        g.fill(vx, y, vx + VIAL_W, y + H, argb(0.9f, GLASS));
        int fill = Math.round(H * Mth.clamp(thirst, 0, 1));
        if (fill > 0) {
            g.fillGradient(vx, y + H - fill, vx + VIAL_W, y + H, argb(1, lerp(0x8A0A10, HOT, lust ? glow * 0.6f : 0)), argb(1, 0x5A0408));
            if (lust) g.fill(vx, y + H - fill, vx + VIAL_W, y + H - fill + 1, argb(0.5f + 0.5f * glow, HOT));
        }
        g.fill(vx - 1, y + H / 2, vx, y + H / 2 + 1, argb(0.9f, 0xC0A080));
    }

    static int argb(float alpha, int rgb) {
        return (int) (Mth.clamp(alpha, 0, 1) * 255) << 24 | rgb & 0xFFFFFF;
    }

    static int lerp(int a, int b, float k) {
        k = Mth.clamp(k, 0, 1);
        int ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        return Math.round(ar + ((b >> 16 & 0xFF) - ar) * k) << 16 | Math.round(ag + ((b >> 8 & 0xFF) - ag) * k) << 8
                | Math.round(ab + ((b & 0xFF) - ab) * k);
    }
}
