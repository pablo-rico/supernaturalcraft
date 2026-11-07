package org.papiricoh.supernaturalcraft.apocworld.worldgen;

/**
 * Which of the Apocalypse World's five regions a column belongs to (v0.41), from the same noises its land is drawn from: the
 * Broken Ridges along the ridges' spines, the Cratered Fields where the pits sink, the Dry Riverbeds in the channels, the Burned
 * Forest where the old woods stood (the climate's vegetation), and the Ash Plains everywhere else. Pure.
 */
public final class AUBiomes {

    public enum Kind { ASH_PLAINS, BURNED_FOREST, CRATERED_FIELDS, BROKEN_RIDGES, DRY_RIVERBED }

    /** On a ridge when its {@link AUShape#ridgeness} passes this. */
    public static final double RIDGE_BIOME = 0.3;
    /** The Cratered Fields start a little before the craters (their rims and scattered pits). */
    public static final double CRATER_BIOME = AUShape.RIM_START - 0.04;
    /** A riverbed (its banks too). */
    public static final double RIVER_BIOME = AUShape.RIVER_WIDTH * 1.3;
    /** The Burned Forest: where the vegetation noise is over this. */
    public static final double FOREST_VEGETATION = 0.12;

    private AUBiomes() {
    }

    /**
     * @param ridges     the land's ridge noise
     * @param pits       the land's crater noise
     * @param river      the land's riverbed noise
     * @param vegetation the climate's vegetation, about -1..1
     */
    public static Kind pick(double ridges, double pits, double river, double vegetation) {
        if (AUShape.ridgeness(ridges) > RIDGE_BIOME) return Kind.BROKEN_RIDGES;
        if (Math.abs(river) < RIVER_BIOME) return Kind.DRY_RIVERBED;
        if (pits > CRATER_BIOME) return Kind.CRATERED_FIELDS;
        if (vegetation > FOREST_VEGETATION) return Kind.BURNED_FOREST;
        return Kind.ASH_PLAINS;
    }
}
