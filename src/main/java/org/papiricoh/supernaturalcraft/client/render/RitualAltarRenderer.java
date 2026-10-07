package org.papiricoh.supernaturalcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

/** Offerings hover in a slow ring above the bowl; while a ritual runs they spin faster and rise. */
public class RitualAltarRenderer implements BlockEntityRenderer<RitualAltarBlockEntity> {

    public RitualAltarRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(RitualAltarBlockEntity altar, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = altar.getLevel();
        if (level == null) return;
        int count = 0;
        for (ItemStack s : altar.offerings()) if (!s.isEmpty()) count++;
        if (count == 0) return;
        float time = level.getGameTime() + partial;
        float channel = altar.isChanneling() ? altar.progressFraction() : 0f;
        float speed = altar.isChanneling() ? 4f + channel * 10f : 1.2f;
        float radius = count == 1 ? 0f : 0.32f * (1f - channel * 0.6f);
        int i = 0;
        for (ItemStack stack : altar.offerings()) {
            if (stack.isEmpty()) continue;
            float angle = time * speed + i * 360f / count;
            pose.pushPose();
            pose.translate(0.5, 1.05 + 0.05 * Mth.sin((time + i * 7) * 0.1f) + channel * 0.6, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(radius, 0, 0);
            pose.mulPose(Axis.YP.rotationDegrees(-angle + time * 2));
            pose.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0,
                    OverlayTexture.NO_OVERLAY, pose, buffers, level, i);
            pose.popPose();
            i++;
        }
    }
}
