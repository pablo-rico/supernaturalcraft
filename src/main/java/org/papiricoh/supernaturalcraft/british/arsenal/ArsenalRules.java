package org.papiricoh.supernaturalcraft.british.arsenal;

/**
 * The British arsenal's numbers (v0.41, pure, tested by {@code ArsenalRulesTest}): the suppressed pistol and its enchanted
 * rounds, the hex grenade, Hess's folio and Dagon's brand. Damage here is before the arsenal's scale
 * ({@code WeaponDamage.ability}/{@code magic}); against a boss every hex is only its small damage, never its effects.
 */
public final class ArsenalRules {

    private ArsenalRules() {
    }

    // --- the pistol ------------------------------------------------------------------------------------------------
    /** One round's damage, its reach (blocks), the pistol's rest between shots (ticks). */
    public static final float PISTOL_DAMAGE = 5f, PISTOL_RANGE = 32f;
    public static final int PISTOL_COOLDOWN = 20;
    /** The round's hex on what it hits (not a boss): slowness and weakness for this long (ticks), at this amplifier. */
    public static final int ROUND_HEX_TICKS = 60, ROUND_HEX_LEVEL = 0;

    // --- the hex grenade -------------------------------------------------------------------------------------------
    /** Its burst: radius, damage, how long it slows (II), weakens (I) and blinds; the thrower's rest (ticks). */
    public static final float GRENADE_RADIUS = 3.5f, GRENADE_DAMAGE = 3f;
    public static final int GRENADE_SLOW_TICKS = 100, GRENADE_WEAK_TICKS = 100, GRENADE_BLIND_TICKS = 60, GRENADE_COOLDOWN = 30;

    // --- Hess's folio ----------------------------------------------------------------------------------------------
    /** Read once: a ward on the reader (resistance II, absorption I) and a hex on every foe this near. */
    public static final int FOLIO_WARD_TICKS = 600, FOLIO_HEX_TICKS = 160;
    public static final float FOLIO_RADIUS = 8f;

    // --- Dagon's brand ---------------------------------------------------------------------------------------------
    /** A ring of hellfire round the bearer: its radius, its damage, how long foes burn (seconds), its rest (ticks). */
    public static final float BRAND_RADIUS = 4f, BRAND_DAMAGE = 3f;
    public static final int BRAND_BURN_SECONDS = 5, BRAND_COOLDOWN = 400, BRAND_SHIELD_TICKS = 200;

    /** How long a hex of {@code ticks} sticks on a target ({@code boss}: never; a boss takes only the damage). */
    public static int hexTicks(int ticks, boolean boss) {
        return boss ? 0 : Math.max(0, ticks);
    }

    /** The share of a burst's damage at {@code distance} from it (full in the middle, half at the rim, none past it). */
    public static float falloff(double distance, float radius) {
        if (distance > radius) return 0f;
        return (float) (1.0 - 0.5 * (distance / radius));
    }
}
