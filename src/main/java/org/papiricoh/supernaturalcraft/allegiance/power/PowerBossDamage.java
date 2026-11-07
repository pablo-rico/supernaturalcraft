package org.papiricoh.supernaturalcraft.allegiance.power;

import org.papiricoh.supernaturalcraft.balance.ProgressionScale;

/**
 * What a faction power deals to a great enemy (pure, v0.38). Boss health is designed in blows of the reference weapon
 * ({@link ProgressionScale#referenceHit}, the Angel Blade ascended to the boss's own tier); a power is priced the same way:
 * each blow or pulse is worth a few of those blows, at the boss's tier, whatever the caster's tier is. Powers help in a
 * fight; they don't replace the weapon. Against anything that is not a boss nothing changes.
 * <p>
 * A higher rank than the power asks for adds {@link #RANK_BONUS} a rank.
 */
public final class PowerBossDamage {

    /** Smite's palm to the face. */
    public static final float SMITE = 2f;
    /** A lance of light. */
    public static final float LIGHT_LANCE = 1f;
    /** The pillar of judgment. */
    public static final float PILLAR = 4f;
    /** The snap of the fingers. */
    public static final float SNAP = 1f;
    /** Healing touch burning a demon boss. */
    public static final float TOUCH_BURN = 0.5f;
    /** Each pulse of true form on a demon boss (one a second). */
    public static final float TRUE_FORM_PULSE = 0.25f;
    /** The ring of hellfire. */
    public static final float HELLFIRE_BURST = 1.5f;
    /** The wave of flame as it passes, and each burn of the ground it leaves. */
    public static final float FLAME_WAVE = 1f, FLAME_WAVE_BURN = 0.15f;
    /** Each burn of the Maw's core. */
    public static final float MAW_BURN = 0.3f;
    /** A shadow step's exposed blow against a boss (against anything else it is {@code DemonPowers.EXPOSED}). */
    public static final float SHADOW_STEP_BOSS = 1.25f;
    /** Extra share a rank above the power's own. */
    public static final float RANK_BONUS = 0.1f;

    private PowerBossDamage() {
    }

    /** The main blow of {@code power}, in reference blows (0 for a power that does not strike a boss). */
    public static float hits(Power power) {
        return switch (power) {
            case SMITE -> SMITE;
            case LIGHT_LANCE -> LIGHT_LANCE;
            case JUDGMENT_PILLAR -> PILLAR;
            case SNAP -> SNAP;
            case HEALING_TOUCH -> TOUCH_BURN;
            case TRUE_FORM -> TRUE_FORM_PULSE;
            case HELLFIRE_BURST -> HELLFIRE_BURST;
            case FLAME_WAVE -> FLAME_WAVE;
            case MAW_OF_HELL -> MAW_BURN;
            default -> 0f;
        };
    }

    /** Every blow of one cast, lingering ones included (the wave's and the maw's burns, true form's pulses). */
    public static float hitsPerCast(Power power) {
        return switch (power) {
            case FLAME_WAVE -> FLAME_WAVE + FLAME_WAVE_BURN * (DemonPowers.WAVE_TICKS / DemonPowers.WAVE_EVERY);
            case MAW_OF_HELL -> MAW_BURN * (DemonPowers.MAW_TICKS / DemonPowers.MAW_EVERY);
            case TRUE_FORM -> TRUE_FORM_PULSE * (ActivePowers.TRUE_FORM_TICKS / 20f);
            default -> hits(power);
        };
    }

    /** The multiplier of being {@code rank} for a power first given at {@code minRank}. */
    public static float rankBonus(int rank, int minRank) {
        return 1f + RANK_BONUS * Math.max(0, rank - minRank);
    }

    /** {@code hits} reference blows against a boss of {@code bossTier}, cast at {@code rank} by a power of {@code minRank}. */
    public static float damage(float hits, int bossTier, int rank, int minRank) {
        return hits * ProgressionScale.referenceHit(bossTier) * rankBonus(rank, minRank);
    }

    /** {@link #damage(float, int, int, int)} for the main blow of {@code power}. */
    public static float damage(Power power, int bossTier, int rank) {
        return damage(hits(power), bossTier, rank, power.minRank);
    }
}
