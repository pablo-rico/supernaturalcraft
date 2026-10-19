package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/** The spell bowl's moments (v0.21): its smoke writing the spell's seal, the seal flying to its target, and the backlash. */
final class BowlFx {

    private BowlFx() {
    }

    /** The seal's normal this frame: level, turned toward the camera. */
    private static float[] facing(FxDraw d, double x, double z) {
        Vec3 eye = d.eye();
        float nx = (float) (eye.x - x), nz = (float) (eye.z - z);
        float len = Mth.sqrt(nx * nx + nz * nz);
        return len < 1e-4f ? new float[]{0, 0, 1} : new float[]{nx / len, 0, nz / len};
    }

    private static void smoke(ClientLevel level, int rgb, double x, double y, double z, double vx, double vy, double vz) {
        level.addParticle(ColorParticleOption.create(AllParticles.BOWL_SMOKE.get(), 0xFF000000 | rgb), x, y, z, vx, vy, vz);
    }

    /**
     * BOWL_SEAL: the smoke rises off the bowl and, at the top of its column, writes the spell's seal stroke by stroke (a sweep round
     * the seal, a spark at the pen's tip, the unwritten part a faint ghost); the seal holds, glowing, then burns away in embers.
     */
    static final class Seal extends MagicEffect {
        private static final double RISE = 1.6;
        private static final int HOLD = 14, BURN = 22;
        private final int draw;
        private final ResourceLocation texture;

        Seal(MagicFxPayload p) {
            this(p, Math.max(10, p.duration() > 0 ? p.duration() : 40));
        }

