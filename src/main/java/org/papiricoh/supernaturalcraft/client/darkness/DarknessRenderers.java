package org.papiricoh.supernaturalcraft.client.darkness;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.darkness.S11Assets;
import org.papiricoh.supernaturalcraft.darkness.casifer.CasiferEntity;
import org.papiricoh.supernaturalcraft.darkness.fog.FoggedVillagerEntity;
import org.papiricoh.supernaturalcraft.darkness.visit.AmaraVisitorEntity;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Season 11's people (v0.42): Casifer, Amara at dawn, the fogged villagers. (Chuck uses the Author's renderer.) */
public final class DarknessRenderers {

    private DarknessRenderers() {
    }

    private static final ResourceLocation CASIFER = SupernaturalCraft.asResource(S11Assets.CASIFER_TEXTURE);

    private static void show(BakedGeoModel baked, String name, boolean on) {
        baked.getBone(name).ifPresent(b -> {
            b.setHidden(!on);
            b.setChildrenHidden(!on);
        });
    }

    /** Lucifer in Castiel: Castiel's rig, the casifer texture, red eyes (its glowmask), every toggled bone hidden. */
    public static class Casifer extends GeoEntityRenderer<CasiferEntity> {
        public Casifer(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(GodstielAssets.RIG)) {
                @Override
                public ResourceLocation getTextureResource(CasiferEntity e) {
                    return CASIFER;
                }
            });
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.5f;
        }

        @Override
        public void render(CasiferEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(SupernaturalCraft.asResource(GodstielAssets.GEO), SupernaturalCraft.asResource(GodstielAssets.ANIM))) return;
            super.render(e, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public void preRender(PoseStack pose, CasiferEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                for (String b : GodstielAssets.TOGGLED) show(baked, b, false);
                GodstielAssets.TOGGLED_PARTS.values().forEach(l -> l.forEach(b -> show(baked, b, false)));
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    /** Amara at dawn: the form rig at her age's scale and texture; the baby is her bundle alone. */
    public static class Visitor extends GeoEntityRenderer<AmaraVisitorEntity> {
        public Visitor(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource(AmaraAssets.RIG_FORM)) {
                @Override
                public ResourceLocation getTextureResource(AmaraVisitorEntity e) {
                    return SupernaturalCraft.asResource(e.age().texturePath());
                }
            });
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
            shadowRadius = 0.4f;
        }

        @Override
        public void render(AmaraVisitorEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!GeoGuard.ready(SupernaturalCraft.asResource(AmaraAssets.FORM_GEO), SupernaturalCraft.asResource(AmaraAssets.FORM_ANIM))) return;
            pose.pushPose();
            float k = e.age().scale;
            pose.scale(k, k, k);
            super.render(e, yaw, partialTick, pose, buffers, light);
            pose.popPose();
        }

        @Override
        public void preRender(PoseStack pose, AmaraVisitorEntity e, BakedGeoModel baked, @Nullable MultiBufferSource buffers, @Nullable VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                boolean baby = e.age() == AmaraAssets.Age.BABY;
                for (String b : AmaraAssets.TOGGLED) show(baked, b, baby && b.equals("bundle"));
                for (String b : new String[]{"torso", "head", "right_arm", "left_arm", "right_leg", "left_leg", "dress"}) {
                    show(baked, b, !baby);
                }
            }
            super.preRender(pose, e, baked, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }
    }

    private static final ResourceLocation VILLAGER_BASE = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final ResourceLocation VEINS = SupernaturalCraft.asResource(S11Assets.FOGGED_VILLAGER_OVERLAY);

    /** A fogged villager: a plain villager under the black veins of the fog. */
    public static class Fogged extends MobRenderer<FoggedVillagerEntity, VillagerModel<FoggedVillagerEntity>> {
        public Fogged(EntityRendererProvider.Context ctx) {
            super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
            addLayer(new Veins(this));
        }

        @Override
        public ResourceLocation getTextureLocation(FoggedVillagerEntity e) {
            return VILLAGER_BASE;
        }
    }

    static final class Veins extends RenderLayer<FoggedVillagerEntity, VillagerModel<FoggedVillagerEntity>> {
        Veins(RenderLayerParent<FoggedVillagerEntity, VillagerModel<FoggedVillagerEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, FoggedVillagerEntity e, float limb, float limbAmount,
                           float partial, float age, float headYaw, float headPitch) {
            if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(VEINS).isEmpty()) return;
            // Partly see-through (the art's soft edges): a translucent pass over the villager.
            getParentModel().renderToBuffer(pose, buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(VEINS)), light,
                    net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(e, 0f));
        }
    }
}
