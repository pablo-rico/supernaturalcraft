package org.papiricoh.supernaturalcraft.entity.boss.metatron;

/**
 * The Word of God, as Metatron speaks it with the Angel Tablet: three orders, and how a hunter breaks
 * each. Pure, so the judgement can be tested without a world.
 */
public final class WordJudge {

    public enum Order {
        /** Do not move. */
        BE_STILL,
        /** Do not look at him. */
        LOOK_AWAY,
        /** Be kneeling (sneaking) when the Word ends. */
        KNEEL;

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Moving further than this from where the Word found you breaks BE STILL. */
    public static final double STILL_TOLERANCE = 0.5;
    /** Facing him closer than this (dot of look and the line to him) breaks LOOK AWAY. */
    public static final double LOOK_TOLERANCE = 0.8;

    private WordJudge() {
    }

    /**
     * @param moved     how far the hunter is from where they stood when the Word was spoken
     * @param lookDot   dot product of the hunter's look and the direction to Metatron
     * @param sneaking  whether they are kneeling now
     * @param ending    whether this is the Word's last moment (KNEEL is only judged then)
     */
    public static boolean disobeys(Order order, double moved, double lookDot, boolean sneaking, boolean ending) {
        return switch (order) {
            case BE_STILL -> moved > STILL_TOLERANCE;
            case LOOK_AWAY -> lookDot > LOOK_TOLERANCE;
            case KNEEL -> ending && !sneaking;
        };
    }

    /** The next order: never the same one twice running. */
    public static Order next(Order last, int roll) {
        Order[] all = Order.values();
        Order pick = all[Math.floorMod(roll, all.length)];
        return pick == last ? all[(pick.ordinal() + 1) % all.length] : pick;
    }
}
