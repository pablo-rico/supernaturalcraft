package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

/**
 * The paperwork of Zachariah's office (v0.18, pure): at each phase's start and every {@code zachariah.formTicks} every hunter is
 * issued a {@link HeavenlyForm} for one of the four cabinets. Until it is filed their blows land at {@link #UNFILED} of their
 * worth; filed in the right cabinet (or stamped by a clerk's approval stamp), they are Approved for {@link #APPROVED_TICKS}
 * and hit harder ({@link #APPROVED}); left too long, the form is overdue and Heaven comes to collect.
 */
public final class FormRules {

    /** A hunter's blows while an issued form is still unfiled. */
    public static final float UNFILED = 0.25f;
    /** A hunter's blows while Approved. */
    public static final float APPROVED = 1.5f;
    /** How long an approval lasts. */
    public static final int APPROVED_TICKS = 200;
    /** Cabinets in the office (forms are numbered 1 to this). */
    public static final int CABINETS = 4;
    /** A form the office forgot (its fight is over) crumbles from an inventory after this many form periods. */
    public static final int STALE_PERIODS = 3;

    private FormRules() {
    }

    /** The multiplier on a hunter's blow to him: an unfiled form weighs it down, an approval lifts it. */
    public static float damageFactor(boolean unfiled, boolean approved) {
        return (unfiled ? UNFILED : 1f) * (approved ? APPROVED : 1f);
    }

    /** Whether a form issued at {@code issued} is overdue at {@code now} ({@code formTicks} to file it). */
    public static boolean overdue(long issued, long now, int formTicks) {
        return now - issued >= formTicks;
    }

    /** Ticks left to file a form issued at {@code issued} (0 once overdue). */
    public static int ticksLeft(long issued, long now, int formTicks) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, issued + formTicks - now));
    }

    /** Whether a form numbered {@code form} goes in cabinet {@code cabinet}. */
    public static boolean matches(int form, int cabinet) {
        return valid(form) && form == cabinet;
    }

    public static boolean valid(int number) {
        return number >= 1 && number <= CABINETS;
    }

    /**
     * The cabinet a new form names, from a random {@code roll}: never the one the hunter's last form named ({@code previous}, 0
     * if none), so nobody can camp one cabinet.
     */
    public static int numberFor(int roll, int previous) {
        if (!valid(previous)) return Math.floorMod(roll, CABINETS) + 1;
        int n = Math.floorMod(roll, CABINETS - 1) + 1;
        return n >= previous ? n + 1 : n;
    }

    /** Whether an approval granted until {@code until} still holds at {@code now}. */
    public static boolean approved(long until, long now) {
        return now < until;
    }

    /** Whether a form issued at {@code issued} has outlived any fight ({@link #STALE_PERIODS} periods). */
    public static boolean stale(long issued, long now, int formTicks) {
        return now - issued > (long) formTicks * STALE_PERIODS || now < issued;
    }

    /** I, II, III, IV: what is printed on a form and on a cabinet's front. */
    public static String roman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "?";
        };
    }
}
