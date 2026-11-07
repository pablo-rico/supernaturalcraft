package org.papiricoh.supernaturalcraft.client.purgatory;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Purgatory's look (v0.39): no sky to speak of (no sun, no moon, no stars, no clouds: a flat grey dusk that the fog fades into),
 * fog everywhere (its colour from the biome), and a lightmap drained toward grey-blue.
 */
public class PurgatoryEffects extends DimensionSpecialEffects {

    public PurgatoryEffects() {
        super(Float.NaN, true, SkyType.NONE, false, false);
    }

    /** The fog keeps its grey whatever the hour (there is only one). */
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.scale(0.85 + 0.15 * brightness);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    /** Colour drains out of every light: toward a cold grey with a little blue in it. */
    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        float grey = (colors.x + colors.y + colors.z) / 3f;
        colors.set(grey * 0.55f + colors.x * 0.4f, grey * 0.55f + colors.y * 0.42f, Math.min(1f, grey * 0.55f + colors.z * 0.5f + 0.02f));
    }
}
