package org.papiricoh.supernaturalcraft.client.heaven.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.client.allegiance.render.AllegianceGeo;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.michael.render.GeoProp;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * Heaven's GeckoLib bodies (v0.18): Naomi, Zachariah, Ash, the guards and clerks (the Host's rig in their own looks) and the
 * reprogramming chair. Each draws nothing at all while its model or animation file is missing (the art lands on its own
 * schedule: no crash, no log spam). An entity that is a {@link GeoAnimatable} is drawn by GeckoLib as itself (the clips the
 * server triggers play); one that is not (yet) is drawn as a prop looping the rig's {@code idle}. Subclasses say which texture,
 * which bones show for the clip playing, whether it is translucent, its colour, its glow.
 */
public class HeavenGeoRenderer<E extends Entity> extends EntityRenderer<E> {

    protected final ResourceLocation model, animation, defaultTexture;
    private final String animName;
    @SuppressWarnings("rawtypes")
    private final Inner inner;
    private final GeoProp prop;

    /**
     * @param name    the rig: {@code geo/entity/<name>.geo.json}, {@code animations/entity/<name>.animation.json}
     * @param texture its default texture
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public HeavenGeoRenderer(EntityRendererProvider.Context ctx, String name, ResourceLocation texture, float shadow) {
        super(ctx);
        this.animName = name;
        this.model = GeoGuard.model(name);
        this.animation = GeoGuard.animation(name);
        this.defaultTexture = texture;
        this.inner = new Inner(ctx, new Model(this), this);
        this.prop = new GeoProp(model, texture, animation, "animation." + name + ".idle");
        this.shadowRadius = shadow;
    }

    // --- what subclasses decide ------------------------------------------------------------------------------------------

    /** The texture for this entity (it may change with its state). */
    protected ResourceLocation texture(E e) {
        return defaultTexture;
    }

    /** Shows or hides bones for what it is doing ({@code clip}: the action clip playing, "" if none). */
    protected void bones(E e, String clip, GeoModel<?> geo) {
    }

    /** Whether it is drawn see-through (wings of smoke, light). */
    protected boolean translucent(E e) {
        return false;
    }

    /** ARGB it is drawn with. */
    protected int colour(E e, float partial) {
        return 0xFFFFFFFF;
    }

    /** The glowmask to draw at full bright, or null: by default {@code <texture>_glowmask.png} if it exists. */
    protected @Nullable ResourceLocation glow(E e) {
        ResourceLocation g = GeoGuard.glowmask(texture(e));
        return GeoGuard.exists(g) ? g : null;
    }

    /** How far past its hitbox it reaches (wings, a gantry), for culling. */
    protected double reach(E e) {
        return 0.5;
    }

    /** Extra transform before drawing (a tremble, a scale). */
    protected void transform(E e, PoseStack pose, float partial) {
    }

    /** Whether the rig and its clips are loaded. */
    public boolean ready() {
        return GeoGuard.ready(model, animation);
    }

