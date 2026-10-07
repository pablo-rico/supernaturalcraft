package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferLook;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.LuciferUncagedEntity;
import org.papiricoh.supernaturalcraft.entity.boss.uncaged.UncagedBalance;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Lucifer Uncaged (and Lucifer waiting in his Cage). One model for six phases: the phase picks the
 * texture, how many pairs of wings show, whether the Enochian chains still hang from him, the
 * broken halo, and his size.
 */
public class LuciferUncagedRenderer<T extends Mob & GeoEntity & LuciferLook> extends GeoEntityRenderer<T> {

    public static final String[] WINGS = {"wing_r1", "wing_l1", "wing_r2", "wing_l2", "wing_r3", "wing_l3"};
    public static final String[] CHAINS = {"chain_r", "chain_l", "chain_neck"};
    public static final String HALO = "halo";

    public LuciferUncagedRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model<>());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 1.0f;
    }

    static class Model<T extends GeoEntity & LuciferLook> extends DefaultedEntityGeoModel<T> {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[UncagedBalance.PHASES];

        static {
            for (int i = 0; i < TEXTURES.length; i++) {
                TEXTURES[i] = SupernaturalCraft.asResource("textures/entity/lucifer_uncaged_p" + (i + 1) + ".png");
            }
        }

        Model() {
            super(SupernaturalCraft.asResource("lucifer_uncaged"), true);
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, animatable.lookPhase() - 1))];
        }
    }

    @Override
    public void preRender(PoseStack pose, T entity, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        int phase = entity.lookPhase();
        float s = UncagedBalance.scale(phase);
        scaleWidth = s;
        scaleHeight = s;
        int pairs = UncagedBalance.wingPairs(phase);
        for (int i = 0; i < WINGS.length; i++) {
            boolean show = i / 2 < pairs;
            getGeoModel().getBone(WINGS[i]).ifPresent(b -> {
                b.setHidden(!show);
                b.setChildrenHidden(!show);
            });
        }
        boolean chained = !(entity instanceof LuciferUncagedEntity) || UncagedBalance.chained(phase);
        for (String c : CHAINS) {
            getGeoModel().getBone(c).ifPresent(b -> {
                b.setHidden(!chained);
                b.setChildrenHidden(!chained);
            });
        }
        getGeoModel().getBone(HALO).ifPresent(b -> b.setHidden(phase < 5));
        super.preRender(pose, entity, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0f;
    }
}
