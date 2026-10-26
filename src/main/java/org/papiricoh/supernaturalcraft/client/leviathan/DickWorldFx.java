package org.papiricoh.supernaturalcraft.client.leviathan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Dick Roman's fight and the Leviathans in the world (v0.25), on the magic's effect kit ({@link MagicEffect}, {@link FxDraw}): each
 * {@code DickFxPayload} moment over its telegraph. The goo is drawn dark (normal blending: {@link FxDraw#darkDisc},
 * {@link FxDraw#darkRing}, unlit {@link FxDraw#crystal}) with a faint oil-blue glow on top for its sheen; borax is white light.
 * <ul>
 *   <li>{@link Downsize}: a steel-white shockwave ring racing out over the floor, paper sheets and dust thrown up at its edge;</li>
 *   <li>{@link Contract}: a violet signature writing itself round the bound hunter's feet, the pen's nib glowing at its tip, a
 *   thin violet column holding them;</li>
 *   <li>{@link Bite}: two rows of teeth closing on the cone's far end, a black spray as they snap;</li>
 *   <li>{@link Swallow}: a dark vortex pulled into his maw (the swallowed hunter sees black); spat out, a gout of goo;</li>
 *   <li>{@link GooSplash}: blobs of goo arcing from him onto the spot, a glossy black puddle left there, fading;</li>
 *   <li>{@link Copy}: an oil-sheen shimmer rippling up the copy, as if its skin were settling;</li>
 *   <li>{@link Exposed}: the disguise bursting off him in black goo and a white flash, a daze of sparks over his head;</li>
 *   <li>{@link Tentacle}: a goo tentacle rising from the floor and slamming down the lane, then sinking back;</li>
 *   <li>{@link Surge}: the goo bubbling up at the arena's rim through the windup, then a black wave rolling in to its centre;</li>
 *   <li>{@link Borax}: a white burst and ring of powder, fizzing ash and bubbles (more where it burned something);</li>
 *   <li>{@link Decapitate}: a black spurt pumping out of a Leviathan's neck;</li>
 *   <li>{@link Reveal}: the head splitting into the maw, a ring of teeth flowering open in a splash of black.</li>
 * </ul>
 */
public final class DickWorldFx {

    static final int GOO = 0x05070B, GOO_DEEP = 0x020305, OIL = 0x4A7AB8, OIL_PALE = 0x9CC4F0, BORAX = 0xF6F6F2, STEEL = 0xDCE6F0,
            VIOLET = 0xB36BFF, VIOLET_HOT = 0xE8C8FF, TEETH = 0xE8E2D0, GUM = 0x5A0A12;
    static final DustParticleOptions GOO_DUST = new DustParticleOptions(new Vector3f(0.02f, 0.025f, 0.035f), 1.4f);

    private DickWorldFx() {
    }

    // --- starting them (ClientDickFx and the previews) -----------------------------------------------------------------------

    public static void downsize(Vec3 centre, float radius, int ticks) {
        MagicFxManager.add(new Downsize(centre, radius, ticks));
    }

    public static void contract(int hunter, Vec3 at, int ticks) {
        MagicFxManager.add(new Contract(hunter, at, ticks));
    }

    public static void bite(Vec3 apex, float yaw, float reach, int ticks) {
        MagicFxManager.add(new Bite(apex, yaw, reach, ticks));
    }

    public static void swallow(int dick, int hunter, boolean spat, Vec3 maw, int ticks) {
        MagicFxManager.add(new Swallow(dick, spat, maw, ticks));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == hunter) {
            DickOverlay.swallowed(spat ? 0 : ticks);
            MagicOverlay.flash(mc.player.position(), spat ? 0x1A2636 : GOO, spat ? 0.5f : 0.95f, 8);
            MagicShake.kick(mc.player.position(), spat ? 0.8f : 0.5f, 10, 8);
        }
    }

    public static void gooSplash(int dick, Vec3 land, float radius, int ticks) {
        Entity e = entity(dick);
        Vec3 from = e != null ? e.position().add(0, e.getBbHeight() * 0.85, 0) : land.add(0, 3, 0);
        MagicFxManager.add(new GooSplash(from, land, radius, ticks));
    }

    public static void copy(int who, boolean self, Vec3 at, int ticks) {
        MagicFxManager.add(new Copy(who, self, at, ticks));
    }

    public static void exposed(int dick, int doubles, Vec3 at, int ticks) {
        MagicFxManager.add(new Exposed(dick, doubles, at, ticks));
    }

    public static void tentacle(Vec3 from, float yaw, float reach, int ticks) {
        MagicFxManager.add(new Tentacle(from, yaw, reach, ticks));
    }

    public static void surge(Vec3 centre, float radius, int windup) {
        MagicFxManager.add(new Surge(centre, radius, windup));
    }

    public static void borax(Vec3 at, float radius, int burned, int ticks) {
        MagicFxManager.add(new Borax(at, radius, burned, ticks));
    }

    public static void decapitate(int body, Vec3 neck, int ticks) {
        MagicFxManager.add(new Decapitate(body, neck, ticks));
    }

    public static void reveal(int who, Vec3 head, int ticks) {
        MagicFxManager.add(new Reveal(who, head, ticks));
    }

    // --- shared ----------------------------------------------------------------------------------------------------------------

    private static Entity entity(int id) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || id < 0 ? null : mc.level.getEntity(id);
    }

    private static double groundY(Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? at.y : FxWorld.floor(mc.level, at.add(0, 1.5, 0), 6);
    }

    private static BlockState ground(ClientLevel level, double x, double y, double z) {
        BlockState s = level.getBlockState(BlockPos.containing(x, y - 0.5, z));
        return s.isAir() ? Blocks.BLACK_CONCRETE.defaultBlockState() : s;
    }

    private static float hash(long seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** A glossy blob of goo: a dark body and an oil-blue glint on its upper side. */
    static void blob(FxDraw d, double x, double y, double z, float r, float alpha) {
        d.darkDisc(x, y, z, r, GOO, 0.85f * alpha);
        d.darkDisc(x, y, z, r * 0.6f, GOO_DEEP, 0.6f * alpha);
        d.glowDisc(x, y + r * 0.35, z, r * 0.35f, OIL, 0.22f * alpha);
    }

    /** A glossy puddle on the floor: overlapping dark pools and a sheen sliding across it. */
    static void puddle(FxDraw d, double x, double y, double z, float r, long seed, float t, float alpha) {
        d.darkRing(x, y, z, 0, r, GOO, 0.75f * alpha);
        for (int i = 0; i < 5; i++) {
            float a = hash(seed, i) * Mth.TWO_PI, k = 0.35f + 0.45f * hash(seed, i + 10);
            d.darkRing(x + Mth.cos(a) * r * k, y + 0.002 * i, z + Mth.sin(a) * r * k, 0, r * (0.35f + 0.25f * hash(seed, i + 20)), GOO, 0.6f * alpha);
        }
        float sa = t * 0.04f + hash(seed, 30) * 6;
        d.flatGlow(x + Mth.cos(sa) * r * 0.4, y + 0.02, z + Mth.sin(sa) * r * 0.4, r * 0.45f, OIL, 0.18f * alpha);
    }

    private static void inkBurst(ClientLevel level, RandomSource r, Vec3 c, int n, double speed, double up) {
        for (int i = FxParticles.n(n); i > 0; i--) {
            FxParticles.plain(level, i % 3 == 0 ? ParticleTypes.SQUID_INK : GOO_DUST, c.x, c.y, c.z, FxParticles.j(r) * speed,
                    up * r.nextDouble() + FxParticles.j(r) * speed * 0.5, FxParticles.j(r) * speed);
        }
    }

    // --- downsizing ------------------------------------------------------------------------------------------------------------

    /** "Downsizing": a shockwave ring racing out from him. */
    static final class Downsize extends MagicEffect {
        private final float radius;
        private final double gy;
        private final int run;

        Downsize(Vec3 centre, float radius, int ticks) {
            super(centre, School.NONE, Math.max(8, ticks) + 14);
            this.radius = Math.max(1, radius);
            this.run = Math.max(6, ticks);
            this.gy = groundY(centre);
        }

        @Override
        protected double reach() {
            return radius + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private float front(float t) {
            return radius * easeOut(t / run);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) MagicShake.kick(at, 0.5f, 8, radius + 6);
            if (age < run) {
                float f = front(age);
                ItemParticleOption paper = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER));
                for (int i = FxParticles.n(6); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double x = at.x + Mth.cos(a) * f, z = at.z + Mth.sin(a) * f;
                    FxParticles.plain(level, i % 2 == 0 ? paper : new BlockParticleOption(ParticleTypes.BLOCK, ground(level, x, gy, z)), x, gy + 0.15, z,
                            Mth.cos(a) * 0.25, 0.2 + r.nextDouble() * 0.2, Mth.sin(a) * 0.25);
                }
                if (r.nextFloat() < 0.6f) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.plain(level, ParticleTypes.CLOUD, at.x + Mth.cos(a) * f, gy + 0.2, at.z + Mth.sin(a) * f, Mth.cos(a) * 0.1, 0.02,
                            Mth.sin(a) * 0.1);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 14);
            float f = front(Math.min(t, run));
            float wave = 1 - Mth.clamp((t - run) / 10f, 0, 1);
            d.groundRing(at.x, gy + 0.05, at.z, Math.max(0, f - 1.2f), f, STEEL, 0.75f * a * wave, true);
            d.groundRing(at.x, gy + 0.06, at.z, Math.max(0, f - 0.25f), f + 0.05f, 0xFFFFFF, 0.9f * a * wave, false);
            // A short standing wall of air at the front.
            d.ring(at.x, gy + 0.4, at.z, 0, 1, 0, Math.max(0, f - 0.15f), f + 0.15f, STEEL, 0.25f * a * wave, true);
            d.flatGlow(at.x, gy + 0.04, at.z, radius * 0.9f, STEEL, 0.2f * (1 - Mth.clamp(t / 8f, 0, 1)));
            d.cracks(at.x, gy + 0.05, at.z, 9, Math.min(f, radius * 0.7f), 0.08f, (long) (at.x * 13 + at.z * 7), STEEL, 0.4f * a);
        }
    }

    // --- the contract ----------------------------------------------------------------------------------------------------------

    /** "Sign here": a violet signature written round the bound hunter's feet, holding them. */
    static final class Contract extends MagicEffect {
        private static final int STROKES = 72;
        private final int hunter;
        private final long seed;

        Contract(int hunter, Vec3 at, int ticks) {
            super(at, School.WITCHCRAFT, Math.max(20, ticks) + 8);
            this.hunter = hunter;
            this.seed = hunter * 7919L + (long) (at.x * 31 + at.z);
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(hunter);
            return e != null && !e.isRemoved() ? e.position() : at;
        }

        private Vec3 feet(float partial) {
            Entity e = entity(hunter);
            return e != null && !e.isRemoved() ? e.getPosition(partial) : at;
        }

        /** The signature's point at {@code k} (0-1) round a hunter at the origin: a loop of cursive swashes. */
        private double[] point(float k) {
            float a = k * Mth.TWO_PI * 1.08f + hash(seed, 0) * Mth.TWO_PI;
            float loops = 7 + (int) (hash(seed, 1) * 4);
            float r = 0.85f + 0.16f * Mth.sin(a * loops) + 0.06f * Mth.sin(a * 3 + hash(seed, 2) * 6);
            // The swash's little curls: a small circle riding along it.
            float curl = 0.08f * Mth.cos(a * loops * 2);
            return new double[]{Mth.cos(a) * (r + curl), Mth.sin(a) * (r + curl)};
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 f = feet(1f);
            if (r.nextFloat() < 0.5f && age < life - 6) {
                float a = r.nextFloat() * Mth.TWO_PI;
                FxParticles.glyph(level, r.nextBoolean() ? VIOLET : VIOLET_HOT, 0.7f, 24, f.x + Mth.cos(a) * 0.9, f.y + 0.1, f.z + Mth.sin(a) * 0.9,
                        0, 0.04, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 2, 8);
            Vec3 f = feet(d.partial);
            double y = f.y + 0.05;
            float written = Mth.clamp(t / 12f, 0, 1);
            int n = Math.max(1, (int) (STROKES * written));
            double[] prev = point(0);
            for (int i = 1; i <= n; i++) {
                double[] p = point(i / (float) STROKES);
                float w = 0.07f + 0.05f * Mth.sin(i * 0.9f);
                d.groundLine(f.x + prev[0], f.z + prev[1], f.x + p[0], f.z + p[1], y, w * 2.4f, VIOLET, 0.45f * a);
                d.groundLine(f.x + prev[0], f.z + prev[1], f.x + p[0], f.z + p[1], y + 0.01, w, VIOLET_HOT, 0.9f * a);
                prev = p;
            }
            // The nib's glow at the tip while it writes, then a flourish underline flash.
            if (written < 1) d.glowDisc(f.x + prev[0], y + 0.15, f.z + prev[1], 0.3f, VIOLET_HOT, 0.9f * a);
            float pulse = 0.75f + 0.25f * Mth.sin(t * 0.4f);
            d.flatGlow(f.x, y, f.z, 1.3f, VIOLET, 0.25f * a * pulse);
            // The column holding them.
            d.cylinder(f.x, f.y, f.z, 0.7f, 2.2f, VIOLET, 0.22f * a * pulse, 0);
        }
    }

    // --- the bite --------------------------------------------------------------------------------------------------------------

    /** The maw's bite: two rows of teeth snapping shut at the cone's far end. */
    static final class Bite extends MagicEffect {
        private static final int TEETH_N = 9;
        private final float yaw, reach;
        private final double gy;

        Bite(Vec3 apex, float yaw, float reach, int ticks) {
            super(apex, School.NONE, Math.max(6, ticks) + 14);
            this.yaw = yaw;
            this.reach = Math.max(1, reach);
            this.gy = groundY(apex);
        }

        @Override
        protected double reach() {
            return reach + 3;
        }

        private Vec3 mouth() {
            Vec3 dir = Vec3.directionFromRotation(0, yaw);
            return new Vec3(at.x + dir.x * reach * 0.6, gy + 1.1, at.z + dir.z * reach * 0.6);
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age == 6) {
                Vec3 m = mouth();
                inkBurst(level, level.random, m, 22, 0.25, 0.2);
                MagicShake.kick(m, 0.4f, 6, reach + 6);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 10);
            Vec3 m = mouth(), dir = Vec3.directionFromRotation(0, yaw);
            double sx = -dir.z, sz = dir.x;
            // Open wide, then snap shut at tick 6 (a little bounce).
            float shut = t < 6 ? easeOut(t / 6f) : 1 - 0.08f * Mth.sin(Mth.clamp((t - 6) / 4f, 0, 1) * Mth.PI);
            float gap = 1.3f * (1 - shut) + 0.05f;
            float width = Math.min(2.6f, reach * 0.55f);
            d.darkDisc(m.x, m.y, m.z, width * 0.65f, GOO, 0.55f * a * (1 - shut * 0.6f));
            for (int row = 0; row < 2; row++) {
                double y = m.y + (row == 0 ? gap : -gap);
                // The gums: a dark-red arc behind each row.
                for (int i = 0; i < TEETH_N; i++) {
                    float k = i / (float) (TEETH_N - 1) - 0.5f;
                    float bow = 0.45f * (1 - 4 * k * k);
                    double x = m.x + sx * k * width + dir.x * bow, z = m.z + sz * k * width + dir.z * bow;
                    d.darkDisc(x, y + (row == 0 ? 0.12 : -0.12), z, 0.2f, GUM, 0.6f * a);
                    float len = 0.22f + 0.1f * (1 - Math.abs(k) * 2);
                    d.crystal(x, y + (row == 0 ? -len * 0.5 : len * 0.5), z, 0, row == 0 ? -1 : 1, 0, 0.07f, len, TEETH, a, false);
                }
            }
            if (t >= 6 && t < 10) d.glowDisc(m.x, m.y, m.z, 1.2f, 0xFFFFFF, 0.35f * (1 - (t - 6) / 4f));
        }
    }

    // --- the swallow -----------------------------------------------------------------------------------------------------------

    /** A dark vortex drawn into his maw (held), or a gout of goo spat out. */
    static final class Swallow extends MagicEffect {
        private final int dick;
        private final boolean spat;

        Swallow(int dick, boolean spat, Vec3 maw, int ticks) {
            super(maw, School.NONE, spat ? 18 : Math.max(10, ticks));
            this.dick = dick;
            this.spat = spat;
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(dick);
            return e != null && !e.isRemoved() ? e.getEyePosition() : at;
        }

        private Vec3 facing() {
            Entity e = entity(dick);
            float yaw = e instanceof LivingEntity l ? l.yHeadRot : e != null ? e.getYRot() : 0;
            return Vec3.directionFromRotation(0, yaw);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 m = anchor(), f = facing();
            if (spat) {
                if (age == 0) {
                    for (int i = FxParticles.n(30); i > 0; i--) {
                        FxParticles.plain(level, i % 3 == 0 ? ParticleTypes.SQUID_INK : GOO_DUST, m.x + f.x * 0.4, m.y, m.z + f.z * 0.4,
                                f.x * (0.3 + r.nextDouble() * 0.4) + FxParticles.j(r) * 0.12, 0.05 + r.nextDouble() * 0.15,
                                f.z * (0.3 + r.nextDouble() * 0.4) + FxParticles.j(r) * 0.12);
                    }
                }
            } else if (age % 2 == 0) {
                // Goo being slurped in.
                float a = r.nextFloat() * Mth.TWO_PI;
                double x = m.x + Mth.cos(a) * 1.4, z = m.z + Mth.sin(a) * 1.4, y = m.y - 0.6 + r.nextDouble();
                FxParticles.plain(level, GOO_DUST, x, y, z, (m.x - x) * 0.18, (m.y - y) * 0.18, (m.z - z) * 0.18);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 m = anchor(), f = facing();
            if (spat) {
                float k = easeOut(t / 10f), a = fade(t, 1, 10);
                for (int i = 0; i < 6; i++) {
                    float s = k * (0.6f + i * 0.4f);
                    blob(d, m.x + f.x * s, m.y - 0.25 * s * s * 0.3, m.z + f.z * s, 0.45f - i * 0.05f, a);
                }
                return;
            }
            float a = fade(t, 4, 6);
            // The vortex: dark wisps spiralling in round the maw.
            for (int i = 0; i < 10; i++) {
                float k = ((t * 0.07f + i / 10f) % 1f), ang = i * 2.4f + t * 0.3f + k * 4;
                float rr = 1.6f * (1 - k);
                d.darkDisc(m.x + Mth.cos(ang) * rr, m.y + (i % 3 - 1) * 0.3 * (1 - k), m.z + Mth.sin(ang) * rr, 0.35f + 0.3f * (1 - k), GOO, 0.5f * a * Mth.sin(Mth.PI * k));
            }
            d.darkDisc(m.x, m.y, m.z, 0.8f, GOO_DEEP, 0.7f * a);
            d.glowDisc(m.x, m.y + 0.2, m.z, 0.5f, OIL, 0.15f * a);
        }
    }

    // --- the goo splash --------------------------------------------------------------------------------------------------------

    /** Goo hurled in an arc onto a spot, leaving a glossy puddle that fades (the server's goo blocks lie under it). */
    static final class GooSplash extends MagicEffect {
        private static final int LINGER = 50, BLOBS = 5;
        private final Vec3 from;
        private final float radius;
        private final int flight;
        private final double gy;
        private final long seed;

        GooSplash(Vec3 from, Vec3 land, float radius, int ticks) {
            super(land, School.NONE, Math.max(6, ticks) + LINGER);
            this.from = from;
            this.radius = Math.max(0.8f, radius);
            this.flight = Math.max(4, ticks);
            this.gy = groundY(land);
            this.seed = (long) (land.x * 31 + land.z * 17);
        }

        @Override
        protected double reach() {
            return from.distanceTo(at) + radius + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private Vec3 arc(float k, int i) {
            float off = (hash(seed, i) - 0.5f) * radius, offz = (hash(seed, i + 7) - 0.5f) * radius;
            double x = Mth.lerp(k, from.x, at.x + off), z = Mth.lerp(k, from.z, at.z + offz);
            double y = Mth.lerp(k, from.y, gy) + 4 * k * (1 - k) * (2 + from.distanceTo(at) * 0.15);
            return new Vec3(x, y, z);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age < flight) {
                Vec3 p = arc(age / (float) flight, r.nextInt(BLOBS));
                FxParticles.plain(level, GOO_DUST, p.x, p.y, p.z, 0, -0.05, 0);
            }
            if (age == flight) {
                MagicShake.kick(at, 0.25f, 5, radius + 6);
                for (int i = FxParticles.n(24); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, dd = radius * r.nextFloat();
                    FxParticles.plain(level, i % 2 == 0 ? ParticleTypes.SQUID_INK : GOO_DUST, at.x + Mth.cos(a) * dd, gy + 0.2, at.z + Mth.sin(a) * dd,
                            Mth.cos(a) * 0.15, 0.15 + r.nextDouble() * 0.2, Mth.sin(a) * 0.15);
                }
            }
            if (age > flight && r.nextFloat() < 0.25f) {
                float a = r.nextFloat() * Mth.TWO_PI, dd = radius * 0.8f * r.nextFloat();
                FxParticles.plain(level, ParticleTypes.BUBBLE_POP, at.x + Mth.cos(a) * dd, gy + 0.1, at.z + Mth.sin(a) * dd, 0, 0.02, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            if (t < flight) {
                float k = t / flight;
                for (int i = 0; i < BLOBS; i++) {
                    float ki = Mth.clamp(k * (1.0f + 0.12f * i) - 0.05f * i, 0, 1);
                    Vec3 p = arc(ki, i);
                    blob(d, p.x, p.y, p.z, 0.32f - i * 0.03f, 1);
                    Vec3 q = arc(Math.max(0, ki - 0.08f), i);
                    blob(d, q.x, q.y, q.z, 0.18f, 0.6f);
                }
                return;
            }
            float since = t - flight, a = fade(t, 0, LINGER * 0.7f);
            float spread = easeOutBack(since / 5f);
            puddle(d, at.x, gy + 0.03, at.z, radius * spread, seed, t, a);
            if (since < 6) d.ring(at.x, gy + 0.1, at.z, 0, 1, 0, radius * spread * 0.8f, radius * spread * 1.15f, OIL_PALE, 0.35f * (1 - since / 6f), true);
        }
    }

    // --- the copy --------------------------------------------------------------------------------------------------------------

    /** A shimmer of oil sheen rippling up a copy (Dick himself in a hunter's skin, or one of his doubles). */
    static final class Copy extends MagicEffect {
        private final int who;
        private final boolean self;

        Copy(int who, boolean self, Vec3 at, int ticks) {
            super(at, School.NONE, Math.max(10, ticks));
            this.who = who;
            this.self = self;
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(who);
            return e != null && !e.isRemoved() ? e.position() : at;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 p = anchor();
            if (age == 0) {
                if (self) level.playLocalSound(p.x, p.y, p.z, AllSounds.dick("copy"), SoundSource.HOSTILE, 1.2f, 1f, false);
                inkBurst(level, r, p.add(0, 1, 0), 14, 0.15, 0.1);
            }
            if (r.nextFloat() < 0.4f) {
                FxParticles.plain(level, ParticleTypes.END_ROD, p.x + FxParticles.j(r) * 0.5, p.y + r.nextDouble() * 1.9,
                        p.z + FxParticles.j(r) * 0.5, 0, 0.01, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 3, 10);
            Entity e = entity(who);
            Vec3 p = e != null && !e.isRemoved() ? e.getPosition(d.partial) : at;
            float h = e != null ? e.getBbHeight() : 1.9f;
            // Bands of sheen rolling up the body, the copy's colours settling under them.
            for (int i = 0; i < 4; i++) {
                float k = ((t * 0.05f + i / 4f) % 1f);
                double y = p.y + k * h;
                int c = i % 2 == 0 ? OIL_PALE : 0xC8A8FF;
                d.ring(p.x, y, p.z, 0, 1, 0, 0.35f, 0.55f, c, 0.5f * a * Mth.sin(Mth.PI * k), true);
            }
            d.glowDisc(p.x, p.y + h * 0.5, p.z, h * 0.6f, OIL, 0.18f * a * (0.7f + 0.3f * Mth.sin(t * 0.8f)));
            if (t < 6) d.glowDisc(p.x, p.y + h * 0.5, p.z, h * 0.8f, 0xFFFFFF, 0.4f * (1 - t / 6f));
        }
    }

    // --- exposed ---------------------------------------------------------------------------------------------------------------

    /** His copies burst and he is found out: goo bursting off him, a white flash, then a daze over his head. */
    static final class Exposed extends MagicEffect {
        private final int dick, doubles;

        Exposed(int dick, int doubles, Vec3 at, int ticks) {
            super(at, School.NONE, Math.max(20, ticks));
            this.dick = dick;
            this.doubles = Math.max(0, doubles);
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(dick);
            return e != null && !e.isRemoved() ? e.position() : at;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 p = anchor();
            if (age == 0) {
                inkBurst(level, r, p.add(0, 1.1, 0), 30 + doubles * 10, 0.35, 0.3);
                for (int i = FxParticles.n(16); i > 0; i--) FxParticles.salt(level, BORAX, 1f, 0, p.x, p.y + 1.1, p.z,
                        FxParticles.j(r) * 0.3, 0.1 + r.nextDouble() * 0.2, FxParticles.j(r) * 0.3);
                MagicOverlay.flash(p, 0xFFFFFF, 0.4f, 24);
                MagicShake.kick(p, 0.5f, 8, 20);
            }
            if (r.nextFloat() < 0.4f) FxParticles.plain(level, GOO_DUST, p.x + FxParticles.j(r) * 0.4, p.y + 1.2 + r.nextDouble() * 0.6,
                    p.z + FxParticles.j(r) * 0.4, 0, -0.08, 0);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 12);
            Entity e = entity(dick);
            Vec3 p = e != null && !e.isRemoved() ? e.getPosition(d.partial) : at;
            float h = e != null ? e.getBbHeight() : 2f;
            if (t < 10) {
                float k = easeOut(t / 10f);
                d.glowDisc(p.x, p.y + h * 0.55, p.z, 1.5f + k, 0xFFFFFF, 0.6f * (1 - k));
                d.groundRing(p.x, p.y + 0.05, p.z, 0.5f + 3 * k, 0.9f + 3.4f * k, OIL_PALE, 0.5f * (1 - k), true);
            }
            // The daze: sparks circling over his head.
            for (int i = 0; i < 5; i++) {
                float ang = t * 0.25f + i * Mth.TWO_PI / 5;
                d.star(p.x + Mth.cos(ang) * 0.45, p.y + h + 0.25 + 0.05 * Mth.sin(t * 0.5f + i), p.z + Mth.sin(ang) * 0.45, 0.14f, t * 0.2f,
                        i % 2 == 0 ? 0xFFFFFF : OIL_PALE, 0.8f * a);
            }
            // The goo sagging off him.
            d.darkRing(p.x, p.y + 0.03, p.z, 0, 1.1f, GOO, 0.55f * a);
        }
    }

    // --- the tentacle ----------------------------------------------------------------------------------------------------------

    /** A goo tentacle rising out of the floor and slamming down the lane, then sinking back into it. */
    static final class Tentacle extends MagicEffect {
        private static final int SEGS = 18, RISE = 5, SLAM = 4, SINK = 10;
        private final float yaw, reach;
        private final double gy;
        private final int hold;

        Tentacle(Vec3 from, float yaw, float reach, int ticks) {
            super(from, School.NONE, RISE + SLAM + Math.max(4, ticks) + SINK);
            this.yaw = yaw;
            this.reach = Math.max(2, reach);
            this.gy = groundY(from);
            this.hold = Math.max(4, ticks);
        }

        @Override
        protected double reach() {
            return reach + 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        /** The tentacle's spine at time {@code t}: SEGS + 1 points from its root. */
        private Vec3[] spine(float t) {
            Vec3 dir = Vec3.directionFromRotation(0, yaw);
            float swing, length;
            if (t < RISE) {
                swing = -0.15f * easeOut(t / RISE);
                length = 4.5f * easeOut(t / RISE);
            } else if (t < RISE + SLAM) {
                float k = (t - RISE) / SLAM;
                swing = -0.15f + 1.15f * k * k;
                length = Mth.lerp(k, 4.5f, reach);
            } else {
                swing = 1;
                float sink = Mth.clamp((t - (life - SINK)) / SINK, 0, 1);
                length = reach * (1 - sink * 0.85f);
            }
            Vec3[] pts = new Vec3[SEGS + 1];
            Vec3 p = new Vec3(at.x, gy, at.z);
            pts[0] = p;
            float ds = length / SEGS;
            for (int i = 1; i <= SEGS; i++) {
                float s = i / (float) SEGS;
                // Bent from upright toward the lane, the tip leading; lying flat it ripples along the floor.
                float bend = Mth.clamp(swing * (0.75f + 0.5f * s), -0.3f, 1) * Mth.HALF_PI;
                double up = Mth.cos(bend), fw = Mth.sin(bend);
                float wig = 0.18f * Mth.sin(t * 0.6f + s * 8) * s;
                p = p.add(dir.x * fw * ds - dir.z * wig * ds, up * ds, dir.z * fw * ds + dir.x * wig * ds);
                if (swing >= 1) p = new Vec3(p.x, Math.max(gy + 0.25 + 0.1 * Mth.sin(t * 0.5f + s * 10), Math.min(p.y, gy + 0.6)), p.z);
                pts[i] = p;
            }
            return pts;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                inkBurst(level, r, new Vec3(at.x, gy + 0.2, at.z), 16, 0.2, 0.3);
            }
            if (age == RISE + SLAM) {
                Vec3[] pts = spine(age);
                MagicShake.kick(pts[SEGS], 0.45f, 8, reach + 6);
                for (int i = 2; i <= SEGS; i += 2) {
                    Vec3 p = pts[i];
                    BlockState s = ground(level, p.x, gy, p.z);
                    for (int j = FxParticles.n(3); j > 0; j--) {
                        FxParticles.plain(level, j % 2 == 0 ? new BlockParticleOption(ParticleTypes.BLOCK, s) : GOO_DUST, p.x + FxParticles.j(r) * 0.5,
                                gy + 0.2, p.z + FxParticles.j(r) * 0.5, FxParticles.j(r) * 0.2, 0.25 + r.nextDouble() * 0.2, FxParticles.j(r) * 0.2);
                    }
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, SINK * 0.6f);
            Vec3[] pts = spine(t);
            // The root's black pool.
            d.darkRing(at.x, gy + 0.03, at.z, 0, 1.3f, GOO, 0.75f * a);
            for (int i = 0; i < SEGS; i++) {
                Vec3 p0 = pts[i], p1 = pts[i + 1];
                float s = i / (float) SEGS;
                float thick = Mth.lerp(s, 0.55f, 0.1f);
                Vec3 mid = p0.lerp(p1, 0.5);
                float dx = (float) (p1.x - p0.x), dy = (float) (p1.y - p0.y), dz = (float) (p1.z - p0.z);
                float seg = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                d.crystal(mid.x, mid.y, mid.z, dx, dy, dz, thick, seg * 0.85f + thick * 0.4f, GOO, a, false);
                // Its glossy back.
                d.glowDisc(mid.x, mid.y + thick * 0.6, mid.z, thick * 0.55f, OIL, 0.2f * a);
            }
            // The slam's trace along the lane.
            float hit = Mth.clamp(1 - (t - RISE - SLAM) / 10f, 0, 1);
            if (t >= RISE + SLAM && hit > 0) {
                Vec3 tip = pts[SEGS];
                d.groundLine(at.x, at.z, tip.x, tip.z, gy + 0.05, 2.2f, OIL_PALE, 0.35f * hit);
            }
        }
    }

    // --- the surge -------------------------------------------------------------------------------------------------------------

    /** The goo gathering at the rim through the windup, then a wave of it rolling in over the whole floor. */
    static final class Surge extends MagicEffect {
        private static final int ROLL = 16;
        private final float radius;
        private final int windup;
        private final double gy;

        Surge(Vec3 centre, float radius, int windup) {
            super(centre, School.NONE, Math.max(10, windup) + ROLL + 8);
            this.radius = Math.max(4, radius);
            this.windup = Math.max(10, windup);
            this.gy = centre.y;
        }

        @Override
        protected double reach() {
            return radius + 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        /** The wave's front (radius from the centre), once it rolls. */
        private float front(float t) {
            return radius * (1 - easeOut((t - windup) / ROLL));
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age < windup) {
                float k = age / (float) windup;
                for (int i = FxParticles.rate(r, 1 + 4 * k); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, rr = radius - 0.5f - r.nextFloat() * 2;
                    FxParticles.plain(level, r.nextBoolean() ? ParticleTypes.BUBBLE_POP : GOO_DUST, at.x + Mth.cos(a) * rr, gy + 0.2,
                            at.z + Mth.sin(a) * rr, 0, 0.04 + 0.05 * k, 0);
                }
            } else if (age < windup + ROLL) {
                float f = front(age);
                for (int i = FxParticles.n(8); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.plain(level, i % 2 == 0 ? ParticleTypes.SQUID_INK : GOO_DUST, at.x + Mth.cos(a) * f, gy + 0.6 + r.nextDouble() * 0.6,
                            at.z + Mth.sin(a) * f, -Mth.cos(a) * 0.15, 0.08, -Mth.sin(a) * 0.15);
                }
                if (age == windup) MagicShake.kick(at, 0.5f, ROLL, radius + 8);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            if (t < windup) {
                float k = t / windup, a = fade(t, 6, 0);
                float band = 0.6f + 2.6f * k;
                d.darkRing(at.x, gy + 0.04, at.z, radius - band, radius + 0.3f, GOO, (0.35f + 0.5f * k) * a);
                // Bulges swelling along the rim.
                int n = 24;
                for (int i = 0; i < n; i++) {
                    float ang = i * Mth.TWO_PI / n + 0.3f * Mth.sin(t * 0.05f + i);
                    float swell = (0.3f + 0.6f * k) * (0.7f + 0.3f * Mth.sin(t * 0.3f + i * 1.7f));
                    blob(d, at.x + Mth.cos(ang) * (radius - band * 0.4f), gy + swell * 0.5, at.z + Mth.sin(ang) * (radius - band * 0.4f), swell, a);
                }
                return;
            }
            float since = t - windup, a = fade(t, 0, 8);
            float f = front(t);
            // Everything the wave has covered lies black; the crest rolls in, tall and glossy.
            d.darkRing(at.x, gy + 0.04, at.z, f, radius + 0.3f, GOO, 0.75f * a);
            float crest = since < ROLL ? 1.4f * Mth.sin(Mth.PI * Mth.clamp(since / ROLL, 0, 1)) + 0.4f : 0.3f;
            int n = Math.max(16, (int) (f * 4));
            for (int i = 0; i < n; i++) {
                float ang = i * Mth.TWO_PI / n;
                double x = at.x + Mth.cos(ang) * f, z = at.z + Mth.sin(ang) * f;
                blob(d, x, gy + crest * 0.5, z, crest * (0.75f + 0.25f * Mth.sin(i * 2.3f + t)), a);
            }
            d.ring(at.x, gy + crest + 0.1, at.z, 0, 1, 0, Math.max(0, f - 0.4f), f + 0.4f, OIL_PALE, 0.3f * a, true);
        }
    }

    // --- borax -----------------------------------------------------------------------------------------------------------------

    /** Borax landing: a white burst and a ring of powder blown out, then fizzing ash and bubbles where it lies. */
    static final class Borax extends MagicEffect {
        private final float radius;
        private final int burned;
        private final double gy;

        Borax(Vec3 at, float radius, int burned, int ticks) {
            super(at, School.HUNTER, Math.max(10, ticks) + 16);
            this.radius = Math.max(0.6f, radius);
            this.burned = Math.max(0, burned);
            this.gy = groundY(at);
        }

        @Override
        protected double reach() {
            return radius + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                level.playLocalSound(at.x, at.y, at.z, AllSounds.leviathan("borax_fizz"), SoundSource.PLAYERS, 0.8f + 0.2f * Math.min(3, burned),
                        0.9f + r.nextFloat() * 0.2f, false);
                for (int i = FxParticles.n(30); i > 0; i--) {
                    FxParticles.salt(level, i % 4 == 0 ? 0xE6F2FF : BORAX, 1f, 0, at.x, gy + 0.3, at.z, FxParticles.j(r) * 0.35,
                            0.1 + r.nextDouble() * 0.25, FxParticles.j(r) * 0.35);
                }
                for (int i = FxParticles.n(5); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.POOF, at.x, gy + 0.3, at.z, FxParticles.j(r) * 0.1, 0.05, FxParticles.j(r) * 0.1);
                }
            }
            if (age < life - 8) {
                for (int i = FxParticles.rate(r, 1.2f + burned * 0.8f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, dd = radius * Mth.sqrt(r.nextFloat());
                    double x = at.x + Mth.cos(a) * dd, z = at.z + Mth.sin(a) * dd;
                    FxParticles.plain(level, i % 2 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.BUBBLE_POP, x, gy + 0.1, z, 0, 0.04, 0);
                }
                if (burned > 0 && r.nextFloat() < 0.4f) {
                    float a = r.nextFloat() * Mth.TWO_PI, dd = radius * r.nextFloat();
                    FxParticles.plain(level, ParticleTypes.WHITE_SMOKE, at.x + Mth.cos(a) * dd, gy + 0.2, at.z + Mth.sin(a) * dd, 0, 0.05, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 12), k = easeOut(t / 8f);
            d.glowDisc(at.x, gy + 0.5, at.z, 1.4f, 0xFFFFFF, 0.6f * (1 - Mth.clamp(t / 6f, 0, 1)));
            d.groundRing(at.x, gy + 0.05, at.z, radius * 0.4f * k, radius * (0.6f + 0.6f * k), BORAX, 0.55f * (1 - k * 0.6f) * a, true);
            // The powder lying white over the spot, sparkling as it fizzes.
            d.flatGlow(at.x, gy + 0.04, at.z, radius * 1.1f, 0xF0F6FF, 0.35f * a);
            for (int i = 0; i < 10; i++) {
                float ang = hash((long) (at.x * 7 + at.z), i) * Mth.TWO_PI, dd = radius * hash((long) (at.z * 5 + at.x), i);
                float flick = Mth.clamp(Mth.sin(t * 1.3f + i * 2.1f), 0, 1);
                d.glowDisc(at.x + Mth.cos(ang) * dd, gy + 0.12, at.z + Mth.sin(ang) * dd, 0.16f, 0xFFFFFF, 0.6f * a * flick);
            }
        }
    }

    // --- decapitation ----------------------------------------------------------------------------------------------------------

    /** A black spurt pumping out of a Leviathan's severed neck. */
    static final class Decapitate extends MagicEffect {
        private final int body;

        Decapitate(int body, Vec3 neck, int ticks) {
            super(neck, School.NONE, Math.max(10, ticks));
            this.body = body;
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(body);
            return e != null && !e.isRemoved() ? new Vec3(e.getX(), e.getEyeY() - 0.3, e.getZ()) : at;
        }

        private float pump(float t) {
            return (1 - t / life) * (0.55f + 0.45f * Mth.sin(t * 0.9f));
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 n = anchor();
            for (int i = FxParticles.rate(r, 4 * pump(age)); i > 0; i--) {
                FxParticles.plain(level, i % 3 == 0 ? ParticleTypes.SQUID_INK : GOO_DUST, n.x, n.y, n.z, FxParticles.j(r) * 0.12,
                        0.25 + r.nextDouble() * 0.2, FxParticles.j(r) * 0.12);
            }
            if (r.nextFloat() < 0.3f) FxParticles.plain(level, GOO_DUST, n.x + FxParticles.j(r) * 0.3, n.y, n.z + FxParticles.j(r) * 0.3,
                    0, -0.1, 0);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Entity e = entity(body);
            Vec3 n = e != null && !e.isRemoved() ? new Vec3(Mth.lerp(d.partial, e.xo, e.getX()), Mth.lerp(d.partial, e.yo, e.getY()) + e.getEyeHeight() - 0.3,
                    Mth.lerp(d.partial, e.zo, e.getZ())) : at;
            float p = pump(t);
            // The jet: blobs along a fountain arc, shrinking as they rise and fall.
            for (int i = 0; i < 8; i++) {
                float k = ((t * 0.12f + i / 8f) % 1f);
                double h = 1.6 * p * 4 * k * (1 - k) + 0.1, sx = Mth.sin(i * 2.3f) * 0.35 * k, sz = Mth.cos(i * 1.7f) * 0.35 * k;
                blob(d, n.x + sx, n.y + h, n.z + sz, 0.16f + 0.14f * p * (1 - k), Math.min(1, p * 2) * (1 - k * 0.5f));
            }
            blob(d, n.x, n.y + 0.05, n.z, 0.22f, 1);
        }
    }

    // --- the reveal ------------------------------------------------------------------------------------------------------------

    /** A head splitting into the maw: a splash of black and a ring of teeth flowering open, then folding back. */
    static final class Reveal extends MagicEffect {
        private static final int TEETH_N = 12;
        private final int who;

        Reveal(int who, Vec3 head, int ticks) {
            super(head, School.NONE, Math.max(8, ticks) + 8);
            this.who = who;
        }

        @Override
        protected Vec3 anchor() {
            Entity e = entity(who);
            return e != null && !e.isRemoved() ? e.getEyePosition() : at;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age == 0) {
                Vec3 h = anchor();
                inkBurst(level, level.random, h, 16, 0.18, 0.15);
                MagicShake.kick(h, 0.2f, 5, 10);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = fade(t, 1, 8);
            Entity e = entity(who);
            Vec3 h = e != null && !e.isRemoved() ? e.getEyePosition(d.partial) : at;
            float yaw = e instanceof LivingEntity l ? l.yHeadRot : e != null ? e.getYRot() : 0;
            Vec3 fwd = Vec3.directionFromRotation(0, yaw);
            Vec3 c = h.add(fwd.scale(0.3));
            float open = easeOutBack(t / 6f) * (1 - Mth.clamp((t - (life - 8)) / 8f, 0, 1));
            // The maw's throat, black, and the petals of teeth round it, facing out of the face.
            d.darkDisc(c.x, c.y, c.z, 0.45f * open + 0.05f, GOO_DEEP, 0.8f * a);
            double sx = -fwd.z, sz = fwd.x;
            for (int i = 0; i < TEETH_N; i++) {
                float ang = i * Mth.TWO_PI / TEETH_N;
                float cx = Mth.cos(ang), cy = Mth.sin(ang);
                double r = 0.28 + 0.3 * open;
                double x = c.x + sx * cx * r, y = c.y + cy * r, z = c.z + sz * cx * r;
                // Teeth point inward toward the throat, and a little forward.
                float dx = (float) (-sx * cx + fwd.x * 0.6), dy = -cy, dz = (float) (-sz * cx + fwd.z * 0.6);
                d.crystal(x, y, z, dx, dy, dz, 0.04f, 0.12f * open + 0.02f, TEETH, a, false);
                d.darkDisc(x - fwd.x * 0.05, y, z - fwd.z * 0.05, 0.1f, GUM, 0.5f * a);
            }
            if (t < 5) d.glowDisc(c.x, c.y, c.z, 0.9f, OIL_PALE, 0.35f * (1 - t / 5f));
        }
    }
}
