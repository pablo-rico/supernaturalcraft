package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

/**
 * The numbers of the Michael of the Apocalypse World (v0.41, pure, tested by {@code AuMichaelBalanceTest}). Four phases of a
 * quarter of his health each:
 * <ol>
 *   <li><b>The general</b>: his archangel blade (a heavy cut, a three-cut combo) and the smite (holy fire under a hunter).</li>
 *   <li><b>The wings</b>: his wings open; a gale beaten forward throws the hunters back, and he orders his soldiers in.</li>
 *   <li><b>The burned sky</b>: the sky over his fortress answers him: bolts on every hunter, and a lance of lightning down a lane.</li>
 *   <li><b>The archangel</b>: he takes to the air over his court and rains holy fire and lightning from it.</li>
 * </ol>
 * He never dies here: at the last floor he breaks off and flees through a rift ({@link #FLEE_TICKS}). Every attack shows on the
 * ground in its windup and lands only in ACTIVE: red for his blade, gold for the holy fire and his soldiers' rings, white for the
 * gale, violet for the lightning.
 */
public final class AuMichaelBalance {

    public static final int PHASES = 4;

    private AuMichaelBalance() {
    }

    /** Health fraction below which {@code phase} ends (no gaps between phases). */
    public static float threshold(int phase) {
        return phase >= PHASES ? 0f : 1f - phase / (float) PHASES;
    }

    /** Ticks between his attacks. */
    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 32;
            case 3 -> 28;
            default -> 24;
        };
    }

    /** Ticks from a clip's start to its moment (the art's timings). */
    public static int cue(String clip) {
        return AuMichaelAssets.HIT_TICKS.getOrDefault(clip, 0);
    }

    /** Whether he walks the court (he hovers in the last phase). */
    public static boolean aerial(int phase) {
        return phase >= PHASES;
    }

    /** Whether his wings are out ({@code wings} bone shown) in {@code phase}. */
    public static boolean wings(int phase) {
        return phase >= 2;
    }

    // --- the general -------------------------------------------------------------------------------------------------------
    public static final float SLASH_REACH = 4.5f, SLASH_DAMAGE = 10f;
    public static final int SLASH_WINDUP = 18;
    /** The combo: three cuts, each a cone this far ahead, each a step forward. */
    public static final float COMBO_REACH = 4f, COMBO_DAMAGE = 6f, COMBO_STEP = 1.2f;
    public static final int COMBO_WINDUP = 22, COMBO_GAP = 6;
    /** The smite: a gold circle under a hunter, holy fire when it lands. */
    public static final float SMITE_RADIUS = 2.5f, SMITE_DAMAGE = 11f;
    public static final int SMITE_WINDUP = 26, SMITE_FIRE = 60;

    // --- the wings ---------------------------------------------------------------------------------------------------------
    /** The gale: a wide cone; whoever is in it is hurt a little and thrown far. */
    public static final float GALE_REACH = 10f, GALE_DAMAGE = 5f, GALE_SHOVE = 2.4f;
    public static final int GALE_WINDUP = 24;
    /** His soldiers: how many per order, how many at most. */
    public static final int SOLDIERS_PER_CALL = 2, MAX_SOLDIERS = 4;
    public static final int COMMAND_WINDUP = 28;

    // --- the burned sky ----------------------------------------------------------------------------------------------------
    /** A bolt on every hunter (and strays): its circle and its blow. */
    public static final float BOLT_RADIUS = 2.2f, BOLT_DAMAGE = 12f;
    public static final int BOLT_WINDUP = 34, STRAY_BOLTS = 2;
    /** The lance: bolts walked down a lane toward a hunter. */
    public static final float LANCE_LENGTH = 18f, LANCE_WIDTH = 2.4f, LANCE_DAMAGE = 10f;
    public static final int LANCE_WINDUP = 30, LANCE_BOLTS = 5;

    // --- the archangel -----------------------------------------------------------------------------------------------------
    /** Holy fire rained from the air: this many gold circles about the hunters. */
    public static final int RAIN_CIRCLES = 6, RAIN_WINDUP = 30;
    public static final float RAIN_RADIUS = 2.0f, RAIN_DAMAGE = 9f;
    /** How high he hovers over his anchor in the air, and how wide he circles. */
    public static final double HOVER_HEIGHT = 6.0, HOVER_RADIUS = 9.0;

    // --- his flight --------------------------------------------------------------------------------------------------------
    /** The whole flight, from the last floor to his last step into the rift (ticks). */
    public static final int FLEE_TICKS = 140;
    /** When Jack's grace flares at the hunters' sides; when he turns to the rift; when he is gone. */
    public static final int JACK_AT = 30, TURN_AT = 60, GONE_AT = TURN_AT + 40;
    /** How long the rift home he leaves behind stays open. */
    public static final int FLEE_RIFT_TICKS = 20 * 60 * 10;
    /** The fight's arena radius at his fortress (the whole court), and how close to his throne a hunter starts it. */
    public static final int ARENA_RADIUS = 26;
    public static final double TRIGGER = 12, RITE_REACH = 10;
}
