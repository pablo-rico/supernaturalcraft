package org.papiricoh.supernaturalcraft.client.chuck.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.AuthorTargetEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckEntity;
import org.papiricoh.supernaturalcraft.entity.boss.chuck.ChuckGeometry;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.ArrayList;
import java.util.List;

/**
 * The Author's fight in the world itself: the snap's frame and countdown, the backspace streak, the letters rising as
 * an arena is written, the burst when his script cracks, and the ambient motes (gold around the light, ink weeping
 * off shielded targets).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ChuckWorldFx {

    private static class Timed {
        int age;
        final int life;

        Timed(int life) {
            this.life = Math.max(1, life);
        }
    }

    private static final class Snap extends Timed {
        final Vec3 point;
        final float radius;

        Snap(Vec3 point, float radius, int life) {
            super(life);
            this.point = point;
            this.radius = radius;
        }
    }

    private static final class Streak extends Timed {
        final Vec3 from;
        final int entity;

        Streak(Vec3 from, int entity) {
            super(20);
            this.from = from;
            this.entity = entity;
        }
    }

    private static final class Wave extends Timed {
        final Vec3 point;
        final float radius;
        final boolean cabin;

        Wave(Vec3 point, float radius, int life, boolean cabin) {
            super(life);
            this.point = point;
            this.radius = radius;
            this.cabin = cabin;
        }
    }

    private static final List<Snap> SNAPS = new ArrayList<>();
    private static final List<Streak> STREAKS = new ArrayList<>();
    private static final List<Wave> WAVES = new ArrayList<>();

    private ChuckWorldFx() {
    }

    public static void snap(Vec3 point, float radius, int duration) {
        SNAPS.add(new Snap(point, Math.max(1, radius), duration));
        ChuckOverlay.add(new ChuckOverlay.SnapWarning(point, Math.max(1, radius), duration));
    }

    public static void backspace(int entity, Vec3 from) {
        STREAKS.add(new Streak(from, entity));
        ClientLevel level = Minecraft.getInstance().level;
        Entity e = level == null ? null : level.getEntity(entity);
        if (e == null) return;
        Vec3 to = e.position().add(0, 1, 0);
        for (int i = 0; i < 24; i++) {
            Vec3 p = from.add(0, 1, 0).lerp(to, i / 23.0);
            level.addParticle(AllParticles.INK_LETTER.get(), p.x, p.y, p.z, (level.random.nextDouble() - 0.5) * 0.05, 0.02,
                    (level.random.nextDouble() - 0.5) * 0.05);
        }
    }

    /** {@code chapter} = the chapter ordinal being written, or -1 for the cabin coming back. */
    public static void wave(Vec3 point, float radius, int duration, int chapter) {
        WAVES.add(new Wave(point, Math.max(2, radius), Math.max(10, duration), chapter < 0));
    }

    public static void crackBurst(Vec3 point) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || point == null) return;
        RandomSource r = level.random;
        for (int i = 0; i < 40; i++) {
            Vec3 v = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(0.15 + r.nextDouble() * 0.25);
            level.addParticle(i % 3 == 0 ? AllParticles.PAGE_SCRAP.get() : AllParticles.WHITE_LIGHT.get(), point.x, point.y, point.z,
                    v.x, v.y, v.z);
        }
    }

    static void clear() {
        SNAPS.clear();
        STREAKS.clear();
        WAVES.clear();
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            clear();
            return;
        }
        SNAPS.removeIf(s -> ++s.age >= s.life);
        STREAKS.removeIf(s -> ++s.age >= s.life);
        boolean few = mc.options.particles().get() != net.minecraft.client.ParticleStatus.ALL;
        for (Wave w : List.copyOf(WAVES)) {
            if (++w.age >= w.life) {
                WAVES.remove(w);
                continue;
            }
            float r = w.radius * w.age / w.life;
            int n = Math.min(few ? 12 : 40, Math.max(4, (int) (r * 1.2f)));
            for (int i = 0; i < n; i++) {
                double a = level.random.nextDouble() * Math.PI * 2;
                double rr = r + (level.random.nextDouble() - 0.5) * 1.5;
                ParticleOptions type = w.cabin ? (i % 2 == 0 ? AllParticles.GOLDEN_MOTE.get() : AllParticles.PAGE_SCRAP.get())
                        : (i % 4 == 0 ? AllParticles.PAGE_SCRAP.get() : AllParticles.INK_LETTER.get());
                level.addParticle(type, w.point.x + Math.cos(a) * rr, w.point.y + 0.2 + level.random.nextDouble(),
                        w.point.z + Math.sin(a) * rr, 0, 0.06 + level.random.nextDouble() * 0.08, 0);
            }
        }
        ambient(mc, level, few);
    }

    /** Gold motes around the light (light leaking as his script cracks), ink weeping off shields. The echoes, hands and
     * allies shed their own particles. */
    private static void ambient(Minecraft mc, ClientLevel level, boolean few) {
        RandomSource r = level.random;
        var box = mc.player.getBoundingBox().inflate(64);
        for (ChuckEntity c : level.getEntitiesOfClass(ChuckEntity.class, box)) {
            if (!c.divine()) continue;
            int motes = few ? 1 : 3;
            for (int i = 0; i < motes; i++) {
                double a = r.nextDouble() * Math.PI * 2, rad = 2 + r.nextDouble() * ChuckGeometry.RING_RADIUS[3];
                level.addParticle(AllParticles.GOLDEN_MOTE.get(), c.getX() + Math.cos(a) * rad,
                        c.getY() + r.nextDouble() * ChuckGeometry.DIVINE_HEIGHT, c.getZ() + Math.sin(a) * rad, 0, 0.01, 0);
            }
            if (c.crack() > 0.05f && r.nextFloat() < c.crack()) {
                level.addParticle(AllParticles.WHITE_LIGHT.get(), c.getX() + (r.nextDouble() - 0.5) * 3,
                        c.getY() + ChuckGeometry.CORE_Y + (r.nextDouble() - 0.5) * 4, c.getZ() + (r.nextDouble() - 0.5) * 3,
                        (r.nextDouble() - 0.5) * 0.1, (r.nextDouble() - 0.5) * 0.1, (r.nextDouble() - 0.5) * 0.1);
            }
        }
        for (AuthorTargetEntity t : level.getEntitiesOfClass(AuthorTargetEntity.class, box)) {
            if (t.shielded() && r.nextInt(4) == 0) {
                level.addParticle(AllParticles.INK.get(), t.getX() + (r.nextDouble() - 0.5) * t.getBbWidth(), t.getY(),
                        t.getZ() + (r.nextDouble() - 0.5) * t.getBbWidth(), 0, -0.04, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (SNAPS.isEmpty() && STREAKS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        float time = (mc.level.getGameTime() + partial) / 20f;
        for (Snap s : SNAPS) snapFrame(pose, buffers, cam, s, partial);
        for (Streak s : STREAKS) {
            Entity e = mc.level.getEntity(s.entity);
            if (e == null) continue;
            float a = 1 - (s.age + partial) / s.life;
            Vec3 to = e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0);
            BeamFx.draw(pose, buffers, s.from.add(0, 1, 0).subtract(cam), to.subtract(cam), 0.5f * a + 0.1f, 0x2B2654, a, time * 3);
        }
        buffers.endBatch();
    }

    /** A viewfinder of ink around the doomed square: corner brackets, a thin border, posts rising, the count above. */
    private static void snapFrame(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Snap s, float partial) {
        float t = s.age + partial;
        float left = (s.life - t) / 20f;
        float pulse = 1 - (left - (float) Math.floor(left));
        float r = s.radius;
        int red = ChuckText.argb(0.75f + 0.25f * pulse, 0xC8261E), thin = ChuckText.argb(0.45f, 0x1A1410);
        pose.pushPose();
        pose.translate(s.point.x - cam.x, s.point.y + 0.06 - cam.y, s.point.z - cam.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer q = WorldDraw.quads(buffers);
        float arm = Math.max(1, r * 0.35f), w = 0.22f + 0.1f * pulse;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                float x = sx * r, z = sz * r;
                WorldDraw.groundStroke(q, m, x, z, x - sx * arm, z, 0, w, red);
                WorldDraw.groundStroke(q, m, x, z, x, z - sz * arm, 0, w, red);
                // A post at each corner, as if the square were a page being lifted out.
                WorldDraw.quad(q, m, new org.joml.Vector3f(x - 0.06f, 0, z), new org.joml.Vector3f(x + 0.06f, 0, z),
                        new org.joml.Vector3f(x + 0.06f, 3, z), new org.joml.Vector3f(x - 0.06f, 3, z), ChuckText.argb(0.5f, 0xC8261E));
            }
        }
        WorldDraw.groundStroke(q, m, -r, -r, r, -r, 0, 0.06f, thin);
        WorldDraw.groundStroke(q, m, r, -r, r, r, 0, 0.06f, thin);
        WorldDraw.groundStroke(q, m, r, r, -r, r, 0, 0.06f, thin);
        WorldDraw.groundStroke(q, m, -r, r, -r, -r, 0, 0.06f, thin);
        pose.translate(0, 3.2 + Math.min(4, r * 0.3), 0);
        int secs = Math.max(1, (int) Math.ceil(left));
        WorldDraw.billboardText(pose, buffers, ChuckText.typed(String.valueOf(secs)), 2.2f + pulse * 0.8f, red, false);
        pose.popPose();
    }
}
