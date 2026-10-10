package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * A Termination Notice (v0.18, {@code HeavenFxPayload.TERMINATION}): served on you, a red-edged notice under the crosshair
 * counting down and saying how to survive it (a desk, or company within three blocks); served on someone else, a line telling
 * you to go and share it.
 */
public final class Termination {

    private static int target = -1, left, total = 1;

    private Termination() {
    }

    public static void serve(int entity, int ticks) {
        target = entity;
        left = Math.max(0, ticks);
        total = Math.max(1, ticks);
        if (ticks <= 0) target = -1;
    }

    public static void clear() {
        target = -1;
    }

    static void tick() {
        if (target < 0) return;
        if (--left <= 0) target = -1;
    }

    static void render(GuiGraphics g, float partial, int w, int h) {
        if (target < 0) return;
        Minecraft mc = Minecraft.getInstance();
        boolean me = mc.player != null && mc.player.getId() == target;
        float a = Mth.clamp((total - left + partial) / 5f, 0, 1);
        float pulse = 0.5f + 0.5f * Mth.sin((total - left + partial) * (left < 40 ? 0.9f : 0.35f));
        if (me) {
            int pw = 190, x0 = (w - pw) / 2, y0 = h / 2 + 30;
            HeavenGui.panel(g, HeavenGuiAtlas.FORM_CARD, x0, y0, x0 + pw, y0 + 38, HeavenGui.Style.OFFICE, a);
            g.fill(x0, y0, x0 + pw, y0 + 2, HeavenGui.argb(a, HeavenGui.DENIED));
            g.fill(x0, y0 + 36, x0 + pw, y0 + 38, HeavenGui.argb(a, HeavenGui.DENIED));
            g.fill(x0 + 1, y0 + 2, x0 + pw - 1, y0 + 36, HeavenGui.argb(a * 0.12f * pulse, HeavenGui.DENIED));
            GuiDraw.centred(g, Component.translatable("hud.supernaturalcraft.heaven.termination"), w / 2f, y0 + 5, 1f,
                    HeavenGui.argb(a, HeavenGui.DENIED), false);
            GuiDraw.centred(g, Component.translatable("hud.supernaturalcraft.heaven.termination_how"), w / 2f, y0 + 16, 0.7f,
                    HeavenGui.argb(a, 0x3A3A44), false);
            GuiDraw.centred(g, Component.literal(HeavenGui.clock(left)), w / 2f, y0 + 26, 0.9f, HeavenGui.argb(a, 0x2A2A30), false);
        } else if (mc.level != null) {
            Entity e = mc.level.getEntity(target);
            Component who = e == null ? Component.literal("?") : e.getDisplayName();
            GuiDraw.centred(g, Component.translatable("hud.supernaturalcraft.heaven.termination_other", who, HeavenGui.clock(left)),
                    w / 2f, h / 2f + 34, 0.85f, HeavenGui.argb(a * (0.7f + 0.3f * pulse), 0xFFB0A8), true);
        }
    }
}
