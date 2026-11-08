package org.papiricoh.supernaturalcraft.hands;

import org.papiricoh.supernaturalcraft.hands.mob.ChittersRules;

/**
 * When season 11's wild creatures rise (v0.42), pure. The slice's own spawner ({@link HandsSpawns}) asks these once in a while
 * round each hunter in the Overworld; nothing of this is in the biomes' spawn lists.
 * <ul>
 *   <li>A banshee: only while the Darkness is loose (the sun dying), by night, where she haunts (swamps, mangroves, dark woods).</li>
 *   <li>Chitters: once season 11 has begun in the world, in the awake days of their cycle, deep in the caves and in pitch dark.</li>
 * </ul>
 */
public final class HandsSpawnRules {

    /** How often the spawner looks round each hunter, in ticks, and the chance it tries each time. */
    public static final int PERIOD = 200;
    public static final double BANSHEE_CHANCE = 0.12, CHITTER_CHANCE = 0.25;
    /** At most this many banshees round one hunter (64 blocks). */
    public static final int BANSHEE_CAP = 1;
    /** How far from the hunter a creature rises. */
    public static final int MIN_DISTANCE = 18, MAX_DISTANCE = 32;

    private HandsSpawnRules() {
    }

    /** Whether a banshee may rise: the Darkness loose, night, a place she haunts. */
    public static boolean banshee(boolean darknessLoose, boolean night, boolean haunt) {
        return darknessLoose && night && haunt;
    }

    /** Whether a Chitter pack may rise at height {@code y} with that light on world day {@code day} (season 11 begun). */
    public static boolean chitters(boolean seasonOpen, long day, int y, int skyLight, int blockLight) {
        return seasonOpen && ChittersRules.awake(day) && ChittersRules.darkDeep(y, skyLight, blockLight);
    }
}
