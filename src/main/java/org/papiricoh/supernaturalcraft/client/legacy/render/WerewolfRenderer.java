package org.papiricoh.supernaturalcraft.client.legacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * A werewolf (v0.17): one rig with two forms -- the man ({@code human}) by day, the wolf ({@code wolf}, yellow eyes in the
 * glowmask) while {@link WerewolfEntity#wolfForm()}. While {@code turn} plays both show: its keys shrink one and swell the other.
 */
public class WerewolfRenderer extends GeoEntityRenderer<WerewolfEntity> {

    public WerewolfRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LegacyGeo.Model<>("werewolf", "h_head", "w_head"));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.6f;
    }

    @Override
    public void render(WerewolfEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!LegacyGeo.ready("werewolf")) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, WerewolfEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            boolean turning = LegacyGeo.clip(e, e.getId(), "action").equals("turn");
            LegacyGeo.show(getGeoModel(), "human", turning || !e.wolfForm());
            LegacyGeo.show(getGeoModel(), "wolf", turning || e.wolfForm());
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
