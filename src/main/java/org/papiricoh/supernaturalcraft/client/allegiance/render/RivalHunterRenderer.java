package org.papiricoh.supernaturalcraft.client.allegiance.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceAssets;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.entity.allegiance.RivalHunterEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A rival hunter: one rig, three looks by texture ({@code rival_hunter_<variant>.png}); bones named {@code v<n>_…} belong to
 * one look only. What is in his hands follows what he does: the salt shotgun by default, the machete for a while after he
 * slashes, the flask while he throws holy water. Until the art exists he borrows Dean's, Sam's or Castiel's model.
 */
public class RivalHunterRenderer extends GeoEntityRenderer<RivalHunterEntity> {

    private static final String[] STAND_INS = {"hunter_dean", "hunter_sam", "hunter_castiel"};
    /** Ticks the machete stays out after a slash. */
    private static final int MACHETE_HOLD = 60;
    private static final Map<RivalHunterEntity, Integer> SLASHED = new WeakHashMap<>();
    /** Pieces only one look wears (rival_hunter_art): 0 cap and hood, 1 vest and shirt tails, 2 the trench coat's skirt. */
    private static final String[][] LOOK_ONLY = {{"cap", "hood"}, {"vest", "shirt_tails"},
            {"coat_tail_r", "coat_tail_l", "coat_back", "belt_tail"}};

    public RivalHunterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        shadowRadius = 0.5f;
    }

    static boolean own() {
        return AllegianceGeo.ready(AllegianceAssets.RIVAL_HUNTER_GEO, AllegianceAssets.RIVAL_HUNTER_ANIM);
    }

    static String standIn(RivalHunterEntity e) {
        return STAND_INS[Math.floorMod(e.variant(), STAND_INS.length)];
    }

    @Override
    public void render(RivalHunterEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!own() && !GeoGuard.ready(GeoGuard.model(standIn(e)), GeoGuard.animation("hunter_ally"))) return;
        super.render(e, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public void preRender(PoseStack pose, RivalHunterEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int light, int overlay, int colour) {
        if (!isReRender && own()) {
            String clip = AllegianceGeo.clip(e, e.getId(), "action");
            if (clip.equals("slash") || e.swinging) SLASHED.put(e, e.tickCount);
            boolean flask = clip.equals("throw_water");
            Integer at = SLASHED.get(e);
            boolean machete = !flask && at != null && e.tickCount - at < MACHETE_HOLD;
            boolean shotgun = !flask && !machete;
            AllegianceGeo.show(getGeoModel(), "flask", flask);
            AllegianceGeo.show(getGeoModel(), "machete", machete);
            AllegianceGeo.show(getGeoModel(), "shotgun", shotgun);
            // What is not in hand hangs where it is kept.
            AllegianceGeo.show(getGeoModel(), "flask_belt", !flask);
            AllegianceGeo.show(getGeoModel(), "machete_sheathed", !machete);
            AllegianceGeo.show(getGeoModel(), "shotgun_slung", !shotgun);
            int variant = Math.floorMod(e.variant(), AllegianceAssets.RIVAL_HUNTER_VARIANTS);
            for (int look = 0; look < LOOK_ONLY.length; look++) {
                for (String bone : LOOK_ONLY[look]) AllegianceGeo.show(getGeoModel(), bone, look == variant);
            }
            for (GeoBone bone : model.topLevelBones()) variantBones(bone, variant);
        }
        super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
    }

    /** {@code v1_coat_tail}: shown only on look 1. */
    private static void variantBones(GeoBone bone, int variant) {
        String n = bone.getName();
        if (n.length() > 2 && n.charAt(0) == 'v' && Character.isDigit(n.charAt(1)) && n.charAt(2) == '_') {
            boolean on = n.charAt(1) - '0' == variant;
            bone.setHidden(!on);
            bone.setChildrenHidden(!on);
            if (!on) return;
        }
        for (GeoBone child : bone.getChildBones()) variantBones(child, variant);
    }

    static class Model extends DefaultedEntityGeoModel<RivalHunterEntity> {
        private static final ResourceLocation GEO = AllegianceGeo.asset(AllegianceAssets.RIVAL_HUNTER_GEO),
                ANIM = AllegianceGeo.asset(AllegianceAssets.RIVAL_HUNTER_ANIM);
        private static final ResourceLocation[] TEX = new ResourceLocation[AllegianceAssets.RIVAL_HUNTER_VARIANTS];

        static {
            for (int i = 0; i < TEX.length; i++) TEX[i] = AllegianceGeo.asset(AllegianceAssets.RIVAL_HUNTER_TEXTURE.formatted(i));
        }

        Model() {
            super(AllegianceGeo.asset("rival_hunter"), true);
        }

        @Override
        public ResourceLocation getModelResource(RivalHunterEntity e) {
            return own() ? GEO : GeoGuard.model(standIn(e));
        }

        @Override
        public ResourceLocation getTextureResource(RivalHunterEntity e) {
            if (own()) {
                ResourceLocation t = TEX[Math.floorMod(e.variant(), TEX.length)];
                return GeoGuard.exists(t) ? t : TEX[0];
            }
            return AllegianceGeo.asset("textures/entity/" + standIn(e) + ".png");
        }

        @Override
        public ResourceLocation getAnimationResource(RivalHunterEntity e) {
            return own() ? ANIM : GeoGuard.animation("hunter_ally");
        }
    }
}
