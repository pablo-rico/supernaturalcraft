package org.papiricoh.supernaturalcraft.purgatory.worldgen;

/**
 * Which of Purgatory's four regions a column belongs to (v0.39), from the same noises its land is drawn from: the Mire where the
 * hills sink into hollows, the Crags on the cliffs' high terraces, the Mother's Wood (round Eve's glade) where the climate is
 * warm and wet, and the Endless Forest everywhere else. Pure, so the layout can be checked without a world.
 */
public final class PurgatoryBiomes {

    public enum Kind { ENDLESS_FOREST, MIRE, CRAGS, MOTHERS_WOOD }

    /** The Mire starts a little before the land sinks below the hollow line, so its edge is boggy. */
    public static final double MIRE_BELOW = PurgatoryShape.HOLLOW + 0.06;
    public static final double CRAG_CLIFFS = PurgatoryShape.CLIFF_HIGH - 0.06, CRAG_HILLS = 0.7;
    /** The Mother's Wood: warm and wet. */
    public static final float WOOD_HOT = 0.3f, WOOD_WET = 0.05f;

    private PurgatoryBiomes() {
    }

    /**
     * @param hills       the land's hills noise
     * @param cliffs      the land's cliffs noise
     * @param temperature climate temperature, about -1..1
     * @param humidity    climate humidity, about -1..1
     */
    public static Kind pick(double hills, double cliffs, float temperature, float humidity) {
        if (hills < MIRE_BELOW) return Kind.MIRE;
        if (cliffs > CRAG_CLIFFS || hills > CRAG_HILLS) return Kind.CRAGS;
        if (temperature > WOOD_HOT && humidity > WOOD_WET) return Kind.MOTHERS_WOOD;
        return Kind.ENDLESS_FOREST;
    }
}
