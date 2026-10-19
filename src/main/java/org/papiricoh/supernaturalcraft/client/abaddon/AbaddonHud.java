package org.papiricoh.supernaturalcraft.client.abaddon;

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
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.entity.boss.abaddon.AbaddonEntity;
import org.papiricoh.supernaturalcraft.util.ClientPresence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.papiricoh.supernaturalcraft.client.abaddon.AbaddonOverlay.argb;

/**
 * Abaddon's boss bar (v0.21), for her bar keys ({@code entity.supernaturalcraft.abaddon.bar.phase<n>}): drawn by code. A black-iron
 * frame studded with rivets, hung by a short chain at either end from a shackle; the fill hellfire in the Knight of Hell's
 * phase (embers flickering along it), crimson veiled by drifting smoke in Possession, the Brand's deep crimson in the last with the
 * seal burning at its left end. What she is doing shows on it: while she rides a knight the fill is drowned in coiling smoke with
 * two crimson glints in it; cast out (the ×2 window) it cracks, its edge flashes and "×2" burns at its right end; held in a devil's
 * trap a red pentagram glows there. Each blow knocks a puff of smoke and a few embers off the fill's edge.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class AbaddonHud {

    public static final String BAR_PREFIX = "entity.supernaturalcraft.abaddon.bar";
    static final int W = 200, H = 7;

    private static final class Puff {
        float x, y, vx, vy;
        int age;
        final int life;
        final boolean ember;

        Puff(float x, float y, boolean ember, Random r) {
            this.x = x;
            this.y = y;
            this.ember = ember;
            vx = (r.nextFloat() - 0.5f) * (ember ? 1.0f : 0.4f);
            vy = ember ? -0.4f - r.nextFloat() * 0.6f : -0.2f - r.nextFloat() * 0.3f;
            life = ember ? 14 + r.nextInt(10) : 20 + r.nextInt(14);
        }
    }

    private static final class Bar {
        int age, flinch;
        float lastProgress = -1;
        final List<Puff> puffs = new ArrayList<>();
    }

    /** What she is doing, as the bar shows it (previews force it). */
    public enum Look {
        NONE, SMOKE, STUNNED, TRAPPED
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static @Nullable Look forced;

    private AbaddonHud() {
    }

    /** Previews: show this state whatever she is doing (null: her own). */
    public static void force(@Nullable Look look) {
        forced = look;
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
            for (Puff p : b.puffs) {
                p.age++;
                p.x += p.vx;
                p.y += p.vy;
                if (p.ember) p.vy += 0.05f;
                else p.vx *= 0.95f;
            }
            b.puffs.removeIf(p -> p.age >= p.life);
        }
    }

    static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '3' ? c - '0' : 1;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(H + 20);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        draw(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1), event.getY(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    /** What the nearest Abaddon is doing. */
    static Look look() {
        if (forced != null) return forced;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return Look.NONE;
        AbaddonEntity near = null;
        double best = 96 * 96;
        for (AbaddonEntity a : ClientPresence.live(AbaddonEntity.class, mc.level)) {
            double d = a.distanceToSqr(mc.player);
            if (d < best) {
                best = d;
                near = a;
            }
        }
        if (near == null) return Look.NONE;
        if (near.smoke()) return Look.SMOKE;
        if (near.stunned()) return Look.STUNNED;
        if (near.trapped()) return Look.TRAPPED;
        return Look.NONE;
    }

    /** One bar, its fill's top at {@code y} (previews call this too). */
    public static void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        Look look = look();
        int x0 = g.guiWidth() / 2 - W / 2, top = y + 5;
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 6;
            int n = 2 + Math.min(5, (int) ((bar.lastProgress - progress) * 500));
            for (int i = 0; i < n; i++) bar.puffs.add(new Puff(x0 + W * progress, top + H / 2f, i % 2 == 1, RANDOM));
        }
        bar.lastProgress = progress;
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        float open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        x0 += shake;
        // The chains hanging the bar from its shackles, and the shackles.
        for (int side = 0; side < 2; side++) {
            int sx = side == 0 ? x0 - 9 : x0 + W + 5;
            for (int i = 0; i < 3; i++) {
                int ly = top - 12 + i * 4;
                if (i % 2 == 0) {
                    g.fill(sx + 1, ly, sx + 3, ly + 4, argb(open, 0x3A3436));
                    g.fill(sx + 1, ly + 1, sx + 2, ly + 3, argb(open, 0x0A0808));
                } else {
                    g.fill(sx + 1, ly, sx + 3, ly + 4, argb(open, 0x5A5458));
                }
            }
            g.fill(sx - 1, top - 1, sx + 5, top + H + 1, argb(open, 0x1A1618));
            g.fill(sx, top, sx + 4, top + H, argb(open, 0x3A3436));
            g.fill(sx + 1, top + 2, sx + 3, top + H - 2, argb(open, 0x0A0808));
        }
        // The frame: black iron, rivets along it.
        g.fill(x0 - 3, top - 3, x0 + W + 3, top + H + 3, argb(open, 0x0C0A0B));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb(open, 0x2A2628));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top - 1, argb(open * 0.8f, 0x5A5458));
        for (int i = 5; i < W; i += 12) {
            g.fill(x0 + i, top - 2, x0 + i + 1, top - 1, argb(open, 0x8A8288));
            g.fill(x0 + i, top + H + 1, x0 + i + 1, top + H + 2, argb(open, 0x6A6468));
        }
        g.fill(x0, top, x0 + W, top + H, argb(open * 0.92f, 0x0A0606));
        int fill = Math.round(W * progress * open);
        if (fill > 0) {
            int base = phase == 1 ? 0xB0281A : phase == 2 ? 0x8A0E2A : 0x7A0A10, light = phase == 1 ? 0xFF7A2A : phase == 2 ? 0xD02A4A : 0xE0301E;
            g.fillGradient(x0, top, x0 + fill, top + H, argb(1, light), argb(1, base));
            Random r = new Random(id.getLeastSignificantBits() ^ (long) (t / 3));
            if (phase != 2) {
                // Embers flickering along the fire.
                for (int i = 0; i < fill / 8; i++) {
                    int ex = x0 + r.nextInt(Math.max(1, fill)), ey = top + r.nextInt(H);
                    g.fill(ex, ey, ex + 1, ey + 1, argb(0.5f + 0.5f * r.nextFloat(), r.nextBoolean() ? 0xFFD08A : 0xFF5A1E));
                }
            }
            if (phase == 2 || look == Look.SMOKE) {
                // Smoke drifting over the fill (drowning it while she rides a knight).
                float thick = look == Look.SMOKE ? 0.75f : 0.35f;
                for (int i = 0; i < 9; i++) {
                    float sx = ((i * 37 + t * (0.4f + 0.1f * (i % 3))) % (W + 30)) - 15;
                    if (sx > fill) continue;
                    float sy = top + H / 2f + 2 * Mth.sin(t * 0.07f + i);
                    for (int s = 6; s > 1; s -= 2) {
                        g.fill((int) Math.max(x0, x0 + sx - s * 2), (int) (sy - s / 2f), (int) Math.min(x0 + fill, x0 + sx + s * 2), (int) (sy + s / 2f),
                                argb(thick * 0.3f, 0x000000));
                    }
                }
            }
            g.fill(x0 + fill - 1, top, x0 + fill, top + H, argb(0.8f, phase == 2 ? 0xFF8090 : 0xFFD08A));
        }
        // Her states at the bar's right end.
        int rx = x0 + W + 14, ry = top + H / 2;
        switch (look) {
            case SMOKE -> {
                // Two crimson glints in the smoke over the fill.
                float on = 0.6f + 0.4f * Mth.sin(t * 0.2f);
                int gx = x0 + Math.max(8, fill - 14);
                g.fill(gx, top + 2, gx + 2, top + 4, argb(on, 0xFF3A2A));
                g.fill(gx + 5, top + 2, gx + 7, top + 4, argb(on, 0xFF3A2A));
            }
            case STUNNED -> {
                // Cracked through, its edge flashing, ×2 burning.
                Random cr = new Random(id.getMostSignificantBits());
                for (int i = 0; i < 6; i++) {
                    float cx = x0 + 10 + cr.nextFloat() * (Math.max(20, fill) - 20);
                    GuiDraw.line(g, cx, top, cx + (cr.nextFloat() - 0.5f) * 6, top + H, 1f, argb(0.9f, 0x0A0303));
                    GuiDraw.line(g, cx + 0.6f, top, cx + 0.6f + (cr.nextFloat() - 0.5f) * 6, top + H, 0.6f, argb(0.6f, 0xFF6A4A));
                }
                float flash = 0.5f + 0.5f * Mth.sin(t * 0.9f);
                g.fill(x0 - 2, top - 2, x0 + W + 2, top - 1, argb(open * flash, 0xFF3A2A));
                g.fill(x0 - 2, top + H + 1, x0 + W + 2, top + H + 2, argb(open * flash, 0xFF3A2A));
                GuiDraw.centred(g, Component.literal("×2"), rx + 2, ry - 4, 1f, argb(0.7f + 0.3f * flash, 0xFF5A3A), true);
            }
            case TRAPPED -> {
                float pulse = 0.7f + 0.3f * Mth.sin(t * 0.4f);
                int col = argb(open * pulse, 0xFF2A1A);
                float pr = 5;
                for (int i = 0; i < 5; i++) {
                    float a0 = -Mth.HALF_PI + i * Mth.TWO_PI / 5, a1 = -Mth.HALF_PI + (i + 2) * Mth.TWO_PI / 5;
                    GuiDraw.line(g, rx + Mth.cos(a0) * pr, ry + Mth.sin(a0) * pr, rx + Mth.cos(a1) * pr, ry + Mth.sin(a1) * pr, 1f, col);
                }
                for (int i = 0; i < 16; i++) {
                    float a0 = i * Mth.TWO_PI / 16, a1 = (i + 1) * Mth.TWO_PI / 16;
                    GuiDraw.line(g, rx + Mth.cos(a0) * (pr + 1.5f), ry + Mth.sin(a0) * (pr + 1.5f), rx + Mth.cos(a1) * (pr + 1.5f),
                            ry + Mth.sin(a1) * (pr + 1.5f), 0.8f, col);
                }
            }
            default -> {
            }
        }
        for (Puff p : bar.puffs) {
            float pa = 1 - p.age / (float) p.life;
            if (p.ember) {
                g.fill((int) p.x, (int) p.y, (int) p.x + 1, (int) p.y + 1, argb(pa, 0xFF7A2A));
            } else {
                int s = 2 + p.age / 6;
                g.fill((int) p.x - s, (int) p.y - s / 2, (int) p.x + s, (int) p.y + s / 2 + 1, argb(pa * 0.5f, 0x0A0606));
            }
        }
        // The seal at the left end in the Brand's phase, beating.
        if (phase >= 3) {
            float ph = (t % 18) / 18f, thump = (float) Math.exp(-Math.pow((ph - 0.05f) / 0.1f, 2));
            float sx = x0 - 22, sy = top + H / 2f;
            g.fill((int) sx - 8, (int) sy - 8, (int) sx + 8, (int) sy + 8, argb(open * 0.85f, 0x140404));
            KnightsSeal.gui(g, sx, sy, 13, open, 1.2f, argb(open * (0.75f + 0.25f * thump), 0xFF7A3A), argb(open * (0.4f + 0.4f * thump), 0xB0101A));
        }
        int nameRgb = phase == 1 ? 0xFF8A5A : phase == 2 ? 0xE07A9A : 0xE04030;
        GuiDraw.centred(g, name, g.guiWidth() / 2f + shake, top - 14, 1f, argb(open, nameRgb), true);
    }
}
