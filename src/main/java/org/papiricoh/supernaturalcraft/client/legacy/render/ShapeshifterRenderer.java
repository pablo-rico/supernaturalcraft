package org.papiricoh.supernaturalcraft.client.legacy.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


/**
 * A shapeshifter (v0.17). While it wears a skin ({@link ShapeshifterEntity#disguise()}) it is drawn as what it copies: a
 * vanilla villager of that profession, or the copied player (their own skin, and their name over its head). Found out
 * ({@link ShapeshifterEntity#revealed()}) it is its own plain man with the borrowed skin peeling away ({@code shed_skin*} bones)
 * and the silver flare in its eyes.
 */
public class ShapeshifterRenderer extends GeoEntityRenderer<ShapeshifterEntity> {

    static final String[] SHED = {"shed_skin", "shed_skin_body", "shed_skin_right_arm", "shed_skin_left_arm", "shed_skin_right_hand",
            "shed_skin_left_hand"};

    public ShapeshifterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LegacyGeo.Model<>("shapeshifter", "head"));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(ShapeshifterEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        String disguise = e.disguise();
        if (!e.revealed() && disguise != null && !disguise.isEmpty()) {
            if (org.papiricoh.supernaturalcraft.client.render.DisguiseMask.render(e, disguise, yaw, partialTick, pose, buffers, light)) return;
        }
        if (!LegacyGeo.ready("shapeshifter")) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, ShapeshifterEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            String clip = LegacyGeo.clip(e, e.getId(), "action");
            boolean shed = e.revealed() || clip.equals("revealed") || clip.equals("shed");
            for (String b : SHED) LegacyGeo.show(getGeoModel(), b, shed);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }
}
