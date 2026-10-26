package org.papiricoh.supernaturalcraft.client.leviathan;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.papiricoh.supernaturalcraft.client.leviathan.DickOverlay.argb;

/**
 * Dick Roman's boss bar (v0.25), for his bar keys ({@code entity.supernaturalcraft.dick_roman.bar.phase<n>}): drawn by code. The
 * businessman's is a brushed-steel frame round a cold blue-white fill with a share price ticking along it and a red tie at its
 * left end; once the maw opens the fill turns to black goo with an oil-blue sheen sliding through it and the tie to a pair of
 * jaws; while he hides among copies the name flickers between his and a blur; in the Flood the frame itself oozes, drops running
 * off its underside. Each blow knocks a few drops (white, then black) off the edge of the fill.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class DickHud {

    public static final String BAR_PREFIX = "entity.supernaturalcraft.dick_roman.bar";
    static final int W = 200, H = 7;

    private static final class Drop {
        float x, y, vx, vy;
        int age;
        final int life, rgb;

        Drop(float x, float y, int rgb, Random r, boolean ooze) {
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            vx = ooze ? 0 : (r.nextFloat() - 0.5f) * 0.8f;
            vy = ooze ? 0.05f : -0.3f - r.nextFloat() * 0.5f;
            life = 18 + r.nextInt(14);
        }
    }

    private static final class Bar {
        int age, flinch, phase = 1;
        float lastProgress = -1;
        int x0, bottom;
        final List<Drop> drops = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private DickHud() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            BARS.clear();
            return;
        }
        for (Bar b : BARS.values()) {
            b.age++;
            if (b.flinch > 0) b.flinch--;
            // In the Flood the frame oozes on its own.
            if (b.phase >= 4 && b.age % 7 == 0 && b.x0 != 0) {
                b.drops.add(new Drop(b.x0 - 3 + RANDOM.nextInt(W + 6), b.bottom + 3, 0x05070B, RANDOM, true));
            }
            for (Drop d : b.drops) {
                d.age++;
                d.x += d.vx;
                d.y += d.vy;
                d.vy += 0.12f;
            }
            b.drops.removeIf(d -> d.age >= d.life);
        }
    }

    /** The phase a bar key names (1 if it names none). */
    static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '4' ? c - '0' : 1;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(H + 18);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        draw(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1),
                event.getY(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    /** One bar, its fill's top at {@code y} (previews call this too). */
    public static void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        bar.phase = phase;
        boolean goo = phase >= 2;
        int x0 = g.guiWidth() / 2 - W / 2, top = y + 4;
        bar.x0 = x0;
        bar.bottom = top + H;
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 6;
            int n = 1 + Math.min(5, (int) ((bar.lastProgress - progress) * 500));
            for (int i = 0; i < n; i++) {
                bar.drops.add(new Drop(x0 + W * progress, top + H / 2f, goo ? 0x05070B : 0xE8F0F8, RANDOM, false));
            }
        }
        bar.lastProgress = progress;
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        float open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        x0 += shake;
        // The frame: brushed steel, darkening to gunmetal and, in the Flood, to wet black.
        int outer = phase >= 4 ? 0x020306 : goo ? 0x161C24 : 0x2A3038, inner = phase >= 4 ? 0x0C1420 : goo ? 0x3A4654 : 0x8C96A2,
                hi = phase >= 4 ? 0x3A6AA8 : goo ? 0x7A8A9C : 0xE4EAF0;
        g.fill(x0 - 3, top - 3, x0 + W + 3, top + H + 3, argb(open, outer));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb(open, inner));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top - 1, argb(open * 0.8f, hi));
        // Brushed lines along the steel (or, in the Flood, beads of goo sitting on it).
        for (int i = 5; i < W; i += phase >= 4 ? 17 : 9) {
            if (phase >= 4) {
                int bw = 2 + (i * 7 % 3);
                g.fill(x0 + i, top + H + 1, x0 + i + bw, top + H + 3, argb(open, 0x05070B));
                g.fill(x0 + i, top + H + 1, x0 + i + 1, top + H + 2, argb(open * 0.6f, 0x6A9AD8));
            } else {
                g.fill(x0 + i, top + H + 1, x0 + i + 6, top + H + 2, argb(open * 0.35f, goo ? 0x9AA8B8 : 0xF4F8FC));
            }
        }
        g.fill(x0, top, x0 + W, top + H, argb(open * 0.92f, 0x06080C));
        int fill = Math.round(W * progress * open);
        if (fill > 0) {
            if (goo) {
                g.fillGradient(x0, top, x0 + fill, top + H, argb(1, 0x1A2636), argb(1, 0x020306));
                // The oil-blue sheen sliding through the goo.
                float sx = x0 + ((t * 2.2f) % (W + 60)) - 30;
                for (int i = -14; i <= 14; i++) {
                    int px = (int) sx + i;
                    if (px < x0 || px >= x0 + fill) continue;
                    float k = 1 - Math.abs(i) / 14f;
                    g.fill(px, top + 1, px + 1, top + 3, argb(0.55f * k, 0x8AB8F0));
                }
                // Slow bubbles rising in it.
                Random r = new Random(id.getLeastSignificantBits() ^ (long) (t / 6));
                for (int i = 0; i < fill / 25; i++) {
                    int bx = x0 + r.nextInt(Math.max(1, fill)), by = top + 2 + r.nextInt(H - 3);
                    g.fill(bx, by, bx + 1, by + 1, argb(0.45f, 0x4A7AB8));
                }
            } else {
                g.fillGradient(x0, top, x0 + fill, top + H, argb(1, 0xF2F8FF), argb(1, 0x8CB0D0));
                // The share price ticking along the fill: a little chart line, mostly climbing.
                Random r = new Random(id.getMostSignificantBits());
                int step = 6;
                float scroll = (t * 0.5f) % step, py = top + H - 2;
                for (int x = 0; x + 2 * step <= fill; x += step) {
                    float ny = Mth.clamp(py - 1.2f + r.nextFloat() * 2.0f, top + 1, top + H - 1);
                    GuiDraw.line(g, x0 + x - scroll + step, py, x0 + x - scroll + 2 * step, ny, 0.8f, argb(0.6f, 0x2A5A3A));
                    py = ny;
                }
            }
            // The fill's bright edge.
            g.fill(x0 + fill - 1, top, x0 + fill, top + H, argb(0.8f, goo ? 0x6A9AD8 : 0xFFFFFF));
        }
        for (Drop d : bar.drops) {
            float da = 1 - d.age / (float) d.life;
            g.fill((int) d.x, (int) d.y, (int) d.x + 2, (int) d.y + 2 + (d.vx == 0 ? 1 : 0), argb(da, d.rgb));
        }
        // At the left end: his red tie, or once the maw has opened, the jaws snapping.
        int ex = x0 - 13, ey = top - 3;
        g.fill(ex - 1, ey - 1, ex + 10, ey + 13, argb(open * 0.9f, goo ? 0x020306 : 0x1A222C));
        if (!goo) {
            g.fill(ex + 3, ey + 1, ex + 6, ey + 3, argb(open, 0xB01A22));
            g.fill(ex + 2, ey + 3, ex + 7, ey + 9, argb(open, 0xB01A22));
            g.fill(ex + 3, ey + 9, ex + 6, ey + 11, argb(open, 0xB01A22));
            g.fill(ex + 4, ey + 3, ex + 5, ey + 9, argb(open * 0.5f, 0xE04A4A));
        } else {
            float bite = phase >= 4 ? 18 : 30, ph = (t % bite) / bite;
            int gap = ph < 0.15f ? 0 : ph < 0.3f ? 2 : 4;
            for (int i = 0; i < 4; i++) {
                int tx = ex + 1 + i * 2;
                g.fill(tx, ey + 2, tx + 1, ey + 5 - (gap == 0 ? 0 : 1), argb(open, 0xE8E2D0));
                g.fill(tx + 1, ey + 8 + (gap == 0 ? 0 : 1), tx + 2, ey + 11, argb(open, 0xE8E2D0));
            }
            g.fill(ex, ey + 5, ex + 9, ey + 5 + gap, argb(open, 0x5A0A12));
        }
        // His name above: hidden among his copies (phase III), it flickers.
        int nameRgb = phase >= 4 ? 0x8AB8F0 : goo ? 0xC8D8E8 : 0xF2F6FA;
        float nameA = open;
        float nx = g.guiWidth() / 2f + shake;
        if (phase == 3) {
            float f = Mth.sin(t * 0.7f) * Mth.sin(t * 0.23f);
            if (f > 0.6f) {
                nameA *= 0.35f;
                nx += (f - 0.6f) * 6;
            }
        }
        GuiDraw.centred(g, name, nx, top - 13, 1f, argb(nameA, nameRgb), true);
    }
}
