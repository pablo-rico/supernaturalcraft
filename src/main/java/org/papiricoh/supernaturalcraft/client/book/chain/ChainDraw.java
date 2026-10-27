package org.papiricoh.supernaturalcraft.client.book.chain;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.UUID;

/**
 * Drawing the Chain of Command's v0.27 pieces share (the officer's record, the front, the talk screen and the duel banner):
 * tiled nine-slices, curves and ring sectors as GUI quads, a hunter's face and an officer's stand-in in a window.
 */
public final class ChainDraw {

    /** {@code tools/artgen/chain_book_art.py} ({@code modal}, {@code dial}) and {@code chain_gui_art.py} ({@code portraits}). */
    public static final ResourceLocation MODAL = SupernaturalCraft.asResource("textures/gui/book/chain_modal.png");
    public static final ResourceLocation DIAL = SupernaturalCraft.asResource("textures/gui/book/chain_dial.png");
    public static final ResourceLocation PORTRAIT = SupernaturalCraft.asResource("textures/gui/book/chain_portrait.png");
    public static final ResourceLocation MEDALLIONS = SupernaturalCraft.asResource("textures/gui/chain/medallions.png"),
            MEDALLIONS_LEVIATHAN = SupernaturalCraft.asResource("textures/gui/chain/medallions_leviathan.png");

    private ChainDraw() {
    }

    /**
     * A nine-slice of the {@code w}×{@code h} region at ({@code u}, {@code v}) of a {@code sw}×{@code sh} sheet, drawn at
     * {@code dw}×{@code dh}: the corners as they are, the edges and the middle tiled (never stretched, so studs and grain keep
     * their pixels).
     */
    public static void nine(GuiGraphics g, ResourceLocation tex, int sw, int sh, int u, int v, int w, int h, int c,
                            int x, int y, int dw, int dh) {
        int mw = w - 2 * c, mh = h - 2 * c, iw = dw - 2 * c, ih = dh - 2 * c;
        RenderSystem.enableBlend();
        g.blit(tex, x, y, u, v, c, c, sw, sh);
        g.blit(tex, x + dw - c, y, u + w - c, v, c, c, sw, sh);
        g.blit(tex, x, y + dh - c, u, v + h - c, c, c, sw, sh);
        g.blit(tex, x + dw - c, y + dh - c, u + w - c, v + h - c, c, c, sw, sh);
        for (int ox = 0; ox < iw; ox += mw) {
            int cw = Math.min(mw, iw - ox);
            g.blit(tex, x + c + ox, y, u + c, v, cw, c, sw, sh);
            g.blit(tex, x + c + ox, y + dh - c, u + c, v + h - c, cw, c, sw, sh);
            for (int oy = 0; oy < ih; oy += mh) {
                g.blit(tex, x + c + ox, y + c + oy, u + c, v + c, cw, Math.min(mh, ih - oy), sw, sh);
            }
        }
        for (int oy = 0; oy < ih; oy += mh) {
            int ch = Math.min(mh, ih - oy);
            g.blit(tex, x, y + c + oy, u, v + c, c, ch, sw, sh);
            g.blit(tex, x + dw - c, y + c + oy, u + w - c, v + c, c, ch, sw, sh);
        }
    }

    /** A three-piece strip: {@code cap} pixels at each end as they are, the middle tiled to {@code dw}. */
    public static void strip(GuiGraphics g, ResourceLocation tex, int sw, int sh, int u, int v, int w, int h, int cap,
                             int x, int y, int dw) {
        RenderSystem.enableBlend();
        g.blit(tex, x, y, u, v, cap, h, sw, sh);
        g.blit(tex, x + dw - cap, y, u + w - cap, v, cap, h, sw, sh);
        int mw = w - 2 * cap;
        for (int ox = 0; ox < dw - 2 * cap; ox += mw) {
            g.blit(tex, x + cap + ox, y, u + cap, v, Math.min(mw, dw - 2 * cap - ox), h, sw, sh);
        }
    }

