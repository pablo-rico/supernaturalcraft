package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckText;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckGeometry;

/**
 * What holds the Author's script together. A {@link AuthorTargetEntity#PAGE} is a manuscript page hanging in the air,
 * turning slowly, typed on both sides; a {@link AuthorTargetEntity#NODE} is the hitbox of a weak point on his rings (the
 * model draws the node itself), marked by a small burning crystal. Either can wear a bubble of ink (shielded) and
 * cracks as it is hurt.
 */
public class AuthorTargetRenderer extends EntityRenderer<AuthorTargetEntity> {

    private static final ResourceLocation PAGE = SupernaturalCraft.asResource("textures/block/page_block.png");
    /** Lines typed on a page (lang keys, picked by the entity id). */
    public static final int PAGE_LINES = 8;

    public AuthorTargetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0f;
    }

    @Override
    public void render(AuthorTargetEntity target, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = (float) (target.level().getGameTime() + partialTick);
        float w = Math.max(0.4f, target.getBbWidth()), h = Math.max(0.4f, target.getBbHeight());
        pose.pushPose();
        pose.translate(0, h / 2, 0);
        if (target.kind() == AuthorTargetEntity.PAGE) page(target, pose, buffers, t, w, h);
        else node(target, pose, buffers, t);
        if (target.shielded()) shield(pose, buffers, t, target.kind() == AuthorTargetEntity.PAGE ? Math.max(w, h) * 0.8f
                : (float) ChuckGeometry.NODE_SIZE * 0.85f);
        pose.popPose();
        super.render(target, yaw, partialTick, pose, buffers, light);
    }

    private void page(AuthorTargetEntity target, PoseStack pose, MultiBufferSource buffers, float t, float w, float h) {
        pose.pushPose();
        pose.translate(0, 0.12f * Mth.sin(t * 0.08f + target.getId()), 0);
        pose.mulPose(Axis.YP.rotationDegrees(t * 1.2f + target.getId() * 37));
        pose.mulPose(Axis.XP.rotationDegrees(6 * Mth.sin(t * 0.11f + target.getId())));
        float pw = w * 0.9f, ph = h * 0.95f;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(PAGE));
        int shade = 255;
        float x0 = -pw / 2, x1 = pw / 2, y0 = -ph / 2, y1 = ph / 2;
        pageVertex(vc, pose, m, x0, y0, 0, 1, shade);
        pageVertex(vc, pose, m, x1, y0, 1, 1, shade);
        pageVertex(vc, pose, m, x1, y1, 1, 0, shade);
        pageVertex(vc, pose, m, x0, y1, 0, 0, shade);
        // The typed lines, on both faces.
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            if (side == 1) pose.mulPose(Axis.YP.rotationDegrees(180));
            typedLines(target, pose, buffers, pw, ph);
            pose.popPose();
        }
        // Cracks: torn strokes of ink through the paper, more as it is hurt.
        float cracks = Mth.clamp(target.cracks(), 0, 1);
        if (cracks > 0.01f) {
            VertexConsumer q = WorldDraw.quads(buffers);
            int n = 2 + Math.round(cracks * 7);
            for (int side = -1; side <= 1; side += 2) {
                WorldDraw.cracks(q, pose.last().pose(), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 0.012f * side),
                        n, Math.min(pw, ph) * (0.35f + 0.25f * cracks), 0.035f, target.getId() * 7919L, 0xE0100C14);
            }
        }
        pose.popPose();
    }

    private static void pageVertex(VertexConsumer vc, PoseStack pose, Matrix4f m, float x, float y, float u, float v, int shade) {
        vc.addVertex(m, x, y, 0).setColor(shade, shade, shade, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose.last(), 0, 0, 1);
    }

    private static void typedLines(AuthorTargetEntity target, PoseStack pose, MultiBufferSource buffers, float pw, float ph) {
        Font font = Minecraft.getInstance().font;
        int lines = 5;
        Component[] text = new Component[lines];
        int widest = 100;
        for (int i = 0; i < lines; i++) {
            text[i] = Component.translatable("fourth_wall.supernaturalcraft.page." + Math.floorMod(target.getId() + i, PAGE_LINES))
                    .withStyle(ChuckText.style());
            widest = Math.max(widest, font.width(text[i]));
        }
        pose.pushPose();
        pose.translate(0, ph / 2 - ph * 0.12f, 0.006f);
        float s = pw * 0.8f / widest;
        pose.scale(s, -s, s);
        for (int i = 0; i < lines; i++) {
            float y = i * (ph * 0.8f / lines) / s;
            font.drawInBatch(text[i], -widest / 2f, y, 0xFF2A2018, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        }
        pose.popPose();
    }

    private void node(AuthorTargetEntity target, PoseStack pose, MultiBufferSource buffers, float t) {
        float cracks = Mth.clamp(target.cracks(), 0, 1);
        float flick = ChuckRenderer.flicker(t, target.getId());
        boolean stutter = cracks > 0.05f && flick < cracks * 0.5f;
        float r = (float) ChuckGeometry.NODE_SIZE * 0.22f * (1 + 0.1f * Mth.sin(t * 0.3f));
        int core = stutter ? 0xC0FF5A3A : 0xD0FFF3C4;
        VertexConsumer q = WorldDraw.quads(buffers);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(t * 4));
        WorldDraw.crystal(q, pose.last().pose(), r, r * 1.6f, core);
        WorldDraw.crystal(q, pose.last().pose(), r * 1.7f, r * 2.4f, 0x40FFE7A0);
        pose.popPose();
        if (cracks > 0.01f) {
            pose.pushPose();
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            WorldDraw.cracks(q, pose.last().pose(), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, -0.3f),
                    2 + Math.round(cracks * 6), r * 2.6f, 0.04f, target.getId() * 104729L, 0xF0140F1E);
            pose.popPose();
        }
    }

    /** A bubble of ink: dark, glossy, its surface crawling. */
    private static void shield(PoseStack pose, MultiBufferSource buffers, float t, float radius) {
        VertexConsumer q = WorldDraw.quads(buffers);
        WorldDraw.sphere(q, pose.last().pose(), radius, 0.05f, t * 0.05f, 0x5A16123A);
        WorldDraw.sphere(q, pose.last().pose(), radius * 1.04f, 0.08f, t * 0.07f + 1, 0x2A5A54A8);
    }

    @Override
    public boolean shouldRender(AuthorTargetEntity target, Frustum frustum, double x, double y, double z) {
        return target.shouldRender(x, y, z) && frustum.isVisible(target.getBoundingBox().inflate(1.5));
    }

    @Override
    public ResourceLocation getTextureLocation(AuthorTargetEntity target) {
        return PAGE;
    }
}
