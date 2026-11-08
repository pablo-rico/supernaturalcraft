package org.papiricoh.supernaturalcraft.client.amara;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;

/**
 * Her fight's living sky (v0.42), by {@link AmaraLook#skyStage()} as {@link ClientAmara} blends it, drawn after the sky (called
 * from {@code DyingSunSky}'s {@code AFTER_SKY} pass, so it lies over the dying sun):
 * <ol>
 *   <li>the black sun: a black disc high in the sky, its corona of white-violet streamers breathing (III-IV);</li>
 *   <li>the eclipse with a face: a vast black disc overhead, a corona as wide as the sky, her face in it with its eyes shut (V);</li>
 *   <li>the same, the eyes open, violet-white, watching (V, under half);</li>
 *   <li>the sun dying: the black sun again, its corona guttering ember-red as the countdown runs out, the stars going out one by
 *   one (VI);</li>
 *   <li>dawn: Chuck's light breaking the disc, rays of gold-white bursting round it, the dark lifting (the reconciliation).</li>
 * </ol>
 * The pull toward the black sun ({@code GRAVITY}) adds a turning vortex round it; the gutter pulse dims it all. Directions are
 * fixed in the world (the sun stands where the arena's sky is), so it never swings with the camera.
 */
public final class AmaraSky {

    /** Where the black sun and the face stand (unit vectors), how big (at distance 100). */
    static final Vector3f SUN_DIR = new Vector3f(0.32f, 0.62f, -0.72f).normalize(), FACE_DIR = new Vector3f(0.12f, 0.86f, -0.5f).normalize();
    static final float SUN_R = 13f, FACE_R = 44f, DIST = 100f;
    static final int STARS = 900;
    private static final float[] STAR_DATA = buildStars();

    private AmaraSky() {
    }

