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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.chorus.ChoirAltarBlockEntity;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;

/**
 * The Choir Altar: the Shattered Hymn turning above it while laid there, and in a storm a shaft of
 * light up into the clouds that can be seen from far across the mountains.
 */
public class ChoirAltarRenderer implements BlockEntityRenderer<ChoirAltarBlockEntity> {

    private static final float BEAM_HEIGHT = 300;

    public ChoirAltarRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ChoirAltarBlockEntity altar, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = altar.getLevel();
        if (level == null) return;
        float time = level.getGameTime() + partial;
        if (altar.armed()) {
            pose.pushPose();
            pose.translate(0.5, 1.3 + 0.08 * Mth.sin(time * 0.08f) + altar.progress() * 0.15, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * (2 + altar.progress() * 3)));
            pose.scale(0.6f, 0.6f, 0.6f);
            Minecraft.getInstance().getItemRenderer().renderStatic(altar.hymn(), ItemDisplayContext.FIXED, 0xF000F0,
                    OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
            pose.popPose();
        }
        float storm = level.getThunderLevel(partial);
        float strength = Math.max(storm, altar.armed() ? 0.6f : 0f);
        if (strength > 0.05f) {
            Vec3 base = new Vec3(0.5, 1.0, 0.5);
            Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(Vec3.atLowerCornerOf(altar.getBlockPos()));
            // Ribbons face the eye along their whole length only if the eye is level with them; aim at the nearest point.
            Vec3 near = new Vec3(0.5, Mth.clamp(eye.y, 1, BEAM_HEIGHT), 0.5);
            // Wider with distance, so it still reads as a shaft of light from across the mountains.
            float width = (float) Math.max(0.8f + 0.4f * strength, Math.sqrt(eye.x * eye.x + eye.z * eye.z) * 0.02);
            BeamFx.draw(pose, buffers, base, base.add(0, BEAM_HEIGHT, 0), width, 0xFFE7A0,
                    Math.min(1f, 0.7f * strength + 0.2f), time / 20f, eye.subtract(near).multiply(1, 0, 1).add(near));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ChoirAltarBlockEntity altar) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    @Override
    public boolean shouldRender(ChoirAltarBlockEntity altar, Vec3 camera) {
        Vec3 c = Vec3.atCenterOf(altar.getBlockPos());
        double dx = c.x - camera.x, dz = c.z - camera.z;
        return dx * dx + dz * dz < (double) getViewDistance() * getViewDistance();
    }

    @Override
    public AABB getRenderBoundingBox(ChoirAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).expandTowards(0, BEAM_HEIGHT, 0).inflate(2);
    }
}
