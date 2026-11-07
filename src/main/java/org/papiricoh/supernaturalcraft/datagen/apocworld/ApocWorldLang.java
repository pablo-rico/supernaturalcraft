package org.papiricoh.supernaturalcraft.datagen.apocworld;

import org.papiricoh.supernaturalcraft.arena.ArenaTheme;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Apocalypse World's English (v0.41): its blocks' and creatures' names where the title-cased id is wrong ({@link #NAMES}), its
 * messages, AU Michael's bar, title cards and taunts, the fortress arena's barrier, the rite's JEI line, the biomes. The Spanish of
 * every key is in {@code tools/lang_fragments/apocworld.json}.
 */
final class ApocWorldLang {

    private ApocWorldLang() {
    }

    static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("au_rift", "Rift to the Apocalypse World"),
            Map.entry("au_michael_trophy", "Trophy of the Other Michael"),
            Map.entry("au_michael_greatcoat", "Michael's Greatcoat"),
            Map.entry("au_angel_soldier", "Angel Soldier"),
            Map.entry("au_prisoner", "Prisoner"),
            Map.entry("au_michael", "Michael of the Apocalypse World"),
            Map.entry("au_angel_soldier_spawn_egg", "Angel Soldier Spawn Egg"),
            Map.entry("au_prisoner_spawn_egg", "Prisoner Spawn Egg"),
            Map.entry("au_michael_spawn_egg", "Michael of the Apocalypse World Spawn Egg"));

    static void lang(BiConsumer<String, String> add) {
        add.accept("item.supernaturalcraft.michaels_orders", "Michael's Orders");
        add.accept("biome.supernaturalcraft.au_ash_plains", "Ash Plains");
        add.accept("biome.supernaturalcraft.au_burned_forest", "Burned Forest");
        add.accept("biome.supernaturalcraft.au_cratered_fields", "Cratered Fields");
        add.accept("biome.supernaturalcraft.au_broken_ridges", "Broken Ridges");
        add.accept("biome.supernaturalcraft.au_dry_riverbed", "Dry Riverbed");

        // Getting in, the camps.
        add.accept("message.supernaturalcraft.apocworld.barred", "The rift will not take you. Not yet.");
        add.accept("message.supernaturalcraft.apocworld.arrived", "Ash falls from a brown sky. This is our world, the one where the Apocalypse "
                + "was not stopped: Michael won. A rift home burns beside you.");
        add.accept("message.supernaturalcraft.apocworld.orders", "On a dead soldier at the rift: Michael's orders, and the way to his fortress "
                + "(near %s, %s). The compass points there.");
        add.accept("message.supernaturalcraft.apocworld.lock_guarded", "The lock will not give while the camp's guards stand.");
        add.accept("message.supernaturalcraft.apocworld.cage_open", "The cage is open.");
        add.accept("message.supernaturalcraft.apocworld.camp_freed", "The camp is free. Its prisoners slip away through a rift to the resistance.");

        // AU Michael.
        for (int n = 1; n <= 4; n++) add.accept("entity.supernaturalcraft.au_michael.bar.phase" + n, BARS[n - 1]);
        String[][] titles = {
                {"The General", "Michael, who won his war"},
                {"Wings Over the Court", "His soldiers answer him"},
                {"The Burned Sky", "The sky over his world is his"},
                {"The Archangel", "He rises over his fortress"}};
        for (int n = 1; n <= 4; n++) {
            add.accept("title.supernaturalcraft.au_michael.phase" + n, titles[n - 1][0]);
            add.accept("title.supernaturalcraft.au_michael.phase" + n + ".sub", titles[n - 1][1]);
        }
        add.accept("title.supernaturalcraft.au_michael.flight", "He Breaks Off");
        add.accept("title.supernaturalcraft.au_michael.flight.sub", "And the rift takes him into our world");
        String[] taunts = {
                "I won my war. I burned every one of them who would not kneel. What makes you think you are different?",
                "My Father left. My brother is in his Cage. I am all the order this world has left.",
                "Your world still has its sun. Not for long.",
                "Soldiers. Bring them to me.",
                "You fight well, for a monkey."};
        for (int i = 0; i < taunts.length; i++) add.accept("message.supernaturalcraft.au_michael.taunt." + i, taunts[i]);
        add.accept("message.supernaturalcraft.au_michael.found", "The general is on his throne. He stands up.");
        add.accept("message.supernaturalcraft.au_michael.left", "Michael sits back down on his throne. You were not worth his time.");
        add.accept("message.supernaturalcraft.au_michael.jack", "Gold light pours out of Jack and drives the archangel back.");
        add.accept("message.supernaturalcraft.au_michael.fled", "Michael is gone through the rift: into our world. Jack's grace is spent.");
        add.accept("message.supernaturalcraft.au_michael.busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.au_michael.rite.elsewhere", "Only at Michael's throne, in his fortress in the burned world.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_au_michael",
                "Calls the Michael of the Apocalypse World back to his throne for a rematch (only by the throne)");

        // The fortress's arena.
        String fortress = ArenaTheme.id(ArenaTheme.FORTRESS);
        add.accept("message.supernaturalcraft.arena.barrier." + fortress, "The court's light throws you back. The general has not dismissed you.");
        add.accept("message.supernaturalcraft.arena.no_escape." + fortress, "Michael's will holds the court shut: there is no leaving it.");
    }

    static final String[] BARS = {"Michael, the General", "Michael, Wings Over the Court", "Michael, Lord of the Burned Sky", "Michael, the Archangel"};
}
