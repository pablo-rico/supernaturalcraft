package org.papiricoh.supernaturalcraft.crossroads.client;

import net.minecraft.client.Minecraft;
import org.papiricoh.supernaturalcraft.network.DealOfferPayload;

/** Client side of the deal: the demon's offer opens the deal screen. */
public final class DealClientHandlers {

    private DealClientHandlers() {
    }

    public static void openDeal(DealOfferPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(new DealScreen(payload, mc.player.getRandom().nextInt(DealScreen.LINES)));
    }
}
