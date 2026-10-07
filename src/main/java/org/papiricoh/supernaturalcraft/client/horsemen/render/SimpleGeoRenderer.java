package org.papiricoh.supernaturalcraft.client.horsemen.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.function.Predicate;

/** A plain GeckoLib model ({@code geo/entity/<id>.geo.json}) drawn only once loaded, and only when {@code visible} says so. */
public class SimpleGeoRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {

    private final String id;
    private final Predicate<T> visible;

    public SimpleGeoRenderer(EntityRendererProvider.Context ctx, String id, float shadow, Predicate<T> visible) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(id), false));
        this.id = id;
        this.visible = visible;
        addRenderLayer(new OptionalGlowLayer<>(this));
        shadowRadius = shadow;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!visible.test(entity) || !GeoGuard.ready(GeoGuard.model(id), GeoGuard.animation(id))) return;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(T entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return visible.test(entity) && super.shouldRender(entity, frustum, x, y, z);
    }
}
