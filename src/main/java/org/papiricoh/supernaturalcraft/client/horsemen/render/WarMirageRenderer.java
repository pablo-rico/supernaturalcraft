package org.papiricoh.supernaturalcraft.client.horsemen.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.war.WarMirageEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** War's mirages wear the black-eyed demon's model and skin; the innocent ones kneel (lowered into its trapped pose). */
public class WarMirageRenderer extends GeoEntityRenderer<WarMirageEntity> {

    public WarMirageRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("black_eyed_demon"), true));
        shadowRadius = 0.5f;
    }

    @Override
    public void preRender(PoseStack pose, WarMirageEntity mirage, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender && mirage.isInnocent()) pose.translate(0, -0.45, 0);
        super.preRender(pose, mirage, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
