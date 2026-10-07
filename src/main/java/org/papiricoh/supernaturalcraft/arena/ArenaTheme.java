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

    private ArenaTheme() {
    }

    /** Blocks below the centre still inside the arena. */
    public static int depth(int theme) {
        return theme == CHORUS ? 16 : 8;
    }

    /** Blocks above the centre still inside the arena. */
    public static int height(int theme) {
        return theme == CHORUS ? 44 : 24;
    }

    /** Whether challengers who fall below the floor are carried back up instead of left to fall. */
    public static boolean rescuesFallers(int theme) {
        return theme == CHORUS;
    }
}
