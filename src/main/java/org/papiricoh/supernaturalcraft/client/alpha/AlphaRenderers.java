package org.papiricoh.supernaturalcraft.client.alpha;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.client.legacy.render.LegacyGeo;
import org.papiricoh.supernaturalcraft.client.render.DisguiseMask;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaShapeshifterEntity;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaVampireEntity;
import org.papiricoh.supernaturalcraft.entity.alpha.AlphaWerewolfEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The three Alphas (v0.39, {@code AlphaAssets}): their rigs with the eyes in the glowmask. The vampire's second row of fangs
 * shows while he hunts or bites; the shapeshifter, wearing a stolen face, is drawn as that hunter ({@link DisguiseMask}).
 */
public final class AlphaRenderers {

    private AlphaRenderers() {
    }

    public static class Vampire extends GeoEntityRenderer<AlphaVampireEntity> {
        public Vampire(EntityRendererProvider.Context ctx) {
            super(ctx, new LegacyGeo.Model<>("alpha_vampire", "head"));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.55f;
        }

        @Override
        public void render(AlphaVampireEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!LegacyGeo.ready("alpha_vampire")) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, AlphaVampireEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                String clip = LegacyGeo.clip(e, e.getId(), "action");
                boolean fangs = e.fangsOut() || clip.equals("fangs_out") || clip.equals("bite") || clip.equals("hiss") || clip.equals("drain")
                        || clip.equals("lunge") || clip.equals("command");
                LegacyGeo.show(getGeoModel(), "fangs", fangs);
            }
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    public static class Werewolf extends GeoEntityRenderer<AlphaWerewolfEntity> {
        public Werewolf(EntityRendererProvider.Context ctx) {
            super(ctx, new LegacyGeo.Model<>("alpha_werewolf", "head"));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.8f;
        }

        @Override
        public void render(AlphaWerewolfEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!LegacyGeo.ready("alpha_werewolf")) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }
    }

    public static class Shapeshifter extends GeoEntityRenderer<AlphaShapeshifterEntity> {
        public Shapeshifter(EntityRendererProvider.Context ctx) {
            super(ctx, new LegacyGeo.Model<>("alpha_shapeshifter", "head"));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.5f;
        }

        @Override
        public void render(AlphaShapeshifterEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            String disguise = e.disguise();
            if (!e.revealed() && disguise != null && !disguise.isEmpty() && DisguiseMask.render(e, disguise, yaw, partialTick, pose, buffers, light)) return;
            if (!LegacyGeo.ready("alpha_shapeshifter")) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }
    }
}
