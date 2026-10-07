package org.papiricoh.supernaturalcraft.client.hell;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Hell's look: no sky at all, fog everywhere (its colour comes from the biome), and a lightmap that
 * pulls every light toward blood red.
 */
public class HellEffects extends DimensionSpecialEffects {

    public HellEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        float torment = ClientTorment.value();
        colors.set(Math.min(1f, colors.x * 1.02f + 0.035f), colors.y * (0.82f - torment * 0.1f), colors.z * (0.76f - torment * 0.12f));
    }
}
