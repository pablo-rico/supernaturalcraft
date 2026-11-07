package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.function.UnaryOperator;

/**
 * Reads and writes a hunter's {@link Pact} (v0.39) and moves them along season 6's road ({@link PactRules}). Server side is the
 * truth. The purgatory package calls {@link #huntingAlpha} (may this Alpha come for them tonight?) and {@link #alphaSlain}
 * (one fell to them).
 */
public final class Pacts {

    /** Advancements of each step (granted by code). */
    public static final String CALLED = "main/castiels_call", MET_CROWLEY = "main/king_of_hell", ALPHAS = "main/the_alphas",
            FORGIVEN = "main/castiel_forgiven", CONDEMNED = "main/castiel_condemned";

    /** Max mana a Shard of Castiel's Grace adds, once. */
    public static final float GRACE_MANA = 25f;

    private Pacts() {
    }

    /** The max mana the pact gives {@code player} (a Shard of Castiel's Grace taken in). */
    public static float graceMana(Player player) {
        return get(player).grace() ? GRACE_MANA : 0f;
    }

    public static Pact get(Player player) {
        return player.getData(AllAttachments.PACT);
    }

    public static void set(Player player, Pact p) {
        player.setData(AllAttachments.PACT, p);
    }

    public static Pact update(Player player, UnaryOperator<Pact> f) {
        Pact p = f.apply(get(player));
        set(player, p);
        return p;
    }

    public static PactRules.Step step(Player player) {
        return get(player).stage();
    }

    /** Moves {@code player} to {@code step} (never back) and grants its advancement. */
    public static void advance(ServerPlayer player, PactRules.Step step) {
        if (step(player).atLeast(step) && step != PactRules.Step.FORGIVEN && step != PactRules.Step.CONDEMNED) return;
        if (step(player).decided()) return;
        update(player, p -> p.withStep(step));
        String adv = switch (step) {
            case CALLED -> CALLED;
            case MET_CROWLEY -> MET_CROWLEY;
            case RITE_READY -> ALPHAS;
            case FORGIVEN -> FORGIVEN;
            case CONDEMNED -> CONDEMNED;
            default -> null;
        };
        if (adv != null) ChorusRewards.award(player, adv);
    }

    /** Whether Alpha {@code a} may come for {@code player} (they are on Crowley's errand and have not brought it down yet). */
    public static boolean huntingAlpha(Player player, PactRules.Alpha a) {
        Pact p = get(player);
        return PactRules.hunts(p.stage(), p.alphas(), a);
    }

    /** Alpha {@code a} fell to {@code player} (or near them, in a fight they were part of). */
    public static void alphaSlain(ServerPlayer player, PactRules.Alpha a) {
        Pact p = get(player);
        if (p.stage() != PactRules.Step.HUNTING || PactRules.has(p.alphas(), a)) return;
        int mask = PactRules.with(p.alphas(), a);
        update(player, q -> q.withAlphas(mask));
        player.sendSystemMessage(Component.translatable("message.supernaturalcraft.pact.alpha_slain",
                Component.translatable("entity.supernaturalcraft." + a.entity()), PactRules.count(mask), PactRules.Alpha.values().length)
                .withStyle(ChatFormatting.DARK_RED));
        PactRules.Step next = PactRules.afterAlpha(PactRules.Step.HUNTING, mask);
        if (next != PactRules.Step.HUNTING) {
            advance(player, next);
            PactServer.riteReady(player);
        }
    }
}
