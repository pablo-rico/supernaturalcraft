package org.papiricoh.supernaturalcraft.bowl;

/**
 * How much a carried bowl loses when its bearer jumps, falls or is hit. Pure: the caller rolls the
 * dice ({@code roll} in [0, 1)) and applies the result ({@link BowlCarry}).
 */
public final class SpillRules {

    /** Chance a jump sloshes out one dose. */
    public static final double JUMP_CHANCE = 0.15;
    /** Falls shorter than this spill nothing; from {@link #FALL_ALL} on, everything goes. */
    public static final float FALL_SAFE = 3, FALL_ALL = 8;
    /** Damage per dose lost to a blow, and the chance per point of damage that an ingredient flies out. */
    public static final float DAMAGE_PER_DOSE = 4, ITEM_CHANCE_PER_DAMAGE = 0.08f, ITEM_CHANCE_MAX = 0.6f;

    /** What to take out of the bowl: the last {@code doses} doses and the last {@code items} ingredients. */
    public record Spill(int doses, int items) {
        public static final Spill NONE = new Spill(0, 0);

        public boolean isNone() {
            return doses <= 0 && items <= 0;
        }
    }

    private SpillRules() {
    }

    public static Spill jump(int doses, double roll) {
        return doses > 0 && roll < JUMP_CHANCE ? new Spill(1, 0) : Spill.NONE;
    }

    /**
     * A fall of {@code distance} blocks: nothing below {@link #FALL_SAFE}, everything from
     * {@link #FALL_ALL}, and in between a share that grows with the height (at least one dose if
     * there is liquid; ingredients only from about halfway).
     */
    public static Spill fall(float distance, int doses, int items) {
        if (distance < FALL_SAFE) return Spill.NONE;
        if (distance >= FALL_ALL) return new Spill(doses, items);
        float f = (distance - FALL_SAFE) / (FALL_ALL - FALL_SAFE);
        int d = doses == 0 ? 0 : Math.max(1, (int) Math.ceil(f * doses));
        int i = (int) Math.floor(f * items);
        return new Spill(Math.min(d, doses), Math.min(i, items));
    }

    /** A blow of {@code damage}: a dose per {@link #DAMAGE_PER_DOSE} (at least one), and maybe one ingredient. */
    public static Spill hit(float damage, int doses, int items, double roll) {
        if (damage <= 0) return Spill.NONE;
        int d = doses == 0 ? 0 : Math.min(doses, Math.max(1, Math.round(damage / DAMAGE_PER_DOSE)));
        double itemChance = Math.min(ITEM_CHANCE_MAX, damage * ITEM_CHANCE_PER_DAMAGE);
        int i = items > 0 && roll < itemChance ? 1 : 0;
        return new Spill(d, i);
    }
}
