package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.marker.TelegraphMarker;

/** Ground warnings that fade in, then pulse faster and faster until the attack lands. */
public class TelegraphRenderer extends EntityRenderer<TelegraphMarker> {

    private static final ResourceLocation CIRCLE = tex("telegraph_circle");
    private static final ResourceLocation RING = tex("telegraph_ring");
    private static final ResourceLocation LINE = tex("telegraph_line");
    private static final ResourceLocation CONE = tex("telegraph_cone");

    private static ResourceLocation tex(String name) {
        return SupernaturalCraft.asResource("textures/effect/" + name + ".png");
    }

    public TelegraphRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(TelegraphMarker e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(TelegraphMarker m, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = m.tickCount + partial;
        float life = Math.max(1, m.lifetime());
        float progress = Mth.clamp(age / life, 0, 1);
        float fadeIn = Mth.clamp(age / 8f, 0, 1);
        float pulse = 0.65f + 0.35f * Mth.sin(age * (0.3f + progress * 1.2f));
        float alpha = fadeIn * pulse * (0.55f + 0.45f * progress);
        float spin = age * 0.8f;
        switch (m.shape()) {
            case CIRCLE -> {
                DecalRenderer.draw(pose, buffers, CIRCLE, m.size(), m.size(), spin, 0.03f, m.color(), alpha);
                // A filling disc shows how long is left.
                float inner = m.size() * progress;
                DecalRenderer.draw(pose, buffers, CIRCLE, inner, inner, -spin, 0.04f, m.color(), alpha * 0.5f);
            }
            case RING -> DecalRenderer.draw(pose, buffers, RING, m.size(), m.size(), spin * 0.5f, 0.03f, m.color(), alpha);
            case LINE, CONE -> {
                pose.pushPose();
                pose.mulPose(Axis.YP.rotationDegrees(-m.getYRot()));
                pose.translate(0, 0, m.length() / 2);
                ResourceLocation t = m.shape() == TelegraphMarker.Shape.LINE ? LINE : CONE;
                float halfWidth = m.shape() == TelegraphMarker.Shape.LINE ? m.size() : m.size();
                DecalRenderer.draw(pose, buffers, t, halfWidth, m.length() / 2, 0, 0.03f, m.color(), alpha);
                pose.popPose();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TelegraphMarker m) {
        return CIRCLE;
    }
}
