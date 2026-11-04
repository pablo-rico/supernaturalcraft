package org.papiricoh.supernaturalcraft.client.crowley;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicShake;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicSounds;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Crowley's fight in the world (v0.35), on the magic's effect kit ({@link MagicEffect}, {@link FxDraw}). His identity: red
 * demon smoke, paper (contracts by the hundred), hellfire, wax seals, the hounds nobody sees.
 * <ul>
 *   <li>{@link Smoke}: red smoke — a lingering cloud, his vanishing (a burst blown out and up from where he stood), his arrival
 *   (smoke rushing in and bursting), the companion's smaller poof;</li>
 *   <li>{@link PaperStorm}: a cone of fluttering, tumbling contracts blown out from him, the gust drawn as streaks;</li>
 *   <li>{@link HellfireLine}: fire running along a line on the floor, burning, then a scorch;</li>
 *   <li>{@link Snap}: the snap's flash at his hand, a shockwave ring, and a streak to whoever it shoves;</li>
 *   <li>{@link SealBreak}: a great wax seal over him, cracking and bursting into shards;</li>
 *   <li>{@link Hounds}: the ground scorched where they come through, burning paw prints running out from it;</li>
 *   <li>{@link Collect}: the mark over a hunter whose debt is due, a turning red sigil and a ring under their feet.</li>
 * </ul>
 */
public final class CrowleyWorldFx {

    static final int RED = 0xD0141E, RED_HOT = 0xFF4A36, SMOKE_RED = 0x7A0A10, EMBER = 0xFF6A2A, FIRE = 0xFF8A2A, FIRE_HOT = 0xFFD08A,
            SCORCH = 0x140806, WAX = 0x9A1414, SNAP = 0xFF5A4A;

    private CrowleyWorldFx() {
    }

    // --- starting them --------------------------------------------------------------------------------------------------

    public static void smoke(Vec3 at, float radius, int ticks, Smoke.Kind kind) {
        MagicFxManager.add(new Smoke(at, radius, ticks, kind));
        switch (kind) {
            case VANISH, POOF_OUT -> MagicSounds.play(AllSounds.crowley("smoke_out"), at, kind == Smoke.Kind.VANISH ? 1.2f : 0.8f, 1f);
            case APPEAR, POOF_IN -> MagicSounds.play(AllSounds.crowley("smoke_in"), at, kind == Smoke.Kind.APPEAR ? 1.2f : 0.8f, 1f);
            default -> {
            }
        }
    }

    public static void paperStorm(Vec3 apex, float yaw, float reach, int ticks) {
        MagicFxManager.add(new PaperStorm(apex, yaw, Math.max(2, reach), Math.max(10, ticks)));
        MagicSounds.play(AllSounds.crowley("paper_storm"), apex, 1.4f, 1f);
    }

    public static void hellfireLine(Vec3 start, Vec3 end, int ticks) {
        MagicFxManager.add(new HellfireLine(start, end, Math.max(10, ticks)));
        MagicSounds.play(AllSounds.crowley("hellfire"), start.add(end).scale(0.5), 1.3f, 1f);
    }

    public static void snap(int crowley, Vec3 hand, int shoved, int ticks) {
        MagicFxManager.add(new Snap(hand, shoved, Math.max(8, ticks)));
        MagicSounds.play(AllSounds.crowley("snap"), hand, 1.5f, 1f);
    }

    public static void sealBreak(Vec3 over, int seal, int ticks) {
        MagicFxManager.add(new SealBreak(over, seal, Math.max(20, ticks)));
        MagicSounds.play(AllSounds.crowley("seal_break"), over, 2f, 1f);
    }

    public static void hounds(Vec3 at, int count, int ticks) {
        MagicFxManager.add(new Hounds(at, Math.max(1, count), Math.max(20, ticks)));
        MagicSounds.play(AllSounds.crowley("hounds"), at, 1.6f, 1f);
    }

