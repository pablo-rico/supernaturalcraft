package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/**
 * WARD_PULSE: a ward's dome. Raised: its edge races out along the ground and a wall of light and glyphs rises off it and fades
 * into a faint dome. Hit: the whole dome flashes and ripples. Broken: the dome flashes once and bursts into falling shards.
 */
final class WardPulseFx extends MagicEffect {

    private final int ward;
    private final int state;
    private final float radius;
    private double ground;
    private boolean grounded;

    WardPulseFx(MagicFxPayload p) {
        super(p.pos(), p.school(), Math.max(p.duration(), switch (p.arg2Index()) {
            case MagicFxPayload.WARD_HIT -> 14;
            case MagicFxPayload.WARD_BROKEN -> 30;
            default -> 34;
        }));
        this.ward = p.entity();
        this.state = p.arg2Index();
        this.radius = Math.max(1f, p.arg());
    }

    @Override
    protected double reach() {
        return radius + 4;
    }

    private Vec3 centre(float partial) {
        Entity e = entity(ward);
        return e != null && !e.isRemoved() ? e.getPosition(partial) : at;
    }

    @Override
    protected void tick(ClientLevel level) {
        if (!grounded) {
            ground = FxWorld.floor(level, at, 4);
            grounded = true;
        }
        if (age != 0) return;
        RandomSource r = level.random;
        Vec3 c = centre(1f);
        switch (state) {
            case MagicFxPayload.WARD_HIT -> MagicSounds.school(school, "impact", c, 0.4f, 1.6f);
            case MagicFxPayload.WARD_BROKEN -> {
                MagicSounds.school(school, "impact", c, 0.9f, 0.7f);
                for (int i = FxParticles.n(36); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, up = r.nextFloat() * Mth.HALF_PI;
                    double x = c.x + Mth.cos(a) * Mth.cos(up) * radius, y = ground + Mth.sin(up) * radius, z = c.z + Mth.sin(a) * Mth.cos(up) * radius;
                    FxParticles.glyph(level, primary, 0.9f, 24, x, y, z, Mth.cos(a) * 0.06, -0.02, Mth.sin(a) * 0.06);
                }
                for (int i = FxParticles.n(10); i > 0; i--) FxParticles.plain(level, ParticleTypes.END_ROD, c.x + FxParticles.j(r) * radius,
                        ground + r.nextDouble() * radius, c.z + FxParticles.j(r) * radius, 0, -0.05, 0);
                MagicShake.kick(c, 0.3f, 8, 12);
            }
            default -> {
                MagicSounds.school(school, "cast", c, 0.6f, 1.2f);
                for (int i = FxParticles.n(20); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.glyph(level, primary, 1f, 26, c.x + Mth.cos(a) * radius, ground + 0.1, c.z + Mth.sin(a) * radius, 0,
                            0.06 + r.nextDouble() * 0.05, 0);
                }
            }
        }
    }

    @Override
    protected void render(FxDraw d) {
        if (!grounded) {
            if (Minecraft.getInstance().level == null) return;
            ground = FxWorld.floor(Minecraft.getInstance().level, at, 4);
            grounded = true;
        }
        Vec3 c = centre(d.partial);
        float t = time(d), k = progress(d);
        double y = Math.max(ground, c.y - 0.5) + 0.04;
        switch (state) {
            case MagicFxPayload.WARD_HIT -> {
                float a = 1 - k;
                d.sphere(c.x, y, c.z, radius * (1 + 0.04f * Mth.sin(t * 2)), primary, 0.16f * a);
                d.groundRing(c.x, y, c.z, radius * 0.92f, radius * 1.04f, FxColor.core(school), 0.8f * a, true);
                d.cylinder(c.x, y, c.z, radius, radius * 0.6f, primary, 0.4f * a, 0);
            }
            case MagicFxPayload.WARD_BROKEN -> {
                float a = 1 - k;
                if (t < 5) d.sphere(c.x, y, c.z, radius, FxColor.core(school), 0.35f * (1 - t / 5f));
                float out = easeOut(t / 10f);
                d.groundRing(c.x, y, c.z, radius * (1 + out), radius * (1 + out) + 0.5f, primary, 0.8f * a, true);
                // Shards of the dome falling away.
                for (int i = 0; i < 14; i++) {
                    float ang = i * 2.39996f, up = (i % 5) / 5f * Mth.HALF_PI;
                    float fall = t * t * 0.004f;
                    double sx = c.x + Mth.cos(ang) * Mth.cos(up) * radius * (1 + out * 0.4f),
                            sy = y + Mth.sin(up) * radius - fall, sz = c.z + Mth.sin(ang) * Mth.cos(up) * radius * (1 + out * 0.4f);
                    if (sy < ground) continue;
                    d.crystal(sx, sy, sz, Mth.cos(ang + t * 0.2f), 0.5f, Mth.sin(ang), 0.12f, 0.26f, primary, 0.7f * a, true);
                }
            }
            default -> {
                float out = easeOut(t / 10f);
                float a = fade(t, 1, 14);
                d.groundRing(c.x, y, c.z, Math.max(0, radius * out - 0.3f), radius * out, FxColor.core(school), 0.9f * a, false);
                d.groundRing(c.x, y, c.z, radius * out * 0.6f, radius * out, primary, 0.25f * a, true);
                float rise = Mth.clamp((t - 4) / 16f, 0, 1);
                if (rise > 0) {
                    d.cylinder(c.x, y, c.z, radius, radius * 0.9f * rise, primary, 0.45f * a, 0);
                    if (MagicFxManager.detail() > 0) {
                        d.band(MagicTextures.glyphStrip(), c.x, y, c.z, radius * 1.01f, 0.6f, t * 0.02f, primary, 0.8f * a, 0.2f * a);
                    }
                    d.sphere(c.x, y, c.z, radius, primary, 0.08f * a * rise);
                }
            }
        }
    }
}
