package org.papiricoh.supernaturalcraft.chain;

import java.util.Locale;

/**
 * A task a Lord sets a hunter of its side at an audience, or a deal a demon strikes with a hunter who parleys (pure; one at a
 * time per hunter, kept in {@link ChainSavedData}). {@code lord}: the chain asking; {@code patron}: the officer to answer to
 * (-1: its Lord); {@code targetFaction}/{@code targetId}: the officer to slay ({@link Kind#SLAY}); {@code count}: the tribute
 * owed ({@link Kind#TRIBUTE}); {@code deal}: a parley's bargain, whose breach the patron never forgets. Done in time it earns
 * the patron's regard ({@link Bonds#TASK_DONE}) and may earn a promotion.
 */
public record ChainTask(Kind kind, ChainFaction lord, ChainFaction targetFaction, int targetId, int count, long untilDay, int patron, boolean deal) {
    public enum Kind {
        /** Slay a named officer of the other side. */
        SLAY,
        /** Fight on the side's part in a skirmish and see it won. */
        SKIRMISH,
        /** Bring the throne its tribute (souls of the dead for the Pit, holy relics for the Host). */
        TRIBUTE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int DAYS = 5;
}
