package org.papiricoh.supernaturalcraft.client.allegiance;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Faction;
import org.papiricoh.supernaturalcraft.allegiance.power.Power;
import org.papiricoh.supernaturalcraft.allegiance.power.PowerRules;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;

import java.util.ArrayList;
import java.util.List;

import static org.papiricoh.supernaturalcraft.client.allegiance.AllegianceGui.*;

/**
 * The power wheel (hold the wheel key): the side's active powers in a ring, each with its icon, cost and cooldown sweep.
 * Point at one with the mouse and let go of the key (or click) to select it; the cast key then casts it. The centre names
 * the one under the mouse and says what it does; the passives sit under the wheel.
 */
public class PowerWheelScreen extends Screen {

    private static final int ICON = WheelLayout.ICON;

    private final KeyMapping hold;
    private List<Power> powers = List.of();
    private int hovered = -1, age;
    private boolean done;

    public PowerWheelScreen(KeyMapping hold) {
        super(Component.translatable("screen.supernaturalcraft.allegiance.wheel"));
        this.hold = hold;
    }

    @Override
    protected void init() {
        powers = ClientPowers.wheel();
        Power sel = ClientPowers.selected();
        if (hovered < 0 && sel != null) hovered = powers.indexOf(sel);
    }

    /** For previews: as if the mouse pointed at slice {@code i}. */
    public void hover(int i) {
        hovered = i;
    }

    @Override
    public void tick() {
        age++;
        if (hold != null && !held(hold)) choose();
    }

