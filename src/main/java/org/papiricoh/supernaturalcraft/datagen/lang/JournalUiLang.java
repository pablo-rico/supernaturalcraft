package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.9 lang strings for the journal's index and pages. */
public final class JournalUiLang {

    private JournalUiLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        String k = "screen.supernaturalcraft.book.journal.";
        add.accept(k + "contents", "Contents");
        add.accept(k + "empty_chapter", "Nothing written here yet.");
        add.accept(k + "locked", "Not yet discovered. Keep hunting.");
        add.accept(k + "slain", "Slain: %s");
        add.accept(k + "back", "Back to contents");
        add.accept(k + "bookmark", "Bookmark this page");
        add.accept(k + "unbookmark", "Remove the bookmark");
        add.accept(k + "mana", "%s mana");
        add.accept(k + "ritual_line", "%s · %s");
        add.accept(k + "toast", "New journal entry");
        add.accept(k + "toast.more", "...and %s more entries");

        add.accept(k + "pattern.small_circle", "Small Circle");
        add.accept(k + "pattern.great_circle", "Great Circle");
        add.accept(k + "pattern.binding_circle", "Binding Circle");
        add.accept(k + "pattern.blood_circle", "Blood Circle");
        add.accept(k + "pattern.cage_circle", "Cage Circle");
        add.accept(k + "pattern.void_circle", "Void Circle");

        String c = "journal.supernaturalcraft.chapter.";
        add.accept(c + "basics", "Basics");
        add.accept(c + "sigils", "Sigils & the Grimoire");
        add.accept(c + "rituals", "Rituals");
        add.accept(c + "bowl", "The Spell Bowl");
        add.accept(c + "demons", "Demons");
        add.accept(c + "angels", "Angels");
        add.accept(c + "monsters", "Monsters & Spirits");
        add.accept(c + "bosses", "Bosses");
        add.accept(c + "arsenal", "Arsenal");
        add.accept(c + "places", "Places");
    }
}
