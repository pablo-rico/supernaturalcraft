package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.9 lang strings for the dashboard. */
public final class HomeLang {

    private HomeLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        String k = "screen.supernaturalcraft.book.home.";
        add.accept(k + "hunter", "The Hunter");
        add.accept(k + "mana", "Mana");
        add.accept(k + "sanity", "Sanity");
        add.accept(k + "grace", "Grace");
        add.accept(k + "grace.desc", "Lucifer's grace burns in you: fifty more mana.");
        add.accept(k + "void_mark", "Void-marked");
        add.accept(k + "void_mark.desc", "You drank the Eclipse Sight: the dark no longer blinds you, and your mana runs deeper.");
        add.accept(k + "boon.hearts", "+%s hearts");
        add.accept(k + "boon.mana", "+%s mana");
        add.accept(k + "boon.both", "%s, %s");
        add.accept(k + "boon.desc", "What the crossroads gave you, for good.");
        add.accept(k + "more_effects", "...and %s more");
        add.accept(k + "no_effects", "Nothing unnatural clings to you.");

        add.accept(k + "deal", "The Deal");
        add.accept(k + "deal.none", "No deal. Keep it that way.");
        add.accept(k + "deal.state.open", "Outstanding");
        add.accept(k + "deal.state.collecting", "Due: they have come to collect");
        add.accept(k + "deal.state.hunted", "Being broken");
        add.accept(k + "deal.state.free", "Settled. You got away with it");
        add.accept(k + "deal.state.collected", "Collected");
        add.accept(k + "deal.left.days", "Due in %s days, %s hours.");
        add.accept(k + "deal.left.hours", "Due in %s hours. Listen for the dogs.");
        add.accept(k + "deal.due", "Due. Any moment now.");
        add.accept(k + "deal.hounds", "The hounds are loose. Run, or stand your ground.");
        add.accept(k + "deal.hunted", "The demon who holds your contract walks again. Kill it.");

        add.accept(k + "next", "Next");
        add.accept(k + "next.none", "The road is walked. Nothing left but you.");
        add.accept(k + "next.open", "Open the road to the Cage");

        add.accept(k + "collection", "Collection");
        add.accept(k + "sigils", "Sigils known");
        add.accept(k + "bowl_spells", "Bowl spells learned");
        add.accept(k + "bosses", "Great enemies slain");
        add.accept(k + "bestiary", "Bestiary");
        add.accept(k + "unread", "Pages unread");

        add.accept(k + "card.journal", "Journal");
        add.accept(k + "card.scriptorium", "Scriptorium");
        add.accept(k + "card.roadmap", "Roadmap");

        add.accept(k + "bookmarks", "Bookmarks");
        add.accept(k + "bookmarks.none", "No pages marked yet.");
    }
}
