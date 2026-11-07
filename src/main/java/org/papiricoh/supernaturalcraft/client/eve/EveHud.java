package org.papiricoh.supernaturalcraft.client.eve;

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

import static org.papiricoh.supernaturalcraft.client.eve.EveOverlay.argb;

/**
 * Eve's boss bar (v0.39), for her bar keys ({@code entity.supernaturalcraft.eve.bar.phase<n>}): drawn by code. A frame of gnarled
 * bark with knots in it round a fill of sap: moss-green in the Mother, the roaches' brown with roaches running along it in the
 * Swarm, pulsing violet with her call, glowing amber in the Mother of All. Roots grow out of both ends and curl down (more of them
 * each phase), and every blow knocks a few flakes of bark off the fill's edge.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class EveHud {

    public static final String BAR_PREFIX = "entity.supernaturalcraft.eve.bar";
    static final int W = 200, H = 7;
    static final int BARK = 0x2A1E14, BARK_LIGHT = 0x4E3A26, BARK_DARK = 0x140E09, MOSS = 0x5E7A3A, MOSS_DARK = 0x2A3A1A,
            ROACH = 0x3A2412, ROACH_SHINE = 0x8A5A2A, VIOLET = 0x8A5ACA, AMBER = 0xF2A23A, AMBER_HOT = 0xFFD27A;

    private static final class Flake {
        float x, y, vx, vy;
        int age;
        final int life, rgb;

        Flake(float x, float y, int rgb, Random r) {
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            vx = (r.nextFloat() - 0.5f) * 0.9f;
            vy = -0.2f - r.nextFloat() * 0.5f;
            life = 16 + r.nextInt(14);
        }
    }

    private static final class Bar {
        int age, flinch;
        float lastProgress = -1;
        final List<Flake> flakes = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private EveHud() {
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
            for (Flake f : b.flakes) {
                f.age++;
                f.x += f.vx;
                f.y += f.vy;
                f.vy += 0.1f;
            }
            b.flakes.removeIf(f -> f.age >= f.life);
        }
    }

    static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '4' ? c - '0' : 1;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(H + 20);
        if (Minecraft.getInstance().level == null) return;
        draw(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1), event.getY(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    /** The fill's colours in a phase: top, bottom. */
    static int[] sap(int phase, float t) {
        return switch (phase) {
            case 1 -> new int[]{0x8AAA52, MOSS_DARK};
            case 2 -> new int[]{0x6A4A2A, 0x22140A};
            case 3 -> {
                float k = 0.5f + 0.5f * Mth.sin(t * 0.15f);
                yield new int[]{EveOverlay.TitleCard.lerpRgb(0x6A4A9A, 0xB08AE8, k), 0x24163A};
            }
            default -> new int[]{AMBER_HOT, 0x8A4A10};
        };
    }

    /** One bar, its fill's top at {@code y} (previews call this too). */
    public static void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        Minecraft mc = Minecraft.getInstance();
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        int x0 = g.guiWidth() / 2 - W / 2, top = y + 5;
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 6;
            int n = 1 + Math.min(5, (int) ((bar.lastProgress - progress) * 500));
            for (int i = 0; i < n; i++) bar.flakes.add(new Flake(x0 + W * progress, top + H / 2f, i % 2 == 0 ? BARK_LIGHT : MOSS, RANDOM));
        }
        bar.lastProgress = progress;
        float t = (mc.level != null ? mc.level.getGameTime() : 0) + partial;
        float open = Mth.clamp((bar.age + partial) / 16f, 0, 1);
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        x0 += shake;
        Random r = new Random(id.getMostSignificantBits());
        // Roots out of both ends, curling down and out; one more pair each phase.
        for (int side = -1; side <= 1; side += 2) {
            float ex = side < 0 ? x0 - 3 : x0 + W + 3, ey = top + H / 2f;
            for (int k = 0; k < phase + 1; k++) {
                float grow = Mth.clamp(open * 1.4f - k * 0.15f, 0, 1);
                root(g, r, ex, ey, side * (0.6f + 0.25f * k), 0.5f + 0.35f * k, 12 + 6 * k, grow, t, phase >= 4 ? AMBER : BARK_LIGHT, 1.4f - 0.2f * k);
            }
        }
        // The frame: rough bark, knots along it, lit a little on top.
        g.fill(x0 - 3, top - 3, x0 + W + 3, top + H + 3, argb(open, BARK_DARK));
        g.fill(x0 - 2, top - 2, x0 + W + 2, top + H + 2, argb(open, BARK));
        for (int i = 0; i < W + 4; i += 2) {
            int dy = r.nextInt(3) == 0 ? 1 : 0;
            g.fill(x0 - 2 + i, top - 2 + dy, x0 + i, top - 1 + dy, argb(open * 0.7f, BARK_LIGHT));
            if (r.nextInt(5) == 0) g.fill(x0 - 2 + i, top + H + 1, x0 + i, top + H + 2, argb(open * 0.6f, BARK_DARK));
        }
        for (int i = 18; i < W; i += 37 + r.nextInt(12)) {
            g.fill(x0 + i, top - 3, x0 + i + 3, top - 1, argb(open, BARK_LIGHT));
            g.fill(x0 + i + 1, top - 3, x0 + i + 2, top - 2, argb(open, BARK_DARK));
        }
        g.fill(x0, top, x0 + W, top + H, argb(open * 0.92f, 0x0A0806));
        int fill = Math.round(W * progress * open);
        if (fill > 0) {
            int[] c = sap(phase, t);
            g.fillGradient(x0, top, x0 + fill, top + H, argb(1, c[0]), argb(1, c[1]));
            if (phase == 2) {
                // Roaches running along the sap.
                for (int i = 0; i < fill / 14; i++) {
                    float sp = 0.6f + r.nextFloat() * 0.8f;
                    int rx = x0 + (int) ((r.nextFloat() * W + t * sp) % Math.max(1, fill)), ry = top + 1 + r.nextInt(H - 3);
                    g.fill(rx, ry, rx + 3, ry + 2, argb(0.95f, ROACH));
                    g.fill(rx + 1, ry, rx + 2, ry + 1, argb(0.7f, ROACH_SHINE));
                }
            } else if (phase >= 4) {
                // Embers of amber rising through it.
                for (int i = 0; i < fill / 20; i++) {
                    int ex = x0 + r.nextInt(Math.max(1, fill));
                    float ph = ((t * 0.08f + r.nextFloat()) % 1f);
                    g.fill(ex, (int) (top + H - 1 - ph * (H - 1)), ex + 1, (int) (top + H - ph * (H - 1)), argb(1 - ph, 0xFFF2C0));
                }
            } else {
                // Veins in the sap.
                for (int x = 4; x < fill; x += 11) g.fill(x0 + x, top + 2 + (x % 3), x0 + x + 5, top + 3 + (x % 3), argb(0.35f, phase == 3 ? 0xD8C0FF : 0xB8D88A));
            }
            g.fill(x0 + fill - 1, top, x0 + fill, top + H, argb(0.8f, phase >= 4 ? 0xFFF2C0 : 0xD8E8B0));
        }
        for (Flake f : bar.flakes) {
            float a = 1 - f.age / (float) f.life;
            g.fill((int) f.x, (int) f.y, (int) f.x + 2, (int) f.y + 1, argb(a, f.rgb));
        }
        int nameRgb = phase >= 4 ? AMBER_HOT : phase == 3 ? 0xD8C0FF : 0xC8DCA8;
        GuiDraw.centred(g, name, g.guiWidth() / 2f + shake, top - 14, 1f, argb(open, nameRgb), true);
    }

    /** A root growing out from {@code (x, y)}, curling: drawn as tapering segments with a side shoot or two. */
    static void root(GuiGraphics g, Random r, float x, float y, float dx, float dy, int steps, float grow, float t, int rgb, float width) {
        int n = Math.round(steps * grow);
        float px = x, py = y, ax = dx, ay = dy;
        for (int i = 0; i < n; i++) {
            float sway = 0.15f * Mth.sin(t * 0.05f + i * 0.6f);
            ax += (r.nextFloat() - 0.5f) * 0.4f + sway * 0.2f;
            ay += 0.05f;
            float len = (float) Math.sqrt(ax * ax + ay * ay);
            float nx = px + ax / len * 1.6f, ny = py + ay / len * 1.6f;
            float w = Math.max(0.5f, width * (1 - i / (float) steps));
            GuiDraw.line(g, px, py, nx, ny, w, argb(0.9f, rgb));
            if (i > 3 && r.nextInt(7) == 0) GuiDraw.line(g, nx, ny, nx + ax * 2.5f, ny - 1.5f, w * 0.6f, argb(0.7f, rgb));
            px = nx;
            py = ny;
        }
    }
}
