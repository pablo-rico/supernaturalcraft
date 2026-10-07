package org.papiricoh.supernaturalcraft.author;

import java.util.ArrayList;
import java.util.List;

/**
 * Writes "The End": the chronicle of one hunter's hunt, in the Author's prose (pure, tested in JUnit). The facts come
 * from the hunter's log and advancements ({@link AuthorRewards} gathers them); the result is the manuscript's lines,
 * one paragraph each, from the title page to "THE END".
 */
public final class Chronicle {

    /** The last line of every manuscript. */
    public static final String THE_END = "THE END";

    /**
     * What the Author read in the hunter's story.
     *
     * @param hunter      the hunter's name
     * @param bosses      the great enemies they beat, by name, in the order of the road
     * @param kills       everything they killed that the journal counts
     * @param favourite   what they killed most, by name ({@code null} if nothing), and how many
     * @param creatures   how many kinds of creature they have met
     * @param spells      how many bowl spells and rites they know
     * @param dealt       whether they ever made a crossroads deal
     * @param treasures   notable things they held, by name
     */
    public record Facts(String hunter, List<String> bosses, int kills, String favourite, int favouriteKills, int creatures,
                        int spells, boolean dealt, List<String> treasures) {
    }

    private Chronicle() {
    }

    public static List<String> write(Facts f) {
        List<String> out = new ArrayList<>();
        String name = f.hunter() == null || f.hunter().isBlank() ? "the hunter" : f.hunter();
        out.add("SUPERNATURAL");
        out.add("The Gospel of " + name);
        out.add("by Carver Edlund");
        out.add("");
        out.add("Chapter One. Every story starts the same way: someone walks into the dark because somebody has to. "
                + "This time it was " + name + ", with a pocket full of salt and no idea what was waiting.");
        if (f.kills() > 0) {
            String fav = f.favourite() == null ? "" : " Most of them were " + f.favourite() + ": " + f.favouriteKills() + " of those alone.";
            out.add("The road was long. " + name + " put down " + plural(f.kills(), "thing") + " that should not have been walking around,"
                    + fav + " The readers liked those chapters. Monster of the week. It never gets old.");
        } else {
            out.add("The strange thing is how little blood there was. " + name + " got here by reading, mostly. "
                    + "I respect that, as a writer.");
        }
        if (f.creatures() > 0) {
            out.add(name + " met " + plural(f.creatures(), "kind") + " of creature along the way and wrote every one of them down. "
                    + "Good notes. Better than mine, some days.");
        }
        if (f.spells() > 0) {
            out.add("There was magic, too: " + plural(f.spells(), "spell") + " and rite" + (f.spells() == 1 ? "" : "s")
                    + " learned by candlelight, the Latin mispronounced only a little.");
        }
        if (f.dealt()) {
            out.add("And there was a crossroads. There is always a crossroads. " + name + " made the deal, heard the dogs, "
                    + "and lived with it. I'd call that character development.");
        }
        List<String> b = f.bosses() == null ? List.of() : f.bosses();
        if (!b.isEmpty()) {
            StringBuilder s = new StringBuilder("Then the big ones. ");
            for (int i = 0; i < b.size(); i++) {
                s.append(i == 0 ? "First " : i == b.size() - 1 ? "And finally " : "Then ").append(b.get(i)).append(". ");
            }
            s.append("Each one was supposed to be the end. None of them were. That is the trick of a series.");
            out.add(s.toString());
        }
        if (f.treasures() != null && !f.treasures().isEmpty()) {
            out.add("Things picked up on the way, for the props department: " + String.join(", ", f.treasures()) + ".");
        }
        out.add("And at the very end, a cabin. A man in a bathrobe. A typewriter. " + name
                + " could have stayed home. Instead they knocked, and they listened, and when he asked if they were ready, they said yes.");
        out.add("I have written a lot of endings. Most of them I tore up. This one I am keeping.");
        out.add(THE_END);
        return out;
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }
}
