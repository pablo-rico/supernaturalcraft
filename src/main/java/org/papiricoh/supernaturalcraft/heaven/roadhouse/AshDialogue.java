package org.papiricoh.supernaturalcraft.heaven.roadhouse;

import java.util.ArrayList;
import java.util.List;

/**
 * What Ash says (v0.18, pure): a greeting and up to {@link #MAX_HINTS} hints picked from where the hunter stands in their
 * Heaven, the most pressing first. Keys under {@link #PREFIX}; an argument starting with {@code @} is itself a translation key
 * (see {@link AshMenu}).
 */
public final class AshDialogue {

    public static final String PREFIX = "ash.supernaturalcraft.";
    public static final int MAX_HINTS = 3;
    /** Greetings for a returning hunter (the first meeting has its own). */
    public static final int GREETINGS = 4;
    /** Ash's ramblings when there is nothing to tell. */
    public static final int RAMBLINGS = 4;

    /** Every hint name ({@code PREFIX + "hint." + name}). */
    public static final List<String> HINTS = List.of("no_plot", "wing_sealed", "naomi", "zachariah", "hearth", "homecoming",
            "memories", "next_boss", "visits", "welcome");

    /**
     * Where a hunter stands, as far as Ash can tell.
     *
     * @param firstMeeting      never talked to him before
     * @param hasPlot           they have a Heaven of their own
     * @param memories          memories in their log
     * @param gathered          memories gathered
     * @param toOpenWing        memories the clinical wing asks for
     * @param naomiWins         victories over Naomi
     * @param zachariahWins     victories over Zachariah
     * @param homeUnlocked      their home is theirs
     * @param rested            they have rested at its hearth
     * @param nextBossKey       the translation key of the next great enemy's name on the main road, or null
     * @param welcome           they welcome visitors
     * @param visitable         other Heavens they may visit now
     */
    public record Progress(boolean firstMeeting, boolean hasPlot, int memories, int gathered, int toOpenWing, int naomiWins,
                           int zachariahWins, boolean homeUnlocked, boolean rested, String nextBossKey, boolean welcome, int visitable) {
    }

    /** One line: a translation key and its arguments. */
    public record Line(String key, List<String> args) {
        static Line of(String key, String... args) {
            return new Line(key, List.of(args));
        }
    }

    private AshDialogue() {
    }

    /** The greeting key for this visit ({@code visit} counts the talks so far). */
    public static String greeting(Progress p, int visit) {
        if (p.firstMeeting()) return PREFIX + "greet.first";
        return PREFIX + "greet." + Math.floorMod(visit, GREETINGS);
    }

    /** Every hint that applies, most pressing first. */
    public static List<Line> all(Progress p) {
        List<Line> out = new ArrayList<>();
        if (!p.hasPlot()) out.add(hint("no_plot"));
        if (p.hasPlot() && p.gathered() < p.toOpenWing()) out.add(hint("wing_sealed", String.valueOf(p.toOpenWing() - p.gathered())));
        if (p.hasPlot() && p.gathered() >= p.toOpenWing() && p.naomiWins() == 0) out.add(hint("naomi"));
        if (p.naomiWins() > 0 && p.zachariahWins() == 0) out.add(hint("zachariah"));
        if (p.homeUnlocked() && !p.rested()) out.add(hint("hearth"));
        if (p.homeUnlocked() && p.rested()) out.add(hint("homecoming"));
        if (p.hasPlot() && p.memories() > p.gathered()) out.add(hint("memories", String.valueOf(p.memories() - p.gathered())));
        if (p.nextBossKey() != null && !p.nextBossKey().isEmpty()) out.add(hint("next_boss", "@" + p.nextBossKey()));
        if (p.visitable() > 0) out.add(hint("visits", String.valueOf(p.visitable())));
        else if (p.hasPlot() && !p.welcome()) out.add(hint("welcome"));
        return out;
    }

    /**
     * Up to {@link #MAX_HINTS} hints: the most pressing always first, the rest turning with {@code rotation} (asking again
     * brings the others round). Never empty: with nothing to tell, Ash rambles.
     */
    public static List<Line> hints(Progress p, int rotation) {
        List<Line> all = all(p);
        if (all.isEmpty()) return List.of(Line.of(PREFIX + "ramble." + Math.floorMod(rotation, RAMBLINGS)));
        List<Line> out = new ArrayList<>();
        out.add(all.getFirst());
        List<Line> rest = all.subList(1, all.size());
        for (int i = 0; i < rest.size() && out.size() < MAX_HINTS; i++) out.add(rest.get(Math.floorMod(rotation + i, rest.size())));
        return out;
    }

    private static Line hint(String name, String... args) {
        return Line.of(PREFIX + "hint." + name, args);
    }

    /** Every key Ash may say (the lang must have them all). */
    public static List<String> keys() {
        List<String> out = new ArrayList<>();
        out.add(PREFIX + "greet.first");
        for (int i = 0; i < GREETINGS; i++) out.add(PREFIX + "greet." + i);
        for (int i = 0; i < RAMBLINGS; i++) out.add(PREFIX + "ramble." + i);
        for (String h : HINTS) out.add(PREFIX + "hint." + h);
        return out;
    }
}
