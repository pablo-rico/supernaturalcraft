package org.papiricoh.supernaturalcraft.client.cain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxColor;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicTextures;
import org.papiricoh.supernaturalcraft.client.mark.MarkGlyph;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainBalance;
import org.papiricoh.supernaturalcraft.magic.school.School;

import java.util.ArrayList;
import java.util.List;

/**
 * Cain's fight in the world (v0.21), on the magic's effect kit ({@link MagicEffect}, {@link FxDraw}): each {@code CainFxPayload}
 * moment over its telegraph.
 * <ul>
 *   <li>{@link Swarm}: the bees rise out of the yellow circle into a buzzing golden-brown cloud, each bee its own dark body with a
 *   glint of gold and a blur of wings, the ground under them washed in honey light, and scatter upward as it ends;</li>
 *   <li>{@link Lunge}: a torn red streak down the lane and afterimages of him standing along it, fading from the start;</li>
 *   <li>{@link Furrow}: the plough's swing as an ember arc, a wave of earth thrown out across the cone, and a decal of torn
 *   furrows left glowing with the Mark's heat;</li>
 *   <li>{@link Spikes}: Abel's blood: a red flash racing out through the ring, dust where the spikes break the ground, a blood-red
 *   mist hanging over them, and blood welling at their feet;</li>
 *   <li>{@link Tools}: the farm tool itself, spinning down the violet lane in a trail of telekinesis, then stuck in the ground;</li>
 *   <li>{@link Execution}: his blink as a dark red smear from where he stood, a flash where he reappears and the blade's arc
 *   coming down on the spot;</li>
 *   <li>{@link Flare}: the Mark burnt into the ground under him, beating faster and brighter as it charges, the shelters (each
 *   its own {@code FLARE_SHELTER} payload) lit green, then a red shockwave and fire across the whole yard from its centre;</li>
 *   <li>{@link Offer}: the Mark hovering before each hunter, red-gold, a thread of it from his forearm; taken, it rushes into the
 *   hunter's arm.</li>
 * </ul>
 */
public final class CainWorldFx {

    static final int HONEY = 0xE8A93A, HONEY_DEEP = 0xB57414, BEE = 0x1A1206, BEE_GOLD = 0xF2C04A, EMBER = 0xFF5A2A,
            MARK_HOT = 0xFFC27A, BLOOD = FxColor.BLOOD, BLOOD_DARK = 0x4A0606, EARTH = 0x5E4128, EARTH_DARK = 0x24170C,
            VIOLET = 0xB36BFF, SAFE = 0x6CFF8E;

    /** The offers showing now, by hunter (a taken offer ends the one before it). */
    private static final List<Offer> OFFERS = new ArrayList<>();
    /** The flares burning now (their shelters arrive in payloads of their own). */
    private static final List<Flare> FLARES = new ArrayList<>();

    private CainWorldFx() {
    }

    // --- starting them (ClientCainFx and the previews) -----------------------------------------------------------------------

    public static void swarm(Vec3 at, float radius, int ticks) {
        MagicFxManager.add(new Swarm(at, radius, ticks));
    }

    public static void lunge(int cain, int dash, Vec3 end, int ticks) {
        Entity e = entity(cain);
        Vec3 from = e != null ? e.position() : end;
        if (from.distanceToSqr(end) < 1) {
            // He is already there (a late packet): the lane behind him, along his facing.
            float yaw = e instanceof LivingEntity l ? l.yBodyRot : e != null ? e.getYRot() : 0;
            from = end.subtract(Vec3.directionFromRotation(0, yaw).scale(CainBalance.LUNGE_REACH * 0.8));
        }
        MagicFxManager.add(new Lunge(from, end, dash, ticks));
    }

    public static void furrow(Vec3 apex, float yaw, float reach, int ticks) {
        MagicFxManager.add(new Furrow(apex, yaw, reach, ticks));
    }

    public static void spikes(Vec3 centre, float outer, float inner, int ticks) {
        MagicFxManager.add(new Spikes(centre, outer, inner, ticks));
    }

    public static void tools(int cain, int tool, Vec3 land, int flight) {
        Entity e = entity(cain);
        Vec3 from;
        if (e != null) {
            float yaw = (e instanceof LivingEntity l ? l.yBodyRot : e.getYRot()) * Mth.DEG_TO_RAD;
            // His right hand, raised: right of his facing is (-cos yaw, -sin yaw).
            from = e.position().add(-Mth.cos(yaw) * 0.45, 1.75, -Mth.sin(yaw) * 0.45);
        } else {
            from = land.add(0, 1.6, 0);
        }
        MagicFxManager.add(new Tools(from, land, tool, flight));
    }

