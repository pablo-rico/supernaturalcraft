package org.papiricoh.supernaturalcraft.allegiance.power;

import org.papiricoh.supernaturalcraft.allegiance.Allegiance;

/** Whether a power may be cast (pure; the server asks this for every {@code CastPowerPayload}). */
public final class PowerRules {

    /** Why a cast was refused (the ordinal travels in {@code AllegianceFxPayload.DENIED}; append new ones at the end). */
    public enum Verdict {
        OK, NOT_YOURS, RANK_TOO_LOW, PASSIVE, NO_ESSENCE, COOLING_DOWN, SUPPRESSED,
        /** Cast, but nothing there to cast it on (no target in range and sight, nowhere to land): nothing is spent. */
        NO_TARGET
    }

    private PowerRules() {
    }

    /** Whether the player has this power at all (passive or not). */
    public static boolean has(Allegiance a, Power p) {
        return p.faction == a.faction() && a.rank() >= p.minRank;
    }

    /**
     * @param cooldownLeft ticks before this power is ready again
     * @param suppressed   powers are nullified here (Chuck's fight: "I gave you that")
     */
    public static Verdict canCast(Allegiance a, Power p, int cooldownLeft, boolean suppressed) {
        if (p.faction != a.faction()) return Verdict.NOT_YOURS;
        if (a.rank() < p.minRank) return Verdict.RANK_TOO_LOW;
        if (p.passive) return Verdict.PASSIVE;
        if (suppressed) return Verdict.SUPPRESSED;
        if (cooldownLeft > 0) return Verdict.COOLING_DOWN;
        if (a.essence() < p.cost) return Verdict.NO_ESSENCE;
        return Verdict.OK;
    }

    /** Whether a passive works now (passives are nullified too while suppressed). */
    public static boolean passiveActive(Allegiance a, Power p, boolean suppressed) {
        return p.passive && has(a, p) && !suppressed;
    }
}
