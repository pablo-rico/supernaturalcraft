package org.papiricoh.supernaturalcraft.client.legacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.entity.legacy.VampireEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * A vampire (v0.17): the red glint of its eyes in the glowmask; the second row of teeth ({@code fangs}) shows while
 * {@link VampireEntity#fangsOut()} or while it bites, hisses or bares them.
 */
public class VampireRenderer extends GeoEntityRenderer<VampireEntity> {

    public VampireRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LegacyGeo.Model<>("vampire", "head"));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(VampireEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!LegacyGeo.ready("vampire")) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, VampireEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            String clip = LegacyGeo.clip(e, e.getId(), "action");
            boolean fangs = e.fangsOut() || clip.equals("fangs_out") || clip.equals("bite") || clip.equals("hiss");
            LegacyGeo.show(getGeoModel(), "fangs", fangs);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
