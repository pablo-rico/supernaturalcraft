package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.List;

/**
 * v0.42: hunters from before season 11's road (once per hunter, at login). One who had already beaten Amara gets the new road
 * before her as done: the Mark's chain that became the main road (Cain, Abaddon, the witch's step through the Mark taken off)
 * and every story step, so nothing they had is taken away. The world's dying sun never starts for an old world either
 * ({@code DyingSun.migrate}). Every boss can still be called again by its rite.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DarknessMigration {

    /** The scene bit that marks a hunter as looked at. */
    public static final int MIGRATED = 1 << 30;

    /** What a hunter past Amara is given, in order. */
    public static final List<String> ROAD = List.of(BossProgression.Boss.CAIN.advancement, "main/marked",
            BossProgression.Boss.ABADDON.advancement, BossProgression.UNMARKED,
            Darknesses.RELEASED, Darknesses.SUPERIOR, Darknesses.CAGE_VISITED, Darknesses.CASIFER, Darknesses.FIRST_HAND,
            Darknesses.REVEALED, Darknesses.TAKEN);

    private DarknessMigration() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) migrate(p);
    }

    /** @return whether {@code p} was given the road as done */
    public static boolean migrate(ServerPlayer p) {
        if (Darknesses.get(p).seen(MIGRATED)) return false;
        Darknesses.update(p, d -> d.withSeen(MIGRATED));
        if (!AuthorWorld.done(p, BossProgression.Boss.AMARA.advancement)) return false;
        for (String adv : ROAD) ChorusRewards.award(p, adv);
        Darknesses.update(p, d -> d.withStep(DarknessRules.Step.DONE, 0L));
        org.papiricoh.supernaturalcraft.dyingsun.DyingSun.restoreQuietly(p.server);
        return true;
    }
}
