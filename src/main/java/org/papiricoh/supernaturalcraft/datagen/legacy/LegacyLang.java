package org.papiricoh.supernaturalcraft.datagen.legacy;

import java.util.function.BiConsumer;

/**
 * The Men of Letters' client and book text (v0.17): the research desk, Henry's card, the case brief, toasts, the rank's
 * title card and the templates of the Archive's generated pages ({@code client.book.journal.ArchivePages}). Owned by agent C.
 */
public final class LegacyLang {

    private LegacyLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        String r = "screen.supernaturalcraft.research.";
        add.accept(r + "rank", "Rank: %s");
        add.accept(r + "desks", "Desks in use: %s/%s");
        add.accept(r + "board", "The Board");
        add.accept(r + "under_way", "Under Way");
        add.accept(r + "empty", "Nothing on the board. Bring field notes, and come back.");
        add.accept(r + "kind_tier", "%s, tier %s");
        add.accept(r + "cost", "Cost");
        add.accept(r + "time", "Time: %s");
        add.accept(r + "start", "Begin");
        add.accept(r + "missing", "You don't carry all of it.");
        add.accept(r + "busy", "Every desk is taken.");
        add.accept(r + "empty_desk", "An empty desk");
        add.accept(r + "finishing", "Finishing...");
        add.accept(r + "cancel", "Abandon it (nothing comes back)");

        String h = "screen.supernaturalcraft.henry";
        add.accept(h, "Henry Winchester");
        add.accept(h + ".name", "Henry Winchester");
        add.accept(h + ".more", "(click)");

        String c = "screen.supernaturalcraft.case.";
        add.accept(c + "missing", "This file is closed and filed away.");
        add.accept(c + "tab", "MEN OF LETTERS");
        add.accept(c + "suspect", "Suspect");
        add.accept(c + "known", "Also known");
        add.accept(c + "danger", "Danger");
        add.accept(c + "where", "Where");
        add.accept(c + "here", "Right here. Something is close.");
        add.accept(c + "distance", "Roughly %s blocks %s of here, near X %s, Z %s.");
        add.accept(c + "dir.north", "north");
        add.accept(c + "dir.north_east", "north-east");
        add.accept(c + "dir.east", "east");
        add.accept(c + "dir.south_east", "south-east");
        add.accept(c + "dir.south", "south");
        add.accept(c + "dir.south_west", "south-west");
        add.accept(c + "dir.west", "west");
        add.accept(c + "dir.north_west", "north-west");
        add.accept(c + "state.0", "OPEN");
        add.accept(c + "state.1", "UNDER WAY");
        add.accept(c + "state.2", "SOLVED");
        add.accept(c + "state.3", "COLD");

        String t = "toast.supernaturalcraft.legacy.";
        add.accept(t + "research_done", "Filed in the Archive");
        add.accept(t + "case_new", "A new case");
        add.accept(t + "case_solved", "Case closed");
        add.accept(t + "case_lost", "The trail went cold");
        add.accept("title.supernaturalcraft.legacy.order", "The Men of Letters");
        add.accept("title.supernaturalcraft.legacy.rank", "Rank %s");

        String b = "screen.supernaturalcraft.book.";
        add.accept(b + "tab.archive", "The Archive");
        add.accept(b + "archive.contents", "The Men of Letters");
        add.accept(b + "archive.order", "The Order");
        add.accept(b + "archive.formulae", "Formulae");
        add.accept(b + "archive.rites", "Rites");
        add.accept(b + "archive.creatures", "Creature Files");
        add.accept(b + "archive.bosses", "Boss Files");
        add.accept(b + "archive.objects", "Cursed Objects");
        add.accept(b + "archive.cases", "Case Files");

        String a = "journal.supernaturalcraft.archive.";
        add.accept(a + "formula", "A formula of the order, worked from %2$s. Tier %3$s; it costs %4$s mana and rests %5$s seconds after use.\n\n"
                + "Its words, changed: %6$s.");
        add.accept(a + "formula.reagents", "Reagents it asks for");
        add.accept(a + "formula.use", "It is in your grimoire now: compose it in the scriptorium like any sigil you know.");
        add.accept(a + "rite", "Pour: %2$s.\n\nThen add what is shown below, light the bowl and recite:\n\"%3$s\"\n\nIt costs %4$s mana. %5$s");
        add.accept(a + "rite.ingredients", "Into the bowl");
        add.accept(a + "rite.use", "The bowl knows this rite now, as it knows the ones on the old spell pages.");
        add.accept(a + "effect.apply_effect", "It lays an effect on whoever the smoke finds.");
        add.accept(a + "effect.ritual", "It works as a rite of the altar would, from the bowl.");
        add.accept(a + "artifact", "A %2$s, %3$s. The order has worked out what it does.");
        add.accept(a + "artifact.boon", "%s: %s");
        add.accept(a + "artifact.curse", "Its price, %s: %s");
        add.accept(a + "artifact.clean", "No curse could be found on it. That is rare.");
        add.accept(a + "creature.title", "File: %s");
        add.accept(a + "creature", "The order's file on %s, level %s. What you know of it makes you %s%% deadlier against it.");
        add.accept(a + "creature.note.1", "How %s moves: where it comes from, when it is about, how fast it closes.");
        add.accept(a + "creature.note.2", "Where %s is weak: the joints and the angles a blade finds first.");
        add.accept(a + "creature.note.3", "How %s fights: its tells, the moment before it strikes.");
        add.accept(a + "creature.note.4", "What %s fears, and what it ignores.");
        add.accept(a + "creature.note.5", "Accounts from the old files: hunters who met %s and lived to write it down.");
        add.accept(a + "creature.note.6", "Everything else the order has on %s. The rest is up to you.");
        add.accept(a + "case", "Case No. %s: %s\n\nThe suspect: %s. %s\n\nThe site: X %s, Z %s. Danger: %s.");
        add.accept(a + "case.written", "Written up and filed in the archive.");
        add.accept(a + "case.unwritten", "Not yet written up: a desk will take it as research.");
    }
}
