package org.papiricoh.supernaturalcraft.client.gabriel;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.chuck.render.OptionalGlowLayer;
import org.papiricoh.supernaturalcraft.entity.boss.gabriel.GabrielAssets;
import org.papiricoh.supernaturalcraft.reward.gabriel.TricksterRemoteItem;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * The Trickster's Remote in hand and on the ground (v0.14): its own GeckoLib model, placed by the transforms of the art's item
 * model (the inventory shows the flat icon through {@code separate_transforms}). Nothing until the model is baked.
 */
public class RemoteItemRenderer extends GeoItemRenderer<TricksterRemoteItem> {

    static final ResourceLocation MODEL = SupernaturalCraft.asResource(GabrielAssets.REMOTE_GEO),
            TEXTURE = SupernaturalCraft.asResource(GabrielAssets.REMOTE_TEXTURE),
            ANIMATION = SupernaturalCraft.asResource("animations/item/trickster_remote.animation.json");

    public RemoteItemRenderer() {
        super(new GeoModel<>() {
            @Override
            public ResourceLocation getModelResource(TricksterRemoteItem item) {
                return MODEL;
            }

            @Override
            public ResourceLocation getTextureResource(TricksterRemoteItem item) {
                return TEXTURE;
            }

            @Override
            public ResourceLocation getAnimationResource(TricksterRemoteItem item) {
                return ANIMATION;
            }
        });
        addRenderLayer(new OptionalGlowLayer<>(this));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!GeoGuard.ready(MODEL, null)) return;
        super.renderByItem(stack, context, pose, buffers, light, overlay);
    }
}
