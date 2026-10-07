package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.azazel.AzazelEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Azazel: a man in a dark suit with yellow eyes; cracked and leaking light once the smoke shows. Not drawn while he is smoke. */
public class AzazelRenderer extends GeoEntityRenderer<AzazelEntity> {

    private static final ResourceLocation[] TEXTURES = {
            SupernaturalCraft.asResource("textures/entity/azazel_p1.png"),
            SupernaturalCraft.asResource("textures/entity/azazel_p2.png")};

    public AzazelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("azazel"), true) {
            @Override
            public ResourceLocation getTextureResource(AzazelEntity animatable) {
                return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, animatable.lookPhase() - 1))];
            }
        });
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(AzazelEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.isSmoke()) return;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected float getDeathMaxRotation(AzazelEntity entity) {
        return 0f;
    }
}