        private Seal(MagicFxPayload p, int draw) {
            super(p.pos(), p.school(), draw + HOLD + BURN);
            this.draw = draw;
            this.texture = MagicTextures.seal(p.argIndex());
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) MagicSounds.other("bowl.seal", at, 1f, 1f);
            if (age < draw + HOLD) {
                for (int i = FxParticles.rate(r, 2.2f); i > 0; i--) {
                    smoke(level, primary, at.x + FxParticles.j(r) * 0.12, at.y + r.nextDouble() * 0.3, at.z + FxParticles.j(r) * 0.12,
                            FxParticles.j(r) * 0.005, 0.06 + r.nextDouble() * 0.03, FxParticles.j(r) * 0.005);
                }
            }
            if (age >= draw + HOLD) {
                for (int i = FxParticles.rate(r, 4f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, k = Mth.sqrt(r.nextFloat()) * 0.75f;
                    FxParticles.ember(level, r.nextBoolean() ? primary : accent, 0.9f, 0, at.x + Mth.cos(a) * k * 0.7, at.y + RISE + Mth.sin(a) * k,
                            at.z + Mth.sin(a) * k * 0.7, FxParticles.j(r) * 0.02, 0.03 + r.nextDouble() * 0.03, FxParticles.j(r) * 0.02);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            double x = at.x, y = at.y + RISE, z = at.z;
            float[] n = facing(d, x, z);
            float written = Mth.clamp(t / draw, 0, 1);
            float burn = Mth.clamp((t - draw - HOLD) / BURN, 0, 1);
            float a = (1 - burn) * Math.min(1, t / 4f);
            float size = 0.75f * (1 + 0.3f * burn);
            float pulse = 0.85f + 0.15f * Mth.sin(t * 0.3f);
            // The column of smoke-light under it.
            d.taper(at.x, at.y, at.z, x, y - size * 0.6, z, 0.25f, 0.6f, primary, 0.35f * a, 0.05f * a);
            d.glowDisc(x, y, z, size * 2.2f, primary, 0.25f * a * pulse);
            d.disc(texture, x, y, z, n[0], n[1], n[2], size, 0, primary, 0.12f * a * (1 - written));
            d.wipe(texture, x, y, z, n[0], n[1], n[2], size, 0, written, written >= 1 ? FxColor.mix(primary, 0xFFFFFF, 0.25f * pulse) : primary,
                    0.95f * a);
            if (written < 1) {
                float ang = Mth.TWO_PI * written;
                float[] ax = new float[3], ay = new float[3];
                FxDraw.basis(n[0], n[1], n[2], ax, ay);
                double tx = x + (ax[0] * Mth.cos(ang) - ay[0] * Mth.sin(ang)) * size * 0.8,
                        ty = y + (ax[1] * Mth.cos(ang) - ay[1] * Mth.sin(ang)) * size * 0.8,
                        tz = z + (ax[2] * Mth.cos(ang) - ay[2] * Mth.sin(ang)) * size * 0.8;
                d.star(tx, ty, tz, 0.35f, t * 0.4f, FxColor.core(school), a);
                d.ribbon(x, y, z, tx, ty, tz, 0.04f, FxColor.core(school), 0.1f * a, 0.6f * a);
            } else if (t < draw + 4) {
                d.star(x, y, z, 2f, 0.3f, FxColor.core(school), 1 - (t - draw) / 4f);
            }
        }
    }

    /**
     * BOWL_SEND: the seal lifts off the bowl, shrinks, and flies in an arc to its target trailing smoke and glyphs; where it lands
     * it is stamped on the ground under the target, flaring and fading.
     */
    static final class Send extends MagicEffect {
        private static final int LIFT = 8, STAMP = 26;
        private final Vec3 dest;
        private final int target;
        private final int travel;
        private final ResourceLocation texture;
        private Vec3 last;

        Send(MagicFxPayload p) {
            this(p, Math.max(8, p.duration() > 0 ? p.duration() : 40));
        }

        private Send(MagicFxPayload p, int travel) {
            super(p.pos(), p.school(), LIFT + travel + STAMP);
            this.dest = p.target();
            this.target = p.entity();
            this.travel = travel;
            this.texture = MagicTextures.seal(p.argIndex());
            this.last = p.pos();
        }

        private Vec3 end(float partial) {
            Entity e = entity(target);
            return e != null && !e.isRemoved() ? e.getPosition(partial).add(0, e.getBbHeight() * 0.6, 0) : dest.add(0, 1, 0);
        }

        private Vec3 path(float t, float partial) {
            Vec3 lift = at.add(0, 1.5, 0);
            if (t < LIFT) return at.add(0, 1.5 * easeOut(t / LIFT), 0);
            Vec3 end = end(partial);
            float u = Mth.clamp((t - LIFT) / travel, 0, 1);
            u = u * u * (3 - 2 * u);
            double dist = lift.distanceTo(end);
            double bump = Math.min(12, 1.5 + dist * 0.15) * 4 * u * (1 - u);
            return lift.add(end.subtract(lift).scale(u)).add(0, bump, 0);
        }

        @Override
        protected Vec3 anchor() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer == null) return last;
            Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
            Vec3 best = at;
            for (Vec3 v : new Vec3[]{dest, last}) if (v.distanceToSqr(cam) < best.distanceToSqr(cam)) best = v;
            return best;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 p = path(age, 1f);
            last = p;
            if (age == 0) MagicSounds.other("bowl.send", at, 1f, 1f);
            if (age == LIFT + travel) {
                Vec3 e = end(1f);
                MagicSounds.school(school, "impact", e, 0.8f, 1.1f);
                for (int i = FxParticles.n(20); i > 0; i--) {
                    FxParticles.glyph(level, r.nextBoolean() ? primary : accent, 1f, 26, e.x, e.y, e.z, FxParticles.j(r) * 0.12,
                            0.05 + r.nextDouble() * 0.1, FxParticles.j(r) * 0.12);
                }
            }
            if (age < LIFT + travel) {
                for (int i = FxParticles.rate(r, 2.5f); i > 0; i--) {
                    smoke(level, primary, p.x + FxParticles.j(r) * 0.15, p.y + FxParticles.j(r) * 0.15, p.z + FxParticles.j(r) * 0.15, 0, 0.01, 0);
                }
                if (FxParticles.rate(r, 0.6f) > 0) FxParticles.glyph(level, accent, 0.7f, 20, p.x, p.y, p.z, 0, 0.01, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            int core = FxColor.core(school);
            if (t < LIFT + travel) {
                Vec3 p = path(t, d.partial);
                float size = 0.75f - 0.3f * Mth.clamp(t / LIFT, 0, 1);
                d.glowDisc(p.x, p.y, p.z, size * 2.4f, primary, 0.4f);
                d.billboard(texture, p.x, p.y, p.z, size * 2, t * 0.15f, primary, 0.95f);
                d.billboard(texture, p.x, p.y, p.z, size * 1.4f, -t * 0.1f, core, 0.4f);
                // A streak behind it along the arc.
                Vec3 prev = p;
                for (int i = 1; i <= 6; i++) {
                    Vec3 q = path(Math.max(0, t - i * 1.6f), d.partial);
                    float f = 1 - i / 7f;
                    d.taper(prev.x, prev.y, prev.z, q.x, q.y, q.z, 0.5f * f, 0.5f * (f - 1 / 7f), primary, 0.45f * f, 0.45f * (f - 1 / 7f));
                    prev = q;
                }
                return;
            }
            float s = t - LIFT - travel, k = Mth.clamp(s / STAMP, 0, 1);
            Entity e = entity(target);
            Vec3 feet = e != null && !e.isRemoved() ? e.getPosition(d.partial) : dest;
            double gy = feet.y + 0.05;
            if (e == null && Minecraft.getInstance().level != null) gy = FxWorld.floor(Minecraft.getInstance().level, dest.add(0, 1, 0), 4) + 0.05;
            float grow = easeOutBack(s / 6f);
            d.decal(texture, feet.x, gy, feet.z, 0.7f + 0.7f * grow, s * 0.02f, primary, 0.95f * (1 - k));
            d.groundRing(feet.x, gy, feet.z, 1.2f * grow, 1.5f * grow + 1.2f * k, primary, 0.7f * (1 - k), true);
            Vec3 c = end(d.partial);
            if (s < 5) d.star(c.x, c.y, c.z, 2.2f, 0.3f, core, 1 - s / 5f);
            d.glowDisc(c.x, c.y, c.z, 1.6f, primary, 0.5f * (1 - k));
        }
    }

    /**
     * BOWL_BACKLASH: the spell turns on the bowl. Its seal flickers red over the bowl and cracks while the air is sucked in, then
     * it bursts in a splash of ink and loose letters, a dark blot spreading and fading.
     */
    static final class Backlash extends MagicEffect {
        private static final int POP = 14;
        private final float strength;
        private final ResourceLocation texture;
        private final boolean sealed;

        Backlash(MagicFxPayload p) {
            super(p.pos(), p.school(), Math.max(p.duration(), 44));
            this.strength = Mth.clamp(p.arg() > 0 ? p.arg() : 2f, 1f, 6f);
            this.sealed = p.arg2Index() >= 0;
            this.texture = MagicTextures.seal(p.arg2Index());
        }

        @Override
        protected double reach() {
            return strength + 4;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) MagicSounds.other("ritual.implode", at, 0.9f, 1.35f);
            if (age < POP) {
                for (int i = FxParticles.rate(r, 3f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double d = strength * (0.8 + r.nextDouble() * 0.5);
                    double x = at.x + Mth.cos(a) * d, z = at.z + Mth.sin(a) * d, y = at.y + r.nextDouble() * 1.5;
                    int left = Math.max(3, POP - age);
                    FxParticles.spark(level, r.nextBoolean() ? primary : 0x30121A, 0.8f, left, x, y, z, (at.x - x) / left * 1.5,
                            (at.y + 0.6 - y) / left * 1.5, (at.z - z) / left * 1.5);
                }
            }
            if (age == POP) {
                MagicShake.kick(at, 0.45f, 10, 12);
                MagicSounds.school(school, "impact", at, 0.9f, 0.7f);
                for (int i = FxParticles.n(30); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.INK.get(), at.x, at.y + 0.4, at.z, FxParticles.j(r) * 0.3, 0.15 + r.nextDouble() * 0.25,
                            FxParticles.j(r) * 0.3);
                }
                for (int i = FxParticles.n(16); i > 0; i--) {
                    FxParticles.plain(level, AllParticles.INK_LETTER.get(), at.x, at.y + 0.5, at.z, FxParticles.j(r) * 0.2,
                            0.1 + r.nextDouble() * 0.2, FxParticles.j(r) * 0.2);
                }
                for (int i = FxParticles.n(10); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, FxParticles.j(r) * 0.12, 0.08,
                            FxParticles.j(r) * 0.12);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            double x = at.x, y = at.y + 1.4, z = at.z;
            if (t < POP) {
                float k = t / POP;
                float flick = 0.6f + 0.4f * Mth.sin(t * 2.3f) * Mth.sin(t * 0.9f);
                float[] n = facing(d, x, z);
                int red = FxColor.mix(primary, 0xFF1A1A, 0.4f + 0.6f * k);
                if (sealed) d.disc(texture, x, y, z, n[0], n[1], n[2], 0.75f * (1 - 0.3f * k), t * 0.05f * k, red, 0.85f * flick);
                d.glowDisc(x, y, z, 1.4f * (1 - 0.5f * k), red, 0.4f * flick);
                // Cracks through the seal.
                for (int i = 0; i < 1 + (int) (k * 4); i++) {
                    float ang = i * 2.1f;
                    d.lightning(x, y, z, x + Mth.cos(ang) * 0.7 * n[2], y + Mth.sin(ang) * 0.7, z - Mth.cos(ang) * 0.7 * n[0], (long) t + i,
                            4, 0.12f, 0.02f, 0xFFE0D0, 0.8f * k, false);
                }
                d.darkDisc(at.x, at.y + 0.6, at.z, 0.4f + 0.6f * k, 0x0A0306, 0.7f * k);
                return;
            }
            float s = t - POP, k = Mth.clamp(s / (life - POP), 0, 1);
            float blast = easeOut(s / 5f);
            d.darkDisc(at.x, at.y + 0.6, at.z, 0.8f + strength * 1.2f * blast, 0x07030A, 0.85f * (1 - k));
            if (s < 4) d.glowDisc(at.x, at.y + 0.6, at.z, strength * 1.5f, primary, 0.8f * (1 - s / 4f));
            // Ink spattered round the bowl.
            for (int i = 0; i < 9; i++) {
                float ang = i * 2.39996f + (float) (at.x % 3), dd = (0.6f + (i % 4) * 0.35f) * strength * 0.6f * blast;
                d.darkRing(at.x + Mth.cos(ang) * dd, at.y - 0.58, at.z + Mth.sin(ang) * dd, 0, 0.18f + (i % 3) * 0.1f, 0x050208,
                        0.8f * (1 - k * k));
            }
        }
    }
}
