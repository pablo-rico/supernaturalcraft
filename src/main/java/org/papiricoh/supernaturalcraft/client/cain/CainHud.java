package org.papiricoh.supernaturalcraft.client.cain;

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
import org.papiricoh.supernaturalcraft.client.mark.MarkGlyph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.papiricoh.supernaturalcraft.client.cain.CainOverlay.argb;

/**
 * Cain's boss bar (v0.21), for his bar keys ({@code entity.supernaturalcraft.cain.bar.phase<n>}): drawn by code. The Beekeeper's is
 * a weathered-wood frame round a fill of honey with a comb shimmering in it; once the Mark wakes, the frame darkens to scorched
 * iron, the fill turns to blood with embers flickering along it, and the Mark burns at the bar's left end (beating faster in the
 * last phase). Each blow knocks a few drops (honey, then blood) off the edge of the fill.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CainHud {

    public static final String BAR_PREFIX = "entity.supernaturalcraft.cain.bar";
    static final int W = 200, H = 7;

    private static final class Drop {
        float x, y, vx, vy;
        int age;
        final int life, rgb;

        Drop(float x, float y, int rgb, Random r) {
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            vx = (r.nextFloat() - 0.5f) * 0.8f;
            vy = -0.3f - r.nextFloat() * 0.5f;
            life = 18 + r.nextInt(14);
        }
    }

    private static final class Bar {
        int age, flinch;
        float lastProgress = -1;
        final List<Drop> drops = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private CainHud() {
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
        return c >= '1' && c <= '3' ? c - '0' : 1;
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
        boolean marked = phase >= 2;
        int x0 = g.guiWidth() / 2 - W / 2, top = y + 4;
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 6;
            int n = 1 + Math.min(5, (int) ((bar.lastProgress - progress) * 500));
            for (int i = 0; i < n; i++) {
                bar.drops.add(new Drop(x0 + W * progress, top + H / 2f, marked ? 0xB01418 : 0xE8A93A, RANDOM));
            }
        }
        bar.lastProgress = progress;
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        float open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        x0 += shake;
        // The frame: weathered wood, or scorched iron once the Mark has him.
        int outer = marked ? 0x1A0806 : 0x2A1A0C, inner = marked ? 0x4A1410 : 0x6B4A26, hi = marked ? 0x8A2A1A : 0xA8804A;
        g.fill(x0 - 3, top - 3, x0 + W + 3, top + H + 3, argb(open, outer));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb(open, inner));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top - 1, argb(open * 0.8f, hi));
        // Grain or rivets along it.
        for (int i = 6; i < W; i += 14) {
            if (marked) {
                g.fill(x0 + i, top - 2, x0 + i + 1, top - 1, argb(open, 0xC0603A));
                g.fill(x0 + i, top + H + 1, x0 + i + 1, top + H + 2, argb(open, 0xC0603A));
            } else {
                g.fill(x0 + i, top + H + 1, x0 + i + 5, top + H + 2, argb(open * 0.6f, 0x3A2610));
            }
        }
        g.fill(x0, top, x0 + W, top + H, argb(open * 0.92f, 0x0E0806));
        int fill = Math.round(W * progress * open);
        if (fill > 0) {
            int base = marked ? (phase >= 3 ? 0x8A0A0E : 0xA81812) : 0xD0901E, light = marked ? 0xE0402A : 0xF6C454;
            g.fillGradient(x0, top, x0 + fill, top + H, argb(1, light), argb(1, base));
            if (marked) {
                // Embers flickering along the blood.
                Random r = new Random(id.getLeastSignificantBits() ^ (long) (t / 3));
                for (int i = 0; i < fill / 9; i++) {
                    int ex = x0 + r.nextInt(Math.max(1, fill)), ey = top + r.nextInt(H);
                    g.fill(ex, ey, ex + 1, ey + 1, argb(0.5f + 0.5f * r.nextFloat(), r.nextBoolean() ? 0xFFC27A : 0xFF6A3A));
                }
            } else {
                // A comb shimmer drifting through the honey.
                for (int cx = x0 - 12 + (int) (t * 0.4f) % 12; cx < x0 + fill; cx += 12) {
                    for (int row = 0; row < 2; row++) {
                        int hx = cx + row * 6;
                        if (hx < x0 || hx + 4 > x0 + fill) continue;
                        int hy = top + 1 + row * 3;
                        g.fill(hx, hy, hx + 4, hy + 1, argb(0.25f, 0xFFF0C0));
                        g.fill(hx + 1, hy + 2, hx + 3, hy + 3, argb(0.18f, 0x8A5A10));
                    }
                }
            }
            // The fill's bright edge.
            g.fill(x0 + fill - 1, top, x0 + fill, top + H, argb(0.8f, marked ? 0xFFC27A : 0xFFF4D0));
        }
        for (Drop d : bar.drops) {
            float da = 1 - d.age / (float) d.life;
            g.fill((int) d.x, (int) d.y, (int) d.x + 2, (int) d.y + 2, argb(da, d.rgb));
        }
        // The Mark at the left end, burning, beating faster in the last phase.
        if (marked) {
            float beat = phase >= 3 ? 14 : 24, ph = (t % beat) / beat;
            float thump = Math.max(bump(ph, 0), 0.6f * bump(ph, 0.2f));
            float rh = 17, rx = x0 - 15, ry = top + H / 2f - rh / 2;
            g.fill((int) rx - 3, (int) ry - 2, (int) (rx + rh * MarkGlyph.ASPECT) + 3, (int) (ry + rh) + 2, argb(open * 0.9f, 0x140404));
            MarkGlyph.gui(g, rx, ry, rh, open, 1.3f, argb(open * (0.75f + 0.25f * thump), 0xFFB070), argb(open * (0.4f + 0.4f * thump), 0xC0201A));
        } else {
            // A little hive at the left end.
            int hx = x0 - 13, hy = top - 2;
            g.fill(hx, hy, hx + 9, hy + 11, argb(open, 0x2A1A0C));
            for (int i = 0; i < 4; i++) g.fill(hx + 1, hy + 1 + i * 2 + (i > 1 ? 1 : 0), hx + 8, hy + 2 + i * 2 + (i > 1 ? 1 : 0), argb(open, 0xD0901E));
            g.fill(hx + 3, hy + 8, hx + 6, hy + 10, argb(open, 0x140A04));
        }
        // His name above.
        int nameRgb = marked ? (phase >= 3 ? 0xE04030 : 0xFF8A5A) : 0xF2C46A;
        GuiDraw.centred(g, name, g.guiWidth() / 2f + shake, top - 13, 1f, argb(open, nameRgb), true);
    }

    private static float bump(float x, float at) {
        float d = (x - at) / 0.08f;
        return (float) Math.exp(-d * d);
    }
}
