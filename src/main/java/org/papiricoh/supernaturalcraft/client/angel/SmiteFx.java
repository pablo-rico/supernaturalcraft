package org.papiricoh.supernaturalcraft.client.angel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.chuck.render.GeoGuard;
import org.papiricoh.supernaturalcraft.client.magic.fx.FxDraw;
import org.papiricoh.supernaturalcraft.client.magic.fx.MagicRenderTypes;
import org.papiricoh.supernaturalcraft.entity.angel.AngelAssets;

import static org.papiricoh.supernaturalcraft.client.angel.ClientAngels.GRACE;
import static org.papiricoh.supernaturalcraft.client.angel.ClientAngels.GRACE_HOT;

/**
 * Draws a smite in the world (v0.22): the beams of light pouring from a smitten angel's eyes and mouth, growing longer, wider and
 * brighter and flickering; the light breaking out through its body; the blade left in its chest; then the flash. And its wings
 * burnt into the ground ({@link #scorch}). The angel's own body glows by {@link SmiteGlowLayer}.
 */
final class SmiteFx {

    static final ResourceLocation WING_SCORCH = SupernaturalCraft.asResource(AngelAssets.WING_SCORCH);
    /** The scorch fades out over its last ticks. */
    private static final int SCORCH_FADE = 200;

    private SmiteFx() {
    }

    static void smite(FxDraw d, Minecraft mc, ClientLevel level, ClientAngels.Smite s, float partial) {
        float age = s.age(partial);
        Entity e = level.getEntity(s.id);
        if (age < s.duration && e != null && e.isAlive()) {
            Vec3 pos = e.getPosition(partial);
            Vec3 chest = pos.add(0, e.getBbHeight() * 0.62, 0);
            s.chest = chest;
            burning(d, s, e, pos, chest, age);
            blade(d, mc, level, s, chest);
        }
        float q = (age - s.duration) / ClientAngels.FLASH_TICKS;
        if (q >= -0.15f && q < 1) flash(d, s, Math.max(0, q), age);
    }

    // --- the light pouring out --------------------------------------------------------------------------------------------

