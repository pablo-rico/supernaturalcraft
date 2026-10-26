package org.papiricoh.supernaturalcraft.balance;

/** Boss-only spell tuning; ordinary creatures retain each sigil's full damage. */
public final class SpellBossDamage {
    private SpellBossDamage() {
    }

    public static float effectScale(String behavior) {
        return switch (behavior) {
            case "smite" -> 0.8f;
            case "exorcise" -> 0.95f;
            default -> 1f;
        };
    }

    /** A persistent circle distributes its boss damage across its pulses. */
    public static float pulseScale(int lifetime) {
        return 1f / Math.max(1, (lifetime - 1) / 20);
    }
}
