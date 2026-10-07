package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.ghost.GhostEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * A ghost as the local player sees it: nothing at all, mostly. It shows, pale and translucent, when
 * it manifests, in brief flickers, when revealed, or to someone with Second Sight, and fades in and
 * out (the fade itself is {@code GhostClientHooks}).
 */
public class GhostRenderer extends GeoEntityRenderer<GhostEntity> {

    public GhostRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("ghost"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0f;
    }

    public static float alpha(GhostEntity ghost, float partialTick) {
        return Mth.lerp(partialTick, ghost.clientAlphaO, ghost.clientAlpha);
    }

    @Override
    public void render(GhostEntity ghost, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (alpha(ghost, partialTick) < 0.02f) return;
        super.render(ghost, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public RenderType getRenderType(GhostEntity ghost, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public Color getRenderColor(GhostEntity ghost, float partialTick, int light) {
        int a = Mth.clamp((int) (alpha(ghost, partialTick) * 255), 0, 255);
        return Color.ofARGB(a, 225, 236, 255);
    }

    @Override
    public boolean shouldShowName(GhostEntity ghost) {
        return alpha(ghost, 0) > 0.3f && super.shouldShowName(ghost);
    }
}
