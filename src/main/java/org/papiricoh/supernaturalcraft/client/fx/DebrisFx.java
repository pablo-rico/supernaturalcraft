package org.papiricoh.supernaturalcraft.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.DebrisPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies of blocks that broke away from an arena floor, tumbling down out of sight. Purely visual:
 * the server already turned the real blocks to air.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DebrisFx {

    private static final int LIFE = 70;
    private static final float GRAVITY = 0.045f;

    private record Piece(BlockState state, Vec3 from, Vec3 velocity, Vec3 axis, float spin, long start, int delay) {
    }

    private static final List<Piece> PIECES = new ArrayList<>();

    private DebrisFx() {
    }

    public static void add(DebrisPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        long now = level.getGameTime();
        RandomSource random = RandomSource.create(now);
        for (int i = 0; i < payload.positions().length; i++) {
            BlockState state = Block.stateById(payload.states()[i]);
            if (state.getRenderShape() != RenderShape.MODEL) continue;
            BlockPos pos = BlockPos.of(payload.positions()[i]);
            Vec3 v = new Vec3((random.nextDouble() - 0.5) * 0.12, random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.12);
            Vec3 axis = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5).normalize();
            // Higher blocks let go a moment later, so the floor seems to crumble from below.
            PIECES.add(new Piece(state, Vec3.atLowerCornerOf(pos), v, axis, 4 + random.nextFloat() * 10, now, random.nextInt(8)));
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        PIECES.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || PIECES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        PIECES.removeIf(p -> now - p.start > LIFE + p.delay);
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        var dispatcher = mc.getBlockRenderer();
        for (Piece p : PIECES) {
            float t = Math.max(0, now - p.start - p.delay + partial);
            Vec3 at = p.from.add(p.velocity.scale(t)).add(0, -0.5 * GRAVITY * t * t, 0);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(at.add(0.5, 0.5, 0.5)));
            pose.pushPose();
            pose.translate(at.x - cam.x + 0.5, at.y - cam.y + 0.5, at.z - cam.z + 0.5);
            pose.mulPose(Axis.of(p.axis.toVector3f()).rotationDegrees(p.spin * t));
            float shrink = 1 - Math.max(0, t - LIFE * 0.7f) / (LIFE * 0.3f);
            pose.scale(Math.max(0.05f, shrink), Math.max(0.05f, shrink), Math.max(0.05f, shrink));
            pose.translate(-0.5, -0.5, -0.5);
            dispatcher.renderSingleBlock(p.state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        buffers.endBatch();
    }
}
