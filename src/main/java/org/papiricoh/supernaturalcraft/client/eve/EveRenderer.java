package org.papiricoh.supernaturalcraft.client.eve;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveAssets;
import org.papiricoh.supernaturalcraft.entity.boss.eve.EveEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Eve (v0.39): her rig ({@link EveAssets#GEO}) with the amber of her eyes and her crown in the glowmask. From her synced phase:
 * the bark creeping over her shoulders from the Swarm on ({@code bark_mantle}), the crown of roots in the last ({@code root_crown}).
 */
public class EveRenderer extends GeoEntityRenderer<EveEntity> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(EveAssets.GEO), ANIMATION = SupernaturalCraft.asResource(EveAssets.ANIM);

    public EveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("eve"), true));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(EveEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(MODEL, ANIMATION)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, EveEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            int phase = e.lookPhase();
            AllegianceGeo.show(getGeoModel(), "bark_mantle", phase >= 2);
            AllegianceGeo.show(getGeoModel(), "root_crown", phase >= EveEntity.MAX_PHASE);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(EveEntity e) {
        return 0f;
    }
}
