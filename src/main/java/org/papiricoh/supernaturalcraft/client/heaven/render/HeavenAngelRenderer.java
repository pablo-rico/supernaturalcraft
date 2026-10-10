package org.papiricoh.supernaturalcraft.client.heaven.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import software.bernie.geckolib.model.GeoModel;

/**
 * Heaven's office angels (v0.18): Naomi's guards (dark suit, earpiece, sunglasses) and Zachariah's clerks (shirt sleeves, vest,
 * sleeve garters, visor), both on the Host's rig ({@link HeavenAssets#HOST_GEO}) in their own texture. Angels in vessels at
 * work: never the Host's helm, plume or open wings; a guard keeps its folded wings, a clerk does not. Their
 * eyes glow with the Host's glowmask.
 */
public class HeavenAngelRenderer<E extends Entity> extends HeavenGeoRenderer<E> {

    private static final String[] NEVER = {"helmet", "plume", "wings_open"};
    private static final ResourceLocation EYES = SupernaturalCraft.asResource("textures/entity/host_angel_glowmask.png");

    private final boolean guard;

    private HeavenAngelRenderer(EntityRendererProvider.Context ctx, String texture, boolean guard) {
        super(ctx, "host_angel", SupernaturalCraft.asResource(texture), 0.5f);
        this.guard = guard;
    }

    public static <E extends Entity> HeavenAngelRenderer<E> guard(EntityRendererProvider.Context ctx) {
        return new HeavenAngelRenderer<>(ctx, HeavenAssets.GUARD_TEXTURE, true);
    }

    public static <E extends Entity> HeavenAngelRenderer<E> clerk(EntityRendererProvider.Context ctx) {
        return new HeavenAngelRenderer<>(ctx, HeavenAssets.CLERK_TEXTURE, false);
    }

    @Override
    protected void bones(E e, String clip, GeoModel<?> geo) {
        for (String bone : NEVER) show(geo, bone, false);
        // A guard keeps its folded wings; a clerk at his desk has put them away.
        show(geo, "wings_folded", guard);
    }

    @Override
    protected @Nullable ResourceLocation glow(E e) {
        ResourceLocation own = GeoGuard.glowmask(texture(e));
        if (GeoGuard.exists(own)) return own;
        return GeoGuard.exists(EYES) ? EYES : null;
    }
}
