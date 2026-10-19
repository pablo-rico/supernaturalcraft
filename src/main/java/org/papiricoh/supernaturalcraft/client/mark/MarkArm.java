package org.papiricoh.supernaturalcraft.client.mark;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.client.allegiance.ClientAllegiance;
import org.papiricoh.supernaturalcraft.client.chuck.render.WorldDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicRenderTypes;

/**
 * The Mark of Cain on a bearer's right forearm (v0.21), drawn by code on the arm's front face in the model's own space (so it
 * follows every pose, PlayerAnimationLib's included): raw reddened skin, the dark scar of the rune, and its light, which beats
 * like a heart -- slowly at rest, hard and fast in bloodlust, ragged at full Thirst. Shared by the player layer (third person,
 * {@link MarkLayer}) and the first-person arm ({@link MarkClientEvents}). Sits just above the sleeve layer, so armour covers it.
 */
public final class MarkArm {

    /** Forearm box on the right arm (pixels, the arm part's own space): the rune's centre x for wide and slim arms, its top and foot. */
    private static final float WIDE_X = -1f, SLIM_X = -0.5f, TOP = 4.4f, FOOT = 9.6f, FACE = -2.32f;
    private static final int SCAR = 0x3A0606, RAW = 0x7A1A12, RED = 0xD0141E, HOT = 0xFFB070;

    private MarkArm() {
    }

    /** Whether {@code player} bears the Mark, as this client knows it. */
    public static boolean marked(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == player && ClientMark.marked()) return true;
        return ClientAllegiance.flag(player, Allegiances.MARKED);
    }

    /** The Thirst to show for {@code player}: the local one's own, another's only as bloodlust or not. */
    public static float thirst(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == player && ClientMark.marked()) return ClientMark.thirst();
        return ClientAllegiance.flag(player, Allegiances.MARK_BLOODLUST) ? 0.75f : 0.2f;
    }

    /** How brightly the Mark burns now (0-1) at this Thirst. */
    public static float glow(float thirst, float time, int seed) {
        float period = thirst >= 0.5f ? 24 - 10 * (thirst - 0.5f) * 2 : 40;
        float ph = ((time + seed * 7) % period) / period;
        float thump = Math.max(bump(ph, 0.05f), 0.6f * bump(ph, 0.24f));
        float rest = 0.25f + 0.3f * thirst;
        float k = rest + (1 - rest) * thump * (0.45f + 0.55f * thirst);
        if (thirst >= 0.99f) k *= 0.8f + 0.2f * Mth.sin(time * 2.3f);
        return Mth.clamp(k, 0, 1);
    }

    private static float bump(float x, float at) {
        float d = (x - at) / 0.07f;
        return (float) Math.exp(-d * d);
    }

    /** Draws the Mark on {@code model}'s right arm (already posed). */
    public static void draw(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> model, boolean slim, float thirst,
                            float time, int seed) {
        pose.pushPose();
        model.rightArm.translateAndRotate(pose);
        pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
        Matrix4f m = pose.last().pose();
        float cx = slim ? SLIM_X : WIDE_X, h = FOOT - TOP, w = h * MarkGlyph.ASPECT;
        float x0 = cx - w / 2;
        float g = glow(thirst, time, seed);
        // The raw skin round it and the dark scar (painted).
        VertexConsumer paint = WorldDraw.quads(buffers);
        rect(paint, m, cx - w / 2 - 0.45f, TOP - 0.45f, cx + w / 2 + 0.45f, FOOT + 0.45f, FACE + 0.01f, argb(0.35f + 0.2f * thirst, RAW));
        MarkGlyph.walk(1, (ax, ay, bx, by) -> stroke(paint, m, x0 + ax * w, TOP + ay * h, x0 + bx * w, TOP + by * h, 0.85f, FACE,
                argb(0.92f, SCAR)));
        // Its light (added).
        VertexConsumer light = buffers.getBuffer(MagicRenderTypes.glow());
        MarkGlyph.walk(1, (ax, ay, bx, by) -> {
            stroke(light, m, x0 + ax * w, TOP + ay * h, x0 + bx * w, TOP + by * h, 1.3f, FACE - 0.01f, argb(0.45f * g, RED));
            stroke(light, m, x0 + ax * w, TOP + ay * h, x0 + bx * w, TOP + by * h, 0.45f, FACE - 0.02f, argb(0.95f * g, HOT));
        });
        pose.popPose();
    }

    private static void rect(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int argb) {
        int a = argb >>> 24, r = argb >> 16 & 0xFF, gg = argb >> 8 & 0xFF, b = argb & 0xFF;
        // Both windings: the painted pass culls back faces.
        vc.addVertex(m, x0, y0, z).setColor(r, gg, b, a);
        vc.addVertex(m, x0, y1, z).setColor(r, gg, b, a);
        vc.addVertex(m, x1, y1, z).setColor(r, gg, b, a);
        vc.addVertex(m, x1, y0, z).setColor(r, gg, b, a);
        vc.addVertex(m, x1, y0, z).setColor(r, gg, b, a);
        vc.addVertex(m, x1, y1, z).setColor(r, gg, b, a);
        vc.addVertex(m, x0, y1, z).setColor(r, gg, b, a);
        vc.addVertex(m, x0, y0, z).setColor(r, gg, b, a);
    }

    /** A stroke in the face's plane (x, y; z fixed), both windings (the painted pass culls). */
    private static void stroke(VertexConsumer vc, Matrix4f m, float ax, float ay, float bx, float by, float width, float z, int argb) {
        float dx = bx - ax, dy = by - ay, len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) return;
        // Lengthen each piece a touch so the joints close.
        float ex = dx / len * width * 0.4f, ey = dy / len * width * 0.4f;
        float nx = -dy / len * width / 2, ny = dx / len * width / 2;
        int a = argb >>> 24, r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
        float x0 = ax - ex, y0 = ay - ey, x1 = bx + ex, y1 = by + ey;
        vc.addVertex(m, x0 + nx, y0 + ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x0 - nx, y0 - ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x1 - nx, y1 - ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x1 + nx, y1 + ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x1 + nx, y1 + ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x1 - nx, y1 - ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x0 - nx, y0 - ny, z).setColor(r, g, b, a);
        vc.addVertex(m, x0 + nx, y0 + ny, z).setColor(r, g, b, a);
    }

    static int argb(float alpha, int rgb) {
        return (int) (Mth.clamp(alpha, 0, 1) * 255) << 24 | rgb & 0xFFFFFF;
    }
}
