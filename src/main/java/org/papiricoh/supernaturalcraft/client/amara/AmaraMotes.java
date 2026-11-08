package org.papiricoh.supernaturalcraft.client.amara;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;

import java.util.ArrayList;
import java.util.List;

/**
 * The Darkness's own particles (v0.42), drawn by hand so they need no registered particle type: camera-facing sprites from
 * {@code textures/particle/} ({@code AmaraAssets.PARTICLES}, white-alpha, tinted here) and real block chunks for the ground
 * breaking up. Ticked and drawn by {@link AmaraFxRenderer}. Capped ({@code SNClientConfig.DEBRIS_LIMIT}-scaled) so a burst never
 * floods a frame.
 */
public final class AmaraMotes {

    /** The sprites: {@code ASH} has four frames (the flake crumbling), the rest one. */
    public enum Sprite {
        ASH("darkness_ash_", 4), SOUL("soul_wisp", 1), STAR("star_mote", 1), INK("ink_drop", 1), DAWN("dawn_mote", 1), VOID("void_mote_", 4);

        final ResourceLocation[] frames;

        Sprite(String name, int n) {
            frames = new ResourceLocation[n];
            for (int i = 0; i < n; i++) {
                frames[i] = SupernaturalCraft.asResource("textures/particle/" + (n > 1 ? name + i : name) + ".png");
            }
        }

        ResourceLocation frame(float life) {
            return frames[Math.min(frames.length - 1, (int) (life * frames.length))];
        }
    }

    /** One sprite: where, how fast, what colour, how it fades; {@code glow} adds it onto the world (light), else it is laid over. */
    static final class Mote {
        final Sprite sprite;
        double x, y, z, vx, vy, vz;
        final float gravity, drag, size, grow;
        final int rgb, life;
        final boolean glow;
        int age;
        float spin, rot;
        /** Pulled toward this point (souls drawn into her mouth), or null. */
        Vec3 home;

        Mote(Sprite sprite, Vec3 at, Vec3 v, float gravity, float drag, float size, float grow, int rgb, int life, boolean glow) {
            this.sprite = sprite;
            x = at.x;
            y = at.y;
            z = at.z;
            vx = v.x;
            vy = v.y;
            vz = v.z;
            this.gravity = gravity;
            this.drag = drag;
            this.size = size;
            this.grow = grow;
            this.rgb = rgb;
            this.life = Math.max(1, life);
            this.glow = glow;
        }
    }

    /** A chunk of the world: a real block, tumbling. */
    static final class Chunk {
        final BlockState state;
        double x, y, z, vx, vy, vz;
        float rx, ry, rz, sx, sy, sz, scale;
        final float gravity;
        final int life;
        int age;
        /** Held still (telekinesis) until this age, then flung by (fx, fy, fz). */
        int holdUntil = -1;
        double fx, fy, fz;

        Chunk(BlockState state, Vec3 at, Vec3 v, float scale, float gravity, int life, RandomSource r) {
            this.state = state;
            x = at.x;
            y = at.y;
            z = at.z;
            vx = v.x;
            vy = v.y;
            vz = v.z;
            this.scale = scale;
            this.gravity = gravity;
            this.life = life;
            rx = r.nextFloat() * 360;
            ry = r.nextFloat() * 360;
            rz = r.nextFloat() * 360;
            sx = (r.nextFloat() - 0.5f) * 16;
            sy = (r.nextFloat() - 0.5f) * 16;
            sz = (r.nextFloat() - 0.5f) * 16;
        }
    }

    private static final List<Mote> MOTES = new ArrayList<>();
    private static final List<Chunk> CHUNKS = new ArrayList<>();
    static final RandomSource RANDOM = RandomSource.create();

    private AmaraMotes() {
    }

    private static int moteCap() {
        return Math.max(200, SNClientConfig.DEBRIS_LIMIT.get() * 6);
    }

    private static int chunkCap() {
        return Math.max(24, SNClientConfig.DEBRIS_LIMIT.get());
    }

    static Mote add(Sprite sprite, Vec3 at, Vec3 v, float gravity, float size, int rgb, int life, boolean glow) {
        Mote m = new Mote(sprite, at, v, gravity, 0.96f, size, 0, rgb, life, glow);
        m.spin = (RANDOM.nextFloat() - 0.5f) * 0.3f;
        m.rot = RANDOM.nextFloat() * Mth.TWO_PI;
        if (MOTES.size() < moteCap()) MOTES.add(m);
        return m;
    }

    static Mote smoke(Vec3 at, Vec3 v, float size, int life) {
        Mote m = new Mote(Sprite.DAWN, at, v, -0.002f, 0.93f, size, 0.025f, 0x07040C, life, false);
        m.rot = RANDOM.nextFloat() * Mth.TWO_PI;
        m.spin = (RANDOM.nextFloat() - 0.5f) * 0.06f;
        if (MOTES.size() < moteCap()) MOTES.add(m);
        return m;
    }

    static Chunk chunk(BlockState state, Vec3 at, Vec3 v, float scale, float gravity, int life) {
        Chunk c = new Chunk(state, at, v, scale, gravity, life, RANDOM);
        if (CHUNKS.size() < chunkCap() && !state.isAir()) CHUNKS.add(c);
        return c;
    }

    static int count() {
        return MOTES.size() + CHUNKS.size();
    }

    static void clear() {
        MOTES.clear();
        CHUNKS.clear();
    }

