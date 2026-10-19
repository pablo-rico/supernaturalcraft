package org.papiricoh.supernaturalcraft.client.witchboss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxParticles;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxWorld;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicEffect;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicFxManager;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * What the witch bosses' world effects share (v0.21, P6): ground lookups, a seeded hash, glowing threads along a curve, paper
 * pages drawn as real (lit, translucent) quads, and the yellow smoke gathering where one of them is about to step out of it.
 */
final class WitchFxKit {

    static final int MAGENTA = 0xE0309A, MAGENTA_HOT = 0xFF9AD8, VIOLET = 0xB36BFF, VIOLET_HOT = 0xE6CCFF, YELLOW = 0xF2D22E,
            YELLOW_HOT = 0xFFF0A0, SMOKE_YELLOW = 0x6E5E22, GOLD = 0xFFD86A, GOLD_HOT = 0xFFF4D0, CRIMSON = 0xC0102A,
            CRIMSON_HOT = 0xFF5A6A, SAFE = 0x7CFF9A, SAFE_HOT = 0xD8FFE0, RED = 0xFF3A1A, FIRE = 0xFF5A1F, FIRE_HOT = 0xFFC070,
            ICE = 0xBFE8FF, ICE_DEEP = 0x5FA8F0, PAPER = 0xEFE2C4;

    static final ResourceLocation PAGE = SupernaturalCraft.asResource("textures/particle/page_0.png");
    static final ResourceLocation PAGE_SCRAP = SupernaturalCraft.asResource("textures/particle/page_scrap_0.png");

    private WitchFxKit() {
    }

