package org.papiricoh.supernaturalcraft.client.allegiance;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.Ranks;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.allegiance.AllegianceGui.*;

/**
 * The allegiance on the HUD (v0.13): a round emblem left of the mana vial with the essence ring around it (Grace gold,
 * Corruption black-red; a hunter's is plain ink) and the rank in Roman numerals, the selected power beside it with its
 * cooldown sweeping, a line above the hotbar when a cast is refused or something whispers, and the Angel Radio's marks
 * around the crosshair, each pointing where a demon or a boss is.
 */
public final class AllegianceHud implements LayeredDraw.Layer {

    /** Emblem size (the ring is drawn around it). */
    private static final int EMBLEM = 20, RING = 30;
    private static final int LINE_TICKS = 60;

    private static Component line;
    private static int lineColour, lineAge = LINE_TICKS;
    private static float shownShare = -1;
    private static int suppressedFlash;

    /** An Angel Radio mark: where it whispers from, until when (client ticks), and what (0 demon, 1 boss). */
    private record Ping(Vec3 at, int kind, long until, long from) {
    }

    private static final List<Ping> PINGS = new ArrayList<>();
    private static long ticks;

    /** A refused cast ("Not enough Grace", "Still recovering"…): red, above the hotbar. */
    public static void deny(Component text) {
        say(text, 0xE05050);
    }

    /** Something whispers (bloodlust, a power given back…). */
    public static void whisper(Component text, int rgb) {
        say(text, rgb);
    }

    private static void say(Component text, int rgb) {
        line = text;
        lineColour = rgb;
        lineAge = 0;
    }

    public static void ping(Vec3 at, int kind, int duration) {
        PINGS.add(new Ping(at, kind, ticks + Math.max(20, duration), ticks));
    }

    public static void suppressedFlash() {
        suppressedFlash = 40;
    }

    static void tick() {
        ticks++;
        if (lineAge < LINE_TICKS) lineAge++;
        if (suppressedFlash > 0) suppressedFlash--;
        PINGS.removeIf(p -> p.until < ticks);
    }

    static void clear() {
        PINGS.clear();
        line = null;
        shownShare = -1;
    }

    /** Where the emblem's centre is on a screen of this size (the book preview and tests ask too). */
    public static int[] anchor(int guiWidth, int guiHeight) {
        return new int[]{guiWidth / 2 - 125, guiHeight - 40};
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        drawPings(g, mc, player, partial);
        drawLine(g, mc, partial);
        Allegiance a = ClientPowers.mine();
        if (!a.committed() && a.rank() <= 0) {
            shownShare = -1;
            return;
        }
        int[] c = anchor(g.guiWidth(), g.guiHeight());
        drawEmblemBlock(g, mc.font, a, c[0], c[1], partial, ClientAllegiance.suppressed());
        if (a.committed()) drawSelected(g, mc.font, c[0] - RING / 2 - 4, c[1], partial);
    }

    /** The emblem, its ring and the rank: also drawn by previews. */
    public static void drawEmblemBlock(GuiGraphics g, Font font, Allegiance a, int cx, int cy, float partial, boolean suppressed) {
        Faction f = a.faction();
        float share = a.committed() ? a.essence() / Math.max(1f, a.maxEssence()) : 1;
        shownShare = shownShare < 0 ? share : Mth.lerp(0.25f, shownShare, share);
        float alpha = suppressed ? 0.45f : 1f;
        // A dark disc behind, so the ring reads on any sky.
        pie(g, cx, cy, RING / 2f + 1, 0, Mth.TWO_PI, argb(0.55f, 0x0A080C));
        drawRing(g, f, cx, cy, RING, a.committed() ? shownShare : 1, alpha);
        drawEmblem(g, f, cx, cy, EMBLEM, alpha);
        if (suppressed) {
            // Chuck's hand over it: a strike through the emblem.
            GuiDraw.line(g, cx - 9, cy + 9, cx + 9, cy - 9, 2, argb(suppressedFlash > 0 && suppressedFlash / 4 % 2 == 0 ? 1 : 0.8f, 0xD8D0C0));
        }
        String roman = Ranks.roman(a.rank());
        if (!roman.isEmpty()) {
            int w = font.width(roman);
            int rx = cx - w / 2, ry = cy + RING / 2 - 4;
            g.fill(rx - 2, ry - 1, rx + w + 2, ry + 8, argb(0.85f, deep(f)));
            g.drawString(font, roman, rx, ry, argb(1, colour(f)), false);
        }
    }

