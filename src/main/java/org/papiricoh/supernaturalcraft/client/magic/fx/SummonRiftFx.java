package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/**
 * SUMMON_RIFT: a tear in the air before a summoning (v0.21). A jagged crack of light splits the air and the ground answers with a
 * shockwave; it yawns open into a tall lens of near-black (at least 3.2 blocks) that always turns its face to whoever looks, so it
 * never shows as a flat cut-out. Its inside has depth: dim layers set back behind its lips, motes swirling down into it and a far
 * light breathing at its heart. Its lips burn in three layers and writhe, arcs spit off them, embers pour out of it and light the
 * ground; the world's dust is sucked in at its edges. It stays open while what is summoned comes through, then collapses in a flash.
 */
final class SummonRiftFx extends MagicEffect {

    private static final int VOID = 0x07020A;
    private static final int POINTS = 18;
    private static final int CLOSE = 12;

    private final float height, width;
    private final long seed;
    private final float[] px = new float[POINTS * 2], py = new float[POINTS * 2];
    private final float[] qx = new float[POINTS * 2], qy = new float[POINTS * 2];

    SummonRiftFx(MagicFxPayload p) {
        super(p.pos(), p.school(), Math.max(30, p.duration() > 0 ? p.duration() : 80));
        this.height = Math.max(3.2f, p.arg() > 0 ? p.arg() : 4f);
        this.width = Math.max(1.6f, p.arg2() > 0 ? p.arg2() : 1.6f) * 1.15f;
        this.seed = (long) (p.pos().x * 131 + p.pos().z * 71);
    }

    @Override
    protected double reach() {
        return height + 8;
    }

    @Override
    protected double floor(ClientLevel level) {
        return at.y;
    }

    private float open(float t) {
        float in = easeOutBack((t - 6) / (life * 0.2f));
        float out = Mth.clamp((life - CLOSE - t) / 8f + 1, 0, 1);
        return Mth.clamp(in, 0, 1.15f) * out;
    }

