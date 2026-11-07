package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ruby's road (v0.40, pure, tested in JUnit by {@code RubyRulesTest}): the steps a hunter goes through with her, when she
 * comes, and her talks. Lines are {@code dialogue.supernaturalcraft.ruby.<node>} and answers {@code ruby.<node>.<option>} (the
 * shared {@code AllegianceDialogue}); silence is always the last option, and the safe one.
 * <ul>
 *   <li>{@code meet} (or {@code meet_angel}/{@code meet_demon}: the same talk in the words she has for each side; listen |
 *   leave) → {@code offer} (take | refuse): either way she gives the warehouse's compass (and, taken, her flask of blood).</li>
 *   <li>{@code lilith} (once Alastair is dead; how | why) → {@code how_reply} | {@code why_reply} (go): Lilith's rite, the seals.</li>
 *   <li>When Lilith falls with the hunter there, Ruby shows what she is and the fight begins ({@link Step#BETRAYAL_PENDING});
 *   if the hunter lost or fled it, she comes for them by night. Put down with a demon-killing blade: {@link Step#DONE}.</li>
 * </ul>
 */
public final class RubyRules {

    /** Saved by ordinal: append only. */
    public enum Step {
        /** Not met (or she was sent away: she comes back the next dawn). */
        NONE,
        /** Heard out, the offer still to come (a talk cut short). */
        MET,
        /** Sent to the warehouse, to Alastair. */
        ALASTAIR,
        /** Told the way to Lilith. */
        LILITHS_WAY,
        /** Lilith fell: Ruby has shown what she is, and is still to be put down. */
        BETRAYAL_PENDING,
        /** Ruby is dead (or the hunter's world was past her before v0.40). */
        DONE;

        public static Step of(int ordinal) {
            Step[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NONE;
        }
    }

    /** Days between two of her visits. */
    public static final int VISIT_REST_DAYS = 1;

    public static final String DIALOGUE = "ruby";

    /** A line and its answers. */
    public record Line(String node, List<String> options) {
    }

    /** What an answer does on the server. */
    public enum Act {
        NONE, LEAVE, TAKE_FLASK, NO_FLASK, LILITHS_WAY
    }

    public record Turn(@Nullable Line next, Act act) {
    }

    private static final List<String> GO = List.of("go");
    public static final Line MEET = new Line("meet", List.of("listen", "leave"));
    public static final Line MEET_ANGEL = new Line("meet_angel", List.of("listen", "leave"));
    public static final Line MEET_DEMON = new Line("meet_demon", List.of("listen", "leave"));
    public static final Line OFFER = new Line("offer", List.of("take", "refuse"));
    public static final Line LILITH = new Line("lilith", List.of("how", "why"));
    public static final Line HOW = new Line("how_reply", GO), WHY = new Line("why_reply", GO);

    private RubyRules() {
    }

    /** The hunter's side, as the opening line cares ({@code human}, {@code angel}, {@code demon}). */
    public static Line meet(String side) {
        return switch (side) {
            case "angel" -> MEET_ANGEL;
            case "demon" -> MEET_DEMON;
            default -> MEET;
        };
    }

    /**
     * What Ruby comes to say, or null (she does not come). {@code azazel}/{@code alastair}/{@code lilith}: those advancements.
     */
    public static @Nullable Line opens(Step step, String side, boolean azazel, boolean alastair, boolean lilith) {
        if (!azazel || lilith) return null;
        return switch (step) {
            case NONE -> meet(side);
            case MET -> OFFER;
            case ALASTAIR -> alastair ? LILITH : null;
            default -> null;
        };
    }

    public static Turn answer(String node, String option) {
        if (node.startsWith("meet")) return "listen".equals(option) ? new Turn(OFFER, Act.NONE) : new Turn(null, Act.LEAVE);
        return switch (node) {
            case "offer" -> new Turn(null, "take".equals(option) ? Act.TAKE_FLASK : Act.NO_FLASK);
            case "lilith" -> new Turn("how".equals(option) ? HOW : WHY, Act.LILITHS_WAY);
            default -> new Turn(null, Act.NONE);
        };
    }

    /** Whether she is due on {@code day} after her last visit on {@code lastVisit}. */
    public static boolean visitDue(long day, long lastVisit) {
        return day >= lastVisit + VISIT_REST_DAYS;
    }

    /** Whether a hunter at {@code step} is still owed the reveal when Lilith falls with them there. */
    public static boolean owedReveal(Step step) {
        return step != Step.DONE;
    }

    /** Every node with its answers (lang and tests). */
    public static List<Line> all() {
        return List.of(MEET, MEET_ANGEL, MEET_DEMON, OFFER, LILITH, HOW, WHY);
    }
}