    /**
     * A gently bowed stroke from (x0, y0) to (x1, y1), {@code bend} of its length to the side, {@code width} wide; in one
     * batch.
     */
    public static void curve(GuiGraphics g, float x0, float y0, float x1, float y1, float bend, float width, int argb) {
        float dx = x1 - x0, dy = y1 - y0, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1e-3f) return;
        float cx = (x0 + x1) / 2 - dy / len * len * bend, cy = (y0 + y1) / 2 + dx / len * len * bend;
        int steps = Math.max(4, Math.min(24, (int) (len / 6)));
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        float px = x0, py = y0;
        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps, a = (1 - t) * (1 - t), b = 2 * (1 - t) * t, c = t * t;
            float qx = a * x0 + b * cx + c * x1, qy = a * y0 + b * cy + c * y1;
            quadLine(vc, m, px, py, qx, qy, width, argb);
            px = qx;
            py = qy;
        }
        g.flush();
    }

    /** The point halfway along {@link #curve}. */
    public static float[] curveMid(float x0, float y0, float x1, float y1, float bend) {
        float dx = x1 - x0, dy = y1 - y0;
        float cx = (x0 + x1) / 2 - dy * bend, cy = (y0 + y1) / 2 + dx * bend;
        return new float[]{0.25f * x0 + 0.5f * cx + 0.25f * x1, 0.25f * y0 + 0.5f * cy + 0.25f * y1};
    }

    private static void quadLine(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0, dy = y1 - y0, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        // Overlap the joints a little so a bowed line has no gaps.
        float ex = dx / len * width * 0.25f, ey = dy / len * width * 0.25f;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        float ax = x0 - ex, ay = y0 - ey, bx = x1 + ex, by = y1 + ey;
        vc.addVertex(m, ax + nx, ay + ny, 0).setColor(argb);
        vc.addVertex(m, ax - nx, ay - ny, 0).setColor(argb);
        vc.addVertex(m, bx - nx, by - ny, 0).setColor(argb);
        vc.addVertex(m, bx + nx, by + ny, 0).setColor(argb);
        vc.addVertex(m, bx + nx, by + ny, 0).setColor(argb);
        vc.addVertex(m, bx - nx, by - ny, 0).setColor(argb);
        vc.addVertex(m, ax - nx, ay - ny, 0).setColor(argb);
        vc.addVertex(m, ax + nx, ay + ny, 0).setColor(argb);
    }

    /**
     * A ring sector between radii {@code rIn} and {@code rOut}, from bearing {@code a0} to {@code a1} in the world's own
     * sense ({@code atan2(z, x)}: 0 east, a quarter turn south), on a map with north up.
     */
    public static void sector(GuiGraphics g, float cx, float cy, float rIn, float rOut, double a0, double a1, int argb) {
        VertexConsumer vc = g.bufferSource().getBuffer(RenderType.gui());
        Matrix4f m = g.pose().last().pose();
        int steps = Math.max(2, (int) Math.ceil((a1 - a0) / (Math.PI / 48)));
        for (int i = 0; i < steps; i++) {
            double b0 = a0 + (a1 - a0) * i / steps, b1 = a0 + (a1 - a0) * (i + 1) / steps;
            float c0 = (float) Math.cos(b0), s0 = (float) Math.sin(b0), c1 = (float) Math.cos(b1), s1 = (float) Math.sin(b1);
            vc.addVertex(m, cx + c0 * rIn, cy + s0 * rIn, 0).setColor(argb);
            vc.addVertex(m, cx + c0 * rOut, cy + s0 * rOut, 0).setColor(argb);
            vc.addVertex(m, cx + c1 * rOut, cy + s1 * rOut, 0).setColor(argb);
            vc.addVertex(m, cx + c1 * rIn, cy + s1 * rIn, 0).setColor(argb);
            // Both windings, so it never culls away.
            vc.addVertex(m, cx + c1 * rIn, cy + s1 * rIn, 0).setColor(argb);
            vc.addVertex(m, cx + c1 * rOut, cy + s1 * rOut, 0).setColor(argb);
            vc.addVertex(m, cx + c0 * rOut, cy + s0 * rOut, 0).setColor(argb);
            vc.addVertex(m, cx + c0 * rIn, cy + s0 * rIn, 0).setColor(argb);
        }
        g.flush();
    }

    /** A hunter's skin: theirs if this client knows them, else the default one for their UUID. */
    public static PlayerSkin skin(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            var conn = Minecraft.getInstance().getConnection();
            PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
            return info != null ? info.getSkin() : DefaultPlayerSkin.get(id);
        } catch (IllegalArgumentException e) {
            return DefaultPlayerSkin.get(new UUID(0, 0));
        }
    }

    /** A hunter's face (and hat), {@code size} pixels square. */
    public static void face(GuiGraphics g, String uuid, int x, int y, int size) {
        RenderSystem.enableBlend();
        PlayerFaceRenderer.draw(g, skin(uuid), x, y, size);
    }

    /**
     * An officer's stand-in, posed, its point at {@code anchor} of its height at ({@code x}, {@code y}) and {@code scale}
     * GUI pixels a block. The caller clips.
     */
    public static void puppet(GuiGraphics g, LivingEntity puppet, float x, float y, float scale, float anchor, float yaw, float pitch) {
        puppet.yBodyRot = puppet.yBodyRotO = 180 + yaw * 0.5f;
        puppet.setYRot(180 + yaw);
        puppet.yRotO = puppet.getYRot();
        puppet.yHeadRot = puppet.yHeadRotO = 180 + yaw;
        puppet.setXRot(pitch);
        puppet.xRotO = pitch;
        InventoryScreen.renderEntityInInventory(g, x, y, scale, new Vector3f(0, puppet.getBbHeight() * anchor, 0),
                new Quaternionf().rotateZ((float) Math.PI), null, puppet);
    }

    /** The opaque rank medallion of the HUD ({@code medallions.png}): side 0 Pit, 1 Host, 2 Fallen; rank 0..4. */
    public static void medallion(GuiGraphics g, int side, int rank, int x, int y, int size) {
        RenderSystem.enableBlend();
        if (side == ChainAtlas.LEVIATHAN) g.blit(MEDALLIONS_LEVIATHAN, x, y, size, size, rank * 36, 0, 36, 36, 180, 108);
        else g.blit(MEDALLIONS, x, y, size, size, rank * 36, side * 36, 36, 36, 180, 108);
    }

    public static int argb(float alpha, int rgb) {
        return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
