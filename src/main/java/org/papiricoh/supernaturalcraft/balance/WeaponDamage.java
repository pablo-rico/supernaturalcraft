package org.papiricoh.supernaturalcraft.balance;

/** Shared weapon numbers without Minecraft dependencies. */
public final class WeaponDamage {
    public static final float WEAPON_DAMAGE_SCALE = 2f;
    public static final int ANGEL_BLADE_EXTRA = 3;
    public static final float ANGEL_BLADE_TIER_DAMAGE = 3f, ANGEL_BLADE_BANE = 3f;
    public static final float ANGEL_BLADE_SPEED = -2.2f;

    private WeaponDamage() {
    }

    public static float angelBladeModifier() {
        return angelBladeBaseModifier() * WEAPON_DAMAGE_SCALE;
    }

    /** The Angel Blade's own damage before {@link #WEAPON_DAMAGE_SCALE}: what boss health is measured against. */
    public static float angelBladeBaseModifier() {
        return ANGEL_BLADE_TIER_DAMAGE + ANGEL_BLADE_EXTRA;
    }

    /** Damage a mod weapon's own ability deals, at the arsenal's scale. */
    public static float ability(float damage) {
        return damage * WEAPON_DAMAGE_SCALE;
    }

    /** The same edge for magic (v0.24): every damaging sigil and faction power deals this much more. */
    public static final float MAGIC_DAMAGE_SCALE = 2f;

    /** Damage a sigil or a faction power deals, at {@link #MAGIC_DAMAGE_SCALE}. */
    public static float magic(float damage) {
        return damage * MAGIC_DAMAGE_SCALE;
    }
}
