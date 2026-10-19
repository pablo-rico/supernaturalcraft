package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

/** Marks on beings and on the ground (v0.21): hexes taking hold or turned away, a devil's trap reacting, the Mark of Cain flaring. */
final class SignFx {

    private SignFx() {
    }

    /**
     * HEX_PULSE. A curse: a thorny magenta ring snaps out from the victim's feet, thorns curl up round them, a sour dark bloom.
     * A protection (a Ward of the Name or a hex bag turning a curse away, a Mirror Hex breaking): a magenta-silver shell flickers
     * round the holder, rims of it turning, glints running over it where the curse struck, and a ring rises through it.
     */
    static final class Hex extends MagicEffect {
        private final int hexed;
        private final boolean curse;
        private final float radius;

        Hex(MagicFxPayload p) {
            super(p.pos(), p.school().isNone() ? School.WITCHCRAFT : p.school(), Math.max(p.duration(), 30));
            this.hexed = p.entity();
            this.curse = p.argIndex() == MagicFxPayload.HEX_CURSE;
            this.radius = Math.max(0.8f, p.arg2());
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(hexed);
            return e != null ? e.position() : at;
        }

        private Vec3 feet(float partial) {
            Entity e = entity(hexed);
            return e != null && !e.isRemoved() ? e.getPosition(partial) : at.subtract(0, 0.9, 0);
        }

