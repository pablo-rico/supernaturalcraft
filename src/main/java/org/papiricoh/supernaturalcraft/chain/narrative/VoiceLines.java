package org.papiricoh.supernaturalcraft.chain.narrative;

import org.papiricoh.supernaturalcraft.chain.Temperament;

/**
 * How many lines each voice has written per situation (pure; filled in by the narrative pass, which writes the lines
 * themselves in {@code datagen/lang/ChainNarrativeLang} and {@code es_es.json}). {@code temperament} null: the lines any
 * officer of the voice may say, of which every voice has at least one per situation ({@code ChainNarrativeTest}).
 */
public final class VoiceLines {
    private VoiceLines() {
    }

    public static int count(Voice voice, Situation situation, Temperament temperament) {
        boolean world = switch (situation) {
            case SIGHT_FOE, SIGHT_AGAIN, SIGHT_ALLY, KILL_HUNTER, FLEE, RETURN_SCARRED,
                    CHALLENGE_ISSUED, DUEL_START, DUEL_WON, DUEL_LOST, YIELD, FEUD_TAUNT,
                    SKIRMISH_CRY, VOW -> true;
            default -> false;
        };
        if (temperament == null) return world ? 5 : 3;
        return world || switch (situation) {
            case GREET_ALLY, GREET_PARLEY, PARLEY_REFUSE, AUDIENCE_GREET, COWARD -> true;
            default -> false;
        } ? 1 : 0;
    }
}
