package org.papiricoh.supernaturalcraft.client.eclipse;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The overworld's effects (v0.42: the eclipse is gone; the class keeps its name for its registration): the red moon's tint of the
 * seals, and the dying sun's lightmap and hidden clouds ({@code DyingSunSky}). With a whole sun everything is vanilla.
 */
public class EclipseOverworldEffects extends DimensionSpecialEffects.OverworldEffects {

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        org.papiricoh.supernaturalcraft.client.apocalypse.seal.SealSky.lightmap(level, partialTicks, skyLight, colors); // v0.40 seals
        // v0.42: the dying sun (and Amara's sky) take the sky's share of the light away.
        org.papiricoh.supernaturalcraft.client.dyingsun.DyingSunSky.lightmap(level, partialTicks, skyDarken, blockLightRedFlicker, skyLight,
                pixelX, pixelY, colors);
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return org.papiricoh.supernaturalcraft.client.dyingsun.DyingSunSky.cloudsHidden(partialTick);
    }
}
