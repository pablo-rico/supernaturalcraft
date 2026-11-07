package org.papiricoh.supernaturalcraft.datagen.apocalypse;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The Apocalypse's English text (v0.40, the core): Alastair and his warehouse, Ruby, the demon blood and the Panic Room, the
 * new road and Swan Song. Called from {@code SNLang.addAll}; the subtitles ({@link #SUBTITLES}) are read by
 * {@code SNLanguageProvider}. The Spanish lives in {@code es_es.json} under the same keys.
 */
public final class ApocalypseLang {

    private ApocalypseLang() {
    }

    /** Subtitles by sound event path. */
    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("alastair.hum", "Alastair hums"),
            Map.entry("alastair.chains", "Chains rattle"),
            Map.entry("alastair.chain_break", "Chains snap"),
            Map.entry("alastair.hook_throw", "A hook flies"),
            Map.entry("alastair.hook_hit", "A hook bites"),
            Map.entry("alastair.scalpel", "A blade whistles"),
            Map.entry("alastair.rack_rise", "The rack rises"),
            Map.entry("alastair.rack_clamp", "The rack clamps shut"),
            Map.entry("alastair.smoke", "White smoke pours"),
            Map.entry("alastair.laugh", "Alastair laughs"),
            Map.entry("alastair.hurt", "Alastair hurts"),
            Map.entry("alastair.death", "Alastair burns out"),
            Map.entry("ruby.smoke_in", "Black smoke gathers"),
            Map.entry("ruby.smoke_out", "Black smoke disperses"),
            Map.entry("ruby.laugh", "Ruby laughs"),
            Map.entry("ruby.slash", "A knife flashes"),
            Map.entry("ruby.shove", "Something shoves"),
            Map.entry("ruby.hurt", "Ruby hurts"),
            Map.entry("ruby.death", "Ruby burns out"),
            Map.entry("blood.drink", "Blood is drunk"),
            Map.entry("blood.push", "A psychic shove"),
            Map.entry("blood.exorcism", "A demon is torn out"),
            Map.entry("blood.smite", "A demon is crushed"),
            Map.entry("blood.craving", "Your blood pounds"),
            Map.entry("blood.door_lock", "The Panic Room locks"),
            Map.entry("apocalypse.cage_open", "The Cage opens"),
            Map.entry("apocalypse.cage_fall", "Something falls into the Cage"),
            Map.entry("music.alastair", "Music plays"));

    public static void add(BiConsumer<String, String> add) {
        // --- arenas, rites, advancements --------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.arena.barrier.warehouse", "The devil's traps on the walls hold the fight inside.");
        add.accept("message.supernaturalcraft.arena.no_escape.warehouse", "\"Leaving so soon? We were just getting to the good part.\"");
        add.accept("advancement.supernaturalcraft.on_the_head_of_a_pin", "On the Head of a Pin");
        add.accept("advancement.supernaturalcraft.on_the_head_of_a_pin.desc", "Bring down Alastair, Hell's torturer, at the warehouse");
        add.accept("advancement.supernaturalcraft.i_know_what_you_did", "I'm Here to Help");
        add.accept("advancement.supernaturalcraft.i_know_what_you_did.desc", "Meet Ruby, a demon who says she is on your side");
        add.accept("advancement.supernaturalcraft.when_the_levee_breaks", "When the Levee Breaks");
        add.accept("advancement.supernaturalcraft.when_the_levee_breaks.desc", "Drink demon blood");
        add.accept("advancement.supernaturalcraft.the_panic_room", "The Panic Room");
        add.accept("advancement.supernaturalcraft.the_panic_room.desc", "Sweat the demon blood out behind an iron door");
        add.accept("advancement.supernaturalcraft.sympathy_for_the_devil", "Sympathy for the Devil");
        add.accept("advancement.supernaturalcraft.sympathy_for_the_devil.desc", "Put Ruby down with a blade that kills demons");
        add.accept("advancement.supernaturalcraft.swan_song", "Swan Song");
        add.accept("advancement.supernaturalcraft.swan_song.desc", "Throw Lucifer back into the Cage with the four rings");

        add.accept("message.supernaturalcraft.apocalypse.shackle_given", "The road has changed since you threw the Devil back: the Cage Shackle is yours, for the day the Cage must open again.");
        add.accept("message.supernaturalcraft.swan_song", "The rings blaze, the Cage opens under him, and the Devil falls back into the dark. The rings come home to you.");
        add.accept("message.supernaturalcraft.swan_song.world", "Far away, something slams shut. The sky starts to clear.");

        // --- Alastair -----------------------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.alastair.bar.phase1", "Alastair, Bound");
        add.accept("entity.supernaturalcraft.alastair.bar.phase2", "Alastair, the Tormentor");
        add.accept("entity.supernaturalcraft.alastair.bar.phase3", "Alastair, Master of the Rack");
        add.accept("title.supernaturalcraft.alastair.phase1", "Alastair");
        add.accept("title.supernaturalcraft.alastair.phase1.sub", "Bound in the angels' trap");
        add.accept("title.supernaturalcraft.alastair.phase2", "The Tormentor");
        add.accept("title.supernaturalcraft.alastair.phase2.sub", "The chains are off");
        add.accept("title.supernaturalcraft.alastair.phase3", "The Rack");
        add.accept("title.supernaturalcraft.alastair.phase3.sub", "Let's get started");
        add.accept("title.supernaturalcraft.alastair.fall", "On the Head of a Pin");
        add.accept("title.supernaturalcraft.alastair.fall.sub", "Hell's torturer is no more");
        add.accept("title.supernaturalcraft.ruby", "Ruby");
        add.accept("title.supernaturalcraft.ruby.sub", "She was never on your side");
        add.accept("message.supernaturalcraft.alastair.found", "Chains, salt and a devil's trap: the angels' prisoner lifts his head. \"Dean's little helpers. Come in.\"");
        add.accept("message.supernaturalcraft.alastair.taunt.0", "\"I taught the best student Hell ever had. Want to know what he learned first?\"");
        add.accept("message.supernaturalcraft.alastair.taunt.1", "\"Holy water. How quaint. Do go on.\"");
        add.accept("message.supernaturalcraft.alastair.taunt.2", "\"Thirty years on my rack, and then he picked up the knife himself.\"");
        add.accept("message.supernaturalcraft.alastair.taunt.3", "\"The seals? They break whether I talk or not.\"");
        add.accept("message.supernaturalcraft.alastair.taunt.4", "\"These chains won't hold me forever. Nothing does.\"");
        add.accept("message.supernaturalcraft.alastair.taunt.5", "\"Let's get started, shall we?\"");
        add.accept("message.supernaturalcraft.alastair.left", "Alastair hums a tune and fades into white smoke. He'll be back on his rack, waiting.");
        add.accept("message.supernaturalcraft.alastair.defeated", "Alastair burns out from the inside. The angels' prisoner will cut no one else.");
        add.accept("message.supernaturalcraft.alastair.busy", "Another fight already holds this world.");
        add.accept("message.supernaturalcraft.alastair.rite.elsewhere", "Alastair only answers at his warehouse, by the angels' trap.");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_alastair", "Calls Alastair back to his warehouse for a rematch (only by the trap)");
        add.accept("item.supernaturalcraft.rubys_compass", "Ruby's Compass");

        // --- Ruby ---------------------------------------------------------------------------------------------------------
        add.accept("message.supernaturalcraft.ruby.warehouse", "Ruby: \"The angels have Alastair chained in a warehouse, out at %s, %s. Go before they get bored of him.\"");
        add.accept("message.supernaturalcraft.ruby.liliths_way", "Ruby: \"Lilith is the last seal. When the eleventh breaks, call her: a great circle at night, three demon blood, two embers, two bones and a vial of bound blood. I'll be there.\"");
        add.accept("message.supernaturalcraft.ruby.reveal", "Ruby laughs over Lilith's body. \"You did it. You actually did it. Lucifer is free.\"");
        add.accept("message.supernaturalcraft.ruby.ambush", "Black smoke, and a familiar voice: \"Miss me? We never finished.\"");
        add.accept("message.supernaturalcraft.ruby.needs_bane", "She shrugs the blow off. Only a blade that kills demons will finish her.");
        add.accept("message.supernaturalcraft.ruby.dead", "Ruby burns out on the blade. The Colt she carried falls into your hands.");
        dialogue(add, "meet", "A woman with a knife at her hip steps out of black smoke. \"Relax. I'm the demon who's on your side. Azazel's dead; that means the seals are next, and you're going to need me.\"",
                "Hear her out", "Leave");
        dialogue(add, "meet_angel", "A demon steps out of black smoke and keeps her distance. \"Easy, feathers. I know what you are. You still want Lilith dead? Then we want the same thing.\"",
                "Hear her out", "Leave");
        dialogue(add, "meet_demon", "A demon steps out of black smoke and smiles. \"One of us. Good. Mother Lilith is breaking the seals, and not everyone down there wants the old man back. Interested?\"",
                "Hear her out", "Leave");
        dialogue(add, "offer", "She holds out a dented steel flask. \"Demon blood. It'll make you strong enough to fight what's coming. And I know where the angels are keeping Alastair.\"",
                "Take the flask", "Just the location");
        dialogue(add, "lilith", "\"Alastair's dead. Nice work. Now the only seal that matters is Lilith herself.\"",
                "How do I find her?", "Why do you care?");
        dialogue(add, "how_reply", "\"Wait for the eleventh seal. Then call her up by night in a great circle, and I'll do the rest.\"", "Go");
        dialogue(add, "why_reply", "\"Because I was human once. And because I want to be there when she dies.\" She smiles a little too long.", "Go");

        // --- demon blood ------------------------------------------------------------------------------------------------
        add.accept("hud.supernaturalcraft.allegiance.denied.no_blood", "Not enough blood in you");
        add.accept("tooltip.supernaturalcraft.demon_blood.drink", "Drink it, if you dare. The powers come, and so does the need.");
        add.accept("tooltip.supernaturalcraft.rubys_flask", "%s of %s doses. Hold a vial of demon blood in the other hand to refill it.");
        add.accept("message.supernaturalcraft.panic_room.locked_in", "The door locks behind you. Now you wait it out.");
        add.accept("message.supernaturalcraft.panic_room.waiting", "Not yet. Ride it out.");
        add.accept("message.supernaturalcraft.panic_room.locked", "Someone is locked in there.");
        add.accept("message.supernaturalcraft.panic_room.leaks", "This room is not sealed: iron all round, floor and ceiling, and no more than seven wide.");
        add.accept("message.supernaturalcraft.panic_room.no_trap", "There is no devil's trap on the ceiling.");
        add.accept("message.supernaturalcraft.panic_room.broken", "You left the Panic Room: the cure is broken off.");
        add.accept("message.supernaturalcraft.panic_room.cured", "The door unlocks. The blood is out of you, and you can breathe again.");
        add.accept("power.supernaturalcraft.blood_push", "Psychic Shove");
        add.accept("power.supernaturalcraft.blood_push.desc", "The blood throws the foes in front of you back.");
        add.accept("power.supernaturalcraft.blood_exorcism", "Exorcism by Will");
        add.accept("power.supernaturalcraft.blood_exorcism.desc", "Tear a demon out of its host from afar: the common ones die, the rest bleed smoke.");
        add.accept("power.supernaturalcraft.blood_smite", "Raised Hand");
        add.accept("power.supernaturalcraft.blood_smite.desc", "Crush a demon from the inside. The greater demons feel it most.");
    }

    private static void dialogue(BiConsumer<String, String> add, String node, String line, String... options) {
        String key = "dialogue.supernaturalcraft." + org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyRules.DIALOGUE + "." + node;
        add.accept(key, line);
        var opts = org.papiricoh.supernaturalcraft.apocalypse.ruby.RubyRules.all().stream().filter(l -> l.node().equals(node)).findFirst()
                .orElseThrow().options();
        for (int i = 0; i < opts.size(); i++) add.accept(key + "." + opts.get(i), options[i]);
    }
}
