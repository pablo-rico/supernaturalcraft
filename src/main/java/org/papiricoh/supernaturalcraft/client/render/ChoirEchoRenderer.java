package org.papiricoh.supernaturalcraft.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChoirEchoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** A Choir Echo: one small wheel around a single eye. */
public class ChoirEchoRenderer extends GeoEntityRenderer<ChoirEchoEntity> {

    public ChoirEchoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("choir_echo"), false));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.2f;
        scaleWidth = scaleHeight = 1.4f;
    }
}
