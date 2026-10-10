package org.papiricoh.supernaturalcraft.client.heaven.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.entity.boss.zachariah.ZachariahEntity;
import org.papiricoh.supernaturalcraft.heaven.HeavenAssets;
import software.bernie.geckolib.model.GeoModel;


/**
 * Zachariah (v0.18): the balding middle manager in grey ({@link HeavenAssets#ZACHARIAH_GEO}), drawn translucent for his six
 * burnt-gold wings, which show only while the server says so (from phase III) or while he reveals them; the stamp only in its
 * clips; his eyes lit by the glowmask.
 */
public class ZachariahRenderer extends HeavenGeoRenderer<ZachariahEntity> {

    public ZachariahRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "zachariah", SupernaturalCraft.asResource(HeavenAssets.ZACHARIAH_TEXTURE), 0.5f);
    }

    /** Whether his wings show: the synced flag, or the reveal while it plays. */
    public static boolean wings(ZachariahEntity e, String clip) {
        return "wings_reveal".equals(clip) || e.wingsShown();
    }

    @Override
    protected void bones(ZachariahEntity e, String clip, GeoModel<?> geo) {
        show(geo, "wings", wings(e, clip));
        for (var prop : HeavenAssets.ZACHARIAH_PROP_CLIPS.entrySet()) show(geo, prop.getKey(), prop.getValue().contains(clip));
    }

    @Override
    protected boolean translucent(ZachariahEntity e) {
        return true;
    }

    @Override
    protected double reach(ZachariahEntity e) {
        return e.wingsShown() ? 3.0 : 0.5;
    }
}
