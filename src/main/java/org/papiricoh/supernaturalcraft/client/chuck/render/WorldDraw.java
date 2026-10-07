package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Small untextured shapes and text in world space for the Author's fight: frames, ink bubbles, crystals, crack
 * strokes, words. Colour only ({@link RenderType#debugQuads()}: translucent, both faces, unlit, so full bright).
 */
public final class WorldDraw {

    private WorldDraw() {
    }

    public static VertexConsumer quads(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.debugQuads());
    }

    public static void quad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int argb) {
        int al = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, bl = argb & 0xFF;
        vc.addVertex(m, a.x, a.y, a.z).setColor(r, g, bl, al);
        vc.addVertex(m, b.x, b.y, b.z).setColor(r, g, bl, al);
        vc.addVertex(m, c.x, c.y, c.z).setColor(r, g, bl, al);
        vc.addVertex(m, d.x, d.y, d.z).setColor(r, g, bl, al);
    }

    /** A flat stroke from {@code a} to {@code b}, {@code width} wide, lying across {@code side} (a unit vector). */
    public static void stroke(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f side, float width, int argb) {
        Vector3f s = new Vector3f(side).mul(width / 2);
        quad(vc, m, new Vector3f(a).add(s), new Vector3f(a).sub(s), new Vector3f(b).sub(s), new Vector3f(b).add(s), argb);
    }

    /** A horizontal stroke on the ground (y fixed), from (x0,z0) to (x1,z1). */
    public static void groundStroke(VertexConsumer vc, Matrix4f m, float x0, float z0, float x1, float z1, float y, float width, int argb) {
        float dx = x1 - x0, dz = z1 - z0, len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 1e-4f) return;
        Vector3f side = new Vector3f(-dz / len, 0, dx / len);
        stroke(vc, m, new Vector3f(x0, y, z0), new Vector3f(x1, y, z1), side, width, argb);
    }

    /** A sphere of {@code radius}, its surface rippling by {@code wobble} (0 = round), as lat-long quads. */
    public static void sphere(VertexConsumer vc, Matrix4f m, float radius, float wobble, float time, int argb) {
        int lat = 10, lon = 16;
        for (int i = 0; i < lat; i++) {
            float t0 = Mth.PI * i / lat, t1 = Mth.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float p0 = Mth.TWO_PI * j / lon, p1 = Mth.TWO_PI * (j + 1) / lon;
                quad(vc, m, sp(radius, t0, p0, wobble, time), sp(radius, t1, p0, wobble, time), sp(radius, t1, p1, wobble, time),
                        sp(radius, t0, p1, wobble, time), argb);
            }
        }
    }

    private static Vector3f sp(float r, float theta, float phi, float wobble, float time) {
        float k = r * (1 + wobble * Mth.sin(theta * 3 + time * 2.1f) * Mth.cos(phi * 2 + time * 1.3f));
        return new Vector3f(k * Mth.sin(theta) * Mth.cos(phi), k * Mth.cos(theta), k * Mth.sin(theta) * Mth.sin(phi));
    }

    /** An octahedron (a cut crystal), {@code rx} wide and {@code ry} tall, centred on the origin. */
    public static void crystal(VertexConsumer vc, Matrix4f m, float rx, float ry, int argb) {
        Vector3f top = new Vector3f(0, ry, 0), bottom = new Vector3f(0, -ry, 0);
        Vector3f[] ring = {new Vector3f(rx, 0, 0), new Vector3f(0, 0, rx), new Vector3f(-rx, 0, 0), new Vector3f(0, 0, -rx)};
        for (int i = 0; i < 4; i++) {
            Vector3f a = ring[i], b = ring[(i + 1) % 4];
            quad(vc, m, top, a, b, top, argb);
            quad(vc, m, bottom, b, a, bottom, argb);
        }
    }

    /**
     * Jagged crack strokes radiating from the origin in the plane spanned by {@code u} and {@code v}: {@code count}
     * branches, {@code reach} long, deterministic for a {@code seed}.
     */
    public static void cracks(VertexConsumer vc, Matrix4f m, Vector3f u, Vector3f v, Vector3f normalNudge, int count, float reach, float width,
                              long seed, int argb) {
        java.util.Random rnd = new java.util.Random(seed);
        for (int i = 0; i < count; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI;
            float x = 0, y = 0;
            int steps = 3 + rnd.nextInt(3);
            for (int s = 0; s < steps; s++) {
                a += (rnd.nextFloat() - 0.5f) * 1.1f;
                float len = reach / steps * (0.7f + rnd.nextFloat() * 0.6f);
                float nx = x + Mth.cos(a) * len, ny = y + Mth.sin(a) * len;
                Vector3f p0 = new Vector3f(u).mul(x).add(new Vector3f(v).mul(y)).add(normalNudge);
                Vector3f p1 = new Vector3f(u).mul(nx).add(new Vector3f(v).mul(ny)).add(normalNudge);
                Vector3f dir = new Vector3f(p1).sub(p0);
                Vector3f side = new Vector3f(u).mul(-Mth.sin(a)).add(new Vector3f(v).mul(Mth.cos(a)));
                if (dir.lengthSquared() > 1e-6f) stroke(vc, m, p0, p1, side, width * (1 - 0.15f * s), argb);
                x = nx;
                y = ny;
            }
        }
    }

    /**
     * Text facing the camera, centred on the pose's origin, {@code height} blocks tall (a line of the font), at full
     * brightness. {@code color} is ARGB (alpha 0 counts as opaque, as the font does).
     */
    public static void billboardText(PoseStack pose, MultiBufferSource buffers, Component text, float height, int color, boolean seeThrough) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float s = height / 9f;
        pose.scale(s, -s, s);
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, -4.5f, color, false, pose.last().pose(), buffers,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.popPose();
    }
}
