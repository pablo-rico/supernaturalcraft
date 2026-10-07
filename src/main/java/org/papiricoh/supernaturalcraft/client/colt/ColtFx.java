package org.papiricoh.supernaturalcraft.client.colt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;

import java.util.ArrayList;
import java.util.List;

/**
 * The Colt in the world: the muzzle's fire and smoke, the round's streak of light, and what a
 * consecrated round does to a demon — orange lightning crawling under its skin before it drops.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ColtFx {

    public static final int TRACER_TICKS = 4, EXECUTION_TICKS = 16, WISP_TICKS = 30;
    private static final int ORANGE = 0xFF8A1E;

    private record Tracer(Vec3 from, Vec3 to, long start) {
    }

    private record Execution(int entity, AABB fallback, long start) {
    }

    private record Wisp(int player, long start) {
    }

    private static final List<Tracer> TRACERS = new ArrayList<>();
    private static final List<Execution> EXECUTIONS = new ArrayList<>();
    private static final List<Wisp> WISPS = new ArrayList<>();

    private ColtFx() {
    }

    /**
     * Where the muzzle is. Seen through the shooter's own eyes it is where the drawn barrel ends,
     * low and to the gun hand's side; anyone else sees it at the end of an arm levelled at the target.
     */
    public static Vec3 muzzle(Player p, float partial) {
        Minecraft mc = Minecraft.getInstance();
        boolean ownView = p == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
        Vec3 look = p.getViewVector(partial);
        float yaw = p.getViewYRot(partial) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        int side = p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        Vec3 up = right.cross(look);
        return ownView
                ? p.getEyePosition(partial).add(look.scale(1.2)).add(right.scale(0.36 * side)).add(up.scale(-0.27))
                : p.getEyePosition(partial).add(look.scale(1.05)).add(right.scale(0.36 * side)).add(up.scale(-0.4));
    }

    public static void tracer(Vec3 from, Vec3 to) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) TRACERS.add(new Tracer(from, to, level.getGameTime()));
    }

    /** Fire, smoke and sparks at the muzzle, and a thread of smoke that keeps curling from it. */
    public static void muzzleBlast(Player p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        Vec3 m = muzzle(p, 1), look = p.getViewVector(1);
        RandomSource r = level.random;
        for (int i = 0; i < 6; i++) {
            Vec3 v = look.scale(0.08 + r.nextDouble() * 0.12).add(r.nextGaussian() * 0.02, 0.01 + r.nextDouble() * 0.03, r.nextGaussian() * 0.02);
            level.addParticle(ParticleTypes.SMOKE, m.x, m.y, m.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 4; i++) {
            Vec3 v = look.scale(0.3 + r.nextDouble() * 0.4).add(r.nextGaussian() * 0.1, r.nextGaussian() * 0.1, r.nextGaussian() * 0.1);
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, m.x, m.y, m.z, v.x, v.y, v.z);
        }
        for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.CRIT, m.x, m.y, m.z, look.x * 0.5, look.y * 0.5, look.z * 0.5);
        WISPS.add(new Wisp(p.getId(), level.getGameTime()));
    }

    /** Dust, chips and smoke where the round struck stone; a spray of sparks where it struck flesh. */
    public static void impact(Vec3 at, Vec3 from, boolean entity) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource r = level.random;
        Vec3 back = from.subtract(at).normalize().scale(0.15);
        if (entity) {
            for (int i = 0; i < 8; i++) level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, r.nextGaussian() * 0.3, r.nextGaussian() * 0.3, r.nextGaussian() * 0.3);
            return;
        }
        BlockPos pos = BlockPos.containing(at.subtract(back.scale(0.1)));
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            for (int i = 0; i < 12; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z,
                        back.x * 4 + r.nextGaussian() * 0.15, 0.1 + r.nextDouble() * 0.2, back.z * 4 + r.nextGaussian() * 0.15);
            }
        }
        for (int i = 0; i < 3; i++) level.addParticle(ParticleTypes.SMOKE, at.x + back.x, at.y + back.y, at.z + back.z, 0, 0.02, 0);
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, back.x * 3, 0.1, back.z * 3);
    }

    /** The demon burns out from within. Follows the body; falls back to where it stood if it is gone. */
    public static void execution(Entity target, Vec3 at) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        AABB box = target != null ? target.getBoundingBox() : AABB.ofSize(at.add(0, 0.9, 0), 0.6, 1.8, 0.6);
        EXECUTIONS.add(new Execution(target != null ? target.getId() : -1, box, level.getGameTime()));
    }

    public static void clear() {
        TRACERS.clear();
        EXECUTIONS.clear();
        WISPS.clear();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }
        long now = level.getGameTime();
        TRACERS.removeIf(t -> now - t.start > TRACER_TICKS || now < t.start);
        EXECUTIONS.removeIf(e -> now - e.start > EXECUTION_TICKS || now < e.start);
        WISPS.removeIf(w -> now - w.start > WISP_TICKS || now < w.start);
        RandomSource r = level.random;
        for (Wisp w : WISPS) {
            if ((now - w.start) % 3 != 0 || !(level.getEntity(w.player) instanceof Player p)) continue;
            Vec3 m = muzzle(p, 1);
            level.addParticle(ParticleTypes.SMOKE, m.x, m.y, m.z, r.nextGaussian() * 0.003, 0.015, r.nextGaussian() * 0.003);
        }
        for (Execution e : EXECUTIONS) {
            AABB box = box(level, e);
            for (int i = 0; i < 3; i++) {
                double x = Mth.lerp(r.nextDouble(), box.minX, box.maxX), y = Mth.lerp(r.nextDouble(), box.minY, box.maxY),
                        z = Mth.lerp(r.nextDouble(), box.minZ, box.maxZ);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15);
            }
            if ((now - e.start) % 4 == 0) {
                Vec3 c = box.getCenter();
                level.addParticle(ParticleTypes.FLAME, c.x, c.y, c.z, r.nextGaussian() * 0.04, 0.05, r.nextGaussian() * 0.04);
            }
        }
    }

    private static AABB box(ClientLevel level, Execution e) {
        Entity entity = e.entity >= 0 ? level.getEntity(e.entity) : null;
        return entity != null ? entity.getBoundingBox() : e.fallback;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (TRACERS.isEmpty() && EXECUTIONS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = level.getGameTime() + partial;
        for (Tracer t : TRACERS) {
            float fade = 1 - (float) Mth.clamp((now - t.start) / TRACER_TICKS, 0, 1);
            if (fade <= 0) continue;
            BeamFx.draw(pose, buffers, t.from.subtract(cam), t.to.subtract(cam), 0.035f * fade + 0.01f, 0xFFD9A0, 0.9f * fade, (float) now);
        }
        if (!TRACERS.isEmpty()) buffers.endBatch();
        if (EXECUTIONS.isEmpty()) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        for (Execution e : EXECUTIONS) {
            double age = now - e.start;
            // Strobe: every other tick the crackle flares.
            boolean flare = ((long) age & 1) == 0;
            float fade = 1 - (float) Mth.clamp(age / EXECUTION_TICKS, 0, 1);
            AABB box = box(level, e).move(-cam.x, -cam.y, -cam.z);
            RandomSource r = RandomSource.create(e.entity * 31L + (long) (age / 2) * 7919L);
            int arcs = 4 + r.nextInt(4);
            for (int a = 0; a < arcs; a++) arc(vc, m, box, r, Vec3.ZERO, flare ? 1f : 0.55f, fade);
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** One jagged arc wandering through the body, drawn as a glow and a white-hot core. */
    private static void arc(VertexConsumer vc, Matrix4f m, AABB box, RandomSource r, Vec3 eye, float bright, float fade) {
        Vec3 p = new Vec3(Mth.lerp(r.nextDouble(), box.minX, box.maxX), Mth.lerp(r.nextDouble(), box.minY, box.maxY),
                Mth.lerp(r.nextDouble(), box.minZ, box.maxZ));
        double step = Math.max(box.getXsize(), box.getYsize()) * 0.22;
        int segments = 5 + r.nextInt(3);
        for (int s = 0; s < segments; s++) {
            Vec3 next = new Vec3(
                    Mth.clamp(p.x + r.nextGaussian() * step, box.minX, box.maxX),
                    Mth.clamp(p.y + r.nextGaussian() * step, box.minY, box.maxY),
                    Mth.clamp(p.z + r.nextGaussian() * step, box.minZ, box.maxZ));
            segment(vc, m, p, next, eye, 0.07f, ORANGE, 0.55f * bright * fade);
            segment(vc, m, p, next, eye, 0.022f, 0xFFF2D0, 0.95f * bright * fade);
            p = next;
        }
    }

    private static void segment(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 eye, float width, int rgb, float alpha) {
        Vec3 dir = b.subtract(a);
        Vec3 side = dir.cross(a.add(b).scale(0.5).subtract(eye)).normalize().scale(width / 2);
        if (Double.isNaN(side.x)) return;
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, bl = (rgb & 0xFF) / 255f;
        float al = Mth.clamp(alpha, 0, 1);
        vc.addVertex(m, (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)).setColor(r, g, bl, al);
        vc.addVertex(m, (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z)).setColor(r, g, bl, al);
        vc.addVertex(m, (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z)).setColor(r, g, bl, al);
        vc.addVertex(m, (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z)).setColor(r, g, bl, al);
    }
}
