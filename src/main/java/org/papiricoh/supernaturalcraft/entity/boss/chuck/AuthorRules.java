package org.papiricoh.supernaturalcraft.entity.boss.chuck;

/**
 * The rules the Author can rewrite for a while, as bits of one int (synced on {@link ChuckEntity#rules()} and
 * sent in {@code AuthorFxPayload.RULE}). Pure.
 */
public final class AuthorRules {

    /** Hunters fall slowly and jump high. */
    public static final int GRAVITY_LOW = 1;
    /** Hunters fall up, onto a ceiling of pages. */
    public static final int GRAVITY_INVERTED = 1 << 1;
    /** Water burns whoever stands in it. */
    public static final int WATER_BURNS = 1 << 2;
    /** Light hurts: standing near a light source burns. */
    public static final int LIGHT_HURTS = 1 << 3;
    /** The floor is lava, but for islands of safe ground. */
    public static final int FLOOR_LAVA = 1 << 4;

    public static final int[] ALL = {GRAVITY_LOW, GRAVITY_INVERTED, WATER_BURNS, LIGHT_HURTS, FLOOR_LAVA};

    private AuthorRules() {
    }

    public static boolean has(int rules, int rule) {
        return (rules & rule) != 0;
    }

    /** {@code rule.supernaturalcraft.<name>}: the line written across the screen as the rule changes. */
    public static String key(int rule) {
        return "rule.supernaturalcraft." + switch (rule) {
            case GRAVITY_LOW -> "gravity_low";
            case GRAVITY_INVERTED -> "gravity_inverted";
            case WATER_BURNS -> "water_burns";
            case LIGHT_HURTS -> "light_hurts";
            case FLOOR_LAVA -> "floor_lava";
            default -> "unknown";
        };
    }
}
