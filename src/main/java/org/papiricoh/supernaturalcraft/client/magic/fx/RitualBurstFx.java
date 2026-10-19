package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * The end of a rite over its altar (v0.21).
 * <ul>
 *   <li>RITUAL_COMPLETE: a pillar of the school's light shoots up, wrapped in rings of glyphs climbing it; a shockwave runs out along
 *   the ground over a great turning circle; a flash, a burst of glyphs and the camera shaking (less the further one stands).</li>
 *   <li>RITUAL_FAIL: the circle implodes: motes and a ring of light are sucked into a swelling dark heart over the altar, which
 *   pops in a gout of black smoke, leaving cracks burnt round it (red sparks too if something came through).</li>
 * </ul>
 */
final class RitualBurstFx extends MagicEffect {

    private final boolean complete;
    private final float a1, a2;

    RitualBurstFx(MagicFxPayload p, boolean complete) {
        super(p.pos(), p.school(), Math.max(p.duration(), complete ? 56 : 44));
        this.complete = complete;
        this.a1 = complete ? (p.arg() > 0 ? p.arg() : 24) : (p.arg() > 0 ? p.arg() : 4);
        this.a2 = complete ? (p.arg2() > 0 ? p.arg2() : 8) : p.arg2();
    }

    @Override
    protected double reach() {
        return complete ? Math.max(a1, a2) + 8 : a1 + 6;
    }

    private int pop() {
        return (int) (life * 0.55f);
    }

    // --- particles ------------------------------------------------------------------------------------------------------------

    @Override
    protected void tick(ClientLevel level) {
        if (complete) tickComplete(level);
        else tickFail(level);
    }

    private void tickComplete(ClientLevel level) {
        RandomSource r = level.random;
        if (age == 0) {
            MagicSounds.school(school, "complete", at, 1.4f, 1f);
            MagicShake.kick(at, 1.1f, 22, 40);
            MagicOverlay.flash(at, FxColor.core(school), 0.3f, 14);
            for (int i = FxParticles.n(44); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                double s = 0.15 + r.nextDouble() * 0.3;
                FxParticles.glyph(level, r.nextInt(3) == 0 ? accent : primary, 1.3f, 40, at.x, at.y + 0.3, at.z, Mth.cos(a) * s,
                        0.1 + r.nextDouble() * 0.35, Mth.sin(a) * s);
            }
            for (int i = FxParticles.n(24); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.spark(level, FxColor.core(school), 1.2f, 0, at.x + Mth.cos(a) * a2, at.y + 0.1, at.z + Mth.sin(a) * a2, 0,
                        0.1, 0);
            }
        }
        if (age < life * 0.6f) {
            for (int i = FxParticles.rate(r, 3f); i > 0; i--) {
                double h = r.nextDouble() * a1 * 0.6;
                FxParticles.spark(level, r.nextBoolean() ? primary : FxColor.core(school), 1f, 0, at.x + FxParticles.j(r) * 0.6, at.y + h,
                        at.z + FxParticles.j(r) * 0.6, FxParticles.j(r) * 0.05, 0.3 + r.nextDouble() * 0.3, FxParticles.j(r) * 0.05);
            }
            if (FxParticles.rate(r, 1f) > 0) schoolMatter(level, r);
        }
    }

    private void schoolMatter(ClientLevel level, RandomSource r) {
        double x = at.x + FxParticles.j(r) * 1.5, y = at.y + r.nextDouble() * 2, z = at.z + FxParticles.j(r) * 1.5;
        switch (school) {
            case INFERNAL -> FxParticles.ember(level, accent, 1.2f, 0, x, y, z, FxParticles.j(r) * 0.05, 0.15, FxParticles.j(r) * 0.05);
            case WITCHCRAFT -> FxParticles.mote(level, accent, 1.2f, 0, x, y, z, FxParticles.j(r) * 0.05, 0.08, FxParticles.j(r) * 0.05);
            case HUNTER -> FxParticles.salt(level, 0xF6F6F2, 1f, 0, x, y + 2, z, FxParticles.j(r) * 0.1, 0.15, FxParticles.j(r) * 0.1);
            default -> FxParticles.plain(level, ParticleTypes.END_ROD, x, y, z, FxParticles.j(r) * 0.03, 0.12, FxParticles.j(r) * 0.03);
        }
    }

