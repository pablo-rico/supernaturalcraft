package org.papiricoh.supernaturalcraft.datagen.gabriel;

import java.util.function.BiConsumer;

/**
 * Gabriel's server-side text (v0.14): the advancements, the boss bar, his lines, the cinematics' titles, the items'
 * tooltips, the pranks' TV lines and the quiz. Owned by the server work; the HUD's and the book's text is {@link GabrielUiLang}.
 */
public final class GabrielLang {

    private GabrielLang() {
    }

    public static void add(BiConsumer<String, String> add) {
        add.accept("advancement.supernaturalcraft.trickster_sighted", "Somebody's Laughing");
        add.accept("advancement.supernaturalcraft.trickster_sighted.desc", "Notice three of the Trickster's pranks");
        add.accept("advancement.supernaturalcraft.changing_channels", "Changing Channels");
        add.accept("advancement.supernaturalcraft.changing_channels.desc", "Beat the Trickster at his own game in TV Land");

        add.accept("entity.supernaturalcraft.gabriel.bar.sitcom", "Gabriel — CH 2: The Sitcom");
        add.accept("entity.supernaturalcraft.gabriel.bar.game_show", "Gabriel — CH 5: Nutcracker!");
        add.accept("entity.supernaturalcraft.gabriel.bar.hospital", "Gabriel — CH 7: Dr. Sexy, M.D.");
        add.accept("entity.supernaturalcraft.gabriel.bar.commercial", "Gabriel — CH 9: A Word from Our Sponsor");

        add.accept("jei.supernaturalcraft.effect.supernaturalcraft.summon_gabriel", "Baits the Trickster into TV Land");

        // His lines.
        String m = "message.supernaturalcraft.gabriel.";
        add.accept(m + "busy", "Another fight holds this world. The Trickster can wait.");
        add.accept(m + "welcome", "Gabriel: \"Welcome to TV Land!\"");
        add.accept(m + "break", "Gabriel: \"We'll be right back after these messages!\"");
        add.accept(m + "or_was_it", "Gabriel: \"...or was it?\"");
        add.accept(m + "cage_gone", "The set goes dark. Somebody switched off TV Land.");
        add.accept(m + "victorious", "Gabriel: \"And that's a wrap! Cancelled after one season. Tough crowd.\"");

        // The cinematics' cards.
        String c = "cinematic.supernaturalcraft.gabriel.";
        add.accept(c + "title", "Gabriel");
        add.accept(c + "subtitle", "The Trickster");
        add.accept(c + "break.title", "Commercial Break");
        add.accept(c + "phase2.subtitle", "Now on CH 5: Nutcracker!");
        add.accept(c + "phase3.subtitle", "Now on CH 7: Dr. Sexy, M.D.");
        add.accept(c + "phase4.subtitle", "And now, a word from our sponsor");
        add.accept(c + "death.subtitle", "The archangel who ran away from home");
        add.accept(c + "victory.title", "That's a Wrap");
        add.accept(c + "victory.subtitle", "...or was it?");

        // The items.
        String t = "tooltip.supernaturalcraft.";
        add.accept(t + "trickster_remote", "Changes a creature's channel");
        add.accept(t + "trickster_remote.use", "Use on a creature: it shrinks, dresses up, or turns into something else its size. Never a boss, a person or a named creature.");
        add.accept(t + "gabriel_blade", "Now and then, two doubles of you strike beside you");
        add.accept(t + "trickster_candy", "Something good happens. Probably.");

        // The pranks: a villager with a line off the TV.
        String p = "message.supernaturalcraft.trickster.";
        add.accept(p + "villager_says", "<%s> %s");
        String[] lines = {
                "I'm not a doctor, but I play one on TV.",
                "We'll be right back after these messages.",
                "Did you hear the studio audience? Nobody else did either.",
                "Nutcracker!",
                "Stay tuned!",
                "Previously, on this village...",
                "This program was brought to you by Trickster Treats.",
                "And... scene!"};
        for (int i = 0; i < lines.length; i++) add.accept(p + "tv_line." + i, lines[i]);

        // The game show: lore of the hunt, the right answer first (the platforms shuffle them).
        question(add, "colt_bullets", "How many chambers has the Colt?", "Five", "Six", "Seven");
        question(add, "salt_line", "What can never cross a line of salt?", "A ghost", "An angel", "A great one");
        question(add, "yellow_eyes", "Whose eyes burn yellow?", "Azazel's", "Lilith's", "Death's");
        question(add, "colt_rounds", "How many Colt rounds does one rite forge?", "Eight", "Six", "Twelve");
        question(add, "first_demon", "Who was the very first demon?", "Lilith", "Azazel", "Crowley");
        question(add, "last_seal", "Who was the last of the sixty-six seals?", "Lilith", "Azazel", "Metatron");
        question(add, "cage_key", "Whose blood forges the Key to the Cage?", "Azazel's", "Lilith's", "Lucifer's");
        question(add, "cracked_key", "What does Lucifer leave behind if he wins?", "A cracked key", "His trophy", "A fallen star");
        question(add, "lucifer_last_face", "What is Lucifer's last face before the end?", "Archangel Unbound", "The Fallen", "The Prisoner");
        question(add, "darkness_called", "When can the Darkness be called?", "Under an eclipse", "At full moon", "In a storm");
        question(add, "amara_wells", "What weakens Amara while it burns?", "A well of holy fire", "A black candle", "A beacon");
        question(add, "hymn_notes", "How many notes wake the Broken Chorus?", "Three", "Seven", "Five");
        question(add, "chorus_weather", "In what weather is the Broken Chorus woken?", "A thunderstorm", "Clear skies", "Snowfall");
        question(add, "metatron_name", "What do you write in a book to call Metatron?", "His name", "A psalm", "Your own name");
        question(add, "metatron_word", "Which of these is one of Metatron's Words?", "KNEEL", "RUN", "JUMP");
        question(add, "michael_asks", "What does Michael ask you for?", "Your yes", "Your blade", "Your name");
        question(add, "michael_true_form", "Where does Michael's true form stand up?", "The Throne Room", "The Garden", "The Cage");
        question(add, "host_captain", "What breaks the ranks of the Host of Heaven?", "Their captain falling", "Holy water", "A salt line");
        question(add, "war_parry", "How do you stagger War?", "A shield, just in time", "Holy water", "Hit his horse");
        question(add, "famine_food", "What happens to food you eat near Famine?", "It feeds him", "It heals you twice", "It poisons you");
        question(add, "pestilence_flies", "What is the only thing that takes Pestilence's flies?", "Fire", "Holy water", "Salt");
        question(add, "death_clock", "What winds your clock back against Death?", "Hitting Death", "Eating", "Praying");
        question(add, "death_rings", "Win or lose, what does Death give back?", "The three rings", "His scythe", "Your soul");
        question(add, "ghost_rest", "How is a ghost laid to rest for good?", "Salt and burn the bones", "Cold iron", "Holy water");
        question(add, "hellhound_sight", "What shows a hellhound for what it is?", "Holy water", "Salt", "Chalk");
        question(add, "azazel_trap", "What still holds Azazel?", "Samuel Colt's rails", "A devil's trap", "A line of salt");
        question(add, "deal_sealed", "How is a crossroads deal sealed?", "With a kiss", "With blood", "With a handshake");
        question(add, "deal_due", "What comes for you when a deal falls due?", "Hellhounds", "Reapers", "The Host of Heaven");
        question(add, "angel_fire", "What fire does an angel fear?", "Holy oil", "Hellfire", "Soul fire");
        question(add, "author_home", "Where does the Author write?", "A lonely cabin", "In the Cage", "A Hymnal Spire");
        question(add, "hex_bag", "What breaks a curse bag?", "Fire", "Salt", "Holy water");
        question(add, "starting_mana", "How much mana does a hunter start with?", "A hundred", "Fifty", "Two hundred");
        question(add, "lilith_light", "What should stand between you and Lilith's light?", "A headstone", "A salt line", "A devil's trap");
        question(add, "human_free_will", "What does a hunter who stays human keep?", "Their free will", "Their wings", "Their Grace");
    }

    /** A quiz question: the right answer first (the platforms shuffle them). */
    public static void question(BiConsumer<String, String> add, String id, String q, String right, String wrong1, String wrong2) {
        add.accept("quiz.supernaturalcraft.gabriel." + id + ".q", q);
        add.accept("quiz.supernaturalcraft.gabriel." + id + ".a0", right);
        add.accept("quiz.supernaturalcraft.gabriel." + id + ".a1", wrong1);
        add.accept("quiz.supernaturalcraft.gabriel." + id + ".a2", wrong2);
    }
}