    private static void drawSelected(GuiGraphics g, Font font, int right, int cy, float partial) {
        Power p = ClientPowers.selected();
        if (p == null) return;
        int size = 16, x = right - size, y = cy - size / 2;
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, argb(0.6f, 0x0A080C));
        boolean ok = ClientPowers.verdict(p) == org.papiricoh.supernaturalcraft.allegiance.power.PowerRules.Verdict.OK;
        drawIcon(g, p, x, y, size, ok ? 1 : 0.55f);
        float cd = ClientPowers.cooldownShare(p, partial);
        if (cd > 0) pie(g, x + size / 2f, y + size / 2f, size / 2f + 0.5f, Mth.TWO_PI * (1 - cd), Mth.TWO_PI, argb(0.6f, 0x000000));
    }

    private static void drawLine(GuiGraphics g, Minecraft mc, float partial) {
        if (line == null || lineAge >= LINE_TICKS) return;
        float a = Math.min(1, (LINE_TICKS - lineAge - partial) / 15f);
        if (a <= 0.02f) return;
        int y = g.guiHeight() - 72;
        GuiDraw.centred(g, line, g.guiWidth() / 2f, y, 1, argb(a, lineColour), true);
    }

    /** The radio's marks: a ring of diamonds round the crosshair, each turned toward its whisper, fading as it ends. */
    private static void drawPings(GuiGraphics g, Minecraft mc, Player player, float partial) {
        if (PINGS.isEmpty()) return;
        float cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f, r = 34;
        Vec3 eye = player.getEyePosition(partial);
        float yaw = player.getViewYRot(partial);
        for (Ping p : PINGS) {
            Vec3 d = p.at.subtract(eye);
            double dist = Math.sqrt(d.x * d.x + d.z * d.z);
            float bearing = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            float rel = Mth.wrapDegrees(bearing - yaw) * Mth.DEG_TO_RAD;
            float life = Mth.clamp((p.until - ticks - partial) / 20f, 0, 1) * Mth.clamp((ticks + partial - p.from) / 6f, 0, 1);
            int colour = p.kind == 1 ? 0xFF6A2A : 0xD02838;
            float px = cx + Mth.sin(rel) * r, py = cy - Mth.cos(rel) * r;
            float s = p.kind == 1 ? 4.5f : 3.2f;
            float pulse = 0.75f + 0.25f * Mth.sin((ticks + partial) * 0.3f);
            int c = argb(life * pulse, colour);
            GuiDraw.line(g, px, py - s, px + s, py, 1.6f, c);
            GuiDraw.line(g, px + s, py, px, py + s, 1.6f, c);
            GuiDraw.line(g, px, py + s, px - s, py, 1.6f, c);
            GuiDraw.line(g, px - s, py, px, py - s, 1.6f, c);
            if (dist < 200) {
                String m = (int) dist + "m";
                var font = mc.font;
                g.pose().pushPose();
                g.pose().translate(px + Mth.sin(rel) * 9, py - Mth.cos(rel) * 9, 0);
                g.pose().scale(0.5f, 0.5f, 1);
                g.drawString(font, m, -font.width(m) / 2, -4, argb(life * 0.8f, 0xE8DCC0), true);
                g.pose().popPose();
            }
        }
    }
}
