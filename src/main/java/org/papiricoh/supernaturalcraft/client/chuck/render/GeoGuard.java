package org.papiricoh.supernaturalcraft.client.chuck.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.util.HashMap;
import java.util.Map;

/**
 * GeckoLib throws when a model or animation file is missing; the Author's art arrives on its own schedule, so every
 * renderer here asks first and draws nothing until its files are loaded.
 */
public final class GeoGuard {

    private static final Map<ResourceLocation, Boolean> TEXTURES = new HashMap<>();

    private GeoGuard() {
    }

    /** Whether GeckoLib has baked {@code geo/entity/<name>.geo.json} and the animation file (null: none needed). */
    public static boolean ready(ResourceLocation model, ResourceLocation animation) {
        return GeckoLibCache.getBakedModels().containsKey(model)
                && (animation == null || GeckoLibCache.getBakedAnimations().containsKey(animation));
    }

    public static ResourceLocation model(String name) {
        return ResourceLocation.fromNamespaceAndPath(org.papiricoh.supernaturalcraft.SupernaturalCraft.MODID, "geo/entity/" + name + ".geo.json");
    }

    public static ResourceLocation animation(String name) {
        return ResourceLocation.fromNamespaceAndPath(org.papiricoh.supernaturalcraft.SupernaturalCraft.MODID, "animations/entity/" + name + ".animation.json");
    }

    /** Whether a texture (or any resource) exists; remembered, since the answer only changes with the resource packs. */
    public static boolean exists(ResourceLocation texture) {
        return TEXTURES.computeIfAbsent(texture, t -> Minecraft.getInstance().getResourceManager().getResource(t).isPresent());
    }

    /** {@code textures/x.png} → {@code textures/x_glowmask.png}. */
    public static ResourceLocation glowmask(ResourceLocation texture) {
        String p = texture.getPath();
        return texture.withPath(p.substring(0, p.length() - 4) + "_glowmask.png");
    }

    /** Forgets what {@link #exists} learned (after a resource reload). */
    public static void forget() {
        TEXTURES.clear();
    }
}
