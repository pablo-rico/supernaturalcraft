package org.papiricoh.supernaturalcraft.client.allegiance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;

/**
 * The allegiance's interface textures ({@code textures/gui/allegiance/}, {@code AllegianceAssets}) and the drawing they share
 * (HUD, wheel, title cards, the book's faction row). Every texture may still be missing: each draw has a plain stand-in.
 */
public final class AllegianceGui {

    /** Grace's gold, Corruption's black-red, the hunter's ink-brown; and their darker rims. */
    public static final int GRACE = 0xF4D77E, GRACE_DEEP = 0xA9802C, CORRUPTION = 0xB0182A, CORRUPTION_DEEP = 0x2A0A0E,
            HUNTER = 0xC9B48C, HUNTER_DEEP = 0x5A4630;

    public static final ResourceLocation RING = res(AllegianceAssets.HUD_RING), WHEEL = res(AllegianceAssets.WHEEL);

    private AllegianceGui() {
    }

    static ResourceLocation res(String path) {
        return SupernaturalCraft.asResource(path);
    }

    /** The emblem, the title card and the HUD key of a side ({@code hunter} for a human). */
    public static String key(Faction f) {
        return f == Faction.HUMAN ? "hunter" : f.getSerializedName();
    }

    public static ResourceLocation emblem(Faction f) {
        return res(AllegianceAssets.EMBLEM.formatted(key(f)));
    }

    public static ResourceLocation titleCard(Faction f) {
        return res(AllegianceAssets.TITLE_CARD.formatted(key(f)));
    }

    public static ResourceLocation icon(Power p) {
        return res(AllegianceAssets.POWER_ICON.formatted(p.id()));
    }

    public static boolean has(ResourceLocation texture) {
        return GeoGuard.exists(texture);
    }

    public static int colour(Faction f) {
        return switch (f) {
            case ANGEL -> GRACE;
            case DEMON -> CORRUPTION;
            case HUMAN -> HUNTER;
        };
    }

    public static int deep(Faction f) {
        return switch (f) {
            case ANGEL -> GRACE_DEEP;
            case DEMON -> CORRUPTION_DEEP;
            case HUMAN -> HUNTER_DEEP;
        };
    }

    public static int argb(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0, 1) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    /** {@code power.supernaturalcraft.<id>}, or the id title-cased while the server text is missing. */
    public static Component name(Power p) {
        return Component.translatableWithFallback(p.nameKey(), titleCase(p.id()));
    }

