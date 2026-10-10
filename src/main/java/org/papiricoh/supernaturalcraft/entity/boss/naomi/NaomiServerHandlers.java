package org.papiricoh.supernaturalcraft.entity.boss.naomi;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.network.ChairStrugglePayload;

/**
 * Server handler of the chair's struggle (v0.18): only the hunter strapped into that chair is heard, and the chair believes at most
 * {@link ChairRules#MAX_PER_SECOND} presses a second ({@link ChairRules.Budget}).
 */
public final class NaomiServerHandlers {

    private NaomiServerHandlers() {
    }

    public static void struggle(ChairStrugglePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) handle(player, payload);
    }

    /** The checks and the count (public for tests). @return presses counted */
    public static int handle(ServerPlayer player, ChairStrugglePayload payload) {
        if (!player.isAlive() || payload.presses() <= 0) return 0;
        if (!(player.level().getEntity(payload.chair()) instanceof ReprogrammingChairEntity chair) || player.getVehicle() != chair) return 0;
        return chair.struggle(player, payload.presses());
    }
}
