package org.papiricoh.supernaturalcraft.memory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Where the rest of the mod tells the memory log that something worth remembering happened (v0.18). One-line calls from other
 * systems, so they never depend on how memories are kept.
 */
public final class MemoryHooks {

    private MemoryHooks() {
    }

    /**
     * A deal sealed at the crossroads ({@code Deals.seal} calls it).
     *
     * @param wish the wish's name ({@code DealTerms.Wish})
     * @param arg  its argument (e.g. which upgrade), may be empty
     * @param wild whether it was struck at a natural crossroads (a wild bargain) rather than over a bowl
     */
    public static void dealSealed(Player player, String wish, String arg, boolean wild) {
        if (!(player instanceof ServerPlayer p)) return;
        int n = MemoryRules.count(Memories.get(p).entries(), MemoryKind.CROSSROADS_DEAL);
        // Ids stay unique even after the oldest deals were pushed out of a full log.
        while (Memories.get(p).has("deal:" + n)) n++;
        Memories.append(p, MemoryRules.deal(n, wish, arg, wild, Memories.gameTime(p), System.currentTimeMillis()));
    }

    /**
     * A Men of Letters case closed ({@code CaseSites.settle} calls it, once, when its hunter hears of it).
     *
     * @param index    the case's number
     * @param monster  the monster's entity type id
     * @param scenario the scenario's id
     * @param solved   solved, or lost
     */
    public static void caseClosed(Player player, int index, String monster, String scenario, boolean solved) {
        if (!(player instanceof ServerPlayer p)) return;
        Memories.append(p, MemoryRules.caseClosed(index, monster, scenario, solved, Memories.gameTime(p), System.currentTimeMillis()));
    }
}
