package org.papiricoh.supernaturalcraft.client.godstiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.client.lucifer.Gothic;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielAssets;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielBalance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Godstiel's boss bar (v0.39), for his bar keys ({@code entity.supernaturalcraft.godstiel.bar.phase<n>}): "light into goo". A
 * stained-glass halo frame, white and pale gold; his health is soul light in the window, and from phase III the Leviathans' black
 * eats in from the right behind a dripping edge, more of the window each phase, the frame cracking. Five halo pips below, one per
 * phase: lit, or blackened once lost. While he struggles the light throbs and the goo recoils. Sheet layout:
 * {@link GodstielAssets#BAR_SHEET} (and {@code tools/artgen/godstiel_gui_art.py}).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class GodstielHud {

    static final ResourceLocation SHEET = SupernaturalCraft.asResource(GodstielAssets.BAR_SHEET);
    static final int TW = 256, TH = 128;
    static final int W = 256, H = 32;
    static final int FILL_X = 24, FILL_Y = 12, FILL_W = 208, FILL_H = 8, LIGHT_V = 32, GOO_V = 40;
    static final int DRIP_V = 48, DRIP_W = 16, DRIP_H = 12, DRIP_FRAMES = 8;
    static final int CRACKS_V = 64, PIP = 16, PIP_LIT_V = 96, PIP_DARK_V = 112, SEAL_U = 128, SEAL_V = 96, SEAL = 32;
    static final int NAME = 0xFFF6DC, NAME_DARK = 0xC7A6FF;

    private static final class Mote {
        float x, y, vx, vy;
        int age;
        final int life, rgb;

        Mote(float x, float y, boolean dark, Random r) {
            this.x = x;
            this.y = y;
            vx = (r.nextFloat() - 0.5f) * 0.8f;
            vy = dark ? 0.2f + r.nextFloat() * 0.5f : -0.3f - r.nextFloat() * 0.6f;
            life = 14 + r.nextInt(12);
            rgb = dark ? (r.nextBoolean() ? 0x150A1E : 0x3A1F52) : (r.nextBoolean() ? 0xBFE9FF : 0xFFF6DC);
        }
    }

    private static final class Bar {
        int age, flinch;
        float lastProgress = -1;
        final List<Mote> motes = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static long ticks, struggleUntil, crackAt = -100;

    private GodstielHud() {
    }

    /** He fell into (or out of) the inner fight. */
    static void struggle(boolean on, int duration) {
        struggleUntil = on ? ticks + duration : ticks;
    }

    /** The cracks opened (a phase change from III). */
    static void crack() {
        crackAt = ticks;
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
            if (b.flinch > 0) b.flinch--;
            for (Mote m : b.motes) {
                m.age++;
                m.x += m.vx;
                m.y += m.vy;
                m.vx *= 0.95f;
            }
            b.motes.removeIf(m -> m.age >= m.life);
        }
    }

    static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '5' ? c - '0' : 1;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(GodstielAssets.BAR_PREFIX)) return;
        // Without the sheet (art not there yet) the vanilla bar stands in.
        if (!MichaelGui.has(SHEET)) return;
        event.setCanceled(true);
        event.setIncrement(H + 26);
        if (Minecraft.getInstance().level == null || CameraDirector.active()) return;
        draw(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1), event.getY(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    private static void blit(GuiGraphics g, float x, float y, int w, int h, float u, float v, float a) {
        MichaelGui.blit(g, SHEET, x, y, w, h, u, v, w, h, TW, TH, a, 0xFFFFFF);
    }

    /** How much of the window the goo has taken in {@code phase} (0 in I-II, growing from III). */
    static float corruption(int phase) {
        return phase < GodstielBalance.LEVIATHAN_PHASE ? 0f : Math.min(1f, (phase - GodstielBalance.LEVIATHAN_PHASE + 1) / 3f);
    }

    /** One bar, the frame's top at {@code y} (previews call this too). */
    public static void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        if (!MichaelGui.has(SHEET)) return;
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        int x0 = g.guiWidth() / 2 - W / 2;
        float t = ticks + partial;
        float open = Mth.clamp((bar.age + partial) / 12f, 0, 1);
        int fill = Math.round(FILL_W * progress);
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 5;
            int n = 2 + Math.min(8, (int) ((bar.lastProgress - progress) * 600));
            for (int i = 0; i < n; i++) bar.motes.add(new Mote(x0 + FILL_X + fill, y + FILL_Y + 4, phase >= GodstielBalance.LEVIATHAN_PHASE, RANDOM));
        }
        bar.lastProgress = progress;
        boolean struggling = ticks < struggleUntil;
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        if (ticks - crackAt < 12) shake += (ticks - crackAt) % 2 == 0 ? 2 : -2;
        float x = x0 + shake;
        // The frame, drawn in from the middle.
        int shown = Math.round(W * open);
        blit(g, x + (W - shown) / 2f, y, shown, H, (W - shown) / 2f, 0, 1);
        if (open < 1) return;
        // The window: soul light up to his health; the goo over the light's right part from phase III.
        if (fill > 0) {
            float throb = struggling ? 0.75f + 0.25f * Mth.sin(t * 0.8f) : 1f;
            blit(g, x + FILL_X, y + FILL_Y, fill, FILL_H, 0, LIGHT_V, throb);
            MichaelGui.glow(g, SHEET, x + FILL_X, y + FILL_Y, fill, FILL_H, 0, LIGHT_V, fill, FILL_H, TW, TH, 0.25f + 0.15f * Mth.sin(t * 0.2f), 0xFFFFFF);
            float goo = corruption(phase) * (struggling ? 0.7f : 1f);
            int gw = Math.round(fill * goo);
            if (gw > 0) {
                int gx = fill - gw;
                blit(g, x + FILL_X + gx, y + FILL_Y, gw, FILL_H, gx, GOO_V, 1);
                int frame = (int) (t / 3) % DRIP_FRAMES;
                blit(g, x + FILL_X + gx - DRIP_W / 2f, y + FILL_Y - 2, DRIP_W, DRIP_H, frame * DRIP_W, DRIP_V, 1);
            }
        }
        // The frame's cracks, darker each phase from III.
        float cracks = corruption(phase);
        if (cracks > 0) blit(g, x, y, W, H, 0, CRACKS_V, Math.min(1f, 0.35f + cracks * 0.65f));
        // His name, in gothic capitals, white light darkening to violet.
        Gothic.centred(g, name.getString(), x + W / 2f, y - 1, 1f, 1f, phase >= GodstielBalance.LEVIATHAN_PHASE ? NAME_DARK : NAME, false);
        // The five halo pips.
        float px = x + W / 2f - GodstielBalance.PHASES * (PIP + 2) / 2f;
        for (int i = 1; i <= GodstielBalance.PHASES; i++) {
            boolean lost = i < phase;
            float pulse = i == phase ? 0.8f + 0.2f * Mth.sin(t * 0.25f) : 1f;
            blit(g, px + (i - 1) * (PIP + 2), y + H + 2, PIP, PIP, (i - 1) * PIP, lost ? PIP_DARK_V : PIP_LIT_V, pulse);
        }
        for (Mote m : bar.motes) {
            float a = 1 - m.age / (float) m.life;
            g.fill((int) m.x, (int) m.y, (int) m.x + 1, (int) m.y + 1, MichaelGui.argb(a, m.rgb));
        }
    }
}
