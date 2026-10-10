package org.papiricoh.supernaturalcraft.heaven;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.entity.heaven.AshEntity;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.AshMenu;
import org.papiricoh.supernaturalcraft.heaven.roadhouse.Roadhouse;
import org.papiricoh.supernaturalcraft.network.AshChoicePayload;

/** Server handlers of Heaven's client payloads (v0.18). */
public final class HeavenServerHandlers {

    private HeavenServerHandlers() {
    }

    /** A choice in Ash's menu: only from someone standing at his bar ({@link AshMenu#REACH}). */
    public static void ashChoice(AshChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.serverLevel().getEntity(payload.ash()) instanceof AshEntity ash) || !ash.isAlive()) return;
        if (player.distanceToSqr(ash) > AshMenu.REACH * AshMenu.REACH) return;
        Roadhouse.choose(ash, player, payload.action(), payload.arg() == null ? "" : payload.arg());
    }
}
