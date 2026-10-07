package org.papiricoh.supernaturalcraft.client.eclipse;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The overworld's effects with one difference: under an eclipse the sky's share of the lightmap
 * goes out (torches, lava and glowstone keep burning) and the clouds are hidden. Without an
 * eclipse everything is vanilla.
 */
public class EclipseOverworldEffects extends DimensionSpecialEffects.OverworldEffects {

    /** What little light the eclipsed sky still gives, as a fraction of normal. */
    public static final float RESIDUAL_SKY = 0.12f, MARKED_SKY = 0.6f;

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        float k = ClientEclipse.intensity(partialTicks);
        if (k <= 0f) return;
        // Vanilla's block-light colour for this column, plus a faint violet remnant of the sky.
        float b = LightTexture.getBrightness(level.dimensionType(), pixelX) * blockLightRedFlicker;
        Vector3f eclipsed = new Vector3f(b, b * ((b * 0.6f + 0.4f) * 0.6f + 0.4f), b * (b * b * 0.6f + 0.4f));
        // Those who drank the Eclipse Sight see by the dark itself.
        float s = skyLight * (org.papiricoh.supernaturalcraft.client.ClientArcana.voidMark() ? MARKED_SKY : RESIDUAL_SKY);
        eclipsed.add(s * 0.75f, s * 0.7f, s * 1.1f);
        eclipsed.lerp(new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
        colors.lerp(eclipsed, k);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return ClientEclipse.intensity(partialTick) > 0.3f;
    }
}