    private void tickFail(ClientLevel level) {
        RandomSource r = level.random;
        int pop = pop();
        if (age == 0) MagicSounds.other("ritual.implode", at, 1.2f, 1f);
        if (age < pop) {
            // Sucked in: motes from the circle's edge with just the speed to reach the heart as it pops.
            for (int i = FxParticles.rate(r, 3f); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI;
                double d = a1 * (0.7 + r.nextDouble() * 0.4);
                double x = at.x + Mth.cos(a) * d, z = at.z + Mth.sin(a) * d, y = at.y - 0.4 + r.nextDouble();
                int left = Math.max(4, pop - age);
                double k = 1.0 / left;
                FxParticles.spark(level, r.nextInt(3) == 0 ? 0x6A0A10 : primary, 0.9f, left, x, y, z, (at.x + 0.0 - x) * k * 1.6,
                        (at.y + 1.0 - y) * k * 1.6, (at.z - z) * k * 1.6);
            }
        }
        if (age == pop) {
            MagicShake.kick(at, 0.95f, 14, 22);
            MagicOverlay.flash(at, 0xFF5030, 0.28f, 14);
            Vec3 c = at.add(0, 1, 0);
            for (int i = FxParticles.n(40); i > 0; i--) {
                float ang = r.nextFloat() * Mth.TWO_PI;
                double sp = 0.3 + r.nextDouble() * 0.35;
                FxParticles.ember(level, r.nextBoolean() ? 0xFF4A2A : primary, 1.2f, 0, c.x, c.y, c.z, Mth.cos(ang) * sp,
                        FxParticles.j(r) * 0.2 + 0.08, Mth.sin(ang) * sp);
            }
            for (int i = FxParticles.n(26); i > 0; i--) {
                FxParticles.plain(level, r.nextBoolean() ? ParticleTypes.LARGE_SMOKE : AllParticles.DEMON_SMOKE.get(), c.x, c.y, c.z,
                        FxParticles.j(r) * 0.25, FxParticles.j(r) * 0.15 + 0.05, FxParticles.j(r) * 0.25);
            }
            for (int i = FxParticles.n(a2 > 0 ? 30 : 12); i > 0; i--) {
                FxParticles.spark(level, a2 > 0 ? 0xFF2A1A : primary, 1f, 0, c.x, c.y, c.z, FxParticles.j(r) * 0.4, FxParticles.j(r) * 0.4,
                        FxParticles.j(r) * 0.4);
            }
        }
    }

    // --- geometry -----------------------------------------------------------------------------------------------------------

    @Override
    protected double floor(ClientLevel level) {
        // The altar stands on the ground: the floor is its foot, not its top.
        return at.y - 0.97;
    }

    @Override
    protected void render(FxDraw d) {
        if (complete) renderComplete(d);
        else renderFail(d);
    }

    /** A column of light from {@code y0} up {@code h}: full for most of its height, fading softly to nothing at the top. */
    private void column(FxDraw d, double y0, float h, float w, int rgb, float a, boolean core) {
        if (h <= 0.01f) return;
        double knee = y0 + h * 0.62;
        d.softBeam(at.x, y0, at.z, at.x, knee, at.z, w, rgb, a, a * 0.8f, core);
        d.softBeam(at.x, knee, at.z, at.x, y0 + h, at.z, w, rgb, a * 0.8f, 0, core);
    }

