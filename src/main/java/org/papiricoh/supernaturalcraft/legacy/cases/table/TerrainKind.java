package org.papiricoh.supernaturalcraft.legacy.cases.table;

/** What the land of a war table's sample is (v0.19), as its hologram tints it; pure. Stored in the low bits of a sample's surface byte. */
public enum TerrainKind {
    OTHER, PLAINS, FOREST, JUNGLE, SWAMP, DESERT, BADLANDS, SNOW, MOUNTAIN, BEACH, RIVER, OCEAN, DEEP_OCEAN, MUSHROOM;

    private static final TerrainKind[] VALUES = values();

    public static TerrainKind of(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : OTHER;
    }
}
