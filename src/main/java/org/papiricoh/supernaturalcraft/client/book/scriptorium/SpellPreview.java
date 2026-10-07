package org.papiricoh.supernaturalcraft.client.book.scriptorium;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.client.book.BookAtlas;
import org.papiricoh.supernaturalcraft.client.book.HunterBookScreen;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;

/**
 * The composing table's scrying pane: a two-second loop of what the draft would do, drawn in 2D
 * with the {@link BookAtlas#SPELL_FX} sprites tinted by the effects' colours. The form decides the
 * shape (a touch, a thrown bolt, a burst, a ward); the modifiers bend it (empower: bigger and
 * brighter, extend: longer trail and linger, widen: larger radius, echo: a ghost repeat).
 */
final class SpellPreview {

    enum Shape {NONE, TOUCH, BOLT, BURST, WARD}

    /** What to draw, read once from the draft. */
    record Look(Shape shape, int[] colors, int empower, int extend, int widen, int echo) {

        static Look of(Draft draft) {
            SigilComponent form = Sigils.get(draft.form);
            Shape shape = Shape.NONE;
            int formColor = 0xE8D9A8;
            if (form != null) {
                formColor = form.color();
                shape = switch (form.behavior().getPath()) {
                    case "touch" -> Shape.TOUCH;
                    case "burst" -> Shape.BURST;
                    case "ward" -> Shape.WARD;
                    default -> Shape.BOLT;
                };
            }
            int[] colors = draft.effects.stream().map(Sigils::get).filter(java.util.Objects::nonNull)
                    .mapToInt(SigilComponent::color).toArray();
            if (colors.length == 0) colors = new int[]{formColor};
            int empower = 0, extend = 0, widen = 0, echo = 0;
            for (ResourceLocation id : draft.modifiers) {
                SigilComponent m = Sigils.get(id);
                if (m == null) continue;
                if (m.param("potency_multiplier", 1f) > 1f) empower++;
                if (m.param("duration_multiplier", 1f) > 1f || m.param("range_multiplier", 1f) > 1f) extend++;
                if (m.param("area_bonus", 0f) > 0f) widen++;
                if (m.param("echo", 0f) > 0f) echo++;
            }
            return new Look(shape, colors, empower, extend, widen, echo);
        }

        int color(int i) {
            return colors[Math.floorMod(i, colors.length)];
        }

        float power() {
            return 1 + 0.3f * empower;
        }

        float reach() {
            return 1 + 0.4f * widen;
        }
    }

    /** One loop, in ticks. */
    static final float PERIOD = 40f;
    private static final int SKY_TOP = 0xFF241B13, SKY_BOTTOM = 0xFF120D08, EARTH = 0xFF0C0805, HORIZON = 0xFF46362A;
    private static final int HUNTER = 0xFF9A8060, DEMON = 0xFF6A3426;

    private SpellPreview() {
    }

