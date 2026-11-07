package org.papiricoh.supernaturalcraft.nephilim;

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
 * v0.41: hunters from before seasons 12-14 (once per hunter, at login). One who had already beaten the Author gets the new main
 * road as done: its advancements (not the side roads': Ketch, Hess, Asmodeus) and the step {@code DONE}; every new boss can still
 * be called again by its rite. One between Amara and the Author simply walks the new road (the Author waits for it).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class NephilimMigration {

    /** The scene bit that marks a hunter as looked at. */
    public static final int MIGRATED = 1 << 30;

    /** The main road's advancements, in order. */
    public static final List<String> MAIN_ROAD = List.of("main/keep_calm_and_carry_on", "main/trace_the_nephilim",
            BossProgression.Boss.DAGON.advancement, Nephilims.BORN, "main/the_bad_place", BossProgression.Boss.AU_MICHAEL.advancement,
            Nephilims.OFFERED, BossProgression.Boss.MICHAEL_VESSEL.advancement, Nephilims.SOULLESS, BossProgression.Boss.SOULLESS_JACK.advancement,
            "main/into_the_empty", BossProgression.Boss.SHADOW.advancement);

    private NephilimMigration() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) migrate(p);
    }

    /** @return whether {@code p} was given the road as done */
    public static boolean migrate(ServerPlayer p) {
        if (Nephilims.get(p).seen(MIGRATED)) return false;
        Nephilims.update(p, n -> n.withSeen(MIGRATED));
        if (!AuthorWorld.done(p, BossProgression.Boss.CHUCK.advancement)) return false;
        for (String adv : MAIN_ROAD) ChorusRewards.award(p, adv);
        Nephilims.update(p, n -> n.withStep(NephilimRules.Step.DONE));
        return true;
    }
}
