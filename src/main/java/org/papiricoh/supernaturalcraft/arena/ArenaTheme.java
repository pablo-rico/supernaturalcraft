package org.papiricoh.supernaturalcraft.arena;

/**
 * Which fight an arena holds, and the shape that fight needs. Shared by both sides: the server
 * uses it to bound the arena, the client to draw the dome.
 */
public final class ArenaTheme {

    /** Lucifer's Cage. */
    public static final int CAGE = 0;
    /** Amara's darkness. */
    public static final int DARKNESS = 1;
    /** The Broken Chorus, high on the Hymnal Spire: it flies, and the floor gives way. */
    public static final int CHORUS = 2;
    /** Lucifer Uncaged, on the island under the Cage in the Pit: the floor falls away into the abyss. */
    public static final int ABYSS = 3;
    /** Azazel, wherever he was called up: Samuel Colt's rails at the centre. */
    public static final int SULFUR = 4;
    /** Lilith, wherever she was called up: headstones to hide behind from her light. */
    public static final int SEAL = 5;
    /** Metatron's library of Heaven, raised wherever he was called down. */
    public static final int SCRIPTORIUM = 6;
    /** Chuck, the Author: an arena he rewrites chapter by chapter, from Eden down to the blank page. */
    public static final int AUTHOR = 7;
    /** War's battlefield: trenches, barbed fences and his standards. */
    public static final int WAR = 8;
    /** Famine's dead farmland: rotten crops and a scarecrow. */
    public static final int FAMINE = 9;
    /** Pestilence's toxic swamp, where the antidote turns up. */
    public static final int PLAGUE = 10;
    /** Death's arena: a living world that turns grey when the world of the dead takes it. */
    public static final int DEATH = 11;
    /** Michael's Heaven: three arenas in turn (the Garden, the War in Heaven, the Throne Room), with room to fly. */
    public static final int HEAVEN = 12;
    /** Gabriel's TV Land (v0.14): four sets in turn on one footprint (sitcom, game show, hospital, commercial). */
    public static final int TV_LAND = 13;
    /** Raphael's abandoned house in a thunderstorm (v0.16): written round the rite, its roof torn off in the last phase. */
    public static final int STORM = 14;
    /** Naomi's reprogramming room (v0.18), in the clinical wing of a hunter's own Heaven: a floating island, so fallers are carried back. */
    public static final int REPROGRAMMING = 15;
    /** Zachariah's endless office (v0.18), high over a hunter's Heaven: low ceilings, fallers carried back. */
    public static final int OFFICE = 16;
    /** A memory staged in a hunter's Heaven (v0.18): written for a visit and restored after, no fight and no dome. */
    public static final int MEMORY = 17;

    private ArenaTheme() {
    }

    /** Blocks below the centre still inside the arena. */
    public static int depth(int theme) {
        return theme == CHORUS ? 16 : theme == ABYSS || theme == AUTHOR || theme == HEAVEN ? 12 : 8;
    }

    /** Blocks above the centre still inside the arena. */
    public static int height(int theme) {
        return theme == CHORUS ? 44 : theme == ABYSS ? 56 : theme == SCRIPTORIUM || theme == DEATH ? 28 : theme == AUTHOR ? 48 : theme == HEAVEN ? 40
                : theme == OFFICE ? 16 : 24;
    }

    /** Whether challengers who fall below the floor are carried back up instead of left to fall. */
    public static boolean rescuesFallers(int theme) {
        return theme == CHORUS || theme == ABYSS || theme == AUTHOR || theme == HEAVEN || theme == REPROGRAMMING || theme == OFFICE;
    }

    /**
     * The fewest blocks the arena must be able to remember, whatever the config says: the Author erases a forest
     * and writes five arenas over it; a Horseman lays his own ground (Death turns it grey and back); Michael writes three
     * Heavens in turn. 0 for every other fight (the config alone decides).
     */
    public static int minSnapshot(int theme) {
        return theme == AUTHOR ? 90_000 : theme == HEAVEN || theme == OFFICE ? 60_000 : theme == TV_LAND ? 40_000
                : theme == STORM || theme == REPROGRAMMING ? 30_000 : theme == MEMORY ? 20_000 : theme >= WAR && theme <= DEATH ? 20_000 : 0;
    }
}
