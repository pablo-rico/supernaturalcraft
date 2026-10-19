package org.papiricoh.supernaturalcraft.client.magic.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.client.render.RitualAltarRenderer;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.RitualChargePayload;
import org.papiricoh.supernaturalcraft.ritual.block.RitualAltarBlockEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A rite charging at an altar ({@link RitualChargePayload}, v0.21). The circle's cells ignite one after another round the altar
 * (from north, clockwise), a spark running ahead along the chalk; a ring of glyphs rises and spins faster over the altar; the
 * offerings circling the altar stream into a point of light above it; a hum swells under it all. The client advances the
 * progress itself; a stop payload fades it out.
 */
public final class RitualChargeFx {

    private static final Map<BlockPos, RitualChargeFx> CHARGES = new HashMap<>();
    /** Fraction of the charge by which every cell is lit (the rest is the rite gathering). */
    private static final float LIGHT_BY = 0.75f;
    private static final int FADE = 20;

    private final BlockPos altar;
    private final School school;
    private final int primary, accent, core;
    /** The cells, sorted by angle round the altar. */
    private final int[] dx, dz;
    private final int[] litAt;
    private final int duration;
    private float progress;
    private int age, stopAge = -1;
    private int lit;

    private RitualChargeFx(RitualChargePayload p) {
        this.altar = p.altar().immutable();
        this.school = p.school();
        this.primary = FxColor.primary(school);
        this.accent = FxColor.accent(school);
        this.core = FxColor.core(school);
        this.duration = Math.max(1, p.duration());
        this.progress = Mth.clamp(p.progress(), 0, 1);
        List<int[]> cells = new ArrayList<>();
        for (short s : p.cellOffsets()) cells.add(new int[]{RitualChargePayload.dx(s), RitualChargePayload.dz(s)});
        cells.sort((a, b) -> {
            int c = Double.compare(angle(a), angle(b));
            return c != 0 ? c : Integer.compare(b[0] * b[0] + b[1] * b[1], a[0] * a[0] + a[1] * a[1]);
        });
        this.dx = new int[cells.size()];
        this.dz = new int[cells.size()];
        for (int i = 0; i < cells.size(); i++) {
            dx[i] = cells.get(i)[0];
            dz[i] = cells.get(i)[1];
        }
        this.litAt = new int[cells.size()];
        Arrays.fill(litAt, -1);
    }

    /** Clockwise from north, in [0, 2π). */
    private static double angle(int[] c) {
        double a = Math.atan2(c[0], -c[1]);
        return a < 0 ? a + Math.PI * 2 : a;
    }

    // --- state ------------------------------------------------------------------------------------------------------------

    static void handle(RitualChargePayload p) {
        if (p.isStop()) {
            RitualChargeFx c = CHARGES.get(p.altar());
            if (c != null && c.stopAge < 0) c.stopAge = 0;
            return;
        }
        RitualChargeFx old = CHARGES.get(p.altar());
        RitualChargeFx fresh = new RitualChargeFx(p);
        // A correction keeps the circle already lit and the hum already playing.
        if (old != null && old.stopAge < 0 && Arrays.equals(old.dx, fresh.dx) && Arrays.equals(old.dz, fresh.dz)) {
            old.progress = fresh.progress;
            return;
        }
        CHARGES.put(fresh.altar, fresh);
        Vec3 c = Vec3.atCenterOf(fresh.altar).add(0, 0.5, 0);
        MagicSounds.loop(fresh.school, "charge", c, () -> 0.25 + 0.65 * fresh.progress,
                () -> CHARGES.get(fresh.altar) == fresh && fresh.stopAge < 0);
    }

    /** How far along the charge at {@code altar} is, or -1 when none is running. */
    public static float channel(BlockPos altar) {
        RitualChargeFx c = CHARGES.get(altar);
        return c == null || c.stopAge >= 0 ? -1 : c.progress;
    }

    static boolean any() {
        return !CHARGES.isEmpty();
    }

    static void clear() {
        CHARGES.clear();
    }

    static void tickAll(ClientLevel level) {
        if (CHARGES.isEmpty()) return;
        CHARGES.values().removeIf(c -> c.tick(level));
    }