    static @Nullable Entity entity(int id) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null || id < 0 ? null : mc.level.getEntity(id);
    }

    static double groundY(Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? at.y : FxWorld.floor(mc.level, at.add(0, 1.5, 0), 6);
    }

    static BlockState ground(ClientLevel level, double x, double y, double z) {
        BlockState s = level.getBlockState(BlockPos.containing(x, y - 0.5, z));
        return s.isAir() ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : s;
    }

    /** A cheap random fixed by a seed. */
    static float hash(long seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    static long seedOf(Vec3 at) {
        return Double.doubleToLongBits(at.x * 31.7 + at.z * 17.3 + at.y);
    }

    static Vec3 bezier(Vec3 a, Vec3 c, Vec3 b, double u) {
        double v = 1 - u;
        return a.scale(v * v).add(c.scale(2 * v * u)).add(b.scale(u * u));
    }

    /**
     * A glowing thread along the curve a → c → b, drawn from {@code u0} to {@code u1} of the way: a soft sheath round a bright core,
     * with beads of light flowing along it at {@code flow} (turns per tick; negative flows back toward a).
     */
    static void thread(FxDraw d, Vec3 a, Vec3 c, Vec3 b, float u0, float u1, float width, int rgb, int hot, float alpha, float t, float flow) {
        if (alpha <= 0.004f || u1 <= u0) return;
        int n = 14;
        Vec3 prev = null;
        for (int i = 0; i <= n; i++) {
            float u = Mth.lerp(i / (float) n, u0, u1);
            Vec3 p = bezier(a, c, b, u);
            if (prev != null) {
                d.softBeam(prev.x, prev.y, prev.z, p.x, p.y, p.z, width * 2.6f, rgb, alpha * 0.35f, alpha * 0.35f, false);
                d.softBeam(prev.x, prev.y, prev.z, p.x, p.y, p.z, width, hot, alpha * 0.8f, alpha * 0.8f, false);
            }
            prev = p;
        }
        if (flow != 0) {
            for (int k = 0; k < 4; k++) {
                float u = ((t * flow + k * 0.25f) % 1f + 1f) % 1f;
                if (u < u0 || u > u1) continue;
                Vec3 p = bezier(a, c, b, u);
                d.glowDisc(p.x, p.y, p.z, width * 4f, hot, alpha * 0.7f);
            }
        }
    }

    /**
     * A sheet of paper (a page of a grimoire) as a lit, translucent quad: centre {@code c}, its width along {@code (ux, uy, uz)} and
     * its height along {@code (vx, vy, vz)} (half-extents), drawn with the world's light at {@code light}.
     */
    static void paper(FxDraw d, ResourceLocation texture, Vec3 c, float ux, float uy, float uz, float vx, float vy, float vz, int rgb,
                      float alpha, int light) {
        alpha *= d.near(c.x, c.y, c.z, Math.max(Mth.sqrt(ux * ux + uy * uy + uz * uz), Mth.sqrt(vx * vx + vy * vy + vz * vz)));
        if (alpha <= 0.01f) return;
        Vec3 eye = d.eye();
        PoseStack.Pose p = d.pose.last();
        float cx = (float) (c.x - eye.x), cy = (float) (c.y - eye.y), cz = (float) (c.z - eye.z);
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF, a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        // One buffer, fetched and filled in one go (nothing else fetches a buffer while it is held).
        VertexConsumer vc = d.buffers.getBuffer(RenderType.entityTranslucent(texture));
        vertex(vc, p, cx - ux - vx, cy - uy - vy, cz - uz - vz, 0, 1, r, g, b, a, light);
        vertex(vc, p, cx + ux - vx, cy + uy - vy, cz + uz - vz, 1, 1, r, g, b, a, light);
        vertex(vc, p, cx + ux + vx, cy + uy + vy, cz + uz + vz, 1, 0, r, g, b, a, light);
        vertex(vc, p, cx - ux + vx, cy - uy + vy, cz - uz + vz, 0, 0, r, g, b, a, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int r, int g, int b, int a,
                               int light) {
        vc.addVertex(p, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }

    /** The packed light at {@code at} (block and sky), for paper drawn there. */
    static int light(Vec3 at) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0xF000F0;
        return net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, BlockPos.containing(at));
    }

    // --- the yellow smoke gathering -------------------------------------------------------------------------------------

    static void gather(Vec3 at, int ticks, float radius) {
        MagicFxManager.add(new Gather(at, ticks, radius));
    }

    /**
     * Yellow smoke gathering in the circle where a witch will step out of it: wisps spiralling in and up into a column, sulphur
     * sparks in it, a glow pulsing faster as the moment nears; it puffs out as she arrives.
     */
    static final class Gather extends MagicEffect {
        private final int ticks;
        private final float radius;
        private final double gy;

        Gather(Vec3 at, int ticks, float radius) {
            super(at, School.WITCHCRAFT, Math.max(6, ticks) + 10);
            this.ticks = Math.max(6, ticks);
            this.radius = Math.max(1, radius);
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
            float k = Mth.clamp(age / (float) ticks, 0, 1);
            if (age < ticks) {
                for (int i = FxParticles.rate(r, 1.2f + 2f * k); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI, rr = radius * (0.7f + 0.3f * r.nextFloat());
                    double x = at.x + Mth.cos(a) * rr, z = at.z + Mth.sin(a) * rr;
                    // Spiralling in: tangent plus a pull to the middle.
                    double vx = -Mth.sin(a) * 0.08 - Mth.cos(a) * 0.06, vz = Mth.cos(a) * 0.08 - Mth.sin(a) * 0.06;
                    FxParticles.plain(level, AllParticles.YELLOW_SMOKE.get(), x, gy + 0.1 + r.nextDouble() * 0.4, z, vx, 0.03 + k * 0.05, vz);
                }
                if (r.nextFloat() < 0.4f + k * 0.5f) {
                    FxParticles.spark(level, YELLOW_HOT, 0.4f, 10, at.x + FxParticles.j(r) * 0.4, gy + 0.3 + r.nextDouble() * 1.8,
                            at.z + FxParticles.j(r) * 0.4, 0, 0.03, 0);
                }
            }
            if (age == ticks) {
                for (int i = FxParticles.n(18); i > 0; i--) {
                    float a = r.nextFloat() * Mth.TWO_PI;
                    FxParticles.plain(level, AllParticles.YELLOW_SMOKE.get(), at.x, gy + 0.3 + r.nextDouble() * 1.6, at.z, Mth.cos(a) * 0.18,
                            0.02, Mth.sin(a) * 0.18);
                }
            }
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), k = Mth.clamp(t / ticks, 0, 1), out = Mth.clamp((t - ticks) / 10f, 0, 1);
            float a = Mth.clamp(t / 5f, 0, 1) * (1 - out);
            if (a <= 0.01f) return;
            float beat = 0.6f + 0.4f * Mth.sin(t * (0.3f + k * 0.9f));
            d.flatGlow(at.x, gy + 0.03, at.z, radius * 1.1f, YELLOW, 0.35f * a * beat);
            // Wisps winding in toward the column.
            for (int i = 0; i < 9; i++) {
                float u = ((t * 0.04f + i / 9f) % 1f), ang = i * 0.7f + t * 0.12f + u * 4f, rr = radius * (1 - u) * 0.95f + 0.15f;
                double x = at.x + Mth.cos(ang) * rr, z = at.z + Mth.sin(ang) * rr, y = gy + 0.25 + u * (0.6 + 1.4 * k);
                d.darkDisc(x, y, z, 0.35f + 0.25f * (1 - u), SMOKE_YELLOW, 0.35f * a * Mth.sin(Mth.PI * u));
                d.glowDisc(x, y, z, 0.25f, YELLOW, 0.22f * a * Mth.sin(Mth.PI * u));
            }
            // The column thickening as she nears.
            d.softBeam(at.x, gy, at.z, at.x, gy + 2.2, at.z, 0.6f + 0.6f * k, YELLOW, 0.3f * a * k, 0, false);
            d.glowDisc(at.x, gy + 1.0, at.z, 0.8f + 0.6f * k, YELLOW_HOT, 0.25f * a * k * beat);
            if (out > 0) d.groundRing(at.x, gy + 0.04, at.z, radius * (0.6f + out), radius * (0.6f + out) + 0.4f, YELLOW, 0.6f * (1 - out), true);
        }
    }
}
