package org.papiricoh.supernaturalcraft.client.allegiance.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

/**
 * Heaven's messenger: Castiel's vessel in his own files ({@code AllegianceAssets.MESSENGER_*}). His shadow wings show only
 * while he appears, spreads them or fades; the vial only while he offers it. He fades in over his first second and goes
 * translucent while fading out. Until the art exists he is drawn as Castiel (the hunter ally's model) so nothing is missing.
 */
public class MessengerRenderer extends GeoEntityRenderer<MessengerEntity> {

    private static final ResourceLocation STAND_IN_GEO = GeoGuard.model("hunter_castiel"),
            STAND_IN_ANIM = GeoGuard.animation("hunter_ally"),
            STAND_IN_TEX = AllegianceGeo.asset("textures/entity/hunter_castiel.png");

    public MessengerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        addRenderLayer(new OptionalGlowLayer<>(this));
        shadowRadius = 0.5f;
    }

    static boolean own() {
        return AllegianceGeo.ready(AllegianceAssets.MESSENGER_GEO, AllegianceAssets.MESSENGER_ANIM);
    }

    @Override
    public void render(MessengerEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!own() && !GeoGuard.ready(STAND_IN_GEO, STAND_IN_ANIM)) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, MessengerEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender && own()) {
            String clip = AllegianceGeo.clip(e, e.getId(), "action");
            boolean wings = clip.equals("appear") || clip.equals("wing_spread") || clip.equals("fade");
            AllegianceGeo.show(getGeoModel(), "shadow_wings", wings);
            AllegianceGeo.show(getGeoModel(), "vial", clip.equals("offer_vial"));
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** 0 (not there) to 1: in over the first 20 ticks, out while the fade clip plays. */
    static float presence(MessengerEntity e, float partialTick) {
        float in = Mth.clamp((e.tickCount + partialTick) / 20f, 0, 1);
        return own() && AllegianceGeo.clip(e, e.getId(), "action").equals("fade") ? in * 0.55f : in;
    }

    @Override
    public Color getRenderColor(MessengerEntity e, float partialTick, int light) {
        return Color.ofARGB((int) (255 * presence(e, partialTick)), 255, 255, 255);
    }

    @Override
    public RenderType getRenderType(MessengerEntity e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
        // Translucent always: the shadow wings are smoke.
        return RenderType.entityTranslucent(texture);
    }

    static class Model extends DefaultedEntityGeoModel<MessengerEntity> {
        private static final ResourceLocation GEO = AllegianceGeo.asset(AllegianceAssets.MESSENGER_GEO),
                TEX = AllegianceGeo.asset(AllegianceAssets.MESSENGER_TEXTURE),
                ANIM = AllegianceGeo.asset(AllegianceAssets.MESSENGER_ANIM);

        Model() {
            super(AllegianceGeo.asset("messenger"), true);
        }

        @Override
        public ResourceLocation getModelResource(MessengerEntity e) {
            return own() ? GEO : STAND_IN_GEO;
        }

        @Override
        public ResourceLocation getTextureResource(MessengerEntity e) {
            return own() && GeoGuard.exists(TEX) ? TEX : STAND_IN_TEX;
        }

        @Override
        public ResourceLocation getAnimationResource(MessengerEntity e) {
            return own() ? ANIM : STAND_IN_ANIM;
        }
    }
}
