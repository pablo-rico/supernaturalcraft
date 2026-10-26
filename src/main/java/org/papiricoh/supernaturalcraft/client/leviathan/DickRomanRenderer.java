package org.papiricoh.supernaturalcraft.client.leviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.render.DisguiseMask;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickAssets;
import org.papiricoh.supernaturalcraft.entity.boss.dick.DickRomanEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Dick Roman (v0.25): his rig ({@link DickAssets#GEO}). From his synced phase: the businessman's face until the Maw, then the
 * maw ({@code face}/{@code maw} swap), and the goo mantle in the Flood. Hidden among his doubles he is drawn as the hunter he
 * copies ({@link DisguiseMask}). The pen shows itself (the art scales it to nothing outside {@code sign_here}).
 */
public class DickRomanRenderer extends GeoEntityRenderer<DickRomanEntity> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(DickAssets.GEO), ANIMATION = SupernaturalCraft.asResource(DickAssets.ANIM);

    public DickRomanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("dick_roman"), true));
        shadowRadius = 0.5f;
    }

    @Override
    public void render(DickRomanEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (e.hidden() && DisguiseMask.render(e, e.disguise(), yaw, partialTick, pose, buffers, light)) return;
        if (!GeoGuard.ready(MODEL, ANIMATION)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, DickRomanEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender) {
            int phase = e.lookPhase();
            AllegianceGeo.show(getGeoModel(), "face", phase < 2);
            AllegianceGeo.show(getGeoModel(), "maw", phase >= 2);
            AllegianceGeo.show(getGeoModel(), "goo_mantle", phase >= DickRomanEntity.MAX_PHASE);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(DickRomanEntity e) {
        return 0f;
    }
}
