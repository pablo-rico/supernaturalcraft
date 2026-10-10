package org.papiricoh.supernaturalcraft.client.heaven.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.client.heaven.render.figure.TintedBuffers;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * A memo of Zachariah's Paper Storm (v0.18): a sheet of Heaven's stationery (the Heavenly Form's sprite) fluttering edge over
 * edge along its flight, lit a little from within so it reads against the office's white.
 */
public class MemoRenderer<E extends Entity> extends EntityRenderer<E> {

    private final ItemRenderer items;
    private ItemStack sheet = ItemStack.EMPTY;

    public MemoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        items = ctx.getItemRenderer();
        shadowRadius = 0.1f;
    }

    @Override
    public void render(E e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (sheet.isEmpty()) sheet = new ItemStack(AllItems.HEAVENLY_FORM.get());
        float t = e.tickCount + partial;
        pose.pushPose();
        pose.translate(0, 0.15, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        // Flutter: rocking about its length, tumbling slowly end over end.
        pose.mulPose(Axis.XP.rotationDegrees(70 + Mth.sin(t * 0.9f + e.getId()) * 35));
        pose.mulPose(Axis.ZP.rotationDegrees(t * 14 % 360));
        pose.scale(0.7f, 0.7f, 0.7f);
        items.renderStatic(sheet, ItemDisplayContext.GROUND, TintedBuffers.lift(light, 11), OverlayTexture.NO_OVERLAY, pose, buffers,
                e.level(), e.getId());
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(E e) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
