package org.papiricoh.supernaturalcraft.legacy.gen;

import org.papiricoh.supernaturalcraft.legacy.LegacyAssets;
import org.papiricoh.supernaturalcraft.legacy.artifact.ArtifactData;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursed objects (v0.17), pure: a seed gives a form, a Latin name, one or two boons, maybe a curse, and a rarity. Rarer objects
 * carry two boons and are less often cursed. What each trait does is {@code legacy.artifact.ArtifactTraits}.
 */
public final class ArtifactGenerator {

    /** Boon ids (help the holder). */
    public static final List<String> BOONS = List.of("swiftness", "mending", "night_eyes", "sixth_sense", "warding", "fortune",
            "haste", "feather");
    /** Curse ids (hurt the holder). */
    public static final List<String> CURSES = List.of("misfortune", "hunger", "whispers", "frailty");

    public static final int MAX_RARITY = 3;
    /** Weight of each rarity when rolled (common … legendary). */
    static final int[] RARITY_WEIGHTS = {60, 28, 10, 2};
    /** Chance of a curse by rarity. */
    static final double[] CURSE_CHANCE = {0.75, 0.6, 0.45, 0.35};

    private ArtifactGenerator() {
    }

    /** An unidentified artifact from {@code seed}, its rarity at most {@code maxRarity} (0–3). */
    public static ArtifactData roll(long seed, int maxRarity) {
        GenSeed.Rng r = new GenSeed.Rng(GenSeed.mix(seed ^ 0xA27F1C7L));
        int cap = Math.max(0, Math.min(MAX_RARITY, maxRarity));
        int rarity = Math.min(cap, r.weighted(RARITY_WEIGHTS));
        String form = r.pick(LegacyAssets.ARTIFACT_FORMS);
        int boonCount = rarity >= 2 ? 2 : (rarity == 1 && r.chance(0.5) ? 2 : 1);
        List<String> pool = new ArrayList<>(BOONS);
        List<String> boons = new ArrayList<>();
        for (int i = 0; i < boonCount; i++) boons.add(pool.remove(r.nextInt(pool.size())));
        String curse = r.chance(CURSE_CHANCE[rarity]) ? r.pick(CURSES) : "";
        return new ArtifactData(seed, form, LatinNames.artifactName(form, seed), boons, curse, rarity, false);
    }

    /** The research tier of identifying an artifact (rarity 0 → I … 3 → IV). */
    public static int tier(int rarity) {
        return Math.max(1, Math.min(5, rarity + 1));
    }
}
