package org.papiricoh.supernaturalcraft.datagen.legacy;

import java.util.Map;
import java.util.function.BiConsumer;

/** The research desk's server-side text (v0.17): topics, costs, results, generated-name templates, artifact traits, tooltips. Owned by agent B. */
public final class ResearchLang {

    private ResearchLang() {
    }

    /** Lore page titles (the board's {@code lore:<id>} topics). */
    static final Map<String, String> LORE = Map.ofEntries(
            Map.entry("order_history", "A History of the Order"),
            Map.entry("bunker", "The Bunker in Lebanon"),
            Map.entry("henry", "Henry Winchester"),
            Map.entry("the_key", "The Key"),
            Map.entry("case_files", "On Case Files"),
            Map.entry("cursed_objects", "Cursed Objects"),
            Map.entry("formulae", "Formulae"),
            Map.entry("rites", "Rites of the Bowl"),
            Map.entry("vampires", "Vampires"),
            Map.entry("werewolves", "Werewolves"),
            Map.entry("shapeshifters", "Shapeshifters"),
            Map.entry("devils_traps", "Devil's Traps"),
            Map.entry("the_thule", "The Thule Society"));

    static final Map<String, String[]> TRAITS = Map.ofEntries(
            Map.entry("swiftness", new String[]{"Swiftness", "You move a little faster."}),
            Map.entry("mending", new String[]{"Mending", "Your wounds close slowly on their own."}),
            Map.entry("night_eyes", new String[]{"Night Eyes", "You see in the dark."}),
            Map.entry("sixth_sense", new String[]{"Sixth Sense", "The hidden show themselves to you, as with Second Sight."}),
            Map.entry("warding", new String[]{"Warding", "Creatures' blows land a little softer (not a boss's)."}),
            Map.entry("fortune", new String[]{"Fortune", "Luck follows you."}),
            Map.entry("haste", new String[]{"Haste", "You dig and work faster."}),
            Map.entry("feather", new String[]{"Feather", "Falls hurt half as much."}),
            Map.entry("misfortune", new String[]{"Misfortune", "Bad luck dogs your steps."}),
            Map.entry("hunger", new String[]{"Hunger", "A hunger nothing fills."}),
            Map.entry("whispers", new String[]{"Whispers", "Something whispers to you; your mind frays."}),
            Map.entry("frailty", new String[]{"Frailty", "Your arms feel weak."}));

    public static void add(BiConsumer<String, String> add) {
        // Board titles: research.supernaturalcraft.topic.<kind> (args: see ResearchBoard).
        String t = "research.supernaturalcraft.topic.";
        add.accept(t + "formula", "Formula No. %s");
        add.accept(t + "rite", "Rite No. %s");
        add.accept(t + "artifact", "Identify the %s");
        add.accept(t + "creature", "%s File, Level %s");
        add.accept(t + "boss", "%s: the Order's File");
        add.accept(t + "case", "Write up Case No. %s (%s)");
        add.accept(t + "case.short", "Case No. %s");
        add.accept(t + "lore", "%s");
        String k = "research.supernaturalcraft.kind.";
        add.accept(k + "formula", "Formula");
        add.accept(k + "rite", "Rite");
        add.accept(k + "artifact", "Artifact");
        add.accept(k + "creature", "Creature File");
        add.accept(k + "boss", "Boss File");
        add.accept(k + "case", "Case");
        add.accept(k + "lore", "Archive");
        LORE.forEach((id, title) -> add.accept("research.supernaturalcraft.lore." + id, title));

        // Desk messages.
        String m = "message.supernaturalcraft.research.";
        add.accept(m + "desk_closed", "The papers on this desk are the Men of Letters'. They mean nothing to you yet.");
        add.accept(m + "not_member", "Only the Men of Letters research here.");
        add.accept(m + "no_slot", "Every desk you may use is busy.");
        add.accept(m + "not_offered", "That is not on your board.");
        add.accept(m + "cannot_pay", "You do not have what this research needs.");
        add.accept(m + "done", "Research finished: %s");

        // Field notes.
        add.accept("item.supernaturalcraft.field_notes.creature", "Field Notes: %s");
        add.accept("item.supernaturalcraft.field_notes.arcane", "Field Notes: Arcana");
        add.accept("item.supernaturalcraft.field_notes.relic", "Field Notes: Relics");
        add.accept("item.supernaturalcraft.field_notes.place", "Field Notes: Places");
        String n = "tooltip.supernaturalcraft.field_notes.";
        add.accept(n + "creature", "What you saw of it, and how it died. Pays for its file at a research desk.");
        add.accept(n + "arcane", "Sigils and rites half understood. Pays for formulae and rites.");
        add.accept(n + "relic", "Notes on cursed things. Pays for identifying artifacts.");
        add.accept(n + "place", "Old maps and older stories. Pays for the archive's lore.");
        add.accept(n + "blank", "Blank pages.");

        // Generated formulae: their names come from the archive; the rest from here.
        add.accept("sigil.supernaturalcraft.formula.desc", "A formula the Men of Letters worked out from %s: the same working, other numbers.");
        add.accept("sigil.supernaturalcraft.formula.source", "Your own research at a desk in the bunker.");

        // Cursed artifacts.
        add.accept("item.supernaturalcraft.cursed_artifact.unknown", "??? (%s)");
        add.accept("tooltip.supernaturalcraft.cursed_artifact.unknown", "What it does is anyone's guess. Research it at a desk.");
        add.accept("tooltip.supernaturalcraft.cursed_artifact.hold", "Works in the off hand or worn as a charm.");
        String a = "artifact.supernaturalcraft.";
        add.accept(a + "form.ring", "Ring");
        add.accept(a + "form.doll", "Doll");
        add.accept(a + "form.mirror", "Mirror");
        add.accept(a + "form.watch", "Pocket Watch");
        add.accept(a + "form.coin", "Coin");
        add.accept(a + "form.book", "Book");
        add.accept(a + "rarity.0", "Common");
        add.accept(a + "rarity.1", "Uncommon");
        add.accept(a + "rarity.2", "Rare");
        add.accept(a + "rarity.3", "Legendary");
        TRAITS.forEach((id, text) -> {
            add.accept(a + "trait." + id, text[0]);
            add.accept(a + "trait." + id + ".desc", text[1]);
        });
    }
}
