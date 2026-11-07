package org.papiricoh.supernaturalcraft.entity.boss.alastair;

/**
 * Alastair's numbers (v0.40, pure, tested by {@code AlastairBalanceTest}). Three phases of a third of his health each:
 * <ol>
 *   <li><b>The interrogation</b>: chained in the angels' devil's trap. Only holy things truly hurt him ({@link #BOUND_MUNDANE}
 *   of anything else); he lashes the floor round the trap with his chains and taunts.</li>
 *   <li><b>The tormentor</b>: free, he cuts, throws scalpels, casts his hook (it drags whoever it bites to him) and whips his
 *   chains along the floor.</li>
 *   <li><b>The rack</b>: white smoke pours out of him, his demons climb out of the floor, and the rack rises under a hunter.</li>
 * </ol>
 * Every attack shows on the ground in its windup and lands only in ACTIVE: red for his blades, hooks and chains, yellow for
 * the smoke, red rings where his demons come up.
 */
public final class AlastairBalance {

    public static final int PHASES = 3;

    private AlastairBalance() {
    }

    /** Health fraction below which {@code phase} ends (no gaps between phases). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 48;
            case 2 -> 34;
            default -> 28;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return AlastairAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- the trap ----------------------------------------------------------------------------------------------------------
    /** Share of an unholy blow he takes while bound. */
    public static final float BOUND_MUNDANE = 0.25f;
    /** The chains' lash round the trap while bound: its ring, its blow. */
    public static final float BOUND_LASH_RADIUS = 4.5f, BOUND_LASH_DAMAGE = 5f;
    public static final int BOUND_LASH_WINDUP = 30;

    // --- the tormentor -----------------------------------------------------------------------------------------------------
    public static final float SLASH_REACH = 4f, SLASH_DAMAGE = 8f;
    public static final int SLASH_WINDUP = 14;
    /** Three thrown scalpels, fanned this far apart (degrees), each a lane this long and wide. */
    public static final float THROW_SPREAD = 18f, THROW_LENGTH = 16f, THROW_WIDTH = 1.2f, THROW_DAMAGE = 6f;
    public static final int THROW_WINDUP = 22;
    /** The hook: a lane, and what it does to whoever it bites (pulled to him, hurt, slowed). */
    public static final float HOOK_LENGTH = 18f, HOOK_WIDTH = 1.4f, HOOK_DAMAGE = 7f, HOOK_PULL = 1.6f;
    public static final int HOOK_WINDUP = 24, HOOK_SLOW = 40;
    /** The chains whipped along the floor: a ring round him. */
    public static final float LASH_RADIUS = 6f, LASH_DAMAGE = 9f;
    public static final int LASH_WINDUP = 26;

    // --- the rack ----------------------------------------------------------------------------------------------------------
    /** White smoke: a zone that blinds and chokes. */
    public static final float SMOKE_RADIUS = 4f, SMOKE_DAMAGE = 3f;
    public static final int SMOKE_WINDUP = 30, SMOKE_BLIND = 60;
    /** The rack: a circle under a hunter; whoever is in it when it rises is held and hurt. */
    public static final float RACK_RADIUS = 2.5f, RACK_DAMAGE = 12f;
    public static final int RACK_WINDUP = 34, RACK_HOLD = 60;
    /** His demons: how many per call, how many at most. */
    public static final int DEMONS_PER_CALL = 2, MAX_DEMONS = 4;
    public static final int SUMMON_WINDUP = 30;
}
