package org.papiricoh.supernaturalcraft.bowl.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.SpellBowlBlock;

import java.util.List;

/**
 * What is in a bowl, drawn in block space (0..1, the bowl's floor at y=0): the liquid as a
 * translucent disc in the mix's colour (higher with more doses, its texture slowly swirling), and
 * the ingredients laid flat in a ring, floating half-sunk on the surface and turning and bobbing
 * gently (resting on the floor when there is no liquid). Shared by the block entity renderer and
 * the item renderer.
 */
public final class BowlContentsRenderer {

    public static final ResourceLocation LIQUID = SupernaturalCraft.asResource("textures/block/bowl_liquid.png");
    /** Liquid radius (blocks): the inside of the bowl's wall. */
    public static final float RADIUS = 4.9f / 16f;
    /** Where ingredients rest on an empty bowl's floor. */
    public static final float FLOOR = 1.1f / 16f;
    private static final int SEGMENTS = 16;
    private static final float ITEM_SCALE = 0.22f;

    private BowlContentsRenderer() {
    }

    /**
     * @param time  game time plus partial tick
     * @param tiltX tilt of the liquid about X (radians), from the bearer's movement
     * @param tiltZ tilt about Z
     * @param seed  per-bowl seed for the items' random models
     */
    public static void render(BowlContents contents, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                              float time, float tiltX, float tiltZ, @Nullable Level level, int seed) {
        if (contents.isEmpty()) return;
        int doses = contents.liquids().size();
        float surface = doses > 0 ? SpellBowlBlock.liquidHeight(doses) : FLOOR;
        pose.pushPose();
        pose.translate(0.5f, surface, 0.5f);
        pose.mulPose(Axis.XP.rotation(tiltX));
        pose.mulPose(Axis.ZP.rotation(tiltZ));
        if (doses > 0) liquid(pose, buffers, light, overlay, contents.mixColor(), time, doses);
        items(contents.stacks(), pose, buffers, light, overlay, time, doses > 0, level, seed);
        pose.popPose();
    }

    private static void liquid(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int rgb, float time, int doses) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(LIQUID));
        Matrix4f m = pose.last().pose();
        PoseStack.Pose last = pose.last();
        int argb = 0xC0000000 | (rgb & 0xFFFFFF);
        float swirl = time * 0.012f;
        // A little narrower when shallow: the bowl's floor curves in.
        float r = RADIUS * (0.86f + 0.14f * Math.min(doses, BowlContents.MAX_DOSES) / BowlContents.MAX_DOSES);
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS, a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float x0 = Mth.cos(a0) * r, z0 = Mth.sin(a0) * r, x1 = Mth.cos(a1) * r, z1 = Mth.sin(a1) * r;
            // A fan as degenerate quads: centre, rim, rim, centre (counter-clockwise seen from above).
            vertex(vc, m, last, 0, 0, uvU(0, 0, swirl), uvV(0, 0, swirl), argb, light, overlay);
            vertex(vc, m, last, x1, z1, uvU(a1, 0.5f, swirl), uvV(a1, 0.5f, swirl), argb, light, overlay);
            vertex(vc, m, last, x0, z0, uvU(a0, 0.5f, swirl), uvV(a0, 0.5f, swirl), argb, light, overlay);
            vertex(vc, m, last, 0, 0, uvU(0, 0, swirl), uvV(0, 0, swirl), argb, light, overlay);
        }
    }

    private static float uvU(float a, float r, float swirl) {
        return 0.5f + Mth.cos(a + swirl) * r;
    }

    private static float uvV(float a, float r, float swirl) {
        return 0.5f + Mth.sin(a + swirl) * r;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, PoseStack.Pose pose, float x, float z, float u, float v,
                               int argb, int light, int overlay) {
        vc.addVertex(m, x, 0, z).setColor(argb).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static void items(List<ItemStack> stacks, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                              float time, boolean floating, @Nullable Level level, int seed) {
        int n = stacks.size();
        if (n == 0) return;
        float ring = n == 1 ? 0 : n <= 3 ? 0.11f : 0.17f;
        float spin = floating ? time * 0.4f : 0;
        for (int i = 0; i < n; i++) {
            float angle = spin + i * 360f / n;
            float bob = floating ? 0.008f * Mth.sin(time * 0.06f + i * 1.7f) : 0;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(ring, bob + (floating ? 0 : 0.01f * i), 0);
            // Each one turned its own way, laid flat, and rocking a little on the ripples.
            pose.mulPose(Axis.YP.rotationDegrees(i * 137.5f + (floating ? time * 0.8f : 0)));
            pose.mulPose(Axis.XP.rotationDegrees(90 + (floating ? 6 * Mth.sin(time * 0.05f + i) : 0)));
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(stacks.get(i), ItemDisplayContext.FIXED, light,
                    overlay == 0 ? OverlayTexture.NO_OVERLAY : overlay, pose, buffers, level, seed + i);
            pose.popPose();
        }
    }
}
