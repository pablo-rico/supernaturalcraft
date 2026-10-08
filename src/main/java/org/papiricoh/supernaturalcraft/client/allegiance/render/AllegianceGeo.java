package org.papiricoh.supernaturalcraft.client.allegiance.render;

import net.minecraft.resources.ResourceLocation;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.model.GeoModel;

/** Small helpers shared by the allegiance renderers (v0.13): bone visibility, the clip playing, guarded resources. */
public final class AllegianceGeo {

    private AllegianceGeo() {
    }

    /** {@code AllegianceAssets} paths are relative to {@code assets/supernaturalcraft/}. */
    public static ResourceLocation asset(String path) {
        return SupernaturalCraft.asResource(path);
    }

    public static void show(GeoModel<?> model, String bone, boolean visible) {
        model.getBone(bone).ifPresent(b -> {
            b.setHidden(!visible);
            b.setChildrenHidden(!visible);
        });
    }

    /** The clip the {@code controller} of this animatable plays now ("" for none): {@code animation.x.<clip>} → {@code <clip>}. */
    public static String clip(GeoAnimatable animatable, long instanceId, String controller) {
        AnimatableManager<?> manager = animatable.getAnimatableInstanceCache().getManagerForId(instanceId);
        if (manager == null) return "";
        AnimationController<?> c = manager.getAnimationControllers().get(controller);
        if (c == null || c.getCurrentAnimation() == null || c.getAnimationState() == AnimationController.State.STOPPED) return "";
        String name = c.getCurrentAnimation().animation().name();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    /** Whether a model and its animation file are both baked (an asset that has not arrived yet draws a stand-in). */
    public static boolean ready(String geo, String anim) {
        return GeoGuard.ready(asset(geo), anim == null ? null : asset(anim));
    }
}
