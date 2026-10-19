package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/**
 * TELEPORT (v0.21): a smoke blink. Where they left, a column of smoke bursts up their height with a flash, a ring along the
 * ground and motes thrown out; for a moment a figure of smoke stands where they were, its eyes still lit, and dissolves upward.
 * Where they appear, smoke gathers first — motes and three arms of light spiralling in and closing — then puffs out round their
 * feet as they step out of it. The gathering lasts {@code duration - 32} ticks (at least 8), so a long duration telegraphs
 * where a boss will appear. {@link MagicFxPayload#SMOKE_WITCH}: a witch's magenta-black; {@link MagicFxPayload#SMOKE_YELLOW}:
 * a boss's yellow (the telegraph colour). When both ends are the same point (a familiar called), only the arrival plays.
 */
final class TeleportSmokeFx extends MagicEffect {

    private final Vec3 to;
    private final boolean yellow, arrivalOnly;
    private final float height;
    private final int gather;
    private final int smoke, glow, core;
    private double floorFrom = Double.NaN, floorTo = Double.NaN;

    TeleportSmokeFx(MagicFxPayload p) {
        super(p.pos(), p.school().isNone() ? School.WITCHCRAFT : p.school(), Math.max(p.duration(), 40));
        this.to = p.target();
        this.yellow = p.argIndex() == MagicFxPayload.SMOKE_YELLOW;
        this.arrivalOnly = p.pos().distanceToSqr(p.target()) < 0.25;
        this.height = Mth.clamp(p.arg2() <= 0 ? 1.9f : p.arg2(), 0.5f, 6f);
        this.gather = gatherTicks(life);
        if (yellow) {
            smoke = 0x6E5A14;
            glow = 0xF2CF46;
            core = 0xFFF2B0;
        } else {
            smoke = 0x1A0718;
            glow = primary;
            core = FxColor.mix(primary, 0xFFFFFF, 0.55f);
        }
    }

    /** Ticks the smoke gathers where they appear before it puffs out (a TELEPORT of {@code duration} ticks). */
    static int gatherTicks(int duration) {
        return Mth.clamp(Math.max(duration, 40) - 32, 8, 80);
    }

    /** How long the one who blinks stays hidden: until the arrival puff (at most half a second). */
    static int hiddenTicks(int duration) {
        return Math.min(10, gatherTicks(duration));
    }

    @Override
    protected Vec3 anchor() {
        return at.add(to).scale(0.5);
    }

    @Override
    protected double reach() {
        return at.distanceTo(to) * 0.5 + height + 3;
    }