    public static void collect(int victim, Vec3 at, int ticks) {
        MagicFxManager.add(new Collect(victim, at, Math.max(20, ticks)));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getId() == victim) MagicOverlay.flash(at, RED, 0.35f, 8);
    }

    // --- shared bits ----------------------------------------------------------------------------------------------------

    static double groundY(Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? at.y : FxWorld.floor(mc.level, at.add(0, 1.5, 0), 6);
    }

    static long seedOf(Vec3 at) {
        return Double.doubleToLongBits(at.x * 31.7 + at.z * 17.3 + at.y);
    }

    static void redSmoke(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, boolean dark) {
        int rgb = dark ? 0x3A0406 : level.random.nextBoolean() ? 0x8A0E12 : 0xB01A16;
        level.addParticle(ColorParticleOption.create(AllParticles.BOWL_SMOKE.get(), 0xFF000000 | rgb), x, y, z, vx, vy, vz);
    }

    /**
     * A sheet of paper (or any piece of the contract sheet), normally blended and lit by the world: corners {@code c ± u ± v},
     * texture region {@code u0..u1, v0..v1} of {@link CrowleyHud#SHEET}.
     */
    static void sheet(FxDraw d, double x, double y, double z, float uX, float uY, float uZ, float vX, float vY, float vZ,
                      float u0, float v0, float u1, float v1, int argb) {
        // Lit by the sky and blocks, but never darker than half: the paper and wax must read in a dim hall.
        Minecraft mc = Minecraft.getInstance();
        int light = mc.level == null ? 0xF000F0 : LevelRenderer.getLightColor(mc.level, BlockPos.containing(x, y, z));
        light = Math.max(light & 0xFFFF, 8 << 4) | Math.max(light >>> 16, 12 << 4) << 16;
        VertexConsumer vc = d.buffers.getBuffer(RenderType.entityTranslucent(CrowleyHud.SHEET));
        PoseStack.Pose p = d.pose.last();
        Vec3 e = d.eye();
        float cx = (float) (x - e.x), cy = (float) (y - e.y), cz = (float) (z - e.z);
        if (uX * uX + uY * uY + uZ * uZ < 1e-8f || vX * vX + vY * vY + vZ * vZ < 1e-8f) return;
        // Shaded as if facing up whatever its tumble: an edge-on sheet would otherwise go dark brown under the entity light.
        float nx = 0, ny = 1, nz = 0;
        float tu0 = u0 / CrowleyHud.TW, tu1 = u1 / CrowleyHud.TW, tv0 = v0 / CrowleyHud.TH, tv1 = v1 / CrowleyHud.TH;
        corner(vc, p, cx - uX - vX, cy - uY - vY, cz - uZ - vZ, tu0, tv1, argb, light, nx, ny, nz);
        corner(vc, p, cx + uX - vX, cy + uY - vY, cz + uZ - vZ, tu1, tv1, argb, light, nx, ny, nz);
        corner(vc, p, cx + uX + vX, cy + uY + vY, cz + uZ + vZ, tu1, tv0, argb, light, nx, ny, nz);
        corner(vc, p, cx - uX + vX, cy - uY + vY, cz - uZ + vZ, tu0, tv0, argb, light, nx, ny, nz);
    }

    private static void corner(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int argb, int light,
                               float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    static int alpha(float a, int rgb) {
        return FxDraw.argb(Mth.clamp(a, 0, 1), rgb);
    }

    // --- red smoke ------------------------------------------------------------------------------------------------------

    /** Red demon smoke: a lingering cloud, or the burst of his going and coming. */
    public static final class Smoke extends MagicEffect {
        public enum Kind { CLOUD, VANISH, APPEAR, POOF_IN, POOF_OUT }

        private final float radius;
        private final int linger;
        private final Kind kind;
        private final double gy;

        Smoke(Vec3 at, float radius, int ticks, Kind kind) {
            super(at, School.INFERNAL, Math.max(10, ticks) + 20);
            this.radius = Math.max(0.6f, radius);
            this.linger = Math.max(10, ticks);
            this.kind = kind;
            this.gy = groundY(at);
        }

        @Override
        protected double reach() {
            return radius + 3;
        }

        private boolean small() {
            return kind == Kind.POOF_IN || kind == Kind.POOF_OUT;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            float k = small() ? 0.5f : 1f;
            if (age == 0 && kind != Kind.CLOUD) {
                // The burst: blown out and up (going) or rushing in (coming).
                boolean in = kind == Kind.APPEAR || kind == Kind.POOF_IN;
                for (int i = FxParticles.n((int) (46 * k)); i > 0; i--) {
                    double a = r.nextDouble() * Mth.TWO_PI, h = r.nextDouble() * 2.0 * k, d = 0.2 + r.nextDouble() * 0.9 * k;
                    double x = at.x + Math.cos(a) * d, z = at.z + Math.sin(a) * d, y = gy + h;
                    double s = in ? -0.06 : 0.12;
                    redSmoke(level, x, y, z, Math.cos(a) * s, (in ? 0.0 : 0.06) + r.nextDouble() * 0.04, Math.sin(a) * s, i % 4 == 0);
                }
                for (int i = FxParticles.n((int) (14 * k)); i > 0; i--) {
                    FxParticles.ember(level, i % 2 == 0 ? RED_HOT : EMBER, 0.5f, 18, at.x + FxParticles.j(r) * 0.5, gy + r.nextDouble() * 1.8,
                            at.z + FxParticles.j(r) * 0.5, FxParticles.j(r) * 0.08, 0.05, FxParticles.j(r) * 0.08);
                }
                MagicShake.kick(at, small() ? 0.1f : 0.25f, 6, 12);
            }
            if (kind == Kind.CLOUD && age < linger) {
                for (int i = FxParticles.rate(r, 2.4f * radius); i > 0; i--) {
                    double a = r.nextDouble() * Mth.TWO_PI, d = Math.sqrt(r.nextDouble()) * radius;
                    redSmoke(level, at.x + Math.cos(a) * d, gy + 0.2 + r.nextDouble() * 1.4, at.z + Math.sin(a) * d,
                            -Math.sin(a) * 0.02, 0.01, Math.cos(a) * 0.02, r.nextInt(3) == 0);
                }
            } else if (kind == Kind.VANISH && age < 12) {
                // A swirling column rising where he stood.
                double a = age * 0.9;
                for (int i = 0; i < 2; i++) {
                    redSmoke(level, at.x + Math.cos(a + i * Math.PI) * 0.4, gy + 0.3 + age * 0.12, at.z + Math.sin(a + i * Math.PI) * 0.4,
                            0, 0.08, 0, false);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            if (kind == Kind.CLOUD) {
                float a = Mth.clamp(t / 10f, 0, 1) * (1 - Mth.clamp((t - linger) / 20f, 0, 1));
                d.darkDisc(at.x, gy + 0.9, at.z, radius * 1.1f, SMOKE_RED, 0.35f * a * d.near(at.x, gy + 0.9, at.z, radius));
                d.flatGlow(at.x, gy + 0.03, at.z, radius, RED, 0.18f * a);
                return;
            }
            float k = small() ? 0.55f : 1f;
            boolean in = kind == Kind.APPEAR || kind == Kind.POOF_IN;
            float p = Mth.clamp(t / (in ? 10f : 14f), 0, 1);
            float a = in ? (t < 10 ? p : 1 - Mth.clamp((t - 10) / 10f, 0, 1)) : 1 - p;
            double y = gy + 1.0 * k;
            float size = (in ? 1.6f - 0.8f * p : 0.8f + 1.4f * p) * k;
            d.darkDisc(at.x, y, at.z, size, SMOKE_RED, 0.55f * a * d.near(at.x, y, at.z, size));
            d.glowDisc(at.x, y, at.z, size * 0.7f, RED, 0.35f * a * d.near(at.x, y, at.z, size));
            d.groundRing(at.x, gy + 0.03, at.z, 0.3f + 1.6f * k * (in ? 1 - p : p), 0.5f + 1.9f * k * (in ? 1 - p : p), RED_HOT, 0.4f * a, true);
        }
    }

    // --- the paper storm ------------------------------------------------------------------------------------------------

    /** Contracts blown out across a cone: each sheet a lit, tumbling square of parchment flying along its own path. */
    static final class PaperStorm extends MagicEffect {
        private static final int SHEETS = 70;
        private final float yaw, reach;
        private final int fly;
        private final long seed;

        PaperStorm(Vec3 apex, float yaw, float reach, int ticks) {
            super(apex.add(0, 1.2, 0), School.INFERNAL, ticks + 20);
            this.yaw = yaw;
            this.reach = reach;
            this.fly = ticks;
            this.seed = seedOf(apex);
        }

        @Override
        protected double reach() {
            return reach + 4;
        }

        private double[] dir(float off) {
            float a = (yaw + off) * Mth.DEG_TO_RAD;
            return new double[]{-Mth.sin(a), Mth.cos(a)};
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) MagicShake.kick(at, 0.2f, 8, reach + 6);
            if (age > fly) return;
            for (int i = FxParticles.rate(r, 2.5f); i > 0; i--) {
                double[] dv = dir((r.nextFloat() - 0.5f) * 60);
                double s = 0.4 + r.nextDouble() * 0.4;
                FxParticles.plain(level, AllParticles.PAGE_SCRAP.get(), at.x + dv[0], at.y + FxParticles.j(r) * 0.4, at.z + dv[1],
                        dv[0] * s, 0.04 + r.nextDouble() * 0.06, dv[1] * s);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            java.util.Random r = new java.util.Random(seed);
            for (int i = 0; i < SHEETS; i++) {
                float start = r.nextFloat() * fly * 0.7f, off = (r.nextFloat() - 0.5f) * 60, speed = 0.45f + r.nextFloat() * 0.4f;
                float rise = (r.nextFloat() - 0.3f) * 0.05f, spinA = 0.2f + r.nextFloat() * 0.4f, spinB = 0.15f + r.nextFloat() * 0.35f;
                float ph = r.nextFloat() * 10, sz = 0.2f + r.nextFloat() * 0.1f;
                int region = r.nextInt(3);
                float lt = t - start;
                if (lt < 0) continue;
                float dist = Math.min(reach, lt * speed * (1 - Math.min(0.6f, lt / 60f)));
                float a = Mth.clamp(lt / 3f, 0, 1) * (1 - Mth.clamp((lt - (reach / speed) - 6) / 10f, 0, 1));
                if (a <= 0.02f) continue;
                double[] dv = dir(off + 8 * Mth.sin(lt * 0.15f + ph));
                double x = at.x + dv[0] * dist, z = at.z + dv[1] * dist;
                double y = at.y + rise * lt + 0.35 * Mth.sin(lt * 0.3f + ph) - Math.max(0, lt - reach / speed) * 0.04;
                // Tumbling: a sheet's two axes turned about each other over time.
                float aa = lt * spinA + ph, bb = lt * spinB + ph * 0.7f;
                float ux = Mth.cos(aa) * sz, uy = Mth.sin(aa) * Mth.cos(bb) * sz, uz = Mth.sin(aa) * Mth.sin(bb) * sz;
                float vx = -Mth.sin(aa) * Mth.sin(bb) * sz * 1.3f, vy = Mth.cos(bb) * sz * 0.4f * 1.3f, vz = Mth.cos(aa) * sz * 1.3f;
                float u0 = 14 + region * 70, v0 = 5;
                sheet(d, x, y, z, ux, uy, uz, vx, vy, vz, u0, v0, u0 + 34, v0 + 30, alpha(a * d.near(x, y, z, sz), 0xFFFFFF));
            }
            // The gust: pale streaks along the cone.
            float g = Mth.clamp(t / 4f, 0, 1) * (1 - Mth.clamp((t - fly) / 10f, 0, 1));
            for (int i = 0; i < 6; i++) {
                float off = -24 + 48 * i / 5f + 6 * Mth.sin(t * 0.2f + i);
                double[] dv = dir(off);
                float s0 = ((t * 0.6f + i * 1.7f) % reach), s1 = Math.min(reach, s0 + 2.2f);
                d.ribbon(at.x + dv[0] * s0, at.y + 0.2 * Mth.sin(i), at.z + dv[1] * s0, at.x + dv[0] * s1, at.y + 0.2 * Mth.sin(i), at.z + dv[1] * s1,
                        0.05f, 0xF2E6C8, 0.25f * g, 0f);
            }
        }
    }

    // --- hellfire along a line ------------------------------------------------------------------------------------------

    static final class HellfireLine extends MagicEffect {
        private static final int RUN = 8;
        private final Vec3 end;
        private final int burn;
        private final double gy;

        HellfireLine(Vec3 start, Vec3 end, int ticks) {
            super(start, School.INFERNAL, RUN + ticks + 30);
            this.end = end;
            this.burn = ticks;
            this.gy = groundY(start);
        }

        @Override
        protected double reach() {
            return end.distanceTo(at) + 3;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        private float front(float t) {
            return Mth.clamp(t / RUN, 0, 1);
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age > RUN + burn) return;
            float f = front(age);
            double len = end.distanceTo(at);
            for (int i = FxParticles.rate(r, (float) (len * 0.9)); i > 0; i--) {
                double u = r.nextDouble() * f;
                double x = Mth.lerp(u, at.x, end.x) + FxParticles.j(r) * 0.25, z = Mth.lerp(u, at.z, end.z) + FxParticles.j(r) * 0.25;
                if (i % 3 == 0) FxParticles.plain(level, ParticleTypes.FLAME, x, gy + 0.1, z, 0, 0.04 + r.nextDouble() * 0.05, 0);
                else FxParticles.ember(level, r.nextBoolean() ? FIRE : EMBER, 0.6f, 20, x, gy + 0.15, z, 0, 0.07, 0);
            }
            if (r.nextFloat() < 0.3f) {
                double u = r.nextDouble() * f;
                FxParticles.plain(level, ParticleTypes.LARGE_SMOKE, Mth.lerp(u, at.x, end.x), gy + 1.0, Mth.lerp(u, at.z, end.z), 0, 0.04, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), f = front(t);
            float a = Mth.clamp(t / 2f, 0, 1) * (1 - Mth.clamp((t - RUN - burn) / 12f, 0, 1));
            double x1 = Mth.lerp(f, at.x, end.x), z1 = Mth.lerp(f, at.z, end.z);
            float flick = 0.8f + 0.2f * Mth.sin(t * 1.3f) * Mth.sin(t * 0.47f);
            // The scorch stays a little after the fire.
            float sc = Mth.clamp(t / 6f, 0, 1) * (1 - Mth.clamp((t - RUN - burn - 10) / 20f, 0, 1));
            d.groundLine(at.x, at.z, x1, z1, gy + 0.03, 1.4f, EMBER, 0.18f * sc);
            d.groundLine(at.x, at.z, x1, z1, gy + 0.05, 0.9f, FIRE, 0.45f * a * flick);
            d.groundLine(at.x, at.z, x1, z1, gy + 0.06, 0.35f, FIRE_HOT, 0.6f * a * flick);
            if (f < 1) d.glowDisc(x1, gy + 0.4, z1, 0.9f, FIRE_HOT, 0.6f * a);
            // A low wall of flame along it.
            int n = (int) (end.distanceTo(at) * 2);
            for (int i = 0; i <= n; i++) {
                float u = i / (float) Math.max(1, n);
                if (u > f) break;
                double x = Mth.lerp(u, at.x, end.x), z = Mth.lerp(u, at.z, end.z);
                float hgt = 0.5f + 0.35f * Mth.sin(t * 0.7f + i * 1.9f);
                d.softBeam(x, gy, z, x, gy + hgt, z, 0.35f, FIRE, 0.35f * a, 0f, false);
            }
        }
    }

    // --- the snap -------------------------------------------------------------------------------------------------------

    static final class Snap extends MagicEffect {
        private final int shoved;

        Snap(Vec3 hand, int shoved, int ticks) {
            super(hand, School.INFERNAL, ticks + 12);
            this.shoved = shoved;
        }

        @Override
        protected double reach() {
            return 12;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                MagicShake.kick(at, 0.3f, 6, 14);
                for (int i = FxParticles.n(14); i > 0; i--) {
                    FxParticles.spark(level, i % 2 == 0 ? SNAP : 0xFFFFFF, 0.4f, 8, at.x, at.y, at.z, FxParticles.j(r) * 0.2, FxParticles.j(r) * 0.2,
                            FxParticles.j(r) * 0.2);
                }
            }
            Entity e = shoved >= 0 ? level.getEntity(shoved) : null;
            if (e != null && age < 10) {
                for (int i = FxParticles.rate(r, 2f); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.CLOUD, e.getX() + FxParticles.j(r) * 0.3, e.getY() + r.nextDouble() * e.getBbHeight(),
                            e.getZ() + FxParticles.j(r) * 0.3, 0, 0.01, 0);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), p = Mth.clamp(t / 10f, 0, 1), a = 1 - p;
            float fl = Mth.clamp(1 - t / 4f, 0, 1);
            d.glowDisc(at.x, at.y, at.z, 0.35f + 0.4f * fl, SNAP, 0.8f * fl * d.near(at.x, at.y, at.z, 0.6f));
            if (fl > 0.05f) d.star(at.x, at.y, at.z, 0.9f * fl, 0.4f, 0xFFFFFF, fl);
            // The ring of force going out level from his hand.
            d.ring(at.x, at.y, at.z, 0, 1, 0, 0.4f + 5.5f * p, 0.7f + 6f * p, SNAP, 0.45f * a * a, true);
            Minecraft mc = Minecraft.getInstance();
            Entity e = shoved >= 0 && mc.level != null ? mc.level.getEntity(shoved) : null;
            if (e != null && t < 8) {
                Vec3 to = e.getPosition(d.partial).add(0, e.getBbHeight() * 0.6, 0);
                float k = 1 - t / 8f;
                d.softBeam(at.x, at.y, at.z, to.x, to.y, to.z, 0.25f, SNAP, 0.5f * k, 0.2f * k, true);
                d.ring(to.x, to.y, to.z, 0, 1, 0, 0.3f + 1.2f * (1 - k), 0.5f + 1.4f * (1 - k), SNAP, 0.5f * k, true);
            }
        }
    }

    // --- a seal breaking ------------------------------------------------------------------------------------------------

    static final class SealBreak extends MagicEffect {
        private static final int HOLD = 14;
        private final int seal;
        private final long seed;

        SealBreak(Vec3 over, int seal, int ticks) {
            super(over.add(0, 3.0, 0), School.INFERNAL, ticks + HOLD);
            this.seal = seal;
            this.seed = seedOf(over) ^ seal;
        }

        @Override
        protected double reach() {
            return 8;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == HOLD) {
                MagicShake.kick(at, 0.5f, 10, 24);
                MagicOverlay.flash(at, RED, 0.25f, 20);
                for (int i = FxParticles.n(30); i > 0; i--) {
                    FxParticles.ember(level, i % 3 == 0 ? RED_HOT : WAX, 0.7f, 24, at.x, at.y, at.z, FxParticles.j(r) * 0.25, FxParticles.j(r) * 0.2,
                            FxParticles.j(r) * 0.25);
                }
                for (int i = FxParticles.n(18); i > 0; i--) redSmoke(level, at.x, at.y, at.z, FxParticles.j(r) * 0.1, 0.02, FxParticles.j(r) * 0.1, false);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 e = d.eye();
            // The seal faces the camera (turned about the vertical), a metre and a half across.
            double dx = e.x - at.x, dz = e.z - at.z, dl = Math.max(1e-4, Math.sqrt(dx * dx + dz * dz));
            float rx = (float) (-dz / dl), rz = (float) (dx / dl);
            float grow = Mth.clamp(t / 6f, 0, 1), s = 0.75f * (0.6f + 0.4f * grow);
            if (t < HOLD) {
                int state = t < HOLD - 5 ? 0 : 1;
                float jx = state == 1 ? 0.03f * Mth.sin(t * 9) : 0;
                d.glowDisc(at.x, at.y, at.z, 1.3f, RED, 0.35f * grow);
                sheet(d, at.x + rx * jx, at.y, at.z + rz * jx, rx * s, 0, rz * s, 0, s, 0, state * 24, 88, state * 24 + 24, 112, alpha(grow, 0xFFFFFF));
                return;
            }
            // Burst: four quarters of the seal flying out and falling, turning, fading.
            float lt = t - HOLD, a = 1 - Mth.clamp(lt / (life - HOLD), 0, 1);
            d.glowDisc(at.x, at.y, at.z, 1.6f * (1 - a) + 0.4f, RED_HOT, 0.6f * a * a);
            java.util.Random r = new java.util.Random(seed);
            for (int q = 0; q < 4; q++) {
                float qx = q % 2 == 0 ? -1 : 1, qy = q < 2 ? 1 : -1;
                float sp = 0.06f + r.nextFloat() * 0.05f, spin = (r.nextFloat() - 0.5f) * 0.4f;
                double x = at.x + rx * qx * (s * 0.5f + lt * sp), z = at.z + rz * qx * (s * 0.5f + lt * sp);
                double y = at.y + qy * s * 0.5f + lt * 0.05 - lt * lt * 0.006;
                float c = Mth.cos(lt * spin), sn = Mth.sin(lt * spin), h = s * 0.5f;
                float u0 = 48 + (qx < 0 ? 0 : 12), v0 = 88 + (qy > 0 ? 0 : 12);
                sheet(d, x, y, z, rx * h * c, h * sn, rz * h * c, -rx * h * sn, h * c, -rz * h * sn, u0, v0, u0 + 12, v0 + 12, alpha(a, 0xFFFFFF));
            }
        }
    }

    // --- the hounds -----------------------------------------------------------------------------------------------------

    static final class Hounds extends MagicEffect {
        private final int count, scorch;
        private final double gy;
        private final long seed;

        Hounds(Vec3 at, int count, int ticks) {
            super(at, School.INFERNAL, ticks + 40);
            this.count = Math.min(6, count);
            this.scorch = ticks;
            this.gy = groundY(at);
            this.seed = seedOf(at);
        }

        @Override
        protected double reach() {
            return 14;
        }

        @Override
        protected double floor(ClientLevel level) {
            return gy;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            if (age == 0) {
                MagicShake.kick(at, 0.3f, 8, 16);
                for (int i = FxParticles.n(20); i > 0; i--) {
                    FxParticles.plain(level, ParticleTypes.FLAME, at.x + FxParticles.j(r) * 1.2, gy + 0.1, at.z + FxParticles.j(r) * 1.2,
                            FxParticles.j(r) * 0.05, 0.1, FxParticles.j(r) * 0.05);
                }
            }
            if (age < scorch && r.nextFloat() < 0.5f) {
                FxParticles.plain(level, ParticleTypes.SMOKE, at.x + FxParticles.j(r) * 1.5, gy + 0.1, at.z + FxParticles.j(r) * 1.5, 0, 0.03, 0);
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            float a = Mth.clamp(t / 5f, 0, 1) * (1 - Mth.clamp((t - scorch) / 30f, 0, 1));
            float flick = 0.75f + 0.25f * Mth.sin(t * 0.9f);
            d.darkRing(at.x, gy + 0.02, at.z, 0, 1.8f, SCORCH, 0.7f * a);
            d.flatGlow(at.x, gy + 0.03, at.z, 1.5f, EMBER, 0.3f * a * flick);
            d.cracks(at.x, gy + 0.04, at.z, 7, 2.2f, 0.06f, seed, EMBER, 0.6f * a * flick);
            // Burning paw prints running out from it, one trail per hound, appearing stride by stride.
            java.util.Random r = new java.util.Random(seed);
            for (int h = 0; h < count; h++) {
                float ang = r.nextFloat() * Mth.TWO_PI, curve = (r.nextFloat() - 0.5f) * 0.08f;
                for (int s = 0; s < 14; s++) {
                    float appear = 4 + s * 2.2f;
                    if (t < appear) break;
                    float fade = 1 - Mth.clamp((t - appear - 30) / 40f, 0, 1);
                    float dist = 1.4f + s * 0.75f, aa = ang + curve * s;
                    float side = (s % 2 == 0 ? 0.18f : -0.18f);
                    double px = at.x + Mth.cos(aa) * dist - Mth.sin(aa) * side, pz = at.z + Mth.sin(aa) * dist + Mth.cos(aa) * side;
                    float hot = Mth.clamp(1 - (t - appear) / 25f, 0, 1);
                    d.darkRing(px, gy + 0.025, pz, 0, 0.24f, SCORCH, 0.85f * fade * a);
                    d.flatGlow(px, gy + 0.035, pz, 0.3f, hot > 0.3f ? FIRE_HOT : EMBER, (0.35f + 0.65f * hot) * fade * a);
                }
            }
        }
    }

    // --- the collection mark --------------------------------------------------------------------------------------------

    static final class Collect extends MagicEffect {
        private final int victim;
        private final int last;

        Collect(int victim, Vec3 at, int ticks) {
            super(at, School.INFERNAL, ticks);
            this.victim = victim;
            this.last = ticks;
        }

        private Vec3 where(float partial) {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level == null || victim < 0 ? null : mc.level.getEntity(victim);
            return e != null ? e.getPosition(partial) : at;
        }

        @Override
        protected Vec3 anchor() {
            return where(0);
        }

        @Override
        protected double reach() {
            return 4;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource r = level.random;
            Vec3 p = where(0);
            if (r.nextFloat() < 0.4f) {
                double a = r.nextDouble() * Mth.TWO_PI;
                FxParticles.ember(level, RED_HOT, 0.4f, 20, p.x + Math.cos(a) * 0.6, p.y + 2.4, p.z + Math.sin(a) * 0.6, 0, -0.03, 0);
            }
            if (age % 20 == 0) MagicSounds.play(AllSounds.crowley("hounds"), p, 0.25f, 0.8f);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d);
            Vec3 p = where(d.partial);
            float a = Mth.clamp(t / 8f, 0, 1) * (1 - Mth.clamp((t - last + 10) / 10f, 0, 1));
            float beat = 0.7f + 0.3f * Mth.sin(t * 0.35f);
            Minecraft mc = Minecraft.getInstance();
            boolean self = mc.player != null && mc.player.getId() == victim && mc.options.getCameraType().isFirstPerson();
            double y = p.y + 2.5;
            if (!self) {
                // A turning crossroads sigil over the head: a spiked ring, a ring, a cross through it.
                d.spikedRing(p.x, y, p.z, 0.42f, 0.14f, 8, t * 0.05f, RED, 0.7f * a * beat);
                d.ring(p.x, y, p.z, 0, 1, 0, 0.5f, 0.56f, RED_HOT, 0.6f * a, false);
                for (int i = 0; i < 2; i++) {
                    float ang = t * 0.05f + i * Mth.HALF_PI;
                    d.ribbon(p.x - Mth.cos(ang) * 0.5, y, p.z - Mth.sin(ang) * 0.5, p.x + Mth.cos(ang) * 0.5, y, p.z + Mth.sin(ang) * 0.5,
                            0.05f, RED_HOT, 0.6f * a, 0.6f * a);
                }
                d.glowDisc(p.x, y, p.z, 0.6f, RED, 0.25f * a * beat * d.near(p.x, y, p.z, 0.6f));
            }
            // The ring under their feet: the hounds know where to go.
            double gy = groundY(p);
            d.groundRing(p.x, gy + 0.03, p.z, 0.7f, 0.85f, RED, 0.5f * a * beat, true);
            d.darkRing(p.x, gy + 0.02, p.z, 0.6f, 0.95f, SCORCH, 0.35f * a);
        }
    }
}
