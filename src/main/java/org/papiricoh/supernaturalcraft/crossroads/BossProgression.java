package org.papiricoh.supernaturalcraft.crossroads;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * The order in which a hunter usually meets the great enemies, read from the advancements for
 * killing them (pure, tested in JUnit). v0.38: the order of the roadmap's chapters (the road to the Cage). The crossroads demon's Knowledge names the first one not
 * yet beaten, and its weakness.
 */
public final class BossProgression {

    private BossProgression() {
    }

    public enum Boss {
        AZAZEL("main/yellow_eyed", "azazel"),
        /** v0.40: the torturer the angels hold in a devil's trap at the warehouse (season 4, On the Head of a Pin). */
        ALASTAIR("main/on_the_head_of_a_pin", "alastair"),
        /** Optional (v0.40): the angel who kills angels for Lucifer, at his garrison once two angel seals were held. */
        URIEL("main/heavens_traitor", "uriel", true),
        /** Season 4's finale: her death is the last seal and Lucifer rises. */
        LILITH("main/lucifer_rising", "lilith"),
        /** Optional (v0.14): a side road after Lilith since v0.40 (season 5), asked for by nothing. */
        GABRIEL("main/changing_channels", "gabriel", true),
        WAR("main/war", "war"),
        FAMINE("main/famine", "famine"),
        PESTILENCE("main/pestilence", "pestilence"),
        DEATH("main/pale_rider", "death"),
        /** Season 5's finale since v0.40: the four rings open the Cage and he falls into it (Swan Song). */
        LUCIFER("main/devil_went_down", "lucifer"),
        /** The archangel of the storm, once the three Horsemen have fallen (v0.16; on the main road since v0.39: the war in Heaven). */
        RAPHAEL("main/free_to_be_you_and_me", "raphael"),
        /** v0.39: Castiel, full of Purgatory's souls and the Leviathans, at the lab where the door opened (season 6). */
        GODSTIEL("main/the_man_who_knew_too_much", "godstiel"),
        /** The head of the Leviathans, called with the righteous bone once they are out (v0.25; main road since v0.39). */
        DICK_ROMAN("main/survival_of_the_fittest", "dick_roman"),
        /** v0.39: Eve, the Mother of All, in her glade in Purgatory (where Dick Roman's death throws the hunters). */
        EVE("main/mommy_dearest", "eve"),
        /** Optional (v0.35): the King of the Crossroads in his asylum, found by a Men of Letters case once Dick Roman has fallen. */
        CROWLEY("main/king_of_the_crossroads", "crowley", true),
        BROKEN_CHORUS("main/silence_falls", "broken_chorus"),
        METATRON("main/scribe_of_god", "metatron"),
        /** Optional (v0.18): Heaven's reprogrammer, in the clinical wing of a hunter's own Heaven once Metatron has fallen. */
        NAOMI("main/deprogrammed", "naomi", true),
        /** Optional (v0.18): the angel of Heaven's paperwork, in his endless office above a hunter's own Heaven. */
        ZACHARIAH("main/out_of_office", "zachariah", true),
        /** v0.21: the Father of Murder at his farm, once Metatron has fallen (v0.38); he gives the Mark. On the main road since v0.42. */
        CAIN("main/first_born", "cain"),
        /** v0.21: the Knight of Hell, called up once Cain and the three Horsemen have fallen; she leaves the Book (main road, v0.42). */
        ABADDON("main/king_of_the_damned", "abaddon"),
        /**
         * v0.21, P6: the witch boss slot after Abaddon, in two variants (one per hunter: {@code coven.ending.WitchBossSlot}); on the
         * main road since v0.42 as ONE step ({@link #WITCH_SLOT}). Rowena, at her house, for a hunter who kept the Book of the
         * Damned...
         */
        ROWENA("main/rowena_defeated", "rowena", WITCH_SLOT),
        /** ...or the Grand Witch, called by the Grand Coven's rite, for one who gave Rowena the Book. */
        GRAND_WITCH("main/grand_witch_defeated", "grand_witch", WITCH_SLOT),
        AMARA("main/dawn", "amara"),
        /** Optional (v0.41, season 12): the British Men of Letters' hound, who hunts a hunter that turned Dr. Hess down (or was betrayed). */
        KETCH("main/the_british_invasion", "ketch", true),
        /** Optional (v0.41, season 12): Dr. Hess, at the British Men of Letters' compound. */
        HESS("main/who_we_are", "hess", true),
        /** v0.41 (season 12's finale): the Prince of Hell who guards Kelly Kline; her death brings the nephilim into the world. */
        DAGON("main/all_along_the_watchtower", "dagon"),
        /** Optional (v0.41, season 13): the Prince of Hell who would be King, with Gabriel in his cellar and a shard of the Lance. */
        ASMODEUS("main/bring_em_back_alive", "asmodeus", true),
        /** v0.41 (season 13): the Michael of the Apocalypse World, at his fortress; beaten, he flees through the rift. */
        AU_MICHAEL("main/let_the_good_times_roll", "au_michael"),
        /** v0.41 (season 14): the same Michael on our side, in a vessel (the hunter who said yes, or one he took). */
        MICHAEL_VESSEL("main/the_spear", "michael_vessel"),
        /** v0.41 (season 14's finale): Jack, without his soul. */
        SOULLESS_JACK("main/moriah", "soulless_jack"),
        /** v0.41: the Shadow, the Empty's keeper, woken in the Empty itself. */
        SHADOW("main/despair", "shadow"),
        /** Back in the Box (v0.41: the bridge from season 14 to 15, after the Empty): Lucifer put back in the Cage... */
        LUCIFER_UNCAGED("main/back_in_the_box", "lucifer_uncaged"),
        /** ...and Michael, the Sword of Heaven. */
        MICHAEL("main/sword_of_heaven", "michael"),
        CHUCK("main/the_end", "chuck");

