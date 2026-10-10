package org.papiricoh.supernaturalcraft.client.heaven.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.ReprogrammingChairEntity;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import software.bernie.geckolib.model.GeoModel;

/**
 * Naomi's reprogramming chair (v0.18, {@link HeavenAssets#CHAIR_GEO}): its leather straps closed only while someone is strapped
 * in (or while they buckle), the drill gantry overhead.
 */
public class ChairRenderer extends HeavenGeoRenderer<ReprogrammingChairEntity> {

    public ChairRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "reprogramming_chair", SupernaturalCraft.asResource(HeavenAssets.CHAIR_TEXTURE), 0.6f);
    }

    @Override
    protected void bones(ReprogrammingChairEntity e, String clip, GeoModel<?> geo) {
        boolean held = e.strapped() || "strap".equals(clip) || "drill_down".equals(clip);
        show(geo, "straps", held);
        show(geo, "straps_open", !held);
    }

    @Override
    protected double reach(ReprogrammingChairEntity e) {
        return 1.5;
    }
}
