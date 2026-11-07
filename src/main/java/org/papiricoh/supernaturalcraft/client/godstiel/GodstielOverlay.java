package org.papiricoh.supernaturalcraft.client.godstiel;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw;
import org.papiricoh.supernaturalcraft.client.lucifer.Gothic;
import org.papiricoh.supernaturalcraft.client.michael.MichaelGui;
import org.papiricoh.supernaturalcraft.entity.boss.godstiel.GodstielBalance;

import java.util.Random;

/**
 * Godstiel's title cards and voices (v0.39), over his camera shots (the layer is let through camera sequences; it draws no game
 * HUD). A card: his halo seal ({@code godstiel_bar.png}) opening in light, the title in gothic white under it, the subtitle below;
 * from phase III the light is stained black and violet. The voices: while he struggles, the Leviathans' words flicker on the
 * screen in violet, Castiel's answer in white under them. A flash for the radiance.
 * Keys: {@code title.supernaturalcraft.godstiel.phase<n>}, {@code .kneel}, {@code .forgiven}, {@code .condemned} (each with an
 * optional {@code .sub}); {@code voice.supernaturalcraft.godstiel.<n>} and {@code .<n>.cas}.
 */
public final class GodstielOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("godstiel");
    static final int LIGHT = 0xFFF6DC, SOUL = 0xBFE9FF, VIOLET = 0xB36BFF, DARK = 0x150A1E;

    private static @Nullable Card card;
    private static @Nullable Voices voices;
    private static float flash;
    private static int flashRgb = 0xFFFFFF;

    /** A card: {@code phase} 1-5, {@code which} 0 opening / 1 kneels / 2 forgiven / 3 condemned. */
    public static void title(int phase, int which, int duration) {
        card = new Card(phase, which, duration);
    }

    static void voices(int line, int duration) {
        voices = new Voices(line, Math.max(40, duration));
    }

    static void flash(float strength, int rgb) {
        flash = Math.max(flash, strength);
        flashRgb = rgb;
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            clear();
            return;
        }
        if (card != null && ++card.age >= card.life) card = null;
        if (voices != null && ++voices.age >= voices.life) voices = null;
        flash = Math.max(0, flash - 0.08f);
    }

    static void clear() {
        card = null;
        voices = null;
        flash = 0;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        if (Minecraft.getInstance().player == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        if (flash > 0.01f) g.fill(0, 0, g.guiWidth(), g.guiHeight(), MichaelGui.argb(flash * 0.7f, flashRgb));
        Voices v = voices;
        if (v != null) v.render(g, partial, g.guiWidth(), g.guiHeight());
        Card c = card;
        if (c != null) c.render(g, partial, g.guiWidth(), g.guiHeight());
    }

    static final class Card {
        final int phase, which, life;
        final String title;
        final @Nullable Component sub;
        int age;

        Card(int phase, int which, int duration) {
            this.phase = Mth.clamp(phase, 1, GodstielBalance.PHASES);
            this.which = which;
            this.life = Mth.clamp(duration, 60, 160);
            String key = switch (which) {
                case 1 -> "title.supernaturalcraft.godstiel.kneel";
                case 2 -> "title.supernaturalcraft.godstiel.forgiven";
                case 3 -> "title.supernaturalcraft.godstiel.condemned";
                default -> "title.supernaturalcraft.godstiel.phase" + this.phase;
            };
            title = I18n.get(key);
            sub = I18n.exists(key + ".sub") ? Component.translatable(key + ".sub") : null;
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            float a = Mth.clamp(Math.min(t / 8f, (life - t) / 20f), 0, 1);
            if (a <= 0.01f) return;
            boolean dark = which == 3 || which == 1 || which == 0 && phase >= GodstielBalance.LEVIATHAN_PHASE;
            float cx = w / 2f, cy = Math.max(h * 0.28f, 80);
            // The seal: a halo pierced by a black drop, opening in light.
            float grow = 1 - (1 - Mth.clamp(t / 14f, 0, 1)) * (1 - Mth.clamp(t / 14f, 0, 1));
            float s = 1.6f * grow;
            if (MichaelGui.has(GodstielHud.SHEET)) {
                g.pose().pushPose();
                g.pose().translate(cx, cy - 26, 0);
                g.pose().scale(s, s, 1);
                MichaelGui.glow(g, GodstielHud.SHEET, -GodstielHud.SEAL / 2f, -GodstielHud.SEAL / 2f, GodstielHud.SEAL, GodstielHud.SEAL,
                        GodstielHud.SEAL_U, GodstielHud.SEAL_V, GodstielHud.SEAL, GodstielHud.SEAL, GodstielHud.TW, GodstielHud.TH, a * 0.6f, dark ? VIOLET : SOUL);
                MichaelGui.blit(g, GodstielHud.SHEET, -GodstielHud.SEAL / 2f, -GodstielHud.SEAL / 2f, GodstielHud.SEAL, GodstielHud.SEAL,
                        GodstielHud.SEAL_U, GodstielHud.SEAL_V, GodstielHud.SEAL, GodstielHud.SEAL, GodstielHud.TW, GodstielHud.TH, a, 0xFFFFFF);
                g.pose().popPose();
            }
            float ta = a * Mth.clamp((t - 8) / 8f, 0, 1);
            float textScale = Math.min(2.2f, (w - 40) / (float) Math.max(1, Gothic.width(title, false)));
            Gothic.centred(g, title, cx, cy, textScale, ta, dark ? VIOLET : LIGHT, false);
            if (sub != null) {
                Component italic = sub.copy().withStyle(st -> st.withItalic(true));
                float ss = Math.min(1f, (w - 60) / (float) Math.max(1, GuiDraw.width(italic, 1f)));
                GuiDraw.centred(g, italic, cx, cy + 12 * textScale + 4, ss, MichaelGui.argb(ta, dark ? 0xD9C6F5 : SOUL), true);
            }
        }
    }

    /** The Leviathans' words flickering in violet, Castiel's answer in white under them. */
    static final class Voices {
        final int life;
        final Component them;
        final @Nullable Component cas;
        final long seed = new Random().nextLong();
        int age;

        Voices(int line, int life) {
            this.life = life;
            String key = "voice.supernaturalcraft.godstiel." + line;
            them = Component.translatable(key);
            cas = I18n.exists(key + ".cas") ? Component.translatable(key + ".cas") : null;
        }

        void render(GuiGraphics g, float partial, int w, int h) {
            float t = age + partial;
            float a = Mth.clamp(Math.min(t / 6f, (life - t) / 14f), 0, 1);
            if (a <= 0.01f) return;
            Random r = new Random(seed + (long) (t / 3));
            float jx = (r.nextFloat() - 0.5f) * 4, jy = (r.nextFloat() - 0.5f) * 3;
            float flicker = 0.7f + 0.3f * r.nextFloat();
            float cy = h * 0.62f;
            for (int k = 2; k >= 0; k--) {
                // Three overlapping copies, the voices out of step with one another.
                GuiDraw.centred(g, them, w / 2f + jx * (k - 1), cy + jy * k, 1.3f, MichaelGui.argb(a * flicker * (k == 0 ? 1 : 0.35f), k == 0 ? VIOLET : DARK), k == 0);
            }
            if (cas != null && t > 20) {
                GuiDraw.centred(g, cas.copy().withStyle(st -> st.withItalic(true)), w / 2f, cy + 16, 1f, MichaelGui.argb(a * Mth.clamp((t - 20) / 10f, 0, 1), LIGHT), true);
            }
        }
    }
}
