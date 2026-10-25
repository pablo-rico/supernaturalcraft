package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/**
 * CAST_CIRCLE: a ring of glyphs that snaps open at the caster's hand (facing where they look) or under their feet, turning, in
 * the school's colours; tier 2 adds a counter-turning inner ring, tier 3 an outer ring and a band of glyphs rising off it. It
 * follows the caster. Never a filled disc: only rims and glyphs, so boss telegraphs under it stay readable.
 */
final class CastCircleFx extends MagicEffect {

    /** The circle at the hand, and its glyphs and sparks, at a fifth of their old size: full size it filled the screen. */
    static final float HAND_SCALE = 0.2f;

    private final int caster;
    private final int tier;
    private final boolean feet;
    private final Vec3 look;
    private final float radius;
    // This frame's centre and normal.
    private double cx, cy, cz;
    private float nx, ny, nz;

    CastCircleFx(MagicFxPayload p) {
        super(p.pos(), p.school(), Math.max(10, p.duration()) + 8);
        this.caster = p.entity();
        this.tier = Mth.clamp(p.argIndex(), 1, 4);
        this.feet = p.arg2Index() == 1;
        this.look = p.target();
        this.radius = feet ? 0.85f + 0.45f * tier : (0.34f + 0.13f * tier) * HAND_SCALE;
    }

    private void frame(float partial) {
        Entity e = entity(caster);
        if (e != null && !e.isRemoved()) {
            Vec3 pos = e.getPosition(partial);
            if (feet) {
                cx = pos.x;
                cy = pos.y + 0.04;
                cz = pos.z;
                nx = 0;
                ny = 1;
                nz = 0;
                return;
            }
            Vec3 eye = e.getEyePosition(partial), view = e.getViewVector(partial);
            float side = e instanceof LivingEntity l && l.getMainArm() == HumanoidArm.LEFT ? -1 : 1;
            double rx = -view.z, rz = view.x, rl = Math.sqrt(rx * rx + rz * rz);
            if (rl > 1e-4) {
                rx /= rl;
                rz /= rl;
            }
            cx = eye.x + view.x * 0.85 + rx * 0.3 * side;
            cy = eye.y + view.y * 0.85 - 0.3;
            cz = eye.z + view.z * 0.85 + rz * 0.3 * side;
            nx = (float) view.x;
            ny = (float) view.y;
            nz = (float) view.z;
            return;
        }
        cx = at.x;
        cy = at.y;
        cz = at.z;
        if (feet) {
            nx = 0;
            ny = 1;
            nz = 0;
        } else {
            Vec3 n = look.subtract(at);
            if (n.lengthSqr() < 1e-6) n = new Vec3(0, 0, 1);
            nx = (float) n.x;
            ny = (float) n.y;
            nz = (float) n.z;
        }
    }

    @Override
    protected Vec3 anchor() {
        Entity e = entity(caster);
        return e != null ? e.position() : at;
    }

    @Override
    protected void tick(ClientLevel level) {
        frame(1f);
        RandomSource r = level.random;
        if (age == 0) {
            MagicSounds.school(school, "cast", new Vec3(cx, cy, cz), 0.7f + 0.15f * tier, 0.9f + 0.1f * tier + r.nextFloat() * 0.1f);
            // A puff of sparks as it snaps open.
            int n = FxParticles.n(6 + tier * 4);
            for (int i = 0; i < n; i++) spark(level, r, 0.12, primary);
        }
        if (age > life - 8) return;
        int n = FxParticles.rate(r, 0.6f + tier * 0.5f);
        float[] ax = new float[3], ay = new float[3];
        FxDraw.basis(nx, ny, nz, ax, ay);
        for (int i = 0; i < n; i++) {
            float a = r.nextFloat() * Mth.TWO_PI, k = radius * (0.95f + r.nextFloat() * 0.15f);
            double ox = (ax[0] * Mth.cos(a) + ay[0] * Mth.sin(a)), oy = (ax[1] * Mth.cos(a) + ay[1] * Mth.sin(a)),
                    oz = (ax[2] * Mth.cos(a) + ay[2] * Mth.sin(a));
            double up = feet ? 0.03 + r.nextDouble() * 0.03 : 0.01;
            float drift = feet ? 1 : HAND_SCALE;
            FxParticles.glyph(level, r.nextInt(3) == 0 ? accent : primary, feet ? 1.1f : 0.7f * HAND_SCALE, 0, cx + ox * k, cy + oy * k,
                    cz + oz * k, (ox * 0.012 + nx * 0.02) * drift, up + oy * 0.012 * drift, (oz * 0.012 + nz * 0.02) * drift);
        }
        if (!feet && r.nextInt(2) == 0 && FxParticles.n(1) > 0) spark(level, r, 0.04, FxColor.core(school));
    }

