package org.papiricoh.supernaturalcraft.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.demon.BlackEyedDemon;
import org.papiricoh.supernaturalcraft.entity.demon.DemonEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders both lesser demons; the black-eyed demon picks its vessel's skin by variant. */
public class DemonRenderer<T extends DemonEntity> extends GeoEntityRenderer<T> {

    public DemonRenderer(EntityRendererProvider.Context ctx, String name) {
        super(ctx, new Model<>(name));
        shadowRadius = 0.5f;
    }

    static class Model<T extends DemonEntity> extends DefaultedEntityGeoModel<T> {
        private static final ResourceLocation DRIFTER =
                SupernaturalCraft.asResource("textures/entity/black_eyed_demon_drifter.png");

        Model(String name) {
            super(SupernaturalCraft.asResource(name), true);
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            if (animatable instanceof BlackEyedDemon demon && demon.getVariant() == 1) {
                return DRIFTER;
            }
            return super.getTextureResource(animatable);
        }
    }
}
