package org.papiricoh.supernaturalcraft.client.nephilim;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.Vector3f;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.empty.EmptyDimension;

/**
 * The Empty's look (v0.41): no sky and no clouds, a fog of black with violet in it, and every light drained toward a dim
 * violet-grey, so the floor, the sleepers and the Shadow are seen and nothing else.
 */
public class EmptyEffects extends DimensionSpecialEffects {

    public EmptyEffects() {
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.035, 0.02, 0.06);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return true;
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        float grey = (colors.x + colors.y + colors.z) / 3f;
        colors.set(Math.max(0.24f, grey * 0.7f + 0.06f), Math.max(0.2f, grey * 0.6f + 0.04f), Math.max(0.32f, grey * 0.8f + 0.12f));
    }

    @EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(EmptyDimension.EFFECTS, new EmptyEffects());
        }
    }
}
