package org.papiricoh.supernaturalcraft.client.crowley;

import com.mojang.math.Axis;
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
import org.papiricoh.supernaturalcraft.entity.boss.crowley.CrowleyAssets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Crowley's boss bar (v0.35), for his bar keys ({@code entity.supernaturalcraft.crowley.bar.phase<n>}): the contract. Aged
 * parchment with its small print, his name across the top in gothic capitals, and his health as his signature in blood ink on
 * the line: as he loses health it burns away from its end, a flickering burning edge eating along it, char and holes behind,
 * embers lifting off each blow. Three red wax seals with a crown hang off its lower right edge, one per phase: those of phases
 * he has lost are broken (a seal cracks first, then splits with chips of wax flying), the current one beats faintly.
 * Sheet layout: {@code tools/artgen/crowley_gui_art.py}.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class CrowleyHud {

    static final ResourceLocation SHEET = SupernaturalCraft.asResource(CrowleyAssets.BAR_SHEET);
    static final int TW = 256, TH = 128;
    static final int W = 256, H = 40;
    static final int SIG_X = 40, SIG_Y = 21, SIG_W = 176, SIG_H = 12, SIG_V = 40, SCORCH_V = 52;
    static final int BURN_V = 64, BURN_W = 12, BURN_H = 20, BURN_FRAMES = 8;
    static final int SEAL_V = 88, SEAL = 24;
    static final int INK = 0x2A1A10, BLOOD = 0x8E1212;
    /** Ticks a seal shows cracked before it splits. */
    static final int CRACK_TICKS = 10;

    private static final class Mote {
        float x, y, vx, vy;
        int age;
        final int life, rgb;
        final boolean wax;

        Mote(float x, float y, boolean wax, Random r) {
            this.x = x;
            this.y = y;
            this.wax = wax;
            vx = (r.nextFloat() - 0.5f) * (wax ? 2.2f : 0.6f);
            vy = wax ? -0.8f - r.nextFloat() * 1.2f : -0.3f - r.nextFloat() * 0.6f;
            life = wax ? 18 + r.nextInt(10) : 16 + r.nextInt(14);
            rgb = wax ? (r.nextBoolean() ? 0x8E1414 : 0xB0221C) : (r.nextInt(3) == 0 ? 0xFFE8A0 : r.nextBoolean() ? 0xFF6A1A : 0x3A2A20);
        }
    }

    private static final class Bar {
        int age, flinch;
        float lastProgress = -1;
        int lastPhase = -1;
        final List<Mote> motes = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    /** When each seal broke (client tick), so it cracks then splits: seal 1-3. */
    private static final long[] BROKE = new long[4];
    private static final Random RANDOM = new Random();
    private static long ticks;

    private CrowleyHud() {
    }

    /** A seal breaks now ({@code SEAL_BREAK}, or a phase change seen on the bar). */
    public static void sealBroken(int seal) {
        if (seal >= 1 && seal <= 3) BROKE[seal] = ticks;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            BARS.clear();
            java.util.Arrays.fill(BROKE, 0);
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
                m.vy += m.wax ? 0.12f : -0.01f;
                m.vx *= 0.96f;
            }
            b.motes.removeIf(m -> m.age >= m.life);
        }
    }

    static int phase(String key) {
        char c = key.charAt(key.length() - 1);
        return c >= '1' && c <= '3' ? c - '0' : 1;
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(CrowleyAssets.BAR_PREFIX)) return;
        event.setCanceled(true);
        event.setIncrement(H + 30);
        if (Minecraft.getInstance().level == null || CameraDirector.active()) return;
        draw(event.getGuiGraphics(), boss.getId(), boss.getName(), phase(tc.getKey()), Mth.clamp(boss.getProgress(), 0, 1), event.getY(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    private static void blit(GuiGraphics g, float x, float y, int w, int h, float u, float v, float a) {
        MichaelGui.blit(g, SHEET, x, y, w, h, u, v, w, h, TW, TH, a, 0xFFFFFF);
    }

    /** One bar, the parchment's top at {@code y} (previews call this too). */
    public static void draw(GuiGraphics g, UUID id, Component name, int phase, float progress, int y, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (!MichaelGui.has(SHEET)) return;
        Bar bar = BARS.computeIfAbsent(id, k -> new Bar());
        int x0 = g.guiWidth() / 2 - W / 2;
        float open = Mth.clamp((bar.age + partial) / 14f, 0, 1);
        float t = ticks + partial;
        int fill = Math.round(SIG_W * progress);
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            bar.flinch = 5;
            int n = 3 + Math.min(8, (int) ((bar.lastProgress - progress) * 600));
            for (int i = 0; i < n; i++) bar.motes.add(new Mote(x0 + SIG_X + fill, y + SIG_Y + 6, false, RANDOM));
        }
        if (bar.lastPhase > 0 && phase > bar.lastPhase && ticks - BROKE[bar.lastPhase] > 40) sealBroken(bar.lastPhase);
        bar.lastProgress = progress;
        bar.lastPhase = phase;
        int shake = bar.flinch > 0 ? (bar.flinch % 2 == 0 ? 1 : -1) : 0;
        float x = x0 + shake;
        // Unrolling: the parchment opens from the middle.
        int shown = Math.round(W * open);
        blit(g, x + (W - shown) / 2f, y, shown, H, (W - shown) / 2f, 0, 1);
        if (open < 1) return;
        // His name, in ink.
        String text = name.getString();
        Gothic.centred(g, text, x + W / 2f, y + 5, 1f, 1f, INK, false);
        // The signature to his health; the burnt band after it; the burning edge between.
        if (fill > 0) blit(g, x + SIG_X, y + SIG_Y, fill, SIG_H, 0, SIG_V, 1);
        if (fill < SIG_W) blit(g, x + SIG_X + fill, y + SIG_Y, SIG_W - fill, SIG_H, fill, SCORCH_V, 1);
        if (fill > 0 && fill < SIG_W) {
            int frame = (int) (t / 2) % BURN_FRAMES;
            MichaelGui.glow(g, SHEET, x + SIG_X + fill - BURN_W / 2f, y + SIG_Y - 6, BURN_W, BURN_H, frame * BURN_W, BURN_V, BURN_W, BURN_H, TW, TH,
                    0.85f + 0.15f * Mth.sin(t * 0.9f), 0xFFFFFF);
            if (RANDOM.nextFloat() < 0.15f) bar.motes.add(new Mote(x0 + SIG_X + fill, y + SIG_Y + 2, false, RANDOM));
        }
        // The seals hanging off the lower right edge.
        for (int s = 1; s <= 3; s++) seal(g, bar, s, phase, x + 172 + s * 22, y + H + 1, t);
        for (Mote m : bar.motes) {
            float a = 1 - m.age / (float) m.life;
            int sz = m.wax ? 2 : 1;
            g.fill((int) m.x, (int) m.y, (int) m.x + sz, (int) m.y + sz, MichaelGui.argb(a, m.rgb));
        }
    }

    private static void seal(GuiGraphics g, Bar bar, int s, int phase, float cx, float cy, float t) {
        int state = 0;
        float since = BROKE[s] > 0 ? ticks - BROKE[s] : Float.MAX_VALUE;
        if (s < phase) state = since < CRACK_TICKS ? 1 : 2;
        else if (since < CRACK_TICKS) state = 1;
        if (since >= CRACK_TICKS && since < CRACK_TICKS + 1) {
            // The split: chips of wax flying.
            for (int i = 0; i < 8; i++) bar.motes.add(new Mote(cx, cy, true, RANDOM));
        }
        float tilt = (s - 2) * 8f + (state == 2 ? 14 * (s % 2 == 0 ? -1 : 1) : 0);
        float pulse = s == phase ? 1 + 0.04f * Mth.sin(t * 0.25f) : 1;
        float jolt = since < CRACK_TICKS ? (since % 2 < 1 ? 1.5f : -1.5f) : 0;
        g.pose().pushPose();
        g.pose().translate(cx + jolt, cy + (state == 2 ? 3 : 0), 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(tilt));
        g.pose().scale(pulse, pulse, 1);
        blit(g, -SEAL / 2f, -SEAL / 2f, SEAL, SEAL, state * SEAL, SEAL_V, state == 2 ? 0.85f : 1);
        g.pose().popPose();
    }
}
