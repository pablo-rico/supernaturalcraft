package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.network.DealChoicePayload;

/** The player chose from the demon's offer (or walked away). Every claim is checked again. */
public final class DealServerHandlers {

    private DealServerHandlers() {
    }

    public static void choice(DealChoicePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer p)) return;
        if (!(p.level().getEntity(payload.entityId()) instanceof CrossroadsDemonEntity demon)) return;
        if (payload.wish().isEmpty()) return;
        if (!demon.isAlive() || demon.isHostile() || demon.isLeaving() || !demon.isSummoner(p)) return;
        if (p.distanceToSqr(demon) > Deals.REACH * Deals.REACH) return;
        DealTerms.Wish wish = DealTerms.Wish.byId(payload.wish());
        if (wish == null || !wish.validArg(payload.arg())) return;
        Deals.seal(p, demon, wish, payload.arg(), payload.bindSoul());
    }
}
