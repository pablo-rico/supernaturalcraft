package org.papiricoh.supernaturalcraft.client.legacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.entity.legacy.HenryEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Henry Winchester (v0.17): his 1958 suit, the fedora he tips, the order's briefcase in his hand. */
public class HenryRenderer extends GeoEntityRenderer<HenryEntity> {

    public HenryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LegacyGeo.Model<>("henry_winchester", "head"));
        shadowRadius = 0.45f;
    }

    @Override
    public void render(HenryEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!LegacyGeo.ready("henry_winchester")) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }
}
