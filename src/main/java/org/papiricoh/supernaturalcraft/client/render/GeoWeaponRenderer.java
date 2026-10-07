package org.papiricoh.supernaturalcraft.client.render;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Draws any GeckoLib weapon from geo/item/&lt;id&gt;, with its glowmask lit at full brightness. */
public class GeoWeaponRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {

    public GeoWeaponRenderer(T item) {
        super(new DefaultedItemGeoModel<>(BuiltInRegistries.ITEM.getKey(item)));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
