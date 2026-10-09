package org.papiricoh.supernaturalcraft.legacy.bunker;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;

/** The order's furniture keeps to the order (v0.17): the bunker's door, its research desks and its map table only break for members. */
public final class BunkerProtection {

    private BunkerProtection() {
    }

    /** Whether {@code player} may break the order's furniture. */
    public static boolean mayBreak(Player player) {
        if (player.isCreative()) return true;
        // The client keeps its own hunter's record apart (the attachment is not synced onto the entity).
        if (player.level().isClientSide) return org.papiricoh.supernaturalcraft.client.legacy.ClientLegacy.member();
        return Legacies.member(player);
    }

    public static boolean guarded(BlockState state) {
        return state.is(AllBlocks.BUNKER_DOOR.get()) || state.is(AllBlocks.RESEARCH_DESK.get()) || state.is(AllBlocks.MAP_TABLE.get());
    }
}