        private float height() {
            Entity e = entity(hexed);
            return e != null ? e.getBbHeight() : 1.8f;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 f = feet(1f);
            if (age == 0) {
                MagicSounds.school(school, curse ? "impact" : "cast", f, 0.7f, curse ? 0.75f : 1.5f);
                for (int i = FxParticles.n(curse ? 18 : 12); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.mote(level, curse ? (r.nextBoolean() ? primary : 0x3A0A2A) : FxColor.mix(primary, 0xFFFFFF, 0.6f), 1f, 34,
                            f.x + Mth.cos(a) * 0.5, f.y + r.nextDouble() * height(), f.z + Mth.sin(a) * 0.5, Mth.cos(a) * 0.05,
                            curse ? 0.01 : 0.04, Mth.sin(a) * 0.05);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 f = feet(d.partial);
            float h = height();
            if (curse) {
                float k = easeOut(t / 12f), a = fade(t, 0, life * 0.6f);
                d.spikedRing(f.x, f.y + 0.06, f.z, radius * (0.4f + 1.1f * k), 0.45f, 9, t * 0.04f, primary, 0.6f * a);
                d.spikedRing(f.x, f.y + 0.07, f.z, radius * (0.3f + 0.7f * k), 0.3f, 6, -t * 0.06f, accent, 0.3f * a);
                for (int i = 0; i < 9; i++) {
                    float ang = i * Mth.TWO_PI / 9, th = easeOutBack((t - i * 0.7f) / 6f) * (0.7f + 0.15f * (i % 3));
                    if (th <= 0) continue;
                    double bx = f.x + Mth.cos(ang) * 0.75, bz = f.z + Mth.sin(ang) * 0.75;
                    double tx = bx - Mth.cos(ang + 0.8f) * 0.3, tz = bz - Mth.sin(ang + 0.8f) * 0.3;
                    d.taper(bx, f.y, bz, tx, f.y + th, tz, 0.14f, 0, primary, 0.85f * a, 0.3f * a);
                }
                d.darkDisc(f.x, f.y + h * 0.5, f.z, 1.2f, 0x1A0414, 0.35f * a);
                d.glowDisc(f.x, f.y + h * 0.5, f.z, 1.4f, primary, 0.3f * a);
            } else {
                float a = fade(t, 2, life * 0.6f);
                int silver = 0xE8F0FF, magenta = FxColor.mix(primary, 0xFF4AC8, 0.35f);
                int soft = FxColor.mix(magenta, silver, 0.5f);
                float rr = Math.max(0.85f, h * 0.62f);
                double cy = f.y + h * 0.5;
                // The shell flickers like a struck ward: bright at first, then stuttering out.
                float flick = 0.55f + 0.45f * Mth.sin(t * 2.7f) * Mth.sin(t * 1.3f + 0.6f);
                float hit = Math.max(0, 1 - t / 6f);
                float sa = a * Math.max(hit, flick);
                d.sphere(f.x, cy, f.z, rr * (1 + 0.04f * Mth.sin(t * 0.6f) + 0.08f * hit), soft, 0.14f * sa + 0.15f * hit);
                for (int i = 0; i < 3; i++) {
                    float ang = t * (0.05f + 0.02f * i) + i * 2.1f;
                    float nx = Mth.sin(ang) * (0.4f + 0.3f * i), nz = Mth.cos(ang) * (0.4f + 0.3f * i);
                    d.ring(f.x, cy, f.z, nx, 1, nz, rr * 0.97f, rr * 1.03f, i == 1 ? silver : magenta, 0.55f * sa, true);
                }
                // Glints running over the shell where it turned the curse.
                for (int i = 0; i < 6; i++) {
                    float u = (t * 0.04f + i / 6f) % 1f;
                    float lon = i * 2.4f + t * 0.03f, lat = (u - 0.5f) * 2.4f;
                    double gx = f.x + Mth.cos(lon) * Mth.cos(lat) * rr, gy = cy + Mth.sin(lat) * rr, gz = f.z + Mth.sin(lon) * Mth.cos(lat) * rr;
                    d.star(gx, gy, gz, 0.28f, t * 0.1f, silver, a * Mth.sin(Mth.PI * u) * flick);
                }
                if (t < 5) d.star(f.x, cy, f.z, rr * 2.2f, 0.2f, silver, 0.8f * (1 - t / 5f));
                float u = Mth.clamp(t / (life * 0.7f), 0, 1);
                d.groundRing(f.x, f.y + 0.05 + u * h, f.z, rr * 0.75f, rr * 0.95f, soft, 0.8f * a * Mth.sin(Mth.PI * u), true);
                d.groundRing(f.x, f.y + 0.04, f.z, rr * 0.9f, rr * 1.1f, magenta, 0.6f * a, true);
                d.glowDisc(f.x, cy, f.z, rr * 1.5f, magenta, 0.18f * sa);
            }
        }
    }

    /**
     * TRAP_FLARE: the devil's trap's lines (a circle and a five-pointed star) light up red on the ground. Caught: they flare and a
     * red curtain rises round the trap, then settle to a smoulder. Strain: they flicker and an arc snaps from the prisoner to a
     * point of the star. Broken: the lines shatter, their pieces flying up and out.
     */
    static final class Trap extends MagicEffect {
        private final int caught;
        private final int state;
        private final float radius;
        private final double[] vx = new double[5], vz = new double[5];

        Trap(MagicFxPayload p) {
            super(p.pos(), School.HUNTER, Math.max(p.duration(), p.arg2Index() == MagicFxPayload.TRAP_CAUGHT ? 40 : 24));
            this.caught = p.entity();
            this.state = p.arg2Index();
            this.radius = Math.max(1f, p.arg());
            for (int k = 0; k < 5; k++) {
                float a = -Mth.HALF_PI + k * Mth.TWO_PI / 5;
                vx[k] = at.x + Mth.cos(a) * radius * 0.92;
                vz[k] = at.z + Mth.sin(a) * radius * 0.92;
            }
        }

        @Override
        protected double reach() {
            return radius + 4;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                switch (state) {
                    case MagicFxPayload.TRAP_CAUGHT -> {
                        MagicSounds.school(School.HUNTER, "impact", at, 0.9f, 0.8f);
                        MagicShake.kick(at, 0.3f, 8, 10);
                    }
                    case MagicFxPayload.TRAP_STRAIN -> MagicSounds.school(School.HUNTER, "impact", at, 0.45f, 1.4f);
                    default -> MagicSounds.school(School.HUNTER, "impact", at, 1f, 0.55f);
                }
            }
            if (state == MagicFxPayload.TRAP_BROKEN && age == 0) {
                for (int i = FxParticles.n(30); i > 0; i--) {
                    int k = r.nextInt(5);
                    double u = r.nextDouble();
                    int j = (k + 2) % 5;
                    FxParticles.spark(level, FxColor.TRAP, 1f, 0, Mth.lerp(u, vx[k], vx[j]), at.y + 0.05, Mth.lerp(u, vz[k], vz[j]),
                            FxParticles.j(r) * 0.1, 0.1 + r.nextDouble() * 0.15, FxParticles.j(r) * 0.1);
                }
                return;
            }
            float rate = state == MagicFxPayload.TRAP_CAUGHT && age < 10 ? 3f : 0.6f;
            for (int i = FxParticles.rate(r, rate); i > 0 && age < life - 6; i--) {
                int k = r.nextInt(5);
                double u = r.nextDouble();
                int j = (k + 2) % 5;
                FxParticles.ember(level, FxColor.TRAP, 0.8f, 0, Mth.lerp(u, vx[k], vx[j]), at.y + 0.05, Mth.lerp(u, vz[k], vz[j]), 0,
                        0.04 + r.nextDouble() * 0.04, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            double y = at.y + 0.02;
            if (state == MagicFxPayload.TRAP_BROKEN) {
                float k = Mth.clamp(t / life, 0, 1), a = 1 - k;
                float fly = easeOut(t / 10f);
                for (int i = 0; i < 5; i++) {
                    int j = (i + 2) % 5;
                    for (int s = 0; s < 3; s++) {
                        double u0 = s / 3.0, u1 = (s + 1) / 3.0;
                        double mx = Mth.lerp((u0 + u1) / 2, vx[i], vx[j]) - at.x, mz = Mth.lerp((u0 + u1) / 2, vz[i], vz[j]) - at.z;
                        double ox = mx * 0.6 * fly, oz = mz * 0.6 * fly, oy = fly * (1.2 + 0.3 * ((i + s) % 3)) - t * t * 0.004;
                        d.ribbon(Mth.lerp(u0, vx[i], vx[j]) + ox, y + oy, Mth.lerp(u0, vz[i], vz[j]) + oz, Mth.lerp(u1, vx[i], vx[j]) + ox,
                                y + oy + 0.1 * (s - 1), Mth.lerp(u1, vz[i], vz[j]) + oz, 0.1f, FxColor.TRAP, a, a * 0.5f);
                    }
                }
                if (t < 5) d.glowDisc(at.x, y + 0.5, at.z, radius * 2, FxColor.TRAP, 0.7f * (1 - t / 5f));
                d.groundRing(at.x, y, at.z, radius * (1 + fly), radius * (1 + fly) + 0.3f, FxColor.TRAP, 0.6f * a, true);
                return;
            }
            float a, flare;
            if (state == MagicFxPayload.TRAP_CAUGHT) {
                flare = Math.max(0, 1 - t / 8f);
                a = Math.max(0.35f, flare) * fade(t, 0, 10);
            } else {
                flare = 0;
                float flick = 0.5f + 0.5f * Mth.sin(t * 2.7f) * Mth.sin(t * 1.1f + 1);
                a = (0.45f + 0.55f * flick) * fade(t, 0, 8);
            }
            int col = FxColor.mix(FxColor.TRAP, 0xFFD0B0, flare * 0.6f);
            float w = 0.14f + 0.12f * flare;
            for (int i = 0; i < 5; i++) {
                int j = (i + 2) % 5;
                d.groundLine(vx[i], vz[i], vx[j], vz[j], y + 0.005, w, col, a);
                d.groundLine(vx[i], vz[i], vx[j], vz[j], y + 0.004, w * 3, FxColor.TRAP, 0.3f * a);
            }
            d.groundRing(at.x, y, at.z, radius - 0.12f, radius, col, a, false);
            d.groundRing(at.x, y, at.z, radius - 0.5f, radius + 0.3f, FxColor.TRAP, 0.25f * a, true);
            d.flatGlow(at.x, y, at.z, radius * 1.1f, FxColor.TRAP, 0.2f * a);
            if (flare > 0) d.cylinder(at.x, y, at.z, radius, 2.4f * (0.5f + flare), FxColor.TRAP, 0.6f * flare, 0);
            if (state == MagicFxPayload.TRAP_STRAIN) {
                Entity e = entity(caught);
                Vec3 c = centre(e, d.partial, at.add(0, 1, 0));
                int k = (int) (t / 4) % 5;
                d.lightning(c.x, c.y, c.z, vx[k], y + 0.05, vz[k], (long) (t / 2), 6, 0.25f, 0.035f, FxColor.TRAP, 0.9f * a, false);
            }
        }
    }

    /**
     * MARK_FLARE: the Mark of Cain flares red on the bearer's forearm, veins of it crawling out (pulse); a red haze wraps them and
     * embers rise (bloodlust); red motes rush into the Mark (a kill feeds it). On the local player it throbs the screen's edges
     * instead of drawing on an arm they cannot see in first person.
     */
    static final class Mark extends MagicEffect {
        private final int marked;
        private final int state;
        private final float thirst;

        Mark(MagicFxPayload p) {
            super(p.pos(), School.INFERNAL, Math.max(p.duration(), switch (p.arg2Index()) {
                case MagicFxPayload.MARK_BLOODLUST -> 40;
                case MagicFxPayload.MARK_KILL -> 26;
                default -> 18;
            }));
            this.marked = p.entity();
            this.state = p.arg2Index();
            this.thirst = Mth.clamp(p.arg(), 0, 1);
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(marked);
            return e != null ? e.position() : at;
        }

        private boolean local() {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && mc.player.getId() == marked;
        }

        private boolean hidden() {
            return local() && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        }

        /** The bearer's right forearm, where the Mark is drawn ({@code client/mark/MarkArm}), whichever hand they favour. */
        private Vec3 arm(float partial) {
            Entity e = entity(marked);
            if (e == null || e.isRemoved()) return at.add(0, 1, 0);
            float yaw = (e instanceof LivingEntity l ? Mth.lerp(partial, l.yBodyRotO, l.yBodyRot) : e.getYRot()) * Mth.DEG_TO_RAD;
            float side = -1;
            Vec3 p = e.getPosition(partial);
            // The body faces (-sin yaw, cos yaw); its right is (-cos yaw, -sin yaw).
            return p.add(Mth.cos(yaw) * 0.38 * side, e.getBbHeight() * 0.48, Mth.sin(yaw) * 0.38 * side);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                if (local()) MagicOverlay.throb(switch (state) {
                    case MagicFxPayload.MARK_BLOODLUST -> 1f;
                    case MagicFxPayload.MARK_KILL -> 0.7f;
                    default -> 0.35f + 0.4f * thirst;
                });
                if (state == MagicFxPayload.MARK_BLOODLUST) MagicSounds.school(School.INFERNAL, "cast", at, 0.8f, 0.55f);
                if (state == MagicFxPayload.MARK_KILL) MagicSounds.school(School.INFERNAL, "impact", at, 0.5f, 0.6f);
            }
            if (hidden()) return;
            Vec3 m = arm(1f);
            if (state == MagicFxPayload.MARK_BLOODLUST) {
                Entity e = entity(marked);
                Vec3 f = e != null ? e.position() : at;
                for (int i = FxParticles.rate(r, 2f); i > 0; i--) {
                    FxParticles.ember(level, r.nextBoolean() ? FxColor.BLOOD : 0xFF4A2A, 0.9f, 0, f.x + FxParticles.j(r) * 0.5,
                            f.y + r.nextDouble() * 1.8, f.z + FxParticles.j(r) * 0.5, 0, 0.05, 0);
                }
            } else if (state == MagicFxPayload.MARK_KILL && age < 14) {
                for (int i = FxParticles.rate(r, 3f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double d = 2 + r.nextDouble();
                    double x = m.x + Mth.cos(a) * d, y = m.y + FxParticles.j(r), z = m.z + Mth.sin(a) * d;
                    FxParticles.spark(level, FxColor.BLOOD, 0.9f, 10, x, y, z, (m.x - x) / 9, (m.y - y) / 9, (m.z - z) / 9);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            if (hidden()) return;
            float t = time(d);
            Vec3 m = arm(d.partial);
            float a = fade(t, 2, life * 0.5f);
            float beat = 0.7f + 0.3f * Mth.sin(t * 0.8f);
            float size = 0.25f + 0.25f * thirst;
            d.glowDisc(m.x, m.y, m.z, size * 2.4f, FxColor.BLOOD, 0.55f * a * beat);
            d.star(m.x, m.y, m.z, size * 1.6f, 0.785f, 0xFF5040, 0.9f * a * beat);
            for (int i = 0; i < 3 + (int) (thirst * 4); i++) {
                float ang = i * 2.1f + (float) (marked % 5);
                d.lightning(m.x, m.y, m.z, m.x + Mth.cos(ang) * 0.45, m.y + Mth.sin(ang * 1.7f) * 0.4, m.z + Mth.sin(ang) * 0.45,
                        (long) (t / 3) + i, 4, 0.08f, 0.02f, FxColor.BLOOD, 0.8f * a, false);
            }
            if (state == MagicFxPayload.MARK_BLOODLUST) {
                Entity e = entity(marked);
                if (e != null) {
                    Vec3 f = e.getPosition(d.partial);
                    d.cylinder(f.x, f.y, f.z, e.getBbWidth() * 0.9f, e.getBbHeight() * 1.1f, FxColor.BLOOD, 0.3f * a * beat, 0);
                    d.darkDisc(f.x, f.y + e.getBbHeight() * 0.5, f.z, 1.3f, 0x1A0204, 0.3f * a);
                }
            } else if (state == MagicFxPayload.MARK_KILL && t < 6) {
                d.star(m.x, m.y, m.z, 1.6f, 0.2f, 0xFF6050, 1 - t / 6f);
            }
        }
    }
}
