package org.papiricoh.supernaturalcraft.datagen.word;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The text of the Word and the Ward (v0.36): wall sigils, traps, interrogation, the crypt, the tablets, prophets, the sigils of
 * the Word and the Trials. Sound subtitles ({@link #SUBTITLES}) are read by {@code SNLanguageProvider}. Called from
 * {@code SNLang.addAll}. The Spanish lives in {@code es_es.json} under the same keys.
 */
public final class WordLang {

    private WordLang() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("word.banish_blast", "A banishing sigil blazes"),
            Map.entry("word.ward_hum", "An Enochian ward hums"),
            Map.entry("word.alarm_chime", "An alarm sigil chimes"),
            Map.entry("word.tablet_whisper", "The Word whispers"),
            Map.entry("word.glyph_wrong", "A glyph burns"),
            Map.entry("word.oil_ignite", "Holy oil catches"));

    public static void add(BiConsumer<String, String> add) {
        String m = "message.supernaturalcraft.";
        add.accept(m + "alarm_sigil.tripped", "Your alarm sigil rings: %s, near %s %s %s");
        add.accept(m + "banishing_sigil.too_weak", "You have too little blood left to paint it");
        add.accept(m + "word_mural.learnt", "The mural gives up %s letters of the Demon Tablet: %s");
        add.accept(m + "word_mural.known", "You have learnt all this mural has to teach");
        add.accept(m + "tablet_pedestal.taken", "You have taken your tablet from this pedestal");
        add.accept(m + "tablet.all_read", "You have read this tablet to its end");
        add.accept(m + "tablet.read", "%s, chapter %s: \"%s\"");
        add.accept(m + "tablet.supreme_ward", "The wards you paint from now on are supreme: twice the reach, and they burn what they cast out.");
        add.accept(m + "tablet.engraving", "You know now how to cut a devil's trap into stone.");
        add.accept(m + "tablet.complete", "The whole tablet is in you: +%s mana.");
        add.accept(m + "engraved_devils_trap.unknown", "You do not know how to cut it. The Demon Tablet does.");
        add.accept(m + "purgatory_jar.none", "You carry no empty Purgatory jar");
        add.accept(m + "trials.begin.1", "The first trial: kill a hellhound and bathe in its blood.");
        add.accept(m + "trials.begin.2", "The second trial: bring an innocent soul up out of Hell.");
        add.accept(m + "trials.begin.3", "The third trial: cure a demon. Eight injections of purified blood while it is held, an hour apart.");
        add.accept(m + "trials.done.1", "The hound's blood is on you. The first trial is done.");
        add.accept(m + "trials.done.2", "The soul goes up. The second trial is done.");
        add.accept(m + "trials.cure.hold", "It must be held fast for this");
        add.accept(m + "trials.cure.wait", "Too soon: the last injection is still working");
        add.accept(m + "trials.cure.step", "Injection %s of %s");
        add.accept(m + "trials.stopped", "You stop. What the Trials took will come back to you.");
        add.accept(m + "trials.sealed", "The gates are shut. No demon will rise within %s blocks of here.");
        add.accept(m + "trials.not_now", "It is only blood, now");
        add.accept(m + "trials.soul_found", "A soul it was tormenting clings to you. Take it out of Hell.");
        add.accept(m + "vision.letters", "You dreamt of the %s. Letters: %s");
        add.accept(m + "vision.0", "You dreamt of a door of stone that crumbled at a painted word.");
        add.accept(m + "vision.1", "You dreamt of a cage under a red sky, and of something knocking from inside.");
        add.accept(m + "vision.2", "You dreamt of a man in a black coat counting contracts by candlelight.");
        add.accept(m + "vision.3", "You dreamt of wings burning as they fell, one feather at a time.");
        add.accept(m + "vision.4", "You dreamt of a forest that never ends, and of teeth in it.");
        add.accept(m + "vision.5", "You dreamt of a hand on a tablet, and a voice that was not yours reading it.");

        add.accept("title.supernaturalcraft.prophet", "A Prophet of the Lord");
        add.accept("title.supernaturalcraft.prophet.sub", "The Word has chosen you");
        String t = "tooltip.supernaturalcraft.";
        add.accept(t + "tablet.angel", "The Word on angels. Crouch and use it to read it.");
        add.accept(t + "tablet.demon", "The Word on demons, and on the gates of Hell. Use it to read it.");
        add.accept(t + "tablet.leviathan", "The Word on the first beasts. Use it to read it.");
        add.accept(t + "tablet.chosen", "It has chosen its prophet");
        add.accept(t + "tablet.unclaimed", "It waits for the one who will read it");
        add.accept(t + "hellhound_blood", "Poured over yourself, on the first trial");
        add.accept(t + "innocent_soul", "Carry it out of Hell");
        add.accept(t + "purgatory_jar", "Empty. Seal in a Jar puts a monster in it");
        add.accept(t + "purgatory_jar_full", "Something is in it. Use it on a block to let it out");
        add.accept("item.supernaturalcraft.crypt_map", "A Confession's Map");

        for (String[] tab : new String[][]{
                {"angel", "Their wings shall fall from Heaven", "A holy sign keeps the house", "Cast out all the children of light"},
                {"demon", "Go out, every unclean spirit", "The sign in stone shall not be blotted out", "Three trials shall shut the gates of Hell"},
                {"leviathan", "Bind the first beasts in Purgatory", "Seal the monster's soul in a vessel", "Remember the dark forest"}}) {
            for (int c = 1; c <= 3; c++) add.accept("tablet.supernaturalcraft." + tab[0] + "." + c, tab[c]);
        }
        String g = "gui.supernaturalcraft.tablet.";
        add.accept(g + "read", "Read");
        add.accept(g + "chapter", "%s — chapter %s");
        add.accept(g + "solved", "The chapter is read");
        add.accept(g + "wrong", "%s glyphs are wrong");
        add.accept(g + "hint", "Click a glyph and type its letter");
        add.accept(g + "hint_prophet", "Click a glyph and type its letter. The Word is kind to you");
        add.accept(g + "known", "Known");

        add.accept("hud.supernaturalcraft.interrogation.broken", "It breaks");
        add.accept("hud.supernaturalcraft.interrogation.lost", "Too far: it is gone");
        add.accept("hud.supernaturalcraft.interrogation.resists", "%s resists");
        add.accept("hud.supernaturalcraft.allegiance.denied.warded", "An Enochian ward smothers your grace");

        String i = "interrogation.supernaturalcraft.";
        String[][] demon = {
                {"pain", "\"Is that all? I've bathed in worse.\"", "\"Ngh — you'll pay for that.\"", "\"Stop! Stop, I'll... no.\""},
                {"faith", "\"Latin. How quaint.\"", "\"Stop reading that!\"", "\"Shut the book! Please!\""},
                {"steel", "\"Cut the meat suit all you like.\"", "\"That one I felt.\"", "\"Enough steel — enough!\""},
                {"words", "\"You don't scare me, hunter.\"", "\"Threats. Cute.\"", "\"You wouldn't. Would you?\""},
                {"weak", "It hisses, and its eyes flick black.", "Smoke shudders in its throat.", "It howls. It is very close to breaking."}};
        String[][] angel = {
                {"pain", "\"Oil. You think fire frightens me?\"", "\"You defile my vessel.\"", "\"Enough. Enough!\""},
                {"faith", "\"I know those words better than you.\"", "\"You twist the Word.\"", "\"Do not — speak — that.\""},
                {"steel", "\"Steel is nothing.\"", "\"Where did you get that blade?\"", "\"Put it down. I will speak.\""},
                {"words", "\"I serve Heaven. Not you.\"", "\"Your threats are small.\"", "\"Heaven will not save me. Will it?\""},
                {"weak", "Its grace flickers behind its eyes.", "Light leaks from its mouth when it breathes.", "Its wings shudder in the dark. It is about to break."}};
        for (String[] r : demon) for (int b = 0; b < 3; b++) add.accept(i + "demon." + r[0] + "." + b, r[b + 1]);
        for (String[] r : angel) for (int b = 0; b < 3; b++) add.accept(i + "angel." + r[0] + "." + b, r[b + 1]);
        add.accept(i + "demon.lost", "It screams, and the smoke tears out of its vessel. It said nothing.");
        add.accept(i + "angel.lost", "Its grace burns out. It said nothing.");
        add.accept(i + "confess.word", "It whispers letters of the Word: %s");
        add.accept(i + "confess.crypt", "It draws you the way to a crypt of the angels.");
        add.accept(i + "confess.no_crypt", "\"There is no crypt near here. I swear it.\"");
        String[] boss = {
                "\"Azazel always went for the ones who ran. Stand your ground.\"",
                "\"Lilith's seals break where she stands. Never let her stand still.\"",
                "\"Metatron hides behind his words. Make him run out of them.\"",
                "\"Michael fights like a soldier. Soldiers follow patterns.\"",
                "\"Gabriel laughs at everything. Watch what he laughs at twice.\"",
                "\"Raphael never forgives. He never forgets oil, either.\"",
                "\"Crowley is never where he seems to be. Hit where he is going.\"",
                "\"Lucifer wants you to look at him. Do not.\""};
        for (int k = 0; k < boss.length; k++) add.accept(i + "confess.boss." + k, boss[k]);

        String d = "dialogue.supernaturalcraft.";
        add.accept(d + "interrogate.demon", "\"Fine! Fine. Ask. Just get it over with.\"");
        add.accept(d + "interrogate.demon.crypt", "Where are the angels' crypts?");
        add.accept(d + "interrogate.demon.boss", "What do you know about the princes?");
        add.accept(d + "interrogate.angel", "\"Ask, then. I will answer once.\"");
        add.accept(d + "interrogate.angel.crypt", "Where are your crypts?");
        add.accept(d + "interrogate.angel.word", "Teach me the Word.");
        add.accept(d + "interrogate.angel.boss", "What does Heaven know?");
        add.accept(d + "trials_end.choice", "One more injection, and the gates of Hell shut. It will take from you for good.");
        add.accept(d + "trials_end.choice.complete", "Finish it. Shut the gates.");
        add.accept(d + "trials_end.choice.stop", "Stop. Not like this.");

        sigil(add, "the_fall", "The Fall", "An angel struck out of the sky: slammed down, hurt and slowed as if its wings were gone.");
        sigil(add, "mass_expulsion", "Mass Expulsion", "Every angel near what it strikes is sent away, as the banishing sigil does.");
        sigil(add, "mass_exorcism", "Mass Exorcism", "Every demon near what it strikes is cast out at once.");
        sigil(add, "purgatory_bind", "Purgatory's Bind", "Held fast by Purgatory, withering, and dragged towards you.");
        sigil(add, "seal_in_jar", "Seal in a Jar", "A monster (no boss) shut in an empty Purgatory jar you carry.");

        adv(add, "first_confession", "Start Talking", "Make a trapped demon or angel confess");
        adv(add, "the_crypt", "Behind Sealed Doors", "Take the Demon Tablet from an Enochian crypt");
        adv(add, "prophet", "Prophet of the Lord", "Be chosen by a tablet of the Word");
        adv(add, "word_of_god", "The Word of God", "Read a tablet to its end");
        adv(add, "gates_sealed", "Shut the Gates", "Complete the Trials and seal the gates of Hell");
    }

    private static void sigil(BiConsumer<String, String> add, String id, String name, String desc) {
        add.accept("sigil.supernaturalcraft." + id, name);
        add.accept("sigil.supernaturalcraft." + id + ".desc", desc);
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }
}
