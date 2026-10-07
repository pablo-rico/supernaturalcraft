package org.papiricoh.supernaturalcraft.entity.boss.horsemen.death;

/**
 * One hunter's death clock in Death's fight (pure, tested in JUnit). It counts down from {@code fullTicks}; hitting
 * Death or killing a reaper winds it back to full. In the world of the dead it runs twice as fast. At zero the hunter
 * falls into limbo: {@code limboTicks} to reach the light, or they die. Reaching the light winds the clock back to full.
 */
public final class DeathClock {

    public enum Event { NONE, ENTER_LIMBO, DIE }

    /** Below this many ticks left, a hunter starts to see the reapers. */
    public static final int REAPERS_SEEN_BELOW = 400;

    private final int fullTicks, limboTicks;
    private int remaining;
    /** Ticks left in limbo, or -1 when not in it. */
    private int limbo = -1;

    public DeathClock(int fullTicks, int limboTicks) {
        this.fullTicks = fullTicks;
        this.limboTicks = limboTicks;
        this.remaining = fullTicks;
    }

    public Event tick(boolean deadWorld) {
        if (limbo >= 0) {
            if (--limbo <= 0) {
                limbo = -1;
                remaining = fullTicks;
                return Event.DIE;
            }
            return Event.NONE;
        }
        remaining -= deadWorld ? 2 : 1;
        if (remaining <= 0) {
            remaining = 0;
            limbo = limboTicks;
            return Event.ENTER_LIMBO;
        }
        return Event.NONE;
    }

    /** A blow on Death, or a reaper's end: back to full (not while in limbo: only the light gets you out). */
    public void reset() {
        if (limbo < 0) remaining = fullTicks;
    }

    /** A reaper's touch: {@code ticks} off the clock (never below one tick: the clock itself has to run out). */
    public void steal(int ticks) {
        if (limbo < 0) remaining = Math.max(1, remaining - Math.max(0, ticks));
    }

    /** Reached the light: out of limbo, the clock full again. */
    public void escape() {
        limbo = -1;
        remaining = fullTicks;
    }

    public boolean inLimbo() {
        return limbo >= 0;
    }

    public int remaining() {
        return remaining;
    }

    public int limboLeft() {
        return Math.max(0, limbo);
    }

    public int fullTicks() {
        return fullTicks;
    }

    public boolean reapersSeen() {
        return inLimbo() || remaining < REAPERS_SEEN_BELOW;
    }

    /** Restores a saved clock. */
    public void restore(int remaining, int limbo) {
        this.remaining = Math.max(0, Math.min(fullTicks, remaining));
        this.limbo = limbo < 0 ? -1 : Math.min(limboTicks, limbo);
    }

    public int limboRaw() {
        return limbo;
    }
}
