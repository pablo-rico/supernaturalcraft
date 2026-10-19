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
import org.papiricoh.supernaturalcraft.magic.school.School;

/**
 * A glyph trap ({@code sigil_glyph}, v0.21): a small sigil on the floor in its spell's colours. It is written in over its first
 * second (a pie wipe closing), flashes once as it arms, then lies low: turning slowly, breathing, a glint now and then running
 * round its rim. When it springs the server's WARD_PULSE and IMPACT take over.
 */
public class GlyphTrapRenderer extends EntityRenderer<WardEntity> {

    /** Ticks it takes to write itself in (and arm). */
    private static final float ARM = 20;

    public GlyphTrapRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    /** The trap's colours: its school's when the client knows it, otherwise its spell's own colour. */
    static int[] colours(WardEntity ward) {
        School school = ward.getSchool();
        if (school != null && !school.isNone()) return new int[]{FxColor.primary(school), FxColor.accent(school)};
        int c = ward.getColor();
        if (FxColor.luminance(c) < 0.35f) c = FxColor.mix(c, 0xFFFFFF, 0.35f);
        return new int[]{c, FxColor.mix(c, 0xFFFFFF, 0.5f)};
    }

    @Override
    public void render(WardEntity ward, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        double x = Mth.lerp(partial, ward.xo, ward.getX()), y = Mth.lerp(partial, ward.yo, ward.getY()),
                z = Mth.lerp(partial, ward.zo, ward.getZ());
        FxDraw d = new FxDraw(pose, buffers, mc.gameRenderer.getMainCamera(), new Vec3(x, y, z), partial, MagicFxManager.clock() + partial);
        d.floor(y);
        float age = ward.tickCount + partial, life = ward.getLifetime();
        float fade = Mth.clamp(Math.min(1, (life - age) / 20f), 0, 1);
        if (fade <= 0) return;
        int[] col = colours(ward);
        int primary = col[0], accent = col[1];
        float r = ward.getRadius();
        double gy = y + 0.03;
        float write = Mth.clamp(age / ARM, 0, 1);
        float breathe = 0.78f + 0.22f * Mth.sin(age * 0.11f + ward.getId());
        float a = fade * (write < 1 ? 1 : breathe);
        ResourceLocation circle = MagicTextures.castCircle();
        // The sigil: the circle (written in as a closing wipe), an inner one turning the other way, glyphs round the rim.
        d.flatGlow(x, gy - 0.01, z, r * 1.3f, primary, 0.22f * a);
        d.wipe(circle, x, gy, z, 0, 1, 0, r * 0.95f, age * 0.012f, write, primary, 0.8f * a);
        if (write >= 1) {
            d.decal(circle, x, gy + 0.005, z, r * 0.5f, -age * 0.025f, accent, 0.55f * a);
            if (MagicFxManager.detail() > 0) {
                d.glyphRing(MagicTextures.glyphStrip(), x, gy + 0.002, z, 0, 1, 0, r * 0.98f, r * 1.14f, -age * 0.008f, primary, 0.5f * a);
            }
        }
        // The pen running round as it is written.
        if (write < 1) {
            float ang = -Mth.TWO_PI * write + age * 0.012f;
            d.star(x + Mth.cos(ang) * r * 0.9, gy + 0.1, z - Mth.sin(ang) * r * 0.9, 0.45f, age * 0.2f, 0xFFFFFF, 0.9f * fade);
        }
        // Armed: one flash, then a glint every few seconds running round the rim.
        float since = age - ARM;
        if (since >= 0 && since < 8) {
            float k = since / 8f;
            d.star(x, gy + 0.25, z, r * 1.6f * (0.6f + 0.4f * k), 0.3f, accent, 1 - k);
            d.groundRing(x, gy + 0.01, z, r * (0.9f + 0.5f * k), r * (1f + 0.6f * k), primary, 0.7f * (1 - k), true);
        }
        if (since >= 8) {
            float cyc = (since % 70f) / 70f;
            if (cyc < 0.3f) {
                float k = cyc / 0.3f, ang = ward.getId() + k * Mth.TWO_PI * 0.75f;
                d.star(x + Mth.cos(ang) * r, gy + 0.08, z + Mth.sin(ang) * r, 0.35f, age * 0.15f, 0xFFFFFF,
                        0.85f * fade * Mth.sin(Mth.PI * k));
            }
            d.glowDisc(x, gy + 0.08, z, 0.25f, accent, 0.5f * a);
        }
        super.render(ward, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WardEntity ward) {
        return MagicTextures.castCircle();
    }
}
