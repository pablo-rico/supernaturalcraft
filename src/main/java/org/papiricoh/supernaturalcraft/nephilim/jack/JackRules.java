package org.papiricoh.supernaturalcraft.nephilim.jack;

import org.papiricoh.supernaturalcraft.nephilim.NephilimRules.Step;

/**
 * v0.41, pure: what Jack can do at each step of the road. Newborn he smites with his grace and shields his hunter; after the
 * Apocalypse World he is stronger; once he has burned his grace to drive Michael back (season 13's end) he is only a boy who
 * can throw a punch, until he trades his soul for his power and leaves (season 14).
 */
public final class JackRules {

    private JackRules() {
    }

    /** He keeps within this of his hunter (blinking back beyond {@link #BLINK}); he strikes within {@link #REACH}. */
    public static final double FOLLOW = 4, BLINK = 24, REACH = 10;
    /** Ticks away after he falls (healing), before he comes back. */
    public static final int RETREAT_TICKS = 20 * 120;
    public static final int SMITE_COOLDOWN = 60, PUNCH_COOLDOWN = 30, SHIELD_COOLDOWN = 20 * 60;
    /** His shield covers a hunter below this share of their health. */
    public static final float SHIELD_BELOW = 0.3f;
    public static final int SHIELD_TICKS = 20 * 10;
    public static final double MAX_HEALTH = 60;

    /** Whether he still has his grace (born, through the Apocalypse World, until he spends it on Michael). */
    public static boolean graced(Step s) {
        return s.atLeast(Step.BORN) && !s.atLeast(Step.MICHAEL_FLED);
    }

    /** One strike's damage at step {@code s} (0 when he is not walking with the hunter). */
    public static float strike(Step s) {
        if (!s.jackWalks()) return 0;
        if (!graced(s)) return 6f;
        return s.atLeast(Step.IN_AU) ? 16f : 12f;
    }

    public static int cooldown(Step s) {
        return graced(s) ? SMITE_COOLDOWN : PUNCH_COOLDOWN;
    }

    /** Whether he can raise his shield over the hunter at step {@code s}. */
    public static boolean shields(Step s) {
        return graced(s);
    }

    /** Whether he may give his blood today ({@code lastBlood} and {@code today} in days): once a day. */
    public static boolean bloodDue(long lastBlood, long today) {
        return today > lastBlood;
    }

    /** The line-set he talks from at step {@code s}: {@code born}, {@code au}, {@code graceless}. */
    public static String mood(Step s) {
        if (!graced(s)) return "graceless";
        return s.atLeast(Step.IN_AU) ? "au" : "born";
    }

    /** How many talk lines each mood has ({@code message.supernaturalcraft.jack.talk.<mood>.<n>}). */
    public static final int LINES = 4;
}
