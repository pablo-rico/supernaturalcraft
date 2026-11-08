package org.papiricoh.supernaturalcraft.client.dyingsun;

import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunPayload;
import org.papiricoh.supernaturalcraft.dyingsun.DyingSunRules;

/**
 * The dying sun as this client sees it (v0.42): the contract the sky, the lightmap, the fog and the HUD's "Light of the World"
 * read. The server's darkness is eased toward over a few seconds so steps never pop. Only the Overworld's sky dies.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID, value = Dist.CLIENT)
public final class ClientDyingSun {

    /** Ticks for the shown darkness to cross the whole range (a step of 1/64 takes ~2 ticks). */
    private static final float EASE_TICKS = 120f;
    private static byte state = DyingSunRules.NONE;
    private static float target, shown, lastShown;
    private static boolean snap = true;

    private ClientDyingSun() {
    }

    public static void apply(DyingSunPayload p) {
        state = p.state();
        target = p.darkness();
        if (snap) {
            shown = lastShown = target;
            snap = false;
        }
    }

    /** {@link DyingSunRules#NONE}, {@link DyingSunRules#DYING} or {@link DyingSunRules#RESTORED}. */
    public static byte state() {
        return state;
    }

    /** 0 = a whole sun, 1 = none (Amara's phase VI at its end). */
    public static float darkness(float partialTick) {
        return Mth.lerp(partialTick, lastShown, shown);
    }

    /** The "Light of the World" (1 - darkness), for the HUD. */
    public static float light(float partialTick) {
        return 1f - darkness(partialTick);
    }

    public static boolean dying() {
        return state == DyingSunRules.DYING;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        lastShown = shown;
        float step = 1f / EASE_TICKS;
        shown = shown < target ? Math.min(target, shown + step) : Math.max(target, shown - step);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = DyingSunRules.NONE;
        target = shown = lastShown = 0f;
        snap = true;
    }
}
