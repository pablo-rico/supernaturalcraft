package org.papiricoh.supernaturalcraft.memory;

/**
 * What a memory is of (v0.18, pure). The weight is how much it matters when the memory lane picks what to show; the set is the
 * collection it counts towards ({@code MemorySets}). Contract from the foundations: the memory work owns the weights and sets,
 * the scenes work draws one scene template per kind.
 */
public enum MemoryKind {
    /** A great enemy's fall: {@code subject} = its {@code BossProgression.Boss} id. */
    BOSS_VICTORY(10, "victories"),
    /** A deal at the crossroads: {@code subject} = the wish, {@code detail} = "bowl" or "wild". */
    CROSSROADS_DEAL(8, "crossroads"),
    /** A Men of Letters case solved: {@code subject} = monster entity type id, {@code detail} = scenario. */
    CASE_SOLVED(6, "cases"),
    /** A case lost: as {@link #CASE_SOLVED}. */
    CASE_LOST(5, "cases"),
    /** A rank of a faction: {@code subject} = faction id, {@code variant} = rank. */
    ASCENSION(7, "kin"),
    /** The messenger's call heeded: {@code subject} = "messenger". */
    HEEDED_CALL(7, "kin"),
    /** A rank of the Men of Letters: {@code variant} = rank. */
    LEGACY_RANK(5, "kin"),
    /** A pet lost: {@code subject} = its entity type id, {@code detail} = its name. */
    PET_LOST(6, "companions"),
    /** The first sight of a creature: {@code subject} = entity type id. */
    FIRST_SIGHTING(2, "sightings"),
    /** The creature hunted most: {@code subject} = entity type id, {@code variant} = kills. */
    FAVOURITE_PREY(4, "sightings");

    public final int weight;
    public final String set;

    MemoryKind(int weight, String set) {
        this.weight = weight;
        this.set = set;
    }
}
