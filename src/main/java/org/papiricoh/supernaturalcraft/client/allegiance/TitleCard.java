package org.papiricoh.supernaturalcraft.client.allegiance;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.papiricoh.supernaturalcraft.client.allegiance.AllegianceGui.*;

/**
 * A new rank's title card (after the ascension's camera): the side's card ({@code title_<side>.png}: celestial, hellfire or
 * ink) with the rank's name over it and "Angel · Rank II" under it; a cure's card says the player is human again. Drawn
 * after every HUD layer, with the screen flashes and the light of a true form, so a camera sequence does not hide it.
 */
public final class TitleCard {

    private static final int LENGTH = 110;
    private static Faction faction;
    private static int rank, delay, age = -1;
    private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

    private TitleCard() {
    }

    public static void show(Faction f, int r, int after) {
        faction = f;
        rank = r;
        delay = Math.max(0, after);
        age = 0;
    }

    public static boolean showing() {
        return age >= 0;
    }

    static void tick() {
        if (age < 0) return;
        if (delay > 0) {
            delay--;
            return;
        }
        if (++age > LENGTH) age = -1;
    }

    static void clear() {
        age = -1;
    }

    /** The card, then flashes and true-form glare: everything this package lays over the whole screen. */
    public static void renderOverlay(GuiGraphics g, float partial) {
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth(), h = g.guiHeight();
        float glare = AllegianceFx.trueFormGlare(mc, partial);
        if (glare > 0.01f) g.fill(0, 0, w, h, argb(glare * 0.85f, 0xFFFBEA));
        float flash = AllegianceFx.flashStrength(partial);
        if (flash > 0.01f) g.fill(0, 0, w, h, argb(flash * 0.6f, AllegianceFx.flashColour()));
        if (age < 0 || delay > 0 || faction == null || mc.options.hideGui && mc.screen == null && !org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector.active()) {
            return;
        }
        float t = age + partial;
        float alpha = Math.min(1, Math.min(t / 12f, (LENGTH - t) / 18f));
        if (alpha <= 0.01f) return;
        float rise = (1 - Math.min(1, t / 14f)) * 8;
        float cx = w / 2f, cy = h * 0.34f + rise;
        int tint = colour(faction);
        ResourceLocation card = titleCard(faction);
        if (has(card)) {
            int[] size = size(card);
            float scale = Math.min(w * 0.62f / size[0], h * 0.3f / size[1]);
            float dw = size[0] * scale, dh = size[1] * scale;
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, alpha);
            g.pose().pushPose();
            g.pose().translate(cx - dw / 2, cy - dh / 2, 0);
            g.pose().scale(scale, scale, 1);
            g.blit(card, 0, 0, 0, 0, size[0], size[1], size[0], size[1]);
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
        } else {
            int bw = Math.round(w * 0.5f), bh = 54;
            g.fillGradient(Math.round(cx - bw / 2f), Math.round(cy - bh / 2f), Math.round(cx + bw / 2f), Math.round(cy + bh / 2f),
                    argb(alpha * 0.0f, 0), argb(alpha * 0.65f, deep(faction)));
            g.fill(Math.round(cx - bw / 2f), Math.round(cy + bh / 2f), Math.round(cx + bw / 2f), Math.round(cy + bh / 2f) + 1, argb(alpha, tint));
        }
        Component title = Component.translatable(Ranks.titleKey(faction, rank));
        GuiDraw.centred(g, title, cx, cy - 12, 2.4f, argb(alpha, tint), true);
        Component sub = rank <= 0 ? Component.translatable("title.supernaturalcraft.allegiance.cured")
                : Component.translatable("title.supernaturalcraft.allegiance.rank", Component.translatable("title.supernaturalcraft.allegiance."
                + key(faction)), Ranks.roman(rank));
        GuiDraw.centred(g, sub, cx, cy + 14, 1.1f, argb(alpha * 0.9f, 0xE8DCC0), true);
    }

    /** A texture's pixel size (read once from the resource; 256×64 if it cannot be read). */
    private static int[] size(ResourceLocation tex) {
        return SIZES.computeIfAbsent(tex, t -> {
            var res = Minecraft.getInstance().getResourceManager().getResource(t);
            if (res.isEmpty()) return new int[]{256, 64};
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                return new int[]{img.getWidth(), img.getHeight()};
            } catch (Exception e) {
                return new int[]{256, 64};
            }
        });
    }

    static void forgetSizes() {
        SIZES.clear();
    }

    /** For previews: show at once. */
    public static void showNow(Faction f, int r) {
        show(f, r, 0);
        age = 20;
    }
}
