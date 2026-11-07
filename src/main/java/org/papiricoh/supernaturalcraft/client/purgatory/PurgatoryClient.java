package org.papiricoh.supernaturalcraft.client.purgatory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SNClientConfig;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.purgatory.PurgatoryDimension;

/**
 * Purgatory on the client (v0.39): its close grey fog, thickest in the Mire's hollows, thinnest on the Crags. Only seen; the config
 * can turn it off ({@code purgatoryFog}) for shader packs that draw their own.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class PurgatoryClient {

    private PurgatoryClient() {
    }

    static boolean inPurgatory() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(PurgatoryDimension.LEVEL);
    }

    /** How far the fog lets you see in each region of Purgatory. */
    public static float fogDistance(Holder<Biome> biome) {
        if (biome.is(PurgatoryDimension.MIRE)) return 26f;
        if (biome.is(PurgatoryDimension.MOTHERS_WOOD)) return 34f;
        if (biome.is(PurgatoryDimension.CRAGS)) return 58f;
        return 42f;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (!inPurgatory() || !SNClientConfig.PURGATORY_FOG.get() || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        Minecraft mc = Minecraft.getInstance();
        float far = fogDistance(mc.level.getBiome(event.getCamera().getBlockPosition()));
        if (event.getFarPlaneDistance() <= far) return;
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(far * 0.08f);
        event.setCanceled(true);
    }
}
