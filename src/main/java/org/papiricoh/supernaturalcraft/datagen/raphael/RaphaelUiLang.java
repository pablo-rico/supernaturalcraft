package org.papiricoh.supernaturalcraft.datagen.raphael;

import java.util.function.BiConsumer;

/** Raphael's texts on the client (v0.16): the title cards of his three phases and his fall. English only. */
public final class RaphaelUiLang {

    private RaphaelUiLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept("title.supernaturalcraft.raphael.phase1", "The Storm");
        add.accept("title.supernaturalcraft.raphael.phase1.sub", "Raphael, archangel of the Lord, has come down to finish it");
        add.accept("title.supernaturalcraft.raphael.phase2", "The Healer");
        add.accept("title.supernaturalcraft.raphael.phase2.sub", "His garrison holds him up: cut the threads");
        add.accept("title.supernaturalcraft.raphael.phase3", "Wrath of Heaven");
        add.accept("title.supernaturalcraft.raphael.phase3.sub", "The roof is gone, and the sky is his");
        add.accept("title.supernaturalcraft.raphael.death", "The Storm Breaks");
        add.accept("title.supernaturalcraft.raphael.death.sub", "Raphael falls, and leaves his wings burnt on the floor");
    }
}