    static void tick() {
        for (Mote m : MOTES) {
            m.age++;
            if (m.home != null) {
                // Drawn along a narrowing spiral into her.
                double dx = m.home.x - m.x, dy = m.home.y - m.y, dz = m.home.z - m.z;
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (d < 0.35) {
                    m.age = m.life;
                    continue;
                }
                double pull = Math.min(0.5, 0.06 + m.age * 0.004);
                m.vx = m.vx * 0.85 + dx / d * pull;
                m.vy = m.vy * 0.85 + dy / d * pull;
                m.vz = m.vz * 0.85 + dz / d * pull;
            }
            m.x += m.vx;
            m.y += m.vy;
            m.z += m.vz;
            m.vx *= m.drag;
            m.vz *= m.drag;
            m.vy = m.vy * m.drag - m.gravity;
            m.rot += m.spin;
        }
        MOTES.removeIf(m -> m.age >= m.life);
        for (Chunk c : CHUNKS) {
            c.age++;
            if (c.holdUntil >= 0 && c.age < c.holdUntil) {
                c.x += c.vx;
                c.y += c.vy;
                c.z += c.vz;
                c.vx *= 0.82;
                c.vy *= 0.82;
                c.vz *= 0.82;
                c.rx += c.sx * 0.3f;
                c.ry += c.sy * 0.3f;
                continue;
            }
            if (c.holdUntil >= 0 && c.age == c.holdUntil) {
                c.vx = c.fx;
                c.vy = c.fy;
                c.vz = c.fz;
            }
            c.x += c.vx;
            c.y += c.vy;
            c.z += c.vz;
            c.vy -= c.gravity;
            c.vx *= 0.98;
            c.vz *= 0.98;
            c.rx += c.sx;
            c.ry += c.sy;
            c.rz += c.sz;
        }
        CHUNKS.removeIf(c -> c.age >= c.life);
    }

    static void render(PoseStack pose, MultiBufferSource buffers, Camera camera, float partial) {
        if (MOTES.isEmpty() && CHUNKS.isEmpty()) return;
        Vec3 cam = camera.getPosition();
        Quaternionf facing = camera.rotation();
        // Grouped by texture and blend, so each buffer is asked for once and used up before the next.
        for (Sprite s : Sprite.values()) {
            for (int f = 0; f < s.frames.length; f++) {
                ResourceLocation tex = s.frames[f];
                if (!GeoGuard.exists(tex)) continue;
                for (int pass = 0; pass < 2; pass++) {
                    boolean glow = pass == 1;
                    VertexConsumer vc = null;
                    for (Mote m : MOTES) {
                        if (m.sprite != s || m.glow != glow) continue;
                        float k = (m.age + partial) / m.life;
                        if (s.frame(k) != tex) continue;
                        if (vc == null) vc = buffers.getBuffer(glow ? RenderType.eyes(tex) : RenderType.entityTranslucentEmissive(tex));
                        sprite(vc, pose, m, cam, facing, partial, k);
                    }
                }
            }
        }
        if (!CHUNKS.isEmpty()) {
            BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            for (Chunk c : CHUNKS) {
                float k = (c.age + partial) / c.life;
                float s = c.scale * (k > 0.8f ? (1 - k) / 0.2f : 1f);
                if (s <= 0.01f) continue;
                pose.pushPose();
                pose.translate(Mth.lerp(partial, c.x - c.vx, c.x) - cam.x, Mth.lerp(partial, c.y - c.vy, c.y) - cam.y,
                        Mth.lerp(partial, c.z - c.vz, c.z) - cam.z);
                pose.mulPose(Axis.XP.rotationDegrees(c.rx));
                pose.mulPose(Axis.YP.rotationDegrees(c.ry));
                pose.mulPose(Axis.ZP.rotationDegrees(c.rz));
                pose.scale(s, s, s);
                pose.translate(-0.5, -0.5, -0.5);
                blocks.renderSingleBlock(c.state, pose, buffers, LightTexture.pack(4, 6), OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
    }

    private static void sprite(VertexConsumer vc, PoseStack pose, Mote m, Vec3 cam, Quaternionf facing, float partial, float k) {
        double x = Mth.lerp(partial, m.x - m.vx, m.x) - cam.x, y = Mth.lerp(partial, m.y - m.vy, m.y) - cam.y,
                z = Mth.lerp(partial, m.z - m.vz, m.z) - cam.z;
        float size = (m.size + m.grow * (m.age + partial)) * (m.sprite == Sprite.STAR ? 0.8f + 0.2f * Mth.sin((m.age + partial) * 0.9f) : 1f);
        float a = m.glow ? 1 - k : Math.min(1, (1 - k) * 2f) * Math.min(1, (m.age + partial) / 3f);
        if (a <= 0.01f) return;
        int r = m.rgb >> 16 & 0xFF, g = m.rgb >> 8 & 0xFF, b = m.rgb & 0xFF, al = (int) (a * 255);
        if (m.glow) {
            // Additive: alpha does nothing, so fade by darkening.
            r = (int) (r * a);
            g = (int) (g * a);
            b = (int) (b * a);
        }
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(facing);
        pose.mulPose(Axis.ZP.rotation(m.rot + m.spin * partial));
        PoseStack.Pose p = pose.last();
        float h = size / 2;
        put(vc, p, -h, -h, 0, 1, r, g, b, al);
        put(vc, p, h, -h, 1, 1, r, g, b, al);
        put(vc, p, h, h, 1, 0, r, g, b, al);
        put(vc, p, -h, h, 0, 0, r, g, b, al);
        pose.popPose();
    }

    private static void put(VertexConsumer vc, PoseStack.Pose p, float x, float y, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(p, x, y, 0).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(p, 0, 0, 1);
    }
}
