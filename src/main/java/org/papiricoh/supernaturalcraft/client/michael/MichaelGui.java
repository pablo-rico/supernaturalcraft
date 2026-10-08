package org.papiricoh.supernaturalcraft.client.michael;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;

/**
 * Michael's HUD textures ({@code textures/gui/michael/}, michael contract) and the drawing they share. Every texture may
 * still be missing while the art is under way: callers ask {@link #has} and draw a plain stand-in otherwise.
 */
public final class MichaelGui {

    public static final ResourceLocation BAR_FRAME = tex("bar_frame"), BAR_FILL = tex("bar_fill"), BAR_WING = tex("bar_wing"),
            FEATHER = tex("feather"), ENOCHIAN = tex("enochian"), TITLE_RAYS = tex("title_rays"), TITLE_WINGS = tex("title_wings"),
            TITLE_FLOURISH = tex("title_flourish"), YES_PANEL = tex("yes_panel"), MARK = tex("mark"), FLIGHT = tex("flight");

    /** Gold, white-blue light, and the parchment's ink. */
    public static final int GOLD = 0xFFE3A5, GOLD_DEEP = 0xC99A3A, LIGHT = 0xDCEFFF, BLUE = 0x8FD8FF, INK = 0x3A2A12;
    /** Standard Galactic: Enochian's stand-in while the glyph atlas is missing. */
    private static final ResourceLocation ALT = ResourceLocation.withDefaultNamespace("alt");

    private MichaelGui() {
    }

    private static ResourceLocation tex(String name) {
        return SupernaturalCraft.asResource("textures/gui/michael/" + name + ".png");
    }

    public static boolean has(ResourceLocation texture) {
        return GeoGuard.exists(texture);
    }

    public static int argb(float alpha, int rgb) {
        return ((int) (Math.max(0, Math.min(1, alpha)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** Draws {@code texture} (a whole sheet {@code tw}×{@code th}) region at alpha {@code a}, tinted {@code rgb}. */
    public static void blit(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                            int tw, int th, float a, int rgb) {
        RenderSystem.enableBlend();
        g.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, a);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.blit(texture, 0, 0, w, h, u, v, uw, vh, tw, th);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
    }

    /** As {@link #blit}, added onto what is under it (light). */
    public static void glow(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                            int tw, int th, float a, int rgb) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        blit(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, rgb);
        RenderSystem.defaultBlendFunc();
    }

    /** {@code text} in the Enochian stand-in letters. */
    public static Component enochian(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(ALT));
    }

    /** Roman numeral of a phase. */
    public static String roman(int n) {
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
    }
}