    /**
     * Draws the pane in book space at x,y (w×h).
     *
     * @param time    ticks since the book opened, partial ticks included
     * @param caption the spell's name, shown in the corner (null: none)
     * @param idle    what to say when there is no form yet
     */
    static void render(GuiGraphics g, HunterBookScreen book, Font font, int x, int y, int w, int h, Look look, float time,
                       @Nullable Component caption, Component idle) {
        book.scissor(g, x, y, x + w, y + h);
        int gy = y + h - 11;
        g.fillGradient(x, y, x + w, gy, SKY_TOP, SKY_BOTTOM);
        g.fill(x, gy, x + w, y + h, EARTH);
        g.fill(x, gy, x + w, gy + 1, HORIZON);
        for (int i = 0; i < 18; i++) {
            int sx = x + 4 + (int) ((i * 97L + 13) % (w - 8)), sy = y + 3 + (int) ((i * 53L + 7) % Math.max(1, h - 24));
            float twinkle = 0.5f + 0.5f * Mth.sin(time * 0.15f + i * 1.7f);
            g.fill(sx, sy, sx + 1, sy + 1, ((int) (40 + 60 * twinkle) << 24) | 0xE8D9A8);
        }

        Shape shape = look.shape();
        boolean centred = shape == Shape.BURST || shape == Shape.WARD;
        int cx = centred ? x + w / 2 : x + 24;
        int tx = shape == Shape.TOUCH ? cx + 30 : x + w - 24;
        hunter(g, cx, gy, centred);
        if (shape == Shape.TOUCH || shape == Shape.BOLT) demon(g, tx, gy);

        if (shape == Shape.NONE) {
            g.drawString(font, idle, x + (w - font.width(idle)) / 2, y + (h - 20) / 2, 0xFFB8A27C, false);
        } else {
            float loops = time / PERIOD;
            int cycle = Mth.floor(loops);
            float t = loops - cycle;
            float dur = Math.min(0.92f, 0.6f + 0.12f * look.extend());
            if (look.echo() > 0) dur = Math.min(dur, 0.66f);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            Stage s = new Stage(g, look, cx, tx, gy, time);
            if (look.echo() > 0) s.cast((t - 0.3f) / dur, 0.45f, cycle + 1);
            s.cast(t / dur, 1f, cycle);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
        if (caption != null) g.drawString(font, caption, x + 4, y + 3, 0xD0E8D9A8, false);
        g.disableScissor();
    }

    /** A little hunter in a wide-brimmed hat, facing right (or with both arms up to cast around him). */
    private static void hunter(GuiGraphics g, int cx, int gy, boolean armsUp) {
        g.fill(cx - 4, gy - 21, cx + 4, gy - 20, HUNTER);
        g.fill(cx - 2, gy - 24, cx + 2, gy - 21, HUNTER);
        g.fill(cx - 2, gy - 20, cx + 2, gy - 16, HUNTER);
        g.fill(cx - 3, gy - 16, cx + 3, gy - 6, HUNTER);
        g.fill(cx - 4, gy - 9, cx + 4, gy - 5, HUNTER);
        g.fill(cx - 3, gy - 5, cx - 1, gy, HUNTER);
        g.fill(cx + 1, gy - 5, cx + 3, gy, HUNTER);
        if (armsUp) {
            g.fill(cx - 5, gy - 20, cx - 3, gy - 14, HUNTER);
            g.fill(cx + 3, gy - 20, cx + 5, gy - 14, HUNTER);
        } else {
            g.fill(cx + 3, gy - 15, cx + 8, gy - 13, HUNTER);
        }
    }

    /** A horned demon, facing left, with black eyes. */
    private static void demon(GuiGraphics g, int tx, int gy) {
        g.fill(tx - 4, gy - 23, tx - 3, gy - 20, DEMON);
        g.fill(tx + 3, gy - 23, tx + 4, gy - 20, DEMON);
        g.fill(tx - 3, gy - 20, tx + 3, gy - 15, DEMON);
        g.fill(tx - 2, gy - 19, tx - 1, gy - 18, 0xFF000000);
        g.fill(tx + 1, gy - 19, tx + 2, gy - 18, 0xFF000000);
        g.fill(tx - 4, gy - 15, tx + 4, gy - 5, DEMON);
        g.fill(tx - 7, gy - 14, tx - 4, gy - 12, DEMON);
        g.fill(tx - 3, gy - 5, tx - 1, gy, DEMON);
        g.fill(tx + 1, gy - 5, tx + 3, gy, DEMON);
    }

    /** One frame of a cast, drawn additively. */
    private record Stage(GuiGraphics g, Look look, int cx, int tx, int gy, float time) {

        /** @param u how far through the cast (0..1; outside: nothing) */
        void cast(float u, float alpha, int shift) {
            if (u < 0 || u > 1) return;
            switch (look.shape()) {
                case TOUCH -> touch(u, alpha, shift);
                case BOLT -> bolt(u, alpha, shift);
                case BURST -> burst(u, alpha, shift);
                case WARD -> ward(u, alpha, shift);
                default -> {
                }
            }
        }

        private void touch(float u, float a, int shift) {
            float pw = look.power();
            float hx = cx + 9, hy = gy - 14;
            float px = tx - 5, py = gy - 13;
            int c = look.color(shift);
            if (u < 0.25f) {
                float k = u / 0.25f;
                fx(BookAtlas.FX_GLOW, hx, hy, (5 + 7 * k) * pw, c, a * k);
                for (int i = -1; i <= 1; i++) {
                    float ang = i * 0.5f;
                    fx(BookAtlas.FX_SPARK, hx + Mth.cos(ang) * 4 * k, hy + Mth.sin(ang) * 4 * k, 4, look.color(shift + i + 1), a * k);
                }
                return;
            }
            float q = (u - 0.25f) / 0.75f, out = easeOut(q);
            float fade = 1 - q;
            fx(BookAtlas.FX_GLOW, hx, hy, 12 * pw * (1 - 0.4f * q), c, a * fade);
            fx(BookAtlas.FX_GLOW, (hx + px) / 2, (hy + py) / 2, 10 * pw, c, a * Math.max(0, 1 - q * 3));
            fx(BookAtlas.FX_RING, px, py, Mth.lerp(out, 4, 22 * look.reach() * pw), c, a * fade);
            if (look.widen() > 0) fx(BookAtlas.FX_RING, px, py, Mth.lerp(easeOut(Math.max(0, q - 0.15f)), 4, 30 * look.reach()), look.color(shift + 1), a * fade * 0.7f);
            fx(BookAtlas.FX_GLOW, px, py, 18 * pw, c, a * Math.max(0, 1 - q * 2));
            int n = 6 + 3 * look.empower();
            for (int i = 0; i < n; i++) {
                float ang = -0.9f + 1.8f * i / (n - 1) + 0.15f * Mth.sin(i * 12.9f);
                float d = out * (10 + 6 * look.reach()) * (0.7f + 0.3f * hash(i));
                fx(BookAtlas.FX_SPARK, px + Mth.cos(ang) * d, py + Mth.sin(ang) * d, 4 * pw, look.color(shift + i), a * fade);
            }
            if (look.extend() > 0) fx(BookAtlas.FX_GLOW, px, py, 10, c, a * 0.4f * fade);
        }

        private void bolt(float u, float a, int shift) {
            float pw = look.power();
            float hx = cx + 9, hy = gy - 14;
            float px = tx - 3, py = gy - 13;
            int c = look.color(shift);
            if (u < 0.15f) {
                float k = u / 0.15f;
                fx(BookAtlas.FX_GLOW, hx, hy, (6 + 8 * k) * pw, c, a * k);
                fx(BookAtlas.FX_SPARK, hx, hy, 5, 0xFFFFFF, a * k * 0.8f);
                return;
            }
            if (u < 0.6f) {
                float q = (u - 0.15f) / 0.45f, p = q * q * (3 - 2 * q) * 0.35f + q * 0.65f;
                float bx = Mth.lerp(p, hx, px), by = Mth.lerp(p, hy, py) - Mth.sin(p * Mth.PI) * 6;
                fx(BookAtlas.FX_GLOW, hx, hy, 10 * pw, c, a * (1 - q) * 0.6f);
                float trail = 22 * (1 + 0.6f * look.extend());
                float len = Math.min(trail, bx - hx + 4);
                fxStretched(BookAtlas.FX_STREAK, bx - len / 2, by, len, 10 * pw, c, a);
                fxStretched(BookAtlas.FX_STREAK, bx - len * 0.35f, by, len * 0.7f, 5 * pw, 0xFFFFFF, a * 0.5f);
                int n = 5 + 3 * look.extend();
                for (int i = 1; i <= n; i++) {
                    float back = i * trail / n;
                    float pp = Math.max(0, p - back / (px - hx));
                    float sx = Mth.lerp(pp, hx, px), sy = Mth.lerp(pp, hy, py) - Mth.sin(pp * Mth.PI) * 6 + 2.5f * Mth.sin(time * 0.9f + i * 2.1f);
                    fx(BookAtlas.FX_SPARK, sx, sy, 3 + 2 * hash(i), look.color(shift + i), a * (1 - (float) i / (n + 1)) * 0.8f);
                }
                fx(BookAtlas.FX_GLOW, bx, by, 24 * pw, c, a * 0.7f);
                fx(BookAtlas.FX_GLOW, bx, by, 12 * pw, c, a);
                fx(BookAtlas.FX_SPARK, bx, by, 6 * pw, 0xFFFFFF, a * 0.9f);
                return;
            }
            float q = (u - 0.6f) / 0.4f, out = easeOut(q), fade = 1 - q;
            float radius = 26 * look.reach() * pw;
            fx(BookAtlas.FX_GLOW, px, py, 22 * pw, c, a * Math.max(0, 1 - q * 2.2f));
            fx(BookAtlas.FX_RING, px, py, Mth.lerp(out, 6, radius), c, a * fade);
            if (look.widen() > 0 || look.empower() > 0) {
                fx(BookAtlas.FX_RING, px, py, Mth.lerp(easeOut(Math.max(0, q - 0.12f)), 4, radius * 0.75f), look.color(shift + 1), a * fade * 0.7f);
            }
            int n = 8 + 4 * look.empower();
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + 0.4f * hash(i + 7);
                float d = out * radius * 0.55f * (0.7f + 0.3f * hash(i));
                fx(BookAtlas.FX_SPARK, px + Mth.cos(ang) * d, py + Mth.sin(ang) * d * 0.8f, 4 * pw, look.color(shift + i), a * fade);
            }
            if (look.extend() > 0) fx(BookAtlas.FX_GLOW, px, py, 12, c, a * 0.35f * fade);
        }

        private void burst(float u, float a, int shift) {
            float pw = look.power();
            float ox = cx, oy = gy - 13;
            int c = look.color(shift);
            if (u < 0.2f) {
                float k = u / 0.2f;
                for (int i = 0; i < 8; i++) {
                    float ang = Mth.TWO_PI * i / 8 + time * 0.05f, d = 20 * (1 - k);
                    fx(BookAtlas.FX_SPARK, ox + Mth.cos(ang) * d, oy + Mth.sin(ang) * d, 4, look.color(shift + i), a * k);
                }
                fx(BookAtlas.FX_GLOW, ox, oy, (6 + 10 * k) * pw, c, a * k);
                return;
            }
            float q = (u - 0.2f) / 0.8f, out = easeOut(q), fade = 1 - q;
            float radius = 46 * look.reach() * pw;
            fx(BookAtlas.FX_GLOW, ox, oy, 26 * pw, c, a * Math.max(0, 1 - q * 2));
            fx(BookAtlas.FX_RING, ox, oy, Mth.lerp(out, 8, radius * 2), c, a * fade);
            float q2 = Math.max(0, q - 0.15f);
            fx(BookAtlas.FX_RING, ox, oy, Mth.lerp(easeOut(q2), 6, radius * 1.5f), look.color(shift + 1), a * fade * 0.7f);
            int n = 12 + 4 * look.empower();
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + 0.3f * hash(i + 3);
                float d = out * radius * (0.75f + 0.25f * hash(i));
                fx(BookAtlas.FX_SPARK, ox + Mth.cos(ang) * d, oy + Mth.sin(ang) * d, 4 * pw, look.color(shift + i), a * fade);
            }
        }

