package org.papiricoh.supernaturalcraft.datagen.lang;

import java.util.function.BiConsumer;

/** v0.9 lang strings for the scriptorium (spell composer, library, sigil encyclopedia). */
public final class ScriptoriumLang {

    private static final String P = "screen.supernaturalcraft.book.scriptorium.";

    private ScriptoriumLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        // The composing table.
        add.accept(P + "idle", "Choose a form to see it cast");
        add.accept(P + "name", "Name");
        add.accept(P + "name_hint", "Name your spell...");
        add.accept(P + "page", "Page");
        add.accept(P + "blank", "blank");
        add.accept(P + "on_page", "on page %s");
        add.accept(P + "same", "Already written");
        add.accept(P + "erase", "Erase");
        add.accept(P + "scrolls", "Scrolls ×%s");
        add.accept(P + "paper", "Paper %s/%s");
        add.accept(P + "ink", "Ink %s/%s");
        add.accept(P + "no_grimoire", "Hold your grimoire to inscribe or write scrolls");
        add.accept(P + "inscribed", "Inscribed on page %s");
        add.accept(P + "erased", "Page %s wiped clean");
        add.accept(P + "written", "%s scrolls written");
        add.accept(P + "no_reagents", "No reagents");
        add.accept(P + "per_cast", "%s per cast · you carry %s");
        add.accept(P + "too_much", "More than your %s mana can hold");
        add.accept(P + "click_remove", "Click to lift it off the table");
        // The right page's leaves.
        add.accept(P + "tab.sigils", "Sigils");
        add.accept(P + "tab.library", "Library");
        // The sigil encyclopedia.
        add.accept(P + "unknown", "???");
        add.accept(P + "tier", "Tier %s");
        add.accept(P + "cooldown", "Cooldown %ss");
        add.accept(P + "burns", "Burns");
        add.accept(P + "found", "Found: %s");
        add.accept(P + "not_learned", "Not yet learned");
        add.accept(P + "click_add", "Click to add it to the draft");
        add.accept(P + "index_hint", "Hover over a sigil to read about it. Click a known one to put it on the table.");
        // The library of designs.
        add.accept(P + "library.empty", "Empty");
        add.accept(P + "library.load", "Load");
        add.accept(P + "library.save", "Save");
        add.accept(P + "library.overwrite", "Overwrite?");
        add.accept(P + "library.delete", "Delete");
        add.accept(P + "library.confirm", "Sure?");
        add.accept(P + "library.hint", "%s of %s designs kept. Pick a slot to save the draft there; double-click a design to load it.");
        add.accept(P + "library.slot", "Slot %s: %s");
        add.accept(P + "library.slot_empty", "Slot %s is empty");
        add.accept(P + "library.overwrite_hint", "Click Overwrite? again to replace %s with the draft.");
        add.accept(P + "library.delete_hint", "Click Sure? again to tear out %s for good.");
        add.accept(P + "library.saved", "Draft saved in slot %s");
        add.accept(P + "library.loaded", "%s is on the table");
        add.accept(P + "library.double_click", "Double-click to load");

        // Where each sigil is found (the encyclopedia's hint for those not yet learned).
        // Kept short: the encyclopedia's card has room for about three lines.
        String starter = "Known from the start: the first spell cast from a grimoire teaches it.";
        String common = "Pages torn from slain demons: 1 in 12, or 1 in 4 from Demon Occultists.";
        String rare = "A rarer page from slain demons; Demon Occultists drop one in four.";
        source(add, "touch", starter);
        source(add, "bolt", starter);
        source(add, "mend", starter);
        source(add, "smite", "Known from the start (a grimoire teaches it). Lucifer Uncaged drops a page.");
        source(add, "burst", common);
        source(add, "frost", common);
        source(add, "repel", common);
        source(add, "reveal", common);
        source(add, "empower", common);
        source(add, "extend", common);
        source(add, "hellfire", "Slain demons' pages, and the cells of Crowley's Corridors in Hell.");
        source(add, "ward", rare);
        source(add, "bind", rare);
        source(add, "exorcise", rare);
        source(add, "widen", rare);
        source(add, "echo", "Lucifer's defeat leaves its page. Only Lucifer's Grace lets a hunter read it.");
    }

    private static void source(BiConsumer<String, String> add, String sigil, String text) {
        add.accept("sigil.supernaturalcraft." + sigil + ".source", text);
    }
}
