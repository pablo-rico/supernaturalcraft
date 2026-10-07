package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Draws a glowing textured circle flat on the ground: wards, telegraphs, the arena floor sigil. */
public final class DecalRenderer {

    private DecalRenderer() {
    }

    /**
     * @param halfWidth  half the side length of the square quad, in blocks
     * @param halfLength half the other side (equal to halfWidth for circles; longer for line telegraphs)
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, float halfWidth,
                            float halfLength, float yawDeg, float y, int rgb, float alpha) {
        pose.pushPose();
        pose.translate(0, y, 0);
        pose.mulPose(Axis.YP.rotationDegrees(yawDeg));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
        PoseStack.Pose p = pose.last();
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF, a = (int) (alpha * 255);
        vertex(vc, p, -halfWidth, -halfLength, 0, 0, r, g, b, a);
        vertex(vc, p, -halfWidth, halfLength, 0, 1, r, g, b, a);
        vertex(vc, p, halfWidth, halfLength, 1, 1, r, g, b, a);
        vertex(vc, p, halfWidth, -halfLength, 1, 0, r, g, b, a);
        // Back face too, so it reads from below a ledge.
        vertex(vc, p, halfWidth, -halfLength, 1, 0, r, g, b, a);
        vertex(vc, p, halfWidth, halfLength, 1, 1, r, g, b, a);
        vertex(vc, p, -halfWidth, halfLength, 0, 1, r, g, b, a);
        vertex(vc, p, -halfWidth, -halfLength, 0, 0, r, g, b, a);
        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float z, float u, float v,
                               int r, int g, int b, int a) {
        vc.addVertex(p, x, 0, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(p, 0, 1, 0);
    }
}
