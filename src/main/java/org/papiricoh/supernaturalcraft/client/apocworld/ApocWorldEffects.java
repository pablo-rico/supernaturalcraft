package org.papiricoh.supernaturalcraft.client.apocworld;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Apocalypse World's look (v0.41): its own sky ({@link ApocWorldSky}), no clouds (the overcast is the sky's), no rain ever (ash
 * drifts on the biomes' air instead), a brown haze, and a lightmap pulled toward a smoky amber: the light of a late afternoon
 * through the smoke of a burned world.
 */
public class ApocWorldEffects extends DimensionSpecialEffects {

    public ApocWorldEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();
        ApocWorldSky.render(modelViewMatrix, partialTick, ticks);
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX,
                                     double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    /** Everything a little dimmer and warmer: amber light, no blue in the shade. */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        colors.set(Mth.clamp(colors.x * 0.92f + 0.03f, 0, 1), Mth.clamp(colors.y * 0.8f, 0, 1), Mth.clamp(colors.z * 0.62f, 0, 1));
    }
}
