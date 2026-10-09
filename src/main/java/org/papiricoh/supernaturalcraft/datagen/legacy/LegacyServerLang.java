package org.papiricoh.supernaturalcraft.datagen.legacy;

import java.util.function.BiConsumer;

/** The Men of Letters' server-side text (v0.17): advancements, Henry's lines, cases, bunker and monster messages. Owned by agent A. */
public final class LegacyServerLang {

    private LegacyServerLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        adv(add, "legacy_1", "Aspirant", "Accept Henry Winchester's offer and join the Men of Letters");
        adv(add, "legacy_2", "Initiate", "Finish five pieces of research in the bunker");
        adv(add, "legacy_3", "Scholar", "Finish fifteen pieces of research, of three kinds");
        adv(add, "legacy_4", "Master of Letters", "Finish thirty-five pieces of research, of four kinds");
        adv(add, "legacy_5", "Keeper of the Lore", "Finish seventy pieces of research, of five kinds");
        String r = "legacy.supernaturalcraft.rank.";
        add.accept(r + "none", "Not a member");
        add.accept(r + "aspirant", "Aspirant");
        add.accept(r + "initiate", "Initiate");
        add.accept(r + "scholar", "Scholar");
        add.accept(r + "master", "Master of Letters");
        add.accept(r + "keeper", "Keeper of the Lore");
        add.accept("container.supernaturalcraft.research", "Research Desk");
        henry(add);
        cases(add);
        world(add);
        for (net.minecraft.world.item.DyeColor c : net.minecraft.world.item.DyeColor.values()) {
            String name = c.getName().substring(0, 1).toUpperCase() + c.getName().substring(1).replace('_', ' ');
            add.accept("block.supernaturalcraft.banner.men_of_letters." + c.getName(), name + " Men of Letters Emblem");
        }
    }

    /** Henry Winchester, 1958: polite, precise, a little stiff, proud of the order. */
    private static void henry(BiConsumer<String, String> add) {
        String h = "legacy.supernaturalcraft.henry.";
        lines(add, h + "offer", "Good morning. Forgive the hour, and the intrusion. My name is Henry Winchester.",
                "I am a Legacy of the Men of Letters: a brotherhood of scholars who studied the things that go bump in the night, so that men like you could put them down.",
                "Word travels, even to us. You stood against the Devil himself, and you walked away. That is not luck. That is a hunter.",
                "We kept our knowledge in a bunker in open country, built to outlast anything. It has sat locked for far too long.",
                "I am offering you a place among us: the key, the library, the war room, and every secret we ever wrote down. Will you take it?");
        lines(add, h + "offer_again", "Good morning again. I did say I would come back, and a Winchester keeps his word.",
                "The offer stands. The archive does not read itself, and the things out there are not waiting for us to make up our minds.",
                "So: will you take up the Legacy?");
        lines(add, h + "welcome", "Splendid. Then welcome to the Men of Letters, Aspirant.",
                "Here is the key. Don't lose it; there is only the one, and the door was built to keep out worse than burglars.",
                "And a map. The bunker lies out in the open country, beneath a little stone hut that nobody ever looks at twice.",
                "I'll be waiting for you in the war room, by the map table. Read everything. Then we'll talk about cases.");
        lines(add, h + "farewell", "I understand. It is a great deal to ask of anyone.",
                "I'll call again in a few days. Do try to stay alive until then.");
        lines(add, h + "war_room", "Ah, there you are. Pull up a chair.",
                "The map table never sleeps: missing persons, strange deaths, cattle drained of blood. Somebody has to look into them.",
                "Shall I give you a case?");
        lines(add, h + "case_open", "You still have a case open, I'm afraid. The file has the map, and the map has the place.",
                "Finish that one first. We don't leave a job half done in this order.");
        lines(add, h + "case_given", "Here is the file, and a map of the place. Read the file on the way.",
                "Mind what you're up against. A vampire wants its head off, a werewolf wants silver, and a shapeshifter wants to be found out before anything else.",
                "Field notes from the scene come back here, to the archive. Good hunting.");
        lines(add, h + "case_solved", "Well done. A clean job, by the sound of it, and the file is closed.",
                "Put your notes on the desk in the library; there is always more to learn from what we kill.",
                "Another? There is always another.");
        lines(add, h + "case_lost", "The trail went cold, did it? It happens to the best of us.",
                "No shame in it. Shall we try another?");
        String c = h + "choice.";
        add.accept(c + "accept", "I'll take the key.");
        add.accept(c + "decline", "Not today, Henry.");
        add.accept(c + "new_case", "Give me a case.");
        add.accept(c + "close", "Thank you, Henry.");
    }

    private static void cases(BiConsumer<String, String> add) {
        String c = "legacy.supernaturalcraft.case.";
        add.accept(c + "scenario.barn", "The Barn on the Hill");
        add.accept(c + "scenario.village", "The Missing Farmhands");
        add.accept(c + "scenario.graveyard", "The Open Graves");
        add.accept(c + "scenario.haunted_house", "The House Nobody Sells");
        add.accept(c + "scenario.night_woods", "The Empty Campsite");
        add.accept(c + "scenario.mine", "The Boarded-Up Mine");
        add.accept(c + "twist.none", "Nothing more is known.");
        add.accept(c + "twist.named_leader", "Witnesses speak of one who gives the orders, and of a name.");
        add.accept(c + "twist.hostage", "Someone was taken alive. Get them out before it's too late.");
        add.accept(c + "twist.second_monster", "The deaths don't all match: something else is out there too.");
        add.accept(c + "hostage", "Captive");
        add.accept(c + "title", "Case File No. %s");
        String m = "message.supernaturalcraft.legacy.";
        add.accept(m + "rank_up", "The Men of Letters: you are now %s.");
        add.accept(m + "case.new", "A new case: %s. The map shows the place.");
        add.accept(m + "case.none", "There is no case to be had right now.");
        add.accept(m + "case.arrived", "This is the place. Something is here.");
        add.accept(m + "case.solved", "Case closed: %s. Field notes are in your pack.");
        add.accept(m + "case.lost", "The trail has gone cold: %s.");
        add.accept(m + "case.filed", "This case is closed and filed.");
        add.accept(m + "map_table.outsider", "Pins and red string across the whole country. None of it means anything to you, yet.");
        add.accept(m + "map_table.open", "Your case: %s, around %s, %s.");
        add.accept(m + "door.locked", "The door is locked. Steel, a good three inches of it.");
        add.accept(m + "blood_smeared", "You smear dead man's blood along the blade.");
        add.accept("item.supernaturalcraft.case_map", "Map to a Case");
        add.accept("item.supernaturalcraft.bunker_map", "Map to the Bunker");
        add.accept("item.supernaturalcraft.case_file.named", "Case File: %s");
        String t = "tooltip.supernaturalcraft.";
        add.accept(t + "case_file", "Use to read the brief; crouch and use for a new map of the place.");
        add.accept(t + "bunker_key", "Opens and shuts the bunker's door.");
        add.accept(t + "dead_mans_blood", "Use on a vampire to stun it, or with a blade in the other hand to smear it.");
        add.accept(t + "men_of_letters_ring", "Research runs a tenth faster while you carry it.");
        add.accept(t + "aquarian_star", "One more research desk at once while you carry it.");
        add.accept(t + "spellwrights_spectacles", "Worn, they show what hides, and a shapeshifter for what it is.");
        add.accept(t + "henrys_case", "Nine pockets for field notes and cursed artifacts.");
        add.accept(t + "henrys_case.count", "%s of %s pockets full");
    }

    private static void world(BiConsumer<String, String> add) {
        String c = "commands.supernaturalcraft.legacy.";
        add.accept(c + "bunker", "The bunker's door is at %s, %s, %s (%s blocks from the origin), %s.");
        add.accept(c + "built", "built");
        add.accept(c + "unbuilt", "not yet built");
        add.accept(c + "placed", "A bunker was built at %s.");
    }

    private static void lines(BiConsumer<String, String> add, String prefix, String... lines) {
        for (int i = 0; i < lines.length; i++) add.accept(prefix + "." + (i + 1), lines[i]);
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }
}
