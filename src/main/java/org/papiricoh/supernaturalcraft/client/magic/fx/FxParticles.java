package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.papiricoh.supernaturalcraft.magic.fx.MagicParticleOptions;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * Spawning the magic's particles within a budget (v0.21): every count goes through {@link #n}, which scales it by the client's
 * {@code magicFxIntensity} and stops once this tick's budget is spent, so a crowd of casters cannot flood the particle engine.
 */
public final class FxParticles {

    private static int left;

    private FxParticles() {
    }

    /** A new tick: the budget refills. */
    static void refill() {
        left = switch (MagicFxManager.detail()) {
            case 0 -> 60;
            case 1 -> 220;
            default -> 600;
        };
    }

    /** How many of {@code full} particles to spawn now (taken from the budget). */
    public static int n(int full) {
        if (full <= 0 || left <= 0) return 0;
        int want = switch (MagicFxManager.detail()) {
            case 0 -> full >= 6 ? Math.max(1, full / 8) : 0;
            case 1 -> Math.max(1, Math.round(full * 0.45f));
            default -> full;
        };
        want = Math.min(want, left);
        left -= want;
        return want;
    }

    /** As {@link #n} for a fractional rate: {@code rate} 0.3 spawns one about every third call. */
    public static int rate(RandomSource r, float rate) {
        int whole = (int) rate;
        return n(whole + (r.nextFloat() < rate - whole ? 1 : 0));
    }

    public static void spark(ClientLevel level, int rgb, float scale, int lifetime, double x, double y, double z, double vx, double vy,
                             double vz) {
        emit(level, AllParticles.MAGIC_SPARK.get(), rgb, scale, lifetime, x, y, z, vx, vy, vz);
    }

    public static void glyph(ClientLevel level, int rgb, float scale, int lifetime, double x, double y, double z, double vx, double vy,
                             double vz) {
        emit(level, AllParticles.MAGIC_GLYPH.get(), rgb, scale, lifetime, x, y, z, vx, vy, vz);
    }

    public static void ember(ClientLevel level, int rgb, float scale, int lifetime, double x, double y, double z, double vx, double vy,
                             double vz) {
        emit(level, AllParticles.RUNE_EMBER.get(), rgb, scale, lifetime, x, y, z, vx, vy, vz);
    }

    public static void mote(ClientLevel level, int rgb, float scale, int lifetime, double x, double y, double z, double vx, double vy,
                            double vz) {
        emit(level, AllParticles.WITCH_MOTE.get(), rgb, scale, lifetime, x, y, z, vx, vy, vz);
    }

    public static void salt(ClientLevel level, int rgb, float scale, int lifetime, double x, double y, double z, double vx, double vy,
                            double vz) {
        emit(level, AllParticles.SALT_GRAIN.get(), rgb, scale, lifetime, x, y, z, vx, vy, vz);
    }

    public static void emit(ClientLevel level, ParticleType<MagicParticleOptions> type, int rgb, float scale, int lifetime, double x,
                            double y, double z, double vx, double vy, double vz) {
        level.addParticle(new MagicParticleOptions(type, rgb, Mth.clamp(scale, 0.01f, 16f), Mth.clamp(lifetime, 0, 1200)), x, y, z,
                vx, vy, vz);
    }

    /** Any particle (vanilla smoke, the mod's soul wisps...). */
    public static void plain(ClientLevel level, ParticleOptions options, double x, double y, double z, double vx, double vy, double vz) {
        level.addParticle(options, x, y, z, vx, vy, vz);
    }

    /** A random unit-ish vector component in [-1, 1]. */
    public static double j(RandomSource r) {
        return r.nextDouble() * 2 - 1;
    }
}
