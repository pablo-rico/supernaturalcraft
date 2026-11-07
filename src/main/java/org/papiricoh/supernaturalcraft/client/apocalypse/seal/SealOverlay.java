package org.papiricoh.supernaturalcraft.client.apocalypse.seal;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealKind;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealRules;
import org.papiricoh.supernaturalcraft.client.cinematic.CameraDirector;
import org.papiricoh.supernaturalcraft.network.SealSitePayload;

/**
 * The seals on the screen (v0.40): their title cards (a seal trembles, breaks, holds; Uriel's phases and fall), drawn through
 * camera sequences; and the mini HUD of the rite while the hunter is near the site they were called to (hidden during camera
 * sequences): the kind, a bar of the demons' rite (red, filling to the seal's break), the time left and what still stands.
 * Cards: {@code title.supernaturalcraft.seal.<warning|broken|held>} and {@code title.supernaturalcraft.uriel.<phase1|phase2|fall|castiel>},
 * each with a subtitle.
 */
public final class SealOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation LAYER = SupernaturalCraft.asResource("seals");
    /** The mini HUD shows within this many blocks of the site. */
    public static final double HUD_RANGE = 48;
    static final int RED = 0xC0281C, GOLD = 0xFFD27A, BONE = 0xE8DCC4, DARK = 0x140808;

    private static @Nullable Card card;

    /** A card: a title line and a subtitle under it, in the colour {@code rgb}. */
    public static void card(Component title, @Nullable Component sub, int rgb, int duration) {
        card = new Card(title, sub, rgb, Mth.clamp(duration, 60, 200));
    }

    static void tick() {
        if (Minecraft.getInstance().level == null) {
            card = null;
            return;
        }
        if (card != null && ++card.age >= card.life) card = null;
    }

    static void clear() {
        card = null;
    }

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        if (!CameraDirector.active() && !mc.options.hideGui) hud(g, mc);
        Card c = card;
        if (c != null) c.render(g, mc.font, partial, g.guiWidth(), g.guiHeight());
    }

    /** The rite's mini HUD, top centre, under the boss bars' row. */
    private static void hud(GuiGraphics g, Minecraft mc) {
        SealSitePayload s = ClientSeals.site();
        if (!s.active() || mc.level == null || mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        if (ClientSeals.distance(mc.player.position()) > HUD_RANGE) return;
        Font font = mc.font;
        int w = 182, x = (g.guiWidth() - w) / 2, y = 34;
        SealKind kind = SealKind.byOrdinal(s.kind());
        Component name = Component.translatable(kind.nameKey());
        g.drawCenteredString(font, name, g.guiWidth() / 2, y, BONE | 0xFF000000);
        int by = y + 11;
        g.fill(x - 1, by - 1, x + w + 1, by + 6, 0xC0000000 | DARK);
        int filled = Math.round(w * Mth.clamp(s.progress() / SealRules.RITE_MAX, 0, 1));
        g.fill(x, by, x + filled, by + 5, 0xFF000000 | RED);
        // A tick every quarter of the rite.
        for (int i = 1; i < 4; i++) g.fill(x + w * i / 4, by, x + w * i / 4 + 1, by + 5, 0x80FFFFFF);
        int secs = s.ticksLeft() / 20;
        Component left = Component.translatable("hud.supernaturalcraft.seal.rite", s.progress(), secs / 60, String.format("%02d", secs % 60));
        g.drawCenteredString(font, left, g.guiWidth() / 2, by + 8, 0xFFB8A890);
        Component stand = s.wardsAtStart() > 0
                ? Component.translatable("hud.supernaturalcraft.seal.wards", s.drivers(), s.wards(), s.wardsAtStart())
                : Component.translatable("hud.supernaturalcraft.seal.drivers", s.drivers());
        g.drawCenteredString(font, stand, g.guiWidth() / 2, by + 18, 0xFFE0B080);
    }

    static final class Card {
        final Component title;
        final @Nullable Component sub;
        final int rgb, life;
        int age;

        Card(Component title, @Nullable Component sub, int rgb, int life) {
            this.title = title;
            this.sub = sub;
            this.rgb = rgb;
            this.life = life;
        }

        void render(GuiGraphics g, Font font, float partial, int w, int h) {
            float t = age + partial;
            float a = Mth.clamp(Math.min(t / 10f, (life - t) / 20f), 0, 1);
            if (a <= 0.02f) return;
            int alpha = Math.max(4, Math.round(a * 255)) << 24;
            float cy = Math.max(h * 0.3f, 70);
            float scale = Math.min(3f, (w - 40) / (float) Math.max(1, font.width(title)));
            var pose = g.pose();
            pose.pushPose();
            pose.translate(w / 2f, cy, 0);
            pose.scale(scale, scale, 1);
            g.drawCenteredString(font, title, 0, -4, alpha | rgb);
            pose.popPose();
            if (sub != null) {
                float ss = Math.min(1.2f, (w - 60) / (float) Math.max(1, font.width(sub)));
                pose.pushPose();
                pose.translate(w / 2f, cy + 8 * scale + 2, 0);
                pose.scale(ss, ss, 1);
                g.drawCenteredString(font, sub, 0, 0, alpha | BONE);
                pose.popPose();
            }
        }
    }
}
