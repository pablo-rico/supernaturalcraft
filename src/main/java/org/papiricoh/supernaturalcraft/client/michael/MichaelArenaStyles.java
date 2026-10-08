package org.papiricoh.supernaturalcraft.client.michael;

import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * Michael's Heavens as the client sees them: the dome's colour in each phase (called by {@code ArenaStyles}), his music,
 * and the air of each Heaven (fog colour and reach), faded in when the arena shifts: the Garden's warm gold, the War in
 * Heaven's smoke and embers, the Throne Room's white light.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class MichaelArenaStyles {

    // Garden gold, the War's fire, the Throne Room's white and (VI) cold blue light.
    private static final int[] DOME = {0xFFE7A8, 0xFFD27A, 0xFF9A5A, 0xFF7A3A, 0xFFFFFF, 0xBFE6FF};
    /** Fog colour of each Heaven, as r, g, b. */
    private static final float[][] FOG = {{1.0f, 0.9f, 0.68f}, {0.62f, 0.5f, 0.46f}, {0.93f, 0.96f, 1.0f}};
    /** How far one sees in each Heaven, in blocks (the War is smoky). */
    private static final float[] FOG_FAR = {120f, 56f, 90f};

    private MichaelArenaStyles() {
    }

    public static int color(int phase) {
        return DOME[Mth.clamp(phase - 1, 0, DOME.length - 1)];
    }

    public static SoundEvent music() {
        return AllSounds.MUSIC_METATRON.get();
    }

    /** Fog colour of Heaven {@code which}. */
    public static float[] fog(int which) {
        return FOG[Mth.clamp(which, 0, FOG.length - 1)];
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        int which = ClientMichael.heaven();
        if (which < 0) return;
        float k = ClientMichael.heavenFade((float) event.getPartialTick());
        if (k <= 0f) return;
        float[] c = fog(which);
        event.setRed(Mth.lerp(k, event.getRed(), c[0]));
        event.setGreen(Mth.lerp(k, event.getGreen(), c[1]));
        event.setBlue(Mth.lerp(k, event.getBlue(), c[2]));
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        int which = ClientMichael.heaven();
        if (which < 0 || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        float k = ClientMichael.heavenFade((float) event.getPartialTick());
        float far = FOG_FAR[Mth.clamp(which, 0, FOG_FAR.length - 1)];
        if (k <= 0f || event.getFarPlaneDistance() <= far) return;
        event.setFarPlaneDistance(Mth.lerp(k, event.getFarPlaneDistance(), far));
        event.setNearPlaneDistance(Mth.lerp(k, event.getNearPlaneDistance(), far * 0.3f));
        event.setCanceled(true);
    }
}
