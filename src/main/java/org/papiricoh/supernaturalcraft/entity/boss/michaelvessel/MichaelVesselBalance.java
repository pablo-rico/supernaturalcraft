package org.papiricoh.supernaturalcraft.entity.boss.michaelvessel;

/**
 * Michael's numbers in this world (v0.41, season 14, pure, tested by {@code MichaelVesselBalanceTest}). The same fight in two
 * places: out in the open (the hunter said no, so he came down in a vessel) or in the mindscape (the hunter said yes, and
 * throws him out of their own head: there his projection hits a little softer, {@link #MINDSCAPE_DAMAGE}, but the room is
 * small). Three phases of a third of his health:
 * <ol>
 *   <li><b>The general</b>: a blade, a palm of blue light, the fingers' snap that brings his improved monsters.</li>
 *   <li><b>The archangel</b>: wings flared (a blue ring), a choking hand (violet), blinking behind a hunter.</li>
 *   <li><b>Cold fire</b>: three lanes of light, wider rings, monsters two at a time.</li>
 * </ol>
 */
public final class MichaelVesselBalance {

    public static final int PHASES = 3;
    public static final int ARENA_RADIUS = 20;
    public static final float MINDSCAPE_DAMAGE = 0.85f;

    private MichaelVesselBalance() {
    }

    public static int attackGap(int phase) {
        return switch (phase) {
            case 1 -> 40;
            case 2 -> 30;
            default -> 24;
        };
    }

    public static final float SLASH_REACH = 4.5f, SLASH_DAMAGE = 12f;
    public static final int SLASH_WINDUP = 14;
    public static final float SMITE_LENGTH = 18f, SMITE_WIDTH = 1.6f, SMITE_DAMAGE = 14f, SMITE_SPREAD = 22f;
    public static final int SMITE_WINDUP = 24;
    public static final float FLARE_RADIUS = 7f, FLARE_RADIUS_LAST = 9f, FLARE_DAMAGE = 13f;
    public static final int FLARE_WINDUP = 32;
    public static final float CHOKE_RADIUS = 3f, CHOKE_DAMAGE = 9f;
    public static final int CHOKE_WINDUP = 28, CHOKE_LIFT = 50;
    public static final int MONSTERS_PER_SNAP = 1, MONSTERS_PER_SNAP_LAST = 2, MAX_MONSTERS = 4, SNAP_WINDUP = 22;
    public static final int BLINK_WINDUP = 12;
    /** What his snap does to the monster it brings ("better"): seconds of each buff, its amplifier. */
    public static final int IMPROVED_SECONDS = 120, IMPROVED_AMPLIFIER = 1;
}
