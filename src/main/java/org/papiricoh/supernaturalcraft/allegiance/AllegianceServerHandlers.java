package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.network.AllegianceChoicePayload;
import org.papiricoh.supernaturalcraft.network.CastPowerPayload;

/** Server side of the allegiance's player → server payloads. */
public final class AllegianceServerHandlers {

    private AllegianceServerHandlers() {
    }

    public static void cast(CastPowerPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        org.papiricoh.supernaturalcraft.allegiance.power.PowerCaster.cast(player, payload);
    }

    public static void choice(AllegianceChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        AllegianceDialogue.answer(player, payload);
    }
}
