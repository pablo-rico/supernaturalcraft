package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferLook;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Lucifer and his illusions. One model for all four phases: the phase picks the texture (and its
 * glowmask), which wings and whether the halo show, and how large he stands.
 */
public class LuciferRenderer<T extends Mob & GeoEntity & LuciferLook> extends GeoEntityRenderer<T> {

    private static final String[][] WINGS_BY_PHASE = {
            {},
            {"wing_r1", "wing_l1"},
            {"wing_r1", "wing_l1", "wing_r2", "wing_l2"},
            {"wing_r1", "wing_l1", "wing_r2", "wing_l2", "wing_r3", "wing_l3"}};
    private static final String[] ALL_WINGS = {"wing_r1", "wing_l1", "wing_r2", "wing_l2", "wing_r3", "wing_l3"};

    public LuciferRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model<>());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.8f;
    }

    static class Model<T extends GeoEntity & LuciferLook> extends DefaultedEntityGeoModel<T> {
        private static final ResourceLocation[] TEXTURES = {
                SupernaturalCraft.asResource("textures/entity/lucifer_p1.png"),
                SupernaturalCraft.asResource("textures/entity/lucifer_p2.png"),
                SupernaturalCraft.asResource("textures/entity/lucifer_p3.png"),
                SupernaturalCraft.asResource("textures/entity/lucifer_p4.png")};

        Model() {
            super(SupernaturalCraft.asResource("lucifer"), true);
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return TEXTURES[Math.max(0, Math.min(3, animatable.lookPhase() - 1))];
        }
    }

    @Override
    public void preRender(PoseStack pose, T entity, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        int phase = entity.lookPhase();
        float s = LuciferEntity.scaleFor(phase);
        scaleWidth = s;
        scaleHeight = s;
        for (String w : ALL_WINGS) {
            getGeoModel().getBone(w).ifPresent(b -> {
                b.setHidden(true);
                b.setChildrenHidden(true);
            });
        }
        for (String w : WINGS_BY_PHASE[Math.max(0, Math.min(3, phase - 1))]) {
            getGeoModel().getBone(w).ifPresent(b -> {
                b.setHidden(false);
                b.setChildrenHidden(false);
            });
        }
        getGeoModel().getBone("halo").ifPresent(b -> b.setHidden(phase < 4));
        super.preRender(pose, entity, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0f;
    }
}
