package org.papiricoh.supernaturalcraft.pact;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Season 6's talks (v0.39), pure: what Castiel and Crowley say at each step of the pact, what the hunter can answer, and what an
 * answer does. Lines are {@code dialogue.supernaturalcraft.<dialogue>.<node>} and answers {@code <dialogue>.<node>.<option>}
 * (the shared {@code AllegianceDialogue}); silence is always the last option. Tested in {@code PactDialogueTest}.
 *
 * <ul>
 *   <li>{@link #CASTIEL}: {@code call} (after Raphael: listen | refuse) → {@code plan} (help | refuse); {@code doubt} (on the
 *   errand, an Alpha down: trust | question) → {@code trust_reply} | {@code question_reply}; {@code ready} (the lab stands:
 *   promise | warn) → {@code promise_reply} | {@code warn_reply}. Every reply node ends with {@code go}.</li>
 *   <li>{@link #CROWLEY}: {@code meet} (listen | leave) → {@code deal} (deal | refuse); {@code progress} (on the errand: go);
 *   {@code lab} (all three down: go, and he hands over the lab).</li>
 *   <li>{@link #FATE}: {@code fate} (forgive | condemn) when Godstiel kneels.</li>
 * </ul>
 */
public final class PactDialogue {

    public static final String CASTIEL = "castiel_pact", CROWLEY = "crowley_king", FATE = "godstiel_fate";

    /** Talks had once ({@link Pact#scenes}): Castiel's doubts, Castiel's last word before the door, Crowley's lab. */
    public static final int SCENE_DOUBT = 1, SCENE_READY = 2, SCENE_LAB = 4;

    /** A line and its answers. */
    public record Line(String node, List<String> options) {
    }

    /** What an answer does on the server. */
    public enum Act {
        NONE, CASTIEL_HELP, CASTIEL_REFUSE, CROWLEY_DEAL, CROWLEY_LEAVE, CROWLEY_LAB, SCENE_DONE, FORGIVE, CONDEMN
    }

    /** The next line (null: the talk is over) and the deed. */
    public record Turn(@Nullable Line next, Act act) {
    }

    private static final List<String> GO = List.of("go");

    public static final Line CALL = new Line("call", List.of("listen", "refuse"));
    public static final Line PLAN = new Line("plan", List.of("help", "refuse"));
    public static final Line DOUBT = new Line("doubt", List.of("trust", "question"));
    public static final Line READY = new Line("ready", List.of("promise", "warn"));
    public static final Line MEET = new Line("meet", List.of("listen", "leave"));
    public static final Line DEAL = new Line("deal", List.of("deal", "refuse"));
    public static final Line PROGRESS = new Line("progress", GO);
    public static final Line LAB = new Line("lab", GO);
    public static final Line FATE_LINE = new Line("fate", List.of("forgive", "condemn"));

    private PactDialogue() {
    }

    /** What Castiel comes to say to a hunter at {@code step}, or null (he has nothing to say). */
    public static @Nullable Line castielOpens(PactRules.Step step, int alphas, int scenes) {
        return switch (step) {
            case NONE -> CALL;
            case HUNTING -> PactRules.count(alphas) >= 1 && (scenes & SCENE_DOUBT) == 0 ? DOUBT : null;
            case RITE_READY -> (scenes & SCENE_LAB) != 0 && (scenes & SCENE_READY) == 0 ? READY : null;
            default -> null;
        };
    }

    public static Turn castiel(String node, String option) {
        return switch (node) {
            case "call" -> "listen".equals(option) ? new Turn(PLAN, Act.NONE) : new Turn(null, Act.CASTIEL_REFUSE);
            case "plan" -> new Turn(null, "help".equals(option) ? Act.CASTIEL_HELP : Act.CASTIEL_REFUSE);
            case "doubt" -> new Turn(new Line("trust".equals(option) ? "trust_reply" : "question_reply", GO), Act.SCENE_DONE);
            case "ready" -> new Turn(new Line("promise".equals(option) ? "promise_reply" : "warn_reply", GO), Act.SCENE_DONE);
            default -> new Turn(null, Act.NONE);
        };
    }

    /** What Crowley says to a hunter at {@code step} when he comes, or null. */
    public static @Nullable Line crowleyOpens(PactRules.Step step, int scenes) {
        return switch (step) {
            case CALLED, MET_CROWLEY -> MEET;
            case HUNTING -> PROGRESS;
            case RITE_READY -> (scenes & SCENE_LAB) == 0 ? LAB : null;
            default -> null;
        };
    }

    public static Turn crowley(String node, String option) {
        return switch (node) {
            case "meet" -> "listen".equals(option) ? new Turn(DEAL, Act.NONE) : new Turn(null, Act.CROWLEY_LEAVE);
            case "deal" -> new Turn(null, "deal".equals(option) ? Act.CROWLEY_DEAL : Act.CROWLEY_LEAVE);
            case "lab" -> new Turn(null, Act.CROWLEY_LAB);
            default -> new Turn(null, Act.NONE);
        };
    }

    /** The hunter's word over the kneeling Godstiel (silence: condemn). */
    public static Act fate(String option) {
        return "forgive".equals(option) ? Act.FORGIVE : Act.CONDEMN;
    }

    /** Every node of {@code dialogue} with its answers (lang and tests). */
    public static List<Line> all(String dialogue) {
        return switch (dialogue) {
            case CASTIEL -> List.of(CALL, PLAN, DOUBT, new Line("trust_reply", GO), new Line("question_reply", GO), READY,
                    new Line("promise_reply", GO), new Line("warn_reply", GO));
            case CROWLEY -> List.of(MEET, DEAL, PROGRESS, LAB);
            case FATE -> List.of(FATE_LINE);
            default -> List.of();
        };
    }
}
