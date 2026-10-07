package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.Chapter;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckBones;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckGeometry;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckLook;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * The Author, as a man or as the light: one renderer for every {@link ChuckLook} (the boss and the man at home).
 *
 * <p>The man is {@code chuck.geo.json} with only the outfit group {@link ChuckLook#outfit()} names. The light is
 * {@code chuck_divine.geo.json} drawn at {@link ChuckGeometry#DIVINE_SCALE} and full bright; its ring frames and rings
 * are posed each frame from {@link ChuckGeometry} (the numbers the weak points' hitboxes use), its halo of keys turns
 * and its core breathes. {@link ChuckLook#crack()} makes light leak from the man and ink flicker through the light.
 *
 * <p>Both halves are GeckoLib renderers of their own, so neither model ever has to be swapped on the other; each stays
 * silent until GeckoLib has its files.
 */
public class ChuckRenderer<T extends LivingEntity & GeoEntity & ChuckLook> extends EntityRenderer<T> {

    public static final ResourceLocation HUMAN_TEXTURE = SupernaturalCraft.asResource("textures/entity/chuck.png");
    public static final ResourceLocation DIVINE_TEXTURE = SupernaturalCraft.asResource("textures/entity/chuck_divine.png");
    private static final ResourceLocation HUMAN_CRACKS = SupernaturalCraft.asResource("textures/entity/chuck_cracks.png");
    private static final ResourceLocation DIVINE_CRACKS = SupernaturalCraft.asResource("textures/entity/chuck_divine_cracks.png");

    /**
     * How the ring frames take {@link ChuckGeometry#TILT_X} and the rings their spin: negated, because GeckoLib bakes
     * Bedrock X mirrored and turns the model 180° (the nodes are built at Bedrock (R cos θ, core, −R sin θ)). With these
     * the node bones land exactly on {@link ChuckGeometry#nodeOffset}, where the weak points' hitboxes are.
     */
    static final float TILT_X_SIGN = -1f, SPIN_SIGN = -1f;

    private final Human<T> human;
    private final Divine<T> divine;

    public ChuckRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        human = new Human<>(ctx);
        divine = new Divine<>(ctx);
        shadowRadius = 0.5f;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.divine()) {
            if (GeoGuard.ready(GeoGuard.model("chuck_divine"), GeoGuard.animation("chuck_divine"))) {
                divine.render(entity, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
            }
        } else if (GeoGuard.ready(GeoGuard.model("chuck"), GeoGuard.animation("chuck"))) {
            human.render(entity, yaw, partialTick, pose, buffers, light);
        }
    }

    @Override
    protected float getShadowRadius(T entity) {
        return entity.divine() ? 0f : 0.5f;
    }

    /** The light reaches far past his hitbox: rings 8 blocks out, the crown 14 up. */
    @Override
    public boolean shouldRender(T entity, Frustum frustum, double x, double y, double z) {
        if (!entity.divine()) return super.shouldRender(entity, frustum, x, y, z);
        if (!entity.shouldRender(x, y, z)) return false;
        AABB b = entity.getBoundingBox();
        double r = ChuckGeometry.RING_RADIUS[ChuckGeometry.RING_RADIUS.length - 1] + 2;
        return frustum.isVisible(new AABB(b.getCenter().x - r, b.minY - 2, b.getCenter().z - r,
                b.getCenter().x + r, b.minY + ChuckGeometry.DIVINE_HEIGHT + r, b.getCenter().z + r));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.divine() ? DIVINE_TEXTURE : HUMAN_TEXTURE;
    }

    /** A 0-1 flicker that jumps a few times a second (the script stutters). */
    static float flicker(double t, int salt) {
        long k = (long) (t / 3) * 31 + salt * 977L;
        k = (k ^ (k >>> 13)) * 0x5DEECE66DL;
        return ((k >>> 17) & 0xFF) / 255f;
    }

    // --- the man ---------------------------------------------------------------------------------------------------

    static class Human<T extends LivingEntity & GeoEntity & ChuckLook> extends GeoEntityRenderer<T> {

        private static final String[] OUTFITS = {ChuckBones.OUTFIT_ROBE, ChuckBones.OUTFIT_FLANNEL, ChuckBones.OUTFIT_SUIT};

        Human(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<T>(SupernaturalCraft.asResource("chuck"), true));
            addRenderLayer(new OptionalGlowLayer<>(this));
            addRenderLayer(new CrackLayer<>(this, HUMAN_CRACKS, false));
            shadowRadius = 0.5f;
        }

        @Override
        public void preRender(PoseStack pose, T entity, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) {
                Chapter.Outfit outfit = entity.outfit();
                for (int i = 0; i < OUTFITS.length; i++) show(OUTFITS[i], i == outfit.ordinal());
                show(ChuckBones.GLASS, outfit != Chapter.Outfit.SUIT);
            }
            super.preRender(pose, entity, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        private void show(String bone, boolean visible) {
            getGeoModel().getBone(bone).ifPresent(b -> {
                b.setHidden(!visible);
                b.setChildrenHidden(!visible);
            });
        }

        @Override
        protected float getDeathMaxRotation(T entity) {
            return 0f;
        }
    }

    // --- the light -------------------------------------------------------------------------------------------------

    static class Divine<T extends LivingEntity & GeoEntity & ChuckLook> extends GeoEntityRenderer<T> {

        Divine(EntityRendererProvider.Context ctx) {
            super(ctx, new DivineModel<>());
            addRenderLayer(new OptionalGlowLayer<>(this));
            addRenderLayer(new CrackLayer<>(this, DIVINE_CRACKS, true));
            scaleWidth = scaleHeight = ChuckGeometry.DIVINE_SCALE;
            shadowRadius = 0f;
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }

        @Override
        protected int getBlockLightLevel(T entity, net.minecraft.core.BlockPos pos) {
            return 15;
        }

        @Override
        protected float getDeathMaxRotation(T entity) {
            return 0f;
        }
    }

    static class DivineModel<T extends LivingEntity & GeoEntity & ChuckLook> extends DefaultedEntityGeoModel<T> {

        /** Which weak points still stand (chapter 4), refreshed each frame from the targets around him. */
        private final boolean[][] standing = new boolean[ChuckBones.RINGS][ChuckBones.NODES];

        DivineModel() {
            super(SupernaturalCraft.asResource("chuck_divine"), false);
        }

        @Override
        public void setCustomAnimations(T e, long instanceId, AnimationState<T> state) {
            super.setCustomAnimations(e, instanceId, state);
            double t = e.level().getGameTime() + state.getPartialTick();
            refreshStanding(e);
            boolean window = e instanceof ChuckEntity c && c.windowOpen();
            for (int r = 0; r < ChuckBones.RINGS; r++) {
                GeoBone tilt = bone(ChuckBones.tilt(r));
                if (tilt != null) {
                    tilt.setRotX((float) (TILT_X_SIGN * ChuckGeometry.TILT_X[r]));
                    tilt.setRotY((float) ChuckGeometry.TILT_Y[r]);
                    tilt.setRotZ(0);
                }
                GeoBone ring = bone(ChuckBones.ring(r));
                if (ring != null) ring.setRotY((float) (SPIN_SIGN * ChuckGeometry.spin(r, t)));
                for (int n = 0; n < ChuckBones.NODES; n++) {
                    GeoBone node = bone(ChuckBones.node(r, n));
                    if (node == null) continue;
                    boolean up = standing[r][n];
                    node.setHidden(!up);
                    node.setChildrenHidden(!up);
                    float s = 1 + 0.12f * Mth.sin((float) (t * 0.21 + r * 1.7 + n * 2.1));
                    node.setScaleX(s);
                    node.setScaleY(s);
                    node.setScaleZ(s);
                }
            }
            float haloAngle = (float) (ChuckGeometry.HALO_SPIN * t);
            GeoBone halo = bone(ChuckBones.HALO);
            if (halo != null) halo.setRotZ(haloAngle);
            // One key of the halo goes down at a time, as if typed.
            int typed = (int) (t / 3) % ChuckBones.KEYS;
            for (int k = 0; k < ChuckBones.KEYS; k++) {
                GeoBone key = bone(ChuckBones.key(k));
                if (key == null) continue;
                float s = k == typed ? 0.8f : 1f;
                // Counter-turned against the halo, so every letter stays upright as the ring goes round.
                key.setRotZ(-haloAngle);
                key.setScaleX(1);
                key.setScaleY(s);
                key.setScaleZ(1);
            }
            GeoBone core = bone(ChuckBones.CORE);
            if (core != null) {
                float pulse = window ? 1.22f + 0.1f * Mth.sin((float) t * 0.9f) : 1f + 0.06f * Mth.sin((float) t * 0.15f);
                core.setScaleX(pulse);
                core.setScaleY(pulse);
                core.setScaleZ(pulse);
            }
        }

        private void refreshStanding(T e) {
            boolean library = e instanceof ChuckEntity c && c.chapter() == Chapter.LIBRARY;
            for (boolean[] row : standing) java.util.Arrays.fill(row, !library);
            if (!library) return;
            for (AuthorTargetEntity target : e.level().getEntitiesOfClass(AuthorTargetEntity.class, e.getBoundingBox().inflate(16, 24, 16))) {
                if (target.kind() != AuthorTargetEntity.NODE) continue;
                int r = target.ring(), n = target.node();
                if (r >= 0 && r < ChuckBones.RINGS && n >= 0 && n < ChuckBones.NODES) standing[r][n] = true;
            }
        }

        private GeoBone bone(String name) {
            return getAnimationProcessor().getBone(name);
        }
    }

    // --- the script breaking -----------------------------------------------------------------------------------------

    /**
     * {@link ChuckLook#crack()}: light leaks out of the man (an emissive white flicker over his whole body), ink
     * flickers through the light. If the art ships a crack texture it glows on top, brighter as the script breaks.
     */
    static class CrackLayer<T extends LivingEntity & GeoEntity & ChuckLook> extends GeoRenderLayer<T> {

        private final ResourceLocation cracks;
        private final boolean ink;

        CrackLayer(GeoRenderer<T> renderer, ResourceLocation cracks, boolean ink) {
            super(renderer);
            this.cracks = cracks;
            this.ink = ink;
        }

        @Override
        public void render(PoseStack pose, T e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            float crack = Mth.clamp(e.crack(), 0, 1);
            if (crack < 0.02f) return;
            double t = e.level().getGameTime() + partialTick;
            if (GeoGuard.exists(cracks)) {
                // The man's cracks shine; the light's are dark fractures. Either shows as much as the script is broken.
                RenderType rt = ink ? RenderType.entityTranslucent(cracks) : RenderType.entityTranslucentEmissive(cracks);
                int a = (int) (255 * crack);
                getRenderer().reRender(model, pose, buffers, e, rt, buffers.getBuffer(rt), partialTick, 0xF000F0, overlay,
                        (a << 24) | 0xFFFFFF);
            }
            float f = flicker(t, e.getId());
            if (f > 1 - crack * 0.6f) {
                ResourceLocation tex = getTextureResource(e);
                RenderType rt = ink ? RenderType.entityTranslucent(tex) : RenderType.entityTranslucentEmissive(tex);
                int a = (int) (255 * Mth.clamp(crack * (0.35f + 0.4f * f), 0, 0.8f));
                int rgb = ink ? 0x14112A : 0xFFFFFF;
                getRenderer().reRender(model, pose, buffers, e, rt, buffers.getBuffer(rt), partialTick, 0xF000F0, overlay, (a << 24) | rgb);
            }
        }
    }
}
