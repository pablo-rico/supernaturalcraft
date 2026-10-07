package org.papiricoh.supernaturalcraft.client.horsemen.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemanKind;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.HorsemenAnimations;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * A Horseman ({@code geo/entity/<id>.geo.json}): his horse ({@code steed}) shows only once he has mounted it; Famine's
 * wheelchair only in his first phase; Death carries his cane in the first phase and his scythe after.
 */
public class HorsemanRenderer<T extends HorsemanEntity> extends GeoEntityRenderer<T> {

    private final String id;

    public HorsemanRenderer(EntityRendererProvider.Context ctx, String id) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(id), true));
        this.id = id;
        addRenderLayer(new OptionalGlowLayer<>(this));
        shadowRadius = 0.6f;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!GeoGuard.ready(GeoGuard.model(id), GeoGuard.animation(id))) return;
        shadowRadius = entity.isMounted() ? 1.0f : 0.6f;
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, T entity, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int light, int overlay, int colour) {
        boolean mounted = entity.isMounted();
        int phase = entity.phase();
        show(model, HorsemenAnimations.STEED_BONE, mounted);
        if (entity instanceof org.papiricoh.supernaturalcraft.entity.boss.horsemen.famine.FamineEntity famine) {
            show(model, HorsemenAnimations.WHEELCHAIR_BONE, famine.seated());
        }
        if (entity.kind() == HorsemanKind.DEATH) {
            show(model, HorsemenAnimations.CANE_BONE, phase == 1);
            show(model, HorsemenAnimations.SCYTHE_BONE, phase >= 2);
        }
        super.preRender(pose, entity, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    static void show(BakedGeoModel model, String bone, boolean visible) {
        model.getBone(bone).ifPresent(b -> {
            b.setHidden(!visible);
            b.setChildrenHidden(!visible);
        });
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0f;
    }
}