    private void spark(ClientLevel level, RandomSource r, double speed, int rgb) {
        if (!feet) speed *= HAND_SCALE;
        FxParticles.spark(level, rgb, feet ? 0.8f : 0.8f * HAND_SCALE, 0, cx, cy, cz, FxParticles.j(r) * speed, FxParticles.j(r) * speed + (feet ? speed : 0),
                FxParticles.j(r) * speed);
    }

    @Override
    protected void render(FxDraw d) {
        frame(d.partial);
        float t = time(d);
        float a = fade(t, 2, 8);
        if (a <= 0) return;
        float open = easeOutBack(t / 6f) * (1 + 0.15f * Mth.clamp((t - (life - 8)) / 8f, 0, 1));
        float r = radius * open;
        float spin = t * (0.05f + 0.02f * tier);
        float pulse = 0.85f + 0.15f * Mth.sin(t * 0.4f);
        var circle = MagicTextures.castCircle();
        // The light it throws round the caster's feet, and a ring flung out as it snaps open.
        if (feet) d.flatGlow(cx, cy - 0.01, cz, r * 1.5f, primary, 0.32f * a * pulse);
        if (t < 7) {
            float k = t / 7f;
            d.ring(cx, cy, cz, nx, ny, nz, r * (1 + 0.7f * k), r * (1.08f + 0.8f * k), FxColor.core(school), 0.7f * (1 - k), true);
        }
        d.disc(circle, cx, cy, cz, nx, ny, nz, r, spin, primary, 0.95f * a * pulse);
        if (tier >= 2) {
            d.disc(circle, cx + nx * 0.01, cy + ny * 0.01, cz + nz * 0.01, nx, ny, nz, r * 0.6f, -spin * 1.7f, accent, 0.75f * a);
        }
        d.ring(cx, cy, cz, nx, ny, nz, r * 0.96f, r * 1.1f, primary, 0.55f * a * pulse, true);
        // A ring of glyphs counter-turning just outside it.
        if (tier >= 2 || feet) {
            d.glyphRing(MagicTextures.glyphStrip(), cx + nx * 0.005, cy + ny * 0.005, cz + nz * 0.005, nx, ny, nz, r * 1.04f, r * 1.2f,
                    -spin * 0.7f, accent, 0.8f * a);
        }
        if (tier >= 3) {
            d.ring(cx, cy, cz, nx, ny, nz, r * 1.22f, r * 1.3f, accent, 0.7f * a, true);
            if (feet && MagicFxManager.detail() > 0) {
                d.band(MagicTextures.glyphStrip(), cx, cy, cz, r * 1.08f, 0.45f + 0.1f * Mth.sin(t * 0.2f), spin * 0.6f, primary,
                        0.75f * a, 0);
            }
        }
        if (tier >= 4) {
            // A Master's working: a third ring of glyphs, wide and slow, and a counter-turning band at the hand too.
            d.disc(circle, cx - nx * 0.01, cy - ny * 0.01, cz - nz * 0.01, nx, ny, nz, r * 1.6f, -spin * 0.4f, accent, 0.5f * a);
            d.ring(cx, cy, cz, nx, ny, nz, r * 1.62f, r * 1.7f, primary, 0.6f * a * pulse, true);
        }
        if (!feet) d.glowDisc(cx, cy, cz, r * 0.75f, FxColor.core(school), 0.45f * a * pulse);
        else d.glowDisc(cx, cy + 0.15, cz, r * 0.5f, primary, 0.12f * a);
    }
}
