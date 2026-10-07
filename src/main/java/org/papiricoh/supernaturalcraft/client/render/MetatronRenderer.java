package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronBalance;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.MetatronEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Metatron: the vessel (glasses, cardigan, a quill behind the ear) in his first two phases; the scribe
 * of Heaven (robe, wings of pages) in the third; the Angel Tablet in his hand in the last.
 */
public class MetatronRenderer extends GeoEntityRenderer<MetatronEntity> {

    public static final String WINGS = "wings", ROBE = "robe", TABLET = "tablet";
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[MetatronBalance.PHASES];

    static {
        for (int i = 0; i < TEXTURES.length; i++) TEXTURES[i] = SupernaturalCraft.asResource("textures/entity/metatron_p" + (i + 1) + ".png");
    }

    public MetatronRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("metatron"), true) {
            @Override
            public ResourceLocation getTextureResource(MetatronEntity animatable) {
                return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, animatable.lookPhase() - 1))];
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.6f;
    }

    @Override
    public void preRender(PoseStack pose, MetatronEntity entity, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        int phase = entity.lookPhase();
        float s = MetatronBalance.scale(phase);
        scaleWidth = s;
        scaleHeight = s;
        boolean scribe = phase >= MetatronBalance.LECTERN_PHASE;
        model.getBone(WINGS).ifPresent(b -> {
            b.setHidden(!scribe);
            b.setChildrenHidden(!scribe);
        });
        model.getBone(ROBE).ifPresent(b -> {
            b.setHidden(!scribe);
            b.setChildrenHidden(!scribe);
        });
        model.getBone(TABLET).ifPresent(b -> b.setHidden(phase < 4));
        super.preRender(pose, entity, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(MetatronEntity entity) {
        return 0f;
    }
}
