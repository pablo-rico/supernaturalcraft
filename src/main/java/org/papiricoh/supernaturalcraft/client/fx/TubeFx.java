package org.papiricoh.supernaturalcraft.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.List;

/**
 * Tentacles and spikes: an eight-sided tube along a curve, thick at the root and tapering to a
 * point, its texture crawling along it. Positions are camera-relative.
 */
public final class TubeFx {

    public static final ResourceLocation TEXTURE = SupernaturalCraft.asResource("textures/effect/tentacle.png");
    private static final int SIDES = 8;

    private TubeFx() {
    }

    /** Quadratic Bezier from {@code a} through control {@code c} to {@code b}, with a travelling wave. */
    public static void tentacle(PoseStack pose, MultiBufferSource buffers, Vec3 a, Vec3 c, Vec3 b, float rootRadius,
                                float wave, float time) {
        int n = 18;
        Vec3[] pts = new Vec3[n + 1];
        float[] radii = new float[n + 1];
        Vec3 side = b.subtract(a).cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        for (int i = 0; i <= n; i++) {
            float u = i / (float) n;
            Vec3 p = a.scale((1 - u) * (1 - u)).add(c.scale(2 * (1 - u) * u)).add(b.scale(u * u));
            float w = wave * Mth.sin(u * 9f - time * 6f) * u * (1 - u) * 4;
            pts[i] = p.add(side.scale(w));
            radii[i] = rootRadius * (1 - u * 0.85f);
        }
        tube(pose, buffers, List.of(pts), radii, time);
    }

    public static void tube(PoseStack pose, MultiBufferSource buffers, List<Vec3> pts, float[] radii, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        PoseStack.Pose p = pose.last();
        Vec3 prevA = null, prevB = null;
        Vec3[] prevRing = null;
        float v = -time * 0.8f;
        for (int i = 0; i < pts.size(); i++) {
            Vec3 here = pts.get(i);
            Vec3 dir = (i < pts.size() - 1 ? pts.get(i + 1) : here).subtract(i > 0 ? pts.get(i - 1) : here);
            if (dir.lengthSqr() < 1e-8) dir = new Vec3(0, 1, 0);
            dir = dir.normalize();
            Vec3 ax = Math.abs(dir.y) < 0.95 ? dir.cross(new Vec3(0, 1, 0)).normalize() : dir.cross(new Vec3(1, 0, 0)).normalize();
            Vec3 ay = dir.cross(ax).normalize();
            Vec3[] ring = new Vec3[SIDES + 1];
            for (int k = 0; k <= SIDES; k++) {
                double a = Math.PI * 2 * k / SIDES;
                ring[k] = here.add(ax.scale(Math.cos(a) * radii[i])).add(ay.scale(Math.sin(a) * radii[i]));
            }
            if (prevRing != null) {
                float v1 = v + (float) here.distanceTo(pts.get(i - 1)) * 0.5f;
                for (int k = 0; k < SIDES; k++) {
                    float u0 = k / (float) SIDES, u1 = (k + 1) / (float) SIDES;
                    vertex(vc, p, prevRing[k], u0, v);
                    vertex(vc, p, prevRing[k + 1], u1, v);
                    vertex(vc, p, ring[k + 1], u1, v1);
                    vertex(vc, p, ring[k], u0, v1);
                }
                v = v1;
            }
            prevRing = ring;
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, Vec3 at, float u, float v) {
        vc.addVertex(p, (float) at.x, (float) at.y, (float) at.z).setColor(255, 255, 255, 255).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }

    /** A flat glowing band around a vertical axis: inner and outer radius, at height {@code y}. */
    public static void ring(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, Vec3 center, float radius,
                            float width, float height, int rgb, float alpha, float time) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
        PoseStack.Pose p = pose.last();
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF, a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        int seg = Math.max(24, (int) (radius * 6));
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float u0 = i / (float) seg * radius / 2 + time, u1 = (i + 1) / (float) seg * radius / 2 + time;
            float ri = Math.max(0, radius - width);
            // The flat band on the ground...
            quad(vc, p, center, a0, a1, ri, radius, 0, 0, u0, u1, r, g, b, a);
            // ...and a short wall of light standing on its outer edge.
            quadWall(vc, p, center, a0, a1, radius, height, u0, u1, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, Vec3 c, float a0, float a1, float ri, float ro, float y0, float y1,
                             float u0, float u1, int r, int g, int b, int a) {
        put(vc, p, c.add(Mth.cos(a0) * ri, y0, Mth.sin(a0) * ri), u0, 1, r, g, b, a);
        put(vc, p, c.add(Mth.cos(a0) * ro, y1, Mth.sin(a0) * ro), u0, 0, r, g, b, a);
        put(vc, p, c.add(Mth.cos(a1) * ro, y1, Mth.sin(a1) * ro), u1, 0, r, g, b, a);
        put(vc, p, c.add(Mth.cos(a1) * ri, y0, Mth.sin(a1) * ri), u1, 1, r, g, b, a);
    }

    private static void quadWall(VertexConsumer vc, PoseStack.Pose p, Vec3 c, float a0, float a1, float ro, float h, float u0, float u1,
                                 int r, int g, int b, int a) {
        put(vc, p, c.add(Mth.cos(a0) * ro, 0, Mth.sin(a0) * ro), u0, 1, r, g, b, a);
        put(vc, p, c.add(Mth.cos(a0) * ro, h, Mth.sin(a0) * ro), u0, 0, r, g, b, 0);
        put(vc, p, c.add(Mth.cos(a1) * ro, h, Mth.sin(a1) * ro), u1, 0, r, g, b, 0);
        put(vc, p, c.add(Mth.cos(a1) * ro, 0, Mth.sin(a1) * ro), u1, 1, r, g, b, a);
    }

    private static void put(VertexConsumer vc, PoseStack.Pose p, Vec3 at, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(p, (float) at.x, (float) at.y, (float) at.z).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
