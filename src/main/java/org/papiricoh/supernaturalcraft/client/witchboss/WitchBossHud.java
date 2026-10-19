package org.papiricoh.supernaturalcraft.client.witchboss;

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
import org.papiricoh.supernaturalcraft.entity.boss.grandwitch.GrandWitchEntity;
import org.papiricoh.supernaturalcraft.entity.boss.rowena.RowenaEntity;
import org.papiricoh.supernaturalcraft.util.ClientPresence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.papiricoh.supernaturalcraft.client.witchboss.WitchBossOverlay.argb;
import static org.papiricoh.supernaturalcraft.client.witchboss.WitchBossOverlay.fillTri;
import static org.papiricoh.supernaturalcraft.client.witchboss.WitchBossOverlay.lerpRgb;

/**
 * The witch bosses' bars (v0.21, P6), for their bar keys ({@code entity.supernaturalcraft.rowena.bar.phase<n>},
 * {@code entity.supernaturalcraft.grand_witch.bar.phase<n>}): drawn by code.
 * <ul>
 *   <li><b>Rowena</b>: a gold frame with filigree studs round a fill of emerald velvet, a slow sheen moving along its nap; her
 *   ring (gold, an emerald set in it) at the left end. Once her familiar comes, ember flecks glow in the velvet; while she
 *   recites, the fill's edge burns violet and letters drift along it. A blow knocks gold sparks off the edge of the fill.</li>
 *   <li><b>The Grand Witch</b>: a silver frame round a fill of plum, frost-silver dust drifting through it, the staff's crystal at the
 *   left end. While her coven shields her, a lattice of silver hexagons lies over the bar and a blow only makes it ring (the fill
 *   does not move); in the last phase red taglock threads hang from the frame. A blow knocks crystal shards off.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class WitchBossHud {

    public static final String ROWENA_PREFIX = "entity.supernaturalcraft.rowena.bar", WITCH_PREFIX = "entity.supernaturalcraft.grand_witch.bar";
    static final int W = 200, H = 7;

    private static final class Spark {
        float x, y, vx, vy;
        int age;
        final int life, rgb;

        Spark(float x, float y, int rgb, Random r) {
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            vx = (r.nextFloat() - 0.5f) * 1.1f;
            vy = -0.4f - r.nextFloat() * 0.6f;
            life = 14 + r.nextInt(12);
        }
    }

    private static final class Bar {
        int age, flinch, ring;
        float lastProgress = -1;
        final List<Spark> sparks = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private WitchBossHud() {
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
            if (b.ring > 0) b.ring--;
            for (Spark s : b.sparks) {
                s.age++;
                s.x += s.vx;
                s.y += s.vy;
                s.vy += 0.12f;
            }
            b.sparks.removeIf(s -> s.age >= s.life);
        }
    }

    static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '3' ? c - '0' : 1;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)) return;
        String key = tc.getKey();
        boolean rowena = key.startsWith(ROWENA_PREFIX);
        if (!rowena && !key.startsWith(WITCH_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(H + 18);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float progress = Mth.clamp(boss.getProgress(), 0, 1);
        if (rowena) drawRowena(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(key), progress, event.getY(), partial, reciting());
        else drawWitch(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(key), progress, event.getY(), partial, shielded());
    }

    private static boolean reciting() {
        Minecraft mc = Minecraft.getInstance();
        for (RowenaEntity r : ClientPresence.live(RowenaEntity.class, mc.level)) if (r.reciting()) return true;
        return false;
    }

    private static boolean shielded() {
        Minecraft mc = Minecraft.getInstance();
        for (GrandWitchEntity w : ClientPresence.live(GrandWitchEntity.class, mc.level)) if (w.shielded()) return true;
        return false;
    }

    private static Bar bar(UUID id, float progress, float x0, float top, int rgb, int rgb2) {
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 6;
            int n = 2 + Math.min(6, (int) ((bar.lastProgress - progress) * 600));
            for (int i = 0; i < n; i++) bar.sparks.add(new Spark(x0 + W * progress, top + H / 2f, i % 2 == 0 ? rgb : rgb2, RANDOM));
        }
        bar.lastProgress = progress;
        return bar;
    }

    private static float time(float partial) {
        Minecraft mc = Minecraft.getInstance();
        return (mc.level != null ? mc.level.getGameTime() : 0) + partial;
    }

    // --- Rowena -------------------------------------------------------------------------------------------------------------

    /** Rowena's bar, its fill's top at {@code y} (previews call this too). */
    public static void drawRowena(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial, boolean reciting) {
        int x0 = g.guiWidth() / 2 - W / 2, top = y + 4;
        Bar bar = bar(id, progress, x0, top, WitchBossOverlay.GOLD, 0xFFF4D0);
        float t = time(partial), open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        x0 += shake;
        int gold = WitchBossOverlay.GOLD, goldDark = 0x6A4A14;
        // The frame: dark gold, a bright upper edge, studs along it.
        g.fill(x0 - 3, top - 3, x0 + W + 3, top + H + 3, argb(open, 0x1A1206));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb(open, goldDark));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top - 1, argb(open * 0.9f, gold));
        g.fill(x0 - 2, top + H + 1, x0 + W + 2, top + H + 2, argb(open * 0.6f, 0xB08A3A));
        for (int i = 10; i < W; i += 20) {
            fillTri(g, x0 + i, top - 4, x0 + i - 2, top - 2, x0 + i + 2, top - 2, argb(open, gold));
            fillTri(g, x0 + i, top + H + 4, x0 + i - 2, top + H + 2, x0 + i + 2, top + H + 2, argb(open, gold));
        }
        g.fill(x0, top, x0 + W, top + H, argb(open * 0.92f, 0x040A06));
        int fill = Math.round(W * progress * open);
        if (fill > 0) {
            g.fillGradient(x0, top, x0 + fill, top + H, argb(1, 0x2A8A52), argb(1, 0x0B3A20));
            // The velvet's nap: a slow sheen moving along it, and fine vertical shading.
            for (int i = 0; i < fill; i += 2) {
                float s = 0.5f + 0.5f * Mth.sin(i * 0.08f - t * 0.06f);
                if (s > 0.7f) g.fill(x0 + i, top, x0 + i + 2, top + H, argb((s - 0.7f) * 0.9f, 0x7AE0A0));
                if (i % 4 == 0) g.fill(x0 + i, top + H - 2, x0 + i + 1, top + H, argb(0.25f, 0x02140A));
            }
            if (phase >= 2) {
                // Ember flecks in the velvet (her familiar, her victims).
                Random r = new Random(id.getLeastSignificantBits() ^ (long) (t / 4));
                for (int i = 0; i < fill / 12; i++) {
                    int ex = x0 + r.nextInt(Math.max(1, fill)), ey = top + 1 + r.nextInt(H - 2);
                    g.fill(ex, ey, ex + 1, ey + 1, argb(0.5f + 0.5f * r.nextFloat(), r.nextBoolean() ? 0xFF8A4A : 0xE0402A));
                }
            }
            int edge = reciting ? WitchBossOverlay.RECITE_PALE : 0xFFF0B8;
            g.fill(x0 + fill - 1, top, x0 + fill, top + H, argb(0.9f, edge));
            if (reciting) {
                // The spell burning at the fill's edge, letters drifting back along it.
                float pulse = 0.5f + 0.5f * Mth.sin(t * 0.4f);
                g.fill(x0 + fill - 3, top - 1, x0 + fill + 1, top + H + 1, argb(0.35f + 0.35f * pulse, WitchBossOverlay.RECITE));
                String abc = "aeiouxrst";
                for (int i = 0; i < 5; i++) {
                    float k = ((t * 0.02f + i * 0.2f) % 1f);
                    float lx = x0 + fill - 6 - k * 60;
                    if (lx < x0) continue;
                    GuiDraw.text(g, Component.literal(String.valueOf(abc.charAt((i * 7 + (int) (t / 20)) % abc.length()))), lx, top - 1,
                            0.6f, argb((1 - k) * 0.8f, WitchBossOverlay.RECITE_PALE), false);
                }
            }
        }
        for (Spark s : bar.sparks) {
            float da = 1 - s.age / (float) s.life;
            g.fill((int) s.x, (int) s.y, (int) s.x + 1, (int) s.y + 1, argb(da, s.rgb));
        }
        // Her ring at the left end: a gold hoop with an emerald set in it, catching the light.
        float rx = x0 - 12, ry = top + H / 2f;
        g.fill((int) rx - 6, (int) ry - 6, (int) rx + 6, (int) ry + 6, argb(open * 0.85f, 0x0A0804));
        ring(g, rx, ry + 1, 4.5f, argb(open, gold));
        float glint = 0.6f + 0.4f * Mth.sin(t * 0.12f);
        fillTri(g, rx, ry - 5, rx - 3, ry - 2, rx + 3, ry - 2, argb(open, lerpRgb(0x1E8A4A, 0x9AFFC8, glint * 0.5f)));
        fillTri(g, rx - 3, ry - 2, rx, ry + 1, rx + 3, ry - 2, argb(open, 0x0E5A2E));
        g.fill((int) rx - 1, (int) ry - 4, (int) rx, (int) ry - 3, argb(open * glint, 0xFFFFFF));
        int nameRgb = reciting ? 0xD8B0FF : phase >= 2 ? 0xE8C870 : 0x8FE0A8;
        GuiDraw.centred(g, name, g.guiWidth() / 2f + shake, top - 13, 1f, argb(open, nameRgb), true);
    }

    /** A stroke into a held GUI buffer (both windings). */
    private static void stroke(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, float x0, float y0, float x1, float y1, float width,
                               int argb) {
        float dx = x1 - x0, dy = y1 - y0, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1e-3f) return;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        vc.addVertex(m, x0 + nx, y0 + ny, 0).setColor(argb);
        vc.addVertex(m, x0 - nx, y0 - ny, 0).setColor(argb);
        vc.addVertex(m, x1 - nx, y1 - ny, 0).setColor(argb);
        vc.addVertex(m, x1 + nx, y1 + ny, 0).setColor(argb);
        vc.addVertex(m, x1 + nx, y1 + ny, 0).setColor(argb);
        vc.addVertex(m, x1 - nx, y1 - ny, 0).setColor(argb);
        vc.addVertex(m, x0 - nx, y0 - ny, 0).setColor(argb);
        vc.addVertex(m, x0 + nx, y0 + ny, 0).setColor(argb);
    }

    private static void ring(GuiGraphics g, float cx, float cy, float r, int argb) {
        float px = cx + r, py = cy;
        for (int i = 1; i <= 14; i++) {
            float a = i / 14f * Mth.TWO_PI, qx = cx + Mth.cos(a) * r, qy = cy + Mth.sin(a) * r * 0.8f;
            GuiDraw.line(g, px, py, qx, qy, 1.2f, argb);
            px = qx;
            py = qy;
        }
    }

    // --- the Grand Witch ----------------------------------------------------------------------------------------------------

    /** The Grand Witch's bar, its fill's top at {@code y} (previews call this too). */
    public static void drawWitch(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial, boolean shielded) {
        int x0 = g.guiWidth() / 2 - W / 2, top = y + 4;
        Bar bar = bar(id, progress, x0, top, WitchBossOverlay.CRYSTAL, 0xFFFFFF);
        float t = time(partial), open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        x0 += shake;
        int silver = WitchBossOverlay.SILVER;
        g.fill(x0 - 3, top - 3, x0 + W + 3, top + H + 3, argb(open, 0x0C0810));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb(open, 0x4A4656));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top - 1, argb(open * 0.9f, silver));
        for (int i = 8; i < W; i += 16) {
            g.fill(x0 + i, top - 2, x0 + i + 1, top - 1, argb(open, 0xFFFFFF));
            g.fill(x0 + i, top + H + 1, x0 + i + 1, top + H + 2, argb(open * 0.8f, silver));
        }
        g.fill(x0, top, x0 + W, top + H, argb(open * 0.92f, 0x07040A));
        int fill = Math.round(W * progress * open);
        if (fill > 0) {
            g.fillGradient(x0, top, x0 + fill, top + H, argb(1, 0x8A3AA8), argb(1, 0x3A1048));
            Random r = new Random(id.getMostSignificantBits() ^ (long) (t / 5));
            for (int i = 0; i < fill / 10; i++) {
                int dx = x0 + r.nextInt(Math.max(1, fill)), dy = top + r.nextInt(H);
                g.fill(dx, dy, dx + 1, dy + 1, argb(0.35f + 0.4f * r.nextFloat(), phase == 2 && r.nextBoolean() ? 0xBFE8FF : 0xE8E0F4));
            }
            g.fill(x0 + fill - 1, top, x0 + fill, top + H, argb(0.85f, 0xF0E6FF));
        }
        if (shielded) {
            // Her coven's shield: a lattice of silver hexagons over the whole bar, shimmering; a blow makes it ring.
            float ring = bar.flinch / 6f;
            float sh = 0.35f + 0.15f * Mth.sin(t * 0.2f) + ring * 0.4f;
            // One buffer for the whole lattice, flushed once (nothing else is drawn while it is held).
            var vc = g.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
            var m = g.pose().last().pose();
            for (int hx = -6; hx < W + 6; hx += 9) {
                for (int row = 0; row < 2; row++) {
                    float cx = x0 + hx + (row == 1 ? 4.5f : 0), cy = top + 1.5f + row * 4;
                    if (cx < x0 - 2 || cx > x0 + W + 2) continue;
                    float k = 0.5f + 0.5f * Mth.sin(cx * 0.12f - t * 0.25f);
                    int c = argb(sh * (0.5f + 0.5f * k) * open, 0xE8E6FF);
                    for (int s = 0; s < 6; s++) {
                        float a0 = s * Mth.PI / 3, a1 = a0 + Mth.PI / 3;
                        stroke(vc, m, cx + Mth.cos(a0) * 3, cy + Mth.sin(a0) * 2.4f, cx + Mth.cos(a1) * 3, cy + Mth.sin(a1) * 2.4f, 0.6f, c);
                    }
                }
            }
            g.flush();
            g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb((0.08f + ring * 0.25f) * open, 0xC8A8FF));
        }
        if (phase >= 3) {
            // Red taglock threads hanging from the frame's underside.
            for (int i = 0; i < 9; i++) {
                float tx = x0 + 12 + i * 22, sway = 1.5f * Mth.sin(t * 0.08f + i * 1.7f), len = 5 + (i * 7 % 4);
                GuiDraw.line(g, tx, top + H + 2, tx + sway, top + H + 2 + len, 0.6f, argb(open * 0.8f, WitchBossOverlay.TAGLOCK));
                g.fill((int) (tx + sway) - 1, (int) (top + H + 2 + len), (int) (tx + sway) + 1, (int) (top + H + 4 + len), argb(open, 0x6A3A1A));
            }
        }
        for (Spark s : bar.sparks) {
            float da = 1 - s.age / (float) s.life;
            g.fill((int) s.x, (int) s.y, (int) s.x + 2, (int) s.y + 1, argb(da, s.rgb));
        }
        // The crystal at the left end.
        float cx = x0 - 12, cy = top + H / 2f;
        g.fill((int) cx - 6, (int) cy - 8, (int) cx + 6, (int) cy + 8, argb(open * 0.85f, 0x07040A));
        WitchBossOverlay.crystal(g, cx, cy, 13, open, 0.5f + 0.5f * Mth.sin(t * 0.15f) + (shielded ? 0.3f : 0), 0);
        int nameRgb = shielded ? 0xE0D8FF : phase >= 3 ? 0xFF8A9A : 0xD8C0F0;
        GuiDraw.centred(g, name, g.guiWidth() / 2f + shake, top - 13, 1f, argb(open, nameRgb), true);
    }
}
