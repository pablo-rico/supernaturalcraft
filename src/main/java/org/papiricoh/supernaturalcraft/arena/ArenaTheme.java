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

    private ArenaTheme() {
    }

    /** Blocks below the centre still inside the arena. */
    public static int depth(int theme) {
        return theme == CHORUS ? 16 : theme == ABYSS ? 12 : 8;
    }

    /** Blocks above the centre still inside the arena. */
    public static int height(int theme) {
        return theme == CHORUS ? 44 : theme == ABYSS ? 56 : 24;
    }

    /** Whether challengers who fall below the floor are carried back up instead of left to fall. */
    public static boolean rescuesFallers(int theme) {
        return theme == CHORUS || theme == ABYSS;
    }
}
