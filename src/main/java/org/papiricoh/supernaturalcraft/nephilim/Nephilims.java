package org.papiricoh.supernaturalcraft.nephilim;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.function.UnaryOperator;

/**
 * v0.41: the server's door to a hunter's {@link Nephilim} (seasons 12-14). Every slice moves the road only through
 * {@link #advance}, which never goes backwards and grants the step's advancement.
 *
 * <p>Scene bits ({@link Nephilim#scenes}): 0-7 the British slice's, 8-15 the Apocalypse World's, 16-30 the core's.
 */
public final class Nephilims {

    /** Advancements granted by code when the road reaches a step (the kills are ordinary kill advancements). */
    public static final String ULTIMATUM = "main/keep_calm_and_carry_on", TRACING = "main/trace_the_nephilim",
            BORN = "main/the_nephilim", OFFERED = "main/stranger_in_a_strange_land", SOULLESS = "main/the_fall_of_the_son";

    private Nephilims() {
    }

    /** What a nephilim's grace, taken in, adds to the well (once per hunter). */
    public static float graceMana(Player player) {
        return get(player).grace() ? NephilimRules.GRACE_MANA : 0f;
    }

    public static Nephilim get(Player player) {
        return player.getData(NephilimRegistry.NEPHILIM);
    }

    public static void set(Player player, Nephilim n) {
        player.setData(NephilimRegistry.NEPHILIM, n);
    }

    public static Nephilim update(Player player, UnaryOperator<Nephilim> f) {
        Nephilim n = f.apply(get(player));
        set(player, n);
        return n;
    }

    public static NephilimRules.Step step(Player player) {
        return get(player).stage();
    }

    /** Moves {@code player} on to {@code step} (never back) and grants that step's advancement, if it has one. */
    public static void advance(ServerPlayer player, NephilimRules.Step step) {
        if (step(player).atLeast(step)) return;
        update(player, n -> n.withStep(step));
        String adv = switch (step) {
            case ULTIMATUM -> ULTIMATUM;
            case TRACING -> TRACING;
            case BORN -> BORN;
            case OFFERED -> OFFERED;
            case SOULLESS -> SOULLESS;
            default -> null;
        };
        if (adv != null) ChorusRewards.award(player, adv);
    }
}
