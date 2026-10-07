package org.papiricoh.supernaturalcraft.datagen.chuck;

import java.util.function.BiConsumer;

/** The arenas the Author writes, in English: chapter titles. Owned by the arena work. */
public final class ChuckArenaLang {

    private ChuckArenaLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        chapter(add, "eden", "Chapter One", "In the Beginning");
        chapter(add, "hell", "Chapter Two", "The Cage");
        chapter(add, "storm", "Chapter Three", "The Choir Sang");
        chapter(add, "library", "Chapter Four", "Every Word Ever Written");
        chapter(add, "blank", "Chapter Five", "The Blank Page");
    }

    private static void chapter(BiConsumer<String, String> add, String id, String number, String title) {
        add.accept("chapter.supernaturalcraft." + id, number);
        add.accept("chapter.supernaturalcraft." + id + ".title", title);
    }
}
