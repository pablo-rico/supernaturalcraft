package org.papiricoh.supernaturalcraft.datagen.pact;

import java.util.function.BiConsumer;

/**
 * Season 6's text (v0.39): Castiel's pact with Crowley, the Alphas' errand, the door to Purgatory, Godstiel's fight and his fate.
 * Called from {@code SNLang.addAll}. The Spanish lives in {@code es_es.json} under the same keys.
 */
public final class PactLang {

    private PactLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        story(add);
        castiel(add);
        crowley(add);
        godstiel(add);
        spoils(add);

        add.accept("message.supernaturalcraft.arena.barrier.lab", "The light of a new God holds you in.");
        add.accept("message.supernaturalcraft.arena.no_escape.lab", "\"Nobody leaves until I say so.\"");

        add.accept("advancement.supernaturalcraft.castiels_call", "Castiel's Call");
        add.accept("advancement.supernaturalcraft.castiels_call.desc", "Hear Castiel out at dawn, after Raphael's fall");
        add.accept("advancement.supernaturalcraft.king_of_hell", "The King of Hell");
        add.accept("advancement.supernaturalcraft.king_of_hell.desc", "Meet Castiel's partner at a crossroads");
        add.accept("advancement.supernaturalcraft.the_alphas", "Mother's Little Helpers");
        add.accept("advancement.supernaturalcraft.the_alphas.desc", "Bring down the Alpha vampire, the Alpha werewolf and the Alpha shapeshifter");
        add.accept("advancement.supernaturalcraft.the_man_who_knew_too_much", "The Man Who Knew Too Much");
        add.accept("advancement.supernaturalcraft.the_man_who_knew_too_much.desc", "Bring down Godstiel, the new God full of souls");
        add.accept("advancement.supernaturalcraft.castiel_forgiven", "I Forgive You");
        add.accept("advancement.supernaturalcraft.castiel_forgiven.desc", "Forgive Castiel when he kneels");
        add.accept("advancement.supernaturalcraft.castiel_condemned", "Into the Water");
        add.accept("advancement.supernaturalcraft.castiel_condemned.desc", "Leave Castiel to the Leviathans when he kneels");
    }

    /** The road: messages as the pact moves, the lab, the door's rite and its scene. */
    private static void story(BiConsumer<String, String> add) {
        add.accept("message.supernaturalcraft.pact.sent_to_crowley", "Castiel wants you to meet his partner. Call on a crossroads (bury a "
                + "crossroads box at a crossroads, or call its demon in the spell bowl), and the King himself will answer. If you never do, "
                + "he will find you.");
        add.accept("message.supernaturalcraft.pact.hunting", "Crowley wants the three Alphas: the vampire, the werewolf, the shapeshifter. "
                + "They hunt at night. Walk it, and let them find you.");
        add.accept("message.supernaturalcraft.pact.alpha_slain", "%s is down. Crowley will want to hear of it (%s of %s).");
        add.accept("message.supernaturalcraft.pact.lab", "Crowley's lab: an old pump house by a reservoir, near %s, %s. Work the door's "
                + "rite at the altar in the middle of its hall, by night.");
        add.accept("item.supernaturalcraft.crowleys_compass", "Crowley's Compass");
        add.accept("message.supernaturalcraft.pact.door.elsewhere", "The door only opens at the altar in the middle of Crowley's lab.");
        add.accept("message.supernaturalcraft.pact.door.not_ready", "Crowley has not found where the door is yet. The Alphas know.");
        add.accept("message.supernaturalcraft.pact.door.opened", "The door has been opened already.");
        add.accept("message.supernaturalcraft.pact.door.busy", "Something holds this place already.");
        add.accept("message.supernaturalcraft.pact.scene.crowley_0", "Crowley: \"Right on time. Fifty-fifty, angel, as agreed.\"");
        add.accept("message.supernaturalcraft.pact.scene.castiel_0", "Castiel: \"I can feel them. All of them.\"");
        add.accept("message.supernaturalcraft.pact.scene.crowley_1", "Crowley: \"Oi. Halves, feathers. That was the deal.\"");
        add.accept("message.supernaturalcraft.pact.scene.crowley_2", "Crowley: \"Bloody hell.\" He is gone.");
        add.accept("message.supernaturalcraft.pact.scene.castiel_1", "Castiel: \"I'm not an angel anymore. I'm your new God. A better one.\"");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.open_purgatory_door", "Opens the door to Purgatory (at Crowley's lab)");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_godstiel", "Calls Godstiel back to Crowley's lab");
    }

    /** Castiel at dawn (dialogue {@code castiel_pact}). */
    private static void castiel(BiConsumer<String, String> add) {
        String d = "dialogue.supernaturalcraft.castiel_pact.";
        add.accept(d + "call", "Raphael is gone, but his armies are not. They will finish what he started: the Apocalypse, on Earth. I can't stop "
                + "them alone. I need power. I need your help.");
        add.accept(d + "call.listen", "What kind of power?");
        add.accept(d + "call.refuse", "Not now, Cas.");
        add.accept(d + "plan", "There is a place where every monster's soul goes when it dies. Purgatory. Opened, those souls would end this "
                + "war in a day. I have a partner who knows how to find the door. Go to a crossroads. He will answer.");
        add.accept(d + "plan.help", "I'll meet him.");
        add.accept(d + "plan.refuse", "This sounds like a bad idea.");
        add.accept(d + "doubt", "You've met Crowley. I know what he is. Do you trust me?");
        add.accept(d + "doubt.trust", "I trust you, Cas.");
        add.accept(d + "doubt.question", "What does he get out of this?");
        add.accept(d + "trust_reply", "Then trust that I know what I'm doing. When it's over, I'll make everything right.");
        add.accept(d + "trust_reply.go", "Go.");
        add.accept(d + "question_reply", "Half the souls. He'll never get them. I'm not a fool. Don't ask me more than that.");
        add.accept(d + "question_reply.go", "Go.");
        add.accept(d + "ready", "Crowley's lab is ready. Tomorrow night the door opens. Will you be there?");
        add.accept(d + "ready.promise", "I'll be there.");
        add.accept(d + "ready.warn", "Cas, whatever's behind that door, it isn't yours.");
        add.accept(d + "promise_reply", "Good. Bring your blade. Things will come through that door that aren't souls.");
        add.accept(d + "promise_reply.go", "Go.");
        add.accept(d + "warn_reply", "...I'm sorry. I have to. You'll understand when it's done.");
        add.accept(d + "warn_reply.go", "Go.");
    }

    /** Crowley, King of Hell (dialogue {@code crowley_king}). */
    private static void crowley(BiConsumer<String, String> add) {
        String d = "dialogue.supernaturalcraft.crowley_king.";
        add.accept(d + "meet", "Hello, hunter. Crowley. King of Hell. Feathers told me you might drop by. Shall we talk business?");
        add.accept(d + "meet.listen", "Talk.");
        add.accept(d + "meet.leave", "I'm leaving.");
        add.accept(d + "deal", "Purgatory, darling. Every monster's soul since the dawn of time. We split it, the angel and I. All I need is "
                + "its door, and the Alphas know where their mother came from. Bring them down. The vampire, the wolf, the shifter.");
        add.accept(d + "deal.deal", "Deal. No kiss.");
        add.accept(d + "deal.refuse", "Find someone else.");
        add.accept(d + "progress", "Still at it? The Alphas don't come to the ones who stay home. Walk the night.");
        add.accept(d + "progress.go", "Fine.");
        add.accept(d + "lab", "All three. Look at you. The door is where they said, and I have a lab nearby. Take this. Tell feathers "
                + "tomorrow night. And hunter... watch him.");
        add.accept(d + "lab.go", "I'll be there.");
    }

    /** Godstiel's fight, his bar, his cards and his voices. */
    private static void godstiel(BiConsumer<String, String> add) {
        String[] bars = {"Castiel, the New God", "Castiel, Who Judges", "Castiel, Cracking", "Castiel, Among the Voices", "What Is Left of Castiel"};
        for (int i = 0; i < bars.length; i++) add.accept("entity.supernaturalcraft.godstiel.bar.phase" + (i + 1), bars[i]);
        String[][] titles = {{"I'm Your New God", "On your knees."}, {"Judgment", "Heretics, all of them."},
                {"The Cracks", "Something is moving under his skin."}, {"The Voices", "They are not his."}, {"The Overflow", "The water goes black."}};
        for (int i = 0; i < titles.length; i++) {
            add.accept("title.supernaturalcraft.godstiel.phase" + (i + 1), titles[i][0]);
            add.accept("title.supernaturalcraft.godstiel.phase" + (i + 1) + ".sub", titles[i][1]);
        }
        add.accept("title.supernaturalcraft.godstiel.kneel", "Castiel Kneels");
        add.accept("title.supernaturalcraft.godstiel.kneel.sub", "\"Please. Help me.\"");
        add.accept("title.supernaturalcraft.godstiel.forgiven", "Forgiven");
        add.accept("title.supernaturalcraft.godstiel.forgiven.sub", "The black pours out of him, and he is only an angel.");
        add.accept("title.supernaturalcraft.godstiel.condemned", "Into the Water");
        add.accept("title.supernaturalcraft.godstiel.condemned.sub", "The Leviathans take what is left.");
        add.accept("message.supernaturalcraft.godstiel.new_god", "Godstiel: \"I'm not Castiel anymore. I am your new God. Bow down "
                + "and profess your love unto me, or I'll destroy you.\"");
        add.accept("message.supernaturalcraft.godstiel.phase2", "Godstiel: \"The heretics will be punished. All of them.\"");
        add.accept("message.supernaturalcraft.godstiel.phase3", "Godstiel: \"Something is... wrong.\"");
        add.accept("message.supernaturalcraft.godstiel.phase4", "A hundred voices out of one mouth: \"We are many. He is nothing.\"");
        add.accept("message.supernaturalcraft.godstiel.phase5", "Castiel, barely: \"The water. Get me... to the water.\"");
        add.accept("message.supernaturalcraft.godstiel.judgment", "Godstiel: \"Kneel, and be judged.\"");
        add.accept("message.supernaturalcraft.godstiel.spoke", "You speak to him. Castiel holds on a little longer.");
        String[][] voices = {
                {"WE ARE STILL HERE", "Castiel: \"Get... out...\""},
                {"HE WAS NEVER STRONG ENOUGH", "Castiel: \"I can hold them. I can.\""},
                {"LET US OUT", "Castiel: \"No. Not yet.\""},
                {"HIS VESSEL IS OURS", "Castiel: \"Help me...\""},
                {"WE WERE FIRST", "Castiel: \"I'm sorry. For all of it.\""},
                {"THE WATER. FIND THE WATER.", "Castiel: \"Stay back. They're coming out.\""}};
        for (int i = 0; i < voices.length; i++) {
            add.accept("voice.supernaturalcraft.godstiel." + i, voices[i][0]);
            add.accept("voice.supernaturalcraft.godstiel." + i + ".cas", voices[i][1]);
            add.accept("message.supernaturalcraft.godstiel.struggle." + i, "Castiel doubles over: the Leviathans fight him from inside. Hit him now, and speak to him.");
        }
        add.accept("message.supernaturalcraft.godstiel.deciding", "%s decides what becomes of Castiel.");
        add.accept("message.supernaturalcraft.godstiel.nobody", "Nobody");
        add.accept("message.supernaturalcraft.godstiel.forgiven", "%s forgives Castiel. The Leviathans pour out of him, and he is gone in a beat of wings.");
        add.accept("message.supernaturalcraft.godstiel.condemned", "%s turns away. The Leviathans drag Castiel down into the black water.");
        add.accept("message.supernaturalcraft.godstiel.cage_gone", "Godstiel's light goes out. The lab is quiet again.");
        add.accept("message.supernaturalcraft.godstiel.victorious", "Godstiel stands alone in the lab. Nobody is left to judge.");
        add.accept("message.supernaturalcraft.godstiel.rite.elsewhere", "He only answers at the altar in the middle of Crowley's lab.");
        add.accept("cinematic.supernaturalcraft.godstiel.forgiven.title", "Forgiven");
        add.accept("cinematic.supernaturalcraft.godstiel.forgiven.subtitle", "He leaves you a shard of his Grace.");
        add.accept("cinematic.supernaturalcraft.godstiel.condemned.title", "Into the Water");
        add.accept("cinematic.supernaturalcraft.godstiel.condemned.subtitle", "All that comes back up is his trench coat.");
        String f = "dialogue.supernaturalcraft.godstiel_fate.";
        add.accept(f + "fate", "Castiel kneels in the light, the black running out of his eyes. \"I don't know how long I can hold them. "
                + "I did this. All of it. I'm sorry.\"");
        add.accept(f + "fate.forgive", "I forgive you, Cas.");
        add.accept(f + "fate.condemn", "You did this. Let them have you.");
    }

    /** What he leaves. */
    private static void spoils(BiConsumer<String, String> add) {
        add.accept("tooltip.supernaturalcraft.new_god_blade", "Use: a wave of Purgatory's light ahead (%s mana); twice as hard on anything borax burns.");
        add.accept("tooltip.supernaturalcraft.grace_shard", "Use: take it in (+%s max mana, once).");
        add.accept("message.supernaturalcraft.grace_shard.taken", "Castiel's Grace settles in you. Your well is %s deeper.");
        add.accept("message.supernaturalcraft.grace_shard.already", "You already carry a shard of his Grace.");
        add.accept("tooltip.supernaturalcraft.castiels_trenchcoat", "Leviathans deal less to its wearer, and their food can't sedate them.");
    }
}
