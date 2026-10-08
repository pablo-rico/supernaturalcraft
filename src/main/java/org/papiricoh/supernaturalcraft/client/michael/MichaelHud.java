package org.papiricoh.supernaturalcraft.client.michael;

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
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.papiricoh.supernaturalcraft.client.michael.MichaelGui.*;

/**
 * Michael's celestial boss bar, for his bar keys ({@code entity.supernaturalcraft.michael.bar.*}): a frame of gold
 * filigree, a fill of shimmering light, a wing at each end that opens as the bar appears (and flinches when he is
 * struck), steel feathers that fall off the bar with each blow, and his name, first in Enochian glyphs that resolve into
 * letters one by one. Textures from {@code textures/gui/michael/} (michael contract); a plain gold-and-light stand-in while
 * they are missing.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MichaelHud {

    public static final String BAR_PREFIX = "entity.supernaturalcraft.michael.bar";
    /** Frame and fill geometry (michael contract): the fill's inner rect inside the 256×32 frame. */
    static final int FRAME_W = 256, FRAME_H = 32, FILL_X = 24, FILL_Y = 12, FILL_W = 208, FILL_H = 8;
    static final int WING_W = 64, WING_H = 32, FRAMES = 8;
    /** Ticks for the wings to open, and per glyph as the name resolves. */
    private static final int OPEN_TICKS = 24, GLYPH_TICKS = 3;

    private static final class Feather {
        float x, y, vx, vy, spin, rot;
        int age;
        final int life;

        Feather(float x, float y, Random r) {
            this.x = x;
            this.y = y;
            vx = (r.nextFloat() - 0.5f) * 1.2f;
            vy = -0.6f - r.nextFloat() * 0.6f;
            spin = (r.nextFloat() - 0.5f) * 0.3f;
            rot = r.nextFloat() * Mth.TWO_PI;
            life = 30 + r.nextInt(25);
        }
    }

    private static final class Bar {
        String name = "";
        int age, flinch;
        float lastProgress = -1;
        final List<Feather> feathers = new ArrayList<>();
    }

    private static final Map<UUID, Bar> BARS = new HashMap<>();
    private static final Random RANDOM = new Random();

    private MichaelHud() {
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            BARS.clear();
            return;
        }
        for (Bar b : BARS.values()) {
            b.age++;
            if (b.flinch > 0) b.flinch--;
            for (Feather f : b.feathers) {
                f.age++;
                f.x += f.vx;
                f.y += f.vy;
                f.vy += 0.09f;
                f.vx *= 0.97f;
                f.rot += f.spin;
            }
            b.feathers.removeIf(f -> f.age >= f.life);
        }
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc) || !tc.getKey().startsWith(BAR_PREFIX)) return;
        event.setCanceled(true);
        // The frame's lower edge, plus room for the next bar's name band.
        event.setIncrement(FRAME_H - FILL_Y + 12);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        GuiGraphics g = event.getGuiGraphics();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Bar bar = BARS.computeIfAbsent(boss.getId(), id -> new Bar());
        String name = boss.getName().getString();
        if (!name.equals(bar.name)) {
            bar.name = name;
            bar.age = 0;
        }
        float progress = Mth.clamp(boss.getProgress(), 0, 1);
        // The light fill sits where the vanilla bar would; the frame's upper band holds the name.
        int cx = g.guiWidth() / 2, top = Math.max(0, event.getY() - FILL_Y + 2);
        int x0 = cx - FRAME_W / 2;
        if (bar.lastProgress >= 0 && progress < bar.lastProgress - 0.0005f) {
            // Struck: the wings flinch and a few feathers come loose at the edge of the light.
            bar.flinch = 8;
            int n = 1 + Math.min(4, (int) ((bar.lastProgress - progress) * 400));
            for (int i = 0; i < n; i++) bar.feathers.add(new Feather(x0 + FILL_X + FILL_W * progress, top + FILL_Y + 2, RANDOM));
        }
        bar.lastProgress = progress;
        double t = mc.level.getGameTime() + partial;
        drawBar(g, bar, x0, top, progress, t, partial);
        drawName(g, bar, cx, top + FILL_Y - 2, partial);
    }

    private static void drawBar(GuiGraphics g, Bar bar, int x0, int y, float progress, double t, float partial) {
        float open = Mth.clamp((bar.age + partial) / OPEN_TICKS, 0, 1);
        int wingFrame = Math.round(open * (FRAMES - 1)) - (bar.flinch > 0 ? 2 : 0);
        wingFrame = Mth.clamp(wingFrame, 0, FRAMES - 1);
        int fill = Math.round(FILL_W * progress);
        if (has(BAR_FRAME)) {
            // The wings either side: the right one is the left one mirrored.
            if (has(BAR_WING)) {
                blit(g, BAR_WING, x0 - WING_W + 30, y, WING_W, WING_H, 0, wingFrame * WING_H, WING_W, WING_H, WING_W, WING_H * FRAMES, 1, 0xFFFFFF);
                g.pose().pushPose();
                g.pose().translate(x0 + FRAME_W - 30 + WING_W, y, 0);
                g.pose().scale(-1, 1, 1);
                blit(g, BAR_WING, 0, 0, WING_W, WING_H, 0, wingFrame * WING_H, WING_W, WING_H, WING_W, WING_H * FRAMES, 1, 0xFFFFFF);
                g.pose().popPose();
            }
            g.fill(x0 + FILL_X, y + FILL_Y, x0 + FILL_X + FILL_W, y + FILL_Y + FILL_H, 0xC0101020);
            if (has(BAR_FILL) && fill > 0) {
                int frame = (int) (t / 3) % FRAMES;
                blit(g, BAR_FILL, x0 + FILL_X, y + FILL_Y, fill, FILL_H, 0, frame * FILL_H, fill, FILL_H, FILL_W, FILL_H * FRAMES, 1, 0xFFFFFF);
            } else {
                standInFill(g, x0 + FILL_X, y + FILL_Y, fill, t);
            }
            blit(g, BAR_FRAME, x0, y, FRAME_W, FRAME_H, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H, 1, 0xFFFFFF);
        } else {
            // Stand-in: a gold frame round a bar of light, little gold wings drawn as strokes.
            int fx = x0 + FILL_X, fy = y + FILL_Y;
            g.fill(fx - 2, fy - 2, fx + FILL_W + 2, fy + FILL_H + 2, argb(1, GOLD_DEEP));
            g.fill(fx - 1, fy - 1, fx + FILL_W + 1, fy + FILL_H + 1, argb(1, GOLD));
            g.fill(fx, fy, fx + FILL_W, fy + FILL_H, 0xFF101020);
            standInFill(g, fx, fy, fill, t);
            for (int side = -1; side <= 1; side += 2) {
                float bx = side < 0 ? fx - 3 : fx + FILL_W + 3;
                for (int k = 0; k < 4; k++) {
                    float len = (14 - k * 3) * (0.3f + 0.7f * wingFrame / 7f);
                    float ang = (-0.35f - k * 0.22f) * (bar.flinch > 0 ? 0.6f : 1f);
                    org.papiricoh.supernaturalcraft.client.chuck.fx.GuiDraw.line(g, bx, fy + 4, bx + side * len * Mth.cos(ang),
                            fy + 4 + len * Mth.sin(ang), 1.4f, argb(0.9f, GOLD));
                }
            }
        }
        // Feathers falling off where the light ends.
        for (Feather f : bar.feathers) {
            float a = 1 - (f.age + partial) / f.life;
            float fx = f.x + f.vx * partial, fy = f.y + f.vy * partial;
            g.pose().pushPose();
            g.pose().translate(fx, fy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(f.rot + f.spin * partial));
            if (has(FEATHER)) blit(g, FEATHER, -4, -4, 8, 8, 0, 0, 16, 16, 16, 16, a, 0xE8F2FF);
            else g.fill(-1, -3, 1, 3, argb(a, 0xE8F2FF));
            g.pose().popPose();
        }
    }

    private static void standInFill(GuiGraphics g, int x, int y, int fill, double t) {
        for (int i = 0; i < fill; i++) {
            float s = 0.5f + 0.5f * Mth.sin((float) (i * 0.12 - t * 0.25));
            int c = lerp(s, BLUE, 0xFFFFFF);
            g.fill(x + i, y, x + i + 1, y + FILL_H, argb(1, c));
        }
    }

    /** The name (its baseline at {@code y}): Enochian glyphs, resolving left to right into letters, gold. */
    private static void drawName(GuiGraphics g, Bar bar, int cx, int y, float partial) {
        Minecraft mc = Minecraft.getInstance();
        String name = bar.name;
        float age = bar.age + partial - OPEN_TICKS / 2f;
        int resolved = Mth.clamp((int) (age / GLYPH_TICKS), 0, name.length());
        Component plain = Component.literal(name);
        int w = mc.font.width(plain);
        int x = cx - w / 2;
        float a = Mth.clamp((bar.age + partial) / 10f, 0, 1);
        for (int i = 0; i < name.length(); i++) {
            String ch = name.substring(i, i + 1);
            int cw = mc.font.width(ch);
            if (i < resolved) {
                g.drawString(mc.font, ch, x, y - 9, argb(a, GOLD), true);
            } else if (!ch.isBlank()) {
                char up = Character.toUpperCase(ch.charAt(0));
                if (has(ENOCHIAN) && up >= 'A' && up <= 'Z') {
                    blit(g, ENOCHIAN, x, y - 10, Math.max(4, cw - 1), 9, (up - 'A') * 8, 0, 8, 16, 208, 16, a, BLUE);
                } else {
                    g.drawString(mc.font, enochian(ch), x, y - 9, argb(a, BLUE), true);
                }
            }
            x += cw;
        }
    }

    static int lerp(float k, int a, int b) {
        int r = (int) Mth.lerp(k, (a >> 16) & 255, (b >> 16) & 255), gg = (int) Mth.lerp(k, (a >> 8) & 255, (b >> 8) & 255),
                bb = (int) Mth.lerp(k, a & 255, b & 255);
        return (r << 16) | (gg << 8) | bb;
    }
}
