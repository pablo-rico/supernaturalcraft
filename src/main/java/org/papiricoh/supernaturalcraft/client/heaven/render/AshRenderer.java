package org.papiricoh.supernaturalcraft.client.heaven.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.heaven.AshEntity;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;

/** Ash (v0.18): mullet, leather jacket, the bar towel over his shoulder ({@link HeavenAssets#ASH_GEO}). */
public class AshRenderer extends HeavenGeoRenderer<AshEntity> {

    public AshRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "ash", SupernaturalCraft.asResource(HeavenAssets.ASH_TEXTURE), 0.45f);
    }
}
