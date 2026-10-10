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
        /** Optional (v0.14): a side road after Lucifer, asked for by nothing. */
        GABRIEL("main/changing_channels", "gabriel", true),
        WAR("main/war", "war"),
        FAMINE("main/famine", "famine"),
        PESTILENCE("main/pestilence", "pestilence"),
        /** Optional (v0.16): the archangel of the storm, a side road once the three Horsemen have fallen. */
        RAPHAEL("main/free_to_be_you_and_me", "raphael", true),
        BROKEN_CHORUS("main/silence_falls", "broken_chorus"),
        METATRON("main/scribe_of_god", "metatron"),
        /** Optional (v0.18): Heaven's reprogrammer, in the clinical wing of a hunter's own Heaven once Metatron has fallen. */
        NAOMI("main/deprogrammed", "naomi", true),
        /** Optional (v0.18): the angel of Heaven's paperwork, in his endless office above a hunter's own Heaven. */
        ZACHARIAH("main/out_of_office", "zachariah", true),
        AMARA("main/dawn", "amara"),
        DEATH("main/pale_rider", "death"),
        LUCIFER_UNCAGED("main/back_in_the_box", "lucifer_uncaged"),
        MICHAEL("main/sword_of_heaven", "michael"),
        CHUCK("main/the_end", "chuck");

        /** Path of the advancement (in the mod's namespace) for having killed it. */
        public final String advancement;
        /** Path of its entity type, in the mod's namespace. */
        public final String entity;
        /** A side road: never "the next" enemy and never asked for by the Author. */
        public final boolean optional;

        Boss(String advancement, String entity) {
            this(advancement, entity, false);
        }

        Boss(String advancement, String entity, boolean optional) {
            this.advancement = advancement;
            this.entity = entity;
            this.optional = optional;
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
     * @return the first boss on the main road not yet beaten (optional ones are never next), or null if all of them are
     */
    public static Boss next(Predicate<String> beaten) {
        for (Boss b : Boss.values()) if (!b.optional && !beaten.test(b.advancement)) return b;
        return null;
    }

    /** Whether every enemy before the Author has been beaten: what "Find the Author" asks of its caster. */
    public static boolean allBeforeChuck(Predicate<String> beaten) {
        for (Boss b : Boss.values()) if (b != Boss.CHUCK && !b.optional && !beaten.test(b.advancement)) return false;
        return true;
    }
}
