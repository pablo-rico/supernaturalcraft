package org.papiricoh.supernaturalcraft.client.lucifer;

import org.jetbrains.annotations.Nullable;

/**
 * The bars of the Cage across both Lucifers' boss bars, phase by phase (no Minecraft types: tested on its own). Against
 * Lucifer the Cage closes on him: the bars come down as he loses his phases. Against Lucifer Uncaged it breaks: red-hot,
 * rimed and bent, torn out one after another, until in his last phase there is only light. A bar that is torn never comes
 * back, and one that has come down never lifts.
 */
public final class CageBars {

    /** How many bars cross the bar (the medallion's column, between 3 and 4, is left free). */
    public static final int COUNT = 8;

    /** A bar's look; its ordinal is its column in {@code bar_bars.png}. */
    public enum State {
        STRAIGHT, HOT, RIMED, BENT_LEFT, BENT_RIGHT, SNAPPED, STUMP, CRACKED;

        /** Still a whole bar (not torn). */
        public boolean whole() {
            return this != SNAPPED && this != STUMP;
        }
    }

    private static final State S = State.STRAIGHT, H = State.HOT, R = State.RIMED, L = State.BENT_LEFT, B = State.BENT_RIGHT,
            T = State.STUMP, C = State.CRACKED;

    /** Lucifer, phase 1-4: no Cage, two bars, four rimed with its cold, the whole Cage. */
    private static final State[][] LUCIFER = {
            {null, null, null, null, null, null, null, null},
            {S, null, null, null, null, null, null, S},
            {R, null, R, null, null, R, null, R},
            {S, S, S, S, S, S, S, S},
    };

    /** Lucifer Uncaged, phase 1-6: whole, red-hot, rimed and bent, torn, cracked with gold, gone. */
    private static final State[][] UNCAGED = {
            {S, S, S, S, S, S, S, S},
            {H, H, H, H, H, H, H, H},
            {R, R, L, R, R, B, R, R},
            {S, B, T, S, S, T, L, S},
            {T, C, T, T, C, T, C, T},
            {null, null, null, null, null, null, null, null},
    };

    private CageBars() {
    }

    public static int phases(boolean uncaged) {
        return (uncaged ? UNCAGED : LUCIFER).length;
    }

    /** Bar {@code index} in {@code phase} (clamped to the fight's phases); null where there is no bar. */
    public static @Nullable State state(boolean uncaged, int phase, int index) {
        State[][] table = uncaged ? UNCAGED : LUCIFER;
        if (index < 0 || index >= COUNT) return null;
        return table[Math.max(1, Math.min(table.length, phase)) - 1][index];
    }

    /** How many whole bars stand in {@code phase}. */
    public static int whole(boolean uncaged, int phase) {
        int n = 0;
        for (int i = 0; i < COUNT; i++) {
            State s = state(uncaged, phase, i);
            if (s != null && s.whole()) n++;
        }
        return n;
    }
}
