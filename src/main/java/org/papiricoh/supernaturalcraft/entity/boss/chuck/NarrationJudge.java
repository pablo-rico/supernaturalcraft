package org.papiricoh.supernaturalcraft.entity.boss.chuck;

import java.util.Locale;

/**
 * The Author narrates the hunter in chapter 5 ("and the hunter ran"), and the only way to hurt him is to make him a
 * liar: do the OPPOSITE of the line. Metatron's {@code WordJudge}, inverted. Pure, so the judgement can be tested
 * without a world.
 *
 * <p>Opposites: RUN ↔ STAND_STILL, LOOK_AWAY ↔ look at him, JUMP ↔ KNEEL (sneak). Doing what he wrote is obeying (he
 * punishes it); doing the opposite is a contradiction (the script cracks); anything in between is neither.
 */
public final class NarrationJudge {

    public enum Order {
        /** "And the hunter ran." Contradicted by standing still. */
        RUN,
        /** "And the hunter stood still." Contradicted by running. */
        STAND_STILL,
        /** "And the hunter looked away." Contradicted by looking him in the eye at the end. */
        LOOK_AWAY,
        /** "And the hunter jumped." Contradicted by kneeling, feet on the ground. */
        JUMP,
        /** "And the hunter knelt." Contradicted by jumping and standing at the end. */
        KNEEL;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The order a byte from {@link ChuckEntity#narratedOrder()} names, or null for -1. */
        public static Order of(int ordinal) {
            Order[] all = values();
            return ordinal < 0 || ordinal >= all.length ? null : all[ordinal];
        }
    }

    public enum Verdict { OBEYED, CONTRADICTED, NEITHER }

    /** Moving less than this (horizontally, from where the line found you) is standing still. */
    public static final double STILL_TOLERANCE = 0.6;
    /** Moving at least this far is running. */
    public static final double RUN_DISTANCE = 3.0;
    /** Facing him closer than this (dot of look and the line to him) is looking at him. */
    public static final double LOOK_TOLERANCE = 0.8;

    private NarrationJudge() {
    }

    /**
     * @param moved          how far the hunter is (on the ground plane) from where they stood when the line was spoken
     * @param lookDot        dot product of the hunter's look and the direction to the Author, at the end
     * @param jumped         whether they left the ground at any moment of it
     * @param sneakingAtEnd  whether they are kneeling (sneaking) at the end
     */
    public static Verdict judge(Order order, double moved, double lookDot, boolean jumped, boolean sneakingAtEnd) {
        boolean still = moved <= STILL_TOLERANCE, ran = moved >= RUN_DISTANCE, looking = lookDot > LOOK_TOLERANCE;
        return switch (order) {
            case RUN -> ran ? Verdict.OBEYED : still ? Verdict.CONTRADICTED : Verdict.NEITHER;
            case STAND_STILL -> still ? Verdict.OBEYED : ran ? Verdict.CONTRADICTED : Verdict.NEITHER;
            case LOOK_AWAY -> looking ? Verdict.CONTRADICTED : Verdict.OBEYED;
            case JUMP -> jumped ? Verdict.OBEYED : sneakingAtEnd ? Verdict.CONTRADICTED : Verdict.NEITHER;
            case KNEEL -> sneakingAtEnd ? Verdict.OBEYED : jumped ? Verdict.CONTRADICTED : Verdict.NEITHER;
        };
    }

    /** The next line: never the same order twice running. */
    public static Order next(Order last, int roll) {
        Order[] all = Order.values();
        Order pick = all[Math.floorMod(roll, all.length)];
        return pick == last ? all[(pick.ordinal() + 1) % all.length] : pick;
    }

    /** The translation key of the narrated line, {@code narration.supernaturalcraft.chuck.order.<order>}. */
    public static String lineKey(Order order) {
        return "narration.supernaturalcraft.chuck.order." + order.key();
    }
}
