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
import net.minecraft.world.phys.Vec3;
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
        // The client's own charge (v0.21) drives the spin when one is running; the block entity's progress otherwise.
        float charge = org.papiricoh.supernaturalcraft.client.magic.fx.RitualChargeFx.channel(altar.getBlockPos());
        float channel = charge >= 0 ? charge : altar.isChanneling() ? altar.progressFraction() : -1f;
        int i = 0;
        for (ItemStack stack : altar.offerings()) {
            if (stack.isEmpty()) continue;
            Vec3 at = offering(i, count, time, channel);
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(Axis.YP.rotationDegrees(time * 2));
            pose.scale(0.4f, 0.4f, 0.4f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0,
                    OverlayTexture.NO_OVERLAY, pose, buffers, level, i);
            pose.popPose();
            i++;
        }
    }

    private static float angle(int i, int count, float time, float channel) {
        float speed = channel >= 0 ? 4f + channel * 10f : 1.2f;
        return time * speed + i * 360f / count;
    }

    /**
     * Where offering {@code i} of {@code count} hovers, relative to the altar block's corner, at {@code time} (game time plus
     * partial); {@code channel} is how far a running rite is along (-1 when none is). Shared with the rite's streams of light.
     */
    public static Vec3 offering(int i, int count, float time, float channel) {
        float c = Math.max(0, channel);
        float radius = count == 1 ? 0f : 0.32f * (1f - c * 0.6f);
        float a = angle(i, count, time, channel) * Mth.DEG_TO_RAD;
        return new Vec3(0.5 + Mth.cos(a) * radius, 1.05 + 0.05 * Mth.sin((time + i * 7) * 0.1f) + c * 0.6, 0.5 - Mth.sin(a) * radius);
    }
}
