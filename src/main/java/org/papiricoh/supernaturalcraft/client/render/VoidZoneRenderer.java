package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.hazard.VoidZone;

/** A slowly turning pool of black on the ground, with a faint violet rim. */
public class VoidZoneRenderer extends EntityRenderer<VoidZone> {

    private static final ResourceLocation CIRCLE = SupernaturalCraft.asResource("textures/effect/telegraph_circle.png");
    private static final ResourceLocation RING = SupernaturalCraft.asResource("textures/effect/telegraph_ring.png");

    public VoidZoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(VoidZone e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(VoidZone z, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = z.tickCount + partial;
        float grow = Mth.clamp(age / 15f, 0, 1);
        float r = z.radius() * grow;
        DecalRenderer.draw(pose, buffers, CIRCLE, r, r, age * 0.3f, 0.025f, 0x07030D, 0.92f);
        DecalRenderer.draw(pose, buffers, CIRCLE, r * 0.8f, r * 0.8f, -age * 0.5f, 0.03f, 0x000000, 0.9f);
        DecalRenderer.draw(pose, buffers, RING, r * 1.05f, r * 1.05f, age * 0.7f, 0.035f, 0x6A3FA8, 0.55f + 0.2f * Mth.sin(age * 0.15f));
    }

    @Override
    public ResourceLocation getTextureLocation(VoidZone z) {
        return CIRCLE;
    }
}
