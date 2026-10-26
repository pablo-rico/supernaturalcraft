package org.papiricoh.supernaturalcraft.client.leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.client.legacy.render.LegacyGeo;
import org.papiricoh.supernaturalcraft.client.render.DisguiseMask;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanEntity;
import org.papiricoh.supernaturalcraft.entity.leviathan.LeviathanHeadEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * A Leviathan (v0.25). Disguised it is drawn as what it copies ({@link DisguiseMask}); found out, as itself, its head split into
 * the maw. Headless, the head bone is hidden and the black stump ({@code goo_neck}) shown.
 */
public class LeviathanRenderer extends GeoEntityRenderer<LeviathanEntity> {

    public LeviathanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LegacyGeo.Model<>("leviathan", "head"));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(LeviathanEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        String disguise = e.disguise();
        if (!disguise.isEmpty() && DisguiseMask.render(e, disguise, yaw, partialTick, pose, buffers, light)) return;
        if (!LegacyGeo.ready("leviathan")) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, LeviathanEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            LegacyGeo.show(getGeoModel(), "head", !e.headless());
            LegacyGeo.show(getGeoModel(), "goo_neck", e.headless());
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** The severed head, crawling home. */
    public static class Head extends GeoEntityRenderer<LeviathanHeadEntity> {
        public Head(EntityRendererProvider.Context ctx) {
            super(ctx, new LegacyGeo.Model<>("leviathan_head"));
            shadowRadius = 0.3f;
        }

        @Override
        public void render(LeviathanHeadEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (LegacyGeo.ready("leviathan_head")) super.render(e, yaw, partialTick, pose, buffers, light);
        }
    }
}