    /** @return true when it is over */
    private boolean tick(ClientLevel level) {
        age++;
        if (stopAge >= 0) return ++stopAge > FADE;
        progress = Math.min(1f, progress + 1f / duration);
        if (!level.isLoaded(altar)) return false;
        RandomSource r = level.random;
        int want = dx.length == 0 ? 0 : Math.min(dx.length, (int) Math.ceil(Mth.clamp(progress / LIGHT_BY, 0, 1) * dx.length));
        double y = altar.getY() + 0.1;
        // Each cell that catches flares with a few sparks and a glyph.
        for (; lit < want; lit++) {
            litAt[lit] = age;
            double x = altar.getX() + dx[lit] + 0.5, z = altar.getZ() + dz[lit] + 0.5;
            for (int i = FxParticles.n(3); i > 0; i--) {
                FxParticles.spark(level, core, 0.7f, 0, x, y, z, FxParticles.j(r) * 0.05, 0.05 + r.nextDouble() * 0.05, FxParticles.j(r) * 0.05);
            }
            if (FxParticles.n(1) > 0) FxParticles.glyph(level, primary, 0.9f, 26, x, y + 0.1, z, 0, 0.04, 0);
        }
        // Lit chalk smoulders with glyphs; the air over the altar gathers motes.
        for (int i = FxParticles.rate(r, 0.04f * lit + 0.3f); i > 0 && lit > 0; i--) {
            int k = r.nextInt(lit);
            FxParticles.glyph(level, r.nextInt(4) == 0 ? accent : primary, 0.8f, 30, altar.getX() + dx[k] + 0.5 + FxParticles.j(r) * 0.3, y,
                    altar.getZ() + dz[k] + 0.5 + FxParticles.j(r) * 0.3, 0, 0.03 + r.nextDouble() * 0.03, 0);
        }
        Vec3 focus = focus(0);
        for (int i = FxParticles.rate(r, progress * 2.5f); i > 0; i--) {
            float a = r.nextFloat() * Mth.TWO_PI;
            double d = 2.5 + r.nextDouble() * 1.5;
            FxParticles.mote(level, primary, 0.8f, 20, focus.x + Mth.cos(a) * d, focus.y - 1 + r.nextDouble() * 2, focus.z + Mth.sin(a) * d,
                    -Mth.cos(a) * d / 20, 0, -Mth.sin(a) * d / 20);
        }
        return false;
    }

    private Vec3 focus(float partial) {
        return new Vec3(altar.getX() + 0.5, altar.getY() + 2.6 + progress * 0.8, altar.getZ() + 0.5);
    }

    // --- drawing -------------------------------------------------------------------------------------------------------------

