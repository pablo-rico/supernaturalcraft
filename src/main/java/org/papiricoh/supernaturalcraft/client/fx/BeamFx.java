package org.papiricoh.supernaturalcraft.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/**
 * A ribbon of light between two points that always turns its face to the camera: a bright core
 * and a softer glow, the texture scrolling along it. Used for every beam in the mod.
 */
public final class BeamFx {

    public static final ResourceLocation TEXTURE = SupernaturalCraft.asResource("textures/effect/beam.png");

    private BeamFx() {
    }

    /** Positions are camera-relative (world minus camera). */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 from, Vec3 to, float width, int rgb, float alpha, float time) {
        draw(pose, buffers, from, to, width, rgb, alpha, time, Vec3.ZERO);
    }

    /** As {@link #draw}, in a frame where the camera sits at {@code eye} (a block entity's local space, say). */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 from, Vec3 to, float width, int rgb, float alpha, float time,
                            Vec3 eye) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        ribbon(vc, pose.last(), from, to, width * 2.2f, rgb, alpha * 0.45f, time * 0.6f, eye);
        ribbon(vc, pose.last(), from, to, width, 0xFFFFFF, alpha, time, eye);
    }

    private static void ribbon(VertexConsumer vc, PoseStack.Pose p, Vec3 a, Vec3 b, float width, int rgb, float alpha, float scroll,
                               Vec3 eye) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 1.0E-3) return;
        // Side vector perpendicular to the beam and to the line of sight.
        Vec3 mid = a.add(b).scale(0.5).subtract(eye);
        Vec3 side = dir.cross(mid).normalize().scale(width / 2);
        if (Double.isNaN(side.x)) return;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, bl = rgb & 0xFF, al = (int) (Math.min(1f, alpha) * 255);
        float v0 = -scroll, v1 = (float) (len / 2.0) - scroll;
        vertex(vc, p, a.add(side), 0, v0, r, g, bl, al);
        vertex(vc, p, a.subtract(side), 1, v0, r, g, bl, al);
        vertex(vc, p, b.subtract(side), 1, v1, r, g, bl, al);
        vertex(vc, p, b.add(side), 0, v1, r, g, bl, al);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, Vec3 v, float u, float vv, int r, int g, int b, int a) {
        vc.addVertex(p, (float) v.x, (float) v.y, (float) v.z).setColor(r, g, b, a).setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
