package org.papiricoh.supernaturalcraft.client.amara;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraBalance;

/** The Darkness closing in: a violet-black vignette and a thin meter above the hotbar. */
public class ConsumptionOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation VIGNETTE = SupernaturalCraft.asResource("textures/misc/consumption_vignette.png");
    private static float value, shown;

    public static void set(float v) {
        value = v;
    }

    public static float value() {
        return value;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        shown = Mth.lerp(0.08f, shown, value);
        float near = ClientPostFx.fallback() ? ClientPostFx.intensity(delta.getGameTimeDeltaPartialTick(false)) : 0;
        int w = g.guiWidth(), h = g.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (near > 0.02f && shown < 0.5f) {
            // The cheap stand-in for the distortion shader: her presence closes in on the edges.
            g.setColor(1f, 1f, 1f, near * 0.7f);
            g.blit(VIGNETTE, 0, 0, 0, 0, w, h, w, h);
            g.setColor(1f, 1f, 1f, 1f);
        }
        if (shown < 0.5f) {
            RenderSystem.disableBlend();
            return;
        }
        float k = shown / AmaraBalance.MAX_CONSUMPTION;
        g.setColor(1f, 1f, 1f, Math.min(1f, 0.25f + k + near * 0.3f));
        g.blit(VIGNETTE, 0, 0, 0, 0, w, h, w, h);
        g.setColor(1f, 1f, 1f, 1f);
        int x = w / 2 - 91, y = h - 32 - 3;
        g.fill(x, y, x + 182, y + 2, 0x80000000);
        int fill = Math.round(182 * k);
        int color = shown >= AmaraBalance.SLOW_AT ? 0xFFB040E0 : 0xFF6A3FA8;
        g.fill(x, y, x + fill, y + 2, color);
        for (float t : new float[]{AmaraBalance.DARKNESS_AT, AmaraBalance.SLOW_AT}) {
            int tx = x + Math.round(182 * t / AmaraBalance.MAX_CONSUMPTION);
            g.fill(tx, y - 1, tx + 1, y + 3, 0xFFE0D0FF);
        }
        RenderSystem.disableBlend();
    }
}
