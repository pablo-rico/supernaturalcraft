package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A GeckoLib model drawn as a prop where the caller has put the pose (projectiles that are not GeckoLib entities: the
 * lances, steel feathers, the halo's spears). One shared animatable per model, looping its {@code idle} clip if it has one.
 * Draws nothing until the model is baked; the texture can be swapped per draw.
 */
public final class GeoProp implements GeoAnimatable {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ResourceLocation model;
    private final @Nullable ResourceLocation animation;
    private final @Nullable String idle;
    private ResourceLocation texture;
    private double time;
    private final GeoObjectRenderer<GeoProp> renderer;
    private boolean translucent, fullBright;

    public GeoProp(ResourceLocation model, ResourceLocation texture, @Nullable ResourceLocation animation, @Nullable String idle) {
        this.model = model;
        this.texture = texture;
        this.animation = animation;
        this.idle = idle;
        renderer = new GeoObjectRenderer<>(new Model()) {
            @Override
            public RenderType getRenderType(GeoProp animatable, ResourceLocation tex, @Nullable MultiBufferSource buffers, float partialTick) {
                return translucent ? RenderType.entityTranslucent(tex) : fullBright ? RenderType.entityCutoutNoCull(tex) : super.getRenderType(animatable, tex, buffers, partialTick);
            }
        };
    }

    public GeoProp translucent() {
        translucent = true;
        return this;
    }

    public GeoProp fullBright() {
        fullBright = true;
        return this;
    }

    /** Whether the model (and its animation, if any) is loaded. */
    public boolean ready() {
        return GeoGuard.ready(model, animation);
    }

    /**
     * Draws the model with its origin at the pose's origin (undoing the object renderer's centring in a block), with
     * {@code texture}, at {@code light} (full bright if this prop was made so).
     */
    public void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, double time, float partial, int light) {
        if (!ready()) return;
        this.texture = GeoGuard.exists(texture) ? texture : this.texture;
        this.time = time;
        pose.pushPose();
        pose.translate(-0.5, -0.51, -0.5);
        renderer.render(pose, this, buffers, null, null, fullBright ? 0xF000F0 : light, partial);
        pose.popPose();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (idle == null || animation == null) return;
        RawAnimation loop = RawAnimation.begin().thenLoop(idle);
        controllers.add(new AnimationController<>(this, "main", 0, s -> s.setAndContinue(loop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return time;
    }

    private final class Model extends GeoModel<GeoProp> {
        @Override
        public ResourceLocation getModelResource(GeoProp p) {
            return model;
        }

        @Override
        public ResourceLocation getTextureResource(GeoProp p) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(GeoProp p) {
            return animation != null ? animation : model;
        }
    }
}
