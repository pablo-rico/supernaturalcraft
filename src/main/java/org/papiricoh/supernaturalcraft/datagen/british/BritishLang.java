package org.papiricoh.supernaturalcraft.datagen.british;

import org.papiricoh.supernaturalcraft.british.BritishDialogue;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Season 12's English (v0.41): the British Men of Letters' ultimatum, Ketch, Hess, their arsenal, Dagon and Jack's birth.
 * Called from {@link BritishData#lang}. The Spanish lives in {@code tools/lang_fragments/british.json} under the same keys.
 */
final class BritishLang {

    private BritishLang() {
    }

    /** English names by registry id where the title-cased id is wrong. */
    static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("ketch", "Arthur Ketch"),
            Map.entry("hess", "Dr. Mildred Hess"),
            Map.entry("hess_envoy", "Dr. Hess"),
            Map.entry("british_agent", "British Men of Letters Agent"),
            Map.entry("kelly_kline", "Kelly Kline"),
            Map.entry("rourke_lucifer", "Lucifer (in President Rourke)"),
            Map.entry("british_pistol", "British Suppressed Pistol"),
            Map.entry("british_dossier", "Ketch's Dossier"),
            Map.entry("hess_folio", "Hess's Folio"),
            Map.entry("dagons_brand", "Dagon's Brand"),
            Map.entry("ketch_trophy", "Ketch's Bust"),
            Map.entry("hess_trophy", "Hess's Bust"),
            Map.entry("dagon_trophy", "Dagon's Bust"),
            Map.entry("british_agent_spawn_egg", "British Agent Spawn Egg"));

    /** Subtitles by sound path. */
    static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("ketch.shot", "Suppressed shot"),
            Map.entry("ketch.knife", "Knife slashes"),
            Map.entry("ketch.grenade", "Hex grenade bursts"),
            Map.entry("ketch.smoke", "Smoke canister hisses"),
            Map.entry("ketch.hurt", "Ketch grunts"),
            Map.entry("ketch.death", "Ketch falls"),
            Map.entry("hess.cast", "Hess casts"),
            Map.entry("hess.ward", "A ward rises"),
            Map.entry("hess.command", "Hess gives an order"),
            Map.entry("hess.hex", "A hex bag bursts"),
            Map.entry("hess.hurt", "Hess hurts"),
            Map.entry("hess.death", "Hess falls"),
            Map.entry("dagon.fling", "Air bends"),
            Map.entry("dagon.crush", "A fist closes"),
            Map.entry("dagon.fire", "Hellfire roars"),
            Map.entry("dagon.scream", "Dagon shrieks"),
            Map.entry("dagon.hurt", "Dagon hisses"),
            Map.entry("dagon.death", "Dagon burns"),
            Map.entry("british_agent.shot", "Carbine fires"),
            Map.entry("british_agent.hurt", "Agent hurts"),
            Map.entry("british_agent.death", "Agent dies"),
            Map.entry("kelly_kline.labor", "Kelly cries out"),
            Map.entry("kelly_kline.glow", "Light pours out"),
            Map.entry("kelly_kline.cry", "A newborn cries"));

    static void add(BiConsumer<String, String> add) {
        road(add);
        ultimatum(add);
        arsenal(add);
        ketch(add);
        hess(add);
        dagon(add);
        birth(add);
        add.accept("message.supernaturalcraft.arena.barrier.british_hq", "The compound's wards hold you in.");
        add.accept("message.supernaturalcraft.arena.no_escape.british_hq", "\"Nobody walks out of the Old Ways, dear.\"");
        add.accept("message.supernaturalcraft.arena.barrier.ambush", "Ketch's smoke closes round you.");
        add.accept("message.supernaturalcraft.arena.no_escape.ambush", "\"Running? Rather beneath you.\"");
        add.accept("message.supernaturalcraft.arena.barrier.safehouse", "The air thickens like a hand: Dagon holds you here.");
        add.accept("message.supernaturalcraft.arena.no_escape.safehouse", "\"You don't leave until I say.\"");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.trace_the_nephilim", "Traces the nephilim's mother: a compass to Kelly Kline's safe house");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_ketch", "Calls Ketch back, wherever you stand, by night");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_hess", "Calls Hess back to the British compound");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_dagon", "Calls Dagon back to Kelly Kline's safe house");
    }

    private static void road(BiConsumer<String, String> add) {
        add.accept("message.supernaturalcraft.british.joined", "You work with the British Men of Letters now. They have given you their kit. London "
                + "expects results.");
        add.accept("message.supernaturalcraft.british.refused", "You turned Dr. Hess down. The British do not take no for an answer: watch the "
                + "dark. They will send someone.");
        add.accept("message.supernaturalcraft.british.bunker_threatened", "Hess knows where the American chapter's bunker is. She means to "
                + "have it.");
        add.accept("message.supernaturalcraft.british.betrayed", "A message from London: your services are no longer required. Hess has decided "
                + "you are a liability, and Ketch has been sent.");
        add.accept("message.supernaturalcraft.british.compound", "Ketch's dossier: the British compound, an old manor near %s, %s. Hess waits "
                + "in its war room.");
        add.accept("message.supernaturalcraft.british.safehouse", "The trace finds the nephilim's mother: a remote cabin near %s, %s. A Prince "
                + "of Hell keeps watch over it.");
        add.accept("message.supernaturalcraft.british.trace.nobody", "Someone has to work the trace.");
        add.accept("message.supernaturalcraft.british.trace.unknown", "You don't know what you are looking for. Not yet.");
        add.accept("message.supernaturalcraft.british.trace.born", "The child is already born.");
        add.accept("item.supernaturalcraft.british_compass", "The British Compound");
        add.accept("item.supernaturalcraft.nephilim_compass", "The Nephilim's Trace");
    }

    /** Dr. Hess at dawn (dialogue {@code british_ultimatum}). */
    private static void ultimatum(BiConsumer<String, String> add) {
        String d = "dialogue.supernaturalcraft." + BritishDialogue.ULTIMATUM + ".";
        add.accept(d + "offer", "Dr. Mildred Hess, of the British Men of Letters. You hunters in America are a disgrace: no code, no discipline, "
                + "and the Darkness nearly ate the world on your watch. We are taking over. Work with us, or step aside.");
        add.accept(d + "offer.terms", "What are your terms?");
        add.accept(d + "offer.refuse", "No.");
        add.accept(d + "offer_letters", "You carry the key of our American chapter. A bunker full of books and nobody fit to read them. We are "
                + "taking over the hunting here, and that bunker with it. Work with us, and you may keep your little library.");
        add.accept(d + "offer_letters.terms", "What are your terms?");
        add.accept(d + "offer_letters.refuse", "The bunker is ours.");
        add.accept(d + "terms", "Our code, our rules, our kit. Enchanted rounds, hex grenades, a proper weapon. You hunt what we tell you "
                + "to hunt, the way we tell you to hunt it.");
        add.accept(d + "terms.join", "Fine. I'm in.");
        add.accept(d + "terms.refuse", "I'll hunt my own way.");
        add.accept(d + "slip_joined", "Splendid. Your first task, then: we have reason to believe a nephilim is about to be born. Lucifer's "
                + "child. If you find the mother before we do, you will tell us.");
        add.accept(d + "slip_joined.go", "Understood.");
        add.accept(d + "slip_refused", "Pity. Do keep out of our way. We have bigger problems than you: a nephilim is about to be born, Lucifer's "
                + "child, and somebody has to deal with it.");
        add.accept(d + "slip_refused.go", "We'll see.");
    }

    private static void arsenal(BiConsumer<String, String> add) {
        add.accept("tooltip.supernaturalcraft.british_pistol", "Fires enchanted rounds (from your inventory); each one carries a hex that slows "
                + "and weakens. A boss only takes the round.");
        add.accept("tooltip.supernaturalcraft.hex_grenade", "Throw it: it bursts into a hex that slows, weakens and blinds. A boss only takes a "
                + "little of it.");
        add.accept("tooltip.supernaturalcraft.british_dossier", "Read it for a compass to the British compound.");
        add.accept("tooltip.supernaturalcraft.hess_folio", "One ward of the Old Ways, read aloud once: you are warded, and every foe near is hexed.");
        add.accept("tooltip.supernaturalcraft.dagons_brand", "Raise it for a ring of hellfire round you; the flames leave you alone for a while.");
        add.accept("message.supernaturalcraft.british_pistol.empty", "No enchanted rounds.");
        add.accept("message.supernaturalcraft.hess_folio.read", "The words of the Old Ways close round you like armour.");
    }

    private static void ketch(BiConsumer<String, String> add) {
        String[] bars = {"Arthur Ketch, the Operative", "Arthur Ketch, the Hound", "Arthur Ketch, Nothing Personal"};
        for (int i = 0; i < bars.length; i++) add.accept("entity.supernaturalcraft.ketch.bar.phase" + (i + 1), bars[i]);
        String[][] titles = {{"Arthur Ketch", "Of the British Men of Letters."}, {"The Hound", "The knife comes out."},
                {"Nothing Personal", "Smoke, and a polite voice behind you."}};
        for (int i = 0; i < titles.length; i++) {
            add.accept("title.supernaturalcraft.ketch.phase" + (i + 1), titles[i][0]);
            add.accept("title.supernaturalcraft.ketch.phase" + (i + 1) + ".sub", titles[i][1]);
        }
        add.accept("title.supernaturalcraft.ketch.fall", "Ketch Is Down");
        add.accept("title.supernaturalcraft.ketch.fall.sub", "\"Well played.\"");
        String[] taunts = {"Ketch: \"Nothing personal. Orders.\"", "Ketch: \"I do enjoy a professional.\"",
                "Ketch: \"Hold still, there's a good chap.\"", "Ketch: \"You Americans. All instinct, no training.\"",
                "Ketch: \"Dr. Hess sends her regards.\""};
        for (int i = 0; i < taunts.length; i++) add.accept("message.supernaturalcraft.ketch.taunt." + i, taunts[i]);
        add.accept("message.supernaturalcraft.ketch.ambush", "A suppressed click in the dark. Arthur Ketch has found you.");
        add.accept("message.supernaturalcraft.ketch.left", "Ketch straightens his cuffs and walks off into the dark. He will be back.");
        add.accept("message.supernaturalcraft.ketch.defeated", "Ketch is down. In his jacket: a dossier on the British compound.");
        add.accept("message.supernaturalcraft.ketch.rite.day", "Ketch only answers by night, and only in this world.");
        add.accept("message.supernaturalcraft.ketch.busy", "There is no room for him here.");
    }

    private static void hess(BiConsumer<String, String> add) {
        String[] bars = {"Dr. Hess, the Director", "Dr. Hess, the Old Ways", "Dr. Hess, the Folio"};
        for (int i = 0; i < bars.length; i++) add.accept("entity.supernaturalcraft.hess.bar.phase" + (i + 1), bars[i]);
        String[][] titles = {{"Dr. Mildred Hess", "Head of the British Men of Letters."}, {"The Old Ways", "Her wards rise."},
                {"The Folio", "She reads a curse aloud."}};
        for (int i = 0; i < titles.length; i++) {
            add.accept("title.supernaturalcraft.hess.phase" + (i + 1), titles[i][0]);
            add.accept("title.supernaturalcraft.hess.phase" + (i + 1) + ".sub", titles[i][1]);
        }
        add.accept("title.supernaturalcraft.hess.fall", "Hess Falls");
        add.accept("title.supernaturalcraft.hess.fall.sub", "Her glasses on the war room floor.");
        add.accept("message.supernaturalcraft.hess.intro", "Hess: \"I did warn you. Such a pity.\"");
        add.accept("message.supernaturalcraft.hess.phase2", "Hess: \"We have had a thousand years to learn this. You have had a weekend.\"");
        add.accept("message.supernaturalcraft.hess.phase3", "Hess opens her folio: \"Let us finish this properly.\"");
        add.accept("message.supernaturalcraft.hess.folio", "Hess reads aloud from the folio. Find the one place the curse spares.");
        add.accept("message.supernaturalcraft.hess.dying", "Hess: \"You have no idea... what is coming.\"");
        add.accept("message.supernaturalcraft.hess.left", "Hess closes her folio: \"We are done here.\" Her agents see her out.");
        add.accept("message.supernaturalcraft.hess.defeated", "Hess is dead. The British Men of Letters in America are finished.");
        add.accept("message.supernaturalcraft.hess.files", "You carry off London's files from the war room: they go into the Archive.");
        add.accept("message.supernaturalcraft.hess.found", "The war room. Dr. Hess is waiting at the head of the table.");
        add.accept("message.supernaturalcraft.hess.rite.elsewhere", "Hess only answers at the British compound, by its war room.");
        add.accept("message.supernaturalcraft.hess.busy", "There is no room for her here.");
    }

    private static void dagon(BiConsumer<String, String> add) {
        String[] bars = {"Dagon, Prince of Hell", "Dagon, Hellfire", "Dagon, the Guard Unbroken"};
        for (int i = 0; i < bars.length; i++) add.accept("entity.supernaturalcraft.dagon.bar.phase" + (i + 1), bars[i]);
        String[][] titles = {{"Dagon", "Prince of Hell. Lucifer's guard dog."}, {"Hellfire", "The air burns."},
                {"The Guard Unbroken", "She lifts from the ground."}};
        for (int i = 0; i < titles.length; i++) {
            add.accept("title.supernaturalcraft.dagon.phase" + (i + 1), titles[i][0]);
            add.accept("title.supernaturalcraft.dagon.phase" + (i + 1) + ".sub", titles[i][1]);
        }
        add.accept("title.supernaturalcraft.dagon.fall", "Dagon Burns");
        add.accept("title.supernaturalcraft.dagon.fall.sub", "And behind the door, a child is coming.");
        add.accept("message.supernaturalcraft.dagon.intro", "Dagon: \"The child is not yours. Walk away, little hunter.\"");
        add.accept("message.supernaturalcraft.dagon.phase2", "Dagon: \"I was old when your kind learned fire.\"");
        add.accept("message.supernaturalcraft.dagon.phase3", "Dagon rises off the ground: \"Enough.\"");
        add.accept("message.supernaturalcraft.dagon.dying", "Dagon's eyes flare, and she burns from inside.");
        add.accept("message.supernaturalcraft.dagon.left", "Dagon turns back to the door: \"Go home.\"");
        add.accept("message.supernaturalcraft.dagon.defeated", "Dagon is gone. Inside the cabin, Kelly Kline cries out.");
        add.accept("message.supernaturalcraft.dagon.found", "A woman in black stands before the cabin door, and the air bends round her raised hand.");
        add.accept("message.supernaturalcraft.dagon.rite.elsewhere", "Dagon only answers at Kelly Kline's safe house, in its clearing.");
        add.accept("message.supernaturalcraft.dagon.busy", "Something holds this place already.");
    }

    private static void birth(BiConsumer<String, String> add) {
        add.accept("title.supernaturalcraft.birth.phase1", "The Birth");
        add.accept("title.supernaturalcraft.birth.phase1.sub", "Kelly Kline's time has come.");
        add.accept("title.supernaturalcraft.birth.fall", "Jack");
        add.accept("title.supernaturalcraft.birth.fall.sub", "Born.");
        add.accept("message.supernaturalcraft.birth.kelly_0", "Kelly: \"It's time. He's coming. I can feel him.\"");
        add.accept("message.supernaturalcraft.birth.lucifer_0", "Lucifer, in the President's suit: \"Hello, Kelly. I came to meet my son.\"");
        add.accept("message.supernaturalcraft.birth.kelly_1", "Kelly: \"Jack. His name is Jack.\" Light pours out of her.");
        add.accept("message.supernaturalcraft.birth.lucifer_1", "Lucifer reaches for the child: \"Come to your father.\"");
        add.accept("message.supernaturalcraft.birth.gone", "The light throws Lucifer back. A rift tears open behind him, and he is gone into another world.");
        add.accept("message.supernaturalcraft.birth.kelly_2", "Kelly sinks back, still. A newborn cries in the light.");
        add.accept("message.supernaturalcraft.birth.born", "Jack is born. The rift Lucifer fell through is still open: another world lies beyond it.");
    }
}
