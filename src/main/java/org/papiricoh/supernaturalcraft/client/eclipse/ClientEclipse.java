package org.papiricoh.supernaturalcraft.client.eclipse;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.network.EclipseStatePayload;

/** The eclipse as this client sees it: on or off for the current dimension, faded over ten seconds. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientEclipse {

    public static final int FADE_TICKS = 200;
    private static boolean active;
    private static long endTick;
    private static float fade, lastFade;

    private ClientEclipse() {
    }

    public static void apply(EclipseStatePayload p) {
        active = p.active();
        endTick = p.endTick();
        var level = Minecraft.getInstance().level;
        // Joining a world under a long-risen eclipse should not watch it rise again.
        if (active && level != null && level.getGameTime() - p.startTick() > FADE_TICKS) fade = lastFade = 1f;
        if (!active && level == null) fade = lastFade = 0f;
    }

    public static boolean active() {
        return active;
    }

    /** 0 = clear sky, 1 = full eclipse. */
    public static float intensity(float partialTick) {
        return Mth.lerp(partialTick, lastFade, fade);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var level = Minecraft.getInstance().level;
        lastFade = fade;
        boolean on = active && level != null && level.getGameTime() < endTick;
        fade = Mth.clamp(fade + (on ? 1f : -1f) / FADE_TICKS, 0f, 1f);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        fade = lastFade = 0f;
    }
}
