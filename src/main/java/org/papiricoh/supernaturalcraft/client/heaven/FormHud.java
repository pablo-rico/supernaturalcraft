package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

/**
 * Zachariah's paperwork on the HUD (v0.18): the Heavenly Form a hunter holds ({@code HeavenFxPayload.FORM}: which cabinet,
 * how long until it is overdue) as a card of Heaven's stationery at the right edge, and APPROVED stamped across it while
 * their blows land harder ({@code APPROVED}).
 */
public final class FormHud {

    /** Ticks left before overdue at which the card starts to burn red. */
    static final int URGENT = 100;

    private static int cabinet, dueIn = -1, approved, approvedTotal = 1, shownFor;

    private FormHud() {
    }

    /** A form held (cabinet 1-4, due in {@code ticks}), or none ({@code cabinet} 0). */
    public static void form(int cabinetNumber, int ticks) {
        if (cabinetNumber <= 0) {
            cabinet = 0;
            dueIn = -1;
            return;
        }
        if (cabinet != cabinetNumber) shownFor = 0;
        cabinet = cabinetNumber;
        dueIn = Math.max(0, ticks);
    }

    public static void approved(int ticks) {
        approved = Math.max(0, ticks);
        approvedTotal = Math.max(1, ticks);
    }

    public static int cabinet() {
        return cabinet;
    }

    public static boolean isApproved() {
        return approved > 0;
    }

    public static void clear() {
        cabinet = 0;
        dueIn = -1;
        approved = 0;
    }

    static void tick() {
        if (dueIn > 0) dueIn--;
        if (approved > 0) approved--;
        shownFor++;
    }

    static void render(GuiGraphics g, float partial, int w, int h) {
        if (cabinet <= 0 && approved <= 0) return;
        int cw = 110, ch = 54, x1 = w - 8, x0 = x1 - cw, y0 = h / 2 - 50;
        float a = Mth.clamp((shownFor + partial) / 6f, 0, 1);
        if (cabinet > 0) {
            boolean urgent = dueIn >= 0 && dueIn < URGENT;
            float pulse = urgent ? 0.5f + 0.5f * Mth.sin((shownFor + partial) * 0.6f) : 0;
            HeavenGui.panel(g, HeavenGuiAtlas.FORM_CARD, x0, y0, x1, y0 + ch, HeavenGui.Style.OFFICE, a);
            if (urgent) g.fill(x0, y0, x1, y0 + ch, HeavenGui.argb(a * 0.22f * pulse, HeavenGui.DENIED));
            GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.form"), x0 + 6, y0 + 5, 0.8f,
                    HeavenGui.argb(a, 0x2A2A30), false);
            // The cabinet's numeral, large, as the form's file number.
            Component numeral = Component.literal(HeavenGui.roman(cabinet));
            if (!HeavenGui.sprite(g, HeavenGuiAtlas.numeral(cabinet), x1 - 26, y0 + 5, 16, 16, a, 0xFFFFFF)) {
                GuiDraw.centred(g, numeral, x1 - 16, y0 + 6, 1.8f, HeavenGui.argb(a, HeavenGui.CARBON), false);
            }
            GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.form_file", HeavenGui.roman(cabinet)), x0 + 6, y0 + 17,
                    0.75f, HeavenGui.argb(a, 0x4C4C58), false);
            for (int y = y0 + 27; y < y0 + ch - 12; y += 4) g.fill(x0 + 6, y, x1 - 30, y + 1, HeavenGui.argb(a * 0.25f, 0x7F9CC8));
            Component due = dueIn <= 0 ? Component.translatable("hud.supernaturalcraft.heaven.form_overdue")
                    : Component.translatable("hud.supernaturalcraft.heaven.form_due", HeavenGui.clock(dueIn));
            GuiDraw.text(g, due, x0 + 6, y0 + ch - 10, 0.75f, HeavenGui.argb(a, urgent || dueIn <= 0 ? HeavenGui.DENIED : 0x4C4C58), false);
            GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.form_weak"), x0 + 6, y0 + ch + 3, 0.7f,
                    HeavenGui.argb(a * 0.9f, 0xFFE0D0), true);
        }
        if (approved > 0) {
            // The stamp, slanted across the card (or where the card would be).
            float k = Mth.clamp((approvedTotal - approved + partial) / 4f, 0, 1);
            float fadeOut = Mth.clamp((approved - partial) / 20f, 0, 1);
            float sa = k * fadeOut;
            g.pose().pushPose();
            g.pose().translate(x0 + cw / 2f, y0 + ch / 2f + 2, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-12));
            float s = 1.6f + (1 - k) * 1.2f;
            g.pose().scale(s, s, 1);
            Component stamp = Component.translatable("hud.supernaturalcraft.heaven.approved");
            int tw = GuiDraw.width(stamp, 1f);
            if (!HeavenGui.sprite(g, HeavenGuiAtlas.APPROVED, -36, -11, 72, 22, sa, 0xFFFFFF)) {
                g.fill(-tw / 2 - 4, -7, tw / 2 + 4, 7, HeavenGui.argb(sa * 0.9f, HeavenGui.APPROVED));
                g.fill(-tw / 2 - 3, -6, tw / 2 + 3, 6, HeavenGui.argb(sa * 0.95f, 0xF4FFF2));
                g.drawString(net.minecraft.client.Minecraft.getInstance().font, stamp, -tw / 2, -4, HeavenGui.argb(sa, HeavenGui.APPROVED), false);
            }
            g.pose().popPose();
            GuiDraw.text(g, Component.translatable("hud.supernaturalcraft.heaven.approved_left", HeavenGui.clock(approved)), x0 + 6,
                    y0 + ch + (cabinet > 0 ? 13 : 3), 0.7f, HeavenGui.argb(sa, 0xB8FFB0), true);
        }
    }
}
