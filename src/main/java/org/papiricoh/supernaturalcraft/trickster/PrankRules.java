package org.papiricoh.supernaturalcraft.trickster;

import java.util.Random;

/**
 * When and how the Trickster plays his pranks (pure, v0.14). After Lucifer falls, at most one prank a world day per hunter,
 * at a random moment of it; none of them break anything, take anything or hurt anyone. The third one noticed puts the hunter
 * on his trail (advancement {@code main/trickster_sighted}: the bait's rite and the journal's page).
 */
public final class PrankRules {

    /** Pranks noticed before the bait can be made. */
    public static final int SIGHTINGS_NEEDED = 3;
    /** One chance in this many, each time the pranks are looked at ({@link #CHECK_EVERY}), that today's comes now. */
    public static final int CHANCE = 30;
    /** Ticks between looks at the pranks. */
    public static final int CHECK_EVERY = 200;
    /** How long a party hat stays on a mob. */
    public static final int HAT_TICKS = 6000;
    /** How far from the hunter a prank may be played (a mob, a chest, a villager). */
    public static final int RADIUS = 12;

    /** The harmless pranks. */
    public enum Prank {
        /** A candy wrapper by the hunter's feet (or their bed). */
        CANDY_WRAPPER,
        /** A party hat on a nearby mob. */
        PARTY_HAT,
        /** A laugh track, somewhere out of sight. */
        LAUGH_TRACK,
        /** A villager says a line from a TV show. */
        TV_LINE,
        /** A nearby chest opens and shuts on its own (nothing in it moves). */
        CHEST;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private PrankRules() {
    }

    /** Whether a prank may be played on a hunter today: Lucifer beaten and none yet this world day. */
    public static boolean due(boolean luciferBeaten, TricksterLedger ledger, long day) {
        return luciferBeaten && ledger.lastPrankDay() != day;
    }

    /** Whether, a prank being due, it comes on this look. */
    public static boolean now(Random random) {
        return random.nextInt(CHANCE) == 0;
    }

    /**
     * Which prank to play, given what is around ({@code mob}, {@code villager}, {@code chest} within {@link #RADIUS}): the
     * wrapper and the laugh track always work, the others only with something to play them on.
     */
    public static Prank pick(Random random, boolean mob, boolean villager, boolean chest) {
        Prank[] all = Prank.values();
        for (int tries = 0; tries < 16; tries++) {
            Prank p = all[random.nextInt(all.length)];
            if (p == Prank.PARTY_HAT && !mob || p == Prank.TV_LINE && !villager || p == Prank.CHEST && !chest) continue;
            return p;
        }
        return Prank.CANDY_WRAPPER;
    }

    public static long day(long dayTime) {
        return Math.floorDiv(dayTime, 24000L);
    }
}
