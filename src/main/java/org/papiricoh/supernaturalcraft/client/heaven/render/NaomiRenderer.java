package org.papiricoh.supernaturalcraft.client.heaven.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.naomi.NaomiEntity;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import software.bernie.geckolib.model.GeoModel;


/**
 * Naomi (v0.18): her grey suit and white coat ({@link HeavenAssets#NAOMI_GEO}), her eyes lit by the glowmask when there is one;
 * her props ({@link HeavenAssets#NAOMI_PROP_CLIPS}: the drill, the palm lights) only while one of their clips plays. Translucent.
 */
public class NaomiRenderer extends HeavenGeoRenderer<NaomiEntity> {

    public NaomiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "naomi", SupernaturalCraft.asResource(HeavenAssets.NAOMI_TEXTURE), 0.45f);
    }

    @Override
    protected void bones(NaomiEntity e, String clip, GeoModel<?> geo) {
        // Her props (the drill, the light in her palms, the light leaving her) only while one of their clips plays.
        for (var prop : HeavenAssets.NAOMI_PROP_CLIPS.entrySet()) show(geo, prop.getKey(), prop.getValue().contains(clip));
        if (e.drillOut()) show(geo, "drill", true);
    }

    @Override
    protected boolean translucent(NaomiEntity e) {
        return true;
    }
}
