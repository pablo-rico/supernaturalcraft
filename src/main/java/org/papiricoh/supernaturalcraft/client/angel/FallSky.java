package org.papiricoh.supernaturalcraft.client.angel;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Fall in the sky (v0.22). On its night ({@link ClientAngels#handleFx}, {@code FALL_NIGHT}) the Overworld's night sky rains
 * falling angels: streaks of light crossing it high up in every direction, falling steeply at their own speeds and sizes, some
 * burning out, some dropping behind the horizon with a faint flash there, and now and then a far whistle overhead. A single star
 * ({@code FALL_STAR}) is one larger angel streaking down toward a point, seen from wherever it can be, ending in a flash and a far
 * boom.
 *
 * <p>All of it is drawn on the sky ({@code AFTER_SKY}, a dome round the camera like {@code EclipseSky}): no fog, no far plane,
 * clouds and the land in front of it. A star is placed on the dome by its true direction and sized by its true distance. Only
 * what lands near enough also lights the ground there ({@link #renderImpacts}).
 *
 * <p>The streak texture ({@link AngelAssets#FALL_STREAK}) stands along v: the tail fading out at v 0, the head at v 1,
 * the streak along the middle of u; without it, a soft streak of vertex colour is drawn instead.
 */
final class FallSky {

    static final ResourceLocation STREAK = SupernaturalCraft.asResource(AngelAssets.FALL_STREAK);
    /** The dome's radius. */
    private static final float R = 100;
    /** Streaks born per tick at full strength. */
    private static final float RATE_NORMAL = 0.95f, RATE_LOW = 0.3f;
    private static final int MAX_STREAKS = 140;
    /** How long a star's flash and glow last after it lands. */
    private static final int IMPACT_TICKS = 50;
    private static final int HORIZON_FLASH_TICKS = 26;

    private static boolean night;
    private static float strength, strengthO;
    private static final List<Streak> STREAKS = new ArrayList<>();
    private static final List<Star> STARS = new ArrayList<>();
    /** Faint flashes where streaks dropped behind the horizon: {@code x, z} (unit direction), age, size. */
    private static final List<float[]> HORIZON = new ArrayList<>();
    private static int whistleIn = 60;

    private FallSky() {
    }

    /** One falling angel on the dome: its head runs along a great circle from {@code d} toward {@code t}. */
    private static final class Streak {
        final Vector3f d, t;
        final float speed, tail, width, life;
        final int rgb;
        final boolean lands;
        int age;

        Streak(Vector3f d, Vector3f t, float speed, float tail, float width, float life, int rgb, boolean lands) {
            this.d = d;
            this.t = t;
            this.speed = speed;
            this.tail = tail;
            this.width = width;
            this.life = life;
            this.rgb = rgb;
            this.lands = lands;
        }

        Vector3f at(float s) {
            return new Vector3f(d).mul(Mth.cos(s)).add(new Vector3f(t).mul(Mth.sin(s)));
        }
    }

    /** One star falling toward (x, z). */
    private static final class Star {
        final double x, z;
        final long start;
        final int duration;
        final float az;
        double y = Double.NaN;
        boolean landed;

        Star(double x, double z, long start, int duration) {
            this.x = x;
            this.z = z;
            this.start = start;
            this.duration = duration;
            long h = Double.doubleToLongBits(x * 31 + z) * 0x9E3779B97F4A7C15L;
            this.az = (h >>> 20 & 0xFFFF) / 65535f * Mth.TWO_PI;
        }

        /** Where it is at {@code u} (0 high and far, 1 landed). */
        Vec3 at(double u) {
            double gy = Double.isNaN(y) ? 64 : y;
            double sx = x + Mth.cos(az) * 260, sz = z + Mth.sin(az) * 260, sy = gy + 320;
            return new Vec3(sx + (x - sx) * u, sy + (gy - sy) * u, sz + (z - sz) * u);
        }
    }

    // --- state --------------------------------------------------------------------------------------------------------------

    static void night(boolean on) {
        night = on;
    }

    static void star(ClientLevel level, double x, double z, int duration) {
        Star s = new Star(x, z, level.getGameTime(), duration);
        STARS.add(s);
        // Its whistle, heard as it comes in.
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer != null) {
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            Vec3 mid = s.at(0.6);
            ClientAngels.sound(eye.add(mid.subtract(eye).normalize().scale(28)), "fall_streak", 2.4f, 0.8f, Math.max(0, duration / 3));
        }
    }

    static boolean busy() {
        return night || strength > 0 || !STREAKS.isEmpty() || !STARS.isEmpty();
    }

    static boolean anyImpact() {
        return !STARS.isEmpty();
    }

    static void clear() {
        night = false;
        strength = strengthO = 0;
        STREAKS.clear();
        STARS.clear();
        HORIZON.clear();
    }

    private static boolean overworld(ClientLevel level) {
        return level.dimension() == Level.OVERWORLD;
    }

    /** How dark the sky is for the Fall to show (0 by day, 1 deep at night), dimmed by rain. */
    private static float dark(ClientLevel level, float partial) {
        return Mth.clamp(level.getStarBrightness(partial) * 2.2f, 0, 1) * (1 - 0.85f * level.getRainLevel(partial));
    }

    static void tick(Minecraft mc, ClientLevel level) {
        strengthO = strength;
        boolean on = night && overworld(level);
        strength = on ? Math.min(1, strength + 1 / 80f) : Math.max(0, strength - 1 / 60f);
        RandomSource r = level.random;
        long now = level.getGameTime();
        // The night's rain of angels.
        float vis = strength * dark(level, 0);
        if (vis > 0.01f && STREAKS.size() < MAX_STREAKS) {
            float rate = (SNClientConfig.FALL_DENSITY.get() == SNClientConfig.FallDensity.LOW ? RATE_LOW : RATE_NORMAL) * vis;
            int n = (int) rate + (r.nextFloat() < rate - (int) rate ? 1 : 0);
            for (int i = 0; i < n; i++) STREAKS.add(spawn(r));
        }
        for (Iterator<Streak> it = STREAKS.iterator(); it.hasNext(); ) {
            Streak s = it.next();
            s.age++;
            Vector3f h = s.at(s.speed * s.age);
            if (h.y < 0.012f) {
                if (s.lands && HORIZON.size() < 24) {
                    float l = Mth.sqrt(h.x * h.x + h.z * h.z);
                    HORIZON.add(new float[]{h.x / l, h.z / l, 0, s.width});
                }
                it.remove();
            } else if (s.age > s.life) {
                it.remove();
            }
        }
        for (Iterator<float[]> it = HORIZON.iterator(); it.hasNext(); ) {
            float[] f = it.next();
            if (++f[2] > HORIZON_FLASH_TICKS) it.remove();
        }
        // Now and then, a far whistle overhead.
        if (vis > 0.2f && --whistleIn <= 0 && !STREAKS.isEmpty() && mc.gameRenderer != null) {
            boolean low = SNClientConfig.FALL_DENSITY.get() == SNClientConfig.FallDensity.LOW;
            whistleIn = (low ? 140 : 70) + r.nextInt(low ? 200 : 120);
            Streak s = STREAKS.get(r.nextInt(STREAKS.size()));
            Vector3f h = s.at(s.speed * s.age);
            Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
            ClientAngels.sound(eye.add(h.x * 34, h.y * 34, h.z * 34), "fall_streak", 2.6f, 0.8f + 0.45f * r.nextFloat(), 0);
        }
        // The stars.
        for (Iterator<Star> it = STARS.iterator(); it.hasNext(); ) {
            Star s = it.next();
            if (Double.isNaN(s.y) && level.isLoaded(net.minecraft.core.BlockPos.containing(s.x, 64, s.z))) {
                s.y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(s.x), Mth.floor(s.z));
            }
            long age = now - s.start;
            if (!s.landed && age >= s.duration) {
                s.landed = true;
                land(mc, level, s, r);
            }
            if (s.landed && age > s.duration + IMPACT_TICKS) it.remove();
        }
    }

    private static Streak spawn(RandomSource r) {
        float az = r.nextFloat() * Mth.TWO_PI, el = (12 + 68 * (float) Math.pow(r.nextFloat(), 0.8)) * Mth.DEG_TO_RAD;
        Vector3f d = new Vector3f(Mth.cos(az) * Mth.cos(el), Mth.sin(el), Mth.sin(az) * Mth.cos(el));
        // Down the dome, steeply, leaning to one side or the other.
        Vector3f down = new Vector3f(0, -1, 0).add(new Vector3f(d).mul(d.y)).normalize();
        Vector3f side = new Vector3f(d).cross(0, 1, 0).normalize();
        float lean = (r.nextFloat() * 2 - 1) * 55 * Mth.DEG_TO_RAD;
        Vector3f t = down.mul(Mth.cos(lean)).add(side.mul(Mth.sin(lean))).normalize();
        float big = r.nextFloat();
        big = big * big * big;
        float speed = (0.006f + 0.016f * r.nextFloat()) * (1 + big * 0.6f);
        float tail = (0.06f + 0.12f * r.nextFloat()) * (1 + big);
        float width = 0.8f + 1.6f * big + 0.4f * r.nextFloat();
        boolean lands = r.nextFloat() < 0.4f;
        float life = lands ? 400 : 22 + 50 * r.nextFloat();
        float pick = r.nextFloat();
        int rgb = pick < 0.55f ? 0xD8ECFF : pick < 0.88f ? 0xFFF1DC : 0xFFC890;
        return new Streak(d, t, speed, tail, width, life, rgb, lands);
    }

    private static void land(Minecraft mc, ClientLevel level, Star s, RandomSource r) {
        if (mc.gameRenderer == null) return;
        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 hit = s.at(1);
        double d = eye.distanceTo(hit);
        // Heard far off: the boom comes after the light, at the speed of sound.
        Vec3 heard = d < 40 ? hit : eye.add(hit.subtract(eye).normalize().scale(40));
        ClientAngels.sound(heard, "fall_impact", d < 40 ? 4f : 4f * (float) Math.max(0.55, 1 - (d - 40) / 400), 0.9f + 0.15f * r.nextFloat(),
                (int) Math.min(100, d / 17));
        MagicOverlay.flash(hit, 0xE8F0FF, 0.35f, 220);
        MagicShake.kick(hit, 1.6f, 14, 70);
        if (d < 64) {
            int n = FxParticles.n(30);
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Mth.TWO_PI, sp = 0.1 + 0.3 * r.nextDouble();
                FxParticles.ember(level, r.nextBoolean() ? 0xFFB070 : ClientAngels.GRACE_HOT, 0.6f, 20 + r.nextInt(20), hit.x, hit.y + 0.3,
                        hit.z, Math.cos(a) * sp, 0.1 + 0.25 * r.nextDouble(), Math.sin(a) * sp);
            }
        }
    }

    // --- the sky ------------------------------------------------------------------------------------------------------------

    static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float vis = Mth.lerp(partial, strengthO, strength) * dark(level, partial);
        boolean sky = overworld(level) && (vis > 0.005f && !STREAKS.isEmpty() || !HORIZON.isEmpty());
        if (!sky && STARS.isEmpty()) return;
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        Matrix4f m = pose.last().pose();
        Vec3 eye = event.getCamera().getPosition();
        long now = level.getGameTime();

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        boolean tex = GeoGuard.exists(STREAK);
        if (tex) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, STREAK);
            BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            if (sky) for (Streak s : STREAKS) texturedStreak(b, m, s, partial, vis);
            for (Star s : STARS) starStreak(b, null, m, s, eye, now - s.start + partial, true);
            draw(b);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        if (sky) {
            for (Streak s : STREAKS) colourStreak(b, m, s, partial, vis, !tex);
            for (float[] f : HORIZON) {
                float k = 1 - (f[2] + partial) / HORIZON_FLASH_TICKS;
                if (k <= 0) continue;
                float a = k * k * Math.max(0.35f, vis);
                Vector3f c = new Vector3f(f[0], 0.015f, f[1]).mul(R);
                glow(b, m, c, 3.5f + 2.5f * f[3], 0xFFF4E4, 0.5f * a);
                glow(b, m, c, 14 + 4 * f[3], 0xBFD8FF, 0.12f * a);
            }
        }
        for (Star s : STARS) starStreak(null, b, m, s, eye, now - s.start + partial, !tex);
        draw(b);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void draw(BufferBuilder b) {
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }

    /** A streak's alpha now: in over a few ticks, out at the end of its life (or as it nears the horizon), flickering. */
    private static float alpha(Streak s, float age, Vector3f head) {
        float a = Mth.clamp(age / 5f, 0, 1);
        if (!s.lands) a *= Mth.clamp((s.life - age) / 12f, 0, 1);
        a *= Mth.clamp(head.y / 0.03f, 0, 1) * 0.6f + (s.lands ? 0.4f : 0.4f * Mth.clamp(head.y / 0.03f, 0, 1));
        return a * (0.85f + 0.15f * Mth.sin(age * 1.9f + s.d.x * 40));
    }

    private static void texturedStreak(BufferBuilder b, Matrix4f m, Streak s, float partial, float vis) {
        float age = s.age + partial, at = s.speed * age;
        Vector3f h = s.at(at), t = s.at(Math.max(0, at - s.tail));
        float a = alpha(s, age, h) * vis;
        if (a <= 0.004f) return;
        Vector3f hv = h.mul(R), tv = t.mul(R);
        float len = hv.distance(tv);
        quad(b, m, tv, hv, Math.max(1.4f, len * 0.22f) * (0.7f + 0.3f * s.width), s.rgb, a);
    }

    /** The streak's bright head (and, without its texture, its tail as a soft strip). */
    private static void colourStreak(BufferBuilder b, Matrix4f m, Streak s, float partial, float vis, boolean tail) {
        float age = s.age + partial, at = s.speed * age;
        Vector3f h = s.at(at);
        float a = alpha(s, age, h) * vis;
        if (a <= 0.004f) return;
        Vector3f hv = new Vector3f(h).mul(R);
        if (tail) soft(b, m, s.at(Math.max(0, at - s.tail)).mul(R), hv, 0.35f * s.width, s.rgb, 0, a);
        glow(b, m, hv, 0.55f * s.width + 0.4f, 0xFFFFFF, a);
        glow(b, m, hv, 1.6f * s.width + 0.8f, s.rgb, 0.35f * a);
    }

    /**
     * A star on the dome by its true direction, sized by its true distance: its streak (textured into {@code tb}, else soft into
     * {@code cb}), its head, and once it lands, the flash there.
     */
    private static void starStreak(BufferBuilder tb, BufferBuilder cb, Matrix4f m, Star s, Vec3 eye, float age, boolean draw) {
        if (age < 0) return;
        if (age < s.duration) {
            double u = Math.pow(age / s.duration, 1.35);
            Vec3 head = s.at(u), tail = s.at(Math.max(0, u - 0.2));
            double dh = Math.max(1, head.distanceTo(eye));
            Vector3f hv = dome(head, eye), tv = dome(tail, eye);
            float scale = (float) (R / dh);
            float a = Mth.clamp(age / 8f, 0, 1);
            if (tb != null) {
                quad(tb, m, tv, hv, Math.max(2.2f, 14 * scale), 0xFFF4E8, a);
            } else {
                if (draw) soft(cb, m, tv, hv, Math.max(0.9f, 6 * scale), 0xFFE8D0, 0, a);
                glow(cb, m, hv, Math.max(1.2f, 9 * scale), 0xFFFFFF, a);
                glow(cb, m, hv, Math.max(3f, 30 * scale), 0xCFE2FF, 0.35f * a);
            }
        } else if (tb == null) {
            float q = (age - s.duration) / IMPACT_TICKS;
            if (q >= 1) return;
            Vec3 hit = s.at(1).add(0, 2, 0);
            double dh = Math.max(1, hit.distanceTo(eye));
            float scale = (float) (R / dh), a = (float) Math.pow(1 - q, 1.8);
            Vector3f hv = dome(hit, eye);
            glow(cb, m, hv, Math.max(3f, 22 * scale) * (1 + q), 0xFFFFFF, a);
            glow(cb, m, hv, Math.max(8f, 80 * scale) * (1 + 0.5f * q), 0xCFE0FF, 0.35f * a);
        }
    }

    /** {@code p} seen from {@code eye}, placed on the dome. */
    private static Vector3f dome(Vec3 p, Vec3 eye) {
        Vec3 d = p.subtract(eye);
        double l = Math.max(1e-4, d.length());
        return new Vector3f((float) (d.x / l * R), (float) (d.y / l * R), (float) (d.z / l * R));
    }

    /** A textured quad from {@code t} (v 0) to {@code h} (v 1), {@code w} wide, turned to face the eye (the dome's centre). */
    private static void quad(BufferBuilder b, Matrix4f m, Vector3f t, Vector3f h, float w, int rgb, float a) {
        Vector3f dir = new Vector3f(h).sub(t), mid = new Vector3f(h).add(t).mul(0.5f);
        Vector3f side = dir.cross(mid, new Vector3f());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize(w / 2);
        // The texture reaches a little past the head (its glow).
        Vector3f h2 = new Vector3f(h).add(new Vector3f(dir).mul(0.06f));
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, bl = rgb & 0xFF, al = (int) (Mth.clamp(a, 0, 1) * 255);
        b.addVertex(m, t.x - side.x, t.y - side.y, t.z - side.z).setUv(0, 0).setColor(r, g, bl, al);
        b.addVertex(m, h2.x - side.x, h2.y - side.y, h2.z - side.z).setUv(0, 1).setColor(r, g, bl, al);
        b.addVertex(m, h2.x + side.x, h2.y + side.y, h2.z + side.z).setUv(1, 1).setColor(r, g, bl, al);
        b.addVertex(m, t.x + side.x, t.y + side.y, t.z + side.z).setUv(1, 0).setColor(r, g, bl, al);
    }

    /** A soft strip from {@code t} to {@code h}: bright along its middle, nothing at its sides, alpha {@code at} → {@code ah}. */
    private static void soft(BufferBuilder b, Matrix4f m, Vector3f t, Vector3f h, float w, int rgb, float at, float ah) {
        Vector3f dir = new Vector3f(h).sub(t), mid = new Vector3f(h).add(t).mul(0.5f);
        Vector3f side = dir.cross(mid, new Vector3f());
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize(w / 2);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, bl = rgb & 0xFF;
        int a0 = (int) (Mth.clamp(at, 0, 1) * 255), a1 = (int) (Mth.clamp(ah, 0, 1) * 255);
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            b.addVertex(m, t.x, t.y, t.z).setColor(r, g, bl, a0);
            b.addVertex(m, t.x + side.x * sgn, t.y + side.y * sgn, t.z + side.z * sgn).setColor(r, g, bl, 0);
            b.addVertex(m, h.x + side.x * sgn, h.y + side.y * sgn, h.z + side.z * sgn).setColor(r, g, bl, 0);
            b.addVertex(m, h.x, h.y, h.z).setColor(r, g, bl, a1);
        }
    }

    /** A soft round glow at {@code c} on the dome, facing its centre. */
    private static void glow(BufferBuilder b, Matrix4f m, Vector3f c, float radius, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        Vector3f n = new Vector3f(c).normalize();
        float[] ax = new float[3], ay = new float[3];
        FxDraw.basis(n.x, n.y, n.z, ax, ay);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, bl = rgb & 0xFF;
        int a0 = (int) (Mth.clamp(alpha, 0, 1) * 255), a1 = (int) (a0 * 0.4f);
        int seg = 12;
        float mid = 0.3f;
        for (int i = 0; i < seg; i++) {
            float t0 = Mth.TWO_PI * i / seg, t1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
            float x0 = ax[0] * c0 + ay[0] * s0, y0 = ax[1] * c0 + ay[1] * s0, z0 = ax[2] * c0 + ay[2] * s0;
            float x1 = ax[0] * c1 + ay[0] * s1, y1 = ax[1] * c1 + ay[1] * s1, z1 = ax[2] * c1 + ay[2] * s1;
            float rm = radius * mid;
            b.addVertex(m, c.x, c.y, c.z).setColor(r, g, bl, a0);
            b.addVertex(m, c.x + x0 * rm, c.y + y0 * rm, c.z + z0 * rm).setColor(r, g, bl, a1);
            b.addVertex(m, c.x + x1 * rm, c.y + y1 * rm, c.z + z1 * rm).setColor(r, g, bl, a1);
            b.addVertex(m, c.x, c.y, c.z).setColor(r, g, bl, a0);
            b.addVertex(m, c.x + x0 * rm, c.y + y0 * rm, c.z + z0 * rm).setColor(r, g, bl, a1);
            b.addVertex(m, c.x + x0 * radius, c.y + y0 * radius, c.z + z0 * radius).setColor(r, g, bl, 0);
            b.addVertex(m, c.x + x1 * radius, c.y + y1 * radius, c.z + z1 * radius).setColor(r, g, bl, 0);
            b.addVertex(m, c.x + x1 * rm, c.y + y1 * rm, c.z + z1 * rm).setColor(r, g, bl, a1);
        }
    }

    // --- the ground where a star lands -----------------------------------------------------------------------------------------

    /** Where a star lands near enough, the ground lit by its flash (a world pass, under fog like anything there). */
    static void renderImpacts(FxDraw d, long time, float partial) {
        for (Star s : STARS) {
            float age = time - s.start + partial;
            if (age < s.duration - 6) continue;
            Vec3 hit = s.at(1);
            if (hit.distanceToSqr(d.eye()) > 160 * 160) continue;
            float q = (age - s.duration) / IMPACT_TICKS;
            float a = q < 0 ? Mth.clamp(1 + q * IMPACT_TICKS / 6f, 0, 1) * 0.5f : (float) Math.pow(1 - Math.min(1, q), 1.5);
            d.floor(hit.y);
            d.flatGlow(hit.x, hit.y + 0.1, hit.z, 8 + 10 * Math.max(0, q), 0xDCE8FF, 0.6f * a);
            d.glowDisc(hit.x, hit.y + 2, hit.z, 4 + 6 * Math.max(0, q), 0xFFFFFF, 0.7f * a);
            if (q >= 0 && q < 1) {
                float r0 = 1 + 18 * q;
                d.ring(hit.x, hit.y + 0.4, hit.z, 0, 1, 0, r0, r0 + 1.5f, 0xE8F0FF, 0.5f * a, true);
            }
        }
    }
}