    static String titleCase(String id) {
        StringBuilder b = new StringBuilder();
        for (String w : id.split("_")) {
            if (w.isEmpty()) continue;
            if (!b.isEmpty()) b.append(' ');
            b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    /** A filled ring from angle {@code from} to {@code to} (radians, 0 = up, clockwise), as GUI quads. */
    public static void annulus(GuiGraphics g, float cx, float cy, float rIn, float rOut, float from, float to, int argb) {
        if (to <= from) return;
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int steps = Math.max(2, (int) Math.ceil((to - from) / (Mth.TWO_PI / 48)));
        for (int i = 0; i < steps; i++) {
            float a0 = from + (to - from) * i / steps, a1 = from + (to - from) * (i + 1) / steps;
            float s0 = Mth.sin(a0), c0 = -Mth.cos(a0), s1 = Mth.sin(a1), c1 = -Mth.cos(a1);
            vc.addVertex(m, cx + s0 * rOut, cy + c0 * rOut, 0).setColor(argb);
            vc.addVertex(m, cx + s0 * rIn, cy + c0 * rIn, 0).setColor(argb);
            vc.addVertex(m, cx + s1 * rIn, cy + c1 * rIn, 0).setColor(argb);
            vc.addVertex(m, cx + s1 * rOut, cy + c1 * rOut, 0).setColor(argb);
        }
        g.flush();
    }

    /** A filled disc (pie from {@code from} to {@code to}). */
    public static void pie(GuiGraphics g, float cx, float cy, float r, float from, float to, int argb) {
        annulus(g, cx, cy, 0, r, from, to, argb);
    }

    /**
     * A pie-slice of a sprite: the part of the {@code size}×{@code size} region at ({@code u}, {@code v}) of a {@code tw}×{@code th}
     * sheet between angles {@code from} and {@code to}, drawn centred on ({@code cx}, {@code cy}) at {@code drawn} pixels, tinted.
     */
    public static void spriteArc(GuiGraphics g, ResourceLocation texture, float cx, float cy, float drawn, int u, int v, int size,
                                 int tw, int th, float from, float to, int argb) {
        if (to <= from) return;
        g.flush();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        float half = drawn / 2f, uc = (u + size / 2f) / tw, vc = (v + size / 2f) / th, ur = size / 2f / tw, vr = size / 2f / th;
        int steps = Math.max(2, (int) Math.ceil((to - from) / (Mth.TWO_PI / 64)));
        for (int i = 0; i < steps; i++) {
            float a0 = from + (to - from) * i / steps, a1 = from + (to - from) * (i + 1) / steps;
            float s0 = Mth.sin(a0), c0 = -Mth.cos(a0), s1 = Mth.sin(a1), c1 = -Mth.cos(a1);
            b.addVertex(m, cx, cy, 0).setUv(uc, vc).setColor(argb);
            b.addVertex(m, cx + s1 * half, cy + c1 * half, 0).setUv(uc + s1 * ur, vc + c1 * vr).setColor(argb);
            b.addVertex(m, cx + s0 * half, cy + c0 * half, 0).setUv(uc + s0 * ur, vc + c0 * vr).setColor(argb);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /** The emblem of a side at {@code size}, centred; a coloured disc with its letter while the art is missing. */
    public static void drawEmblem(GuiGraphics g, Faction f, float cx, float cy, int size, float alpha) {
        ResourceLocation t = emblem(f);
        if (has(t)) {
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, alpha);
            g.pose().pushPose();
            g.pose().translate(cx - size / 2f, cy - size / 2f, 0);
            g.blit(t, 0, 0, size, size, 0, 0, 32, 32, 32, 32);
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
            return;
        }
        float r = size / 2f;
        pie(g, cx, cy, r, 0, Mth.TWO_PI, argb(alpha * 0.9f, deep(f)));
        pie(g, cx, cy, r - 1.5f, 0, Mth.TWO_PI, argb(alpha * 0.95f, f == Faction.DEMON ? 0x140608 : 0x1A1610));
        String letter = switch (f) {
            case ANGEL -> "†";
            case DEMON -> "♦";
            case HUMAN -> "★";
        };
        var font = Minecraft.getInstance().font;
        g.drawString(font, letter, Math.round(cx - font.width(letter) / 2f), Math.round(cy - 4), argb(alpha, colour(f)), false);
    }

    /** A power's icon, {@code size} square at (x, y); a lettered tile while the art is missing. */
    public static void drawIcon(GuiGraphics g, Power p, float x, float y, int size, float alpha) {
        ResourceLocation t = icon(p);
        if (has(t)) {
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, alpha);
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.blit(t, 0, 0, size, size, 0, 0, 24, 24, 24, 24);
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
            return;
        }
        int c = colour(p.faction);
        float r = size / 2f;
        pie(g, x + r, y + r, r, 0, Mth.TWO_PI, argb(alpha, deep(p.faction)));
        pie(g, x + r, y + r, r - 1.5f, 0, Mth.TWO_PI, argb(alpha, 0x18141C));
        String[] words = p.id().split("_");
        String ab = words.length > 1 ? "" + words[0].charAt(0) + words[1].charAt(0) : words[0].substring(0, Math.min(2, words[0].length()));
        ab = ab.toUpperCase(java.util.Locale.ROOT);
        var font = Minecraft.getInstance().font;
        g.drawString(font, ab, Math.round(x + r - font.width(ab) / 2f), Math.round(y + r - 4), argb(alpha, c), false);
    }

    /**
     * The essence ring around an emblem: the empty ring, then the full ring's arc for {@code share} (the 64×32 sheet:
     * empty left, full right), tinted by side. Procedural while the sheet is missing.
     */
    public static void drawRing(GuiGraphics g, Faction f, float cx, float cy, int size, float share, float alpha) {
        share = Mth.clamp(share, 0, 1);
        int tint = argb(alpha, colour(f));
        if (has(RING)) {
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, alpha);
            g.pose().pushPose();
            g.pose().translate(cx - size / 2f, cy - size / 2f, 0);
            g.blit(RING, 0, 0, size, size, 0, 0, 32, 32, 64, 32);
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
            spriteArc(g, RING, cx, cy, size, 32, 0, 32, 64, 32, 0, share * Mth.TWO_PI, tint);
            return;
        }
        float r = size / 2f;
        annulus(g, cx, cy, r - 3.5f, r, 0, Mth.TWO_PI, argb(alpha * 0.85f, 0x16121A));
        annulus(g, cx, cy, r - 3f, r - 0.5f, 0, share * Mth.TWO_PI, tint);
    }
}
