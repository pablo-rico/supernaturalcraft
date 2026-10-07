package org.papiricoh.supernaturalcraft.reward;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;

/** Advancements the Broken Chorus grants from code (their criteria are impossible otherwise). */
public final class ChorusRewards {

    private ChorusRewards() {
    }

    /** Broke a Hymn with the right bell. */
    public static void hymnBroken(ServerPlayer player) {
        award(player, "main/silence");
    }

    static void award(ServerPlayer player, String id) {
        if (player.getServer() == null) return;
        AdvancementHolder adv = player.getServer().getAdvancements().get(SupernaturalCraft.asResource(id));
        if (adv == null) return;
        for (String criterion : adv.value().criteria().keySet()) player.getAdvancements().award(adv, criterion);
    }
}