    private static boolean held(KeyMapping key) {
        InputConstants.Key k = key.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (k.getType() == InputConstants.Type.MOUSE) return GLFW.glfwGetMouseButton(window, k.getValue()) == GLFW.GLFW_PRESS;
        return k.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, k.getValue());
    }

    private void choose() {
        if (done) return;
        done = true;
        if (hovered >= 0 && hovered < powers.size()) {
            Power p = powers.get(hovered);
            if (p != ClientPowers.selected()) {
                ClientPowers.select(p);
                AllegianceHud.whisper(name(p), colour(p.faction));
            }
        }
        onClose();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (hold != null && hold.matches(keyCode, scanCode)) {
            choose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = sliceAt(mx, my);
        if (i >= 0) hovered = i;
        choose();
        return true;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        int i = sliceAt(mx, my);
        if (i >= 0) hovered = i;
    }

    private int sliceAt(double mx, double my) {
        // The wheel is drawn scaled to fit the window: undo that before asking which slice.
        WheelLayout l = WheelLayout.of(powers.size());
        float fit = fit(l);
        return l.slice((mx - width / 2.0) / fit, (my - height / 2.0) / fit, powers.size());
    }

    /** Small windows: the wheel, its name line above and the words below must fit. */
    private float fit(WheelLayout l) {
        return Math.min(1, (height - 16) / (l.outer() * 2 + 90));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fillGradient(0, 0, width, height, 0x60000000, 0x90000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        Allegiance a = ClientPowers.mine();
        Faction f = a.faction();
        float cx = width / 2f, cy = height / 2f;
        float open = Mth.clamp((age + partial) / 4f, 0, 1);
        int n = powers.size();
        WheelLayout l = WheelLayout.of(n);
        float outerR = l.outer(), innerR = l.inner(), iconR = l.iconR();
        float fit = fit(l);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        float grow = (0.85f + 0.15f * open) * fit;
        g.pose().scale(grow, grow, 1);
        g.pose().translate(-cx, -cy, 0);
        int tint = colour(f), deep = deep(f);
        boolean art = has(WHEEL);
        if (art) {
            // wheel.png: the ring at (128,128), r 52..120, cut out as a disc so the sheet's corner pieces stay out;
            // the medallion (40x40 at 216,216) in the middle.
            float k = outerR / 120f;
            spriteArc(g, WHEEL, cx, cy, 240 * k, 8, 8, 240, 256, 256, 0, Mth.TWO_PI, argb(open, 0xFFFFFF));
            float m = 40 * k * 1.55f;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.setColor(1, 1, 1, open);
            g.pose().pushPose();
            g.pose().translate(cx - m / 2, cy - m / 2, 0);
            g.pose().scale(m / 40f, m / 40f, 1);
            g.blit(WHEEL, 0, 0, 216, 216, 40, 40, 256, 256);
            g.pose().popPose();
            g.setColor(1, 1, 1, 1);
        } else {
            annulus(g, cx, cy, outerR, outerR + 3, 0, Mth.TWO_PI, argb(open, deep));
            annulus(g, cx, cy, innerR, outerR, 0, Mth.TWO_PI, argb(0.72f * open, 0x0E0B10));
            annulus(g, cx, cy, innerR - 2, innerR, 0, Mth.TWO_PI, argb(open, deep));
        }
        if (n == 0) {
            GuiDraw.centred(g, Component.translatable("hud.supernaturalcraft.allegiance.no_powers"), cx, cy - 4, 1, argb(open, tint), true);
        }
        float step = Mth.TWO_PI / Math.max(1, n);
        // The glow behind the hovered icon: as wide as the room between icons allows.
        float glow = Math.min(48, l.chord(n) + 8);
        Power selected = ClientPowers.selected();
        for (int i = 0; i < n; i++) {
            Power p = powers.get(i);
            float mid = i * step;
            boolean hot = i == hovered;
            if (hot) {
                float inner = l.ringInner(art);
                annulus(g, cx, cy, inner + 1, outerR - 1, mid - step / 2 + 0.02f, mid + step / 2 - 0.02f, argb(0.28f * open, tint));
                if (art) {
                    float gx = cx + Mth.sin(mid) * iconR, gy = cy - Mth.cos(mid) * iconR, gs = glow;
                    com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                    g.setColor(((tint >> 16) & 255) / 255f, ((tint >> 8) & 255) / 255f, (tint & 255) / 255f, 0.8f * open);
                    g.pose().pushPose();
                    g.pose().translate(gx - gs / 2, gy - gs / 2, 0);
                    g.pose().scale(gs / 32f, gs / 32f, 1);
                    g.blit(WHEEL, 0, 0, 0, 0, 32, 32, 256, 256);
                    g.pose().popPose();
                    g.setColor(1, 1, 1, 1);
                }
            }
            if (n > 1) {
                float edge = mid + step / 2, in = l.ringInner(art);
                GuiDraw.line(g, cx + Mth.sin(edge) * in, cy - Mth.cos(edge) * in, cx + Mth.sin(edge) * outerR, cy - Mth.cos(edge) * outerR,
                        1, argb(0.5f * open, deep));
            }
            float ix = cx + Mth.sin(mid) * iconR - ICON / 2f, iy = cy - Mth.cos(mid) * iconR - ICON / 2f;
            PowerRules.Verdict v = ClientPowers.verdict(p);
            boolean ready = v == PowerRules.Verdict.OK;
            drawIcon(g, p, ix, iy, ICON, open * (ready ? 1 : 0.5f));
            float cd = ClientPowers.cooldownShare(p, partial);
            if (cd > 0) pie(g, ix + ICON / 2f, iy + ICON / 2f, ICON / 2f + 1, Mth.TWO_PI * (1 - cd), Mth.TWO_PI, argb(0.62f * open, 0x000000));
            if (p == selected) annulus(g, ix + ICON / 2f, iy + ICON / 2f, ICON / 2f + 1.5f, ICON / 2f + 3, 0, Mth.TWO_PI, argb(open, tint));
            // Cost under the icon, red when there is not enough.
            String cost = String.valueOf(ClientPowers.cost(p));
            int cc = v == PowerRules.Verdict.NO_ESSENCE ? 0xE05050 : 0xE8DCC0;
            g.pose().pushPose();
            g.pose().translate(ix + ICON / 2f, iy + ICON + 2, 0);
            g.pose().scale(l.costScale(), l.costScale(), 1);
            g.drawString(font, cost, -font.width(cost) / 2, 0, argb(open, cc), true);
            g.pose().popPose();
        }
        // The centre: the power under the mouse, its cost and what it does.
        if (hovered >= 0 && hovered < n) {
            Power p = powers.get(hovered);
            GuiDraw.centred(g, name(p), cx, cy - 8, 1, argb(open, tint), true);
            int secs = Math.round(p.cooldown / 20f);
            Component meta = Component.translatable("screen.supernaturalcraft.allegiance.wheel.meta", ClientPowers.cost(p), secs);
            GuiDraw.centred(g, meta, cx, cy + 3, 0.6f, argb(open * 0.85f, 0xE8DCC0), true);
            int y = Math.round(cy + outerR + 10);
            Component desc = Component.translatableWithFallback(p.descKey(), "");
            List<FormattedCharSequence> lines = font.split(desc, 240);
            int shown = Math.min(3, lines.size());
            if (shown > 0) g.fill(Math.round(cx - 126), y - 3, Math.round(cx + 126), y + shown * 10 + 1, argb(0.55f * open, 0x0A080C));
            for (int i = 0; i < shown; i++) {
                FormattedCharSequence line = lines.get(i);
                g.drawString(font, line, Math.round(cx - font.width(line) / 2f), y, argb(open, 0xE8DCC0), true);
                y += 10;
            }
        }
        Component essence = Component.translatable("screen.supernaturalcraft.allegiance.wheel.essence." + key(f),
                Math.round(a.essence()), a.maxEssence());
        GuiDraw.centred(g, essence, cx, cy - outerR - 14, 1, argb(open, tint), true);
        if (ClientAllegiance.silenced()) {
            // The Curse of Silence: the wheel's heart stitched shut, and the curse named under it.
            org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay.stitches(g, cx, cy + 16, innerR * 1.4f, open);
            GuiDraw.centred(g, Component.translatable("effect.supernaturalcraft.silenced"), cx, cy + 24, 0.7f,
                    argb(open, org.papiricoh.supernaturalcraft.client.magic.fx.MagicOverlay.SILENCE), true);
        }
        g.pose().popPose();
        drawPassives(g, a, open);
    }

    /** The passives of the side at this rank, small, down the left edge (their names beside them). */
    private void drawPassives(GuiGraphics g, Allegiance a, float open) {
        List<Power> passives = new ArrayList<>();
        for (Power p : Power.of(a.faction(), a.rank())) if (p.passive) passives.add(p);
        if (passives.isEmpty()) return;
        int size = 14, x = 8, y = 10;
        GuiDraw.text(g, Component.translatable("screen.supernaturalcraft.allegiance.wheel.passives"), x, y, 0.75f, argb(open * 0.85f, 0xC8BCA8), true);
        y += 10;
        for (Power p : passives) {
            drawIcon(g, p, x, y, size, open * 0.9f);
            GuiDraw.text(g, name(p), x + size + 4, y + 4, 0.75f, argb(open * 0.85f, colour(p.faction)), true);
            y += size + 3;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
