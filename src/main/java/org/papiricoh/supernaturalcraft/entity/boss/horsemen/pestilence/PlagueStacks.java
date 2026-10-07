package org.papiricoh.supernaturalcraft.entity.boss.horsemen.pestilence;

/**
 * The plague's numbers (pure, tested in JUnit). The effect's amplifier is the number of stacks minus one; each new dose
 * adds stacks up to {@link #MAX_STACKS} and refreshes how long it lasts. With every stack it eats more of you each second
 * and takes away more of your hearts. The antidote cures it all and keeps it off for {@link #IMMUNITY_TICKS}.
 */
public final class PlagueStacks {

    public static final int MAX_STACKS = 5;
    public static final int DURATION_TICKS = 240;
    public static final int IMMUNITY_TICKS = 300;
    /** Damage per stack, every {@link #DAMAGE_EVERY} ticks. */
    public static final float DAMAGE_PER_STACK = 0.5f;
    public static final int DAMAGE_EVERY = 30;
    /** Max health lost per stack while it lasts. */
    public static final float HEALTH_PER_STACK = 2f;
    /** Heals at most this big are stopped while plagued (natural regeneration); potions still work. */
    public static final float BLOCKED_HEAL = 1.0f;

    private PlagueStacks() {
    }

    /** Stacks from an effect amplifier; -1 (no effect) is none. */
    public static int stacks(int amplifier) {
        return amplifier < 0 ? 0 : Math.min(MAX_STACKS, amplifier + 1);
    }

    /** The amplifier after {@code doses} more stacks on top of {@code amplifier} (-1 = not plagued). */
    public static int add(int amplifier, int doses) {
        int s = Math.min(MAX_STACKS, stacks(amplifier) + Math.max(0, doses));
        return s - 1;
    }

    public static float damage(int amplifier) {
        return stacks(amplifier) * DAMAGE_PER_STACK;
    }

    public static float healthLost(int amplifier) {
        return stacks(amplifier) * HEALTH_PER_STACK;
    }

    /** Whether a heal of {@code amount} goes through while plagued. */
    public static boolean healAllowed(float amount) {
        return amount > BLOCKED_HEAL;
    }
}
