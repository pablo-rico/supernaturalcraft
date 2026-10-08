package org.papiricoh.supernaturalcraft.client.michael;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.reward.michael.WingStamina;

import static org.papiricoh.supernaturalcraft.client.michael.MichaelGui.*;

/** The Seraph Wings' stamina beside the hotbar, while they carry you on Michael's Grace (hidden when full and grounded). */
public final class FlightHud implements LayeredDraw.Layer {

    private static float shown = -1;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        WingStamina s = ClientMichael.stamina();
        if (mc.player == null || mc.options.hideGui || s == null) {
            shown = -1;
            return;
        }
        boolean flying = mc.player.getAbilities().flying;
        if (!flying && s.share() >= 0.999f) return;
        float share = s.share();
        shown = shown < 0 ? share : Mth.lerp(0.3f, shown, share);
        int w = 96, x = g.guiWidth() / 2 - 91 - w - 8, y = g.guiHeight() - 14;
        int colour = s.exhausted() ? 0xC8261E : share < 0.25f ? GOLD_DEEP : LIGHT;
        if (has(FLIGHT)) {
            blit(g, FLIGHT, x, y, w, 8, 0, 0, w, 8, w, 16, 1, 0xFFFFFF);
            int fw = Math.round(w * shown);
            if (fw > 0) blit(g, FLIGHT, x, y, fw, 8, 0, 8, fw, 8, w, 16, 1, colour);
        } else {
            g.fill(x - 1, y - 1, x + w + 1, y + 6, argb(0.8f, GOLD_DEEP));
            g.fill(x, y, x + w, y + 5, 0xC0101020);
            g.fill(x, y, x + Math.round(w * shown), y + 5, argb(1, colour));
        }
        g.drawString(mc.font, Component.translatable("hud.supernaturalcraft.flight"), x, y - 10, argb(0.9f, GOLD), true);
    }
}
