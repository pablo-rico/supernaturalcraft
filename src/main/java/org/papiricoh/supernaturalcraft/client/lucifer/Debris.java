package org.papiricoh.supernaturalcraft.client.lucifer;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.papiricoh.supernaturalcraft.client.lucifer.LuciferGui.*;

/**
 * What flies off the Cage on the screen, in GUI pixels: embers rising, chain links and rivets, shards of ice, the torn half
 * of a bar, dust where one lands. Ticked with the HUD and the title cards; drawn from {@code debris.png} and
 * {@code bar_bars.png}.
 */
final class Debris {

    /** Kinds: the first four are the cells of {@code debris.png}. */
    static final int LINK = 0, RIVET = 1, EMBER = 2, ICE = 3, PIECE = 4, DUST = 5;

    static final class Bit {
        final int kind, life, rgb;
        float x, y, vx, vy, rot, spin, size, gravity;
        int age;

        Bit(int kind, float x, float y, float vx, float vy, float size, int life, int rgb, Random r) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.life = Math.max(1, life);
            this.rgb = rgb;
            rot = r.nextFloat() * 6.28f;
            spin = kind == EMBER || kind == DUST ? 0 : (r.nextFloat() - 0.5f) * 0.5f;
            gravity = switch (kind) {
                case EMBER -> -0.035f;
                case DUST -> 0.01f;
                default -> 0.22f;
            };
        }
    }

    final List<Bit> bits = new ArrayList<>();
    final Random random = new Random();

    Bit add(int kind, float x, float y, float vx, float vy, float size, int life, int rgb) {
        Bit b = new Bit(kind, x, y, vx, vy, size, life, rgb, random);
        // Never let one burst flood the screen.
        if (bits.size() < 400) bits.add(b);
        return b;
    }

    /** {@code n} embers from around (x, y), rising. */
    void embers(int n, float x, float y, float spread, int rgb) {
        for (int i = 0; i < n; i++) {
            add(EMBER, x + (random.nextFloat() - 0.5f) * spread, y + (random.nextFloat() - 0.5f) * spread * 0.4f,
                    (random.nextFloat() - 0.5f) * 0.8f, -0.4f - random.nextFloat() * 0.9f, 0.35f + random.nextFloat() * 0.45f,
                    18 + random.nextInt(22), rgb);
        }
    }

    void tick() {
        for (Bit b : bits) {
            b.age++;
            b.x += b.vx;
            b.y += b.vy;
            b.vy += b.gravity;
            b.vx *= b.kind == EMBER ? 0.94f : 0.97f;
            if (b.kind == EMBER) b.vx += (random.nextFloat() - 0.5f) * 0.12f;
            b.rot += b.spin;
        }
        bits.removeIf(b -> b.age >= b.life);
    }

    void clear() {
        bits.clear();
    }

    void render(GuiGraphics g, float partial, float alpha) {
        for (Bit b : bits) {
            float k = (b.age + partial) / b.life;
            float a = alpha * (b.kind == EMBER ? (1 - k) : Math.min(1, (1 - k) * 2.5f));
            if (a <= 0.01f) continue;
            float x = b.x + b.vx * partial, y = b.y + b.vy * partial;
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().mulPose(Axis.ZP.rotation(b.rot + b.spin * partial));
            g.pose().scale(b.size, b.size, 1);
            switch (b.kind) {
                case EMBER -> glow(g, DEBRIS, -4, -4, 8, 8, 16, 0, 8, 8, 32, 8, a, b.rgb);
                case PIECE -> blit(g, BAR_BARS, -6, -10, BAR_W, 19, CageBars.State.SNAPPED.ordinal() * BAR_W, 0, BAR_W, 19,
                        BAR_W * 8, BAR_H, a, b.rgb);
                case DUST -> g.fill(-1, -1, 1, 1, argb(a * 0.55f, b.rgb));
                default -> blit(g, DEBRIS, -4, -4, 8, 8, b.kind * 8, 0, 8, 8, 32, 8, a, b.rgb);
            }
            g.pose().popPose();
        }
    }
}