    /** The horizontal unit vector across the rift's face (it turns to the camera about the vertical). */
    private static float[] side(Vec3 c) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.gameRenderer != null ? mc.gameRenderer.getMainCamera().getPosition() : c.add(0, 0, 4);
        double fx = eye.x - c.x, fz = eye.z - c.z, l = Math.sqrt(fx * fx + fz * fz);
        if (l < 1e-4) return new float[]{1, 0, 0, 1};
        return new float[]{(float) (-fz / l), (float) (fx / l), (float) (fx / l), (float) (fz / l)};
    }

    @Override
    protected void tick(ClientLevel level) {
        RandomSource r = level.random;
        Vec3 c = at.add(0, height / 2, 0);
        float[] f = side(c);
        float sx = f[0], sz = f[1], fx = f[2], fz = f[3];
        if (age == 0) MagicSounds.other("ritual.rift", c, 1.4f, 1f);
        if (age == 6) {
            MagicShake.kick(c, 0.7f, 16, 28);
            MagicOverlay.flash(c, FxColor.core(school), 0.25f, 16);
            for (int i = FxParticles.n(36); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                double s = 0.25 + r.nextDouble() * 0.3;
                FxParticles.ember(level, r.nextBoolean() ? primary : accent, 1.2f, 0, at.x, at.y + 0.1, at.z, Mth.cos(a) * s, 0.06,
                        Mth.sin(a) * s);
            }
            for (int i = FxParticles.n(12); i > 0; i--) {
                FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x + FxParticles.j(r) * 0.8, at.y + 0.2, at.z + FxParticles.j(r) * 0.8,
                        FxParticles.j(r) * 0.1, 0.03, FxParticles.j(r) * 0.1);
            }
        }
        if (age == life - CLOSE) MagicSounds.school(school, "impact", c, 0.9f, 0.6f);
        if (age == life - 4) {
            MagicShake.kick(c, 0.5f, 10, 20);
            for (int i = FxParticles.n(24); i > 0; i--) {
                FxParticles.spark(level, FxColor.core(school), 1.1f, 0, c.x, c.y + FxParticles.j(r) * height * 0.3, c.z,
                        FxParticles.j(r) * 0.3, FxParticles.j(r) * 0.2, FxParticles.j(r) * 0.3);
            }
        }
        float open = open(age);
        if (open <= 0.05f) return;
        // Drawn in: dust and portal sparks from round the lips toward the middle.
        for (int i = FxParticles.rate(r, 3.5f * open); i > 0; i--) {
            float v = r.nextFloat(), s = r.nextBoolean() ? 1 : -1;
            float hw = width / 2 * Mth.sin(Mth.PI * v) * open + 1.4f;
            double x = at.x + sx * hw * s, y = at.y + v * height, z = at.z + sz * hw * s;
            FxParticles.plain(level, ParticleTypes.REVERSE_PORTAL, x, y, z, (c.x - x) * 0.04, (c.y - y) * 0.04, (c.z - z) * 0.04);
        }
        // Embers pouring out of it, toward the watcher and down.
        for (int i = FxParticles.rate(r, 3f * open); i > 0; i--) {
            float v = 0.15f + r.nextFloat() * 0.7f;
            float hw = width / 2 * Mth.sin(Mth.PI * v) * open * (float) FxParticles.j(r) * 0.8f;
            double out = 0.04 + r.nextDouble() * 0.08;
            FxParticles.ember(level, r.nextBoolean() ? primary : accent, 0.9f + r.nextFloat() * 0.6f, 0, at.x + sx * hw, at.y + v * height,
                    at.z + sz * hw, fx * out + sx * FxParticles.j(r) * 0.03, -0.02 + r.nextDouble() * 0.05, fz * out + sz * FxParticles.j(r) * 0.03);
        }
        // Sparks off the lips.
        for (int i = FxParticles.rate(r, 1.4f * open); i > 0; i--) {
            float v = r.nextFloat(), s = r.nextBoolean() ? 1 : -1;
            float hw = width / 2 * Mth.sin(Mth.PI * v) * open;
            FxParticles.spark(level, FxColor.core(school), 0.9f, 0, at.x + sx * hw * s, at.y + v * height, at.z + sz * hw * s,
                    sx * s * 0.06, 0.02, sz * s * 0.06);
        }
    }

    @Override
    protected void render(FxDraw d) {
        float t = time(d);
        double cx = at.x, cy = at.y + height / 2, cz = at.z;
        float[] f = side(new Vec3(cx, cy, cz));
        float sx = f[0], sz = f[1], fx = f[2], fz = f[3];
        int core = FxColor.core(school);
        int detail = MagicFxManager.detail();
        renderGround(d, t);
        if (t < 6) {
            // The first crack: a jagged line of light splitting the air from the middle out.
            float k = t / 6f;
            d.lightning(cx, cy - height * 0.5 * k, cz, cx, cy + height * 0.5 * k, cz, seed + (long) (t * 2), 9, 0.22f, 0.05f, primary, 1, false);
            d.glowDisc(cx, cy, cz, height * 0.45f * k, primary, 0.6f);
            d.glowDisc(cx, cy, cz, 0.8f, 0xFFFFFF, 0.8f * k);
            return;
        }
        float open = open(t);
        if (open <= 0.01f) {
            // Sealed: the collapse flash.
            float k = Mth.clamp((t - (life - CLOSE)) / CLOSE, 0, 1);
            float fl = Mth.clamp((life - t) / 5f, 0, 1);
            d.star(cx, cy, cz, height * (0.4f + 0.7f * k), 0.4f, core, fl);
            d.glowDisc(cx, cy, cz, height * 0.5f, primary, 0.6f * fl);
            d.ring(cx, cy, cz, fx, 0, fz, height * 0.5f * (1 - k), height * 0.5f * (1 - k) + 0.3f, core, 0.8f * fl, true);
            return;
        }
        lens(t, open);
        float breathe = 0.6f + 0.4f * Mth.sin(t * 0.12f);
        float flick = 0.85f + 0.15f * Mth.sin(t * 1.3f) * Mth.sin(t * 0.47f + 1);
        // Behind it all: the light it bleeds into the air.
        d.glowDisc(cx, cy, cz, height * 0.8f, primary, 0.3f * Math.min(1, open) * flick);
        // The void, then the depth inside it (each layer set back from the lips).
        d.darkFan(cx, cy, cz, px, py, sx, sz, VOID, 0.97f, 0.9f);
        if (detail > 0) {
            for (int layer = 1; layer <= 2; layer++) {
                float s = 1 - layer * 0.24f, back = layer * 0.18f;
                double lx = cx - fx * back, lz = cz - fz * back;
                int col = FxColor.mix(VOID, primary, 0.18f + 0.1f * layer);
                outline(d, lx, cy, lz, sx, sz, s, 0.35f, col, 0.35f * breathe, t * (layer == 1 ? 0.04f : -0.06f));
            }
        }
        // Motes swirling down into it.
        int motes = detail == 0 ? 6 : detail == 1 ? 12 : 20;
        for (int i = 0; i < motes; i++) {
            float u = (t * 0.018f + i / (float) motes) % 1f;
            float ang = i * 2.399f + u * 5.5f;
            float rr = (1 - u) * 0.85f;
            float hx = Mth.cos(ang) * rr * width * 0.42f * open, hy = Mth.sin(ang) * rr * height * 0.42f;
            double back = 0.05 + u * 0.45;
            d.glowDisc(cx + sx * hx - fx * back, cy + hy, cz + sz * hx - fz * back, 0.09f + 0.12f * (1 - u),
                    i % 3 == 0 ? 0xFFFFFF : i % 3 == 1 ? primary : accent, 0.85f * Mth.sin(Mth.PI * u));
        }
        // The far light at its heart.
        d.glowDisc(cx - fx * 0.5, cy, cz - fz * 0.5, width * 0.5f * open, primary, 0.3f * breathe);
        d.star(cx - fx * 0.5, cy, cz - fz * 0.5, width * 0.35f * open, t * 0.02f, core, 0.4f * breathe);
        // The burning lips: a wide haze, a body, a white-hot edge.
        outline(d, cx, cy, cz, sx, sz, 1, 0.75f, primary, 0.35f * flick, 0);
        outline(d, cx, cy, cz, sx, sz, 1, 0.26f, accent, 0.85f * flick, 0);
        outline(d, cx, cy, cz, sx, sz, 1, 0.08f, 0xFFFFFF, flick, 0);
        // Arcs spitting off the lips.
        int arcs = detail == 0 ? 1 : 2;
        for (int k = 0; k < arcs; k++) {
            long slot = (long) (t / 5) * 7 + k * 13;
            if ((slot + k) % 3 == 2) continue;
            int i = (int) (Math.abs(seed + slot * 31) % (POINTS * 2));
            if (i == POINTS - 1 || i == POINTS * 2 - 1) continue;
            double x0 = cx + sx * px[i], y0 = cy + py[i], z0 = cz + sz * px[i];
            float out = Math.signum(px[i] == 0 ? 1 : px[i]) * (1.2f + 0.6f * k);
            d.lightning(x0, y0, z0, x0 + sx * out, y0 + jag(seed + slot) * 1.3f, z0 + sz * out, slot + (long) (t / 2), 6, 0.3f, 0.045f,
                    primary, 0.95f, k == 0);
        }
    }

    /** The light on the ground beneath it: a pool, pulsing rings, scorched cracks, and the shockwave when it opens. */
    private void renderGround(FxDraw d, float t) {
        double gy = at.y + 0.03;
        float open = Math.min(1, open(t));
        float pulse = 0.75f + 0.25f * Mth.sin(t * 0.3f);
        if (t >= 6) {
            float s = t - 6, k = easeOut(s / 14f);
            if (k < 1) {
                d.groundRing(at.x, gy, at.z, Math.max(0, height * 1.6f * k - 0.8f), height * 1.6f * k, FxColor.core(school), 0.85f * (1 - k), true);
                d.shockwave(MagicTextures.BEAM, at.x, gy, at.z, height * 1.6f * k, 0.6f, 1.1f * (1 - k), primary, 0.7f * (1 - k));
            }
        }
        if (open <= 0.01f) return;
        d.flatGlow(at.x, gy, at.z, width * 2.4f, primary, 0.5f * open * pulse);
        d.cracks(at.x, gy + 0.005, at.z, 7, width * 1.8f, 0.07f, seed, primary, 0.8f * open);
        float u = (t * 0.03f) % 1f;
        d.groundRing(at.x, gy, at.z, width * (0.6f + 1.4f * u), width * (0.75f + 1.4f * u), primary, 0.4f * open * (1 - u), true);
    }

    /** The lens's rim, writhing: right lip bottom to top, then the left lip top to bottom (the void's polygon in px, py). */
    private void lens(float t, float open) {
        long frame = (long) (t / 2);
        float sub = (t / 2) - frame;
        for (int i = 0; i < POINTS; i++) {
            float v = i / (float) (POINTS - 1);
            float env = Mth.sin(Mth.PI * v);
            float hw = width / 2 * (float) Math.pow(env, 0.75) * open;
            float a0 = Mth.lerp(sub, jag(seed + i * 31 + frame), jag(seed + i * 31 + frame + 1));
            float a1 = Mth.lerp(sub, jag(seed + i * 57 + frame * 3), jag(seed + i * 57 + frame * 3 + 3));
            float wave = 0.05f * Mth.sin(t * 0.5f + v * 9f);
            float j0 = (a0 * 0.17f + wave) * width * env, j1 = (a1 * 0.17f - wave) * width * env;
            float y = (v - 0.5f) * height * (0.92f + 0.08f * open);
            px[i] = hw + j0;
            py[i] = y;
            px[POINTS * 2 - 1 - i] = -hw - j1;
            py[POINTS * 2 - 1 - i] = y;
        }
    }

    /**
     * The rim drawn as a ribbon of light: scaled by {@code s} toward the centre (inner layers), {@code width} wide, its jag shifted
     * by {@code turn} radians round the rim (so inner layers drift).
     */
    private void outline(FxDraw d, double cx, double cy, double cz, float sx, float sz, float s, float w, int rgb, float alpha,
                         float turn) {
        int n = POINTS * 2;
        for (int i = 0; i < n; i++) {
            qx[i] = px[i] * s * (1 + 0.04f * Mth.sin(turn * 7 + i));
            qy[i] = py[i] * s;
        }
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            // The two tips close the lens; skip the seam between the lips there only if they coincide.
            double x0 = cx + sx * qx[i], y0 = cy + qy[i], z0 = cz + sz * qx[i];
            double x1 = cx + sx * qx[j], y1 = cy + qy[j], z1 = cz + sz * qx[j];
            float taperK = Math.min(Mth.sin(Mth.PI * (i % POINTS) / (POINTS - 1f)), 1) * 0.6f + 0.4f;
            d.ribbon(x0, y0, z0, x1, y1, z1, w * taperK, rgb, alpha, alpha);
        }
    }

    private static float jag(long s) {
        s = s * 0x5DEECE66DL + 11;
        return ((s >>> 17) & 0xFFFF) / 65535f * 2 - 1;
    }
}
