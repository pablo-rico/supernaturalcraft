package org.papiricoh.supernaturalcraft.client.chuck.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.chuck.fx.ChuckText;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.FloatingWordEntity;

/**
 * A word the Author writes into the air: big typewriter letters in ink, always turned to the viewer, trembling
 * slightly (the ink is still wet), with a faint smear of itself behind. {@link FloatingWordEntity#size()} is the
 * letter height in blocks.
 */
public class FloatingWordRenderer extends EntityRenderer<FloatingWordEntity> {

    public FloatingWordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0f;
    }

    @Override
    public void render(FloatingWordEntity word, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        String text = word.text();
        if (text.isEmpty()) return;
        Font font = Minecraft.getInstance().font;
        Component line = Component.literal(text).withStyle(ChuckText.style());
        double t = word.level().getGameTime() + partialTick;
        float size = Math.max(0.1f, word.size());
        // Jitter: a new small offset every two ticks.
        long k = (long) (t / 2) * 73 + word.getId() * 31L;
        float jx = (((k * 1103515245L + 12345) >>> 16) & 0xFF) / 255f - 0.5f, jy = (((k * 214013L + 2531011) >>> 16) & 0xFF) / 255f - 0.5f;
        int ink = 0xFF000000 | (word.color() & 0xFFFFFF);
        pose.pushPose();
        pose.translate(0, word.getBbHeight() / 2, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees(2.5f * Mth.sin((float) t * 0.07f + word.getId())));
        float s = size / 7f;
        pose.scale(s, -s, s);
        float x = -font.width(line) / 2f, y = -4f;
        // The smear: the same word, paler and a little behind.
        font.drawInBatch(line, x + 0.6f, y + 0.5f, (0x55 << 24) | (word.color() & 0xFFFFFF), false, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.translate(0, 0, 0.02f / s);
        font.drawInBatch(line, x + jx * 0.6f, y + jy * 0.6f, ink, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.popPose();
        super.render(word, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(FloatingWordEntity word, Frustum frustum, double x, double y, double z) {
        if (!word.shouldRender(x, y, z)) return false;
        float reach = word.size() * Math.max(1, word.text().length()) * 0.4f + 1;
        return frustum.isVisible(word.getBoundingBox().inflate(reach));
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingWordEntity word) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
