package org.papiricoh.supernaturalcraft.crossroads;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * The order in which a hunter usually meets the great enemies, read from the advancements for
 * killing them (pure, tested in JUnit). The crossroads demon's Knowledge names the first one not
 * yet beaten, and its weakness.
 */
public final class BossProgression {

    private BossProgression() {
    }

    public enum Boss {
        AZAZEL("main/yellow_eyed"),
        LILITH("main/lucifer_rising"),
        LUCIFER("main/devil_went_down"),
        BROKEN_CHORUS("main/silence_falls"),
        METATRON("main/scribe_of_god"),
        AMARA("main/dawn"),
        LUCIFER_UNCAGED("main/back_in_the_box");

        /** Path of the advancement (in the mod's namespace) for having killed it. */
        public final String advancement;

        Boss(String advancement) {
            this.advancement = advancement;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Translation key of the book's page about it ({@code .name}, {@code .weakness}). */
        public String key() {
            return "book.supernaturalcraft.crossroads." + id();
        }
    }

    /**
     * @param beaten whether the advancement with this path (e.g. {@code main/yellow_eyed}) is done
     * @return the first boss not yet beaten, or null if all of them are
     */
    public static Boss next(Predicate<String> beaten) {
        for (Boss b : Boss.values()) if (!beaten.test(b.advancement)) return b;
        return null;
    }
}