    @Override
    protected void tick(ClientLevel level) {
        if (Double.isNaN(floorFrom)) {
            floorFrom = FxWorld.floorOrNaN(level, at.add(0, 0.5, 0), 4);
            floorTo = FxWorld.floorOrNaN(level, to.add(0, 0.5, 0), 4);
        }
        RandomSource r = level.random;
        int moteRgb = yellow ? (r.nextBoolean() ? glow : 0xB89428) : (r.nextBoolean() ? glow : 0x5A1450);
        if (!arrivalOnly) {
            if (age == 0) {
                for (int i = FxParticles.n(26); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, s = 0.08f + r.nextFloat() * 0.12f;
                    FxParticles.mote(level, r.nextBoolean() ? glow : core, 1.1f, 26 + r.nextInt(14), at.x + Mth.cos(a) * 0.3,
                            at.y + r.nextDouble() * height, at.z + Mth.sin(a) * 0.3, Mth.cos(a) * s, 0.02 + r.nextDouble() * 0.05, Mth.sin(a) * s);
                }
                for (int i = FxParticles.n(12); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x + FxParticles.j(r) * 0.4, at.y + r.nextDouble() * height,
                            at.z + FxParticles.j(r) * 0.4, FxParticles.j(r) * 0.04, 0.06 + r.nextDouble() * 0.05, FxParticles.j(r) * 0.04);
                }
            } else if (age < 20) {
                for (int i = FxParticles.rate(r, 1.2f); i > 0; i--) {
                    FxParticles.mote(level, moteRgb, 1.4f, 30, at.x + FxParticles.j(r) * 0.5, at.y + r.nextDouble() * height,
                            at.z + FxParticles.j(r) * 0.5, FxParticles.j(r) * 0.01, 0.03, FxParticles.j(r) * 0.01);
                }
            }
        }
        // The arrival: motes drawn in on a spiral while it gathers, then a puff.
        if (age < gather) {
            for (int i = FxParticles.rate(r, 2.0f); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI, rad = 1.3f + r.nextFloat() * 0.5f;
                double y = to.y + r.nextDouble() * height;
                double px = to.x + Mth.cos(a) * rad, pz = to.z + Mth.sin(a) * rad;
                // Inward and round (counter-clockwise), a little up.
                double in = 0.075, tang = 0.06;
                FxParticles.mote(level, moteRgb, 1.0f, 16, px, y, pz, -Mth.cos(a) * in - Mth.sin(a) * tang, 0.01, -Mth.sin(a) * in + Mth.cos(a) * tang);
            }
        } else if (age == gather) {
            for (int i = FxParticles.n(18); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI, s = 0.06f + r.nextFloat() * 0.08f;
                FxParticles.mote(level, r.nextBoolean() ? glow : core, 1.0f, 24, to.x + Mth.cos(a) * 0.4, to.y + 0.1 + r.nextDouble() * 0.4,
                        to.z + Mth.sin(a) * 0.4, Mth.cos(a) * s, 0.01, Mth.sin(a) * s);
            }
            for (int i = FxParticles.n(8); i > 0; i--) {
                FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, to.x + FxParticles.j(r) * 0.6, to.y + 0.2 + r.nextDouble() * height * 0.6,
                        to.z + FxParticles.j(r) * 0.6, FxParticles.j(r) * 0.06, 0.03, FxParticles.j(r) * 0.06);
            }
        }
    }

    @Override
    protected void render(FxDraw d) {
        float t = time(d);
        if (!arrivalOnly) {
            d.floor(floorFrom);
            origin(d, t);
        }
        d.floor(floorTo);
        arrival(d, t);
    }

    /** The burst where they left and the figure of smoke they leave behind. */
    private void origin(FxDraw d, float t) {
        float a = fade(t, 0, life * 0.55f);
        double fy = Double.isNaN(floorFrom) ? at.y : Math.max(at.y, floorFrom);
        // Flash and the ring along the ground.
        if (t < 6) {
            float k = 1 - t / 6f;
            d.glowDisc(at.x, fy + height * 0.5, at.z, height * 0.9f, core, 0.7f * k);
            d.star(at.x, fy + height * 0.55, at.z, height * 0.8f, 0.3f, core, 0.6f * k);
        }
        float out = easeOut(t / 12f);
        d.groundRing(at.x, fy + 0.04, at.z, 0.2f + 1.3f * out, 0.5f + 1.5f * out, glow, 0.7f * (1 - out) * a, true);
        d.darkRing(at.x, fy + 0.03, at.z, 0, 0.9f + 0.6f * out, smoke, 0.5f * a);
        // The column: puffs stacked up their height, swelling and climbing, curling as they go.
        for (int i = 0; i < 7; i++) {
            float u = i / 6f;
            float rise = t * (0.02f + 0.012f * i);
            float rad = (0.35f + 0.35f * u) * (0.8f + 0.6f * easeOut(t / 16f));
            double x = at.x + Mth.sin(t * 0.11f + i * 1.7f) * 0.18 * (1 + u), z = at.z + Mth.cos(t * 0.09f + i * 2.3f) * 0.18 * (1 + u);
            float pa = a * (0.75f - 0.25f * u) * Mth.clamp(t / 2f, 0, 1);
            d.darkDisc(x, fy + 0.2 + u * height + rise, z, rad, smoke, pa);
            if (i % 2 == 0) d.glowDisc(x, fy + 0.2 + u * height + rise, z, rad * 0.7f, glow, 0.16f * pa * (1 - u * 0.5f));
        }
        // The figure: legs, body, head of smoke, eyes still lit, dissolving upward.
        if (t < 14) {
            float k = 1 - t / 14f, s = height / 1.9f, drift = t * 0.025f;
            double fx = at.x, fz = at.z;
            d.darkDisc(fx, fy + 0.45 * s + drift * 0.5, fz, 0.3f * s * (1 + t * 0.03f), smoke, 0.85f * k);
            d.darkDisc(fx, fy + 1.05 * s + drift, fz, 0.36f * s * (1 + t * 0.04f), smoke, 0.85f * k);
            d.darkDisc(fx, fy + 1.62 * s + drift * 1.4, fz, 0.22f * s * (1 + t * 0.05f), smoke, 0.9f * k);
            if (t < 8) {
                float e = (1 - t / 8f) * (0.7f + 0.3f * Mth.sin(t * 3));
                // Eyes: two glints a hand apart, facing the camera's side of the head.
                Vec3 eye = d.eye();
                double dx = eye.x - fx, dz = eye.z - fz, l = Math.max(1e-3, Math.sqrt(dx * dx + dz * dz));
                double sx = -dz / l * 0.07 * s, sz = dx / l * 0.07 * s, ox = dx / l * 0.2 * s, oz = dz / l * 0.2 * s;
                double ey = fy + 1.68 * s + drift * 1.4;
                d.star(fx + ox + sx, ey, fz + oz + sz, 0.16f * s, 0, core, e);
                d.star(fx + ox - sx, ey, fz + oz - sz, 0.16f * s, 0, core, e);
            }
        }
    }

    /** The smoke gathering where they appear, then the puff round their feet. */
    private void arrival(FxDraw d, float t) {
        double fy = Double.isNaN(floorTo) ? to.y : Math.max(to.y, floorTo);
        if (t < gather + 2) {
            float g = Mth.clamp(t / gather, 0, 1);
            float a = Mth.clamp(t / 3f, 0, 1) * (t > gather ? 1 - (t - gather) / 2f : 1);
            // Three arms of light spiralling in and closing on the axis.
            for (int arm = 0; arm < 3; arm++) {
                double px = 0, py = 0, pz = 0;
                for (int j = 0; j <= 8; j++) {
                    float u = j / 8f;
                    float ang = t * 0.32f + arm * Mth.TWO_PI / 3 + u * 2.6f;
                    float rad = (1.5f - u * 0.6f) * (1 - 0.85f * g);
                    double x = to.x + Mth.cos(ang) * rad, y = fy + 0.15 + u * height, z = to.z + Mth.sin(ang) * rad;
                    if (j > 0) d.ribbon(px, py, pz, x, y, z, 0.07f, glow, 0.55f * a * (1 - u * 0.4f), 0.55f * a * (1 - (u + 0.125f) * 0.4f));
                    px = x;
                    py = y;
                    pz = z;
                }
            }
            d.groundRing(to.x, fy + 0.04, to.z, 1.4f * (1 - g) + 0.2f, 1.6f * (1 - g) + 0.45f, glow, 0.5f * a, true);
            // Smoke thickening on the spot.
            for (int i = 0; i < 4; i++) {
                float u = i / 3f;
                d.darkDisc(to.x, fy + 0.3 + u * height * 0.85, to.z, (0.25f + 0.25f * g) * (1 + 0.4f * u), smoke, 0.6f * g * a);
            }
            d.glowDisc(to.x, fy + height * 0.5, to.z, 0.6f + 0.6f * g, glow, 0.25f * g * a);
        }
        float p = t - gather;
        if (p >= 0) {
            float k = Mth.clamp(p / Math.max(10f, life - gather - 4), 0, 1), a = 1 - k;
            if (p < 4) d.glowDisc(to.x, fy + height * 0.5, to.z, height * 0.75f, core, 0.55f * (1 - p / 4f));
            float out = easeOut(p / 10f);
            d.groundRing(to.x, fy + 0.04, to.z, 0.3f + 1.2f * out, 0.6f + 1.4f * out, glow, 0.6f * (1 - out), true);
            d.darkRing(to.x, fy + 0.03, to.z, 0, 0.8f + 0.9f * out, smoke, 0.45f * a);
            // The cloud she steps out of, thinning and rolling outward and up.
            for (int i = 0; i < 6; i++) {
                float ang = i * Mth.TWO_PI / 6 + p * 0.03f;
                float rad = 0.25f + 0.8f * out;
                double y = fy + 0.3 + (i % 3) * height * 0.3 + p * 0.02;
                d.darkDisc(to.x + Mth.cos(ang) * rad, y, to.z + Mth.sin(ang) * rad, 0.45f + 0.3f * out, smoke, 0.6f * a * a);
            }
        }
    }
}
