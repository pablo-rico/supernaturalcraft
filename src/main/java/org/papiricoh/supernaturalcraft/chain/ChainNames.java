package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.legacy.gen.GenSeed;

import java.util.Locale;
import java.util.Set;

/** Stable names independent of rank, level, or a later Fall. No boss or canonical character identities. */
public final class ChainNames {
    private static final String[] DEMON = {"Azrakel", "Malthus", "Vesh", "Corvane", "Sarrow", "Belith", "Ozrin"};
    private static final String[] ANGEL = {"Haniel", "Ithuriel", "Sariel", "Penemue", "Zophiel", "Cassiel"};
    private static final String[] DEMON_START = {"Az", "Mal", "Vesh", "Cor", "Sar", "Bel", "Oz", "Vor", "Draz", "Kel", "Ner", "Thar", "Zor", "Vel", "Mor", "Rhaz"};
    private static final String[] ANGEL_START = {"Han", "Ithur", "Sar", "Pen", "Zoph", "Cass", "Or", "Ely", "Lum", "Ari", "Tir", "Ser", "Oph", "Yev", "Mir", "Tal"};
    private static final String[] MIDDLE = {"a", "e", "i", "o", "u", "ae", "ir", "an"};
    private static final String[] DEMON_END = {"kel", "thus", "vane", "rrow", "lith", "rin", "gash", "vek", "zor", "moth", "rek", "nax"};
    private static final String[] ANGEL_END = {"iel", "uel", "iah", "mue", "riel", "niel", "thiel", "veth", "rielis", "on", "ielan", "ath"};
    private static final Set<String> BANNED = Set.of("lucifer", "azazel", "lilith", "metatron", "michael", "gabriel",
            "raphael", "naomi", "zachariah", "cain", "abaddon", "rowena", "amara", "chuck", "castiel", "crowley",
            "chorus", "uncaged", "death", "war", "famine", "pestilence", "grandwitch");

    private ChainNames() {}

    public static String name(ChainFaction faction, long nameSeed) {
        GenSeed.Rng rng = new GenSeed.Rng(GenSeed.mix(nameSeed));
        boolean demon = faction == ChainFaction.DEMON;
        String[] curated = demon ? DEMON : ANGEL;
        if (rng.nextInt(5) == 0) return curated[rng.nextInt(curated.length)];
        String[] starts = demon ? DEMON_START : ANGEL_START, ends = demon ? DEMON_END : ANGEL_END;
        String name = starts[rng.nextInt(starts.length)] + MIDDLE[rng.nextInt(MIDDLE.length)] + ends[rng.nextInt(ends.length)];
        return BANNED.contains(name.toLowerCase(Locale.ROOT)) ? curated[0] : name;
    }

    public static String name(Officer officer) { return name(officer.faction, officer.nameSeed); }

    /** Translation-key suffix: of_the_pit, of_the_host, or the_fallen; intentionally independent of rank. */
    public static String epithet(ChainFaction faction, ChainFaction.Branch branch, ChainRank rank) {
        if (faction == ChainFaction.DEMON) return "of_the_pit";
        return branch == ChainFaction.Branch.FALLEN ? "the_fallen" : "of_the_host";
    }
}