    public static void render(RenderLevelStageEvent event, float partial) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !SNClientConfig.ECLIPSE_SKY.get()) return;
        AmaraLook look = ClientAmara.current();
        float w1 = ClientAmara.skyWeight(1, partial), w2 = ClientAmara.skyWeight(2, partial), w3 = ClientAmara.skyWeight(3, partial),
                w4 = ClientAmara.skyWeight(4, partial), w5 = ClientAmara.skyWeight(5, partial);
        float pull = ClientAmara.gravity(partial);
        if (w1 + w2 + w3 + w4 + w5 < 0.005f && pull < 0.005f) return;
        float countdown = look == null ? 1f : Mth.clamp(look.countdown(), 0, 1);
        boolean rage = look != null && look.enraged();
        float time = (level.getGameTime() + partial) / 20f;
        float gutter = ClientAmara.gutter(partial);

        PoseStack pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        Matrix4f m = pose.last().pose();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        // The vault darkens under her.
        float vault = Math.max(Math.max(w1 * 0.6f, (w2 + w3) * 0.86f), w4 * (0.72f + 0.25f * (1 - countdown))) + gutter * 0.25f;
        org.papiricoh.supernaturalcraft.client.dyingsun.DyingSunSky.vault(m, Mth.clamp(vault, 0, 0.97f), w4);
        // The stars: many under the face, few round the black sun, going out in VI, fading at dawn.
        float starA = w1 * 0.5f + (w2 + w3) * 1f + w4 * 0.8f + w5 * 0.35f;
        additive();
        stars(m, starA, w4 > 0.01f ? 1 - countdown : 0, time);
        // The black sun (I, IV, V's dawn).
        float sunW = w1 + w4 + w5;
        if (sunW > 0.005f) {
            Basis b = new Basis(SUN_DIR);
            float r = SUN_R * (1 + 0.15f * w4 * (1 - countdown));
            int coronaRgb = rage ? (Mth.sin(time * 6) > 0 ? 0xFF2E5E : 0xB040FF) : lerp(lerp(0xE6DCFF, 0xFF6A2A, w4 / Math.max(0.01f, sunW)), 0xFFF0C8, w5 / Math.max(0.01f, sunW));
            float gutterFlicker = w4 > 0.01f ? 0.55f + 0.45f * countdown * (0.7f + 0.3f * Mth.sin(time * 9.3f) * Mth.sin(time * 3.1f)) : 1f;
            additive();
            glow(m, b, r * 4.2f, coronaRgb, 0.28f * sunW * gutterFlicker * (1 - gutter * 0.6f));
            corona(m, b, r, r * (2.1f + 0.6f * w5), 28, coronaRgb, 0.85f * sunW * gutterFlicker * (1 - gutter * 0.5f), time, 1.6f);
            if (w5 > 0.01f) dawnRays(m, b, r, w5, time);
            if (pull > 0.01f) vortex(m, b, r, pull, time);
            normal();
            // The disc itself; at dawn the light eats in from one side.
            disc(m, b, r * (1 - 0.35f * w5), 0x020104, 0x1A0E2A, sunW * (1 - 0.8f * w5), w5 * r * 0.9f);
        }
        // The eclipse with a face (V).
        float faceW = w2 + w3;
        if (faceW > 0.005f) {
            Basis b = new Basis(FACE_DIR);
            additive();
            glow(m, b, FACE_R * 2.2f, 0x6A3FA8, 0.35f * faceW);
            corona(m, b, FACE_R, FACE_R * 1.7f, 64, 0xD8CCFF, 0.7f * faceW * (1 - gutter * 0.5f), time * 0.4f, 1.1f);
            if (pull > 0.01f) vortex(m, b, FACE_R, pull, time);
            normal();
            disc(m, b, FACE_R, 0x020104, 0x140A22, faceW, 0);
            face(m, b, w2 / Math.max(0.01f, faceW), w3 / Math.max(0.01f, faceW), faceW, time);
        }
        // The pull without a sun to pull toward (a preview, a sky between stages): a vortex at the zenith.
        if (pull > 0.01f && sunW + faceW < 0.01f) {
            additive();
            vortex(m, new Basis(FACE_DIR), FACE_R * 0.6f, pull, time);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // --- pieces ----------------------------------------------------------------------------------------------------------

    /** A disc's frame: its centre on the dome and two axes across it. */
    record Basis(Vector3f c, Vector3f u, Vector3f v) {
        Basis(Vector3f dir) {
            this(new Vector3f(dir).mul(DIST), axis(dir, true), axis(dir, false));
        }

        static Vector3f axis(Vector3f dir, boolean first) {
            Vector3f u = new Vector3f(dir).cross(0, 1, 0);
            if (u.lengthSquared() < 1e-4f) u.set(1, 0, 0);
            u.normalize();
            return first ? u : new Vector3f(u).cross(dir).normalize().negate();
        }

        Vector3f at(float x, float y) {
            return new Vector3f(c).add(new Vector3f(u).mul(x)).add(new Vector3f(v).mul(y));
        }
    }

    private static void additive() {
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private static void normal() {
        RenderSystem.defaultBlendFunc();
    }

    private static void put(BufferBuilder b, Matrix4f m, Vector3f p, int rgb, float a) {
        b.addVertex(m, p.x, p.y, p.z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, (int) (Mth.clamp(a, 0, 1) * 255));
    }

    /** A black disc with a faint rim; {@code bite} > 0 cuts light into it from the lower left (the dawn breaking it). */
    private static void disc(Matrix4f m, Basis b, float r, int core, int rim, float a, float bite) {
        if (a <= 0.005f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int n = 64;
        Vector3f centre = b.at(bite * 0.5f, bite * 0.5f);
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            put(bb, m, centre, core, a);
            put(bb, m, b.at(bite * 0.5f + Mth.cos(a0) * r, bite * 0.5f + Mth.sin(a0) * r), rim, a);
            put(bb, m, b.at(bite * 0.5f + Mth.cos(a1) * r, bite * 0.5f + Mth.sin(a1) * r), rim, a);
        }
        draw(bb);
    }

    /** A soft round glow round a disc. */
    private static void glow(Matrix4f m, Basis b, float r, int rgb, float a) {
        if (a <= 0.005f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        put(bb, m, b.c(), rgb, a);
        for (int i = 0; i <= 40; i++) {
            float t = Mth.TWO_PI * i / 40;
            put(bb, m, b.at(Mth.cos(t) * r, Mth.sin(t) * r), rgb, 0);
        }
        draw(bb);
    }

    /** Streamers of corona from the rim outward, each breathing on its own. */
    private static void corona(Matrix4f m, Basis b, float r0, float r1, int n, int rgb, float a, float time, float width) {
        if (a <= 0.005f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        RandomSource rnd = RandomSource.create(77L);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + rnd.nextFloat() * 0.1f;
            float len = r0 + (r1 - r0) * (0.45f + 0.55f * rnd.nextFloat()) * (0.8f + 0.2f * Mth.sin(time * (0.6f + rnd.nextFloat()) + i));
            float half = width * Mth.TWO_PI / n * (0.7f + rnd.nextFloat() * 0.6f);
            float bend = 0.08f * Mth.sin(time * 0.5f + i * 1.7f);
            Vector3f l = b.at(Mth.cos(ang - half) * r0 * 0.98f, Mth.sin(ang - half) * r0 * 0.98f);
            Vector3f rr = b.at(Mth.cos(ang + half) * r0 * 0.98f, Mth.sin(ang + half) * r0 * 0.98f);
            Vector3f tip = b.at(Mth.cos(ang + bend) * len, Mth.sin(ang + bend) * len);
            put(bb, m, l, rgb, a);
            put(bb, m, rr, rgb, a);
            put(bb, m, tip, rgb, 0);
        }
        // An inner ring of light hugging the rim.
        int ring = 64;
        for (int i = 0; i < ring; i++) {
            float a0 = Mth.TWO_PI * i / ring, a1 = Mth.TWO_PI * (i + 1) / ring;
            Vector3f i0 = b.at(Mth.cos(a0) * r0, Mth.sin(a0) * r0), i1 = b.at(Mth.cos(a1) * r0, Mth.sin(a1) * r0);
            Vector3f o0 = b.at(Mth.cos(a0) * r0 * 1.18f, Mth.sin(a0) * r0 * 1.18f), o1 = b.at(Mth.cos(a1) * r0 * 1.18f, Mth.sin(a1) * r0 * 1.18f);
            put(bb, m, i0, 0xFFFFFF, a);
            put(bb, m, i1, 0xFFFFFF, a);
            put(bb, m, o1, rgb, 0);
            put(bb, m, i0, 0xFFFFFF, a);
            put(bb, m, o1, rgb, 0);
            put(bb, m, o0, rgb, 0);
        }
        draw(bb);
    }

    /** Chuck's light breaking out round the disc: long rays of gold-white. */
    private static void dawnRays(Matrix4f m, Basis b, float r, float w, float time) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int n = 18;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + time * 0.03f;
            float len = r * (3 + 4 * w) * (0.85f + 0.15f * Mth.sin(time + i));
            float half = 0.05f;
            put(bb, m, b.at(Mth.cos(ang - half) * r, Mth.sin(ang - half) * r), 0xFFF6DC, 0.7f * w);
            put(bb, m, b.at(Mth.cos(ang + half) * r, Mth.sin(ang + half) * r), 0xFFF6DC, 0.7f * w);
            put(bb, m, b.at(Mth.cos(ang) * len, Mth.sin(ang) * len), 0xFFD58A, 0);
        }
        draw(bb);
        glow(m, b, r * 6, 0xFFD58A, 0.35f * w);
    }

    /** Spiral arms turning in toward the sun: everything is pulled up to it. */
    private static void vortex(Matrix4f m, Basis b, float r, float k, float time) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        int arms = 5, segs = 24;
        for (int arm = 0; arm < arms; arm++) {
            for (int s = 0; s < segs; s++) {
                float u0 = s / (float) segs, u1 = (s + 1) / (float) segs;
                float a0 = arm * Mth.TWO_PI / arms + time * 1.2f + u0 * 4.5f, a1 = arm * Mth.TWO_PI / arms + time * 1.2f + u1 * 4.5f;
                float d0 = r * (1.1f + 3.5f * u0), d1 = r * (1.1f + 3.5f * u1);
                float w0 = r * 0.25f * (1 - u0 * 0.6f), w1 = r * 0.25f * (1 - u1 * 0.6f);
                Vector3f p0 = b.at(Mth.cos(a0) * d0, Mth.sin(a0) * d0), p1 = b.at(Mth.cos(a1) * d1, Mth.sin(a1) * d1);
                Vector3f q0 = b.at(Mth.cos(a0) * (d0 + w0), Mth.sin(a0) * (d0 + w0)), q1 = b.at(Mth.cos(a1) * (d1 + w1), Mth.sin(a1) * (d1 + w1));
                float al0 = k * 0.45f * (1 - u0), al1 = k * 0.45f * (1 - u1);
                put(bb, m, p0, 0x8A5AE0, al0);
                put(bb, m, p1, 0x8A5AE0, al1);
                put(bb, m, q1, 0x2A1240, 0);
                put(bb, m, p0, 0x8A5AE0, al0);
                put(bb, m, q1, 0x2A1240, 0);
                put(bb, m, q0, 0x2A1240, 0);
            }
        }
        draw(bb);
    }

    /**
     * Her face in the disc: two eyes (shut: glowing lid lines with lashes; open: violet-white almonds with black irises) and
     * the faint line of a mouth. {@code shut}/{@code open} share the weight while the eyes open.
     */
    private static void face(Matrix4f m, Basis b, float shut, float open, float a, float time) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        additive();
        float ex = FACE_R * 0.34f, ey = FACE_R * 0.12f, ew = FACE_R * 0.2f, eh = FACE_R * 0.085f;
        float blink = open > 0.5f ? (Mth.sin(time * 0.35f) > 0.985f ? 0.15f : 1f) : 1f;
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int side = -1; side <= 1; side += 2) {
            float cx = side * ex, cy = ey;
            int n = 24;
            for (int i = 0; i < n; i++) {
                float t0 = i / (float) n, t1 = (i + 1) / (float) n;
                float x0 = cx + (t0 * 2 - 1) * ew, x1 = cx + (t1 * 2 - 1) * ew;
                float s0 = Mth.sin(t0 * Mth.PI), s1 = Mth.sin(t1 * Mth.PI);
                // Shut: a thin arc, the lid, curving down.
                float lid0 = cy - s0 * eh * 0.5f, lid1 = cy - s1 * eh * 0.5f;
                quad(bb, m, b, x0, lid0 - 0.5f, x1, lid1 - 0.5f, x1, lid1 + 0.5f, x0, lid0 + 0.5f, 0xC8B4FF, a * shut * 0.9f);
                if (i % 4 == 2) {
                    float lx = (x0 + x1) / 2, ly = (lid0 + lid1) / 2;
                    quad(bb, m, b, lx - 0.25f, ly, lx + 0.25f, ly, lx + 0.25f + side * 0.3f, ly - 2.2f, lx - 0.25f + side * 0.3f, ly - 2.2f,
                            0x8A70C0, a * shut * 0.7f);
                }
                // Open: an almond, upper and lower lids apart.
                float h0 = s0 * eh * blink, h1 = s1 * eh * blink;
                quad(bb, m, b, x0, cy - h0, x1, cy - h1, x1, cy + h1, x0, cy + h0, 0xE6DCFF, a * open * 0.85f);
            }
        }
        draw(bb);
        normal();
        if (open > 0.01f) {
            // The irises: black, ringed violet, looking down at the arena.
            for (int side = -1; side <= 1; side += 2) {
                Basis eye = new Basis(new Vector3f(b.c()).add(new Vector3f(b.u()).mul(side * ex)).add(new Vector3f(b.v()).mul(ey - eh * 0.25f)),
                        b.u(), b.v());
                disc(m, eye, eh * 0.95f * blink, 0x000000, 0x6A3FA8, a * open, 0);
            }
        }
        // The mouth: a faint dark-violet curve under them.
        additive();
        BufferBuilder mb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float my = -FACE_R * 0.38f, mw = FACE_R * 0.22f;
        int n = 16;
        for (int i = 0; i < n; i++) {
            float t0 = i / (float) n, t1 = (i + 1) / (float) n;
            float x0 = (t0 * 2 - 1) * mw, x1 = (t1 * 2 - 1) * mw;
            float y0 = my + Mth.sin(t0 * Mth.PI) * 1.4f, y1 = my + Mth.sin(t1 * Mth.PI) * 1.4f;
            quad(mb, m, b, x0, y0 - 0.35f, x1, y1 - 0.35f, x1, y1 + 0.35f, x0, y0 + 0.35f, 0x6A3FA8, a * 0.5f);
        }
        draw(mb);
        normal();
    }

    private static void quad(BufferBuilder bb, Matrix4f m, Basis b, float x0, float y0, float x1, float y1, float x2, float y2, float x3,
                             float y3, int rgb, float a) {
        Vector3f p0 = b.at(x0, y0), p1 = b.at(x1, y1), p2 = b.at(x2, y2), p3 = b.at(x3, y3);
        put(bb, m, p0, rgb, a);
        put(bb, m, p1, rgb, a);
        put(bb, m, p2, rgb, a);
        put(bb, m, p0, rgb, a);
        put(bb, m, p2, rgb, a);
        put(bb, m, p3, rgb, a);
    }

    private static float[] buildStars() {
        RandomSource r = RandomSource.create(1111L);
        float[] out = new float[STARS * 5];
        for (int i = 0; i < STARS; i++) {
            float x = r.nextFloat() * 2 - 1, y = r.nextFloat() * 1.6f - 0.4f, z = r.nextFloat() * 2 - 1;
            float l = Mth.sqrt(x * x + y * y + z * z);
            if (l < 0.1f || l > 1f) {
                i--;
                continue;
            }
            out[i * 5] = x / l;
            out[i * 5 + 1] = y / l;
            out[i * 5 + 2] = z / l;
            out[i * 5 + 3] = 0.12f + r.nextFloat() * 0.22f;
            out[i * 5 + 4] = r.nextFloat();
        }
        return out;
    }

    /** Stars; {@code gone} of them (0..1, by each star's own threshold) have gone out. */
    private static void stars(Matrix4f m, float a, float gone, float time) {
        if (a <= 0.005f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int drawn = 0;
        for (int i = 0; i < STARS; i++) {
            int o = i * 5;
            if (STAR_DATA[o + 4] < gone) continue;
            float k = Mth.clamp(a, 0, 1) * (0.55f + 0.45f * Mth.sin(time * 1.1f + i * 0.7f));
            float s = STAR_DATA[o + 3];
            Vector3f c = new Vector3f(STAR_DATA[o], STAR_DATA[o + 1], STAR_DATA[o + 2]).mul(DIST);
            Vector3f t1 = new Vector3f(c).cross(Math.abs(STAR_DATA[o + 1]) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0)).normalize().mul(s);
            Vector3f t2 = new Vector3f(c).cross(t1).normalize().mul(s);
            int al = (int) (k * 255);
            b.addVertex(m, c.x - t1.x - t2.x, c.y - t1.y - t2.y, c.z - t1.z - t2.z).setColor(236, 230, 255, al);
            b.addVertex(m, c.x + t1.x - t2.x, c.y + t1.y - t2.y, c.z + t1.z - t2.z).setColor(236, 230, 255, al);
            b.addVertex(m, c.x + t1.x + t2.x, c.y + t1.y + t2.y, c.z + t1.z + t2.z).setColor(236, 230, 255, al);
            b.addVertex(m, c.x - t1.x + t2.x, c.y - t1.y + t2.y, c.z - t1.z + t2.z).setColor(236, 230, 255, al);
            drawn++;
        }
        if (drawn > 0) draw(b);
        else b.build();
    }

    static int lerp(int a, int b, float k) {
        k = Mth.clamp(k, 0, 1);
        return (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255) << 16 | (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255) << 8
                | (int) Mth.lerp(k, a & 255, b & 255);
    }

    private static void draw(BufferBuilder b) {
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
