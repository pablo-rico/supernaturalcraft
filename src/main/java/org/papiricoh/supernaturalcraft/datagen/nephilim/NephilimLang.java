package org.papiricoh.supernaturalcraft.datagen.nephilim;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * v0.41 core's English: Jack, the rift key, Asmodeus, season 14 (Michael's offer and vessel, soulless Jack), the Empty and the
 * Shadow, and every advancement of seasons 12-14 (the lead writes the whole tree). The Spanish is in {@code es_es.json}.
 */
public final class NephilimLang {

    private NephilimLang() {
    }

    /** Registry names the title-cased id would get wrong. */
    static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("jack", "Jack Kline"),
            Map.entry("michael_vessel", "Michael"),
            Map.entry("michael_envoy", "Michael"),
            Map.entry("soulless_jack", "Jack"),
            Map.entry("shadow", "The Shadow"),
            Map.entry("nephilim_grace", "Nephilim's Grace"),
            Map.entry("nephilim_blood", "Nephilim's Blood"),
            Map.entry("archangel_bane_shard", "Shard of the Lance of Michael"),
            Map.entry("asmodeus_trophy", "Asmodeus's Bust"),
            Map.entry("michael_vessel_trophy", "Bust of Michael's Vessel"),
            Map.entry("soulless_jack_trophy", "Jack's Bust"),
            Map.entry("shadow_trophy", "Bust of the Shadow"),
            Map.entry("empty_floor", "Ground of the Empty"),
            Map.entry("empty_tar", "Black Tar"),
            Map.entry("empty_sleeper", "Sleeper"));

    static final Map<String, String> SUBTITLES = Map.ofEntries(
            Map.entry("asmodeus.cane", "Asmodeus's cane swings"),
            Map.entry("asmodeus.slam", "A cane strikes the ground"),
            Map.entry("asmodeus.grace", "Stolen grace burns"),
            Map.entry("asmodeus.summon", "Asmodeus snaps his fingers"),
            Map.entry("asmodeus.hurt", "Asmodeus hurts"),
            Map.entry("asmodeus.death", "Asmodeus dies"),
            Map.entry("michael_vessel.snap", "Michael snaps his fingers"),
            Map.entry("michael_vessel.choke", "An unseen hand closes"),
            Map.entry("michael_vessel.smite", "Michael's light strikes"),
            Map.entry("michael_vessel.flare", "Wings of light flare"),
            Map.entry("michael_vessel.hurt", "Michael hurts"),
            Map.entry("michael_vessel.expelled", "Michael is cast out"),
            Map.entry("michael_vessel.death", "Michael falls"),
            Map.entry("jack.smite", "Jack's grace strikes"),
            Map.entry("jack.shield", "A golden shield rises"),
            Map.entry("jack.burn", "Jack burns his grace"),
            Map.entry("jack.snap", "Jack snaps his fingers"),
            Map.entry("jack.nova", "A burst of golden light"),
            Map.entry("jack.hurt", "Jack hurts"),
            Map.entry("jack.death", "Jack falls"),
            Map.entry("shadow.lash", "A tendril of tar lashes"),
            Map.entry("shadow.engulf", "Tar surges"),
            Map.entry("shadow.lull", "The Empty whispers sleep"),
            Map.entry("shadow.roar", "The Shadow screams soundlessly"),
            Map.entry("shadow.hurt", "The Shadow ripples"),
            Map.entry("shadow.death", "The Shadow sinks back to sleep"),
            Map.entry("asmodeus.ambient", "Asmodeus hums"),
            Map.entry("asmodeus.emerge", "Asmodeus arrives"),
            Map.entry("asmodeus.roar", "Asmodeus laughs"),
            Map.entry("asmodeus.transform", "Stolen grace flares"),
            Map.entry("michael_vessel.ambient", "Michael's grace hums"),
            Map.entry("michael_vessel.emerge", "Michael arrives"),
            Map.entry("michael_vessel.roar", "Michael's voice rings out"),
            Map.entry("michael_vessel.transform", "Michael's wings unfold"),
            Map.entry("jack.ambient", "Jack's grace hums"),
            Map.entry("jack.emerge", "Jack comes down"),
            Map.entry("jack.roar", "Jack's power surges"),
            Map.entry("jack.transform", "Jack's power surges"),
            Map.entry("shadow.ambient", "The Empty breathes"),
            Map.entry("shadow.emerge", "The Shadow rises"),
            Map.entry("shadow.transform", "The tar boils"),
            // The Apocalypse World slice's (it asked for better than the title-cased fallback).
            Map.entry("au_angel_soldier.smite", "An angel soldier smites"),
            Map.entry("au_angel_soldier.hurt", "An angel soldier hurts"),
            Map.entry("au_angel_soldier.death", "An angel soldier dies"),
            Map.entry("au_michael.slash", "Michael's blade cuts"),
            Map.entry("au_michael.smite", "Michael's light strikes"),
            Map.entry("au_michael.gale", "Wings beat a gale"),
            Map.entry("au_michael.thunder", "The burned sky answers"),
            Map.entry("au_michael.hurt", "Michael hurts"),
            Map.entry("au_michael.flee", "Michael flees through the rift"));

    public static void add(BiConsumer<String, String> add) {
        journal(add);
        advancements(add);
        jack(add);
        rites(add);
        asmodeus(add);
        michael(add);
        soulless(add);
        empty(add);
    }

    private static void journal(BiConsumer<String, String> add) {
        add.accept("journal.supernaturalcraft.chapter.nephilim", "The Nephilim");
        // The crossroads demon's Knowledge names the next enemy and its weakness (every boss of seasons 12-14, all slices).
        crossroads(add, "ketch", "Arthur Ketch", "He is only a man: no ward stops him, but no trick of his survives a blow he did not see coming. "
                + "Keep moving when the smoke falls.");
        crossroads(add, "hess", "Dr. Hess", "Her ward halves what she takes; step out of it and hit her when it drops. Kill her agents first.");
        crossroads(add, "dagon", "Dagon, Prince of Hell", "A demon like any other to a demon-killing blade. Stay out of her ring of fire and "
                + "never stand still under her hand.");
        crossroads(add, "asmodeus", "Asmodeus, Prince of Hell", "Before he drinks the grace he is only a demon in a good suit; after, "
                + "watch the gold on the ground.");
        crossroads(add, "au_michael", "the Michael of the Apocalypse World", "An archangel: only the Lance made whole truly bites. He will "
                + "not die; drive him off.");
        crossroads(add, "michael_vessel", "Michael, in a vessel", "Say no and fight him in the open, or say yes and fight him in your own "
                + "head. Kill his improved monsters fast.");
        crossroads(add, "soulless_jack", "Jack, without his soul", "He is strongest in the air. Stay out of the gold and never let his "
                + "hand close near you.");
        crossroads(add, "shadow", "the Shadow", "Never stand in the violet when it lulls: sleep in the Empty is forever.");
    }

    private static void crossroads(BiConsumer<String, String> add, String id, String name, String weakness) {
        add.accept("book.supernaturalcraft.crossroads." + id + ".name", name);
        add.accept("book.supernaturalcraft.crossroads." + id + ".weakness", weakness);
    }

    private static void adv(BiConsumer<String, String> add, String id, String title, String desc) {
        add.accept("advancement.supernaturalcraft." + id, title);
        add.accept("advancement.supernaturalcraft." + id + ".desc", desc);
    }

    private static void advancements(BiConsumer<String, String> add) {
        adv(add, "keep_calm_and_carry_on", "Keep Calm and Carry On", "Hear the British Men of Letters' ultimatum");
        adv(add, "the_british_invasion", "The British Invasion", "Defeat Arthur Ketch");
        adv(add, "who_we_are", "Who We Are", "Defeat Dr. Hess at the British compound");
        adv(add, "trace_the_nephilim", "Something Is Coming", "Trace the nephilim to Kelly Kline's safe house");
        adv(add, "all_along_the_watchtower", "All Along the Watchtower", "Defeat Dagon, Prince of Hell, at Kelly's safe house");
        adv(add, "the_nephilim", "Lost and Found", "Be there when Jack is born");
        adv(add, "bring_em_back_alive", "Bring 'Em Back Alive", "Defeat Asmodeus, Prince of Hell");
        adv(add, "lance_of_michael", "The Spear Made Whole", "Work the shard of the Lance into Michael's Lance at the Hellforge");
        adv(add, "the_bad_place", "The Bad Place", "Cross the rift into the Apocalypse World");
        adv(add, "war_of_the_worlds", "War of the Worlds", "Free a prisoner camp in the Apocalypse World");
        adv(add, "let_the_good_times_roll", "Let the Good Times Roll", "Drive the Michael of the Apocalypse World out of his fortress");
        adv(add, "stranger_in_a_strange_land", "Stranger in a Strange Land", "Hear Michael's offer");
        adv(add, "the_spear", "The Spear", "Throw Michael out of his vessel for good");
        adv(add, "the_fall_of_the_son", "Absence", "Jack has traded his soul away");
        adv(add, "moriah", "Moriah", "Defeat Jack without his soul");
        adv(add, "into_the_empty", "Into the Empty", "Go into the Empty after him");
        adv(add, "despair", "Despair", "Put the Shadow back to sleep");
    }

    private static void jack(BiConsumer<String, String> add) {
        String d = "dialogue.supernaturalcraft.jack.";
        add.accept(d + "menu.born", "\"I'm still learning what I can do. What do you need?\"");
        add.accept(d + "menu.au", "\"I can feel the other world on me. It's heavy. What do you need?\"");
        add.accept(d + "menu.graceless", "\"I can't do what I used to. But I can still help. Can't I?\"");
        for (String mood : new String[]{"born", "au", "graceless"}) {
            add.accept(d + "menu." + mood + ".talk", "Talk");
            add.accept(d + "menu." + mood + ".blood", "Some of your blood (for the rift)");
            add.accept(d + "menu." + mood + ".stay", "Stay here");
            add.accept(d + "menu." + mood + ".follow", "Come with me");
            add.accept(d + "menu." + mood + ".bye", "Nothing");
        }
        String m = "message.supernaturalcraft.jack.";
        add.accept(m + "talk.born.0", "Jack: \"Castiel says I should be careful what I wish for. I don't think I wish for anything yet.\"");
        add.accept(m + "talk.born.1", "Jack: \"Is it normal to want to help everyone? Even the ones who want to hurt us?\"");
        add.accept(m + "talk.born.2", "Jack: \"Nougat is the best thing in the world. I checked.\"");
        add.accept(m + "talk.born.3", "Jack: \"My mother made a video for me. I watch it sometimes.\"");
        add.accept(m + "talk.au.0", "Jack: \"Everyone there is so afraid. Michael did that.\"");
        add.accept(m + "talk.au.1", "Jack: \"The people in the camps... we have to get them out.\"");
        add.accept(m + "talk.au.2", "Jack: \"My father is over there. I don't want to be like him.\"");
        add.accept(m + "talk.au.3", "Jack: \"If I'm strong enough, nobody has to die.\"");
        add.accept(m + "talk.graceless.0", "Jack: \"It's quiet inside me now. I don't like it.\"");
        add.accept(m + "talk.graceless.1", "Jack: \"I used to be able to fix things. Now I just watch.\"");
        add.accept(m + "talk.graceless.2", "Jack: \"Maybe there's a way to get it back. There's always a way.\"");
        add.accept(m + "talk.graceless.3", "Jack: \"I'm not useless. I'm not.\"");
        add.accept(m + "fell", "Jack is hurt; he goes away to heal. He'll be back.");
        add.accept(m + "stay", "Jack: \"I'll wait here.\"");
        add.accept(m + "follow", "Jack: \"Coming.\"");
        add.accept(m + "blood", "Jack cuts his palm and fills a vial for you. It glows.");
        add.accept(m + "blood_tomorrow", "Jack: \"I already gave you some today. Tomorrow.\"");
        add.accept(m + "no_grace", "Jack: \"There's nothing in it anymore. Not since Michael.\"");
        add.accept(m + "grace_spent", "Jack spent his grace driving Michael back. He is only a boy now.");
        add.accept(m + "leaving.0", "Jack: \"I found a way to get my power back. Don't be angry.\"");
        add.accept(m + "leaving.1", "Jack: \"It only cost me something I wasn't using.\"");
        add.accept(m + "leaving.2", "Jack is gone. Something about his smile was wrong.");
    }

    private static void rites(BiConsumer<String, String> add) {
        String j = "jei.supernaturalcraft.effect.supernaturalcraft.";
        add.accept(j + "open_apocalypse_rift", "Opens a rift into the Apocalypse World");
        add.accept(j + "summon_asmodeus", "Calls Asmodeus, Prince of Hell");
        add.accept(j + "summon_michael_vessel", "Calls Michael back into a vessel (rematch)");
        add.accept(j + "summon_soulless_jack", "Calls Jack, without his soul (rematch)");
        add.accept(j + "summon_shadow", "Wakes the Shadow (rematch, in the Empty)");
        add.accept(j + "open_the_empty", "Opens the way into the Empty (or out of it)");
        add.accept("message.supernaturalcraft.apocalypse_rift.elsewhere", "The rift can only be torn open in this world.");
        add.accept("message.supernaturalcraft.apocalypse_rift.unborn", "There is no nephilim to open the way yet.");
        add.accept("sigil.supernaturalcraft.eternal_sleep", "Eternal Sleep");
        add.accept("sigil.supernaturalcraft.eternal_sleep.desc", "The Empty's own sleep, taken from the Shadow: whatever it touches stops where it stands. "
                + "The greater enemies only nod.");
        add.accept("tooltip.supernaturalcraft.nephilim_grace", "Use: +%s max mana, once. Held up in the rite, it opens the Empty.");
        add.accept("message.supernaturalcraft.nephilim_grace.already", "You have already taken a nephilim's grace in.");
        add.accept("message.supernaturalcraft.nephilim_grace.taken", "Golden light fills you: +%s max mana.");
        add.accept("tooltip.supernaturalcraft.archangel_bane", "Made whole: ×1.5 damage against archangels");
        add.accept("screen.supernaturalcraft.hellforge.bane_lance_only", "Only Michael's Lance takes this shard");
        add.accept("screen.supernaturalcraft.hellforge.bane_done", "The Lance is already whole");
    }

    private static void bars(BiConsumer<String, String> add, String key, String p1, String p2, String p3) {
        add.accept("entity.supernaturalcraft." + key + ".bar.phase1", p1);
        add.accept("entity.supernaturalcraft." + key + ".bar.phase2", p2);
        add.accept("entity.supernaturalcraft." + key + ".bar.phase3", p3);
    }

    private static void titles(BiConsumer<String, String> add, String key, String[] titles, String[] subs) {
        for (int i = 0; i < titles.length; i++) {
            add.accept("boss.supernaturalcraft." + key + ".phase" + (i + 1), titles[i]);
            add.accept("boss.supernaturalcraft." + key + ".phase" + (i + 1) + ".sub", subs[i]);
        }
    }

    private static void arena(BiConsumer<String, String> add, String theme, String barrier, String noEscape) {
        add.accept("message.supernaturalcraft.arena.barrier." + theme, barrier);
        add.accept("message.supernaturalcraft.arena.no_escape." + theme, noEscape);
    }

    private static void asmodeus(BiConsumer<String, String> add) {
        bars(add, "asmodeus", "Asmodeus, the Gentleman", "Asmodeus, Stolen Grace", "Asmodeus, Prince of Hell");
        titles(add, "asmodeus", new String[]{"Asmodeus", "Stolen Grace", "A Prince of Hell"},
                new String[]{"\"Now, now. Let's be civilised.\"", "He drinks from the vial of Gabriel's grace", "\"I am the King of Hell now.\""});
        add.accept("message.supernaturalcraft.asmodeus.left", "Asmodeus tips his hat and is gone.");
        add.accept("message.supernaturalcraft.asmodeus.defeated", "The stolen grace burns out of Asmodeus. Among his things: a shard of the Lance of Michael.");
        add.accept("message.supernaturalcraft.asmodeus.rite.elsewhere", "Asmodeus will only answer in this world.");
        add.accept("message.supernaturalcraft.asmodeus.rite.unborn", "Asmodeus has no reason to come: there is no nephilim yet.");
        arena(add, "princes", "Asmodeus's court will not let you go", "A Prince of Hell does not let his guests leave early.");
    }

    private static void michael(BiConsumer<String, String> add) {
        String d = "dialogue.supernaturalcraft.michaels_offer.";
        add.accept(d + "offer", "\"I've seen how this world ends, hunter. I can make it better. Say yes, and we fix it together.\"");
        add.accept(d + "offer.yes", "Yes");
        add.accept(d + "offer.no", "No");
        add.accept(d + "again", "\"You can't keep running from me. Say yes.\"");
        add.accept(d + "again.yes", "Yes");
        add.accept(d + "again.no", "No");
        bars(add, "michael_vessel", "Michael, the General", "Michael, Archangel", "Michael, Cold Fire");
        titles(add, "michael_vessel", new String[]{"Michael", "The Archangel", "Cold Fire"},
                new String[]{"The general of another world", "Wings of light", "\"I've burned worlds better than this one.\""});
        String m = "message.supernaturalcraft.michael_vessel.";
        add.accept(m + "yes", "Light pours into you. Michael's strength is yours... for now.");
        add.accept(m + "no", "\"Then I'll take what I need.\" Michael comes for you in a vessel of his own.");
        add.accept(m + "wear.0", "Michael: \"Relax. I'll drive.\"");
        add.accept(m + "wear.1", "Michael: \"Feel that? That's what you could have been.\"");
        add.accept(m + "wear.2", "Michael: \"You're tiring. Let go.\"");
        add.accept(m + "wear.3", "Michael: \"Sleep now. I'll take it from here.\"");
        add.accept(m + "mindscape", "You wake inside your own head. Michael is between you and the way out.");
        add.accept(m + "expelled", "You throw Michael out of your head. You are yourself again.");
        add.accept(m + "left", "Michael is gone, for now.");
        add.accept(m + "defeated", "Michael is cast out of his vessel for good.");
        add.accept(m + "rite.elsewhere", "Michael will only answer in this world.");
        arena(add, "mindscape", "There is no way out of your own head", "You can't walk away from yourself.");
        arena(add, "vessel", "Michael's light walls you in", "Michael is not done with you.");
    }

    private static void soulless(BiConsumer<String, String> add) {
        bars(add, "soulless_jack", "Jack, the Boy", "Jack, the Nephilim", "Jack, the Torn Sky");
        titles(add, "soulless_jack", new String[]{"Jack", "The Nephilim", "The Torn Sky"},
                new String[]{"Without his soul", "\"I'm doing what's right.\"", "The sky splits around him"});
        String m = "message.supernaturalcraft.soulless_jack.";
        add.accept(m + "comes", "Jack is here. His eyes are gold, and he isn't smiling.");
        add.accept(m + "left", "Jack is gone, for now.");
        add.accept(m + "defeated", "Jack falls. Black tar rises out of the ground and takes him into the Empty.");
        add.accept(m + "rite.elsewhere", "Jack will only come in this world.");
        arena(add, "nephilim", "A nephilim's power holds you", "Jack won't let you leave.");
    }

    private static void empty(BiConsumer<String, String> add) {
        bars(add, "shadow", "The Shadow, Woken", "The Shadow, Angry", "The Shadow, the Empty Itself");
        titles(add, "shadow", new String[]{"The Shadow", "Angry", "The Empty Itself"},
                new String[]{"It wanted to sleep", "\"You woke me.\"", "Everything here sleeps. Everything."});
        add.accept("message.supernaturalcraft.shadow.wakes", "Something that has slept since before Creation opens its eyes.");
        add.accept("message.supernaturalcraft.shadow.left", "The Shadow sinks back into the dark.");
        add.accept("message.supernaturalcraft.shadow.defeated", "The Shadow sinks back to sleep. The Empty is quiet again.");
        add.accept("message.supernaturalcraft.shadow.rite.elsewhere", "The Shadow can only be woken in the Empty.");
        String m = "message.supernaturalcraft.the_empty.";
        add.accept(m + "arrive", "The Empty. It is dark, and everything here is asleep.");
        add.accept(m + "leave", "The dark lets you go.");
        add.accept(m + "elsewhere", "The way into the Empty only opens in this world.");
        add.accept(m + "not_yet", "Nothing of yours is in the Empty yet.");
        arena(add, "the_empty", "The dark holds you", "Nothing leaves the Empty while the Shadow is awake.");
    }
}
