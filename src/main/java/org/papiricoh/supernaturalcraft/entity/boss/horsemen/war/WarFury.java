package org.papiricoh.supernaturalcraft.entity.boss.horsemen.war;

/**
 * War's fury, from 0 to {@link #MAX} (pure, tested in JUnit). Every blow he takes feeds it, every standard still
 * standing feeds it each second, a hunter striking an innocent or a friend under his illusion feeds it; breaking a
 * standard takes a lump of it away, and with no standard left it slowly cools. The fury makes him hit harder and move
 * faster.
 */
public final class WarFury {

    public static final float MAX = 100;
    /** Fury per point of damage he takes, counted as if he had {@link #REFERENCE_HEALTH} true health. */
    public static final float PER_DAMAGE = 0.5f;
    /** The health a blow is measured against (v0.15: his true health is far larger; what counts is the share taken). */
    public static final float REFERENCE_HEALTH = 800;
    /** Fury each standing standard adds per second. */
    public static final float PER_STANDARD = 0.75f;
    /** Fury lost when a standard is broken. */
    public static final float STANDARD_BROKEN = 20;
    /** Fury lost per second when no standard stands. */
    public static final float COOLING = 1.0f;
    /** Fury gained when a hunter strikes an innocent or a friend under his illusion. */
    public static final float BETRAYAL = 10;
    /** At full fury: damage ×(1 + this), speed ×(1 + {@link #MAX_SPEED_BONUS}). */
    public static final float MAX_DAMAGE_BONUS = 0.6f, MAX_SPEED_BONUS = 0.35f;

    private float fury;

    public float value() {
        return fury;
    }

    public void set(float value) {
        fury = clamp(value);
    }

    public void onHurt(float damage) {
        set(fury + Math.max(0, damage) * PER_DAMAGE);
    }

    /** He took {@code share} of his true max health in one blow. */
    public void onHurtShare(float share) {
        onHurt(share * REFERENCE_HEALTH);
    }

    /** Once a second: standards feed him; with none left he cools. */
    public void tickSecond(int standards) {
        set(standards > 0 ? fury + standards * PER_STANDARD : fury - COOLING);
    }

    public void standardBroken() {
        set(fury - STANDARD_BROKEN);
    }

    public void betrayal() {
        set(fury + BETRAYAL);
    }

    public float share() {
        return fury / MAX;
    }

    public float damageMultiplier() {
        return 1 + MAX_DAMAGE_BONUS * share();
    }

    public float speedMultiplier() {
        return 1 + MAX_SPEED_BONUS * share();
    }

    private static float clamp(float v) {
        return Math.max(0, Math.min(MAX, v));
    }
}