        private void ward(float u, float a, int shift) {
            float pw = look.power();
            float radius = 24 * look.reach();
            int n = 13;
            float rise = Math.min(1, u / 0.3f);
            float fade = u > 0.8f ? (1 - u) / 0.2f : 1;
            float pulse = 0.75f + 0.25f * Mth.sin(time * 0.4f);
            fxStretched(BookAtlas.FX_RING, cx, gy, radius * 2.2f, radius * 0.6f, look.color(shift), a * rise * fade);
            fx(BookAtlas.FX_GLOW, cx, gy - radius * 0.5f, radius * 2, look.color(shift), a * 0.15f * rise * fade * pw);
            for (int j = 0; j < n; j++) {
                float fromEnd = Math.min(j, n - 1 - j) / ((n - 1) / 2f);
                if (fromEnd > rise) continue;
                float ang = Mth.PI * j / (n - 1);
                float px = cx - Mth.cos(ang) * radius, py = gy - Mth.sin(ang) * radius;
                int c = look.color(shift + j);
                fx(j % 2 == 0 ? BookAtlas.FX_RING : BookAtlas.FX_GLOW, px, py, (j % 2 == 0 ? 9 : 7) * pw, c, a * pulse * fade);
            }
            if (u > 0.3f) {
                int sparks = 5 + 2 * look.empower();
                for (int i = 0; i < sparks; i++) {
                    float k = ((time * 0.03f + hash(i)) % 1f);
                    float sx = cx + (hash(i + 11) - 0.5f) * radius * 1.4f, sy = gy - k * radius * 0.9f;
                    fx(BookAtlas.FX_SPARK, sx, sy, 3, look.color(shift + i), a * (1 - k) * fade);
                }
            }
        }

        private void fx(BookAtlas.Sprite s, float x, float y, float size, int rgb, float alpha) {
            fxStretched(s, x, y, size, size, rgb, alpha);
        }

        /** A sprite centred on x,y, stretched to w×h and tinted. */
        private void fxStretched(BookAtlas.Sprite s, float x, float y, float w, float h, int rgb, float alpha) {
            if (alpha <= 0.01f || w <= 0.2f || h <= 0.2f) return;
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(w / s.w(), h / s.h(), 1);
            RenderSystem.setShaderColor(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f, Math.min(1, alpha));
            s.draw(g, -s.w() / 2, -s.h() / 2);
            pose.popPose();
        }
    }

    private static float easeOut(float q) {
        return 1 - (1 - q) * (1 - q);
    }

    /** A fixed pseudo-random number in [0,1) for a particle index. */
    private static float hash(int i) {
        float v = Mth.sin(i * 12.9898f + 4.1f) * 43758.547f;
        return v - Mth.floor(v);
    }
}
