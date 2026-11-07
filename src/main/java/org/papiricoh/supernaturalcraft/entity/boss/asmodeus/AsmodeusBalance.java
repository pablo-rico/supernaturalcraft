package org.papiricoh.supernaturalcraft.entity.boss.asmodeus;

/**
 * Asmodeus's numbers (v0.41, pure, tested by {@code AsmodeusBalanceTest}). Three phases of a third of his health each:
 * <ol>
 *   <li><b>The gentleman</b>: the cane (a red cone, a red ring when he slams it), his hellhounds, a borrowed face to step
 *   behind a hunter (violet).</li>
 *   <li><b>Stolen grace</b>: he drinks from the vial of Gabriel's grace: gold lanes and gold circles under the hunters.</li>
 *   <li><b>A Prince of Hell</b>: three lanes of grace at once, a gold nova round him, more hounds.</li>
 * </ol>
 */
public final class AsmodeusBalance {

    public static final int PHASES = 3;
    /** The arena round a rite. */
    public static final int ARENA_RADIUS = 20;

    private AsmodeusBalance() {
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 44;
            case 2 -> 34;
            default -> 26;
        };
    }

    public static final float CANE_REACH = 4.5f, CANE_DAMAGE = 10f;
    public static final int CANE_WINDUP = 16;
    public static final float SLAM_RADIUS = 6f, SLAM_DAMAGE = 11f;
    public static final int SLAM_WINDUP = 28;
    public static final int HOUNDS_PER_CALL = 2, MAX_HOUNDS = 4, MAX_HOUNDS_LAST = 6, SUMMON_WINDUP = 30;
    public static final int BLINK_WINDUP = 14;

    public static final float GRACE_LENGTH = 18f, GRACE_WIDTH = 1.6f, GRACE_DAMAGE = 13f, GRACE_SPREAD = 20f;
    public static final int GRACE_WINDUP = 26;
    public static final float SMITE_RADIUS = 3.5f, SMITE_DAMAGE = 12f;
    public static final int SMITE_WINDUP = 30;
    public static final float NOVA_RADIUS = 8f, NOVA_DAMAGE = 15f;
    public static final int NOVA_WINDUP = 40;
}
