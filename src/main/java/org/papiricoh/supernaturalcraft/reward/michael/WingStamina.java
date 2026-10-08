package org.papiricoh.supernaturalcraft.reward.michael;

/**
 * The Seraph Wings' stamina under Michael's Grace (pure, tested in JUnit): flight drains it a tick at a time; on the
 * ground it comes back, the full bar in {@link #RECHARGE_SECONDS}. Run dry in the air and the wings will not carry you
 * again until you have touched the ground and got some back.
 */
public final class WingStamina {

    /** Seconds on the ground from empty to full. */
    public static final float RECHARGE_SECONDS = 5f;
    /** The share of the bar needed before the wings lift you again after running dry. */
    public static final float RESUME_SHARE = 0.15f;

    private final int max;
    private float ticks;
    private boolean exhausted;

    /** A full bar of {@code seconds} of flight. */
    public WingStamina(int seconds) {
        this.max = Math.max(1, seconds * 20);
        this.ticks = max;
    }

    public int max() {
        return max;
    }

    public float ticks() {
        return ticks;
    }

    /** 0..1. */
    public float share() {
        return ticks / max;
    }

    /** Whether the wings can carry you now. */
    public boolean canFly() {
        return !exhausted && ticks > 0;
    }

    public boolean exhausted() {
        return exhausted;
    }

    /**
     * One tick. Returns true the tick the wings give out (the caller drops the flight).
     *
     * @param flying   flying on the wings this tick
     * @param onGround standing on the ground
     */
    public boolean tick(boolean flying, boolean onGround) {
        if (flying && !exhausted) {
            ticks = Math.max(0, ticks - 1);
            if (ticks == 0) {
                exhausted = true;
                return true;
            }
            return false;
        }
        if (onGround) {
            ticks = Math.min(max, ticks + max / (RECHARGE_SECONDS * 20));
            if (exhausted && ticks >= max * RESUME_SHARE) exhausted = false;
        }
        return false;
    }

    /** Fills the bar (a new grant, a respawn). */
    public void refill() {
        ticks = max;
        exhausted = false;
    }
}
