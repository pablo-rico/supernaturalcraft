package org.papiricoh.supernaturalcraft.entity.ghost;

/**
 * Every number a ghost lives by, and the few rules that need no world to answer (pure: JUnit tests
 * them, and the client and the server agree on them without syncing anything).
 */
public final class GhostBalance {

    /** How far a ghost may stray from its bones before it is pulled back. */
    public static final double LEASH = 32;
    /** Hauntings reach players this close. */
    public static final double HAUNT_RANGE = 10;
    /** Ticks between two hauntings (cold or snuffed lights). */
    public static final int HAUNT_INTERVAL = 70;
    /** Lights snuffed around the haunted player, and how many at most each time. */
    public static final int SNUFF_RADIUS = 6, SNUFF_MAX = 4;
    /** The cold: Slowness I for this long. */
    public static final int COLD_TICKS = 80;

    /** Telekinesis: range band, damage, and the pause before it can throw again. */
    public static final double TELEKINESIS_MIN = 1.5, TELEKINESIS_MAX = 10;
    public static final float TELEKINESIS_DAMAGE = 3f;
    public static final int TELEKINESIS_COOLDOWN = 80;
    public static final double TELEKINESIS_PUSH = 1.4, TELEKINESIS_LIFT = 0.45;

    /** How long it shows itself after lashing out. */
    public static final int MANIFEST_TICKS = 30;
    /** Scattered by iron, salt or anything holy: gone this long, then it gathers again at its bones. */
    public static final int DISPERSE_TICKS = 200;
    /** The fade it plays (dispersing, at dawn, at rest), shared with the animation. */
    public static final int FADE_TICKS = 20;
    public static final float ECTOPLASM_CHANCE = 0.3f;
    /** The Reveal sigil shows it to everyone for this long. */
    public static final int REVEAL_TICKS = 600;
    /** Day must last this long before a ghost gives up and fades back into its grave. */
    public static final int DAWN_GRACE = 40;

    /** Movement: speeds for the move control (Vex-like acceleration), hover height and drift radius. */
    public static final double CHASE_SPEED = 0.45, DRIFT_SPEED = 0.22;
    public static final double HOVER_DISTANCE = 2.2;
    public static final int DRIFT_RADIUS = 7;

    /** Bones raise their ghost when a player is this close at night; checked this often. */
    public static final double RAISE_RANGE = 24;
    public static final int RAISE_CHECK = 40, RAISE_COOLDOWN = 200;

    /** Translucency when seen; flicker windows. */
    public static final float SEEN_ALPHA = 0.7f, FLICKER_ALPHA = 0.45f;
    public static final int FLICKER_SLOT = 50, FLICKER_LENGTH = 5;

    private GhostBalance() {
    }

    /** Night by the clock (13000–23000 of each day), so it holds the very tick the time is set. */
    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 13000 && t < 23000;
    }

    /**
     * Whether a hidden ghost flickers into sight at {@code gameTime}: about one slot in five holds a
     * short window, picked by a hash of the entity id and the slot so ghosts don't blink together.
     */
    public static boolean flickers(int entityId, long gameTime) {
        long slot = Math.floorDiv(gameTime, FLICKER_SLOT);
        long h = mix(entityId * 0x9E3779B97F4A7C15L + slot);
        if (Math.floorMod(h, 5) != 0) return false;
        int start = (int) Math.floorMod(h >>> 8, FLICKER_SLOT - FLICKER_LENGTH);
        long at = Math.floorMod(gameTime, FLICKER_SLOT);
        return at >= start && at < start + FLICKER_LENGTH;
    }

    /** 0 → 1 over a fade that began at {@code since}; 1 once it is over. */
    public static float fadeProgress(long since, long gameTime) {
        return Math.min(1f, Math.max(0f, (gameTime - since) / (float) FADE_TICKS));
    }

    /** Whether a dispersal that began at {@code since} is still holding the ghost apart. */
    public static boolean stillDispersed(long since, long gameTime) {
        return since >= 0 && gameTime - since < DISPERSE_TICKS;
    }

    static long mix(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}