        /** Path of the advancement (in the mod's namespace) for having killed it. */
        public final String advancement;
        /** Path of its entity type, in the mod's namespace. */
        public final String entity;
        /** A side road: never "the next" enemy and never asked for by the Author. */
        public final boolean optional;
        /**
         * v0.42: the main-road step this boss shares with others (one per hunter, whichever their road gives them), or null.
         * A slot counts once on the road and is done when any of its members is beaten ({@link #stepBeaten}).
         */
        public final String slot;

        Boss(String advancement, String entity) {
            this(advancement, entity, false, null);
        }

        Boss(String advancement, String entity, boolean optional) {
            this(advancement, entity, optional, null);
        }

        Boss(String advancement, String entity, String slot) {
            this(advancement, entity, false, slot);
        }

        Boss(String advancement, String entity, boolean optional, String slot) {
            this.advancement = advancement;
            this.entity = entity;
            this.optional = optional;
            this.slot = slot;
        }

        /** Whether this boss is a step of the main road (not a side road). */
        public boolean onMainRoad() {
            return !optional;
        }

        /** The first member of this boss's slot (itself when it has none): who stands for the step on the road. */
        public Boss step() {
            if (slot == null) return this;
            for (Boss b : values()) if (slot.equals(b.slot)) return b;
            return this;
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
        for (Boss b : mainRoad()) if (!stepBeaten(b, beaten)) return b;
        return null;
    }

    /** Whether every enemy before the Author has been beaten: what "Find the Author" asks of its caster. */
    public static boolean allBeforeChuck(Predicate<String> beaten) {
        for (Boss b : mainRoad()) if (b != Boss.CHUCK && !stepBeaten(b, beaten)) return false;
        return true;
    }

    /** The witch slot (v0.42): Rowena or the Grand Witch, whichever the hunter's choice of the Book gives them. */
    public static final String WITCH_SLOT = "witch";
    /**
     * Taking the Mark off ({@code coven.ending.WitchBossSlot.UNMARKED}) also settles the witch step: the hunter who did it beat
     * their witch (and migrated hunters are granted it).
     */
    public static final String UNMARKED = "main/unmarked";

    /**
     * The main road in order, one boss per step: side roads left out and each slot once (by its first member, see
     * {@link Boss#step()}).
     */
    public static java.util.List<Boss> mainRoad() {
        java.util.List<Boss> out = new java.util.ArrayList<>();
        java.util.Set<String> slots = new java.util.HashSet<>();
        for (Boss b : Boss.values()) {
            if (b.optional) continue;
            if (b.slot != null && !slots.add(b.slot)) continue;
            out.add(b);
        }
        return java.util.List.copyOf(out);
    }

    /** Whether the main-road step of {@code boss} is done: the boss itself, or any member of its slot (or the Mark taken off, for the witch's). */
    public static boolean stepBeaten(Boss boss, Predicate<String> beaten) {
        if (boss.slot == null) return beaten.test(boss.advancement);
        if (WITCH_SLOT.equals(boss.slot) && beaten.test(UNMARKED)) return true;
        for (Boss b : Boss.values()) if (boss.slot.equals(b.slot) && beaten.test(b.advancement)) return true;
        return false;
    }
}
