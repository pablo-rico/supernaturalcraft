package org.papiricoh.supernaturalcraft.datagen.allegiance;

import java.util.function.BiConsumer;

/**
 * The allegiance's game text (v0.13): ranks ({@code allegiance.supernaturalcraft.rank.<faction>.<n>}), powers
 * ({@code power.supernaturalcraft.<id>} + {@code .desc}), dialogue ({@code dialogue.supernaturalcraft.<dialogue>.<node>[.<option>]}),
 * messages ({@code message.supernaturalcraft.allegiance.*}), advancements and the bosses' lines. The HUD, wheel and book
 * text is in {@link AllegianceUiLang}.
 */
public final class AllegianceLang {

    private AllegianceLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        ranks(add);
        powers(add);
        dialogue(add);
        messages(add);
        bosses(add);
        advancements(add);
        misc(add);
    }

    private static void ranks(BiConsumer<String, String> add) {
        String r = "allegiance.supernaturalcraft.rank.";
        add.accept(r + "human.0", "Human");
        add.accept(r + "human.1", "Hunter");
        add.accept(r + "human.2", "Veteran");
        add.accept(r + "human.3", "Legend");
        add.accept(r + "angel.0", "Angel");
        add.accept(r + "angel.1", "Lesser Angel");
        add.accept(r + "angel.2", "Seraph");
        add.accept(r + "angel.3", "Archangel");
        add.accept(r + "angel.4", "General of the Host");
        add.accept(r + "demon.0", "Demon");
        add.accept(r + "demon.1", "Crossroads Demon");
        add.accept(r + "demon.2", "Prince of Hell");
        add.accept(r + "demon.3", "Knight of Hell");
        add.accept(r + "demon.4", "King of Hell");
    }

    private static void power(BiConsumer<String, String> add, String id, String name, String desc) {
        add.accept("power.supernaturalcraft." + id, name);
        add.accept("power.supernaturalcraft." + id + ".desc", desc);
    }

    private static void powers(BiConsumer<String, String> add) {
        power(add, "teleport", "Flight of the Unseen", "Blink up to 16 blocks where you look, in a flutter of unseen wings. Not while held by a trap.");
        power(add, "healing_touch", "Healing Touch", "Two fingers to the brow of what you look at (or your own): heals four hearts and cures poison and wither. A demon burns instead.");
        power(add, "angel_blade", "Angel Blade", "A blade drops from your sleeve for a minute, bound to you: dropped, it is gone. Cast again with it in hand to dash.");
        power(add, "angel_radio", "Angel Radio", "For 20 seconds, demons and bosses within 128 blocks whisper where they are.");
        power(add, "wings", "Wings", "Flight with stamina, as Michael's Grace gives the Seraph Wings, with no wings to wear.");
        power(add, "smite", "Smite", "A palm to the face of what you look at: holy fire through the eyes. A lesser demon does not survive it.");
        power(add, "true_form", "True Form", "For 10 seconds light pours out of you: everything near that sees it is blinded, and demons burn.");
        power(add, "host_squad", "Squad of the Host", "A captain and four soldiers of the Host answer you for a minute: they follow you, fight what you fight and hunt demons.");
        power(add, "light_lance", "Lance of Light", "A lance of light thrown where you look; it strikes hard.");
        power(add, "smoke", "Black Smoke", "Pour out as smoke and rush up to 12 blocks where you look, untouchable on the way. Not while held by a trap.");
        power(add, "fire_immunity", "Hellfire Born", "Fire and lava do not burn you.");
        power(add, "summon_hound", "Call a Hellhound", "A bound hellhound fights at your side for a minute.");
        power(add, "black_eyes", "Black Eyes", "You see in the dark. Your eyes show when you use your powers, and villagers run from them.");
        power(add, "telekinesis", "Telekinesis", "Seize what you look at; it hangs before you. Cast again to hurl it where you look.");
        power(add, "possess", "Possession", "Ride a creature for 20 seconds: it fights for you while you wait inside it, untouchable.");
        power(add, "yellow_eyes", "Yellow Eyes", "A Prince's eyes. From this rank you need neither food nor sleep.");
        power(add, "first_blade", "The First Blade", "The First Blade answers its Knight: it strikes half again as hard in your hand.");
        power(add, "kill_regen", "Feeding", "Every kill heals you two hearts.");
        power(add, "bloodlust", "Bloodlust", "The Mark's thirst: ten minutes without a kill and your Corruption drains, then your health.");
        power(add, "dominion", "Dominion", "Demons and hellhounds obey the King: they never turn on you, and whatever strikes you has every demon near after it.");
        power(add, "throne", "The Throne", "Every creature within 12 blocks is brought to its knees for 8 seconds.");
        power(add, "hunter_mana", "Hunter's Discipline", "25 more mana for each rank, as every road gives. (Every human's rites already cost a quarter less and allow longer to recite.)");
        power(add, "hunter_sense", "Veteran's Sense", "You feel the supernatural within 24 blocks: it is marked for you.");
        power(add, "hunter_edge", "Legend's Edge", "Your demon-bane weapons strike a fifth harder against anything supernatural.");
    }

    private static void dialogue(BiConsumer<String, String> add) {
        String m = "dialogue.supernaturalcraft.messenger.";
        add.accept(m + "greeting", "I am a messenger of the Lord. You have done Heaven a service, hunter: the Yellow-Eyed Demon is dead. I was sent to make you an offer.");
        add.accept(m + "greeting.listen", "I'm listening.");
        add.accept(m + "greeting.decline", "Not interested.");
        add.accept(m + "offer", "This is Grace. Take it to an altar by day, in a circle of white candles, with an angel's blade, and you will be one of us. It is a war, hunter. We could use you.");
        add.accept(m + "offer.accept", "I'll take it.");
        add.accept(m + "offer.decline", "Keep it.");
        String l = "dialogue.supernaturalcraft.lucifer_offer.";
        add.accept(l + "offer", "A demon. One of my children, come to kill me? Kneel instead. Serve me, and I will make you a prince of the world to come.");
        add.accept(l + "offer.serve", "I serve.");
        add.accept(l + "offer.refuse", "Never.");
    }

    private static void messages(BiConsumer<String, String> add) {
        String m = "message.supernaturalcraft.allegiance.";
        add.accept(m + "wrong_side.human", "Only a human may perform this rite.");
        add.accept(m + "wrong_side.angel", "Only an angel may perform this rite.");
        add.accept(m + "wrong_side.demon", "Only a demon may perform this rite.");
        add.accept(m + "wrong_rank", "This rite is not for your rank.");
        add.accept(m + "cooldown", "Your soul is still settling. You can't choose a side again yet.");
        add.accept(m + "ascended", "You are now: %s");
        add.accept(m + "cured.demon", "The last of the blood takes. You are human again: your ranks are gone, and the crossroads gives back what it took.");
        add.accept(m + "cured.angel", "You tear out your Grace. You are human again, and it is left in a vial.");
        add.accept(m + "cure_needs_trap", "The cure takes only on a demon held in a devil's trap.");
        add.accept(m + "cure_same_night", "The blood has been given tonight. Another night.");
        add.accept(m + "cure_night", "The purified blood burns going in. Night %s of %s.");
        add.accept(m + "consecrated", "The ground is holy for %s blocks around the altar.");
        add.accept(m + "convert_sealed", "\"One of us, then.\" The crossroads keeps %s hearts of yours. Welcome to Hell.");
        add.accept(m + "soul_bound", "Your soul is bound to the crossroads. If the hounds collect, you will not come back hollow: you will come back one of them.");
        add.accept(m + "soul_taken", "The hounds drag your soul down. Something else will wake in your body.");
        add.accept(m + "expelled", "The rite tears the Corruption out of you!");
        add.accept(m + "banished", "The sigil flares and hurls you far away!");
        add.accept(m + "pact", "A pact is sealed.");
        add.accept(m + "messenger_declined", "He nods. \"Then I will come again.\"");
        add.accept(m + "messenger_heeded", "He presses the vial into your hand: Receive Grace by day, in a circle of white candles, with an angel's blade. The page will call him back if you lose it.");
        add.accept(m + "messenger_silent", "No one answers.");
    }

    private static void boss(BiConsumer<String, String> add, String boss, String angel, String demon, String human) {
        String b = "message.supernaturalcraft.allegiance.boss." + boss + ".";
        add.accept(b + "angel", angel);
        add.accept(b + "demon", demon);
        add.accept(b + "human", human);
    }

    private static void bosses(BiConsumer<String, String> add) {
        boss(add, "azazel", "Azazel: \"A feathered little soldier. Heaven sends its errand boys to do its killing now?\"",
                "Azazel: \"Well, look at you. A crossroads dog, biting the hand that made the road.\"",
                "Azazel: \"A hunter with a rank. Your daddy would be so proud. Shame he won't see you die.\"");
        boss(add, "lilith", "Lilith: \"An angel. Do you know what your brothers did to me? Come closer, I'll show you.\"",
                "Lilith: \"You're one of mine and you raise a blade to me? I'll make you a hellhound's chew toy.\"",
                "Lilith: \"A real hunter. I've worn a few of you. You fit like a glove.\"");
        boss(add, "lucifer", "Lucifer: \"Brother. Little brother. They sent you to put me back in my box?\"",
                "Lucifer: \"One of my children. Let's see if you know who your father is.\"",
                "Lucifer: \"Free will. My Father's favourite toy. Let's see how free you are.\"");
        boss(add, "metatron", "Metatron: \"A seraph who reads! No? Pity. I'd have written you a better part.\"",
                "Metatron: \"A demon in my library. I'll write you out in a single line.\"",
                "Metatron: \"The hunter. Every story has one, and every hunter dies in the second act.\"");
        boss(add, "amara", "Amara: \"His light, in a little vessel. I will drink it.\"",
                "Amara: \"A creature of the dark, come to the Darkness. You are almost home.\"",
                "Amara: \"A human soul. Warm. I could hold it forever.\"");
        boss(add, "broken_chorus", "The Chorus sings one note at you, in recognition. Then a hundred, in judgement.",
                "The Chorus falls silent at the sight of you. The silence is worse.",
                "The Chorus sings over you as if you were not there.");
        boss(add, "war", "War: \"Heaven's soldiers. My favourite kind.\"",
                "War: \"A demon. You'd sell your own side for a better seat. I respect that.\"",
                "War: \"A veteran. You've seen a few of my fields already.\"");
        boss(add, "famine", "Famine: \"An angel never hungers. I'll teach you.\"",
                "Famine: \"Demons. Always hungry for one more soul. Let me feed.\"",
                "Famine: \"Hunters. Whiskey, burgers and grief. You're starving, kid.\"");
        boss(add, "pestilence", "Pestilence: \"Grace doesn't keep the flies off, little angel.\"",
                "Pestilence: \"Rot to rot. You'll keep nicely.\"",
                "Pestilence: \"Wash your hands, hunter. Not that it will help.\"");
        boss(add, "death", "Death: \"Angels die too. I've reaped more of your kind than you have brothers.\"",
                "Death: \"A demon. I'll take you back to where you came from, and further.\"",
                "Death: \"Hunter. I'm going to reap God someday. You won't be any trouble.\"");
        boss(add, "lucifer_uncaged", "Lucifer: \"Out of the Cage, brother. And you came all this way to see me.\"",
                "Lucifer: \"Kneel, child. The Cage is open, and your King is home.\"",
                "Lucifer: \"Still human. Still free. Still so small.\"");
        boss(add, "michael", "Michael: \"Soldier. Stand with your General, or fall with my brother.\"",
                "Michael: \"An abomination, at the gates of Heaven. I'll end it myself.\"",
                "Michael: \"A human with no yes to give me. Then you are only in my way.\"");
        boss(add, "chuck", "Chuck: \"Wings, Grace, all of it. I gave you that. I can take it back.\"",
                "Chuck: \"Hell, Corruption, the whole bit. I gave you that. You know that, right?\"",
                "Chuck: \"A hunter. Always my favourite character. Let's see how it ends.\"");
        boss(add, "gabriel", "Gabriel: \"Little brother! Look at you, all wings and no sense of humour. You'll miss every commercial break.\"",
                "Gabriel: \"Every episode needs a villain, and look who walked on set. Hit your mark, demon. You're the bad guy tonight.\"",
                "Gabriel: \"Free will, huh? Sure. Change the channel, then. Oh, wait. You can't.\"");
        String l ="message.supernaturalcraft.allegiance.boss.lucifer.";
        add.accept(l + "refused", "Lucifer: \"Pity. You'd have made a fine prince.\"");
        add.accept(l + "trap", "\"Thank you.\" He is inside you. Your body is not yours.");
        add.accept(l + "released", "He lets you go, laughing.");
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }

    private static void advancements(BiConsumer<String, String> add) {
        adv(add, "heeded_the_call", "Heeded the Call", "Accept Heaven's messenger's Grace");
        adv(add, "angel_1", "Lesser Angel", "Receive Grace and become an angel");
        adv(add, "angel_2", "Seraph", "Rise to Seraph: wings of your own");
        adv(add, "angel_3", "Archangel", "Rise to Archangel");
        adv(add, "angel_4", "General of the Host", "Usurp Michael's place at the head of the Host");
        adv(add, "soul_bound", "Soul Bound", "Bind your soul to a crossroads deal");
        adv(add, "demon_1", "Crossroads Demon", "Become a demon");
        adv(add, "demon_2", "Prince of Hell", "Rise to Prince of Hell");
        adv(add, "demon_3", "Knight of Hell", "Take the Mark and become a Knight of Hell");
        adv(add, "demon_4", "King of Hell", "Usurp the throne of Hell");
        adv(add, "hunter_1", "Hunter", "Swear the hunter's oath");
        adv(add, "hunter_2", "Veteran", "Keep the veteran's vigil");
        adv(add, "hunter_3", "Legend of the Road", "Become a legend among hunters");
    }

    private static void misc(BiConsumer<String, String> add) {
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.allegiance", "Changes your side or your rank");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.consecrate_ground", "Consecrates the ground around the altar");
        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_messenger", "Calls Heaven's messenger");
        add.accept("bowl_spell.supernaturalcraft.purified_blood", "Purified Blood");
        add.accept("bowl_spell.supernaturalcraft.purified_blood.desc",
                "Human blood made holy: the demon cure, given a night at a time to a demon held in a devil's trap.");
        add.accept("bowl_spell.supernaturalcraft.summon_messenger", "Summon the Messenger");
        add.accept("bowl_spell.supernaturalcraft.summon_messenger.desc",
                "Calls Heaven's messenger to a human free to choose, to offer Grace again. He never answers a demon.");
        add.accept("sigil.supernaturalcraft.banishing", "Enochian Banishing");
        add.accept("sigil.supernaturalcraft.banishing.desc",
                "Drawn in blood: an angel struck by it is flung far away and loses its Grace; a lesser angel is sent back to Heaven.");
        add.accept("tooltip.supernaturalcraft.vial_of_grace", "Grace, glowing in glass");
    }
}
