package org.papiricoh.supernaturalcraft.legacy;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What Henry Winchester says, and what a hunter can answer (v0.17), pure. The server opens a stage with
 * {@code LegacyFxPayload.HENRY} ({@code value} = {@link Stage#ordinal()}, or {@link #CLOSED} to close the screen; {@code entity} =
 * Henry); the client draws its lines ({@link #line}, {@link Stage#lines}) and its answers ({@link Stage#choices}, labels
 * {@link #choiceKey}) and replies with {@code LegacyChoicePayload}. The server checks the answer with {@link #next}.
 *
 * <p>Answers are the {@code LegacyChoicePayload} bytes: {@link #ACCEPT}, {@link #DECLINE}, {@link #NEW_CASE}, {@link #CLOSE}.
 */
public final class HenryDialogue {

    public static final byte ACCEPT = 0, DECLINE = 1, NEW_CASE = 2, CLOSE = 3;
    /** {@code LegacyFxPayload.HENRY} with this value closes Henry's screen. */
    public static final int CLOSED = -1;
    /** Lang key prefix: {@code legacy.supernaturalcraft.henry.<stage>.<n>} and {@code legacy.supernaturalcraft.henry.choice.<choice>}. */
    public static final String PREFIX = "legacy.supernaturalcraft.henry.";
    private static final String[] CHOICE_IDS = {"accept", "decline", "new_case", "close"};

    /** The stages of the talk; {@code lines} = how many lines it has (keys {@code .1} … {@code .lines}). */
    public enum Stage {
        /** His first call, at dawn after Lucifer fell: who he is, what the Men of Letters were, the offer. */
        OFFER(5, ACCEPT, DECLINE),
        /** He calls again after being turned away. */
        OFFER_AGAIN(3, ACCEPT, DECLINE),
        /** Accepted: the key, the map, where the bunker is. */
        WELCOME(4, CLOSE),
        /** Turned away: he tips his hat and goes. */
        FAREWELL(2, CLOSE),
        /** A member, in the war room, with no case under way: he offers one. */
        WAR_ROOM(3, NEW_CASE, CLOSE),
        /** A case is still open: get to it. */
        CASE_OPEN(2, CLOSE),
        /** He hands a new case over (the file and its map). */
        CASE_GIVEN(3, CLOSE),
        /** The last case was solved: well done, another? */
        CASE_SOLVED(3, NEW_CASE, CLOSE),
        /** The last case went cold (lost): no shame, another? */
        CASE_LOST(2, NEW_CASE, CLOSE);

        public final int lines;
        private final byte[] choices;

        Stage(int lines, byte... choices) {
            this.lines = lines;
            this.choices = choices;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        /** The answers this stage offers, in order. */
        public List<Byte> choices() {
            Byte[] out = new Byte[choices.length];
            for (int i = 0; i < choices.length; i++) out[i] = choices[i];
            return List.of(out);
        }

        public boolean offers(byte choice) {
            for (byte c : choices) if (c == choice) return true;
            return false;
        }

        public static @Nullable Stage of(int ordinal) {
            Stage[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
        }
    }

    /**
     * What Henry knows of the hunter in front of him.
     *
     * @param member already one of the order
     * @param declined turned him away before
     * @param openCase has a case handed out and not closed
     * @param lastCase the state of the hunter's latest case ({@code CaseFile} state), or -1 if none
     */
    public record Context(boolean member, boolean declined, boolean openCase, int lastCase) {
    }

    private HenryDialogue() {
    }

    /** The lang key of line {@code n} (1-based) of {@code stage}. */
    public static String line(Stage stage, int n) {
        return PREFIX + stage.id() + "." + n;
    }

    /** The lang key of an answer's label. */
    public static String choiceKey(byte choice) {
        return PREFIX + "choice." + CHOICE_IDS[Math.max(0, Math.min(CHOICE_IDS.length - 1, choice))];
    }

    /** Where a talk with Henry starts. */
    public static Stage start(Context ctx) {
        if (!ctx.member()) return ctx.declined() ? Stage.OFFER_AGAIN : Stage.OFFER;
        if (ctx.openCase()) return Stage.CASE_OPEN;
        if (ctx.lastCase() == 2) return Stage.CASE_SOLVED;
        if (ctx.lastCase() == 3) return Stage.CASE_LOST;
        return Stage.WAR_ROOM;
    }

    /**
     * Where an answer leads.
     *
     * @return the next stage, {@code null} if the talk ends (the screen closes), or the same stage if the answer is not one
     * {@code stage} offers (refused: nothing happens)
     */
    public static @Nullable Stage next(Stage stage, byte choice) {
        if (!stage.offers(choice)) return stage;
        return switch (choice) {
            case ACCEPT -> Stage.WELCOME;
            case DECLINE -> Stage.FAREWELL;
            case NEW_CASE -> Stage.CASE_GIVEN;
            default -> null;
        };
    }
}
