package org.papiricoh.supernaturalcraft.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.lilith.LilithEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Lilith: a fair-haired woman in a pale dress with white eyes; cracked with light, then blazing. */
public class LilithRenderer extends GeoEntityRenderer<LilithEntity> {

    private static final ResourceLocation[] TEXTURES = {
            SupernaturalCraft.asResource("textures/entity/lilith_p1.png"),
            SupernaturalCraft.asResource("textures/entity/lilith_p2.png"),
            SupernaturalCraft.asResource("textures/entity/lilith_p3.png")};

    public LilithRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("lilith"), true) {
            @Override
            public ResourceLocation getTextureResource(LilithEntity animatable) {
                return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, animatable.lookPhase() - 1))];
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.5f;
    }

    @Override
    protected float getDeathMaxRotation(LilithEntity entity) {
        return 0f;
    }
}
