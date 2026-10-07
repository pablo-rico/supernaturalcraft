package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.9 lang strings for the roadmap canvas (node names and hints come from {@code SNRoadmap}). */
public final class RoadmapLang {

    private RoadmapLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        String k = "screen.supernaturalcraft.book.roadmap.";
        add.accept(k + "roads.tooltip", "Choose another road");
        add.accept(k + "next", "Next");
        add.accept(k + "next.tooltip", "Centre the map on your next objective");
        add.accept(k + "fit", "Whole map");
        add.accept(k + "fit.tooltip", "Show the whole road");
        add.accept(k + "done", "Done");
        add.accept(k + "unknown", "???");
        add.accept(k + "requires", "Requires: %s");
        add.accept(k + "read", "Click to read about it");
        add.accept(k + "boss", "Boss");
        add.accept(k + "legend.done", "Done");
        add.accept(k + "legend.next", "Within reach");
        add.accept(k + "legend.locked", "Unknown");
        add.accept(k + "controls", "Drag to move, scroll to zoom");
        add.accept(k + "the_end", "Every step of this road is walked.");
        add.accept(k + "empty", "The map is blank.");
    }
}
