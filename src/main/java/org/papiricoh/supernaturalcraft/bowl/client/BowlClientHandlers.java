package org.papiricoh.supernaturalcraft.bowl.client;

import net.minecraft.client.Minecraft;
import org.papiricoh.supernaturalcraft.network.OpenRecitationPayload;

/** Client side of the recitation: the bowl caught, so the incantation screen opens. */
public final class BowlClientHandlers {

    private BowlClientHandlers() {
    }

    public static void openRecitation(OpenRecitationPayload payload) {
        Minecraft.getInstance().setScreen(new RecitationScreen(payload));
    }
}
