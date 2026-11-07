package org.papiricoh.supernaturalcraft.client.apocalypse;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.client.lucifer.Gothic;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The Apocalypse's boss bars (v0.40: Alastair, Uriel) share one layout, each from its own 256x128 sheet (laid out by
 * {@code alastair_gui_art.py} / {@code uriel_art.py}): a 256x40 band with a 188x10 trough at (34, 22) and the name over it; the
 * health filling the trough from the left (fill at (0, 40)), the drained part after it ((0, 50)); an animated edge between the
 * two (6 frames of 8x16 at (96, 64)); one emblem per phase hanging under the band's right end (24 wide, at (0, 64): whole,
 * the current phase's, lost), the current one swinging. A blow throws motes off the edge (blood that falls, or sparks that
 * rise). The HUD classes recognise their keys and call {@link #draw}.
 */
public final class ApocalypseBar {

    public static final int TW = 256, TH = 128, W = 256, H = 40;
    static final int TROUGH_X = 34, TROUGH_Y = 22, TROUGH_W = 188, TROUGH_H = 10, FILL_V = 40, DRAINED_V = 50;
    static final int EMBLEM_V = 64, EMBLEM_W = 24, EDGE_U = 96, EDGE_V = 64, EDGE_W = 8, EDGE_H = 16, EDGE_FRAMES = 6;

    private final ResourceLocation sheet;
    private final int phases, emblemH, edgeDy, nameRgb, moteRgbA, moteRgbB;
    /** Motes fall (blood, +) or rise (sparks, -). */
    private final float gravity;
    private final Map<UUID, Bar> bars = new HashMap<>();
    private final Random random = new Random();
    private long ticks;

    private static final class Mote {
        float x, y, vx, vy;
        int age;
        final int life, rgb;

        Mote(float x, float y, int rgb, float gravity, Random r) {
            this.x = x;
            this.y = y;
            this.rgb = rgb;
            vx = (r.nextFloat() - 0.5f) * 1.2f;
            vy = gravity > 0 ? 0.2f + r.nextFloat() * 0.6f : -0.4f - r.nextFloat() * 0.8f;
            life = 14 + r.nextInt(14);
        }
    }

    private static final class Bar {
        int age, flinch;
        float lastProgress = -1;
        final List<Mote> motes = new ArrayList<>();
    }

    /**
     * @param emblemH an emblem's height on the sheet
     * @param edgeDy  where the edge's frame sits relative to the trough's top (a drip hangs from it: 0; a flame stands over it: -6)
     */
    public ApocalypseBar(ResourceLocation sheet, int phases, int emblemH, int edgeDy, int nameRgb, int moteRgbA, int moteRgbB, float gravity) {
        this.sheet = sheet;
        this.phases = phases;
        this.emblemH = emblemH;
        this.edgeDy = edgeDy;
        this.nameRgb = nameRgb;
        this.moteRgbA = moteRgbA;
        this.moteRgbB = moteRgbB;
        this.gravity = gravity;
    }

    /** The phase in a bar key ({@code ...bar.phase<n>}; 1 if none). */
    public static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '9' ? c - '0' : 1;
    }

    /** Once a client tick (forgets everything without a level). */
    public void tick(boolean inWorld) {
        if (!inWorld) {
            bars.clear();
            return;
        }
        ticks++;
        for (Bar b : bars.values()) {
            b.age++;
            if (b.flinch > 0) b.flinch--;
            for (Mote m : b.motes) {
                m.age++;
                m.x += m.vx;
                m.y += m.vy;
                m.vy += gravity * 0.08f;
                m.vx *= 0.95f;
            }
            b.motes.removeIf(m -> m.age >= m.life);
        }
    }

    public boolean ready() {
        return MichaelGui.has(sheet);
    }

    private void blit(GuiGraphics g, float x, float y, int w, int h, float u, float v, float a) {
        MichaelGui.blit(g, sheet, x, y, w, h, u, v, w, h, TW, TH, a, 0xFFFFFF);
    }

    /** One bar, its band's top at {@code y}. */
    public void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        if (!ready()) return;
        Bar bar = bars.computeIfAbsent(id, k -> new Bar());
        int x0 = g.guiWidth() / 2 - W / 2;
        float open = Mth.clamp((bar.age + partial) / 12f, 0, 1);
        float t = ticks + partial;
        int fill = Math.round(TROUGH_W * Mth.clamp(progress, 0, 1));
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 5;
            int n = 3 + Math.min(8, (int) ((bar.lastProgress - progress) * 600));
            for (int i = 0; i < n; i++)
                bar.motes.add(new Mote(x0 + TROUGH_X + fill, y + TROUGH_Y + 5, random.nextBoolean() ? moteRgbA : moteRgbB, gravity, random));
        }
        bar.lastProgress = progress;
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        float x = x0 + shake;
        // The band slides open from the middle.
        int shown = Math.round(W * open);
        blit(g, x + (W - shown) / 2f, y, shown, H, (W - shown) / 2f, 0, 1);
        if (open < 1) return;
        Gothic.centred(g, name.getString(), x + W / 2f, y + 5, 1f, 1f, nameRgb, false);
        if (fill > 0) blit(g, x + TROUGH_X, y + TROUGH_Y, fill, TROUGH_H, 0, FILL_V, 1);
        if (fill < TROUGH_W) blit(g, x + TROUGH_X + fill, y + TROUGH_Y, TROUGH_W - fill, TROUGH_H, fill, DRAINED_V, 1);
        if (fill > 0 && fill < TROUGH_W) {
            int frame = (int) (t / 3) % EDGE_FRAMES;
            blit(g, x + TROUGH_X + fill - EDGE_W / 2f, y + TROUGH_Y + edgeDy, EDGE_W, EDGE_H, EDGE_U + frame * EDGE_W, EDGE_V, 1);
        }
        // The phases' emblems under the band's right end; the current one swings.
        for (int s = 1; s <= phases; s++) {
            int state = s < phase ? 2 : s == phase ? 1 : 0;
            float ex = x + W - 16 - (phases - s + 1) * (EMBLEM_W + 2) + EMBLEM_W / 2f;
            float swing = state == 1 ? 8f * Mth.sin(t * 0.12f + s) : state == 0 ? 2f * Mth.sin(t * 0.05f + s * 2) : 0;
            g.pose().pushPose();
            g.pose().translate(ex, y + H - 2, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(swing));
            blit(g, -EMBLEM_W / 2f, 0, EMBLEM_W, emblemH, state * EMBLEM_W, EMBLEM_V, state == 2 ? 0.8f : 1);
            g.pose().popPose();
        }
        for (Mote m : bar.motes) {
            float a = 1 - m.age / (float) m.life;
            g.fill((int) m.x, (int) m.y, (int) m.x + 1, (int) m.y + 1, MichaelGui.argb(a, m.rgb));
        }
    }
}
