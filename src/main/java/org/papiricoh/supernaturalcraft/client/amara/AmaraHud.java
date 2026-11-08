package org.papiricoh.supernaturalcraft.client.amara;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.dyingsun.ClientDyingSun;
import org.papiricoh.supernaturalcraft.client.lucifer.Gothic;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraAssets;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraHudSheet;
import org.papiricoh.supernaturalcraft.entity.boss.amara.AmaraLook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Amara's boss bar (v0.42), "the closing eclipse": top centre, a sun. Its face is the phase's ({@link AmaraHudSheet#skin}: smoke,
 * a child's crayon sun, art-deco filigree, two suns, stars, a guttering sun); round it the corona, wiped radially, is her health
 * (the lit arc, with a pale ghost of the last blows draining after it); over it her black disc slides in phase by phase, a sliver
 * in I, all but a ring in VI, ink dripping from its edge and smoke curling off; at the reconciliation it slides off again. Six
 * lunar pips under it count the phases; under them the "Light of the World" ({@link ClientDyingSun#light}) and, in phase IV, the
 * light of Chuck she is wounding ({@link AmaraLook#chuckLight}). Her name resolves letter by letter in the Cage's gothic
 * capitals; blows knock ink and embers off the corona; a phase change flares; phase VI shows the countdown as a ring of beads
 * going out, and her rage pulses red and violet. Keys: {@code entity.supernaturalcraft.amara.bar.phase<n>} (n 1-6) and the old
 * {@code entity.supernaturalcraft.amara.phase<n>}. Without the sheet the vanilla bar stands in.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AmaraHud {

    static final ResourceLocation SHEET = SupernaturalCraft.asResource(AmaraAssets.HUD_SHEET);
    static final String OLD_PREFIX = "entity.supernaturalcraft.amara.phase";
    static final int TW = AmaraHudSheet.WIDTH, TH = AmaraHudSheet.HEIGHT;
    /** The corona's tint per phase, its rage colours, the name's. */
    static final int[] TINT = {0xC9B6F2, 0xFFE27A, 0xF2C86A, 0xFFF2D2, 0xCFDCFF, 0xFF7A3C};
    static final int RAGE_A = 0xFF2E5E, RAGE_B = 0xB040FF, NAME = 0xE9E0FF, NAME_DAWN = 0xFFE7B0, EMBER = 0xFF6A2A, DAWN = 0xFFD877;
    /** How much of the sun her disc covers in each phase (VI: centred, a ring left). */
    static final float[] COVER = {0.12f, 0.3f, 0.48f, 0.66f, 0.84f, 1f};
    private static final int SLIDE_TICKS = 40, NAME_TICKS_PER_LETTER = 2;

    private static final class Bit {
        float x, y, vx, vy;
        int age;
        final int life, rgb, kind;

        Bit(float x, float y, float vx, float vy, int life, int rgb, int kind) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.rgb = rgb;
            this.kind = kind;
        }
    }

    private static final class Bar {
        int age, phase = -1, flare, flinch, dawnAge = -1;
        float progress = -1, ghost = -1, cover = -1, coverFrom, light = -1;
        int ghostHold, slide = SLIDE_TICKS, chuckFlash;
        float chuckLast = -1;
        final List<Bit> bits = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static long ticks;

    private AmaraHud() {
    }

    // --- the bar's key ---------------------------------------------------------------------------------------------------

    /** The phase a key names, or -1 if it is not one of her bars. */
    static int phaseOf(String key) {
        String rest;
        if (key.startsWith(AmaraAssets.BAR_PREFIX + ".phase")) rest = key.substring(AmaraAssets.BAR_PREFIX.length() + 6);
        else if (key.startsWith(OLD_PREFIX)) rest = key.substring(OLD_PREFIX.length());
        else return -1;
        try {
            return Mth.clamp(Integer.parseInt(rest), 1, AmaraHudSheet.PHASES);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)) return;
        int phase = phaseOf(tc.getKey());
        if (phase < 0 || !MichaelGui.has(SHEET)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        event.setIncrement(height(g, phase));
        if (CameraDirector.active()) return;
        draw(g, boss.getId(), boss.getName(), phase, Mth.clamp(boss.getProgress(), 0, 1), event.getY(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            BARS.clear();
            return;
        }
        ticks++;
        for (Bar b : BARS.values()) {
            b.age++;
            if (b.flare > 0) b.flare--;
            if (b.flinch > 0) b.flinch--;
            if (b.chuckFlash > 0) b.chuckFlash--;
            if (b.slide < SLIDE_TICKS) b.slide++;
            if (b.dawnAge >= 0) b.dawnAge++;
            if (b.ghostHold > 0) b.ghostHold--;
            else if (b.ghost > b.progress) b.ghost = Math.max(b.progress, b.ghost - 0.005f);
            for (Bit bit : b.bits) {
                bit.age++;
                bit.x += bit.vx;
                bit.y += bit.vy;
                bit.vx *= 0.95f;
                bit.vy += bit.kind == 1 ? 0.12f : bit.kind == 0 ? -0.02f : -0.01f;
            }
            b.bits.removeIf(bit -> bit.age >= bit.life);
        }
        BARS.entrySet().removeIf(e -> e.getValue().age > 20 * 60 * 30);
    }

    // --- layout ----------------------------------------------------------------------------------------------------------

    /** The sun's drawn scale: 1 on most screens, smaller on short ones. */
    static float scale(GuiGraphics g) {
        return g.guiHeight() >= 300 ? 1f : 0.75f;
    }

    static int height(GuiGraphics g, int phase) {
        int sun = Math.round(64 * scale(g));
        return 12 + sun + 26 + (phase == 4 ? 14 : 0);
    }

    // --- drawing ---------------------------------------------------------------------------------------------------------

    /** One bar, its top at {@code y} (the previews call this too). */
    public static void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        if (!MichaelGui.has(SHEET)) return;
        AmaraLook look = ClientAmara.current();
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        float s = scale(g);
        float size = 64 * s;
        float cx = g.guiWidth() / 2f, cy = y + 12 + size / 2;
        float t = ticks + partial;
        boolean dawn = look != null && look.skyStage() == 5;
        boolean rage = look != null && look.enraged();

        // Phase changes slide the disc and flare the corona.
        if (bar.phase != phase) {
            if (bar.phase > 0) {
                bar.flare = 24;
                burst(bar, cx, cy, size * 0.5f, 26, TINT[phase - 1]);
            }
            bar.coverFrom = bar.cover < 0 ? COVER[phase - 1] : bar.cover;
            bar.slide = bar.phase < 0 ? SLIDE_TICKS : 0;
            bar.phase = phase;
        }
        if (dawn && bar.dawnAge < 0) bar.dawnAge = 0;
        if (!dawn) bar.dawnAge = -1;
        float slideK = Mth.clamp((bar.slide + partial) / SLIDE_TICKS, 0, 1);
        slideK = slideK * slideK * (3 - 2 * slideK);
        bar.cover = Mth.lerp(slideK, bar.coverFrom, COVER[phase - 1]);
        // Blows: the ghost holds a moment, ink and embers fly from the wipe's edge.
        if (bar.progress >= 0 && progress < bar.progress - 0.0005f) {
            if (bar.ghost < bar.progress) bar.ghost = bar.progress;
            bar.ghostHold = 14;
            bar.flinch = 5;
            float a = (progress - 0.25f) * Mth.TWO_PI;
            float ex = cx + Mth.cos(a) * size * 0.46f, ey = cy + Mth.sin(a) * size * 0.46f;
            int n = 3 + Math.min(10, (int) ((bar.progress - progress) * 500));
            for (int i = 0; i < n; i++) {
                boolean ink = RANDOM.nextBoolean();
                bar.bits.add(new Bit(ex, ey, (RANDOM.nextFloat() - 0.5f) * 2.4f, -RANDOM.nextFloat() * 1.6f, 16 + RANDOM.nextInt(14),
                        ink ? 0x120A1C : TINT[phase - 1], ink ? 1 : 0));
            }
        }
        if (bar.ghost < 0) bar.ghost = progress;
        bar.progress = progress;
        float shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) * s : 0;
        cx += shake;
        float open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        float tint = open;

        // Behind: her rage, or the flare of a new phase.
        if (rage) {
            float pulse = 0.5f + 0.5f * Mth.sin(t * 0.35f);
            int rgb = lerpRgb(RAGE_A, RAGE_B, pulse);
            region(g, AmaraHudSheet.CORONA, cx, cy, size * (1.45f + 0.08f * pulse), 0.55f + 0.3f * pulse, rgb, true);
        }
        if (bar.flare > 0) {
            float k = bar.flare / 24f;
            region(g, AmaraHudSheet.CORONA, cx, cy, size * (1 + (1 - k) * 0.8f), k, TINT[phase - 1], true);
        }
        // The sun's face; it dims with the countdown in phase VI.
        float faceA = tint * (phase == 6 && look != null && !dawn ? 0.45f + 0.55f * look.countdown() : 1f);
        region(g, AmaraHudSheet.skin(phase - 1), cx, cy, size, faceA, dawn ? 0xFFF4DC : 0xFFFFFF, false);
        // The corona: health lit, the ghost pale, the rest a dim ember.
        int coronaRgb = rage ? lerpRgb(RAGE_A, RAGE_B, 0.5f + 0.5f * Mth.sin(t * 0.35f)) : dawn ? DAWN : TINT[phase - 1];
        float breathe = 0.85f + 0.15f * Mth.sin(t * 0.12f);
        fan(g, AmaraHudSheet.CORONA, cx, cy, size * 1.1f, 0, 1, 0x30202A, 0.5f * tint, false);
        if (bar.ghost > progress) fan(g, AmaraHudSheet.CORONA, cx, cy, size * 1.1f, progress, bar.ghost, 0xFFFFFF, 0.7f * tint, true);
        fan(g, AmaraHudSheet.CORONA, cx, cy, size * 1.1f, 0, progress * open, coronaRgb, tint * breathe, true);
        fan(g, AmaraHudSheet.CORONA, cx, cy, size * 1.1f, 0, progress * open, coronaRgb, 0.35f * tint, false);
        // Her disc: slides in from the upper right; centred and a little smaller in VI; off again at dawn.
        float cover = bar.cover;
        if (bar.dawnAge >= 0) {
            float k = Mth.clamp((bar.dawnAge + partial) / 80f, 0, 1);
            cover = Mth.lerp(k * k, cover, -0.15f);
        }
        float off = (1 - cover) * size * 0.95f;
        float dx = off * 0.7071f, dy = -off * 0.7071f;
        float discSize = size * (cover >= 0.999f ? 0.86f : 0.96f);
        float discA = Mth.clamp(1.15f + cover, 0, 1) * tint;
        if (discA > 0.01f) {
            region(g, AmaraHudSheet.DISC, cx + dx, cy + dy, discSize, discA, 0xFFFFFF, false);
            smoke(g, cx + dx, cy + dy, discSize * 0.5f, t, discA, phase);
            drip(g, bar, cx + dx, cy + dy, discSize * 0.5f, t, discA);
        }
        // Phase VI's countdown: a ring of beads round the disc, going out one by one.
        if (phase == 6 && look != null && !dawn) {
            int beads = 48;
            float left = look.countdown();
            for (int i = 0; i < beads; i++) {
                float a = (i / (float) beads - 0.25f) * Mth.TWO_PI;
                boolean lit = i / (float) beads < left;
                float bx = cx + Mth.cos(a) * size * 0.39f, by = cy + Mth.sin(a) * size * 0.39f;
                int rgb = lit ? (rage ? RAGE_A : 0xFFB070) : 0x2A1020;
                g.fill((int) bx, (int) by, (int) bx + 1, (int) by + 1, MichaelGui.argb(lit ? 0.9f : 0.5f, rgb));
            }
        }
        // The six pips on an arc under the sun.
        for (int i = 0; i < AmaraHudSheet.PHASES; i++) {
            float a = Mth.DEG_TO_RAD * (160 - i * 28f);
            float px = cx + Mth.cos(a) * size * 0.66f, py = cy + Mth.sin(a) * size * 0.66f * 0.55f + size * 0.34f;
            int state = i < phase - 1 ? 1 : i == phase - 1 ? 2 : 0;
            float pulse = state == 2 ? 0.8f + 0.2f * Mth.sin(t * 0.25f) : 1f;
            AmaraHudSheet.Region r = AmaraHudSheet.pip(i, state);
            if (state == 2) region(g, r, px, py, r.w() * s * 1.6f, 0.35f * pulse * tint, TINT[phase - 1], true);
            region(g, r, px, py, r.w() * s, pulse * tint, 0xFFFFFF, false);
        }
        // Her name over it, resolving out of the dark.
        name(g, name.getString(), cx, y, bar.age + partial, dawn ? NAME_DAWN : NAME, tint);
        // The Light of the World, and Chuck's in phase IV.
        float by = cy + size / 2 + 8;
        float light = ClientDyingSun.light(partial);
        if (look != null && look.phase() == 6 && !dawn) light = Math.min(light, look.countdown());
        if (dawn) light = Math.max(light, Mth.clamp((bar.dawnAge + partial) / 80f, 0, 1));
        lightBar(g, cx, by, light, t, tint, Component.translatable("hud.supernaturalcraft.amara.world_light"), false);
        if (phase == 4 && look != null) {
            float cl = Mth.clamp(look.chuckLight(), 0, 1);
            if (bar.chuckLast >= 0 && cl < bar.chuckLast - 0.002f) bar.chuckFlash = 8;
            bar.chuckLast = cl;
            lightBar(g, cx, by + 14, cl, t, tint, Component.translatable("hud.supernaturalcraft.amara.chuck_light"), true);
            if (bar.chuckFlash > 0) {
                AmaraHudSheet.Region f = AmaraHudSheet.CHUCK_FRAME;
                g.fill((int) (cx - f.w() / 2f), (int) (by + 14), (int) (cx + f.w() / 2f), (int) (by + 14 + f.h()),
                        MichaelGui.argb(bar.chuckFlash / 8f * 0.5f, 0xC0203A));
            }
        }
        for (Bit bit : bar.bits) {
            float k = 1 - bit.age / (float) bit.life;
            int c = MichaelGui.argb(k, bit.rgb);
            float x = bit.x + bit.vx * partial, yy = bit.y + bit.vy * partial;
            if (bit.kind == 2) g.fill((int) x - 1, (int) yy - 1, (int) x + 1, (int) yy + 1, MichaelGui.argb(k * 0.5f, bit.rgb));
            else g.fill((int) x, (int) yy, (int) x + 1, (int) yy + (bit.kind == 1 ? 2 : 1), c);
        }
    }

    private static void burst(Bar bar, float cx, float cy, float r, int n, int rgb) {
        for (int i = 0; i < n; i++) {
            float a = RANDOM.nextFloat() * Mth.TWO_PI;
            float sp = 0.8f + RANDOM.nextFloat() * 1.6f;
            bar.bits.add(new Bit(cx + Mth.cos(a) * r, cy + Mth.sin(a) * r, Mth.cos(a) * sp, Mth.sin(a) * sp, 18 + RANDOM.nextInt(16),
                    RANDOM.nextInt(3) == 0 ? 0xFFFFFF : rgb, 0));
        }
    }

    /** Smoke tendrils curling off the disc's edge. */
    private static void smoke(GuiGraphics g, float cx, float cy, float r, float t, float alpha, int phase) {
        int n = 3 + phase;
        for (int i = 0; i < n; i++) {
            float base = i * Mth.TWO_PI / n + t * 0.01f + i;
            float px = cx + Mth.cos(base) * r, py = cy + Mth.sin(base) * r;
            for (int k = 1; k <= 6; k++) {
                float u = k / 6f;
                float wob = Mth.sin(t * 0.08f + i * 1.3f + u * 4) * 3 * u;
                float a = base + wob * 0.05f;
                float nx = cx + Mth.cos(a) * (r + u * r * 0.55f) + Mth.cos(a + Mth.HALF_PI) * wob,
                        ny = cy + Mth.sin(a) * (r + u * r * 0.55f) + Mth.sin(a + Mth.HALF_PI) * wob - u * 3;
                GuiDraw.line(g, px, py, nx, ny, 1.6f * (1 - u * 0.6f), MichaelGui.argb(alpha * 0.45f * (1 - u), 0x1A1028));
                px = nx;
                py = ny;
            }
        }
    }

    /** Ink running off the disc: one drop forming at its lowest edge, cycling through the sheet's frames. */
    private static void drip(GuiGraphics g, Bar bar, float cx, float cy, float r, float t, float alpha) {
        int frame = (int) (t / 4) % AmaraHudSheet.DRIP_FRAMES;
        for (int i = 0; i < 2; i++) {
            float a = Mth.DEG_TO_RAD * (100 + i * 35);
            float x = cx + Mth.cos(a) * r * 0.92f, y = cy + Mth.sin(a) * r * 0.92f;
            int f = (frame + i * 3) % AmaraHudSheet.DRIP_FRAMES;
            AmaraHudSheet.Region d = AmaraHudSheet.drip(f);
            MichaelGui.blit(g, SHEET, x - 8, y - 2, 16, 16, d.u(), d.v(), d.w(), d.h(), TW, TH, alpha, 0xFFFFFF);
        }
    }

    /** A thin bar: the worn gold frame and its fill (or Chuck's ink-and-paper one), a small label under it. */
    private static void lightBar(GuiGraphics g, float cx, float y, float value, float t, float alpha, Component label, boolean chuck) {
        AmaraHudSheet.Region frame = chuck ? AmaraHudSheet.CHUCK_FRAME : AmaraHudSheet.WORLD_FRAME;
        AmaraHudSheet.Region fill = chuck ? AmaraHudSheet.CHUCK_FILL : AmaraHudSheet.WORLD_FILL;
        float x0 = cx - frame.w() / 2f;
        MichaelGui.blit(g, SHEET, x0, y, frame.w(), frame.h(), frame.u(), frame.v(), frame.w(), frame.h(), TW, TH, alpha, 0xFFFFFF);
        int w = Math.round(fill.w() * Mth.clamp(value, 0, 1));
        if (w > 0) {
            int rgb = chuck ? 0xFFF6E0 : lerpRgb(EMBER, DAWN, Mth.clamp(value * 1.3f - 0.15f, 0, 1));
            float flick = value < 0.25f && !chuck ? 0.7f + 0.3f * Mth.sin(t * 1.3f) : 1f;
            MichaelGui.blit(g, SHEET, x0 + 2, y + 2, w, fill.h(), fill.u(), fill.v(), w, fill.h(), TW, TH, alpha * flick, rgb);
            MichaelGui.glow(g, SHEET, x0 + 2, y + 2, w, fill.h(), fill.u(), fill.v(), w, fill.h(), TW, TH, alpha * 0.4f * flick, rgb);
        }
        GuiDraw.centred(g, label, cx, y + frame.h() + 1, 0.5f, MichaelGui.argb(alpha * 0.75f, chuck ? 0xEFE6D2 : 0xD9C9A0), true);
    }

    /** Her name in gothic capitals, each letter resolving from a flicker of other letters. */
    private static void name(GuiGraphics g, String text, float cx, float y, float age, int rgb, float alpha) {
        int shown = (int) (age / NAME_TICKS_PER_LETTER);
        if (!Gothic.covers(text)) {
            String part = text.substring(0, Math.min(text.length(), shown));
            GuiDraw.centred(g, Component.literal(part), cx, y + 1, 1f, MichaelGui.argb(alpha, rgb), true);
            return;
        }
        String upper = Gothic.upper(text);
        float x = cx - Gothic.width(upper, false) / 2f;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            int adv = Gothic.advance(c, false);
            if (i < shown) {
                Gothic.glyph(g, c, x + 1, y + 1, 1f, alpha * 0.85f, 0x000000, false, false);
                Gothic.glyph(g, c, x, y, 1f, alpha, rgb, false, false);
            } else if (i < shown + 3 && c != ' ') {
                // Not yet itself: a flicker of the letters it might be.
                char other = (char) ('A' + Math.floorMod((int) (age * 3) + i * 7, 26));
                Gothic.glyph(g, other, x, y, 1f, alpha * 0.35f, 0x8A70C0, false, false);
            }
            x += adv;
        }
    }

    // --- sheet drawing ---------------------------------------------------------------------------------------------------

    static int lerpRgb(int a, int b, float k) {
        int r = (int) Mth.lerp(k, a >> 16 & 255, b >> 16 & 255), gg = (int) Mth.lerp(k, a >> 8 & 255, b >> 8 & 255),
                bb = (int) Mth.lerp(k, a & 255, b & 255);
        return r << 16 | gg << 8 | bb;
    }

    /** A sheet region centred on (cx, cy), drawn {@code size} wide (square regions keep their aspect). */
    static void region(GuiGraphics g, AmaraHudSheet.Region r, float cx, float cy, float size, float alpha, int rgb, boolean light) {
        if (alpha <= 0.005f) return;
        float h = size * r.h() / (float) r.w();
        g.pose().pushPose();
        g.pose().translate(cx - size / 2, cy - h / 2, 0);
        g.pose().scale(size / r.w(), h / r.h(), 1);
        if (light) MichaelGui.glow(g, SHEET, 0, 0, r.w(), r.h(), r.u(), r.v(), r.w(), r.h(), TW, TH, alpha, rgb);
        else MichaelGui.blit(g, SHEET, 0, 0, r.w(), r.h(), r.u(), r.v(), r.w(), r.h(), TW, TH, alpha, rgb);
        g.pose().popPose();
    }

    /**
     * A wedge of a square region centred on (cx, cy): from {@code from} to {@code to} of the full turn, clockwise from the top,
     * as a fan clipped to the region's inscribed circle (so no texel outside the cell is ever sampled).
     */
    static void fan(GuiGraphics g, AmaraHudSheet.Region r, float cx, float cy, float size, float from, float to, int rgb, float alpha,
                    boolean light) {
        from = Mth.clamp(from, 0, 1);
        to = Mth.clamp(to, 0, 1);
        if (to - from <= 0.001f || alpha <= 0.005f) return;
        g.flush();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SHEET);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        if (light) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        int steps = Math.max(2, (int) Math.ceil((to - from) * 72));
        float rad = size / 2;
        float uc = (r.u() + r.w() / 2f) / TW, vc = (r.v() + r.h() / 2f) / TH;
        int cr = rgb >> 16 & 255, cg = rgb >> 8 & 255, cb = rgb & 255, ca = (int) (Mth.clamp(alpha, 0, 1) * 255);
        for (int i = 0; i < steps; i++) {
            float a0 = (from + (to - from) * i / steps - 0.25f) * Mth.TWO_PI;
            float a1 = (from + (to - from) * (i + 1) / steps - 0.25f) * Mth.TWO_PI;
            b.addVertex(m, cx, cy, 0).setUv(uc, vc).setColor(cr, cg, cb, ca);
            b.addVertex(m, cx + Mth.cos(a1) * rad, cy + Mth.sin(a1) * rad, 0)
                    .setUv(uc + Mth.cos(a1) * r.w() / 2f / TW, vc + Mth.sin(a1) * r.h() / 2f / TH).setColor(cr, cg, cb, ca);
            b.addVertex(m, cx + Mth.cos(a0) * rad, cy + Mth.sin(a0) * rad, 0)
                    .setUv(uc + Mth.cos(a0) * r.w() / 2f / TW, vc + Mth.sin(a0) * r.h() / 2f / TH).setColor(cr, cg, cb, ca);
        }
        MeshData mesh = b.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }
}
