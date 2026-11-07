package org.papiricoh.supernaturalcraft.british;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Dr. Hess's ultimatum (v0.41), pure: what she says at dawn, what the hunter can answer and what an answer does. Lines are
 * {@code dialogue.supernaturalcraft.british_ultimatum.<node>} and answers {@code <node>.<option>} (the shared
 * {@code AllegianceDialogue}); silence is always the last option, and here it is the refusal. Tested in
 * {@code BritishDialogueTest}.
 *
 * <ul>
 *   <li>{@code offer} (a hunter) or {@code offer_letters} (one of the American Men of Letters: their bunker is mentioned):
 *   terms | refuse.</li>
 *   <li>{@code terms}: join | refuse. Joining takes the kit; refusing (or silence) sets Ketch on them.</li>
 *   <li>Either way she lets slip what London is really after: {@code slip_joined} | {@code slip_refused} (go), the nephilim.</li>
 * </ul>
 */
public final class BritishDialogue {

    public static final String ULTIMATUM = "british_ultimatum";

    /** A line and its answers. */
    public record Line(String node, List<String> options) {
    }

    /** What an answer does on the server. */
    public enum Act {
        NONE, JOIN, REFUSE, DONE
    }

    /** The next line (null: the talk is over) and the deed. */
    public record Turn(@Nullable Line next, Act act) {
    }

    private static final List<String> GO = List.of("go");

    public static final Line OFFER = new Line("offer", List.of("terms", "refuse"));
    public static final Line OFFER_LETTERS = new Line("offer_letters", List.of("terms", "refuse"));
    public static final Line TERMS = new Line("terms", List.of("join", "refuse"));
    public static final Line SLIP_JOINED = new Line("slip_joined", GO);
    public static final Line SLIP_REFUSED = new Line("slip_refused", GO);

    private BritishDialogue() {
    }

    /** What Hess opens with, or null (she has nothing to say: the hunter has had the ultimatum). */
    public static @Nullable Line opens(BritishRules.Choice choice, boolean letters) {
        if (choice != BritishRules.Choice.NONE) return null;
        return letters ? OFFER_LETTERS : OFFER;
    }

    public static Turn answer(String node, String option) {
        return switch (node) {
            case "offer", "offer_letters" -> "terms".equals(option) ? new Turn(TERMS, Act.NONE) : new Turn(SLIP_REFUSED, Act.REFUSE);
            case "terms" -> "join".equals(option) ? new Turn(SLIP_JOINED, Act.JOIN) : new Turn(SLIP_REFUSED, Act.REFUSE);
            case "slip_joined", "slip_refused" -> new Turn(null, Act.DONE);
            default -> new Turn(null, Act.NONE);
        };
    }

    /** The option silence stands for at {@code line}: its last. */
    public static String silence(Line line) {
        return line.options().getLast();
    }

    /** Every node with its answers (lang and tests). */
    public static List<Line> all() {
        return List.of(OFFER, OFFER_LETTERS, TERMS, SLIP_JOINED, SLIP_REFUSED);
    }
}
