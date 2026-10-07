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
        AZAZEL("main/yellow_eyed", "azazel"),
        LILITH("main/lucifer_rising", "lilith"),
        LUCIFER("main/devil_went_down", "lucifer"),
        BROKEN_CHORUS("main/silence_falls", "broken_chorus"),
        METATRON("main/scribe_of_god", "metatron"),
        AMARA("main/dawn", "amara"),
        LUCIFER_UNCAGED("main/back_in_the_box", "lucifer_uncaged"),
        CHUCK("main/the_end", "chuck");

        /** Path of the advancement (in the mod's namespace) for having killed it. */
        public final String advancement;
        /** Path of its entity type, in the mod's namespace. */
        public final String entity;

        Boss(String advancement, String entity) {
            this.advancement = advancement;
            this.entity = entity;
        }

        /** @return the boss whose entity type has this path, or null */
        public static Boss byEntity(String entityPath) {
            for (Boss b : values()) if (b.entity.equals(entityPath)) return b;
            return null;
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

    /** Whether every enemy before the Author has been beaten: what "Find the Author" asks of its caster. */
    public static boolean allBeforeChuck(Predicate<String> beaten) {
        for (Boss b : Boss.values()) if (b != Boss.CHUCK && !beaten.test(b.advancement)) return false;
        return true;
    }
}