    static void renderAll(FxDraw d) {
        if (CHARGES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        for (RitualChargeFx c : CHARGES.values()) {
            if (!MagicFxManager.visible(mc, Vec3.atCenterOf(c.altar), 8)) continue;
            d.floor(c.altar.getY());
            c.render(d, mc);
        }
    }

    private void render(FxDraw d, Minecraft mc) {
        float t = age + d.partial;
        float master = stopAge >= 0 ? 1 - (stopAge + d.partial) / FADE : Math.min(1, t / 10f);
        if (master <= 0) return;
        float p = stopAge >= 0 ? progress : Math.min(1f, progress + d.partial / duration);
        double y = altar.getY() + 0.075;
        double cx = altar.getX() + 0.5, cz = altar.getZ() + 0.5;
        int n = dx.length;
        float lightK = Mth.clamp(p / LIGHT_BY, 0, 1) * n;
        int full = Math.min(n, (int) lightK);
        // The chalk: every lit cell glowing, freshly lit ones flaring; strokes between neighbours.
        for (int i = 0; i < full; i++) {
            double x = altar.getX() + dx[i] + 0.5, z = altar.getZ() + dz[i] + 0.5;
            float since = litAt[i] < 0 ? 99 : t - litAt[i];
            float flare = Math.max(0, 1 - since / 8f);
            float breathe = 0.75f + 0.25f * Mth.sin(t * 0.15f + i * 0.7f);
            d.flatGlow(x, y, z, 0.75f + 0.4f * flare, primary, (0.45f * breathe + 0.5f * flare) * master);
            if (flare > 0) d.glowDisc(x, y + 0.25, z, 0.6f, core, flare * master);
            int j = i + 1 < full ? i + 1 : (full == n && n > 2 ? 0 : -1);
            if (j >= 0 && Math.abs(dx[j] - dx[i]) <= 1 && Math.abs(dz[j] - dz[i]) <= 1) {
                d.groundLine(x, z, altar.getX() + dx[j] + 0.5, altar.getZ() + dz[j] + 0.5, y + 0.005, 0.32f, core, 0.6f * master);
            }
        }
        // The spark running ahead to the next cell.
        if (full < n && full > 0 && stopAge < 0) {
            float f = lightK - full;
            double x = Mth.lerp(f, altar.getX() + dx[full - 1] + 0.5, altar.getX() + dx[full] + 0.5);
            double z = Mth.lerp(f, altar.getZ() + dz[full - 1] + 0.5, altar.getZ() + dz[full] + 0.5);
            d.star(x, y + 0.2, z, 0.7f, t * 0.3f, core, master);
            d.flatGlow(x, y, z, 1f, primary, 0.6f * master);
        }
        // Over the altar: a circle turning on its top and a ring of glyphs rising off it, faster as the rite gathers.
        float spin = t * (0.02f + 0.06f * p);
        double top = altar.getY() + 1.02;
        float show = Math.min(1, p * 4) * master;
        d.decal(MagicTextures.castCircle(), cx, top, cz, 0.8f + 0.5f * p, -spin, primary, 0.75f * show);
        // Round the whole circle, on the ground: a ring of glyphs that closes as the chalk lights.
        float reachR = 0.5f;
        for (int i = 0; i < n; i++) reachR = Math.max(reachR, Mth.sqrt(dx[i] * dx[i] + dz[i] * dz[i]));
        d.glyphRing(MagicTextures.glyphStrip(), cx, y + 0.01, cz, 0, 1, 0, reachR + 0.7f, reachR + 1.3f, spin * 0.5f, primary,
                0.7f * master * Mth.clamp(p / LIGHT_BY, 0, 1));
        boolean full2 = MagicFxManager.detail() > 0;
        if (full2) {
            d.band(MagicTextures.glyphStrip(), cx, top + 0.25 + p * 1.0, cz, 1.0f + 0.35f * p, 0.42f, spin * 1.5f, primary, 0.85f * show,
                    0.25f * show);
            d.band(MagicTextures.glyphStrip(), cx, top + 0.85 + p * 1.2, cz, 0.65f + 0.2f * p, 0.3f, -spin * 2.2f, accent,
                    0.5f * show * p, 0.1f * show);
        }
        d.groundRing(cx, top + 0.25 + p, cz, 1.0f + 0.35f * p, 1.1f + 0.35f * p, primary, 0.5f * show, true);
        // The offerings stream into a point of light.
        Vec3 focus = focus(d.partial);
        float orb = 0.4f + 1.2f * p * p;
        d.glowDisc(focus.x, focus.y, focus.z, orb * 1.8f, primary, (0.25f + 0.45f * p) * master);
        d.star(focus.x, focus.y, focus.z, orb, t * 0.05f, core, (0.3f + 0.7f * p) * master);
        if (mc.level != null && mc.level.getBlockEntity(altar) instanceof RitualAltarBlockEntity be) {
            int count = 0;
            for (ItemStack s : be.offerings()) if (!s.isEmpty()) count++;
            float time = mc.level.getGameTime() + d.partial;
            for (int i = 0; i < count; i++) {
                Vec3 o = RitualAltarRenderer.offering(i, count, time, p).add(altar.getX(), altar.getY(), altar.getZ());
                float w = 0.04f + 0.1f * p;
                d.ribbon(o.x, o.y, o.z, focus.x, focus.y, focus.z, w * 2.5f, primary, 0.35f * p * master, 0.6f * p * master);
                d.ribbon(o.x, o.y, o.z, focus.x, focus.y, focus.z, w, core, 0.7f * p * master, 0.9f * p * master);
                d.glowDisc(o.x, o.y, o.z, 0.35f, primary, 0.6f * p * master);
            }
        }
        // Past the half the circle itself streams up: thin threads from every few lit cells.
        if (p > 0.5f && full2 && n > 0) {
            float s = (p - 0.5f) * 2;
            int every = Math.max(1, n / 8);
            for (int i = 0; i < full; i += every) {
                double x = altar.getX() + dx[i] + 0.5, z = altar.getZ() + dz[i] + 0.5;
                d.taper(x, y, z, focus.x, focus.y, focus.z, 0.12f * s, 0.02f, primary, 0.5f * s * master, 0.1f * master);
            }
        }
    }
}
