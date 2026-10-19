package org.papiricoh.supernaturalcraft.client.magic.fx;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.witch.WitchGeo;
import org.papiricoh.supernaturalcraft.entity.witch.CovenWitchEntity;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * A Coven Witch's shield (v0.21): while {@link CovenWitchEntity#shielded()} a magenta bubble of hexagonal cells round her, a
 * wave of light running up through the cells, two bands of glyphs turning against each other and a bright rim. Her WARD_PULSE
 * effects (entity = the witch) play on the bubble instead of the generic dome ({@link #pulse}): raised, it swells in from her
 * hands; struck, it flashes and a crack runs from where the blow landed (cracks gather until it breaks); broken, it shatters —
 * the cells fly out and fall, glyphs scatter, a ring runs along the ground.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ShieldBubbles {

    /** Witches further than this are not drawn; at most this many (the nearest). */
    private static final double RANGE = 48;
    private static final int MAX_DRAWN = 8, MAX_CRACKS = 9;
    private static final int MAGENTA = 0xE04AB8, PALE = 0xFFC8F0, DEEP = 0x8A1E6E;

    /** The cells: a Fibonacci spread of points over the unit sphere. */
    static final int CELLS = 42;
    static final float[] CX = new float[CELLS], CY = new float[CELLS], CZ = new float[CELLS];

    static {
        float golden = Mth.PI * (3 - Mth.sqrt(5));
        for (int i = 0; i < CELLS; i++) {
            float y = 1 - (i + 0.5f) / CELLS * 2, r = Mth.sqrt(1 - y * y), a = golden * i;
            CX[i] = Mth.cos(a) * r;
            CY[i] = y;
            CZ[i] = Mth.sin(a) * r;
        }
    }

    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();
    private static final List<CovenWitchEntity> NEAR = new ArrayList<>();

    private ShieldBubbles() {
    }

    private static final class State {
        float raisedAt = Float.NaN, hitAt = Float.NEGATIVE_INFINITY;
        final List<Crack> cracks = new ArrayList<>();
    }

    /** A crack from the point (x, y, z) on the unit sphere, its path drawn from {@code seed}. */
    record Crack(float x, float y, float z, long seed, float at) {
    }

    private static float now(float partial) {
        return MagicFxManager.clock() + partial;
    }

    /** The bubble's centre and radius for {@code witch}. */
    static float radius(Entity witch) {
        return Math.max(CovenWitchEntity.SHIELD_RADIUS, witch.getBbHeight() * 0.62f);
    }

    /**
     * A WARD_PULSE: if its ward is a Coven Witch, her bubble plays it. @return whether it was hers (the generic dome is then not
     * drawn)
     */
    static boolean pulse(MagicFxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entity()) instanceof CovenWitchEntity witch)) return false;
        State s = STATES.computeIfAbsent(witch.getId(), k -> new State());
        float now = now(0);
        Vec3 c = witch.position().add(0, witch.getBbHeight() / 2, 0);
        switch (p.arg2Index()) {
            case MagicFxPayload.WARD_RAISED -> {
                s.raisedAt = now;
                s.cracks.clear();
                MagicSounds.school(School.WITCHCRAFT, "cast", c, 0.4f, 1.5f);
            }
            case MagicFxPayload.WARD_HIT -> {
                s.hitAt = now;
                // The blow came from the camera's side, most likely: a crack starts there, a little off.
                RandomSource r = mc.level.random;
                Vec3 from = mc.gameRenderer.getMainCamera().getPosition().subtract(c).normalize();
                Vec3 d = from.add(FxParticles.j(r) * 0.6, FxParticles.j(r) * 0.5, FxParticles.j(r) * 0.6).normalize();
                if (s.cracks.size() >= MAX_CRACKS) s.cracks.removeFirst();
                s.cracks.add(new Crack((float) d.x, (float) d.y, (float) d.z, r.nextLong(), now));
                MagicSounds.school(School.WITCHCRAFT, "impact", c, 0.35f, 1.9f);
                float rad = radius(witch);
                for (int i = FxParticles.n(8); i > 0; i--) {
                    FxParticles.spark(mc.level, r.nextBoolean() ? PALE : MAGENTA, 0.8f, 10, c.x + d.x * rad, c.y + d.y * rad, c.z + d.z * rad,
                            d.x * 0.08 + FxParticles.j(r) * 0.05, d.y * 0.08 + FxParticles.j(r) * 0.05, d.z * 0.08 + FxParticles.j(r) * 0.05);
                }
            }
            case MagicFxPayload.WARD_BROKEN -> {
                MagicFxManager.add(new Shatter(witch, s.cracks));
                STATES.remove(witch.getId());
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // --- which witches -----------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.gameRenderer == null) {
            NEAR.clear();
            STATES.clear();
            return;
        }
        if (level.getGameTime() % 10 != 0) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        NEAR.clear();
        NEAR.addAll(level.getEntitiesOfClass(CovenWitchEntity.class, new AABB(cam, cam).inflate(RANGE)));
        NEAR.sort((a, b) -> Double.compare(a.distanceToSqr(cam), b.distanceToSqr(cam)));
        if (NEAR.size() > MAX_DRAWN) NEAR.subList(MAX_DRAWN, NEAR.size()).clear();
        STATES.int2ObjectEntrySet().removeIf(e -> !(level.getEntity(e.getIntKey()) instanceof CovenWitchEntity w) || !w.isAlive());
    }

    static boolean any() {
        return !NEAR.isEmpty();
    }

    // --- drawing -----------------------------------------------------------------------------------------------------------

    static void renderAll(FxDraw d, Minecraft mc) {
        for (CovenWitchEntity w : NEAR) {
            if (w.isRemoved() || !w.shielded() || w.isInvisible() || WitchGeo.vanished(w)) continue;
            if (!MagicFxManager.visible(mc, w.position(), 4)) continue;
            draw(d, w, STATES.get(w.getId()));
        }
        d.floor(Double.NaN);
    }

    private static void draw(FxDraw d, CovenWitchEntity w, State s) {
        Vec3 f = w.getPosition(d.partial);
        float rad = radius(w), now = now(d.partial), t = d.now + w.getId() * 31;
        double cx = f.x, cy = f.y + w.getBbHeight() / 2, cz = f.z;
        d.floor(f.y);
        float grow = s == null || Float.isNaN(s.raisedAt) ? 1 : easeOutBack((now - s.raisedAt) / 10f);
        if (grow <= 0.01f) return;
        float r = rad * grow;
        float hit = s == null ? 0 : Math.max(0, 1 - (now - s.hitAt) / 8f);
        float cracked = s == null ? 0 : s.cracks.size() / (float) MAX_CRACKS;
        // A stressed shield flickers.
        float flick = 1 - cracked * 0.35f * (0.5f + 0.5f * Mth.sin(t * 1.7f) * Mth.sin(t * 0.63f));
        Vec3 eye = d.eye();
        float ex = (float) (eye.x - cx), ey = (float) (eye.y - cy), ez = (float) (eye.z - cz);
        float el = Mth.sqrt(ex * ex + ey * ey + ez * ez);
        if (el > 1e-3f) {
            ex /= el;
            ey /= el;
            ez /= el;
        }
        // Fill and rim.
        d.sphere(cx, cy, cz, r, MAGENTA, (0.045f + 0.14f * hit) * flick);
        d.ring(cx, cy, cz, ex, ey, ez, r * 0.94f, r * 1.02f, MAGENTA, (0.32f + 0.4f * hit) * flick, true);
        d.ring(cx, cy, cz, ex, ey, ez, r * 0.985f, r * 1.0f, PALE, (0.25f + 0.5f * hit) * flick, false);
        // The cells, a wave of light climbing through them.
        if (MagicFxManager.detail() > 0 || el < 12) {
            float spin = t * 0.004f, cs = Mth.cos(spin), sn = Mth.sin(spin);
            float cell = r * 0.27f;
            float[] ax = new float[3], ay = new float[3];
            for (int i = 0; i < CELLS; i++) {
                float nx = CX[i] * cs - CZ[i] * sn, ny = CY[i], nz = CX[i] * sn + CZ[i] * cs;
                float facing = nx * ex + ny * ey + nz * ez;
                float wave = Mth.sin(ny * 3.2f - t * 0.12f + i * 0.05f);
                wave = wave > 0 ? wave * wave * wave : 0;
                float a = (0.16f + 0.5f * wave + 0.35f * hit) * (facing > 0 ? 1 : 0.35f) * flick;
                FxDraw.basis(nx, ny, nz, ax, ay);
                hexagon(d, cx, cy, cz, r, nx, ny, nz, ax, ay, cell, i * 0.37f, wave > 0.5f ? PALE : MAGENTA, a);
            }
        }
        // Two bands of glyphs turning against each other.
        float hb = 0.26f;
        d.band(MagicTextures.glyphStrip(), cx, cy - hb / 2, cz, r * 1.005f, hb, t * 0.025f, MAGENTA, 0.55f * flick, 0.55f * flick);
        double up = r * 0.55;
        float r2 = (float) Math.sqrt(Math.max(0.01, r * r - up * up));
        d.band(MagicTextures.glyphStrip(), cx, cy + up - hb * 0.4, cz, r2 * 1.01f, hb * 0.8f, -t * 0.035f, PALE, 0.4f * flick, 0.2f * flick);
        d.band(MagicTextures.glyphStrip(), cx, cy - up - hb * 0.4, cz, r2 * 1.01f, hb * 0.8f, -t * 0.03f + 1.3f, DEEP, 0.2f * flick, 0.4f * flick);
        // Cracks.
        if (s != null) {
            for (Crack c : s.cracks) {
                float age = now - c.at;
                crack(d, cx, cy, cz, r * 1.005f, c, Mth.clamp(age / 3f, 0, 1), age < 6 ? 1 : 0.75f);
                if (age < 6) d.star(cx + c.x * r, cy + c.y * r, cz + c.z * r, 0.4f, c.seed * 0.1f, PALE, 1 - age / 6f);
            }
        }
        // A faint pool of magenta under her.
        d.groundRing(cx, f.y + 0.04, cz, r * 0.75f, r * 0.98f, MAGENTA, 0.35f * flick, true);
    }

    /** A hexagon of light on the sphere of radius {@code r}, centred on the unit normal n, {@code size} from centre to corner. */
    static void hexagon(FxDraw d, double cx, double cy, double cz, float r, float nx, float ny, float nz, float[] ax, float[] ay, float size,
                        float roll, int rgb, float alpha) {
        if (alpha <= 0.01f) return;
        double px = 0, py = 0, pz = 0;
        for (int k = 0; k <= 6; k++) {
            float th = roll + k * Mth.TWO_PI / 6;
            float vx = nx * r + (ax[0] * Mth.cos(th) + ay[0] * Mth.sin(th)) * size;
            float vy = ny * r + (ax[1] * Mth.cos(th) + ay[1] * Mth.sin(th)) * size;
            float vz = nz * r + (ax[2] * Mth.cos(th) + ay[2] * Mth.sin(th)) * size;
            float l = r / Mth.sqrt(vx * vx + vy * vy + vz * vz);
            double x = cx + vx * l, y = cy + vy * l, z = cz + vz * l;
            if (k > 0) d.ribbon(px, py, pz, x, y, z, 0.028f, rgb, alpha, alpha);
            px = x;
            py = y;
            pz = z;
        }
    }

    /** A jagged crack walking over the sphere from its start, a branch or two off it; {@code grow} 0-1 how far it has run. */
    static void crack(FxDraw d, double cx, double cy, double cz, float r, Crack c, float grow, float alpha) {
        RandomSource rnd = RandomSource.create(c.seed);
        float[] ax = new float[3], ay = new float[3];
        FxDraw.basis(c.x, c.y, c.z, ax, ay);
        for (int branch = 0; branch < 3; branch++) {
            float heading = rnd.nextFloat() * Mth.TWO_PI;
            float px = c.x, py = c.y, pz = c.z;
            int steps = branch == 0 ? 6 : 3;
            float len = branch == 0 ? 0.16f : 0.11f;
            int shown = Math.round(steps * grow);
            for (int i = 0; i < shown; i++) {
                heading += (rnd.nextFloat() - 0.5f) * 1.4f;
                float dx = ax[0] * Mth.cos(heading) + ay[0] * Mth.sin(heading);
                float dy = ax[1] * Mth.cos(heading) + ay[1] * Mth.sin(heading);
                float dz = ax[2] * Mth.cos(heading) + ay[2] * Mth.sin(heading);
                float qx = px + dx * len, qy = py + dy * len, qz = pz + dz * len;
                float l = Mth.sqrt(qx * qx + qy * qy + qz * qz);
                qx /= l;
                qy /= l;
                qz /= l;
                float a = alpha * (1 - i / (float) (steps + 1) * 0.6f);
                d.ribbon(cx + px * r, cy + py * r, cz + pz * r, cx + qx * r, cy + qy * r, cz + qz * r, branch == 0 ? 0.045f : 0.03f, PALE, a, a);
                px = qx;
                py = qy;
                pz = qz;
            }
        }
    }

    private static float easeOutBack(float k) {
        k = Mth.clamp(k, 0, 1) - 1;
        return 1 + k * k * (2.7f * k + 1.7f);
    }

    /** The bubble breaking: a flash, its cells thrown out and falling, glyphs scattering, a ring along the ground. */
    static final class Shatter extends MagicEffect {
        private final float radius, height;
        private final List<Crack> cracks;
        private final float[] speed = new float[CELLS], tumble = new float[CELLS];

        Shatter(CovenWitchEntity w, List<Crack> cracks) {
            super(w.position(), School.WITCHCRAFT, 30);
            this.radius = radius(w);
            this.height = w.getBbHeight();
            this.cracks = List.copyOf(cracks);
            RandomSource r = RandomSource.create(w.getId() * 7919L + w.tickCount);
            for (int i = 0; i < CELLS; i++) {
                speed[i] = 0.05f + r.nextFloat() * 0.07f;
                tumble[i] = (r.nextFloat() - 0.5f) * 0.4f;
            }
        }

        @Override
        protected double reach() {
            return radius + 4;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age != 0) return;
            RandomSource r = level.random;
            Vec3 c = at.add(0, height / 2, 0);
            for (int i = FxParticles.n(30); i > 0; i--) {
                float a = r.nextFloat() * Mth.TWO_PI, up = (r.nextFloat() - 0.4f) * Mth.PI * 0.8f;
                double dx = Mth.cos(a) * Mth.cos(up), dy = Mth.sin(up), dz = Mth.sin(a) * Mth.cos(up);
                FxParticles.glyph(level, r.nextBoolean() ? MAGENTA : PALE, 0.9f, 22 + r.nextInt(10), c.x + dx * radius, c.y + dy * radius,
                        c.z + dz * radius, dx * 0.09, dy * 0.06 + 0.02, dz * 0.09);
            }
            for (int i = FxParticles.n(14); i > 0; i--) {
                FxParticles.spark(level, PALE, 1f, 14, c.x, c.y, c.z, FxParticles.j(r) * 0.25, FxParticles.j(r) * 0.2, FxParticles.j(r) * 0.25);
            }
            MagicShake.kick(c, 0.25f, 7, 10);
            MagicOverlay.flash(c, MAGENTA, 0.25f, 10);
        }

        @Override
        protected void render(FxDraw d) {
            float t = time(d), a = 1 - progress(d);
            double cx = at.x, cy = at.y + height / 2, cz = at.z;
            if (t < 5) {
                float k = 1 - t / 5f;
                d.sphere(cx, cy, cz, radius * (1 + 0.1f * t / 5f), PALE, 0.3f * k);
                d.glowDisc(cx, cy, cz, radius * 2f, MAGENTA, 0.6f * k);
                for (Crack c : cracks) crack(d, cx, cy, cz, radius, c, 1, k);
            }
            float out = easeOut(t / 12f);
            d.groundRing(cx, at.y + 0.04, cz, radius * (0.8f + 1.4f * out), radius * (0.8f + 1.4f * out) + 0.4f, MAGENTA, 0.8f * a, true);
            float[] ax = new float[3], ay = new float[3];
            for (int i = 0; i < CELLS; i++) {
                float nx = CX[i], ny = CY[i], nz = CZ[i];
                float dist = radius + speed[i] * t * 6;
                float fall = t * t * 0.0035f;
                double x = cx + nx * dist, y = cy + ny * dist - fall, z = cz + nz * dist;
                if (y < at.y) continue;
                // Each cell flies as a small flat hexagon tumbling about its own centre.
                float tn = tumble[i] * t;
                float mx = nx + Mth.sin(tn) * 0.6f, my = ny + Mth.cos(tn) * 0.3f, mz = nz - Mth.sin(tn) * 0.6f;
                FxDraw.basis(mx, my, mz, ax, ay);
                float size = radius * 0.22f * (1 - 0.4f * progress(d));
                double px = 0, py = 0, pz = 0;
                for (int k = 0; k <= 6; k++) {
                    float th = k * Mth.TWO_PI / 6;
                    double vx = x + (ax[0] * Mth.cos(th) + ay[0] * Mth.sin(th)) * size;
                    double vy = y + (ax[1] * Mth.cos(th) + ay[1] * Mth.sin(th)) * size;
                    double vz = z + (ax[2] * Mth.cos(th) + ay[2] * Mth.sin(th)) * size;
                    if (k > 0) d.ribbon(px, py, pz, vx, vy, vz, 0.03f, i % 3 == 0 ? PALE : MAGENTA, 0.8f * a, 0.8f * a);
                    px = vx;
                    py = vy;
                    pz = vz;
                }
            }
        }
    }
}