    private static void burning(FxDraw d, ClientAngels.Smite s, Entity e, Vec3 pos, Vec3 chest, float age) {
        float k = Mth.clamp(age / s.duration, 0, 1), grow = (float) Math.pow(k, 1.5);
        float flick = 0.84f + 0.16f * Mth.sin(age * 2.9f + s.id) * Mth.sin(age * 0.83f + 1.3f);
        d.floor(pos.y);
        // Its lights: as its renderer saw them this frame, else from where it looks.
        Vec3 look = e.getViewVector(d.partial);
        Vec3 side = new Vec3(-look.z, 0, look.x);
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 eye = e.getEyePosition(d.partial);
        for (int i = 0; i < 3; i++) {
            Vec3 p, f;
            if (s.current(i)) {
                int o = i * 6;
                p = new Vec3(s.lights[o], s.lights[o + 1], s.lights[o + 2]);
                f = new Vec3(s.lights[o + 3], s.lights[o + 4], s.lights[o + 5]);
            } else {
                double across = i == ClientAngels.Smite.EYE_L ? 0.11 : i == ClientAngels.Smite.EYE_R ? -0.11 : 0;
                double down = i == ClientAngels.Smite.MOUTH ? -0.17 : 0;
                p = eye.add(side.scale(across)).add(0, down, 0).add(look.scale(0.24));
                f = look;
            }
            light(d, s, p, f, i == ClientAngels.Smite.MOUTH, k, grow, flick, age, i);
        }
        // The light breaking out through the body: thin rays from the chest, more and longer as it burns.
        if (k > 0.3f) {
            float kk = (k - 0.3f) / 0.7f;
            Vec3 fwd = s.forward();
            int rays = 3 + (int) (5 * kk);
            for (int r = 0; r < rays; r++) {
                long h = (s.id * 31L + r) * 0x9E3779B97F4A7C15L;
                float yaw = ((h >>> 8 & 0xFFFF) / 65535f) * Mth.TWO_PI, pitch = ((h >>> 24 & 0xFFFF) / 65535f - 0.35f) * 1.4f;
                Vec3 dir = new Vec3(Mth.cos(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.sin(yaw) * Mth.cos(pitch));
                float on = Mth.clamp(kk * rays - r, 0, 1);
                float fl = 0.6f + 0.4f * Mth.sin(age * (1.7f + r * 0.37f) + r * 2.1f);
                double off = ((h >>> 40 & 0xFF) / 255f - 0.5f) * 0.5;
                Vec3 from = chest.add(0, off, 0).add(fwd.scale(0.08));
                float len = (0.35f + 1.9f * kk) * (0.6f + 0.4f * ((h >>> 48 & 0xFF) / 255f));
                d.softBeam(from.x, from.y, from.z, from.x + dir.x * len, from.y + dir.y * len, from.z + dir.z * len,
                        0.05f + 0.16f * kk, GRACE, 0.55f * on * fl, 0, true);
            }
        }
        // Its whole shape lit from within, and the ground under it.
        d.glowDisc(chest.x, chest.y, chest.z, 0.55f + 1.9f * grow * grow, GRACE, (0.12f + 0.42f * grow) * flick);
        d.flatGlow(pos.x, pos.y + 0.04, pos.z, 1.2f + 3.2f * grow, GRACE, 0.4f * grow);
    }

    /** One light (an eye, the mouth): a growing beam with a softer cone round it and a hot point where it leaves the face. */
    private static void light(FxDraw d, ClientAngels.Smite s, Vec3 p, Vec3 f, boolean mouth, float k, float grow, float flick, float age,
                              int i) {
        float len = (mouth ? 0.22f : 0.3f) + (mouth ? 2.6f : 3.4f) * grow;
        float w = (mouth ? 0.14f : 0.09f) + (mouth ? 0.62f : 0.5f) * grow;
        float a = (0.38f + 0.62f * k) * flick;
        // Never into the viewer's eye: a beam aimed at whoever struck it stops short of them.
        Vec3 toEye = d.eye().subtract(p);
        double ahead = toEye.dot(f);
        if (ahead > 0 && toEye.lengthSqr() - ahead * ahead < 1.0) len = (float) Math.max(0.2, Math.min(len, ahead - 0.6));
        Vec3 end = p.add(f.scale(len));
        d.softBeam(p.x, p.y, p.z, end.x, end.y, end.z, w, GRACE, a, 0, true);
        // The cone: two wider, fainter beams diverging and turning slowly round the main one.
        float[] ax = new float[3], ay = new float[3];
        FxDraw.basis((float) f.x, (float) f.y, (float) f.z, ax, ay);
        float spread = 0.1f + 0.22f * k;
        for (int j = 0; j < 2; j++) {
            float ang = age * 0.21f + j * Mth.PI + i * 1.1f;
            double dx = f.x + (ax[0] * Mth.cos(ang) + ay[0] * Mth.sin(ang)) * spread,
                    dy = f.y + (ax[1] * Mth.cos(ang) + ay[1] * Mth.sin(ang)) * spread,
                    dz = f.z + (ax[2] * Mth.cos(ang) + ay[2] * Mth.sin(ang)) * spread;
            double n = Math.sqrt(dx * dx + dy * dy + dz * dz), l = len * 0.85 / n;
            d.softBeam(p.x, p.y, p.z, p.x + dx * l, p.y + dy * l, p.z + dz * l, w * 1.5f, GRACE, a * 0.32f, 0, false);
        }
        d.glowDisc(p.x, p.y, p.z, 0.07f + 0.32f * grow, GRACE_HOT, (0.65f + 0.35f * k) * flick);
        if (k > 0.55f) {
            float kk = (k - 0.55f) / 0.45f;
            d.star(p.x, p.y, p.z, 0.25f + 0.85f * kk, age * 0.04f + i, GRACE_HOT, 0.75f * kk * flick);
        }
    }

    /** The flash as it falls ({@code q} 0-1 through it; a breath of it just before). */
    private static void flash(FxDraw d, ClientAngels.Smite s, float q, float age) {
        Vec3 c = s.chest;
        float pre = age < s.duration ? Mth.clamp(1 - (s.duration - age) / (0.15f * ClientAngels.FLASH_TICKS), 0, 1) : 1;
        float a = pre * (float) Math.pow(1 - q, 1.6);
        d.floor(s.point.y);
        d.glowDisc(c.x, c.y, c.z, 2.2f + 4.5f * q, GRACE_HOT, a);
        d.glowDisc(c.x, c.y, c.z, 0.8f + 1.2f * q, 0xFFFFFF, Math.min(1, a * 1.3f));
        d.star(c.x, c.y, c.z, 7.5f * (1 - 0.45f * q), 0.4f + age * 0.01f, 0xFFFFFF, a);
        d.flatGlow(c.x, s.point.y + 0.05, c.z, 5.5f + 4f * q, GRACE, 0.65f * a);
        float r0 = 0.6f + 7.5f * q;
        d.ring(c.x, s.point.y + 0.35, c.z, 0, 1, 0, r0, r0 + 0.7f + 0.8f * q, GRACE, 0.45f * a, true);
    }

    // --- the blade in its chest ---------------------------------------------------------------------------------------------

    private static void blade(FxDraw d, Minecraft mc, ClientLevel level, ClientAngels.Smite s, Vec3 chest) {
        if (s.blade <= 0) return;
        var item = BuiltInRegistries.ITEM.byId(s.blade);
        if (item == Items.AIR) return;
        Vec3 fwd = s.forward(), eye = d.eye();
        // Driven in up under the ribs: the hilt stands out of its chest.
        Vec3 at = chest.add(fwd.scale(0.2)).add(0, -0.08, 0);
        PoseStack pose = d.pose;
        pose.pushPose();
        pose.translate(at.x - eye.x, at.y - eye.y, at.z - eye.z);
        pose.mulPose(Axis.YP.rotationDegrees(-s.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(14));
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        pose.scale(0.75f, 0.75f, 0.75f);
        mc.getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, pose,
                d.buffers, level, s.id);
        pose.popPose();
    }

    // --- the burnt wings ------------------------------------------------------------------------------------------------------

    /** The scorch, laid on its grid of ground heights; fading in with the flash and out over its last {@link #SCORCH_FADE} ticks. */
    static void scorch(FxDraw d, ClientAngels.Scorch s, float age) {
        float alpha = Mth.clamp(age / 5f, 0, 1) * Mth.clamp((AngelAssets.SCORCH_TICKS - age) / SCORCH_FADE, 0, 1);
        if (alpha <= 0.004f) return;
        // Still hot at first: a glow pool along the wings under the dark.
        if (age < ClientAngels.SCORCH_HOT) {
            float hot = 1 - age / ClientAngels.SCORCH_HOT;
            d.flatGlow(s.x(), s.y() + 0.03, s.z(), 2.6f, 0xFF9A50, 0.3f * hot * hot);
        }
        if (!GeoGuard.exists(WING_SCORCH)) return;
        float yr = s.yaw() * Mth.DEG_TO_RAD;
        float fx = -Mth.sin(yr), fz = Mth.cos(yr);
        Vec3 eye = d.eye();
        VertexConsumer vc = d.buffers.getBuffer(MagicRenderTypes.ink(WING_SCORCH));
        PoseStack.Pose p = d.pose.last();
        int g = ClientAngels.Scorch.GRID, a = (int) (alpha * 255);
        float lift = 0.015f + (s.index() % 8) * 0.002f;
        for (int j = 0; j < g; j++) {
            for (int i = 0; i < g; i++) {
                float h00 = s.heights()[j * (g + 1) + i], h10 = s.heights()[j * (g + 1) + i + 1];
                float h01 = s.heights()[(j + 1) * (g + 1) + i], h11 = s.heights()[(j + 1) * (g + 1) + i + 1];
                if (Float.isNaN(h00) || Float.isNaN(h10) || Float.isNaN(h01) || Float.isNaN(h11)) continue;
                vertex(vc, p, s, eye, fx, fz, i, j, h00 + lift, a);
                vertex(vc, p, s, eye, fx, fz, i + 1, j, h10 + lift, a);
                vertex(vc, p, s, eye, fx, fz, i + 1, j + 1, h11 + lift, a);
                vertex(vc, p, s, eye, fx, fz, i, j + 1, h01 + lift, a);
            }
        }
    }

    /**
     * Grid corner (i, j) of a scorch. The texture (twice as wide as deep) lies with its top (v 0) behind the angel and its bottom
     * middle where it stood: the wings spread across it to either side.
     */
    private static void vertex(VertexConsumer vc, PoseStack.Pose p, ClientAngels.Scorch s, Vec3 eye, float fx, float fz, int i, int j,
                               float h, int a) {
        int g = ClientAngels.Scorch.GRID;
        float u = i / (float) g, v = j / (float) g;
        float across = (u * 2 - 1) * ClientAngels.Scorch.HALF, along = ClientAngels.Scorch.along(v);
        // u runs to the angel's right; v from behind it (top of the texture) to before it.
        float x = (float) (s.x() - eye.x) - fz * across + fx * along, z = (float) (s.z() - eye.z) + fx * across + fz * along;
        float y = (float) (s.y() - eye.y) + h;
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(p, 0, 1, 0);
    }
}
