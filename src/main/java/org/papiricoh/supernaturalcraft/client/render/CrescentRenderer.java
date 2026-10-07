package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.projectile.SoulCrescent;

/** The soul crescent: a flat glowing arc lying along its flight, spinning slightly. */
public class CrescentRenderer extends EntityRenderer<SoulCrescent> {

    private static final ResourceLocation TEX = SupernaturalCraft.asResource("textures/effect/soul_crescent.png");

    public CrescentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SoulCrescent e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0, 0.25, 0);
        float y = e.getYRot();
        DecalRenderer.draw(pose, buffers, TEX, 1.1f, 0.55f, -y + 180, 0, 0x9FF6FF, 0.9f);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulCrescent e) {
        return TEX;
    }
}
