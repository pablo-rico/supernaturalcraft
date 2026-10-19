package org.papiricoh.supernaturalcraft.client.magic.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.entity.magic.WardEntity;

/**
 * A lingering zone ({@code lingering_zone}: a catalysed Burst or the {@code linger} modifier, v0.21): a pool of its spell's light on
 * the ground (its school's colours once the client knows them, {@link GlyphTrapRenderer#colours}), a ring of glyphs low round its
 * edge, a thin wall of light breathing on that edge, and motes drifting up out of it. It fades in and out with its life.
 */
public class LingerZoneRenderer extends EntityRenderer<WardEntity> {

    public LingerZoneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public void render(WardEntity zone, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        double x = Mth.lerp(partial, zone.xo, zone.getX()), y = Mth.lerp(partial, zone.yo, zone.getY()),
                z = Mth.lerp(partial, zone.zo, zone.getZ());
        FxDraw d = new FxDraw(pose, buffers, mc.gameRenderer.getMainCamera(), new Vec3(x, y, z), partial, MagicFxManager.clock() + partial);
        d.floor(y);
        float age = zone.tickCount + partial, life = zone.getLifetime();
        float fade = Mth.clamp(Math.min(age / 10f, (life - age) / 20f), 0, 1);
        if (fade <= 0) return;
        int[] col = GlyphTrapRenderer.colours(zone);
        int primary = col[0], accent = col[1];
        float r = zone.getRadius();
        double gy = y + 0.03;
        float breathe = 0.75f + 0.25f * Mth.sin(age * 0.09f + zone.getId());
        float a = fade * breathe;
        int detail = MagicFxManager.detail();
        d.flatGlow(x, gy, z, r * 1.15f, primary, 0.38f * a);
        d.groundRing(x, gy + 0.005, z, r * 0.9f, r, primary, 0.7f * a, true);
        d.decal(MagicTextures.castCircle(), x, gy + 0.004, z, r * 0.85f, age * 0.006f, primary, 0.35f * fade);
        if (detail > 0) {
            d.glyphRing(MagicTextures.glyphStrip(), x, gy + 0.01, z, 0, 1, 0, r * 0.82f, r * 0.96f, age * 0.01f, accent, 0.55f * a);
            d.band(MagicTextures.glyphStrip(), x, gy, z, r, 0.45f, -age * 0.012f, primary, 0.35f * a, 0);
        }
        d.cylinder(x, gy, z, r, 0.9f, primary, 0.22f * a, 0);
        // Motes drifting up out of it (drawn, so they cost no particles and keep their place in the pool).
        int motes = detail == 0 ? 4 : detail == 1 ? 7 : 12;
        for (int i = 0; i < motes; i++) {
            float u = (age * 0.012f + i * 0.618f) % 1f;
            float ang = i * 2.399f + age * 0.01f, rr = r * (0.2f + 0.7f * ((i * 0.37f) % 1f));
            d.glowDisc(x + Mth.cos(ang) * rr, gy + 0.1 + u * 1.6f, z + Mth.sin(ang) * rr, 0.12f + 0.06f * (1 - u),
                    i % 2 == 0 ? accent : primary, 0.75f * fade * Mth.sin(Mth.PI * u));
        }
        super.render(zone, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WardEntity zone) {
        return MagicTextures.castCircle();
    }
}
