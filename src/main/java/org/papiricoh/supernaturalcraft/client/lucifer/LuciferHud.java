package org.papiricoh.supernaturalcraft.client.lucifer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.papiricoh.supernaturalcraft.client.lucifer.LuciferGui.*;

/**
 * The Cage's boss bar, for both Lucifers' bar keys ({@code entity.supernaturalcraft.lucifer.bar.phase<n>} and
 * {@code entity.supernaturalcraft.lucifer_uncaged.bar.phase<n>}): a frame of black iron with runes that glow in the
 * phase's colour, the phase's fire (or ice, or light) running inside, and the bars of the Cage across it
 * ({@link CageBars}). Against Lucifer the bars slam down as he loses each phase; against Lucifer Uncaged they glow, rime,
 * bend and are torn off and thrown away. Blows rattle the bars and strike embers (ice in the cold) off the edge of the
 * fire. His name burns in above, in gothic capitals. The vanilla bar stays while the textures are missing.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class LuciferHud {

    public static final String LUCIFER_PREFIX = "entity.supernaturalcraft.lucifer.bar";
    public static final String UNCAGED_PREFIX = "entity.supernaturalcraft.lucifer_uncaged.bar";
    /** Space for the name over the frame. */
    private static final int NAME_H = 13;
    /** Ticks for a bar to come down; between one bar and the next; for a change of state to flash. */
    private static final int DROP_TICKS = 12, DROP_STAGGER = 3, FLASH_TICKS = 10;

    private static final class Bar {
        boolean uncaged;
        int phase = -1, prevPhase = -1, changeAge = 1000, landed;
        String name = "";
        int nameAge, rattle, ghostHold, flare;
        float progress = -1, ghost = -1;
        final Debris debris = new Debris();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();

    private LuciferHud() {
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            BARS.clear();
            return;
        }
        for (Bar b : BARS.values()) {
            b.changeAge++;
            b.nameAge++;
            if (b.rattle > 0) b.rattle--;
            if (b.flare > 0) b.flare--;
            // The ghost of the last blows drains after a moment.
            if (b.ghostHold > 0) b.ghostHold--;
            else if (b.ghost > b.progress) b.ghost = Math.max(b.progress, b.ghost - 0.006f);
            b.debris.tick();
        }
    }

    static void clear() {
        BARS.clear();
    }

    /** The phase a bar key names ({@code ...phase<n>}), or -1. */
    static int phaseOf(String key) {
        int i = key.lastIndexOf("phase");
        if (i < 0) return -1;
        try {
            return Integer.parseInt(key.substring(i + 5));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Whether this key is one of the Cage's bars: null if not, else whether it is Uncaged's. */
    static @Nullable Boolean variant(String key) {
        if (key.startsWith(UNCAGED_PREFIX)) return true;
        if (key.startsWith(LUCIFER_PREFIX)) return false;
        return null;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)) return;
        Boolean uncaged = variant(tc.getKey());
        if (uncaged == null || !has(BAR_FRAME)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        event.setCanceled(true);
        event.setIncrement(FRAME_H + NAME_H + 2);
        GuiGraphics g = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int phase = Math.max(1, phaseOf(tc.getKey()));
        Bar bar = BARS.computeIfAbsent(boss.getId(), id -> new Bar());
        bar.uncaged = uncaged;
        int cx = g.guiWidth() / 2, top = event.getY() + NAME_H - 9;
        int x0 = cx - FRAME_W / 2;
        if (bar.phase != phase) {
            if (bar.phase > 0) {
                bar.prevPhase = bar.phase;
                bar.changeAge = 0;
                bar.landed = 0;
                bar.flare = 20;
                breakBars(bar, x0, top);
            }
            bar.phase = phase;
        }
        String name = boss.getName().getString();
        if (!name.equals(bar.name)) {
            bar.name = name;
            bar.nameAge = 0;
        }
        float progress = Mth.clamp(boss.getProgress(), 0, 1);
        Style style = style(uncaged, phase);
        if (bar.progress >= 0 && progress < bar.progress - 0.0005f) {
            // Struck: the bars rattle, the runes flare, sparks fly off the edge of the fire.
            bar.rattle = 6;
            bar.flare = Math.max(bar.flare, 8);
            bar.ghostHold = 14;
            if (bar.ghost < bar.progress) bar.ghost = bar.progress;
            float ex = x0 + FILL_X + FILL_W * progress, ey = top + FILL_Y + FILL_H / 2f;
            int n = 2 + Math.min(6, (int) ((bar.progress - progress) * 500));
            for (int i = 0; i < n; i++) {
                if (cold(phase)) {
                    bar.debris.add(Debris.ICE, ex, ey, (bar.debris.random.nextFloat() - 0.3f) * 2.4f, -1.2f - bar.debris.random.nextFloat() * 1.6f,
                            0.45f + bar.debris.random.nextFloat() * 0.3f, 22 + bar.debris.random.nextInt(14), 0xFFFFFF);
                } else {
                    bar.debris.embers(1, ex, ey, 4, style.rune());
                }
            }
        }
        if (bar.ghost < progress) bar.ghost = progress;
        bar.progress = progress;
        double t = mc.level.getGameTime() + partial;
        drawBar(g, bar, style, x0, top, progress, t, partial);
        drawName(g, bar, style, cx, top - NAME_H + 1, partial);
    }

    /** At a change of phase: the bars Lucifer Uncaged tears out fly off, with their rivets and a shower of sparks. */
    private static void breakBars(Bar bar, int x0, int top) {
        if (!bar.uncaged) return;
        var r = bar.debris.random;
        for (int i = 0; i < CageBars.COUNT; i++) {
            CageBars.State was = CageBars.state(true, bar.prevPhase, i), now = CageBars.state(true, bar.phase, i);
            if (was == null || !was.whole() || (now != null && now.whole())) continue;
            float bx = x0 + barX(i), side = barX(i) < FRAME_W / 2f ? -1 : 1;
            bar.debris.add(Debris.PIECE, bx, top + 9, side * (0.8f + r.nextFloat() * 1.6f), -2.2f - r.nextFloat() * 1.5f, 1f, 40, 0xFFFFFF);
            for (int k = 0; k < 2; k++) {
                bar.debris.add(Debris.RIVET, bx, top + 10 + k * 10, side * r.nextFloat() * 2f, -1.5f - r.nextFloat(), 0.6f, 30, 0xFFFFFF);
            }
            bar.debris.embers(5, bx, top + FILL_Y + 2, 6, style(true, bar.phase).rune());
        }
    }

    private static void drawBar(GuiGraphics g, Bar bar, Style style, int x0, int y, float progress, double t, float partial) {
        int fx = x0 + FILL_X, fy = y + FILL_Y;
        int fill = Math.round(FILL_W * progress), ghost = Math.round(FILL_W * Math.max(progress, bar.ghost));
        g.fill(fx, fy, fx + FILL_W, fy + FILL_H, 0xE8080305);
        // What the last blows took, fading.
        if (ghost > fill) g.fill(fx + fill, fy, fx + ghost, fy + FILL_H, argb(0.55f, lerp(0.4f, style.rune(), 0xFFFFFF)));
        drawFill(g, style, fx, fy, fill, t);
        if (fill > 0 && fill < FILL_W) {
            // The fire's edge: a bright line and a glow past it.
            g.fill(fx + fill - 1, fy, fx + fill, fy + FILL_H, argb(0.85f, lerp(0.6f, style.rune(), 0xFFFFFF)));
            g.fill(fx + fill, fy, fx + fill + 2, fy + FILL_H, argb(0.3f, style.rune()));
        }
        blit(g, BAR_FRAME, x0, y, FRAME_W, FRAME_H, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H, 1, 0xFFFFFF);
        float pulse = 0.5f + 0.18f * Mth.sin((float) t * 0.12f) + 0.035f * Math.max(0, bar.flare - partial);
        glow(g, BAR_RUNES, x0, y, FRAME_W, FRAME_H, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H, Math.min(1, pulse), style.rune());
        drawBars(g, bar, x0, y, t, partial);
        bar.debris.render(g, partial, 1);
    }

    private static void drawBars(GuiGraphics g, Bar bar, int x0, int y, double t, float partial) {
        float since = bar.changeAge + partial;
        for (int i = 0; i < CageBars.COUNT; i++) {
            CageBars.State now = CageBars.state(bar.uncaged, bar.phase, i);
            CageBars.State was = bar.prevPhase > 0 ? CageBars.state(bar.uncaged, bar.prevPhase, i) : now;
            if (now == null) continue;
            float bx = x0 + barX(i) - 5, by = y;
            if (was == null && !bar.uncaged) {
                // The Cage closing: the new bars slam down, the outer ones first.
                int order = Math.min(i, CageBars.COUNT - 1 - i);
                float local = since - order * DROP_STAGGER;
                if (local < 0) continue;
                float k = bounce(local / DROP_TICKS);
                by = y - (1 - k) * (y + BAR_H + 4);
                if (local >= DROP_TICKS * 0.7f && (bar.landed & (1 << i)) == 0) {
                    bar.landed |= 1 << i;
                    for (int d = 0; d < 6; d++) {
                        bar.debris.add(Debris.DUST, bx + 5, y + 4, (bar.debris.random.nextFloat() - 0.5f) * 2.2f,
                                -0.2f - bar.debris.random.nextFloat() * 0.5f, 1, 14 + bar.debris.random.nextInt(8), 0x8A8078);
                    }
                    bar.rattle = Math.max(bar.rattle, 3);
                }
            } else if (now.whole() && bar.rattle > 0) {
                bx += ((i + bar.rattle) % 2 == 0 ? 1 : -1) * 0.75f;
            }
            int u = now.ordinal() * BAR_W;
            blit(g, BAR_BARS, bx, by, BAR_W, BAR_H, u, 0, BAR_W, BAR_H, BAR_W * 8, BAR_H, 1, 0xFFFFFF);
            // Red-hot iron and gold-cracked iron shine.
            if (now == CageBars.State.HOT || now == CageBars.State.CRACKED) {
                float a = 0.3f + 0.15f * Mth.sin((float) t * 0.2f + i);
                glow(g, BAR_BARS, bx, by, BAR_W, BAR_H, u, 0, BAR_W, BAR_H, BAR_W * 8, BAR_H, a, now == CageBars.State.HOT ? 0xFF7A2A : 0xFFE08A);
            }
            // A bar that has just changed (heated, frozen, bent) flashes.
            if (was != null && was != now && since < FLASH_TICKS) {
                glow(g, BAR_BARS, bx, by, BAR_W, BAR_H, u, 0, BAR_W, BAR_H, BAR_W * 8, BAR_H, 1 - since / FLASH_TICKS, 0xFFFFFF);
            }
        }
    }

    /** His name over the bar in gothic capitals, catching fire letter by letter whenever it changes. */
    private static void drawName(GuiGraphics g, Bar bar, Style style, int cx, int y, float partial) {
        String name = bar.name;
        float age = bar.nameAge + partial;
        if (!Gothic.covers(name)) {
            GuiDraw.centred(g, Component.literal(name), cx, y + 2, 1, argb(Mth.clamp(age / 8f, 0, 1), style.name()), true);
            return;
        }
        String upper = Gothic.upper(name);
        float x = cx - Gothic.width(upper, false) / 2f;
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            float k = Mth.clamp((age - i * 1.2f) / 5f, 0, 1);
            if (k > 0) {
                Gothic.glyph(g, c, x + 1, y + 1, 1, k * 0.85f, 0x000000, false, false);
                Gothic.glyph(g, c, x, y, 1, k, style.name(), false, false);
                if (k < 1) Gothic.glyph(g, c, x, y, 1, 1 - k, 0xFFFFFF, false, true);
            }
            x += Gothic.advance(c, false);
        }
    }

    /** The fill: frames of the phase's sheet, running. */
    private static void drawFill(GuiGraphics g, Style style, int fx, int fy, int fill, double t) {
        if (fill <= 0) return;
        var sheet = LuciferGui.fill(style);
        int frame = (int) (t / 3) % FILL_FRAMES;
        if (has(sheet)) {
            blit(g, sheet, fx, fy, fill, FILL_H, 0, frame * FILL_H, fill, FILL_H, FILL_W, FILL_H * FILL_FRAMES, 1, 0xFFFFFF);
        } else {
            g.fill(fx, fy, fx + fill, fy + FILL_H, argb(1, style.rune()));
        }
    }
}
