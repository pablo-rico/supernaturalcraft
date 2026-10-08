package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBalance;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelBones;
import org.papiricoh.supernaturalcraft.entity.boss.michael.MichaelEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Michael in either of his two models (michael contract).
 * <ul>
 *   <li>The vessel ({@code michael.geo.json}), drawn translucent so the shadow wings' partial alpha shows (the body's pixels
 *   are opaque): {@code shadow_wings} hidden before phase III, {@code lance} hidden while it is out of his hand and the
 *   {@code blade} put away while he holds the lance.</li>
 *   <li>The true form ({@code michael_archangel.geo.json}, 4.5 blocks): the cracked textures once the halo breaks, four of
 *   the halo's spears gone with it, the lance hidden while thrown, and the bones no clip touches
 *   ({@link MichaelBones#PROCEDURAL}) driven here: the halo turns, the visor's light breathes, the cape of light sways.</li>
 * </ul>
 * Each is a GeckoLib renderer of its own (neither model swapped on the other) and stays silent until its files exist.
 */
public class MichaelRenderer extends EntityRenderer<MichaelEntity> {

    public static final ResourceLocation VESSEL_TEXTURE = SupernaturalCraft.asResource("textures/entity/michael.png");
    public static final ResourceLocation ARCHANGEL_TEXTURE = SupernaturalCraft.asResource("textures/entity/michael_archangel.png");
    public static final ResourceLocation ARCHANGEL_CRACKED = SupernaturalCraft.asResource("textures/entity/michael_archangel_cracked.png");
    /** Turns of the halo per tick, and of the visor's breath and the cape's sway. */
    static final float HALO_SPIN = 0.02f, BREATH = 0.09f, SWAY = 0.07f;

    private final Vessel vessel;
    private final Archangel archangel;

    public MichaelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        vessel = new Vessel(ctx);
        archangel = new Archangel(ctx);
        shadowRadius = 0.6f;
    }

    @Override
    public void render(MichaelEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (e.isArchangel()) {
            if (GeoGuard.ready(GeoGuard.model("michael_archangel"), GeoGuard.animation("michael_archangel"))) {
                archangel.render(e, yaw, partialTick, pose, buffers, light);
            }
        } else if (GeoGuard.ready(GeoGuard.model("michael"), GeoGuard.animation("michael"))) {
            vessel.render(e, yaw, partialTick, pose, buffers, light);
        }
    }

    @Override
    protected float getShadowRadius(MichaelEntity e) {
        return e.isArchangel() ? 1.2f : 0.6f;
    }

    /** His wings reach far past his hitbox: six blocks either side in his true form. */
    @Override
    public boolean shouldRender(MichaelEntity e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        AABB b = e.getBoundingBox();
        double r = e.isArchangel() ? 7 : e.wings() != MichaelEntity.WINGS_NONE ? 3.5 : 1;
        return frustum.isVisible(b.inflate(r, 1.5, r));
    }

    @Override
    public ResourceLocation getTextureLocation(MichaelEntity e) {
        return e.isArchangel() ? ARCHANGEL_TEXTURE : VESSEL_TEXTURE;
    }

    static void show(software.bernie.geckolib.model.GeoModel<?> model, String bone, boolean visible) {
        model.getBone(bone).ifPresent(b -> {
            b.setHidden(!visible);
            b.setChildrenHidden(!visible);
        });
    }

    // --- the vessel --------------------------------------------------------------------------------------------------

    static class Vessel extends GeoEntityRenderer<MichaelEntity> {

        Vessel(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(SupernaturalCraft.asResource("michael"), true));
            addRenderLayer(new OptionalGlowLayer<>(this));
            shadowRadius = 0.6f;
        }

        @Override
        public RenderType getRenderType(MichaelEntity e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        public void preRender(PoseStack pose, MichaelEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                boolean lanceOut = e.phase() >= MichaelBalance.SHADOW_WINGS_PHASE;
                show(getGeoModel(), MichaelBones.SHADOW_WINGS, e.wings() != MichaelEntity.WINGS_NONE);
                show(getGeoModel(), MichaelBones.LANCE, lanceOut && e.lanceHeld());
                show(getGeoModel(), MichaelBones.BLADE, !lanceOut || !e.lanceHeld());
            }
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        protected float getDeathMaxRotation(MichaelEntity e) {
            return 0f;
        }
    }

    // --- the true form -----------------------------------------------------------------------------------------------

    static class Archangel extends GeoEntityRenderer<MichaelEntity> {

        Archangel(EntityRendererProvider.Context ctx) {
            super(ctx, new ArchangelModel());
            addRenderLayer(new OptionalGlowLayer<>(this));
            shadowRadius = 1.2f;
        }

        /** Translucent: the cape of light and the halo's spear heads fade to a ragged hem. */
        @Override
        public RenderType getRenderType(MichaelEntity e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        public void preRender(PoseStack pose, MichaelEntity e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                show(getGeoModel(), MichaelBones.LANCE, e.lanceHeld());
                for (String spear : MichaelBones.BROKEN_SPEARS) show(getGeoModel(), spear, !e.haloBroken());
            }
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        protected float getDeathMaxRotation(MichaelEntity e) {
            return 0f;
        }
    }

    static class ArchangelModel extends DefaultedEntityGeoModel<MichaelEntity> {

        ArchangelModel() {
            super(SupernaturalCraft.asResource("michael_archangel"), false);
        }

        @Override
        public ResourceLocation getTextureResource(MichaelEntity e) {
            return e.haloBroken() && GeoGuard.exists(ARCHANGEL_CRACKED) ? ARCHANGEL_CRACKED : ARCHANGEL_TEXTURE;
        }

        @Override
        public void setCustomAnimations(MichaelEntity e, long instanceId, AnimationState<MichaelEntity> state) {
            super.setCustomAnimations(e, instanceId, state);
            double t = e.level().getGameTime() + state.getPartialTick();
            boolean dying = e.state() == LuciferEntity.DYING;
            // The halo: turning, slower as he dies; broken in VI it limps round.
            GeoBone halo = bone("halo_spin");
            if (halo != null) {
                float speed = (e.haloBroken() ? 0.55f : 1f) * (dying ? 0.3f : 1f);
                halo.setRotZ(halo.getInitialSnapshot().getRotZ() + (float) (t * HALO_SPIN * speed) % Mth.TWO_PI);
            }
            // The visor's light breathes.
            GeoBone visor = bone("visor_light");
            if (visor != null) {
                float s = 1 + 0.08f * Mth.sin((float) (t * BREATH));
                visor.setScaleX(s);
                visor.setScaleY(s);
                visor.setScaleZ(1);
            }
            // The cape of light: each segment sways a little more than the one above it, and streams back as he moves.
            float moving = Mth.clamp((float) e.getDeltaMovement().horizontalDistance() * 6f, 0, 1);
            for (int i = 0; i < 5; i++) {
                GeoBone cape = bone("cape_" + i);
                if (cape == null) continue;
                float sway = (0.04f + 0.03f * i) * Mth.sin((float) (t * SWAY) - i * 0.6f);
                float stream = moving * 0.12f * (i + 1) / 5f;
                cape.setRotX(cape.getInitialSnapshot().getRotX() + sway + stream);
                cape.setRotZ(cape.getInitialSnapshot().getRotZ() + 0.03f * Mth.sin((float) (t * SWAY * 0.7f) + i));
            }
        }

        private GeoBone bone(String name) {
            return getAnimationProcessor().getBone(name);
        }
    }
}
