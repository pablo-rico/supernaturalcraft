package org.papiricoh.supernaturalcraft.client.magic.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw;
import org.papiricoh.supernaturalcraft.client.fx.BeamFx;

/**
 * The magic's drawing kit (v0.21): every primitive takes world coordinates and draws them relative to an origin (the camera in a
 * world stage, an entity in its renderer), so far-off places keep their precision. Light is additive ({@link MagicRenderTypes});
 * {@code dark*} primitives blend normally (smoke, a rift's inside, scorch). Each call fetches its own buffer, so calls can be mixed
 * freely; nothing here allocates per vertex.
 *
 * <p>Two things keep the light readable: a <b>floor</b> ({@link #floor(double)}, set per effect by {@link MagicFxManager}) under
 * which camera-facing glows fade out smoothly instead of being cut by the ground in a hard line (and which they light with a round
 * pool), and by day ({@link #day}) a painted "ink" pass under cores, edges and decals so they do not vanish against sunlit ground.
 */
public final class FxDraw {

    public final PoseStack pose;
    public final MultiBufferSource buffers;
    public final Camera camera;
    /** Partial tick and the effects' clock (ticks + partial). */
    public final float partial, now;
    private final double ox, oy, oz;
    /** The eye, relative to the origin. */
    private final float ex, ey, ez;
    private final float lx, ly, lz, ux, uy, uz;
    /** How bright the day is where the camera stands (0 night or underground, 1 open noon): the weight of the ink pass. */
    public final float day;
    /** The floor under the effect being drawn (world y), or NaN. */
    private double floorY = Double.NaN;

    /** A glow's brightness from its centre (index 0) to its rim, at {@link #RING_R} of its radius: a soft bell. */
    private static final float[] RING_R = {0, 0.14f, 0.32f, 0.55f, 0.78f, 1f};
    private static final float[] RING_A = bell(RING_R);
    /** The same, finer, for glows the floor cuts (the fade needs vertices near it). */
    private static final float[] CUT_R = {0, 0.1f, 0.2f, 0.31f, 0.43f, 0.56f, 0.7f, 0.85f, 1f};
    private static final float[] CUT_A = bell(CUT_R);
    /** Smoke's flatter bell: it has a body. */
    private static final float[] DARK_R = {0, 0.3f, 0.55f, 0.78f, 1f};
    private static final float[] DARK_A = {1, 0.92f, 0.62f, 0.25f, 0};

    private static float[] bell(float[] r) {
        float[] a = new float[r.length];
        float floor = (float) Math.exp(-4.5);
        for (int i = 0; i < r.length; i++) a[i] = Math.max(0, ((float) Math.exp(-4.5 * r[i] * r[i]) - floor) / (1 - floor));
        return a;
    }

    public FxDraw(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 origin, float partial, float now) {
        this(pose, buffers, camera, origin, camera.getPosition(), camera.getLeftVector(), camera.getUpVector(), partial, now);
    }

