package org.papiricoh.supernaturalcraft.datagen.cain;

import org.papiricoh.supernaturalcraft.cain.CainDialogue;
import org.papiricoh.supernaturalcraft.entity.boss.cain.CainSpoils;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Cain's and the Mark's text (v0.21, P4): sound subtitles (read by {@code SNLanguageProvider} for {@code AllSounds.CAIN_SOUNDS}),
 * his bar and title cards, his dialogue (every node and every answer any node may offer, from {@link CainDialogue}), the offer
 * of the Mark ({@link CainSpoils#DIALOGUE}), messages, tooltips, advancements and the greetings of the sworn. Called from
 * {@code SNLang.addAll}.
 */
public final class CainLang {

    private CainLang() {
    }

    public static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("cain.ambient", "Cain breathes slow"),
            Map.entry("cain.hurt", "Cain is struck"),
            Map.entry("cain.death", "The Father of Murder falls"),
            Map.entry("cain.lunge", "Cain lunges"),
            Map.entry("cain.swarm", "A swarm rises"),
            Map.entry("cain.bees", "Bees hum"),
            Map.entry("cain.furrow", "Earth is torn open"),
            Map.entry("cain.abels_blood", "Spikes burst from the ground"),
            Map.entry("cain.throw", "Farm tools fly"),
            Map.entry("cain.execution", "A blade comes down"),
            Map.entry("cain.mark_flare", "The Mark of Cain blazes"),
            Map.entry("cain.offer_mark", "The Mark calls"));

    /** Cain's lines, by node. */
    static final Map<String, String> LINES = Map.ofEntries(
            Map.entry(CainDialogue.HELLO, "Easy. You'll upset the bees, and they don't take to strangers any better than I do. "
                    + "You've come a long way to find an old man with a smoker. So. Who sent you?"),
            Map.entry(CainDialogue.AGAIN, "You again. The bees are calmer than you are. Say what you came to say."),
            Map.entry(CainDialogue.WHO, "My name is Cain. Yes, that Cain. I killed my brother to save his soul, and I killed a great "
                    + "deal more after, as the first Knight of Hell. Then I met a woman named Colette, and I put down the blade. "
                    + "Now I keep bees. It's quieter."),
            Map.entry(CainDialogue.HONEY, "Here. From the east hives. It's good for the throat — and for other thirsts, if you "
                    + "ever have the misfortune."),
            Map.entry(CainDialogue.NO_HONEY, "One jar a day. The bees work hard, and so should you."),
            Map.entry(CainDialogue.MARK, "The Mark. Everyone who finds me wants to talk about the Mark. It isn't a gift. It's a "
                    + "curse that wants to be fed — kill, and it quiets; stop, and it gnaws at you until you're something else."),
            Map.entry(CainDialogue.REFUSE, "No. I won't hand it to you. I've seen what it does. If you want it that badly, you'll "
                    + "have to prove you can carry it — and that means getting past me."),
            Map.entry(CainDialogue.NOT_TONIGHT, "Not with the sun up. Not in front of the hives. Come back when it's dark, and we'll "
                    + "see what you're made of."),
            Map.entry(CainDialogue.NOT_READY, "You haven't seen enough of Hell to know what you're asking for. Go put the Devil "
                    + "back in his cage first."));

    static final Map<String, String> OPTIONS = Map.of(
            CainDialogue.OPT_WHO, "Who are you?",
            CainDialogue.OPT_HONEY, "Could I have some of that honey?",
            CainDialogue.OPT_MARK, "Tell me about the Mark.",
            CainDialogue.OPT_ASK, "Give me the Mark.",
            CainDialogue.OPT_FIGHT, "Then test me.",
            CainDialogue.BACK, "Something else.",
            CainDialogue.LEAVE, "Goodbye.");

    public static void add(BiConsumer<String, String> add) {
        // --- His dialogue ------------------------------------------------------------------------------------------------------
        for (String node : CainDialogue.NODES) {
            String key = "dialogue.supernaturalcraft." + CainDialogue.ID + "." + node;
            add.accept(key, LINES.get(node));
            for (String option : CainDialogue.allOptions(node)) add.accept(key + "." + option, OPTIONS.get(option));
        }
        // --- The offer of the Mark: as he dies, and at his grave ----------------------------------------------------------------
        String o = "dialogue.supernaturalcraft." + CainSpoils.DIALOGUE + ".";
        add.accept(o + CainSpoils.OFFER, "\"You beat me. Then it's yours, if you want it — the Mark, and everything it costs. "
                + "Think hard. Once it's on you, it doesn't come off.\"");
        add.accept(o + CainSpoils.OFFER + "." + CainSpoils.TAKE, "Take his arm. Take the Mark.");
        add.accept(o + CainSpoils.OFFER + "." + CainSpoils.REFUSE, "No. Let it die with him.");
        add.accept(o + CainSpoils.GRAVE, "The earth over Cain's grave is warm. Something under it is waiting — the Mark, for whoever "
                + "beat him.");
        add.accept(o + CainSpoils.GRAVE + "." + CainSpoils.TAKE, "Put your hand on the earth. Take the Mark.");
        add.accept(o + CainSpoils.GRAVE + "." + CainSpoils.REFUSE, "Walk away.");
        add.accept(o + CainSpoils.GRAVE_AGAIN, "The earth over Cain's grave is warm. The Mark was taken off you once; it has not "
                + "forgotten you. Take it again and it comes back thirsty — it remembers the way.");
        add.accept(o + CainSpoils.GRAVE_AGAIN + "." + CainSpoils.TAKE, "Put your hand on the earth. Take it back.");
        add.accept(o + CainSpoils.GRAVE_AGAIN + "." + CainSpoils.REFUSE, "Walk away.");

        // --- His bar, title cards, cinematics ------------------------------------------------------------------------------------
        add.accept("entity.supernaturalcraft.cain.bar.phase1", "Cain, the Beekeeper");
        add.accept("entity.supernaturalcraft.cain.bar.phase2", "Cain, the Mark Awakens");
        add.accept("entity.supernaturalcraft.cain.bar.phase3", "Cain, Father of Murder");
        add.accept("title.supernaturalcraft.cain.phase1", "The Beekeeper");
        add.accept("title.supernaturalcraft.cain.phase1.sub", "An old man sets down his smoker. The bees go quiet.");
        add.accept("title.supernaturalcraft.cain.phase2", "The Mark Awakens");
        add.accept("title.supernaturalcraft.cain.phase2.sub", "It burns on his arm, and it is hungry");
        add.accept("title.supernaturalcraft.cain.phase3", "Father of Murder");
        add.accept("title.supernaturalcraft.cain.phase3.sub", "He takes up the First Blade again");
        add.accept("title.supernaturalcraft.cain.death", "First Born");
        add.accept("title.supernaturalcraft.cain.death.sub", "The first murderer kneels in his own field");
        add.accept("cinematic.supernaturalcraft.cain.victory.title", "First Born");
        add.accept("cinematic.supernaturalcraft.cain.victory.subtitle", "The bees will want a new keeper");

        // --- Messages ----------------------------------------------------------------------------------------------------------
        String m = "message.supernaturalcraft.cain.";
        add.accept(m + "busy", "Another fight already holds this world: Cain goes back to his bees.");
        add.accept(m + "challenge", "Cain sets down his smoker and rolls up his sleeve. \"All right. Let's see it.\"");
        add.accept(m + "cage_gone", "The fight is over. Cain is back among his hives, as if nothing happened.");
        add.accept(m + "victorious", "Cain: \"Go home. You're not ready to carry it.\"");
        add.accept(m + "already_marked", "Cain looks at your arm. \"You already carry one. God help you.\"");
        add.accept(m + "refused", "You leave the Mark where it lies.");
        add.accept(m + "blade", "His First Blade is in your hand now. It knows the Mark.");
        add.accept(m + "grave_quiet", "The grave is quiet. Whatever is buried here is not for you.");
        add.accept("screen.supernaturalcraft.cain.grave", "Cain's Grave");
        add.accept(m + "grave_marked", "The Mark on your arm throbs over his grave.");
        String k = "message.supernaturalcraft.mark.";
        add.accept(k + "granted", "The Mark of Cain burns into your arm.");
        add.accept(k + "remembers", "It remembers the way. The Mark comes back thirsty, as if it never left.");
        add.accept(k + "honey", "The honey is sweet, and the Mark goes quiet for a while.");
        add.accept(k + "fall", "The Mark will not let you die. You come back something else...");
        add.accept(k + "fall_angel", "The Mark burns your grace out. You fall, and you rise something else...");
        add.accept("message.supernaturalcraft.cains_address.elsewhere", "The address means nothing here.");
        add.accept("message.supernaturalcraft.cains_address.here", "You're at his gate.");
        add.accept("message.supernaturalcraft.cains_address.heading", "His farm lies %2$s — about %1$s blocks.");

        // --- Tooltips ----------------------------------------------------------------------------------------------------------
        add.accept("tooltip.supernaturalcraft.cains_honey", "From the Father of Murder's hives. Eaten by a bearer of the Mark, it quiets a quarter of its Thirst.");
        add.accept("tooltip.supernaturalcraft.cains_address", "In Rowena's elegant hand: \"Bees. End of the roads. Knock.\"");
        add.accept("tooltip.supernaturalcraft.cains_address.use", "Use it to see which way his farm lies");

        // --- Advancements ------------------------------------------------------------------------------------------------------
        adv(add, "met_cain", "The Beekeeper", "Find Cain at his farm, after Lucifer is back in his box");
        adv(add, "first_born", "First Born", "Beat Cain, the Father of Murder, in his own yard at night");
        adv(add, "marked", "The Mark of Cain", "Bear the Mark of Cain");

        // --- The sworn are greeted (BossTwists) ----------------------------------------------------------------------------------
        String g = "message.supernaturalcraft.allegiance.boss.cain.";
        add.accept(g + "angel", "Cain: \"An angel. You lot never could stand that God liked my brother's offering better.\"");
        add.accept(g + "demon", "Cain: \"A demon. I made your kind's Knights. I can unmake one.\"");
        add.accept(g + "human", "Cain: \"A hunter. I'll go easy. At first.\"");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }
}
