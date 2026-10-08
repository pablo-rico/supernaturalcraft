package org.papiricoh.supernaturalcraft.entity.boss.michael;

/**
 * What Michael says, and when (pure): one line as each phase begins, the odd line in between. Once his challengers have
 * put Lucifer back in the box ({@code main/back_in_the_box}) some lines change: he speaks of his brother.
 * Keys are {@code message.supernaturalcraft.michael.quote.<n>} and {@code ...quote.uncaged.<n>} (MichaelLang).
 */
public final class MichaelQuotes {

    /** Lines of each set (MichaelLang.QUOTES, QUOTES_AFTER_UNCAGED). */
    public static final int LINES = 8, UNCAGED_LINES = 4;
    /** Ticks between lines he says unprompted. */
    public static final int GAP = 1200;

    private MichaelQuotes() {
    }

    /** The line as phase {@code phase} begins (1 at his arrival). */
    public static String onPhase(int phase, boolean uncaged) {
        if (uncaged) {
            // His brother is back in the Cage: the Lance and the Archangel speak of him.
            if (phase == 3) return uncaged(0);
            if (phase == 5) return uncaged(3);
        }
        return regular(Math.max(0, Math.min(LINES - 1, phase - 1)));
    }

    /** A line between phases, from {@code roll} (any non-negative number). */
    public static String any(int roll, boolean uncaged) {
        if (uncaged && roll % 3 == 0) return uncaged((roll / 3) % UNCAGED_LINES);
        return regular(roll % LINES);
    }

    public static String regular(int n) {
        return "message.supernaturalcraft.michael.quote." + n;
    }

    public static String uncaged(int n) {
        return "message.supernaturalcraft.michael.quote.uncaged." + n;
    }
}
