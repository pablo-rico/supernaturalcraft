package org.papiricoh.supernaturalcraft.client.amara;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;
import org.papiricoh.supernaturalcraft.client.fx.TubeFx;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraEntity;
import org.papiricoh.supernaturalcraft.network.AmaraFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the Darkness's attacks: tentacles that rise and slam, sweep and grasp; spikes; the corona
 * flare's turning beam; rings of black light. Each effect runs from the tick its payload arrived.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AmaraFxRenderer {

    public static final ResourceLocation RING = SupernaturalCraft.asResource("textures/effect/energy_ring.png");
    private record Active(AmaraFxPayload fx, long start) {
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    private AmaraFxRenderer() {
    }

    public static void add(AmaraFxPayload fx) {
        var level = Minecraft.getInstance().level;
        if (level != null) ACTIVE.add(new Active(fx, level.getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(a -> now - a.start > a.fx.duration() + 10);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float time = (now + partial) / 20f;
        for (Active a : ACTIVE) {
            Entity e = mc.level.getEntity(a.fx.boss());
            if (!(e instanceof AmaraEntity boss)) continue;
            float t = (now - a.start + partial) / Math.max(1, a.fx.duration());
            draw(pose, buffers, boss, a.fx, Mth.clamp(t, 0, 1.2f), cam, partial, time);
        }
        buffers.endBatch();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, AmaraEntity boss, AmaraFxPayload fx, float t, Vec3 cam,
                             float partial, float time) {
        Vec3 body = boss.getPosition(partial);
        Vec3 core = body.add(0, AmaraEntity.MASS_CENTER, 0);
        Vec3 p = fx.point();
        Vec3 root = rootOf(body, p);
        switch (fx.kind()) {
            case AmaraFxPayload.SLAM -> {
                // Rise over the spot (0-0.6), crash down (0.6-0.7), lie there, then withdraw.
                float lift = t < 0.6f ? t / 0.6f : t < 0.7f ? 1 - (t - 0.6f) / 0.1f : 0;
                float reach = t > 0.95f ? Math.max(0, 1 - (t - 0.95f) * 8) : 1;
                Vec3 tip = root.lerp(p.add(0, lift * 7, 0), reach);
                Vec3 ctrl = root.lerp(tip, 0.5).add(0, 4 + lift * 3, 0);
                TubeFx.tentacle(pose, buffers, root.subtract(cam), ctrl.subtract(cam), tip.subtract(cam), 0.9f, 0.4f, time);
            }
            case AmaraFxPayload.SWEEP -> {
                float a = (fx.yaw() + t * 360f) * Mth.DEG_TO_RAD;
                Vec3 tip = body.add(Mth.cos(a) * fx.radius(), 0.4 - (body.y - p.y), Mth.sin(a) * fx.radius());
                Vec3 r0 = rootOf(body, tip);
                Vec3 ctrl = r0.lerp(tip, 0.5).add(0, 1.5, 0);
                TubeFx.tentacle(pose, buffers, r0.subtract(cam), ctrl.subtract(cam), tip.subtract(cam), 1.0f, 0.2f, time);
            }
            case AmaraFxPayload.GRASP -> {
                float reach = Math.min(1, t * 6);
                Vec3 tip = root.lerp(p.add(0, 1.0, 0), reach);
                Vec3 ctrl = root.lerp(tip, 0.5).add(0, 3, 0);
                TubeFx.tentacle(pose, buffers, root.subtract(cam), ctrl.subtract(cam), tip.subtract(cam), 0.7f, 0.15f, time);
                if (reach >= 1) TubeFx.ring(pose, buffers, RING, p.add(0, 0.9, 0).subtract(cam), 0.8f, 0.3f, 0.6f, 0x6A3FA8, 0.8f, time);
            }
            case AmaraFxPayload.SPIKE -> {
                float h = t < 0.3f ? t / 0.3f : t > 0.8f ? Math.max(0, 1 - (t - 0.8f) * 5) : 1;
                Vec3 top = p.add(0, 3.5 * h, 0);
                TubeFx.tube(pose, buffers, List.of(p.subtract(cam), p.add(0, 1.2 * h, 0).subtract(cam), top.subtract(cam)),
                        new float[]{0.7f, 0.45f, 0.02f}, time);
            }
            case AmaraFxPayload.FLARE -> {
                float a = (fx.yaw() + t * 360f) * Mth.DEG_TO_RAD;
                Vec3 from = core.subtract(cam);
                Vec3 to = new Vec3(core.x + Mth.cos(a) * fx.radius(), p.y + 0.6, core.z + Mth.sin(a) * fx.radius()).subtract(cam);
                BeamFx.draw(pose, buffers, from, to, 0.9f, 0xFFE9B8, 0.95f, time);
            }
            case AmaraFxPayload.RING_OUT, AmaraFxPayload.RING_IN -> {
                float k = fx.kind() == AmaraFxPayload.RING_OUT ? t : 1 - t;
                float r = Math.max(0.5f, fx.radius() * k);
                TubeFx.ring(pose, buffers, RING, new Vec3(core.x, p.y + 0.05, core.z).subtract(cam), r, 1.2f, 2.5f,
                        fx.kind() == AmaraFxPayload.RING_OUT ? 0x8A5AE0 : 0x2A1240, 0.9f * (1.1f - t * 0.5f), time);
            }
            default -> {
            }
        }
    }

    private static Vec3 rootOf(Vec3 body, Vec3 toward) {
        Vec3 flat = toward.subtract(body).multiply(1, 0, 1);
        Vec3 dir = flat.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
        return body.add(dir.scale(2.2)).add(0, AmaraEntity.MASS_CENTER - 2.4, 0);
    }
}
