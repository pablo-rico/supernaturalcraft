package org.papiricoh.supernaturalcraft.client.gabriel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.Channel;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;

/**
 * TV Land's HUD textures ({@code textures/gui/gabriel/}, {@link GabrielAssets}) and the drawing they share. Any of them may
 * still be missing while the art is under way: callers ask {@link #has} and draw a plain stand-in otherwise.
 */
public final class GabrielGui {

    public static final ResourceLocation OSD = tex(GabrielAssets.OSD), STATIC = tex(GabrielAssets.STATIC),
            QUIZ_PANEL = tex(GabrielAssets.QUIZ_PANEL), BAR = tex(GabrielAssets.BAR);
    public static final ResourceLocation[] SIGNS = new ResourceLocation[GabrielAssets.SIGNS.size()];

    static {
        for (int i = 0; i < SIGNS.length; i++) SIGNS[i] = tex(GabrielAssets.SIGN.formatted(GabrielAssets.SIGNS.get(i)));
    }

    /** The OSD's green, the studio sign's red, the quiz's three platforms (RED/BLUE/YELLOW), the ad's gold. */
    public static final int OSD_GREEN = 0x5CFF6A, SIGN_RED = 0xFF3B30, GOLD = 0xFFD45A, CREAM = 0xFFF6DE;
    public static final int[] PLATFORM = {0xE8383D, 0x3A7BE8, 0xF2C230};
    /** Each channel's own colour (title cards, the bar's banner light). */
    public static final int[] CHANNEL = {0xFFB547, 0xFF4FD8, 0x5FE0C8, 0xFFF1C2};

    /** Sign geometry, quiz panel geometry, bar geometry (see the requests to the art). */
    public static final int SIGN_W = 128, SIGN_H = 32, QUIZ_W = 256, QUIZ_H = 96, BAR_W = 256, BAR_H = 32;

    private GabrielGui() {
    }

    private static ResourceLocation tex(String path) {
        return SupernaturalCraft.asResource(path);
    }

    public static boolean has(ResourceLocation texture) {
        return MichaelGui.has(texture);
    }

    public static int argb(float alpha, int rgb) {
        return MichaelGui.argb(alpha, rgb);
    }

    public static void blit(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                            int tw, int th, float a, int rgb) {
        MichaelGui.blit(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, rgb);
    }

    public static void glow(GuiGraphics g, ResourceLocation texture, float x, float y, int w, int h, float u, float v, int uw, int vh,
                            int tw, int th, float a, int rgb) {
        MichaelGui.glow(g, texture, x, y, w, h, u, v, uw, vh, tw, th, a, rgb);
    }

    /** {@code "CH 2"}, in the OSD's strip (or the font's green if it is missing), its right edge at {@code right}. */
    public static void osd(GuiGraphics g, int number, float right, float top, float scale, float a) {
        String digits = String.valueOf(number);
        if (has(OSD)) {
            float w = (16 + 4 + digits.length() * 11) * scale;
            g.pose().pushPose();
            g.pose().translate(right - w, top, 0);
            g.pose().scale(scale, scale, 1);
            // A soft dark halo first, as old sets bled their OSD.
            blit(g, OSD, 1, 1, 16, 16, 0, 0, 16, 16, 128, 16, a * 0.5f, 0x000000);
            blit(g, OSD, 0, 0, 16, 16, 0, 0, 16, 16, 128, 16, a, 0xFFFFFF);
            for (int i = 0; i < digits.length(); i++) {
                int d = digits.charAt(i) - '0';
                float x = 20 + i * 11;
                blit(g, OSD, x + 1, 1, 11, 16, 16 + d * 11, 0, 11, 16, 128, 16, a * 0.5f, 0x000000);
                blit(g, OSD, x, 0, 11, 16, 16 + d * 11, 0, 11, 16, 128, 16, a, 0xFFFFFF);
            }
            g.pose().popPose();
        } else {
            Component text = Component.literal("CH " + digits);
            float w = Minecraft.getInstance().font.width(text) * scale * 1.6f;
            org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.text(g, text, right - w, top, scale * 1.6f, argb(a, OSD_GREEN), true);
        }
    }

    public static int channelColour(Channel c) {
        return CHANNEL[c.ordinal()];
    }
}