    /**
     * Drawing in view space, as the first-person hand pass does: the eye on the origin, looking down -z, left -x, up +y.
     * Coordinates are then view-space ones, not world ones ({@link #eye()} still answers the camera's world position).
     */
    public static FxDraw view(PoseStack pose, MultiBufferSource buffers, Camera camera, float partial, float now) {
        return new FxDraw(pose, buffers, camera, Vec3.ZERO, Vec3.ZERO, new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0), partial, now);
    }

    private FxDraw(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 origin, Vec3 eye, Vector3f l, Vector3f u,
                   float partial, float now) {
        this.pose = pose;
        this.buffers = buffers;
        this.camera = camera;
        this.partial = partial;
        this.now = now;
        this.ox = origin.x;
        this.oy = origin.y;
        this.oz = origin.z;
        this.ex = (float) (eye.x - ox);
        this.ey = (float) (eye.y - oy);
        this.ez = (float) (eye.z - oz);
        this.lx = l.x;
        this.ly = l.y;
        this.lz = l.z;
        this.ux = u.x;
        this.uy = u.y;
        this.uz = u.z;
        this.day = MagicFxManager.daylight(partial);
    }

    public Vec3 eye() {
        return camera.getPosition();
    }

    private double dist2(double x, double y, double z) {
        double dx = x - ox - ex, dy = y - oy - ey, dz = z - oz - ez;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * How much of a camera-facing shape of {@code size} (its radius) centred at (x, y, z) to draw so it never swallows the view
     * (v0.21): nothing when it sits on the eye, rising to whole once it is clear of it by about its own size. A small spark
     * passing close stays nearly whole; a wide glow or a seal flown into the face fades away.
     */
    public float near(double x, double y, double z, float size) {
        float d = (float) Math.sqrt(dist2(x, y, z));
        float k = Mth.clamp((d - 0.3f) / (0.5f + size * 0.9f), 0, 1);
        return k * k * (3 - 2 * k);
    }

    /** The floor (world y) under what is drawn next: glows reaching below it fade out over it. NaN for none. */
    public void floor(double y) {
        floorY = y;
    }

    public double floor() {
        return floorY;
    }

    private boolean inked() {
        return day > 0.02f;
    }

    private VertexConsumer ink() {
        return buffers.getBuffer(MagicRenderTypes.ink());
    }

    // --- vertices --------------------------------------------------------------------------------------------------------

    private static void c(VertexConsumer vc, Matrix4f m, float x, float y, float z, int r, int g, int b, int a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a);
    }

    private static void t(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(p, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(p, 0, 1, 0);
    }

    private static int al(float a) {
        return (int) (Mth.clamp(a, 0f, 1f) * 255);
    }

    /** A vertex whose alpha fades to nothing as it nears the floor at {@code fl} (relative y) over {@code fh} (0: no floor). */
    private static void cf(VertexConsumer vc, Matrix4f m, float x, float y, float z, int r, int g, int b, float a, float fl, float fh) {
        if (fh > 0) {
            float k = Mth.clamp((y - fl) / fh, 0, 1);
            a *= k * k * (3 - 2 * k);
        }
        c(vc, m, x, y, z, r, g, b, al(a));
    }

    private VertexConsumer glow() {
        return buffers.getBuffer(MagicRenderTypes.glow());
    }

    // --- light, camera-facing -----------------------------------------------------------------------------------------------

    /**
     * A soft round glow facing the camera: a bell of light, bright in the middle, nothing at the rim. Where it reaches below the
     * floor it fades out over it (never a hard line where it meets the ground) and lights the floor with a round pool instead.
     */
    public void glowDisc(double x, double y, double z, float radius, int rgb, float alpha) {
        alpha *= near(x, y, z, radius);
        if (alpha <= 0.004f || radius <= 0) return;
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        boolean cut = !Double.isNaN(floorY) && y - radius < floorY + 0.1;
        float fl = cut ? (float) (floorY - oy) : 0, fh = cut ? Math.max(0.35f, radius * 0.5f) : 0;
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        fan(glow(), m, cx, cy, cz, radius, r, g, b, alpha, cut ? CUT_R : RING_R, cut ? CUT_A : RING_A, cut ? 18 : 12, fl, fh);
        if (inked()) fan(ink(), m, cx, cy, cz, radius * 0.45f, r, g, b, alpha * day * 0.55f, RING_R, RING_A, 12, fl, fh);
        if (cut && y > floorY - radius * 0.5) {
            // The light it would have thrown on the ground, as a round pool.
            float h = (float) Math.max(0, y - floorY), k = 1 - Mth.clamp(h / radius, 0, 1);
            pool(x, floorY + 0.03, z, radius * (0.75f + 0.35f * k), rgb, alpha * 0.32f * k);
        }
    }

    /** Concentric rings of a camera-facing fan, faded near the floor at {@code fl} over {@code fh} (0: no floor). */
    private void fan(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float radius, int r, int g, int b, float alpha,
                     float[] rr, float[] ra, int seg, float fl, float fh) {
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float x0 = lx * c0 + ux * s0, y0 = ly * c0 + uy * s0, z0 = lz * c0 + uz * s0;
            float x1 = lx * c1 + ux * s1, y1 = ly * c1 + uy * s1, z1 = lz * c1 + uz * s1;
            for (int k = 0; k < rr.length - 1; k++) {
                float ri = rr[k] * radius, ro = rr[k + 1] * radius, ai = ra[k] * alpha, ao = ra[k + 1] * alpha;
                cf(vc, m, cx + x0 * ri, cy + y0 * ri, cz + z0 * ri, r, g, b, ai, fl, fh);
                cf(vc, m, cx + x0 * ro, cy + y0 * ro, cz + z0 * ro, r, g, b, ao, fl, fh);
                cf(vc, m, cx + x1 * ro, cy + y1 * ro, cz + z1 * ro, r, g, b, ao, fl, fh);
                cf(vc, m, cx + x1 * ri, cy + y1 * ri, cz + z1 * ri, r, g, b, ai, fl, fh);
            }
        }
    }

    /** A soft round pool of light lying flat (a bell like {@link #glowDisc}), no ink. */
    private void pool(double x, double y, double z, float radius, int rgb, float alpha) {
        if (alpha <= 0.004f || radius <= 0) return;
        flat(glow(), x, y, z, radius, rgb, alpha);
    }

    private void flat(VertexConsumer vc, double x, double y, double z, float radius, int rgb, float alpha) {
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            for (int k = 0; k < RING_R.length - 1; k++) {
                float ri = RING_R[k] * radius, ro = RING_R[k + 1] * radius;
                int ai = al(RING_A[k] * alpha), ao = al(RING_A[k + 1] * alpha);
                c(vc, m, cx + c0 * ri, cy, cz + s0 * ri, r, g, b, ai);
                c(vc, m, cx + c0 * ro, cy, cz + s0 * ro, r, g, b, ao);
                c(vc, m, cx + c1 * ro, cy, cz + s1 * ro, r, g, b, ao);
                c(vc, m, cx + c1 * ri, cy, cz + s1 * ri, r, g, b, ai);
            }
        }
    }

    /** A four-pointed star of light (a glow and four long thin rays), turned by {@code roll} radians. */
    public void star(double x, double y, double z, float radius, float roll, int rgb, float alpha) {
        alpha *= near(x, y, z, radius);
        if (alpha <= 0.004f) return;
        glowDisc(x, y, z, radius * 0.55f, rgb, alpha);
        rays(glow(), x, y, z, radius, roll, rgb, alpha);
        if (inked()) rays(ink(), x, y, z, radius * 0.8f, roll, FxColor.mix(rgb, 0xFFFFFF, 0.3f), alpha * day * 0.6f);
    }

    /**
     * Four streaks of light: each a long thin kite whose alpha runs from its heart to nothing at its sides and tip (feathered, never
     * a hard-edged wedge), a narrow bright core inside a wider soft one; the width stays streak-thin however large the star.
     */
    private void rays(VertexConsumer vc, double x, double y, double z, float radius, float roll, int rgb, float alpha) {
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha), ah = al(alpha * 0.45f);
        float w = Math.min(radius * 0.09f, 0.22f + radius * 0.025f);
        for (int k = 0; k < 4; k++) {
            float ang = roll + k * Mth.HALF_PI;
            float dc = Mth.cos(ang), ds = Mth.sin(ang);
            float dx = lx * dc + ux * ds, dy = ly * dc + uy * ds, dz = lz * dc + uz * ds;
            float px = -lx * ds + ux * dc, py = -ly * ds + uy * dc, pz = -lz * ds + uz * dc;
            for (int pass = 0; pass < 2; pass++) {
                float ww = pass == 0 ? w : w * 0.35f, len = pass == 0 ? radius : radius * 0.85f;
                int ac = pass == 0 ? ah : a;
                // The sides sit a little out along the streak, so the kite tapers both ways from a soft shoulder.
                float sh = ww * 0.6f;
                c(vc, m, cx, cy, cz, r, g, b, ac);
                c(vc, m, cx + px * ww + dx * sh, cy + py * ww + dy * sh, cz + pz * ww + dz * sh, r, g, b, 0);
                c(vc, m, cx + dx * len, cy + dy * len, cz + dz * len, r, g, b, 0);
                c(vc, m, cx - px * ww + dx * sh, cy - py * ww + dy * sh, cz - pz * ww + dz * sh, r, g, b, 0);
            }
        }
    }

    /** A textured quad facing the camera, {@code size} blocks across, turned by {@code roll} radians. */
    public void billboard(ResourceLocation texture, double x, double y, double z, float size, float roll, int rgb, float alpha) {
        alpha *= near(x, y, z, size / 2);
        float h = size / 2, c = Mth.cos(roll) * h, s = Mth.sin(roll) * h;
        plane(texture, x, y, z, lx * c + ux * s, ly * c + uy * s, lz * c + uz * s, -lx * s + ux * c, -ly * s + uy * c,
                -lz * s + uz * c, rgb, alpha);
    }

    /** A textured quad standing upright and turned toward the camera about the vertical (a seal, a rift). */
    public void standing(ResourceLocation texture, double x, double y, double z, float width, float height, int rgb, float alpha) {
        float dx = (float) (x - ox) - ex, dz = (float) (z - oz) - ez;
        float len = Mth.sqrt(dx * dx + dz * dz);
        float sx = len < 1e-4f ? 1 : -dz / len, sz = len < 1e-4f ? 0 : dx / len;
        plane(texture, x, y, z, sx * width / 2, 0, sz * width / 2, 0, height / 2, 0, rgb, alpha);
    }

    /** A textured quad: corners {@code c ± u ± v} (u along the texture's x, v along its y). */
    public void plane(ResourceLocation texture, double x, double y, double z, float uX, float uY, float uZ, float vX, float vY, float vZ,
                      int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        quad(buffers.getBuffer(MagicRenderTypes.glow(texture)), x, y, z, uX, uY, uZ, vX, vY, vZ, rgb, alpha);
        if (inked()) quad(buffers.getBuffer(MagicRenderTypes.ink(texture)), x, y, z, uX, uY, uZ, vX, vY, vZ, rgb, alpha * day * 0.6f);
    }

    private void quad(VertexConsumer vc, double x, double y, double z, float uX, float uY, float uZ, float vX, float vY, float vZ,
                      int rgb, float alpha) {
        PoseStack.Pose p = pose.last();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha);
        t(vc, p, cx - uX - vX, cy - uY - vY, cz - uZ - vZ, 0, 1, r, g, b, a);
        t(vc, p, cx + uX - vX, cy + uY - vY, cz + uZ - vZ, 1, 1, r, g, b, a);
        t(vc, p, cx + uX + vX, cy + uY + vY, cz + uZ + vZ, 1, 0, r, g, b, a);
        t(vc, p, cx - uX + vX, cy - uY + vY, cz - uZ + vZ, 0, 0, r, g, b, a);
    }

    /** A texture laid flat (or tilted: {@code nx, ny, nz} is its normal), {@code radius} from centre to edge, turned by {@code spin}. */
    public void disc(ResourceLocation texture, double x, double y, double z, float nx, float ny, float nz, float radius, float spin,
                     int rgb, float alpha) {
        // A disc stood up toward the camera (a seal, a sigil) thins out when it comes at the eye; one lying flat stays.
        if (Math.abs(ny) < 0.5f * Mth.sqrt(nx * nx + ny * ny + nz * nz)) alpha *= near(x, y, z, radius);
        float[] ax = new float[3], ay = new float[3];
        basis(nx, ny, nz, ax, ay);
        float c = Mth.cos(spin) * radius, s = Mth.sin(spin) * radius;
        plane(texture, x, y, z, ax[0] * c + ay[0] * s, ax[1] * c + ay[1] * s, ax[2] * c + ay[2] * s,
                -ax[0] * s + ay[0] * c, -ax[1] * s + ay[1] * c, -ax[2] * s + ay[2] * c, rgb, alpha);
    }

    /** A flat texture on the ground (normal up). */
    public void decal(ResourceLocation texture, double x, double y, double z, float radius, float spin, int rgb, float alpha) {
        disc(texture, x, y, z, 0, 1, 0, radius, spin, rgb, alpha);
    }

    /**
     * A texture swept round a circle in a plane ({@code nx, ny, nz} its normal) as a pie wipe: only the first {@code fraction} of
     * the turn is drawn (a seal being written, a circle closing).
     */
    public void wipe(ResourceLocation texture, double x, double y, double z, float nx, float ny, float nz, float radius, float spin,
                     float fraction, int rgb, float alpha) {
        if (alpha <= 0.004f || fraction <= 0) return;
        if (fraction >= 1) {
            disc(texture, x, y, z, nx, ny, nz, radius, spin, rgb, alpha);
            return;
        }
        wedges(buffers.getBuffer(MagicRenderTypes.glow(texture)), x, y, z, nx, ny, nz, radius, spin, fraction, rgb, alpha);
        if (inked()) wedges(buffers.getBuffer(MagicRenderTypes.ink(texture)), x, y, z, nx, ny, nz, radius, spin, fraction, rgb,
                alpha * day * 0.6f);
    }

    private void wedges(VertexConsumer vc, double x, double y, double z, float nx, float ny, float nz, float radius, float spin,
                        float fraction, int rgb, float alpha) {
        float[] ax = new float[3], ay = new float[3];
        basis(nx, ny, nz, ax, ay);
        PoseStack.Pose p = pose.last();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha);
        int seg = 48, n = Math.max(1, Math.round(seg * fraction));
        float end = Mth.TWO_PI * fraction;
        for (int i = 0; i < n; i++) {
            float a0 = end * i / n, a1 = end * (i + 1) / n;
            // Each wedge runs from the centre to the square's edge, so the whole square is covered at the end.
            float k0 = 1f / Math.max(Math.abs(Mth.cos(a0)), Math.abs(Mth.sin(a0)));
            float k1 = 1f / Math.max(Math.abs(Mth.cos(a1)), Math.abs(Mth.sin(a1)));
            float u0 = Mth.cos(a0) * k0, v0 = -Mth.sin(a0) * k0, u1 = Mth.cos(a1) * k1, v1 = -Mth.sin(a1) * k1;
            float s0 = Mth.cos(spin), s1 = Mth.sin(spin);
            float px0 = (u0 * s0 - v0 * s1) * radius, py0 = (u0 * s1 + v0 * s0) * radius;
            float px1 = (u1 * s0 - v1 * s1) * radius, py1 = (u1 * s1 + v1 * s0) * radius;
            t(vc, p, cx, cy, cz, 0.5f, 0.5f, r, g, b, a);
            t(vc, p, cx + ax[0] * px0 + ay[0] * py0, cy + ax[1] * px0 + ay[1] * py0, cz + ax[2] * px0 + ay[2] * py0,
                    0.5f + u0 / 2, 0.5f - v0 / 2, r, g, b, a);
            t(vc, p, cx + ax[0] * px1 + ay[0] * py1, cy + ax[1] * px1 + ay[1] * py1, cz + ax[2] * px1 + ay[2] * py1,
                    0.5f + u1 / 2, 0.5f - v1 / 2, r, g, b, a);
            t(vc, p, cx, cy, cz, 0.5f, 0.5f, r, g, b, a);
        }
    }

    // --- light, shapes -----------------------------------------------------------------------------------------------------

    /** A flat ring of light, lying in the plane with normal {@code n}; {@code soft} fades both its edges. */
    public void ring(double x, double y, double z, float nx, float ny, float nz, float r0, float r1, int rgb, float alpha, boolean soft) {
        if (alpha <= 0.004f || r1 <= r0) return;
        ring(glow(), x, y, z, nx, ny, nz, r0, r1, rgb, alpha, soft);
        if (inked()) ring(ink(), x, y, z, nx, ny, nz, r0, r1, rgb, alpha * day * 0.5f, soft);
    }

    private void ring(VertexConsumer vc, double x, double y, double z, float nx, float ny, float nz, float r0, float r1, int rgb,
                      float alpha, boolean soft) {
        float[] ax = new float[3], ay = new float[3];
        basis(nx, ny, nz, ax, ay);
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha);
        int seg = Mth.clamp((int) (r1 * 10), 20, 96);
        float mid = (r0 + r1) / 2;
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float dx0 = ax[0] * c0 + ay[0] * s0, dy0 = ax[1] * c0 + ay[1] * s0, dz0 = ax[2] * c0 + ay[2] * s0;
            float dx1 = ax[0] * c1 + ay[0] * s1, dy1 = ax[1] * c1 + ay[1] * s1, dz1 = ax[2] * c1 + ay[2] * s1;
            if (soft) {
                band(vc, m, cx, cy, cz, dx0, dy0, dz0, dx1, dy1, dz1, r0, mid, r, g, b, 0, a);
                band(vc, m, cx, cy, cz, dx0, dy0, dz0, dx1, dy1, dz1, mid, r1, r, g, b, a, 0);
            } else {
                band(vc, m, cx, cy, cz, dx0, dy0, dz0, dx1, dy1, dz1, r0, r1, r, g, b, a, a);
            }
        }
    }

    private static void band(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float dx0, float dy0, float dz0, float dx1,
                             float dy1, float dz1, float ra, float rb, int r, int g, int b, int aa, int ab) {
        c(vc, m, cx + dx0 * ra, cy + dy0 * ra, cz + dz0 * ra, r, g, b, aa);
        c(vc, m, cx + dx0 * rb, cy + dy0 * rb, cz + dz0 * rb, r, g, b, ab);
        c(vc, m, cx + dx1 * rb, cy + dy1 * rb, cz + dz1 * rb, r, g, b, ab);
        c(vc, m, cx + dx1 * ra, cy + dy1 * ra, cz + dz1 * ra, r, g, b, aa);
    }

    /** A soft round pool of light lying flat: bright in the middle, nothing at the rim. */
    public void flatGlow(double x, double y, double z, float radius, int rgb, float alpha) {
        if (alpha <= 0.004f || radius <= 0) return;
        flat(glow(), x, y, z, radius, rgb, alpha);
        if (inked()) flat(ink(), x, y, z, radius * 0.7f, rgb, alpha * day * 0.35f);
    }

    /** A flat stroke of light on the ground from (x0, z0) to (x1, z1) at height y, its edges soft. */
    public void groundLine(double x0, double z0, double x1, double z1, double y, float width, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        float ax = (float) (x0 - ox), az = (float) (z0 - oz), bx = (float) (x1 - ox), bz = (float) (z1 - oz), cy = (float) (y - oy);
        float dx = bx - ax, dz = bz - az, len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 1e-4f) return;
        float sx = -dz / len * width / 2, sz = dx / len * width / 2;
        groundLine(glow(), ax, az, bx, bz, cy, sx, sz, rgb, alpha);
        if (inked()) groundLine(ink(), ax, az, bx, bz, cy, sx, sz, rgb, alpha * day * 0.65f);
    }

    private void groundLine(VertexConsumer vc, float ax, float az, float bx, float bz, float cy, float sx, float sz, int rgb, float alpha) {
        Matrix4f m = pose.last().pose();
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha);
        c(vc, m, ax, cy, az, r, g, b, a);
        c(vc, m, ax + sx, cy, az + sz, r, g, b, 0);
        c(vc, m, bx + sx, cy, bz + sz, r, g, b, 0);
        c(vc, m, bx, cy, bz, r, g, b, a);
        c(vc, m, ax, cy, az, r, g, b, a);
        c(vc, m, bx, cy, bz, r, g, b, a);
        c(vc, m, bx - sx, cy, bz - sz, r, g, b, 0);
        c(vc, m, ax - sx, cy, az - sz, r, g, b, 0);
    }

    /** A flat ring on the ground. */
    public void groundRing(double x, double y, double z, float r0, float r1, int rgb, float alpha, boolean soft) {
        ring(x, y, z, 0, 1, 0, r0, r1, rgb, alpha, soft);
    }

    /**
     * A star-shaped ring on the ground: its radius swings between {@code r} and {@code r + spike} {@code points} times round
     * (thorns, a hex's teeth).
     */
    public void spikedRing(double x, double y, double z, float r, float spike, int points, float spin, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        spikes(glow(), x, y, z, r, spike, points, spin, rgb, alpha);
        if (inked()) spikes(ink(), x, y, z, r, spike, points, spin, rgb, alpha * day * 0.55f);
    }

    private void spikes(VertexConsumer vc, double x, double y, double z, float r, float spike, int points, float spin, int rgb, float alpha) {
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int cr = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha), tip = al(alpha * 0.35f);
        int seg = points * 2;
        float in = Math.max(0, r - 0.12f);
        for (int i = 0; i < seg; i++) {
            float a0 = spin + Mth.TWO_PI * i / seg, a1 = spin + Mth.TWO_PI * (i + 1) / seg;
            boolean out0 = i % 2 == 0;
            float o0 = out0 ? r + spike : r, o1 = out0 ? r : r + spike;
            // Feathered: the thorns' points fade (no hard-edged plane where a ring is wide).
            c(vc, m, cx + Mth.cos(a0) * in, cy, cz + Mth.sin(a0) * in, cr, g, b, 0);
            c(vc, m, cx + Mth.cos(a0) * o0, cy, cz + Mth.sin(a0) * o0, cr, g, b, out0 ? tip : a);
            c(vc, m, cx + Mth.cos(a1) * o1, cy, cz + Mth.sin(a1) * o1, cr, g, b, out0 ? a : tip);
            c(vc, m, cx + Mth.cos(a1) * in, cy, cz + Mth.sin(a1) * in, cr, g, b, 0);
        }
    }

    /** An upright tube of light: alpha {@code a0} at its foot, {@code a1} at its top. */
    public void cylinder(double x, double y, double z, float radius, float height, int rgb, float a0, float a1) {
        if (Math.max(a0, a1) <= 0.004f || radius <= 0) return;
        tube(glow(), x, y, z, radius, height, rgb, a0, a1);
        if (inked()) tube(ink(), x, y, z, radius, height, rgb, a0 * day * 0.22f, a1 * day * 0.22f);
    }

    private void tube(VertexConsumer vc, double x, double y, double z, float radius, float height, int rgb, float a0, float a1) {
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, al0 = al(a0), al1 = al(a1);
        int seg = Mth.clamp((int) (radius * 8), 12, 64);
        for (int i = 0; i < seg; i++) {
            float t0 = Mth.TWO_PI * i / seg, t1 = Mth.TWO_PI * (i + 1) / seg;
            float x0 = Mth.cos(t0) * radius, z0 = Mth.sin(t0) * radius, x1 = Mth.cos(t1) * radius, z1 = Mth.sin(t1) * radius;
            c(vc, m, cx + x0, cy, cz + z0, r, g, b, al0);
            c(vc, m, cx + x1, cy, cz + z1, r, g, b, al0);
            c(vc, m, cx + x1, cy + height, cz + z1, r, g, b, al1);
            c(vc, m, cx + x0, cy + height, cz + z0, r, g, b, al1);
        }
    }

    /**
     * A texture wrapped round an upright tube ({@code glyph_strip}: a band of glyphs), tiled along the circumference by the
     * texture's own aspect and turned by {@code spin} radians.
     */
    public void band(ResourceLocation texture, double x, double y, double z, float radius, float height, float spin, int rgb, float a0,
                     float a1) {
        if (Math.max(a0, a1) <= 0.004f || radius <= 0) return;
        strip(buffers.getBuffer(MagicRenderTypes.glow(texture)), texture, x, y, z, radius, height, spin, rgb, a0, a1);
        if (inked()) strip(buffers.getBuffer(MagicRenderTypes.ink(texture)), texture, x, y, z, radius, height, spin, rgb, a0 * day * 0.5f,
                a1 * day * 0.5f);
    }

    private void strip(VertexConsumer vc, ResourceLocation texture, double x, double y, double z, float radius, float height, float spin,
                       int rgb, float a0, float a1) {
        PoseStack.Pose p = pose.last();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, al0 = al(a0), al1 = al(a1);
        int seg = Mth.clamp((int) (radius * 10), 16, 64);
        float tiles = Math.max(1, Math.round(Mth.TWO_PI * radius / (height * MagicTextures.aspect(texture))));
        for (int i = 0; i < seg; i++) {
            float t0 = spin + Mth.TWO_PI * i / seg, t1 = spin + Mth.TWO_PI * (i + 1) / seg;
            float u0 = tiles * i / seg, u1 = tiles * (i + 1) / seg;
            float x0 = Mth.cos(t0) * radius, z0 = Mth.sin(t0) * radius, x1 = Mth.cos(t1) * radius, z1 = Mth.sin(t1) * radius;
            t(vc, p, cx + x0, cy, cz + z0, u0, 1, r, g, b, al0);
            t(vc, p, cx + x1, cy, cz + z1, u1, 1, r, g, b, al0);
            t(vc, p, cx + x1, cy + height, cz + z1, u1, 0, r, g, b, al1);
            t(vc, p, cx + x0, cy + height, cz + z0, u0, 0, r, g, b, al1);
        }
    }

    /**
     * A texture laid round a flat ring ({@code glyph_strip}: a circle of glyphs), lying in the plane with normal {@code n}: U runs
     * round the ring, tiled by the texture's aspect, V across it from {@code r1} (top of the strip) to {@code r0}.
     */
    public void glyphRing(ResourceLocation texture, double x, double y, double z, float nx, float ny, float nz, float r0, float r1,
                          float spin, int rgb, float alpha) {
        if (alpha <= 0.004f || r1 <= r0) return;
        glyphs(buffers.getBuffer(MagicRenderTypes.glow(texture)), texture, x, y, z, nx, ny, nz, r0, r1, spin, rgb, alpha);
        if (inked()) glyphs(buffers.getBuffer(MagicRenderTypes.ink(texture)), texture, x, y, z, nx, ny, nz, r0, r1, spin, rgb,
                alpha * day * 0.55f);
    }

    private void glyphs(VertexConsumer vc, ResourceLocation texture, double x, double y, double z, float nx, float ny, float nz, float r0,
                        float r1, float spin, int rgb, float alpha) {
        float[] ax = new float[3], ay = new float[3];
        basis(nx, ny, nz, ax, ay);
        PoseStack.Pose p = pose.last();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha);
        float mid = (r0 + r1) / 2, width = r1 - r0;
        float tiles = Math.max(1, Math.round(Mth.TWO_PI * mid / (width * MagicTextures.aspect(texture))));
        int seg = Mth.clamp((int) (mid * 12), 24, 96);
        for (int i = 0; i < seg; i++) {
            float t0 = spin + Mth.TWO_PI * i / seg, t1 = spin + Mth.TWO_PI * (i + 1) / seg;
            float u0 = tiles * i / seg, u1 = tiles * (i + 1) / seg;
            float c0 = Mth.cos(t0), s0 = Mth.sin(t0), c1 = Mth.cos(t1), s1 = Mth.sin(t1);
            float dx0 = ax[0] * c0 + ay[0] * s0, dy0 = ax[1] * c0 + ay[1] * s0, dz0 = ax[2] * c0 + ay[2] * s0;
            float dx1 = ax[0] * c1 + ay[0] * s1, dy1 = ax[1] * c1 + ay[1] * s1, dz1 = ax[2] * c1 + ay[2] * s1;
            t(vc, p, cx + dx0 * r0, cy + dy0 * r0, cz + dz0 * r0, u0, 1, r, g, b, a);
            t(vc, p, cx + dx0 * r1, cy + dy0 * r1, cz + dz0 * r1, u0, 0, r, g, b, a);
            t(vc, p, cx + dx1 * r1, cy + dy1 * r1, cz + dz1 * r1, u1, 0, r, g, b, a);
            t(vc, p, cx + dx1 * r0, cy + dy1 * r0, cz + dz1 * r0, u1, 1, r, g, b, a);
        }
    }

    /** A thin strip of light from a to b turned to the camera, alpha {@code a0} at a and {@code a1} at b. */
    public void ribbon(double x0, double y0, double z0, double x1, double y1, double z1, float width, int rgb, float a0, float a1) {
        taper(x0, y0, z0, x1, y1, z1, width, width, rgb, a0, a1);
    }

    /** As {@link #ribbon}, tapering from {@code w0} to {@code w1}. */
    public void taper(double x0, double y0, double z0, double x1, double y1, double z1, float w0, float w1, int rgb, float a0, float a1) {
        if (Math.max(a0, a1) <= 0.004f) return;
        float ax = (float) (x0 - ox), ay = (float) (y0 - oy), az = (float) (z0 - oz), bx = (float) (x1 - ox), by = (float) (y1 - oy),
                bz = (float) (z1 - oz);
        ribbon(glow(), pose.last().pose(), ax, ay, az, bx, by, bz, w0, w1, rgb, al(a0), al(a1));
        if (inked()) {
            float k = day * 0.6f;
            ribbon(ink(), pose.last().pose(), ax, ay, az, bx, by, bz, w0 * 0.7f, w1 * 0.7f, rgb, al(a0 * k), al(a1 * k));
        }
    }

    /**
     * A strip of light from a to b turned to the camera with soft sides (bright along its middle, nothing at its edges): a beam or
     * a pillar that fades from {@code a0} at a to {@code a1} at b. With {@code core}, a thin white-hot line runs down its middle.
     */
    public void softBeam(double x0, double y0, double z0, double x1, double y1, double z1, float width, int rgb, float a0, float a1,
                         boolean core) {
        if (Math.max(a0, a1) <= 0.004f || width <= 0) return;
        float ax = (float) (x0 - ox), ay = (float) (y0 - oy), az = (float) (z0 - oz), bx = (float) (x1 - ox), by = (float) (y1 - oy),
                bz = (float) (z1 - oz);
        Matrix4f m = pose.last().pose();
        soft(glow(), m, ax, ay, az, bx, by, bz, width, rgb, a0, a1);
        if (core) soft(glow(), m, ax, ay, az, bx, by, bz, width * 0.28f, 0xFFFFFF, Math.min(1, a0 * 1.2f), Math.min(1, a1 * 1.2f));
        if (inked()) {
            float k = day * 0.55f;
            soft(ink(), m, ax, ay, az, bx, by, bz, width * 0.6f, FxColor.mix(rgb, 0xFFFFFF, 0.35f), a0 * k, a1 * k);
        }
    }

    private void soft(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float width, int rgb,
                      float a0, float a1) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) / 2 - ex, my = (ay + by) / 2 - ey, mz = (az + bz) / 2 - ez;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        float len = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1e-6f) return;
        float h = width / 2 / len;
        sx *= h;
        sy *= h;
        sz *= h;
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, al0 = al(a0), al1 = al(a1);
        for (int side = -1; side <= 1; side += 2) {
            c(vc, m, ax, ay, az, r, g, b, al0);
            c(vc, m, ax + sx * side, ay + sy * side, az + sz * side, r, g, b, 0);
            c(vc, m, bx + sx * side, by + sy * side, bz + sz * side, r, g, b, 0);
            c(vc, m, bx, by, bz, r, g, b, al1);
        }
    }

    private void ribbon(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float w0, float w1,
                        int rgb, int a0, int a1) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) / 2 - ex, my = (ay + by) / 2 - ey, mz = (az + bz) / 2 - ez;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        float len = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1e-6f) return;
        sx /= len;
        sy /= len;
        sz /= len;
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        float h0 = w0 / 2, h1 = w1 / 2;
        c(vc, m, ax + sx * h0, ay + sy * h0, az + sz * h0, r, g, b, a0);
        c(vc, m, ax - sx * h0, ay - sy * h0, az - sz * h0, r, g, b, a0);
        c(vc, m, bx - sx * h1, by - sy * h1, bz - sz * h1, r, g, b, a1);
        c(vc, m, bx + sx * h1, by + sy * h1, bz + sz * h1, r, g, b, a1);
    }

    /**
     * A bolt of lightning from a to b: {@code segments} jagged steps (fixed by {@code seed}), a coloured sheath round a white core,
     * and a fork or two when {@code forks}.
     */
    public void lightning(double x0, double y0, double z0, double x1, double y1, double z1, long seed, int segments, float jag,
                          float width, int rgb, float alpha, boolean forks) {
        if (alpha <= 0.004f || segments <= 0) return;
        float ax = (float) (x0 - ox), ay = (float) (y0 - oy), az = (float) (z0 - oz);
        float bx = (float) (x1 - ox), by = (float) (y1 - oy), bz = (float) (z1 - oz);
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-3f) return;
        float[] p = new float[3], q = new float[3];
        basis(dx / len, dy / len, dz / len, p, q);
        // The path first (segments + 1 points), so the light and the ink follow the same one.
        float[] pts = new float[(segments + 1) * 3];
        pts[0] = ax;
        pts[1] = ay;
        pts[2] = az;
        long s = seed * 0x5DEECE66DL + 11;
        float fj0 = 0, fj1 = 0;
        int mid = Math.max(1, segments / 2);
        for (int i = 1; i <= segments; i++) {
            float k = i / (float) segments;
            float env = Mth.sin(Mth.PI * k);
            s = s * 0x5DEECE66DL + 11;
            float j0 = ((s >>> 16 & 0xFFFF) / 65535f - 0.5f) * 2 * jag * env;
            s = s * 0x5DEECE66DL + 11;
            float j1 = ((s >>> 16 & 0xFFFF) / 65535f - 0.5f) * 2 * jag * env;
            pts[i * 3] = ax + dx * k + p[0] * j0 + q[0] * j1;
            pts[i * 3 + 1] = ay + dy * k + p[1] * j0 + q[1] * j1;
            pts[i * 3 + 2] = az + dz * k + p[2] * j0 + q[2] * j1;
            if (i == mid) {
                fj0 = j0;
                fj1 = j1;
            }
        }
        float nx = pts[mid * 3], ny = pts[mid * 3 + 1], nz = pts[mid * 3 + 2], fl = len * 0.3f;
        float fx = nx + (dx / len + p[0] * 0.8f) * fl * 0.5f + p[0] * fj1, fy = ny + (dy / len + p[1] * 0.8f) * fl * 0.5f + q[1] * fj0,
                fz = nz + (dz / len + p[2] * 0.8f) * fl * 0.5f + q[2] * fj0;
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = glow();
        int a = al(alpha), ac = al(alpha * 0.55f);
        for (int i = 1; i <= segments; i++) {
            int u = (i - 1) * 3, v = i * 3;
            ribbon(vc, m, pts[u], pts[u + 1], pts[u + 2], pts[v], pts[v + 1], pts[v + 2], width * 3.2f, width * 3.2f, rgb, ac, ac);
            ribbon(vc, m, pts[u], pts[u + 1], pts[u + 2], pts[v], pts[v + 1], pts[v + 2], width, width, 0xFFFFFF, a, a);
        }
        if (forks) ribbon(vc, m, nx, ny, nz, fx, fy, fz, width * 2f, 0, rgb, ac, 0);
        if (inked()) {
            VertexConsumer ik = ink();
            int ai = al(alpha * day * 0.7f), col = FxColor.mix(rgb, 0xFFFFFF, 0.4f);
            for (int i = 1; i <= segments; i++) {
                int u = (i - 1) * 3, v = i * 3;
                ribbon(ik, m, pts[u], pts[u + 1], pts[u + 2], pts[v], pts[v + 1], pts[v + 2], width * 1.6f, width * 1.6f, col, ai, ai);
            }
        }
    }

    /** A sphere of light. */
    public void sphere(double x, double y, double z, float radius, int rgb, float alpha) {
        // Seen from inside (a shell round the camera's own body) it would wash the whole view: it fades as the eye nears its skin.
        float inside = (float) Math.sqrt(dist2(x, y, z));
        alpha *= Mth.clamp((inside - radius * 0.75f) / (radius * 0.5f), 0.08f, 1);
        if (alpha <= 0.004f || radius <= 0) return;
        pose.pushPose();
        pose.translate(x - ox, y - oy, z - oz);
        WorldDraw.sphere(glow(), pose.last().pose(), radius, 0, 0, argb(alpha, rgb));
        pose.popPose();
    }

    /** A cut crystal (an ice shard, a fragment), tilted so its long axis points along {@code dir}. */
    public void crystal(double x, double y, double z, float dx, float dy, float dz, float rx, float ry, int rgb, float alpha, boolean lit) {
        // Shards and thorns round the camera's own body (a curse on the local hunter) must not wall in the view.
        alpha *= near(x, y, z, Math.max(rx, ry));
        if (alpha <= 0.004f) return;
        pose.pushPose();
        pose.translate(x - ox, y - oy, z - oz);
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1e-4f) {
            Vector3f from = new Vector3f(0, 1, 0), to = new Vector3f(dx / len, dy / len, dz / len);
            pose.mulPose(new org.joml.Quaternionf().rotationTo(from, to));
        }
        WorldDraw.crystal(lit ? glow() : WorldDraw.quads(buffers), pose.last().pose(), rx, ry, argb(alpha, rgb));
        pose.popPose();
    }

    /** A beam ({@link BeamFx}: scrolling core and glow). */
    public void beam(double x0, double y0, double z0, double x1, double y1, double z1, float width, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        BeamFx.draw(pose, buffers, new Vec3(x0 - ox, y0 - oy, z0 - oz), new Vec3(x1 - ox, y1 - oy, z1 - oz), width, rgb,
                Math.min(1f, alpha), now * 0.05f, new Vec3(ex, ey, ez));
    }

    /** A band of light along the ground with a short wall on its outer edge ({@code TubeFx.ring}): a shockwave. */
    public void shockwave(ResourceLocation texture, double x, double y, double z, float radius, float width, float height, int rgb,
                          float alpha) {
        if (alpha <= 0.004f || radius <= 0.05f) return;
        org.papiricoh.supernaturalcraft.client.fx.TubeFx.ring(pose, buffers, texture, new Vec3(x - ox, y - oy, z - oz), radius, width,
                height, rgb, alpha, now * 0.05f);
    }

    // --- dark (normal blending) -------------------------------------------------------------------------------------------------

    /** A soft dark blot facing the camera (smoke, a void's heart); like a glow, it fades out over the floor. */
    public void darkDisc(double x, double y, double z, float radius, int rgb, float alpha) {
        alpha *= near(x, y, z, radius);
        if (alpha <= 0.004f || radius <= 0) return;
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        boolean cut = !Double.isNaN(floorY) && y - radius < floorY + 0.1;
        float fl = cut ? (float) (floorY - oy) : 0, fh = cut ? Math.max(0.3f, radius * 0.4f) : 0;
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        fan(WorldDraw.quads(buffers), m, cx, cy, cz, radius, r, g, b, alpha, DARK_R, DARK_A, cut ? 16 : 12, fl, fh);
    }

    /** A dark ring on the ground (scorch, a burnt circle), soft at both edges. */
    public void darkRing(double x, double y, double z, float r0, float r1, int rgb, float alpha) {
        if (alpha <= 0.004f || r1 <= r0) return;
        VertexConsumer vc = WorldDraw.quads(buffers);
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(alpha);
        int seg = Mth.clamp((int) (r1 * 8), 16, 64);
        float mid = r0 + (r1 - r0) * 0.4f;
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            band(vc, m, cx, cy, cz, c0, 0, s0, c1, 0, s1, r0, mid, r, g, b, r0 <= 0 ? a : 0, a);
            band(vc, m, cx, cy, cz, c0, 0, s0, c1, 0, s1, mid, r1, r, g, b, a, 0);
        }
    }

    /** A jagged polygon in an upright plane (the lips of a rift), filled from its centre: dark, normal blending. */
    public void darkFan(double x, double y, double z, float[] px, float[] py, float sideX, float sideZ, int rgb, float centreAlpha,
                        float rimAlpha) {
        VertexConsumer vc = WorldDraw.quads(buffers);
        Matrix4f m = pose.last().pose();
        float cx = (float) (x - ox), cy = (float) (y - oy), cz = (float) (z - oz);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = al(centreAlpha), ar = al(rimAlpha);
        int n = px.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            c(vc, m, cx, cy, cz, r, g, b, a);
            c(vc, m, cx + sideX * px[i], cy + py[i], cz + sideZ * px[i], r, g, b, ar);
            c(vc, m, cx + sideX * px[j], cy + py[j], cz + sideZ * px[j], r, g, b, ar);
            c(vc, m, cx, cy, cz, r, g, b, a);
        }
    }

    /** Glowing crack strokes on the ground radiating from a point ({@link WorldDraw#cracks}). */
    public void cracks(double x, double y, double z, int count, float reach, float width, long seed, int rgb, float alpha) {
        if (alpha <= 0.004f) return;
        WorldDraw.cracks(glow(), pose.last().pose(), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1),
                new Vector3f((float) (x - ox), (float) (y - oy), (float) (z - oz)), count, reach, width, seed, argb(alpha, rgb));
        if (inked()) WorldDraw.cracks(ink(), pose.last().pose(), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1),
                new Vector3f((float) (x - ox), (float) (y - oy) + 0.002f, (float) (z - oz)), count, reach, width * 0.8f, seed,
                argb(alpha * day * 0.7f, rgb));
    }

    /** A word or letter facing the camera, {@code height} blocks tall, full bright ({@code argb} with its alpha). */
    public void text(String text, double x, double y, double z, float height, int argb) {
        float n = near(x, y, z, height);
        if (n < 0.999f) argb = Math.round((argb >>> 24) * n) << 24 | (argb & 0xFFFFFF);
        if ((argb >>> 24) < 4) return;
        pose.pushPose();
        pose.translate(x - ox, y - oy, z - oz);
        WorldDraw.billboardText(pose, buffers, Component.literal(text), height, argb, false);
        pose.popPose();
    }

    // --- helpers -----------------------------------------------------------------------------------------------------------

    /** Two unit vectors perpendicular to (nx, ny, nz) and to each other. */
    public static void basis(float nx, float ny, float nz, float[] ax, float[] ay) {
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) {
            nx = 0;
            ny = 1;
            nz = 0;
        } else {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        // Pick the world axis least like the normal, cross it in.
        float rx, ry, rz;
        if (Math.abs(ny) < 0.9f) {
            rx = 0;
            ry = 1;
            rz = 0;
        } else {
            rx = 1;
            ry = 0;
            rz = 0;
        }
        float x = ry * nz - rz * ny, y = rz * nx - rx * nz, z = rx * ny - ry * nx;
        float l = Mth.sqrt(x * x + y * y + z * z);
        ax[0] = x / l;
        ax[1] = y / l;
        ax[2] = z / l;
        ay[0] = ny * ax[2] - nz * ax[1];
        ay[1] = nz * ax[0] - nx * ax[2];
        ay[2] = nx * ax[1] - ny * ax[0];
    }

    public static int argb(float alpha, int rgb) {
        return al(alpha) << 24 | (rgb & 0xFFFFFF);
    }
}
