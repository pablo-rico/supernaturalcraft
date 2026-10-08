package org.papiricoh.supernaturalcraft.client.michael.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * The Lance of Michael in hand, on the ground and in the inventory, and the borrowed one (brighter): the one lance model,
 * with the transforms the art's item model gives it. Draws nothing until the model is baked.
 */
public class LanceItemRenderer<T extends Item & GeoAnimatable> extends GeoItemRenderer<T> {

    public LanceItemRenderer() {
        super(new Model<>());
        addRenderLayer(new OptionalGlowLayer<>(this));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!GeoGuard.ready(LanceRenderer.MODEL, LanceRenderer.ANIMATION)) return;
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }

    static class Model<T extends Item & GeoAnimatable> extends GeoModel<T> {
        @Override
        public ResourceLocation getModelResource(T item) {
            return LanceRenderer.MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(T item) {
            return item == AllItems.BORROWED_LANCE.get() && GeoGuard.exists(LanceRenderer.BORROWED) ? LanceRenderer.BORROWED : LanceRenderer.TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(T item) {
            return LanceRenderer.ANIMATION;
        }
    }
}
