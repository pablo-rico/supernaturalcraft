package org.papiricoh.supernaturalcraft.client.allegiance.render;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoObjectRenderer;
import software.bernie.geckolib.util.Color;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * A GeckoLib model worn by a player (wings, regalia, true form): one {@link Wearable} per player carries that player's
 * clip and time; one renderer per model draws them all, tinted per draw.
 */
public final class PlayerGeo {

    private PlayerGeo() {
    }

    /** What one player's copy of a model is doing: the clip ({@code animation.<prefix>.<clip>}), whether it holds, the time. */
    public static final class Wearable implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private final String prefix;
        String clip;
        boolean hold;
        double time;
        /** Bones posed by hand after the clip (the regalia's cloak). */
        Consumer<GeoModel<Wearable>> posing;

        public Wearable(String prefix, String clip) {
            this.prefix = prefix;
            this.clip = clip;
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            if (prefix == null) return;
            controllers.add(new AnimationController<>(this, "main", 5, state -> {
                if (clip == null) return PlayState.STOP;
                String name = "animation." + prefix + "." + clip;
                return state.setAndContinue(hold ? RawAnimation.begin().thenPlayAndHold(name) : RawAnimation.begin().thenLoop(name));
            }));
        }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() {
            return cache;
        }

        @Override
        public double getTick(Object object) {
            return time;
        }
    }

    /** A model with no animation file needs none ({@code anim} null): the controller is then never added. */
    public static final class Model extends GeoModel<Wearable> {
        private final ResourceLocation geo, anim;
        ResourceLocation texture;

        public Model(ResourceLocation geo, ResourceLocation texture, ResourceLocation anim) {
            this.geo = geo;
            this.texture = texture;
            this.anim = anim;
        }

        @Override
        public ResourceLocation getModelResource(Wearable w) {
            return geo;
        }

        @Override
        public ResourceLocation getTextureResource(Wearable w) {
            return texture;
        }

        @Override
        public ResourceLocation getAnimationResource(Wearable w) {
            return anim;
        }

        @Override
        public void setCustomAnimations(Wearable w, long instanceId, software.bernie.geckolib.animation.AnimationState<Wearable> state) {
            if (w.posing != null) w.posing.accept(this);
        }
    }

    /** The renderer, with a colour set before each draw. */
    public static final class Renderer extends GeoObjectRenderer<Wearable> {
        int colour = 0xFFFFFFFF;

        public Renderer(Model model) {
            super(model);
        }

        public Model model() {
            return (Model) getGeoModel();
        }

        @Override
        public Color getRenderColor(Wearable animatable, float partialTick, int packedLight) {
            return new Color(colour);
        }
    }
}
