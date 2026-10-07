package org.papiricoh.supernaturalcraft.bowl;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.network.RecitationResultPayload;

/** Server side of the recitation: the caster's report goes to the bowl, which re-validates it. */
public final class BowlServerHandlers {

    private BowlServerHandlers() {
    }

    public static void recitationResult(RecitationResultPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        // Only loaded chunks: a far-off position is not worth loading, and the bowl checks distance anyway.
        if (!player.serverLevel().isLoaded(payload.pos())) return;
        if (player.serverLevel().getBlockEntity(payload.pos()) instanceof SpellBowlBlockEntity bowl) {
            bowl.resolveRecitation(player, payload.success(), payload.typos());
        }
    }
}
