package org.papiricoh.supernaturalcraft.client.colt;

import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.reward.ColtItem;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Draws the Colt: the model, its glowing rounds and flame, the motto's gold, and in first person the hands. */
public class ColtRenderer extends GeoItemRenderer<ColtItem> {

    public ColtRenderer() {
        super(new ColtModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new ColtEngravingLayer(this));
        addRenderLayer(new ColtArmsLayer(this));
    }

    public boolean firstPerson() {
        return renderPerspective != null && renderPerspective.firstPerson();
    }

    public ItemDisplayContext perspective() {
        return renderPerspective;
    }

    public long gunId() {
        return GeoItem.getId(currentItemStack);
    }

    public ItemStack stack() {
        return currentItemStack;
    }
}
