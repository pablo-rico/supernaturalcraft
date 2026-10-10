package org.papiricoh.supernaturalcraft.client.heaven;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.heaven.HeavenDimension;

/**
 * Heaven's look (v0.18): its own sky ({@link HeavenSky}), vanilla's clouds lowered beneath the floating plots so the islands
 * stand on a sea of cloud, never rain, warm far fog, and a lightmap warmed and lifted so shade under the islands is soft.
 */
public class HeavenEffects extends DimensionSpecialEffects {

    /** The clouds' height: under every plot's floor ({@link HeavenDimension#PLOT_Y}) and above the rescue line. */
    public static final float CLOUD_HEIGHT = HeavenDimension.PLOT_Y - 30;

    public HeavenEffects() {
        super(CLOUD_HEIGHT, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    /** Never a sunrise glow: it is always noon here. */
    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        setupFog.run();
        HeavenSky.render(modelViewMatrix, partialTick, ticks);
        return true;
    }

    /** The sea of cloud under the islands, then vanilla's clouds over it. */
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        HeavenSky.cloudSea(modelViewMatrix, partialTick, ticks, camX, camY, camZ);
        return false;
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

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        // Shade is never dark in Heaven, and the light is a little golden.
        float lift = 0.22f;
        colors.set(Mth.clamp(colors.x + (1 - colors.x) * lift + 0.015f, 0, 1), Mth.clamp(colors.y + (1 - colors.y) * lift, 0, 1),
                Mth.clamp(colors.z + (1 - colors.z) * lift * 0.85f - 0.01f, 0, 1));
    }
}
