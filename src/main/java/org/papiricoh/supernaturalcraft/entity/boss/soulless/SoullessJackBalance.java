package org.papiricoh.supernaturalcraft.entity.boss.soulless;

/**
 * Soulless Jack's numbers (v0.41, season 14's finale, pure, tested by {@code SoullessJackBalanceTest}). Three phases of a third
 * of his health:
 * <ol>
 *   <li><b>The boy</b>: gold blasts (lanes), a wave of the hand that throws hunters back (violet cone), a snap of the fingers
 *   that unmakes what is under it (a violet circle: withering).</li>
 *   <li><b>The nephilim</b>: three lanes at once, the snap twice as wide, the gold nova round him (a gold ring).</li>
 *   <li><b>The sky torn</b>: aloft ({@code float_idle}), the nova wider, the lanes faster.</li>
 * </ol>
 */
public final class SoullessJackBalance {

    public static final int PHASES = 3;
    public static final int ARENA_RADIUS = 22;
    /** How far from the hunter he comes down at night. */
    public static final int AMBUSH_DISTANCE = 8;

    private SoullessJackBalance() {
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 38;
            case 2 -> 30;
            default -> 22;
        };
    }

    public static final float BLAST_LENGTH = 20f, BLAST_WIDTH = 1.5f, BLAST_DAMAGE = 14f, BLAST_SPREAD = 18f;
    public static final int BLAST_WINDUP = 22, BLAST_WINDUP_LAST = 16;
    public static final float WAVE_REACH = 6f, WAVE_DAMAGE = 9f, WAVE_PUSH = 1.8f;
    public static final int WAVE_WINDUP = 16;
    public static final float SNAP_RADIUS = 2.5f, SNAP_RADIUS_WIDE = 4.5f, SNAP_DAMAGE = 10f;
    public static final int SNAP_WINDUP = 26, SNAP_WITHER = 80;
    public static final float NOVA_RADIUS = 8f, NOVA_RADIUS_LAST = 11f, NOVA_DAMAGE = 16f;
    public static final int NOVA_WINDUP = 44;
}
