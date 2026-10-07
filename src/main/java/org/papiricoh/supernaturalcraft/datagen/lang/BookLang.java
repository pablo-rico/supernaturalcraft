package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.9 lang strings for the Hunter's Book frame (tabs). */
public final class BookLang {

    private BookLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept("screen.supernaturalcraft.book", "Hunter's Book");
        add.accept("screen.supernaturalcraft.book.tab.home", "The Hunter");
        add.accept("screen.supernaturalcraft.book.tab.journal", "Journal");
        add.accept("screen.supernaturalcraft.book.tab.scriptorium", "Scriptorium");
        add.accept("screen.supernaturalcraft.book.tab.roadmap", "The Road to the Cage");
    }
}