    // --- drawing -----------------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void render(E e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ready()) return;
        ResourceLocation texture = texture(e);
        if (!GeoGuard.exists(texture) && !GeoGuard.exists(texture = defaultTexture)) return;
        pose.pushPose();
        transform(e, pose, partial);
        if (e instanceof GeoAnimatable) {
            inner.render(e, yaw, partial, pose, buffers, light);
        } else {
            float body = e instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : Mth.rotLerp(partial, e.yRotO, e.getYRot());
            pose.mulPose(Axis.YP.rotationDegrees(180f - body));
            if (translucent(e)) prop.translucent();
            prop.draw(pose, buffers, texture, e.tickCount + partial, partial, light);
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRender(E e, Frustum frustum, double x, double y, double z) {
        if (!e.shouldRender(x, y, z)) return false;
        double r = reach(e);
        return frustum.isVisible(e.getBoundingBox().inflate(r, Math.max(1, r * 0.5), r));
    }

    @Override
    public ResourceLocation getTextureLocation(E e) {
        return texture(e);
    }

    // --- GeckoLib, for entities that are GeoAnimatable ---------------------------------------------------------------

    /** The GeckoLib renderer behind it; its type is the entity's, known only at run time. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class Inner<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {
        private final HeavenGeoRenderer owner;

        Inner(EntityRendererProvider.Context ctx, GeoModel<T> model, HeavenGeoRenderer owner) {
            super(ctx, model);
            this.owner = owner;
            addRenderLayer(new Glow<>(this, owner));
            shadowRadius = 0;
        }

        @Override
        public void preRender(PoseStack pose, T e, BakedGeoModel model, MultiBufferSource buffers, VertexConsumer buffer,
                              boolean isReRender, float partialTick, int light, int overlay, int colour) {
            if (!isReRender) owner.bones(e, AllegianceGeo.clip(e, getInstanceId(e), "action"), getGeoModel());
            super.preRender(pose, e, model, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        }

        @Override
        public RenderType getRenderType(T e, ResourceLocation texture, MultiBufferSource buffers, float partialTick) {
            if (owner.translucent(e) || (owner.colour(e, partialTick) >>> 24) < 250) return RenderType.entityTranslucent(texture);
            return super.getRenderType(e, texture, buffers, partialTick);
        }

        @Override
        public Color getRenderColor(T e, float partialTick, int light) {
            int c = owner.colour(e, partialTick);
            return c == 0xFFFFFFFF ? super.getRenderColor(e, partialTick, light) : Color.ofARGB(c >>> 24, (c >> 16) & 255, (c >> 8) & 255, c & 255);
        }

        @Override
        protected float getDeathMaxRotation(T e) {
            return 0f;
        }
    }

    /** The rig, its clips, the entity's texture; the head turned toward where it looks. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class Model<T extends GeoAnimatable> extends GeoModel<T> {
        private final HeavenGeoRenderer owner;

        Model(HeavenGeoRenderer owner) {
            this.owner = owner;
        }

        @Override
        public ResourceLocation getModelResource(T e) {
            return owner.model;
        }

        @Override
        public ResourceLocation getTextureResource(T e) {
            if (!(e instanceof Entity entity)) return owner.defaultTexture;
            ResourceLocation t = owner.texture(entity);
            return GeoGuard.exists(t) ? t : owner.defaultTexture;
        }

        @Override
        public ResourceLocation getAnimationResource(T e) {
            return owner.animation;
        }

        @Override
        public void setCustomAnimations(T e, long instanceId, AnimationState<T> state) {
            super.setCustomAnimations(e, instanceId, state);
            if (!(e instanceof LivingEntity)) return;
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (data == null) return;
            getBone("head").ifPresent(b -> {
                b.setRotX(b.getRotX() + data.headPitch() * Mth.DEG_TO_RAD);
                b.setRotY(b.getRotY() + data.netHeadYaw() * Mth.DEG_TO_RAD);
            });
        }
    }

    /** Its glowmask at full bright (eyes, light), when there is one. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class Glow<T extends Entity & GeoAnimatable> extends GeoRenderLayer<T> {
        private final HeavenGeoRenderer owner;

        Glow(GeoRenderer<T> renderer, HeavenGeoRenderer owner) {
            super(renderer);
            this.owner = owner;
        }

        @Override
        public void render(PoseStack pose, T e, BakedGeoModel model, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                           float partialTick, int light, int overlay) {
            ResourceLocation glow = owner.glow(e);
            if (glow == null) return;
            RenderType eyes = RenderType.eyes(glow);
            getRenderer().reRender(model, pose, buffers, e, eyes, buffers.getBuffer(eyes), partialTick, LightTexture.FULL_BRIGHT, overlay,
                    0xFFFFFFFF);
        }
    }

    /** {@code show(geo, bone, visible)} for subclasses. */
    protected static void show(GeoModel<?> geo, String bone, boolean visible) {
        AllegianceGeo.show(geo, bone, visible);
    }

    public String animName() {
        return animName;
    }
}
