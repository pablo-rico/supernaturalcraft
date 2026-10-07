package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;

/** A slowly turning sigil circle that fades in, pulses, and fades out at the end of its life. */
public class WardRenderer extends EntityRenderer<WardEntity> {

    private static final ResourceLocation CIRCLE = SupernaturalCraft.asResource("textures/effect/ward_circle.png");

    public WardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WardEntity ward, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = ward.tickCount + partialTick;
        float life = ward.getLifetime();
        float fade = Mth.clamp(Math.min(age / 10f, (life - age) / 20f), 0f, 1f);
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        float r = ward.getRadius();
        DecalRenderer.draw(pose, buffers, CIRCLE, r, r, age * 1.5f, 0.05f, ward.getColor(), fade * pulse);
        DecalRenderer.draw(pose, buffers, CIRCLE, r * 0.55f, r * 0.55f, -age * 2.5f, 0.06f, ward.getColor(), fade * 0.6f);
        super.render(ward, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WardEntity ward) {
        return CIRCLE;
    }
}
