package org.papiricoh.supernaturalcraft.client.witch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchAssets;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * The Grand Witch (v0.21, P6): her eyes and the staff's crystal always glowing ({@link GrandWitchAssets#GLOW}), and nothing drawn
 * while her yellow smoke covers a {@code smoke_veil} blink (at its {@link GrandWitchAssets#HIT_TICKS} moment, or a TELEPORT
 * payload). Her staff's {@code crystal} is tracked ({@link WitchBones}): her coven's beams land on it and her spells leave it.
 * Register it with {@code ctx -> new GrandWitchRenderer<>(ctx)}.
 */
public class GrandWitchRenderer<T extends LivingEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private static final String RIG = "grand_witch";

    public GrandWitchRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(RIG), true));
        addRenderLayer(new OptionalGlowLayer<>(this));
        shadowRadius = 0.55f;
    }

    @Override
    public void render(T e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model(RIG), GeoGuard.animation(RIG)) || WitchGeo.vanished(e)
                || WitchGeo.inBlink(e, "smoke_veil", GrandWitchAssets.HIT_TICKS.get("smoke_veil"), partialTick)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
        WitchBones.record(e, getGeoModel().getBakedModel(getGeoModel().getModelResource(e, this)), partialTick, TRACKED);
    }

    private static final WitchBones.Bone[] TRACKED = {WitchBones.Bone.CRYSTAL, WitchBones.Bone.LEFT_HAND, WitchBones.Bone.HEAD};

    @Override
    public void preRender(PoseStack pose, T e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int light, int overlay, int colour) {
        if (!isReRender) WitchBones.track(model, TRACKED);
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