    private void renderComplete(FxDraw d) {
        float t = time(d);
        float a = fade(t, 0, life * 0.45f);
        float rise = easeOut(t / 7f);
        float h = a1 * rise;
        float w = Mth.lerp(Mth.clamp(t / life, 0, 1), 1.5f, 0.45f) * (t < 2 ? t / 2 : 1);
        int core = FxColor.core(school);
        double base = at.y - 0.95;
        float flick = 0.92f + 0.08f * Mth.sin(t * 1.7f);
        // The pillar: a wide aura, the body, a white-hot heart; every layer fades out softly at the top.
        column(d, base, h * 1.05f, w * 4.2f, primary, 0.22f * a, false);
        column(d, base, h, w * 2.0f, primary, 0.75f * a * flick, false);
        column(d, base, h * 0.97f, w * 0.9f, core, 0.95f * a, true);
        d.cylinder(at.x, base, at.z, w * 0.55f, h * 0.8f, core, 0.55f * a, 0);
        d.cylinder(at.x, base, at.z, w * 1.3f, h * 0.55f, primary, 0.26f * a, 0);
        // Surges of light running up it.
        for (int i = 0; i < 4; i++) {
            float u = (t * 0.045f + i / 4f) % 1f;
            float sa = a * Mth.sin(Mth.PI * u);
            d.glowDisc(at.x, base + u * h, at.z, w * 1.6f, core, 0.4f * sa);
        }
        // Rings of glyphs climbing it.
        if (MagicFxManager.detail() > 0) {
            var strip = MagicTextures.glyphStrip();
            for (int i = 0; i < 3; i++) {
                float u = ((t * 0.02f) + i / 3f) % 1f;
                float yy = u * Math.min(h, a1 * 0.6f);
                d.band(strip, at.x, base + yy, at.z, w * 1.15f + 0.4f * (1 - u), 0.55f, t * 0.06f * (i % 2 == 0 ? 1 : -1), primary,
                        0.9f * a * (1 - u), 0.2f * a * (1 - u));
            }
        }
        // The base: a great circle turning, a flash, a shockwave racing out.
        float circle = fade(t, 3, life * 0.5f);
        float gr = Math.max(2f, a2 * 0.55f) * easeOutBack(t / 8f);
        d.decal(MagicTextures.castCircle(), at.x, at.y - 0.94, at.z, gr, t * 0.03f, primary, 0.85f * circle);
        d.glyphRing(MagicTextures.glyphStrip(), at.x, at.y - 0.935, at.z, 0, 1, 0, gr * 1.02f, gr * 1.25f, -t * 0.02f, accent,
                0.8f * circle);
        d.flatGlow(at.x, at.y - 0.93, at.z, gr * 1.6f, primary, 0.45f * circle);
        d.glowDisc(at.x, at.y + 0.4, at.z, 4.5f, core, a * (t < 8 ? 1 : 0.5f));
        if (t < 7) d.star(at.x, at.y + 0.6, at.z, 9f, 0.25f, 0xFFFFFF, 1 - t / 7f);
        float k = easeOut(t / 18f);
        float rad = a2 * k;
        if (k < 1) {
            d.shockwave(MagicTextures.BEAM, at.x, at.y - 0.94, at.z, rad, 0.9f, 1.4f * (1 - k), primary, 0.9f * (1 - k));
            d.groundRing(at.x, at.y - 0.93, at.z, Math.max(0, rad - 1.2f), rad + 0.2f, core, 0.6f * (1 - k), true);
        }
    }

