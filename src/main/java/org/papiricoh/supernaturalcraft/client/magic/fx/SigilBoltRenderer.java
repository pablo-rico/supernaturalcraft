package org.papiricoh.supernaturalcraft.client.magic.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;

/**
 * A sigil bolt in flight (v0.21), built to read at 15-25 blocks: a white-hot core (the {@code bolt_core} star, or a drawn one) inside
 * layered halos of its school's colour, a cross of rays, sparks circling it with short tails, a thick soft ribbon tapering back
 * through the points it has just passed ({@link BoltTrails}), and the light it throws on the floor beneath it.
 *
 * <p>The entity renderer only makes sure the bolt is known; the drawing happens in {@link MagicFxManager}'s world stage
 * ({@link #renderAll}), with the rest of the magic's light, so clouds never paint over it.
 */
public class SigilBoltRenderer extends EntityRenderer<SigilBolt> {

    public SigilBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public void render(SigilBolt bolt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        BoltTrails.info(bolt);
        super.render(bolt, yaw, partial, pose, buffers, light);
    }

    /** Every bolt, in the magic's world stage. */
    static void renderAll(FxDraw d, Minecraft mc) {
        if (mc.level == null || !BoltTrails.any()) return;
        BoltTrails.forEach(mc.level, (bolt, info) -> {
            double x = Mth.lerp(d.partial, bolt.xo, bolt.getX()), y = Mth.lerp(d.partial, bolt.yo, bolt.getY()),
                    z = Mth.lerp(d.partial, bolt.zo, bolt.getZ());
            if (!MagicFxManager.visible(mc, new net.minecraft.world.phys.Vec3(x, y, z), 4)) return;
            d.floor(info.floor);
            draw(d, bolt, info, x, y, z);
        });
    }

    private static void draw(FxDraw d, SigilBolt bolt, BoltTrails.Info info, double x, double y, double z) {
        float now = d.now;
        int detail = MagicFxManager.detail();
        float pulse = 0.85f + 0.15f * Mth.sin(now * 0.9f + bolt.getId());
        float flick = 0.9f + 0.1f * Mth.sin(now * 2.3f + bolt.getId() * 3);

        // The trail: from the bolt back through its last points, thinning and fading.
        double px = x, py = y, pz = z;
        int n = info.count;
        float total = Math.max(1, n);
        for (int i = 0; i < n; i++) {
            int k = info.index(i);
            double qx = info.hx[k], qy = info.hy[k], qz = info.hz[k];
            if (i == 0 && (qx - x) * (qx - x) + (qy - y) * (qy - y) + (qz - z) * (qz - z) < 1e-4) continue;
            float f0 = 1 - i / total, f1 = 1 - (i + 1) / total;
            float e0 = f0 * f0, e1 = f1 * f1;
            d.softBeam(px, py, pz, qx, qy, qz, 1.15f * f0 + 0.1f, info.primary, 0.55f * f0, 0.55f * f1, false);
            d.taper(px, py, pz, qx, qy, qz, 0.42f * f0, 0.42f * f1, FxColor.mix(info.primary, 0xFFFFFF, 0.3f), 0.7f * e0, 0.7f * e1);
            d.taper(px, py, pz, qx, qy, qz, 0.14f * f0, 0.14f * f1, 0xFFFFFF, 0.95f * e0, 0.95f * e1);
            if (detail > 0 && i % 3 == 1) d.glowDisc(qx, qy, qz, 0.7f * f0, info.primary, 0.25f * f0);
            px = qx;
            py = qy;
            pz = qz;
        }

        // The light it throws on the floor.
        if (!Double.isNaN(info.floor)) {
            float h = (float) (y - info.floor);
            if (h < 5 && h > -0.5f) {
                float k = 1 - Mth.clamp(h / 5f, 0, 1);
                d.flatGlow(x, info.floor + 0.03, z, 1.4f + 1.6f * k, info.primary, 0.42f * k * flick);
            }
        }

        // Halos, widest and faintest first, then the core.
        if (detail > 0) d.glowDisc(x, y, z, 2.6f * pulse, info.primary, 0.16f * flick);
        d.glowDisc(x, y, z, 1.5f * pulse, info.primary, 0.38f * flick);
        d.glowDisc(x, y, z, 0.75f, info.core, 0.75f);
        ResourceLocation core = MagicTextures.boltCore();
        if (core != null) {
            d.billboard(core, x, y, z, 1.5f * pulse, now * 0.12f, info.primary, 1f);
            d.billboard(core, x, y, z, 0.85f, -now * 0.2f, 0xFFFFFF, 1f);
        } else {
            d.star(x, y, z, 0.9f * pulse, now * 0.1f, info.primary, 0.95f);
            d.star(x, y, z, 0.55f, -now * 0.16f + 0.4f, info.accent, 0.7f);
        }
        d.star(x, y, z, 1.25f * pulse, 0.785f + now * 0.03f, FxColor.mix(info.primary, 0xFFFFFF, 0.5f), 0.55f * flick);
        d.glowDisc(x, y, z, 0.32f, 0xFFFFFF, 1f);

        // Sparks circling it, each with a short tail.
        int sparks = detail == 0 ? 2 : 3;
        for (int i = 0; i < sparks; i++) {
            float a = now * 0.45f + i * Mth.TWO_PI / sparks, b = a - 0.5f;
            double ox = Mth.cos(a) * 0.48, oy = Mth.sin(a * 1.3f) * 0.26, oz = Mth.sin(a) * 0.48;
            double tx = Mth.cos(b) * 0.48, ty = Mth.sin(b * 1.3f) * 0.26, tz = Mth.sin(b) * 0.48;
            d.taper(x + ox, y + oy, z + oz, x + tx, y + ty, z + tz, 0.1f, 0, info.accent, 0.8f, 0);
            d.glowDisc(x + ox, y + oy, z + oz, 0.18f, info.accent, 0.9f);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SigilBolt bolt) {
        return MagicTextures.castCircle();
    }
}
