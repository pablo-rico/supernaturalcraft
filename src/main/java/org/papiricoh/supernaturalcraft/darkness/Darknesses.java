package org.papiricoh.supernaturalcraft.darkness;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.function.UnaryOperator;

/**
 * Season 11's state per hunter (v0.42): the server's door to a hunter's {@link Darkness}. The road moves only through
 * {@link #advance}, which never goes backwards and grants the step's advancement. Other slices call only the hooks marked as
 * theirs ({@link #handClaimed}, {@link #huntsHands}).
 *
 * <p>Scene bits ({@link Darkness#scenes}): 0-15 the core's, 30 = migrated ({@code DarknessMigration}).
 */
public final class Darknesses {

    /** Advancements granted by code when the road reaches a step (Amara's {@code main/dawn} is her kill advancement). */
    public static final String RELEASED = "main/the_darkness", SUPERIOR = "main/form_and_void",
            CAGE_VISITED = "main/o_brother_where_art_thou", CASIFER = "main/the_devil_in_the_details",
            FIRST_HAND = "main/hand_of_god", REVEALED = "main/dont_call_me_shurley", TAKEN = "main/we_happy_few";

    private Darknesses() {
    }

    /** The world's day number (whole days of the calendar {@link WorldClock} keeps, which counts ticks). */
    public static long today(net.minecraft.server.MinecraftServer server) {
        return WorldClock.day(server) / 24000L;
    }

    public static Darkness get(Player player) {
        return player.getData(DarknessRegistry.DARKNESS);
    }

    public static void set(Player player, Darkness d) {
        player.setData(DarknessRegistry.DARKNESS, d);
    }

    public static Darkness update(Player player, UnaryOperator<Darkness> f) {
        Darkness d = f.apply(get(player));
        set(player, d);
        return d;
    }

    public static DarknessRules.Step step(Player player) {
        return get(player).stage();
    }

    /** The advancement a step grants when reached, or null. */
    public static String advancementOf(DarknessRules.Step step) {
        return switch (step) {
            case RELEASED -> RELEASED;
            case SUPERIOR -> SUPERIOR;
            case CAGE_VISITED -> CAGE_VISITED;
            case CASIFER -> CASIFER;
            case REVEALED -> REVEALED;
            case TAKEN -> TAKEN;
            default -> null;
        };
    }

    /** Moves {@code player} on to {@code step} (never back), dated today, and grants that step's advancement. @return whether it moved */
    public static boolean advance(ServerPlayer player, DarknessRules.Step step) {
        if (step(player).atLeast(step)) return false;
        long day = today(player.server);
        update(player, d -> d.withStep(step, day));
        String adv = advancementOf(step);
        if (adv != null) ChorusRewards.award(player, adv);
        return true;
    }

    /**
     * Hook for the Hands slice: {@code player} has just taken {@code hand} from its reliquary's altar (once per hunter and kind).
     * Records it (the claimed mask) and grants the first Hand's advancement; Chuck comes at dawn once enough are held.
     */
    public static void handClaimed(ServerPlayer player, S11Assets.Hand hand) {
        Darkness d = update(player, x -> x.withHand(hand));
        if (d.handCount() == 1) ChorusRewards.award(player, FIRST_HAND);
    }

    /** Hook for the Hands slice: whether {@code player} may claim Hands yet (the Darkness is out for them, not yet at peace). */
    public static boolean huntsHands(ServerPlayer player) {
        return DarknessRules.huntsHands(step(player));
    }
}
