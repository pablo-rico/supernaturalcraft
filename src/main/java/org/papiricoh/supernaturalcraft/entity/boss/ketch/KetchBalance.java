package org.papiricoh.supernaturalcraft.entity.boss.ketch;

/**
 * Arthur Ketch's numbers (v0.41, pure, tested by {@code KetchBalanceTest}). Three phases of a third of his health each:
 * <ol>
 *   <li><b>The Operative</b>: his suppressed pistol (a single shot down a red lane, or a volley of three fanned lanes) and his
 *   knife for whoever comes close (a red cone).</li>
 *   <li><b>The Hound</b>: he lunges with the knife (a red lane he crosses himself) and throws hex grenades (a violet circle
 *   where it will burst: whoever stands in it is slowed and weakened).</li>
 *   <li><b>Nothing Personal</b>: he drops smoke (a yellow circle: whoever is in it is blinded) and steps out of it behind his
 *   target; everything comes faster.</li>
 * </ol>
 * Every attack shows on the ground in its windup and lands only in ACTIVE.
 */
public final class KetchBalance {

    public static final int PHASES = 3;
    /** How long his turn into a phase lasts, his death, his entrance. */
    public static final int TRANSITION = 40, DEATH = 60, EMERGE = 40;

    private KetchBalance() {
    }

    /** Health fraction below which {@code phase} ends (no gaps between phases). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 36;
            case 2 -> 28;
            default -> 22;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return KetchAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    // --- the operative ---------------------------------------------------------------------------------------------
    /** One aimed shot: a lane. */
    public static final float SHOT_LENGTH = 22f, SHOT_WIDTH = 1.0f, SHOT_DAMAGE = 12f;
    public static final int SHOT_WINDUP = 16;
    /** Three quick shots, fanned this far apart (degrees). */
    public static final float VOLLEY_SPREAD = 13f, VOLLEY_LENGTH = 18f, VOLLEY_WIDTH = 1.0f, VOLLEY_DAMAGE = 9f;
    public static final int VOLLEY_WINDUP = 22;
    /** The knife, close: a cone. */
    public static final float SLASH_REACH = 3.5f, SLASH_DAMAGE = 14f;
    public static final int SLASH_WINDUP = 14;

    // --- the hound -------------------------------------------------------------------------------------------------
    /** A lunge with the knife: a lane he crosses. */
    public static final float LUNGE_LENGTH = 9f, LUNGE_WIDTH = 1.6f, LUNGE_DAMAGE = 16f;
    public static final int LUNGE_WINDUP = 18;
    /** A hex grenade: a circle where it bursts; slowness II and weakness for this long. */
    public static final float GRENADE_RADIUS = 3.5f, GRENADE_DAMAGE = 8f;
    public static final int GRENADE_WINDUP = 26, GRENADE_HEX = 80;

    // --- nothing personal ------------------------------------------------------------------------------------------
    /** Smoke round him: blinds who is in it; he steps out behind his target this far. */
    public static final float SMOKE_RADIUS = 4f, SMOKE_DAMAGE = 4f, VANISH_BEHIND = 3f;
    public static final int SMOKE_WINDUP = 20, SMOKE_BLIND = 60;
    /** Too far, or out of sight this long: he closes the distance in smoke. */
    public static final float CHASE_DISTANCE = 18f;
    public static final int CHASE_NO_SIGHT = 60;
}
