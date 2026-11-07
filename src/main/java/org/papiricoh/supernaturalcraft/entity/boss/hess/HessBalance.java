package org.papiricoh.supernaturalcraft.entity.boss.hess;

/**
 * Dr. Mildred Hess's numbers (v0.41, pure, tested by {@code HessBalanceTest}). Three phases of a third of her health each, in
 * the war room of the British compound:
 * <ol>
 *   <li><b>The Director</b>: bolts from her wand (a violet lane), a hex bag (a violet circle where it lands: slowed and
 *   weakened), and her agents, called in from the doors (red rings).</li>
 *   <li><b>The Old Ways</b>: she raises a ward (a violet circle round her: whoever is in it is thrown out and hurt), and while it
 *   holds every blow on her lands for {@link #WARD_SHARE} of itself.</li>
 *   <li><b>The Folio</b>: she reads a curse over the whole room (a wide violet ring) and only a small green circle is spared;
 *   everything else as before, faster.</li>
 * </ol>
 * Every attack shows on the ground in its windup and lands only in ACTIVE.
 */
public final class HessBalance {

    public static final int PHASES = 3;
    public static final int TRANSITION = 40, DEATH = 60, EMERGE = 40;

    private HessBalance() {
    }

    /** Health fraction below which {@code phase} ends (no gaps between phases). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between her attacks. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 32;
            default -> 26;
        };
    }

    public static int cue(String clip) {
        return HessAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    /** The share of a blow that lands while her ward holds ({@code warded}), else all of it. */
    public static float taken(boolean warded) {
        return warded ? WARD_SHARE : 1f;
    }

    // --- the director ----------------------------------------------------------------------------------------------
    public static final float BOLT_LENGTH = 20f, BOLT_WIDTH = 1.2f, BOLT_DAMAGE = 13f;
    public static final int BOLT_WINDUP = 18;
    public static final float HEX_RADIUS = 3f, HEX_DAMAGE = 7f;
    public static final int HEX_WINDUP = 24, HEX_TICKS = 100;
    /** Her agents: how many per call, how many at most. */
    public static final int AGENTS_PER_CALL = 2, MAX_AGENTS = 4, SUMMON_WINDUP = 30;

    // --- the old ways ----------------------------------------------------------------------------------------------
    public static final float WARD_RADIUS = 5f, WARD_DAMAGE = 10f, WARD_PUSH = 1.4f, WARD_SHARE = 0.4f;
    public static final int WARD_WINDUP = 26, WARD_TICKS = 100;

    // --- the folio -------------------------------------------------------------------------------------------------
    /** The curse: everyone this near her and outside the green circle (this wide, at least this far from her). */
    public static final float CURSE_RADIUS = 24f, SAFE_RADIUS = 3.5f, SAFE_MIN_DISTANCE = 6f, CURSE_DAMAGE = 16f;
    public static final int CURSE_WINDUP = 44, CURSE_WEAKNESS = 100;
}
