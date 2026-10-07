package org.papiricoh.supernaturalcraft.client.chorus;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.amara.AmaraFxRenderer;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;
import org.papiricoh.supernaturalcraft.client.fx.TubeFx;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chorus.ChorusGeometry;
import org.papiricoh.supernaturalcraft.network.ChorusFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the Broken Chorus's light: eyes locking their gaze, beams, rings racing over the
 * platform, the wheels' sweeping eye-beams, a burning wheel rolling, the Hymn's notes running
 * down to their bells, and the core's last pulses.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChorusFxRenderer {

    private record Active(ChorusFxPayload fx, long start) {
    }

    private static final List<Active> ACTIVE = new ArrayList<>();

    private ChorusFxRenderer() {
    }

    public static void add(ChorusFxPayload fx) {
        var level = Minecraft.getInstance().level;
        if (level != null) ACTIVE.add(new Active(fx, level.getGameTime()));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long now = mc.level.getGameTime();
        rays(mc, event, partial);
        if (ACTIVE.isEmpty()) return;
        ACTIVE.removeIf(a -> now - a.start > a.fx.duration() + 6);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float time = (now + partial) / 20f;
        for (Active a : ACTIVE) {
            Entity e = mc.level.getEntity(a.fx.boss());
            if (!(e instanceof ChorusEntity boss)) continue;
            float t = Mth.clamp((now - a.start + partial) / Math.max(1, a.fx.duration()), 0, 1);
            draw(mc, pose, buffers, boss, a.fx, t, cam, partial, time);
        }
        buffers.endBatch();
    }

    /**
     * Its heart's light: a slow crown of rays round the core, faint while the choir still stands,
     * blazing once only the core is left.
     */
    private static void rays(Minecraft mc, RenderLevelStageEvent event, float partial) {
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ChorusEntity boss) || boss.distanceToSqr(cam) > 160 * 160 || boss.state() == ChorusEntity.EMERGING) continue;
            if (buffers == null) buffers = mc.renderBuffers().bufferSource();
            boolean last = boss.phase() == 4;
            Vec3 core = boss.getPosition(partial).add(ChorusGeometry.coreOffset());
            float time = (float) boss.time(partial);
            int n = last ? 12 : 6;
            for (int i = 0; i < n; i++) {
                double yaw = time * 0.01 + i * Math.PI * 2 / n, pitch = Math.sin(time * 0.013 + i * 1.7) * 0.6;
                float len = (last ? 6.5f : 3.5f) + 1.5f * Mth.sin(time * 0.05f + i);
                Vec3 d = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
                BeamFx.draw(event.getPoseStack(), buffers, core.subtract(cam), core.add(d.scale(len)).subtract(cam), last ? 0.7f : 0.35f,
                        0xFFE7A0, last ? 0.55f : 0.25f, time / 20f);
            }
        }
        if (buffers != null) buffers.endBatch();
    }

    /** Where an eye is right now, in the world. */
    static Vec3 eye(ChorusEntity boss, int eye, float partial) {
        double time = boss.time(partial);
        return boss.getPosition(partial).add(ChorusGeometry.eyeOffset(eye, boss.wheelRot(ChorusGeometry.wheelOf(eye), time)));
    }

    private static void draw(Minecraft mc, PoseStack pose, MultiBufferSource buffers, ChorusEntity boss, ChorusFxPayload fx, float t,
                             Vec3 cam, float partial, float time) {
        Vec3 body = boss.getPosition(partial);
        Vec3 core = body.add(ChorusGeometry.coreOffset());
        switch (fx.kind()) {
            case ChorusFxPayload.GAZE -> {
                Vec3 from = eye(boss, fx.arg(), partial);
                Entity target = mc.level.getEntity(fx.target());
                Vec3 to = target != null ? target.getEyePosition(partial) : fx.aux();
                if (t < 0.97f) {
                    BeamFx.draw(pose, buffers, from.subtract(cam), to.subtract(cam), 0.04f + 0.12f * t, fx.color(), 0.25f + 0.6f * t, time);
                } else {
                    BeamFx.draw(pose, buffers, from.subtract(cam), to.subtract(cam), 0.9f, 0xFFFFFF, 1f, time);
                }
                // A glint on the eye itself, swelling as it charges.
                float g = 0.6f + 1.6f * t;
                for (Vec3 d : new Vec3[]{new Vec3(g, 0, 0), new Vec3(0, g, 0), new Vec3(0, 0, g)}) {
                    BeamFx.draw(pose, buffers, from.subtract(d).subtract(cam), from.add(d).subtract(cam), 0.15f, fx.color(), 0.8f, time);
                }
            }
            case ChorusFxPayload.BEAM -> BeamFx.draw(pose, buffers, fx.point().subtract(cam), fx.aux().subtract(cam), fx.radius(),
                    fx.color(), (float) Math.sqrt(1 - t), time);
            case ChorusFxPayload.RING_OUT -> TubeFx.ring(pose, buffers, AmaraFxRenderer.RING, fx.point().subtract(cam),
                    Math.max(0.3f, fx.radius() * t), 0.9f, 1.4f, fx.color(), 1 - t * t, time);
            case ChorusFxPayload.EYE_BEAMS -> {
                float alpha = Math.min(1, t * 8) * Math.min(1, (1 - t) * 8);
                Vec3 centre = core;
                for (int i = 0; i < ChorusGeometry.EYES; i++) {
                    if (ChorusGeometry.wheelOf(i) != fx.arg() || !boss.partAlive(ChorusEntity.FIRST_EYE + i)) continue;
                    Vec3 from = eye(boss, i, partial);
                    Vec3 to = from.add(from.subtract(centre).normalize().scale(fx.radius()));
                    var hit = mc.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
                    BeamFx.draw(pose, buffers, from.subtract(cam), hit.getLocation().subtract(cam), 0.5f, fx.color(), alpha, time);
                }
            }
            case ChorusFxPayload.ROLLING -> {
                Vec3 along = fx.aux().subtract(fx.point());
                Vec3 dir = along.normalize();
                Vec3 at = fx.point().add(along.scale(t)).add(0, fx.radius(), 0);
                Vec3 side = new Vec3(-dir.z, 0, dir.x);
                float roll = t * (float) along.length() / fx.radius();
                int seg = 16;
                for (int i = 0; i < seg; i++) {
                    double a0 = roll + Math.PI * 2 * i / seg, a1 = roll + Math.PI * 2 * (i + 1) / seg;
                    Vec3 p0 = at.add(dir.scale(Math.cos(a0) * fx.radius())).add(0, Math.sin(a0) * fx.radius(), 0);
                    Vec3 p1 = at.add(dir.scale(Math.cos(a1) * fx.radius())).add(0, Math.sin(a1) * fx.radius(), 0);
                    BeamFx.draw(pose, buffers, p0.subtract(cam), p1.subtract(cam), 0.6f, fx.color(), 1f, time);
                    if (i % 4 == 0) BeamFx.draw(pose, buffers, at.add(side.scale(0.3)).subtract(cam), p0.subtract(cam), 0.2f, fx.color(), 0.7f, time);
                }
            }
            case ChorusFxPayload.NOTE -> {
                Vec3 halo = body.add(0, 100 / ChorusGeometry.PX, 0);
                Vec3 bell = fx.point();
                BeamFx.draw(pose, buffers, halo.subtract(cam), bell.subtract(cam), 0.25f, fx.color(), 0.8f * (1 - t * 0.5f), time);
                TubeFx.ring(pose, buffers, AmaraFxRenderer.RING, halo.subtract(cam).subtract(0, 0.3, 0), 3.2f + 0.3f * Mth.sin(time * 6),
                        0.6f, 0.6f, fx.color(), 0.9f, time);
                TubeFx.ring(pose, buffers, AmaraFxRenderer.RING, bell.subtract(cam).subtract(0, 1.0, 0), 1.2f, 0.4f, 2.5f, fx.color(), 0.8f, time);
            }
            case ChorusFxPayload.PULSE -> {
                float r = fx.radius() * t;
                for (int i = 0; i < 14; i++) {
                    double yaw = i * 2.399, pitch = Math.asin(-1 + 2 * (i + 0.5) / 14.0);
                    Vec3 d = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
                    BeamFx.draw(pose, buffers, core.subtract(cam), core.add(d.scale(r)).subtract(cam), 0.35f, fx.color(), 1 - t, time);
                }
                TubeFx.ring(pose, buffers, AmaraFxRenderer.RING, fx.point().subtract(cam), Math.max(0.3f, r), 1.2f, 2f, fx.color(), 1 - t, time);
            }
            default -> {
            }
        }
    }
}
