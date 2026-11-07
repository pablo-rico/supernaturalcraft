package org.papiricoh.supernaturalcraft.client.purgatory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.legacy.render.LegacyGeo;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatoryVampireEntity;
import org.papiricoh.supernaturalcraft.purgatory.mob.PurgatoryWerewolfEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Purgatory's souls (v0.39): their living kin's rigs and clips ({@code vampire}, {@code werewolf}) in their own ghost-grey textures
 * with soul-light eyes ({@code purgatory_<kin>} and its glowmask).
 */
public final class SoulRenderers {

    private SoulRenderers() {
    }

    /** The kin's model, this soul's texture (the glow layer finds its glowmask beside it). */
    static final class Model<T extends LivingEntity & GeoAnimatable> extends LegacyGeo.Model<T> {
        private final ResourceLocation texture;

        Model(String kin, String soul, String... heads) {
            super(kin, heads);
            texture = SupernaturalCraft.asResource("textures/entity/" + soul + ".png");
        }

        @Override
        public ResourceLocation getTextureResource(T animatable) {
            return texture;
        }
    }

    /** A vampire's soul: its second row of teeth shows while it hunts or bites, as the living one's. */
    public static class Vampire extends GeoEntityRenderer<PurgatoryVampireEntity> {
        public Vampire(EntityRendererProvider.Context ctx) {
            super(ctx, new Model<>("vampire", "purgatory_vampire", "head"));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.4f;
        }

        @Override
        public void render(PurgatoryVampireEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!LegacyGeo.ready("vampire")) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, PurgatoryVampireEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                String clip = LegacyGeo.clip(e, e.getId(), "action");
                LegacyGeo.show(getGeoModel(), "fangs", e.fangsOut() || clip.equals("fangs_out") || clip.equals("bite") || clip.equals("hiss"));
            }
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    /** A werewolf's soul: only ever the wolf. */
    public static class Werewolf extends GeoEntityRenderer<PurgatoryWerewolfEntity> {
        public Werewolf(EntityRendererProvider.Context ctx) {
            super(ctx, new Model<>("werewolf", "purgatory_werewolf", "h_head", "w_head"));
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.5f;
        }

        @Override
        public void render(PurgatoryWerewolfEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!LegacyGeo.ready("werewolf")) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, PurgatoryWerewolfEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                LegacyGeo.show(getGeoModel(), "human", false);
                LegacyGeo.show(getGeoModel(), "wolf", true);
            }
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }
}