    public static void execution(int cain, int victim, Vec3 behind, int ticks) {
        Entity e = entity(cain);
        Vec3 from = e != null ? e.position() : behind;
        MagicFxManager.add(new Execution(from, behind, victim, ticks));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == victim) {
            MagicOverlay.flash(mc.player.position(), 0x8A0A10, 0.45f, 8);
            MagicShake.kick(mc.player.position(), 0.6f, 8, 8);
        }
    }

    /** The Mark's flare over the yard centred on {@code centre}; its shelters follow, one {@link #shelter} each. */
    public static void flare(int cain, Vec3 centre, float radius, int windup) {
        Flare f = new Flare(cain, centre, radius, windup);
        FLARES.removeIf(Flare::isDone);
        FLARES.add(f);
        MagicFxManager.add(f);
    }

    /** One green shelter of the latest flare of {@code cain} (sent right after it). */
    public static void shelter(int cain, Vec3 at, float radius) {
        for (int i = FLARES.size() - 1; i >= 0; i--) {
            Flare f = FLARES.get(i);
            if (f.cain == cain && !f.isDone()) {
                f.shelter(at, radius);
                return;
            }
        }
    }

    public static void offer(int cain, int hunter, boolean taken, Vec3 at, int ticks) {
        Offer before = null;
        for (Offer o : OFFERS) if (o.hunter == hunter && !o.taken) before = o;
        if (taken && before != null) before.close();
        OFFERS.removeIf(o -> o.done() || o.closing);
        Offer o = new Offer(cain, hunter, taken, at, ticks, before != null ? before.sigil(1f) : null);
        OFFERS.add(o);
        MagicFxManager.add(o);
        Minecraft mc = Minecraft.getInstance();
        if (taken && mc.player != null && mc.player.getId() == hunter) MagicOverlay.throb(1f);
    }

    static void clear() {
        OFFERS.clear();
        FLARES.clear();
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
        return s.isAir() ? Blocks.DIRT.defaultBlockState() : s;
    }

    /** A small cheap random for decals fixed by a seed. */
    private static float hash(long seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** The Mark laid flat on the ground at {@code c}, its top toward {@code yaw}, {@code size} blocks tall. */
    static void groundGlyph(FxDraw d, Vec3 c, double y, float yaw, float size, float fraction, float width, int rgb, float alpha) {
        float fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        // The rune's x runs to the right of the facing, (-fz, fx), as read from its foot.
        float rx = -fz, rz = fx;
        float w = size * MarkGlyph.ASPECT;
        MarkGlyph.walk(fraction, (x0, y0, x1, y1) -> {
            double ax = c.x + rx * (x0 - 0.5f) * w + fx * (0.5f - y0) * size, az = c.z + rz * (x0 - 0.5f) * w + fz * (0.5f - y0) * size;
            double bx = c.x + rx * (x1 - 0.5f) * w + fx * (0.5f - y1) * size, bz = c.z + rz * (x1 - 0.5f) * w + fz * (0.5f - y1) * size;
            d.groundLine(ax, az, bx, bz, y, width, rgb, alpha);
        });
    }

    /** The Mark standing in the air at {@code c} facing the camera, {@code size} blocks tall. */
    static void airGlyph(FxDraw d, Vec3 c, float size, float fraction, float width, int rgb, float alpha, boolean core) {
        Vector3f l = d.camera.getLeftVector(), u = d.camera.getUpVector();
        float w = size * MarkGlyph.ASPECT;
        MarkGlyph.walk(fraction, (x0, y0, x1, y1) -> {
            float ax = -(x0 - 0.5f) * w, ay = (0.5f - y0) * size, bx = -(x1 - 0.5f) * w, by = (0.5f - y1) * size;
            d.softBeam(c.x + l.x * ax + u.x * ay, c.y + l.y * ax + u.y * ay, c.z + l.z * ax + u.z * ay,
                    c.x + l.x * bx + u.x * by, c.y + l.y * bx + u.y * by, c.z + l.z * bx + u.z * by, width, rgb, alpha, alpha, core);
        });
    }

    // --- the swarm -------------------------------------------------------------------------------------------------------------

    /** The bees over a yellow circle. */
    static final class Swarm extends MagicEffect {
        private static final int BEES = 110;
        private final float radius;
        private final double gy;
        private final float[] ang, rad, hgt, spd, ph;

        Swarm(Vec3 at, float radius, int ticks) {
            super(at, School.INFERNAL, Math.max(20, ticks) + 14);
            this.radius = Math.max(1, radius);
            this.gy = groundY(at);
            RandomSource r = RandomSource.create((long) (at.x * 31 + at.z * 17));
            ang = new float[BEES];
            rad = new float[BEES];
            hgt = new float[BEES];
            spd = new float[BEES];
            ph = new float[BEES];
            for (int i = 0; i < BEES; i++) {
                ang[i] = r.nextFloat() * Mth.TWO_PI;
                rad[i] = Mth.sqrt(r.nextFloat());
                hgt[i] = r.nextFloat();
                spd[i] = (0.09f + r.nextFloat() * 0.16f) * (r.nextBoolean() ? 1 : -1);
                ph[i] = r.nextFloat() * 100;
            }
        }

        @Override
        protected double reach() {
            return radius + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        /** Where bee {@code i} is at time {@code t}. */
        private Vec3 bee(int i, float t) {
            float rise = easeOut(t / 12f), left = life - t, out = Mth.clamp(1 - left / 14f, 0, 1);
            float a = ang[i] + t * spd[i] + 0.4f * Mth.sin(t * 0.13f + ph[i]);
            float r = radius * (0.15f + 0.85f * rad[i]) * (0.85f + 0.15f * Mth.sin(t * 0.21f + ph[i])) * (0.2f + 0.8f * rise) * (1 + out * 0.8f);
            double y = gy + (0.25 + hgt[i] * 2.1 + 0.25 * Mth.sin(t * 0.37f + ph[i] * 1.7f)) * rise + out * out * 6 * (0.5 + hgt[i]);
            // The buzz: a quick jitter on top of the drift.
            float jx = 0.07f * Mth.sin(t * 2.3f + ph[i] * 3), jz = 0.07f * Mth.cos(t * 2.9f + ph[i] * 5), jy = 0.05f * Mth.sin(t * 3.7f + ph[i]);
            return new Vec3(at.x + Mth.cos(a) * r + jx, y + jy, at.z + Mth.sin(a) * r + jz);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                for (int i = FxParticles.n(14); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, d = radius * Mth.sqrt(r.nextFloat());
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, at.x, gy, at.z)),
                            at.x + Mth.cos(a) * d, gy + 0.1, at.z + Mth.sin(a) * d, 0, 0.15, 0);
                }
            }
            if (age < life - 10) {
                if (r.nextFloat() < 0.25f) {
                    float a = r.nextFloat() * Mth.TWO_PI, d = radius * r.nextFloat();
                    FxParticles.plain(level, ParticleTypes.FALLING_HONEY, at.x + Mth.cos(a) * d, gy + 1.8 + r.nextDouble(), at.z + Mth.sin(a) * d,
                            0, 0, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = fade(t, 8, 14);
            float pulse = 0.8f + 0.2f * Mth.sin(t * 0.5f);
            // The honey light on the ground and its comb-like rim.
            d.flatGlow(at.x, gy + 0.04, at.z, radius * 1.15f, HONEY, 0.32f * a * pulse);
            d.spikedRing(at.x, gy + 0.05, at.z, radius - 0.1f, 0.22f, 12, t * 0.02f, BEE_GOLD, 0.45f * a);
            // The cloud's body: a faint brown haze swirling at chest height (the bees themselves make the cloud).
            for (int k = 0; k < 5; k++) {
                float ang = t * 0.05f + k * Mth.TWO_PI / 5;
                double cx = at.x + Mth.cos(ang) * radius * 0.4, cz = at.z + Mth.sin(ang) * radius * 0.4;
                d.darkDisc(cx, gy + 1.1 + 0.3 * Mth.sin(t * 0.1f + k), cz, radius * 0.45f, 0x2B1D0A, 0.1f * a);
            }
            // Each bee: a small dark body (an elongated cut shape along its flight) with an amber band, a faint gold glint, and its
            // wings' flicker. Fewer at lower magic detail.
            int bees = switch (MagicFxManager.detail()) {
                case 0 -> BEES / 4;
                case 1 -> BEES / 2;
                default -> BEES;
            };
            for (int i = 0; i < bees; i++) {
                Vec3 b = bee(i, t), n = bee(i, t + 0.4f);
                float dx = (float) (n.x - b.x), dy = (float) (n.y - b.y), dz = (float) (n.z - b.z);
                if (dx * dx + dy * dy + dz * dz < 1e-6f) dx = 1;
                float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                double fx = dx / len, fy = dy / len, fz = dz / len;
                d.crystal(b.x, b.y, b.z, dx, dy, dz, 0.032f, 0.062f, BEE, a, false);
                d.crystal(b.x + fx * 0.012, b.y + fy * 0.012, b.z + fz * 0.012, dx, dy, dz, 0.035f, 0.022f, 0xB07A18, a, false);
                d.glowDisc(b.x, b.y + 0.02, b.z, 0.05f, BEE_GOLD, 0.28f * a);
                float wing = 0.5f + 0.5f * Mth.sin(t * 11f + ph[i] * 7);
                // The wings: two quick strokes up and out from its back, a blur that comes and goes.
                double sx = -fz, sz = fx;
                d.taper(b.x, b.y + 0.02, b.z, b.x + sx * 0.07, b.y + 0.06, b.z + sz * 0.07, 0.045f, 0.01f, 0xF4F0E6, 0.3f * wing * a, 0);
                d.taper(b.x, b.y + 0.02, b.z, b.x - sx * 0.07, b.y + 0.06, b.z - sz * 0.07, 0.045f, 0.01f, 0xF4F0E6, 0.3f * wing * a, 0);
            }
        }
    }

    // --- the lunge -------------------------------------------------------------------------------------------------------------

    /** One dash: a torn red streak and his afterimages down the lane. */
    static final class Lunge extends MagicEffect {
        private static final int GHOSTS = 6;
        private final Vec3 from;
        private final int dash;
        private final double gy0, gy1;

        Lunge(Vec3 from, Vec3 end, int dash, int ticks) {
            super(end, School.INFERNAL, Math.max(10, ticks) + 16);
            this.from = from;
            this.dash = dash;
            this.gy0 = groundY(from);
            this.gy1 = groundY(end);
        }

        @Override
        protected double reach() {
            return from.distanceTo(at) + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Math.min(gy0, gy1);
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age != 0) return;
            RandomSource r = level.random;
            for (int i = FxParticles.n(8); i > 0; i--) {
                FxParticles.plain(level, ParticleTypes.CLOUD, from.x + FxParticles.j(r) * 0.4, gy0 + 0.15, from.z + FxParticles.j(r) * 0.4,
                        FxParticles.j(r) * 0.05, 0.03, FxParticles.j(r) * 0.05);
            }
            Vec3 dir = at.subtract(from).multiply(1, 0, 1).normalize();
            BlockState s = ground(level, at.x, gy1, at.z);
            for (int i = FxParticles.n(16); i > 0; i--) {
                FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, s), at.x + FxParticles.j(r) * 0.6, gy1 + 0.1,
                        at.z + FxParticles.j(r) * 0.6, dir.x * 0.25 + FxParticles.j(r) * 0.1, 0.2 + r.nextDouble() * 0.2,
                        dir.z * 0.25 + FxParticles.j(r) * 0.1);
            }
            for (int i = FxParticles.n(12); i > 0; i--) {
                double k = r.nextDouble();
                Vec3 p = from.lerp(at, k);
                FxParticles.spark(level, i % 3 == 0 ? MARK_HOT : EMBER, 0.6f, 12, p.x, gy0 + (gy1 - gy0) * k + 0.3 + r.nextDouble() * 1.4, p.z,
                        dir.x * 0.08, 0.02, dir.z * 0.08);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 dir = at.subtract(from).multiply(1, 0, 1);
            double len = dir.length();
            if (len < 0.1) return;
            dir = dir.scale(1 / len);
            float hot = 0.85f + 0.15f * dash;
            // The torn streak along the ground, burning out from its start.
            float sa = fade(t, 1, life * 0.8f);
            d.groundLine(from.x, from.z, at.x, at.z, Math.max(gy0, gy1) + 0.04, 1.9f, BLOOD, 0.5f * sa * hot);
            d.groundLine(from.x, from.z, at.x, at.z, Math.max(gy0, gy1) + 0.05, 0.45f, EMBER, 0.75f * sa * hot);
            // The afterimages: a figure of red light standing at each step, the oldest (nearest the start) gone first.
            for (int i = 0; i < GHOSTS; i++) {
                float k = i / (float) (GHOSTS - 1);
                float ga = Mth.clamp(1 - t / (life * (0.25f + 0.65f * k)), 0, 1) * (0.35f + 0.65f * k);
                if (ga <= 0.01f) continue;
                Vec3 p = from.add(dir.scale(len * (0.08 + 0.84 * k)));
                double y = gy0 + (gy1 - gy0) * k;
                // Leaning into the dash.
                double lx = dir.x * 0.25, lz = dir.z * 0.25;
                d.darkDisc(p.x, y + 1.0, p.z, 0.65f, 0x1A0204, 0.3f * ga);
                d.softBeam(p.x - lx, y + 0.1, p.z - lz, p.x + lx, y + 1.45, p.z + lz, 0.78f, 0xB01010, 0.55f * ga, 0.75f * ga, false);
                d.glowDisc(p.x + lx * 1.2, y + 1.72, p.z + lz * 1.2, 0.34f, EMBER, 0.6f * ga);
            }
            // Speed lines through the air behind him.
            float la = Mth.clamp(1 - t / 9f, 0, 1);
            for (int i = 0; i < 5; i++) {
                double h = 0.35 + i * 0.36, side = (i - 2) * 0.16;
                Vec3 s = new Vec3(-dir.z * side, 0, dir.x * side);
                double lag = len * Mth.clamp(t / 7f, 0, 1);
                Vec3 a0 = from.add(dir.scale(lag)).add(s), a1 = at.add(s);
                d.taper(a0.x, gy0 + h, a0.z, a1.x, gy1 + h, a1.z, 0.02f, 0.09f, i % 2 == 0 ? EMBER : MARK_HOT, 0, 0.8f * la);
            }
            // Where he lands, the ground kicked up red.
            float land = Mth.clamp(1 - t / 8f, 0, 1);
            d.flatGlow(at.x, gy1 + 0.04, at.z, 1.6f, EMBER, 0.45f * land);
        }
    }

    // --- the furrow ------------------------------------------------------------------------------------------------------------

    /** The plough's swing ripping a cone. */
    static final class Furrow extends MagicEffect {
        private static final int LINES = 8, STEPS = 7;
        /** Length of one piece of a scar (blocks): short enough to follow each block top. */
        private static final double PIECE = 0.45;
        private final float yaw, reach;
        private final double gy;
        /** The torn furrows: per line, per step: x, y, z. */
        private double[][] furrows;

        Furrow(Vec3 apex, float yaw, float reach, int ticks) {
            super(apex, School.INFERNAL, Math.max(20, ticks) + 50);
            this.yaw = yaw;
            this.reach = Math.max(2, reach);
            this.gy = groundY(apex);
        }

        @Override
        protected double reach() {
            return reach + 2;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        /** Direction at {@code off} radians off the swing's middle. */
        private Vec3 dir(float off) {
            float a = yaw * Mth.DEG_TO_RAD + off;
            return new Vec3(-Mth.sin(a), 0, Mth.cos(a));
        }

        private float front(float t) {
            return reach * easeOut(t / 9f);
        }

        /**
         * The torn furrows: a few control points per line (a slightly wandering path out across the cone), then filled in every
         * {@link #PIECE} blocks with a sideways jitter and a ragged width, each point on the block top under it -- a flat decal that
         * hugs the ground, steps with it, and never floats.
         */
        private void lay(ClientLevel level) {
            long seed = (long) (at.x * 73856093) ^ (long) (at.z * 19349663) ^ (long) yaw;
            furrows = new double[LINES][];
            for (int l = 0; l < LINES; l++) {
                float off = (l / (float) (LINES - 1) - 0.5f) * Mth.PI * 0.62f + (hash(seed, l) - 0.5f) * 0.12f;
                float len = reach * (0.6f + 0.4f * hash(seed, l + 50));
                Vec3[] ctrl = new Vec3[STEPS + 1];
                ctrl[0] = at.add(dir(off).scale(0.8));
                for (int s = 0; s < STEPS; s++) {
                    float k = (s + 1) / (float) STEPS;
                    float wob = (hash(seed, l * 31 + s) - 0.5f) * 0.18f * k;
                    ctrl[s + 1] = at.add(dir(off + wob).scale(0.8 + len * k));
                }
                List<double[]> pts = new ArrayList<>();
                int n = 0;
                for (int s = 0; s < STEPS; s++) {
                    Vec3 p0 = ctrl[s], p1 = ctrl[s + 1];
                    Vec3 dd = p1.subtract(p0);
                    int pieces = Math.max(1, (int) Math.ceil(dd.horizontalDistance() / PIECE));
                    Vec3 side = new Vec3(-dd.z, 0, dd.x).normalize();
                    for (int i = 0; i < pieces; i++, n++) {
                        Vec3 p = p0.add(dd.scale(i / (float) pieces)).add(side.scale((hash(seed, l * 977 + n) - 0.5f) * 0.14f));
                        float k = (s + i / (float) pieces) / STEPS;
                        double w = 0.2 * (1 - 0.55 * k) * (0.65 + 0.7 * hash(seed, l * 409 + n));
                        pts.add(new double[]{p.x, FxWorld.floor(level, p.add(0, 1.5, 0), 4) + 0.025, p.z, w});
                    }
                }
                double[] flat = new double[pts.size() * 4];
                for (int i = 0; i < pts.size(); i++) System.arraycopy(pts.get(i), 0, flat, i * 4, 4);
                furrows[l] = flat;
            }
        }

        @Override
        protected void tick(ClientLevel level) {
            if (furrows == null) lay(level);
            RandomSource r = level.random;
            if (age == 0) MagicShake.kick(at, 0.4f, 8, 14);
            if (age < 10) {
                float f0 = front(age), f1 = front(age + 1);
                for (int i = FxParticles.n(16); i > 0; i--) {
                    float off = (r.nextFloat() - 0.5f) * Mth.PI * 0.66f;
                    double d = f0 + (f1 - f0) * r.nextDouble();
                    Vec3 o = dir(off);
                    Vec3 p = at.add(o.scale(d));
                    double y = FxWorld.floor(level, p.add(0, 1.5, 0), 4);
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, ground(level, p.x, y, p.z)), p.x, y + 0.15, p.z,
                            o.x * 0.25, 0.35 + r.nextDouble() * 0.35, o.z * 0.25);
                    if (i % 3 == 0) {
                        FxParticles.plain(level, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DIRT.defaultBlockState()),
                                p.x, y + 0.6 + r.nextDouble(), p.z, 0, 0, 0);
                    }
                }
            }
            // The Mark's heat breathing out of the furrows for a while.
            if (furrows != null && age < life - 20 && r.nextFloat() < 0.5f) {
                double[] line = furrows[r.nextInt(LINES)];
                int s = r.nextInt(line.length / 4);
                FxParticles.ember(level, EMBER, 0.5f, 18, line[s * 4], line[s * 4 + 1] + 0.05, line[s * 4 + 2], 0, 0.03, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            // The swing: an ember arc sweeping across the cone at knee height.
            if (t < 6) {
                float sweep = easeOut(t / 4f), aa = 1 - Mth.clamp((t - 3) / 3f, 0, 1);
                int n = 12;
                for (int i = 0; i < n; i++) {
                    float k0 = i / (float) n, k1 = (i + 1) / (float) n;
                    if (k1 > sweep) break;
                    Vec3 p0 = at.add(dir((k0 - 0.5f) * Mth.PI * 0.66f).scale(2.2)), p1 = at.add(dir((k1 - 0.5f) * Mth.PI * 0.66f).scale(2.2));
                    float fa = aa * (0.3f + 0.7f * k1 / Math.max(0.01f, sweep));
                    d.softBeam(p0.x, gy + 0.6, p0.z, p1.x, gy + 0.6, p1.z, 0.5f, EMBER, fa * 0.7f, fa * 0.8f, true);
                }
            }
            // The wave of earth thrown out across the cone.
            if (t < 18) {
                float f = front(t), wa = (1 - t / 18f);
                for (int i = 0; i < 9; i++) {
                    float off = (i / 8f - 0.5f) * Mth.PI * 0.62f;
                    Vec3 p = at.add(dir(off).scale(f));
                    float h = 0.4f + 0.5f * Mth.sin(t * 0.4f + i);
                    d.darkDisc(p.x, gy + h, p.z, 0.75f + 0.25f * (i % 3), EARTH, 0.45f * wa);
                }
            }
            // The torn furrows, dark with a red heat in them, fading at the end.
            if (furrows == null) return;
            float da = fade(t, 4, 24), heat = (0.6f + 0.4f * Mth.sin(t * 0.25f)) * fade(t, 6, 30);
            float grow = Mth.clamp(t / 9f, 0, 1);
            // Two passes, one buffer each: the painted scars first (one consumer, fetched here and used before anything else asks
            // the buffer source for another type), then the heat in them through FxDraw (which fetches its own). Each piece lies on
            // its own block top; a piece across a step is drawn as two halves, one on each side.
            var vc = org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw.quads(d.buffers);
            var m = d.pose.last().pose();
            Vec3 cam = d.eye();
            for (double[] line : furrows) {
                int n = line.length / 4, shown = Math.max(1, Math.round((n - 1) * grow));
                for (int i = 0; i < shown && i + 1 < n; i++) {
                    double w = (line[i * 4 + 3] + line[i * 4 + 7]) / 2;
                    piece(vc, m, cam, line, i, w * 1.7, 0.0, FxDraw.argb(0.4f * da, EARTH));
                    piece(vc, m, cam, line, i, w, 0.004, FxDraw.argb(0.85f * da, EARTH_DARK));
                }
            }
            for (double[] line : furrows) {
                int n = line.length / 4, shown = Math.max(1, Math.round((n - 1) * grow));
                for (int i = 0; i < shown && i + 1 < n; i++) {
                    double x0 = line[i * 4], y0 = line[i * 4 + 1], z0 = line[i * 4 + 2], x1 = line[i * 4 + 4], y1 = line[i * 4 + 5],
                            z1 = line[i * 4 + 6];
                    float w = (float) (line[i * 4 + 3] * 0.3);
                    if (Math.abs(y0 - y1) < 0.05) {
                        d.groundLine(x0, z0, x1, z1, y0 + 0.008, w, BLOOD, 0.8f * heat);
                    } else {
                        double mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
                        d.groundLine(x0, z0, mx, mz, y0 + 0.008, w, BLOOD, 0.8f * heat);
                        d.groundLine(mx, mz, x1, z1, y1 + 0.008, w, BLOOD, 0.8f * heat);
                    }
                }
            }
        }

        /** One flat piece of a scar from point {@code i} to the next, {@code w} wide, {@code lift} above its block tops. */
        private static void piece(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, Vec3 cam, double[] line, int i, double w,
                                  double lift, int argb) {
            double x0 = line[i * 4], y0 = line[i * 4 + 1], z0 = line[i * 4 + 2], x1 = line[i * 4 + 4], y1 = line[i * 4 + 5], z1 = line[i * 4 + 6];
            if (Math.abs(y0 - y1) < 0.05) {
                stroke(vc, m, cam, x0, z0, x1, z1, y0 + lift, w, argb);
            } else {
                double mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
                stroke(vc, m, cam, x0, z0, mx, mz, y0 + lift, w, argb);
                stroke(vc, m, cam, mx, mz, x1, z1, y1 + lift, w, argb);
            }
        }

        private static void stroke(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, Vec3 cam, double x0, double z0, double x1,
                                   double z1, double y, double w, int argb) {
            org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw.groundStroke(vc, m, (float) (x0 - cam.x), (float) (z0 - cam.z),
                    (float) (x1 - cam.x), (float) (z1 - cam.z), (float) (y - cam.y), (float) w, argb);
        }
    }

    // --- Abel's blood ----------------------------------------------------------------------------------------------------------

    /** The spikes' ring: flash, dust, blood mist and welling blood. */
    static final class Spikes extends MagicEffect {
        private final float outer, inner;
        private final double gy;
        private final List<double[]> spikes = new ArrayList<>();

        Spikes(Vec3 centre, float outer, float inner, int ticks) {
            super(centre, School.INFERNAL, Math.max(20, ticks) + 20);
            this.outer = Math.max(1, outer);
            this.inner = Mth.clamp(inner, 0, this.outer - 0.5f);
            this.gy = groundY(centre);
            // The server's spikes: every other block of the ring (CainAttacks.AbelsBlood.spikes).
            int r = Mth.ceil(this.outer), bx = Mth.floor(centre.x), bz = Mth.floor(centre.z);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.floorMod(dx + dz, 2) != 0) continue;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < this.inner || dist > this.outer) continue;
                    spikes.add(new double[]{bx + dx + 0.5, bz + dz + 0.5});
                }
            }
        }

        @Override
        protected double reach() {
            return outer + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                MagicShake.kick(at, 0.5f, 10, outer + 8);
                BlockState stone = Blocks.POINTED_DRIPSTONE.defaultBlockState(), earth = Blocks.ROOTED_DIRT.defaultBlockState();
                for (double[] s : spikes) {
                    for (int i = FxParticles.n(3); i > 0; i--) {
                        FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, i % 2 == 0 ? stone : earth), s[0] + FxParticles.j(r) * 0.3,
                                gy + 0.2, s[1] + FxParticles.j(r) * 0.3, FxParticles.j(r) * 0.12, 0.3 + r.nextDouble() * 0.3, FxParticles.j(r) * 0.12);
                    }
                    if (r.nextFloat() < 0.5f) {
                        FxParticles.plain(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, s[0], gy + 0.3, s[1], 0, 0.02, 0);
                    }
                }
            }
            if (age < life - 16 && !spikes.isEmpty()) {
                for (int i = FxParticles.rate(r, 2.5f); i > 0; i--) {
                    double[] s = spikes.get(r.nextInt(spikes.size()));
                    FxParticles.plain(level, new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.03f), 1.6f), s[0] + FxParticles.j(r) * 0.4,
                            gy + 0.2 + r.nextDouble() * 0.6, s[1] + FxParticles.j(r) * 0.4, 0, 0.02, 0);
                }
                if (r.nextFloat() < 0.6f) {
                    double[] s = spikes.get(r.nextInt(spikes.size()));
                    FxParticles.mote(level, BLOOD, 0.5f, 30, s[0], gy + 0.3, s[1], FxParticles.j(r) * 0.01, 0.025, FxParticles.j(r) * 0.01);
                }
                if (r.nextFloat() < 0.3f) {
                    double[] s = spikes.get(r.nextInt(spikes.size()));
                    FxParticles.plain(level, ParticleTypes.DRIPPING_DRIPSTONE_LAVA, s[0], gy + 0.9, s[1], 0, 0, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = fade(t, 3, 20);
            // The flash racing out through the ring as the spikes break the ground.
            if (t < 8) {
                float k = easeOut(t / 6f), r = inner + (outer - inner) * k;
                d.groundRing(at.x, gy + 0.05, at.z, Math.max(inner, r - 1.4f), r, EMBER, 0.85f * (1 - t / 8f), true);
                d.flatGlow(at.x, gy + 0.04, at.z, outer * 1.1f, BLOOD, 0.5f * (1 - t / 8f));
            }
            // The ground reddened between the radii.
            d.groundRing(at.x, gy + 0.03, at.z, inner, outer, BLOOD, 0.22f * a, true);
            // Blood welling at each spike's foot, beating.
            float beat = 0.7f + 0.3f * Mth.sin(t * 0.3f);
            for (int i = 0; i < spikes.size(); i++) {
                double[] s = spikes.get(i);
                d.flatGlow(s[0], gy + 0.05, s[1], 0.8f, BLOOD, 0.4f * a * (beat + 0.15f * Mth.sin(t * 0.5f + i)));
                if (t < 10) d.glowDisc(s[0], gy + 0.4 + t * 0.08, s[1], 0.45f, MARK_HOT, 0.4f * (1 - t / 10f));
            }
            // The blood-red mist hanging over the ring, drifting up.
            int n = 18;
            for (int i = 0; i < n; i++) {
                float ang = i * Mth.TWO_PI / n + t * 0.01f;
                float rr = (inner + outer) / 2 + (outer - inner) * 0.3f * Mth.sin(i * 1.7f);
                double y = gy + 0.35 + 0.012 * t + 0.15 * Mth.sin(t * 0.08f + i);
                d.darkDisc(at.x + Mth.cos(ang) * rr, y, at.z + Mth.sin(ang) * rr, 1.1f, BLOOD_DARK, 0.2f * a);
            }
            d.glowDisc(at.x, gy + 0.6, at.z, outer * 0.9f, BLOOD, 0.12f * a);
        }
    }

    // --- the thrown tools ------------------------------------------------------------------------------------------------------

    /** A farm tool hurled down a violet lane, then stuck in the ground. */
    static final class Tools extends MagicEffect {
        private static final int STUCK = 34;
        private final Vec3 from, land;
        private final int tool, flight;
        private final ItemStack stack;

        Tools(Vec3 from, Vec3 land, int tool, int flight) {
            super(land, School.INFERNAL, Math.max(2, flight) + STUCK);
            this.from = from;
            this.flight = Math.max(2, flight);
            this.tool = tool;
            this.land = new Vec3(land.x, groundY(land), land.z);
            this.stack = new ItemStack(switch (Math.floorMod(tool, 4)) {
                case 0 -> Items.TRIDENT;     // the pitchfork
                case 1 -> Items.IRON_HOE;    // the sickle
                case 2 -> Items.IRON_SHOVEL;
                default -> Items.IRON_AXE;
            });
        }

        @Override
        protected double reach() {
            return from.distanceTo(land) + 3;
        }

        @Override
        protected Vec3 anchor() {
            return land;
        }

        /** Where it is at time {@code t} (in flight: an arc from his hand). */
        private Vec3 pos(float t) {
            float k = Mth.clamp(t / flight, 0, 1);
            Vec3 p = from.lerp(land.add(0, 0.35, 0), k);
            return p.add(0, Mth.sin(Mth.PI * k) * 0.9, 0);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age <= flight) {
                Vec3 p = pos(age);
                for (int i = FxParticles.n(3); i > 0; i--) {
                    FxParticles.spark(level, i % 2 == 0 ? VIOLET : 0xE0C8FF, 0.5f, 10, p.x + FxParticles.j(r) * 0.2, p.y + FxParticles.j(r) * 0.2,
                            p.z + FxParticles.j(r) * 0.2, 0, 0.01, 0);
                }
            }
            if (age == flight) {
                BlockState s = ground(level, land.x, land.y, land.z);
                for (int i = FxParticles.n(14); i > 0; i--) {
                    FxParticles.plain(level, new BlockParticleOption(ParticleTypes.BLOCK, s), land.x + FxParticles.j(r) * 0.3, land.y + 0.1,
                            land.z + FxParticles.j(r) * 0.3, FxParticles.j(r) * 0.15, 0.25 + r.nextDouble() * 0.2, FxParticles.j(r) * 0.15);
                }
                for (int i = FxParticles.n(10); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, VIOLET, 0.6f, 14, land.x, land.y + 0.2, land.z, Mth.cos(a) * 0.2, 0.05, Mth.sin(a) * 0.2);
                }
            }
            if (age == life - 8) {
                for (int i = FxParticles.n(6); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.POOF, land.x + FxParticles.j(r) * 0.2, land.y + 0.4, land.z + FxParticles.j(r) * 0.2,
                            0, 0.03, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 dir = land.subtract(from).multiply(1, 0, 1);
            float yaw = (float) Mth.atan2(dir.z, dir.x);
            if (t <= flight) {
                Vec3 p = pos(t);
                // The telekinesis' trail: violet streaks behind it, a glow round it.
                for (int i = 1; i <= 4; i++) {
                    Vec3 q = pos(Math.max(0, t - i * 0.8f)), q0 = pos(Math.max(0, t - (i - 1) * 0.8f));
                    float ta = 0.75f * (1 - i / 5f);
                    d.taper(q.x, q.y, q.z, q0.x, q0.y, q0.z, 0.12f * (1 - i / 5f), 0.18f * (1 - (i - 1) / 5f), VIOLET, ta * 0.6f, ta);
                }
                d.glowDisc(p.x, p.y, p.z, 0.7f, VIOLET, 0.5f);
                d.glowDisc(p.x, p.y, p.z, 0.25f, 0xF0E4FF, 0.5f);
                item(d, p, yaw, t * 70f, 0);
            } else {
                float s = t - flight;
                float sink = Mth.clamp((s - (STUCK - 10)) / 10f, 0, 1);
                Vec3 p = land.add(0, 0.45 - sink * 0.9, 0);
                float aura = Mth.clamp(1 - s / 20f, 0, 1);
                d.glowDisc(p.x, p.y, p.z, 0.6f, VIOLET, 0.35f * aura);
                float k = easeOut(s / 8f);
                d.groundRing(land.x, land.y + 0.04, land.z, Math.max(0, 1.6f * k - 0.3f), 1.6f * k, VIOLET, 0.7f * (1 - k), true);
                item(d, p, yaw, 0, 35);
            }
        }

        /** The tool itself: spinning end over end along its flight, or stuck slanting into the ground. */
        private void item(FxDraw d, Vec3 p, float yaw, float spin, float tilt) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Vec3 cam = d.eye();
            PoseStack pose = d.pose;
            pose.pushPose();
            pose.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            // Along the flight: the item's flat face turned to the side, spinning about the side axis.
            pose.mulPose(Axis.YP.rotation(-yaw));
            if (tilt != 0) {
                pose.mulPose(Axis.ZP.rotationDegrees(-90 - tilt));
            } else {
                pose.mulPose(Axis.ZP.rotationDegrees(-spin));
            }
            pose.scale(1.25f, 1.25f, 1.25f);
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(p));
            mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, d.buffers, mc.level,
                    tool);
            pose.popPose();
        }
    }

    // --- the execution ---------------------------------------------------------------------------------------------------------

    /** His blink behind the hunter and the blade coming down. */
    static final class Execution extends MagicEffect {
        private final Vec3 from;
        private final int victim;
        private final double gy0, gy1;
        private Vec3 spot;

        Execution(Vec3 from, Vec3 behind, int victim, int ticks) {
            super(behind, School.INFERNAL, Math.max(16, ticks) + 10);
            this.from = from;
            this.victim = victim;
            this.gy0 = groundY(from);
            this.gy1 = groundY(behind);
            Entity v = entity(victim);
            this.spot = v != null ? v.position() : behind;
        }

        @Override
        protected double reach() {
            return from.distanceTo(at) + 4;
        }

        @Override
        protected double floor(ClientLevel level) {
            return Math.min(gy0, gy1);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Entity v = entity(victim);
            if (v != null && age < 4) spot = v.position();
            if (age == 0) {
                for (int i = FxParticles.n(14); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.SMOKE, from.x + FxParticles.j(r) * 0.3, gy0 + r.nextDouble() * 1.8,
                            from.z + FxParticles.j(r) * 0.3, 0, 0.02, 0);
                }
                for (int i = FxParticles.n(16); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.spark(level, i % 3 == 0 ? MARK_HOT : BLOOD, 0.7f, 12, at.x, gy1 + 1.0, at.z, Mth.cos(a) * 0.18,
                            0.05 + r.nextDouble() * 0.1, Mth.sin(a) * 0.18);
                }
            }
            if (age == 2) {
                for (int i = FxParticles.n(12); i > 0; i--) {
                    FxParticles.plain(level, new DustParticleOptions(new Vector3f(0.45f, 0.0f, 0.02f), 1.4f), spot.x + FxParticles.j(r) * 0.6,
                            spot.y + 0.2 + r.nextDouble() * 1.2, spot.z + FxParticles.j(r) * 0.6, 0, -0.02, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            // The smear from where he stood to where he is: dark red, gone in a moment.
            float sm = Mth.clamp(1 - t / 10f, 0, 1);
            if (sm > 0) {
                for (int i = 0; i <= 6; i++) {
                    float k = i / 6f;
                    Vec3 p = from.lerp(at, k);
                    double y = gy0 + (gy1 - gy0) * k;
                    d.darkDisc(p.x, y + 1.0, p.z, 0.75f - 0.25f * k, 0x2A0204, 0.45f * sm * (1 - k * 0.5f));
                }
                for (int i = 0; i < 3; i++) {
                    double h = 0.5 + i * 0.55;
                    d.taper(from.x, gy0 + h, from.z, at.x, gy1 + h, at.z, 0.35f, 0.08f, 0x8A0A10, 0.6f * sm, 0.1f * sm);
                }
                // Where he stood: the darkness he left folding in on itself.
                float c = Mth.clamp(1 - t / 8f, 0, 1);
                d.darkDisc(from.x, gy0 + 1.0, from.z, 1.1f * c, 0x14010A, 0.55f * c);
            }
            // The flash where he reappears.
            if (t < 6) {
                float f = 1 - t / 6f;
                d.star(at.x, gy1 + 1.1, at.z, 2.4f * f + 0.4f, 0.4f, EMBER, 0.9f * f);
                d.glowDisc(at.x, gy1 + 1.1, at.z, 1.4f, 0xFFD0B8, 0.6f * f);
            }
            // The First Blade's arc coming down on the spot.
            if (t < 10) {
                float k = easeOut(t / 4f), fa = Mth.clamp(1 - (t - 3) / 7f, 0, 1);
                Vec3 toward = spot.subtract(at).multiply(1, 0, 1);
                if (toward.lengthSqr() < 1e-3) toward = new Vec3(0, 0, 1);
                toward = toward.normalize();
                int n = 10;
                Vec3 prev = null;
                for (int i = 0; i <= n; i++) {
                    float u = i / (float) n;
                    if (u > k) break;
                    // From over his shoulder, over the top, down into the ground just past the spot.
                    float ang = Mth.PI * (0.15f + 0.85f * u);
                    Vec3 p = at.add(toward.scale(0.4 + 1.6 * Math.sin(ang))).add(0, 1.0 + 1.5 * Math.cos(ang), 0);
                    if (prev != null) {
                        float w = 0.12f + 0.38f * Mth.sin(Mth.PI * u);
                        d.softBeam(prev.x, prev.y, prev.z, p.x, p.y, p.z, w * 1.8f, BLOOD, 0.6f * fa, 0.7f * fa, false);
                        d.softBeam(prev.x, prev.y, prev.z, p.x, p.y, p.z, w * 0.5f, MARK_HOT, 0.8f * fa, 0.9f * fa, true);
                    }
                    prev = p;
                }
                if (t > 3) d.flatGlow(spot.x, spot.y + 0.04, spot.z, 1.8f, BLOOD, 0.5f * fa);
            }
        }
    }

    // --- the Mark's flare ------------------------------------------------------------------------------------------------------

    /** The Mark burnt into the ground under him, charging, then the yard on fire round the arena's centre. */
    static final class Flare extends MagicEffect {
        private static final float GLYPH = 3.4f;
        final int cain;
        private final float radius;
        private final int windup;
        private final Vec3 centre;
        private Vec3 feet;
        private double gy;
        /** The shelters (x, floor y, z) and their radii, as their payloads come. */
        private final List<Vec3> shelters = new ArrayList<>();
        private final List<Float> shelterRadii = new ArrayList<>();

        Flare(int cain, Vec3 centre, float radius, int windup) {
            super(centre, School.INFERNAL, Math.max(10, windup) + 32);
            this.cain = cain;
            this.centre = centre;
            Entity e = entity(cain);
            this.feet = e != null ? e.position() : centre;
            this.radius = Math.max(3, radius);
            this.windup = Math.max(10, windup);
            this.gy = groundY(feet);
        }

        boolean isDone() {
            return done();
        }

        void shelter(Vec3 at, float r) {
            shelters.add(new Vec3(at.x, groundY(at), at.z));
            shelterRadii.add(r > 0.2f ? r : CainBalance.FLARE_SAFE_RADIUS);
        }

        private float shelterRadius(int i) {
            return i < shelterRadii.size() ? shelterRadii.get(i) : CainBalance.FLARE_SAFE_RADIUS;
        }

        @Override
        protected double reach() {
            return radius + 4;
        }

        @Override
        protected Vec3 anchor() {
            return centre;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private float yaw(float partial) {
            Entity e = entity(cain);
            return e instanceof LivingEntity l ? Mth.lerp(partial, l.yBodyRotO, l.yBodyRot) : e != null ? e.getYRot() : 0;
        }

        private float charge(float t) {
            return Mth.clamp(t / windup, 0, 1);
        }

        /** Ticks between the Mark's beats: slow, then racing. */
        private float beat(float t) {
            float k = charge(t);
            return 22 - 15 * k * k;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Entity e = entity(cain);
            if (e != null && age < windup) {
                feet = e.position();
                gy = groundY(feet);
            }
            if (age < windup) {
                float k = charge(age);
                // Embers rising off the burning rune, more as it charges.
                for (int i = FxParticles.rate(r, 0.8f + 3 * k); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, d = r.nextFloat() * GLYPH * 0.55f;
                    FxParticles.ember(level, r.nextBoolean() ? EMBER : BLOOD, 0.7f, 20, feet.x + Mth.cos(a) * d, gy + 0.1, feet.z + Mth.sin(a) * d,
                            0, 0.06 + k * 0.05, 0);
                }
                for (int si = 0; si < shelters.size(); si++) {
                    Vec3 s = shelters.get(si);
                    if (r.nextFloat() < 0.4f) {
                        float a = r.nextFloat() * Mth.TWO_PI, d = r.nextFloat() * shelterRadius(si);
                        FxParticles.mote(level, SAFE, 0.5f, 26, s.x + Mth.cos(a) * d, s.y + 0.1, s.z + Mth.sin(a) * d, 0, 0.04, 0);
                    }
                }
            }
            if (age == windup) {
                MagicShake.kick(centre, 1.0f, 16, radius + 12);
                MagicOverlay.flash(centre, 0xFF3A1E, 0.35f, radius + 6);
            }
            if (age >= windup && age < windup + 12) {
                float k = (age - windup) / 10f, rr = radius * easeOut(k);
                for (int i = FxParticles.n(24); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    double d = Math.min(radius, rr + FxParticles.j(r) * 0.8);
                    double x = centre.x + Mth.cos(a) * d, z = centre.z + Mth.sin(a) * d;
                    if (sheltered(x, z)) continue;
                    if (i % 3 == 0) FxParticles.plain(level, ParticleTypes.FLAME, x, gy + 0.2, z, 0, 0.08, 0);
                    else FxParticles.ember(level, i % 2 == 0 ? EMBER : MARK_HOT, 0.9f, 22, x, gy + 0.2, z, 0, 0.1 + r.nextDouble() * 0.1, 0);
                }
            }
        }

        private boolean sheltered(double x, double z) {
            for (int i = 0; i < shelters.size(); i++) {
                Vec3 s = shelters.get(i);
                double dx = x - s.x, dz = z - s.z;
                float sr = shelterRadius(i);
                if (dx * dx + dz * dz < sr * sr) return true;
            }
            return false;
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float k = charge(t);
            if (t < windup) {
                float bt = beat(t), ph = (t % bt) / bt;
                float thump = Math.max(bump(ph, 0), 0.6f * bump(ph, 0.22f));
                float glow = (0.35f + 0.65f * k) * (0.7f + 0.3f * thump) * fade(t, 4, 0);
                Vec3 f = feet;
                float yaw = yaw(d.partial);
                // The rune burnt into the ground, drawn on over the first part of the windup.
                float drawn = Mth.clamp(t / (windup * 0.45f), 0, 1);
                groundGlyph(d, f, gy + 0.05, yaw, GLYPH, drawn, 0.55f, BLOOD, 0.75f * glow);
                groundGlyph(d, f, gy + 0.06, yaw, GLYPH, drawn, 0.18f, MARK_HOT, 0.9f * glow);
                d.flatGlow(f.x, gy + 0.04, f.z, GLYPH * 1.1f, BLOOD, 0.35f * glow);
                d.groundRing(f.x, gy + 0.05, f.z, GLYPH * 0.66f, GLYPH * 0.72f, EMBER, 0.6f * glow, false);
                d.spikedRing(f.x, gy + 0.05, f.z, GLYPH * 0.76f, 0.35f, 9, -t * 0.03f, BLOOD, 0.5f * glow);
                // Each beat, a ring of heat going out from it.
                float bk = easeOut(ph);
                d.groundRing(f.x, gy + 0.04, f.z, GLYPH * 0.7f + bk * 4.5f - 0.5f, GLYPH * 0.7f + bk * 4.5f, EMBER, 0.55f * (1 - ph) * k + 0.1f * (1 - ph),
                        true);
                // Heat over him and at the yard's rim, rising as it charges.
                d.glowDisc(f.x, gy + 1.2, f.z, 1.6f + k, BLOOD, 0.25f * glow);
                d.groundRing(centre.x, gy + 0.05, centre.z, radius - 0.9f, radius, BLOOD, 0.4f * k, true);
                // The shelters, lit green.
                for (int si = 0; si < shelters.size(); si++) {
                    Vec3 s = shelters.get(si);
                    float sr = shelterRadius(si), sp = 0.8f + 0.2f * Mth.sin(t * 0.3f + (float) s.x);
                    d.cylinder(s.x, s.y + 0.02, s.z, sr, 2.6f + k, SAFE, 0.32f * sp * fade(t, 6, 0), 0);
                    d.flatGlow(s.x, s.y + 0.05, s.z, sr * 1.2f, SAFE, 0.35f * sp);
                    d.groundRing(s.x, s.y + 0.06, s.z, sr - 0.15f, sr, SAFE, 0.8f * sp, false);
                }
            } else {
                // The burst: a red shockwave out to the yard's edge, fire behind it, the rune a white-hot scar fading.
                float s = t - windup, w = Mth.clamp(s / 10f, 0, 1), r = radius * easeOut(w), fa = 1 - Mth.clamp((s - 4) / 26f, 0, 1);
                d.shockwave(MagicTextures.ENERGY_RING, centre.x, gy + 0.05, centre.z, Math.max(0.2f, r), 1.6f, 2.4f * (1 - w) + 0.3f, EMBER,
                        0.9f * (1 - w * 0.6f));
                d.groundRing(centre.x, gy + 0.06, centre.z, Math.max(0, r - 3.5f), r, MARK_HOT, 0.7f * (1 - w * 0.5f), true);
                d.flatGlow(centre.x, gy + 0.04, centre.z, Math.max(0.5f, r), BLOOD, 0.55f * fa);
                groundGlyph(d, feet, gy + 0.06, yaw(d.partial), GLYPH, 1, 0.3f, 0xFFE8D0, 0.9f * fa);
                if (s < 5) d.star(feet.x, gy + 1.4, feet.z, 6f * (1 - s / 5f) + 1, 0.3f, EMBER, 1 - s / 5f);
                for (int si = 0; si < shelters.size(); si++) {
                    Vec3 sh = shelters.get(si);
                    d.cylinder(sh.x, sh.y + 0.02, sh.z, shelterRadius(si), 2.6f, SAFE, 0.45f * fa, 0);
                }
            }
        }

        private static float bump(float x, float at) {
            float d = (x - at) / 0.08f;
            return (float) Math.exp(-d * d);
        }
    }

    // --- the offer of the Mark -------------------------------------------------------------------------------------------------

    /** The Mark offered (hovering before the hunter) or taken (rushing into their arm). */
    static final class Offer extends MagicEffect {
        private final int cain, hunter;
        final boolean taken;
        private final Vec3 startSigil;
        boolean closing;
        private int closeAt = -1;

        Offer(int cain, int hunter, boolean taken, Vec3 at, int ticks, Vec3 startSigil) {
            super(at, School.INFERNAL, taken ? Math.max(30, Math.min(ticks, 50)) : Math.max(40, ticks));
            this.cain = cain;
            this.hunter = hunter;
            this.taken = taken;
            this.startSigil = startSigil;
        }

        void close() {
            closing = true;
            closeAt = age;
        }

        @Override
        protected boolean done() {
            return super.done() || closing && age - closeAt > 2;
        }

        @Override
        protected double reach() {
            return 8;
        }

        @Override
        protected Vec3 anchor() {
            Entity h = entity(hunter);
            return h != null ? h.position() : at;
        }

        private boolean firstPerson() {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && mc.player.getId() == hunter && mc.options.getCameraType().isFirstPerson();
        }

        /** Where the Mark hangs while offered. */
        Vec3 sigil(float partial) {
            Entity h = entity(hunter);
            if (h == null) return at.add(0, 1.6, 0);
            float bob = 0.06f * Mth.sin((age + partial) * 0.12f);
            if (firstPerson()) {
                Vec3 look = h.getViewVector(partial);
                return h.getEyePosition(partial).add(look.scale(1.5)).add(0, -0.1 + bob, 0);
            }
            Entity c = entity(cain);
            Vec3 hp = h.getPosition(partial).add(0, 1.45 + bob, 0);
            if (c != null) {
                Vec3 cp = c.getPosition(partial).add(0, 1.45, 0);
                return hp.lerp(cp, 0.3);
            }
            return hp.add(0, 0.8, 0);
        }

        /** The hunter's right forearm. */
        private Vec3 arm(float partial) {
            Entity h = entity(hunter);
            if (h == null) return at.add(0, 1, 0);
            float yaw = (h instanceof LivingEntity l ? Mth.lerp(partial, l.yBodyRotO, l.yBodyRot) : h.getYRot()) * Mth.DEG_TO_RAD;
            Vec3 p = h.getPosition(partial);
            if (firstPerson()) {
                Vec3 look = h.getViewVector(partial);
                return h.getEyePosition(partial).add(look.scale(0.6)).add(-Mth.cos(yaw) * 0.35, -0.45, -Mth.sin(yaw) * 0.35);
            }
            return p.add(-Mth.cos(yaw) * 0.38, h.getBbHeight() * 0.45, -Mth.sin(yaw) * 0.38);
        }

        /** Cain's right forearm (the thread's start). */
        private Vec3 cainArm(float partial) {
            Entity c = entity(cain);
            if (c == null) return null;
            float yaw = (c instanceof LivingEntity l ? Mth.lerp(partial, l.yBodyRotO, l.yBodyRot) : c.getYRot()) * Mth.DEG_TO_RAD;
            return c.getPosition(partial).add(-Mth.cos(yaw) * 0.38, 0.75, -Mth.sin(yaw) * 0.38);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (taken) {
                Vec3 m = arm(1f);
                if (age == 12) {
                    for (int i = FxParticles.n(20); i > 0; i--) {
                        float a = r.nextFloat() * Mth.TWO_PI;
                        FxParticles.spark(level, i % 2 == 0 ? BLOOD : MARK_HOT, 0.7f, 14, m.x, m.y, m.z, Mth.cos(a) * 0.16, FxParticles.j(r) * 0.12,
                                Mth.sin(a) * 0.16);
                    }
                }
                if (age > 12 && age < life - 6 && !firstPerson()) {
                    for (int i = FxParticles.rate(r, 0.8f); i > 0; i--) {
                        FxParticles.ember(level, BLOOD, 0.5f, 14, m.x + FxParticles.j(r) * 0.1, m.y, m.z + FxParticles.j(r) * 0.1, 0, 0.04, 0);
                    }
                }
                return;
            }
            if (age < life - 10 && !firstPerson()) {
                Vec3 s = sigil(1f);
                for (int i = FxParticles.rate(r, 1.2f); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.ember(level, r.nextBoolean() ? EMBER : MARK_HOT, 0.45f, 18, s.x + Mth.cos(a) * 0.5, s.y + FxParticles.j(r) * 0.4,
                            s.z + Mth.sin(a) * 0.5, 0, 0.02, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            if (taken) {
                renderTaken(d, t);
                return;
            }
            float a = fade(t, 12, 20) * (closing ? 0 : 1);
            if (a <= 0.01f) return;
            Vec3 s = sigil(d.partial);
            boolean fp = firstPerson();
            float size = fp ? 0.55f : 0.9f;
            float beat = 0.8f + 0.2f * Mth.sin(t * 0.33f);
            float drawn = Mth.clamp(t / 30f, 0, 1);
            d.glowDisc(s.x, s.y, s.z, size * 1.6f * beat, BLOOD, 0.45f * a);
            d.glowDisc(s.x, s.y, s.z, size * 0.8f, 0xFFB050, 0.25f * a);
            airGlyph(d, s, size, drawn, size * 0.16f, BLOOD, 0.8f * a * beat, false);
            airGlyph(d, s, size, drawn, size * 0.06f, MARK_HOT, 0.95f * a, true);
            // A ring of gold embers slowly circling it.
            Vector3f l = d.camera.getLeftVector(), u = d.camera.getUpVector();
            for (int i = 0; i < 9; i++) {
                float ang = t * 0.05f + i * Mth.TWO_PI / 9, rr = size * 0.95f;
                float cx = Mth.cos(ang) * rr, cy = Mth.sin(ang) * rr;
                d.glowDisc(s.x + l.x * cx + u.x * cy, s.y + l.y * cx + u.y * cy, s.z + l.z * cx + u.z * cy, size * 0.12f, 0xFFD27A, 0.7f * a);
            }
            // The thread from his own Mark to it.
            Vec3 c = cainArm(d.partial);
            if (c != null && !fp) {
                float pulse = 0.5f + 0.5f * Mth.sin(t * 0.25f);
                d.softBeam(c.x, c.y, c.z, s.x, s.y, s.z, 0.12f, BLOOD, 0.35f * a * pulse, 0.55f * a, false);
            }
        }

        private void renderTaken(FxDraw d, float t) {
            Vec3 m = arm(d.partial);
            Vec3 from = startSigil != null ? startSigil : m.add(0, 0.9, 0);
            if (t < 12) {
                // It rushes into the arm, shrinking.
                float k = easeOut(t / 12f);
                Vec3 p = from.lerp(m, k);
                float size = 0.8f * (1 - k) + 0.15f;
                d.glowDisc(p.x, p.y, p.z, size * 1.6f, BLOOD, 0.6f);
                airGlyph(d, p, size, 1, size * 0.15f, MARK_HOT, 0.9f, true);
                d.taper(from.x, from.y, from.z, p.x, p.y, p.z, 0.02f, size * 0.5f, BLOOD, 0, 0.6f);
                return;
            }
            if (firstPerson()) return;
            float s = t - 12, f = Mth.clamp(1 - s / 8f, 0, 1);
            if (f > 0) {
                d.star(m.x, m.y, m.z, 2.2f * f + 0.3f, 0.785f, EMBER, f);
                d.glowDisc(m.x, m.y, m.z, 1.2f, 0xFFD8C0, 0.6f * f);
            }
            float a = fade(t, 0, 16);
            d.glowDisc(m.x, m.y, m.z, 0.35f, BLOOD, 0.6f * a * (0.7f + 0.3f * Mth.sin(t * 0.5f)));
        }
    }
}