    private void renderFail(FxDraw d) {
        float t = time(d);
        int pop = pop();
        Vec3 c = at.add(0, 1, 0);
        Vec3 eye = d.eye();
        float vx = (float) (c.x - eye.x), vy = (float) (c.y - eye.y), vz = (float) (c.z - eye.z);
        int rim = FxColor.mix(primary, 0x8A0A14, 0.5f);
        if (t < pop) {
            float k = t / pop;
            float flicker = 0.7f + 0.3f * Mth.sin(t * 1.9f) * Mth.sin(t * 0.7f);
            float rad = a1 * (1 - easeOut(k));
            // The circle's light dragged inward along the ground.
            d.groundRing(at.x, at.y - 0.93, at.z, Math.max(0, rad - 0.5f), rad, FxColor.mix(primary, 0x6A0A10, k), 0.75f * flicker, true);
            d.cylinder(at.x, at.y - 0.94, at.z, rad, 0.8f * (1 - k), primary, 0.3f * flicker, 0);
            float heart = 0.25f + 0.85f * k * k;
            // Light being swallowed: streaks spiralling into the heart.
            int streaks = MagicFxManager.detail() == 0 ? 5 : 10;
            for (int i = 0; i < streaks; i++) {
                float u = (t * 0.05f + i / (float) streaks) % 1f;
                float ang = i * 2.399f + t * 0.16f + u * 2.4f;
                float r0 = heart * 1.1f + (a1 * 0.9f) * (1 - u), r1 = Math.max(heart * 0.9f, r0 - 1.1f);
                float tilt = (i % 3 - 1) * 0.45f;
                double x0 = c.x + Mth.cos(ang) * r0, y0 = c.y + Mth.sin(ang * 1.3f) * r0 * tilt, z0 = c.z + Mth.sin(ang) * r0;
                double x1 = c.x + Mth.cos(ang + 0.5f) * r1, y1 = c.y + Mth.sin((ang + 0.5f) * 1.3f) * r1 * tilt,
                        z1 = c.z + Mth.sin(ang + 0.5f) * r1;
                float sa = Mth.sin(Mth.PI * u) * flicker;
                d.taper(x0, y0, z0, x1, y1, z1, 0.02f, 0.16f, i % 3 == 0 ? 0xFF6A50 : primary, 0.1f * sa, 0.85f * sa);
            }
            // A ring of light collapsing onto it, and two rims turning about it (it has depth).
            float cr = Mth.lerp(easeOut(k), a1 * 0.9f, heart * 1.2f);
            d.ring(c.x, c.y, c.z, vx, vy, vz, Math.max(0, cr - 0.18f), cr, 0xFF5040, 0.7f * flicker * k, true);
            d.ring(c.x, c.y, c.z, Mth.sin(t * 0.21f), 1, Mth.cos(t * 0.21f), heart * 1.15f, heart * 1.35f, rim, 0.6f * flicker, true);
            d.ring(c.x, c.y, c.z, 1, Mth.sin(t * 0.17f) * 0.4f, Mth.cos(t * 0.17f), heart * 1.2f, heart * 1.4f, rim, 0.45f * flicker, true);
            // The heart: a red bloom behind, then layers of dark, lumpy and turning, then a black core.
            d.glowDisc(c.x, c.y, c.z, heart * 2.6f, FxColor.mix(primary, 0x8A0A14, 0.6f), 0.4f * flicker);
            d.darkDisc(c.x, c.y, c.z, heart * 1.7f, 0x14030A, 0.5f);
            for (int i = 0; i < 4; i++) {
                float ang = i * Mth.HALF_PI + t * (i % 2 == 0 ? 0.11f : -0.08f);
                float off = heart * 0.3f;
                d.darkDisc(c.x + Mth.cos(ang) * off, c.y + Mth.sin(ang * 1.7f) * off * 0.8f, c.z + Mth.sin(ang) * off, heart * 1.05f,
                        0x0A0206, 0.55f);
            }
            d.darkDisc(c.x, c.y, c.z, heart * 0.85f, 0x020001, 0.97f);
            d.ring(c.x, c.y, c.z, vx, vy, vz, heart * 0.9f, heart * 1.12f, 0xFF3A2A, 0.55f * flicker, true);
            return;
        }
        float s = t - pop, k = Mth.clamp(s / (life - pop), 0, 1);
        float blast = easeOut(s / 6f);
        int hot = a2 > 0 ? 0xFF2A1A : primary;
        // The pop: a flash and a shell of light thrown out, a ring racing over the ground, then the smoke and the scars.
        if (s < 5) {
            d.star(c.x, c.y, c.z, 6.5f, 0.6f + s * 0.05f, 0xFFD0C0, 1 - s / 5f);
            d.glowDisc(c.x, c.y, c.z, 3.6f, hot, 0.95f * (1 - s / 5f));
        }
        if (s < 9) {
            float sk = s / 9f;
            d.sphere(c.x, c.y, c.z, 0.6f + 3.4f * easeOut(sk), hot, 0.22f * (1 - sk));
            d.ring(c.x, c.y, c.z, vx, vy, vz, 0.4f + 4.2f * easeOut(sk), 0.8f + 4.6f * easeOut(sk), 0xFF6040, 0.7f * (1 - sk), true);
        }
        float gk = easeOut(s / 12f);
        if (gk < 1) d.groundRing(at.x, at.y - 0.93, at.z, Math.max(0, a1 * 1.8f * gk - 0.6f), a1 * 1.8f * gk + 0.2f, hot, 0.8f * (1 - gk), true);
        d.darkDisc(c.x, c.y, c.z, 1.4f + 3.2f * blast, 0x050003, 0.85f * (1 - k));
        for (int i = 0; i < 3; i++) {
            float ang = i * 2.1f + s * 0.05f;
            d.darkDisc(c.x + Mth.cos(ang) * blast * 1.4f, c.y + 0.5f * blast + i * 0.3f, c.z + Mth.sin(ang) * blast * 1.4f,
                    1f + 1.8f * blast, 0x0A0406, 0.6f * (1 - k));
        }
        d.darkRing(at.x, at.y - 0.93, at.z, 0, a1 * 0.8f, 0x0A0406, 0.6f * (1 - k * k));
        d.cracks(at.x, at.y - 0.92, at.z, 9, a1 * 1.0f, 0.08f, (long) (at.x * 7 + at.z * 13), 0xB0141E, 0.9f * (1 - k));
        if (a2 > 0 && s < 10) {
            d.lightning(c.x, c.y, c.z, c.x, c.y + 4, c.z, (long) s * 31, 6, 0.6f, 0.06f, 0xFF2A1A, 1 - s / 10f, true);
        }
    }
}
