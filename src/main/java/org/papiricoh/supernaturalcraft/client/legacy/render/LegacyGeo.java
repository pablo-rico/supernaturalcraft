package org.papiricoh.supernaturalcraft.client.legacy.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Shared bits of the Men of Letters' renderers (v0.17): their GeckoLib model (assets by creature id, {@code legacy.LegacyAssets}),
 * the head turned toward where the creature looks (any bone named in {@code heads}), bone visibility and the clip playing.
 */
public final class LegacyGeo {

    private LegacyGeo() {
    }

    public static ResourceLocation geo(String id) {
        return SupernaturalCraft.asResource("geo/entity/" + id + ".geo.json");
    }

    public static ResourceLocation anim(String id) {
        return SupernaturalCraft.asResource("animations/entity/" + id + ".animation.json");
    }

    public static boolean ready(String id) {
        return GeoGuard.ready(geo(id), anim(id));
    }

    public static void show(GeoModel<?> model, String bone, boolean visible) {
        AllegianceGeo.show(model, bone, visible);
    }

    public static String clip(GeoAnimatable animatable, long instanceId, String controller) {
        return AllegianceGeo.clip(animatable, instanceId, controller);
    }

    /** A creature's model: {@code geo/entity/<id>}, {@code textures/entity/<id>}, {@code animations/entity/<id>}; turns its head bones. */
    public static class Model<T extends LivingEntity & GeoAnimatable> extends DefaultedEntityGeoModel<T> {
        private final String[] heads;

        public Model(String id, String... heads) {
            super(SupernaturalCraft.asResource(id), false);
            this.heads = heads;
        }

        @Override
        public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> state) {
            super.setCustomAnimations(animatable, instanceId, state);
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (data == null) return;
            for (String h : heads) {
                getBone(h).ifPresent(b -> {
                    b.setRotX(b.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
                    b.setRotY(b.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
                });
            }
        }
    }
}
