package org.papiricoh.supernaturalcraft.datagen.heaven;

import java.util.function.BiConsumer;

/**
 * v0.18's journal-side text (owned by the journal and data work): the advancements' titles and descriptions (journal entries
 * carry their own text in {@code JournalHeaven}). Called from {@code SNLang.addAll}.
 */
public final class HeavenLang {

    private HeavenLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        adv(add, "heavens_door", "Knockin' on Heaven's Door", "Cross a gate of light into a Heaven of your own");
        adv(add, "memory_lane", "Memory Lane", "Gather a memory in your Heaven");
        adv(add, "met_ash", "Harvelle's Roadhouse", "Find Ash behind the bar of the Roadhouse in Heaven");
        adv(add, "deprogrammed", "Deprogrammed", "Break out of Naomi's chair and bring her down");
        adv(add, "out_of_office", "Out of Office", "Close Zachariah's file for good");
        adv(add, "home_sweet_heaven", "Home Sweet Heaven", "Rest by the hearth of your home in Heaven");
        adv(add, "wild_bargain", "A Wild Bargain", "Bury a box at a natural crossroads and strike a deal");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }
}
