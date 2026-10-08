package org.papiricoh.supernaturalcraft.allegiance;

/**
 * The names and limits of each rank (pure). Angel: Lesser Angel, Seraph, Archangel, General of the Host. Demon: Crossroads
 * Demon, Prince of Hell, Knight of Hell, King of Hell. Human (hunter): Hunter, Veteran, Legend. Rank 0 is "none yet".
 */
public final class Ranks {

    /** Grace or Corruption a rank can hold (index = rank; a human holds none). */
    private static final int[] MAX_ESSENCE = {0, 100, 150, 200, 250};

    private Ranks() {
    }

    public static int maxEssence(Faction faction, int rank) {
        if (!faction.supernatural()) return 0;
        return MAX_ESSENCE[clamp(faction, rank)];
    }

    /** Max mana every rank adds, on any road (an angel, a demon or a ranked hunter): 25 a rank. */
    public static final int MANA_PER_RANK = 25;

    /** Extra max mana at a rank: what lets each rank's rite (paid in mana) be afforded by the rank below it. */
    public static int manaBonus(Faction faction, int rank) {
        return MANA_PER_RANK * clamp(faction, rank);
    }

    public static int clamp(Faction faction, int rank) {
        return Math.max(0, Math.min(faction.maxRank(), rank));
    }

    /** {@code allegiance.supernaturalcraft.rank.<faction>.<rank>}: "Seraph", "Prince of Hell", "Veteran"… (rank 0: "Human"). */
    public static String titleKey(Faction faction, int rank) {
        return "allegiance.supernaturalcraft.rank." + faction.getSerializedName() + "." + clamp(faction, rank);
    }

    /** I, II, III, IV (empty for rank 0). */
    public static String roman(int rank) {
        return switch (rank) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "";
        };
    }

    /** The advancement a rank is recorded by ({@code main/angel_2}, {@code main/demon_4}, {@code main/hunter_1}); null for rank 0. */
    public static String advancement(Faction faction, int rank) {
        if (rank <= 0) return null;
        String road = faction == Faction.HUMAN ? "hunter" : faction.getSerializedName();
        return "main/" + road + "_" + clamp(faction, rank);
    }
}
