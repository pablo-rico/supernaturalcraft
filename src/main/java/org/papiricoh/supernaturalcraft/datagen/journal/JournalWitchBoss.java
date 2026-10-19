package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.bowl;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * The ending of the witches' road (v0.21 P6): the choice with the Book of the Damned (a hint before it, the outcome after), the
 * witch boss slot in its two variants (Rowena at her house for the Book's keeper, the Grand Witch by the Coven's rite for its
 * giver), Rowena at the hunters' side, the Grand Witch at peace, their spoils (Rowena's Grimoire, the Coven Staff), the cure for
 * the Mark ({@code unmark}: the spell and both ceremonies) and the last lines. Every number here is the code's
 * ({@code RowenaBalance}, {@code GrandWitchBalance}, {@code RowenaSummoning}, {@code Unmarking}, {@code RowenaLessons},
 * {@code GrandWitchNpcEntity}).
 */
final class JournalWitchBoss {

    private JournalWitchBoss() {
    }

    static void add(List<SNJournal.Entry> out) {
        choice(out);
        bosses(out);
        spoils(out);
        cure(out);
    }

    // --- the choice -------------------------------------------------------------------------------------------------------

    private static void choice(List<SNJournal.Entry> out) {
        out.add(entry("the_choice", JournalChapter.PLACES).order(19).icon(AllItems.BOOK_OF_THE_DAMNED)
                .unlock(any(adv("king_of_the_damned"), adv("gave_the_book"), adv("kept_the_book")))
                .title("What to Do with the Book")
                .text("Abaddon is dead and her Book is in my pack. Somewhere in it is the cure for the Mark of Cain, and only a witch "
                        + "can read it. I know exactly one witch who would not kill me on sight.")
                .text("Take it to Rowena and speak of it. She will ask for it. Give it to her and she is in your debt, and against her "
                        + "old sisters of the Grand Coven she will stand with you. Keep it and she will not forgive you: she will want it "
                        + "back, and she will take it if she can.")
                .text("Either way it is final. You choose once, for good, and a second copy of the Book changes nothing.")
                .items("The Book of the Damned", AllItems.BOOK_OF_THE_DAMNED));

        out.add(entry("book_given", JournalChapter.PLACES).order(20).icon(AllItems.ROWENAS_CALLING_CARD)
                .unlock(adv("gave_the_book"))
                .title("The Book, Given")
                .text("I gave Rowena the Book. She took it like a cat takes cream, and for a moment she almost looked grateful. She trusts "
                        + "me more now, which is to say a little.")
                .text("The Grand Coven will not let her keep it. Ask her how to call them: she will tell you, and when their Grand Witch "
                        + "is dead she will take the Mark off you herself.")
                .text("She is my ally now. She teaches me as before, and she keeps her bargains. Mostly."));

        out.add(entry("book_kept", JournalChapter.PLACES).order(21).icon(AllItems.BOOK_OF_THE_DAMNED)
                .unlock(adv("kept_the_book"))
                .title("The Book, Kept")
                .text("I kept the Book. Rowena laughed, and it was not a nice laugh. She will not teach me or sell me anything again, "
                        + "and she greets me with nothing but threats.")
                .text("She wants it back. Read the Book in her house at night and she will come for it: that is the fight, on her "
                        + "ground. Win, and the Grand Coven, who never forgave her, will owe me for it.")
                .text("Lost the Book? Abaddon's copy comes back to whoever fights her again."));
    }

    // --- the witch boss slot ----------------------------------------------------------------------------------------------

    private static void bosses(List<SNJournal.Entry> out) {
        out.add(entry("rowena_boss", JournalChapter.BOSSES).order(52).icon(AllItems.ROWENA_TROPHY)
                .unlock(any(seen(AllEntities.ROWENA.get()), adv("kept_the_book"), adv("rowena_defeated")))
                .creature(AllEntities.ROWENA.get())
                .title("Rowena, Unbound")
                .entity(AllEntities.ROWENA.get(), "Rowena, done being polite")
                .text("Only for the one who kept the Book. Read it in her house at night, within the twelve-block ring round the middle "
                        + "of her house (or a few blocks past it), and she steps out of yellow smoke. Her house is the arena, and it is "
                        + "put back as it was afterwards. Three phases, a third of her strength each.")
                .text("The Witch at Home: a flick of the wrist down a violet lane that throws whoever is in it; Ignis, a red circle "
                        + "where you stand and then fire; Glacies, a blue one, and frost that slows you to a crawl.")
                .text("The Familiar and the Hexed: a raven answers her and goes for a lock of your hair. While it holds one, every six "
                        + "seconds she sends a curse down it, pain or frailty, wherever you hide. Kill the raven and the lock drops; she "
                        + "calls another twenty seconds later. Her hexed victims shamble at you: kill one and she heals four parts in a "
                        + "hundred; free it with Break Hex or a salt round and it costs her three. Yellow smoke where she is about to "
                        + "appear: step out of it.")
                .text("The Recitation: she rises into the air and recites over three bowls for fifteen seconds. Smash them: three "
                        + "blows each. Break all three and she drops, stunned for five seconds and taking half as much again. Leave any "
                        + "whole and for the last three seconds the whole house glows red; then the hex sweeps it, twelve for every bowl "
                        + "still standing.")
                .text("When she falls the Grand Witch walks out of the smoke for whoever kept the Book. Read it again while sneaking, in "
                        + "her house at night, for a rematch.")
                .items("What she leaves: her Grimoire and her bust the first time (a bust in a rematch)", AllItems.ROWENAS_GRIMOIRE,
                        AllItems.ROWENA_TROPHY));

        out.add(entry("grand_witch", JournalChapter.BOSSES).order(53).icon(AllItems.GRAND_WITCH_TROPHY)
                .unlock(any(seen(AllEntities.GRAND_WITCH.get()), adv("gave_the_book"), adv("grand_witch_defeated")))
                .creature(AllEntities.GRAND_WITCH.get())
                .title("The Grand Witch")
                .entity(AllEntities.GRAND_WITCH.get(), "The Grand Witch of the Grand Coven")
                .text("Only for the one who gave Rowena the Book: the Coven's rite calls her, wherever it is done, and she steps out of "
                        + "the smoke a few blocks from the altar with three sisters round her. Three phases, a third of her strength each.")
                .recipe(ritual("call_the_grand_coven"), "Calling the Grand Coven")
                .text("The Coven's Shield: three coven witches channel into her from seven blocks away, and while even one still "
                        + "channels, every blow on her is swallowed. Break a channeler's own bubble (blows, a salt round, Break Hex) and "
                        + "she stops for twelve seconds; kill her and she is gone for good. Then the shield falls.")
                .text("The Matriarch's Hexes: a blue circle under every hunter and then frost; an inferno, a red circle six blocks round "
                        + "her; and a violet circle that crushes and pins whoever is still in it.")
                .text("The Mass Taglock: she holds a lock of everyone's hair. A red circle follows each hunter for two seconds, stops, "
                        + "and then the curse lands, twenty and frailty, on everyone not standing in one of the two green circles Rowena "
                        + "draws. Rowena arrives for this part: she cannot be hurt, and she throws bolts at the coven. Yellow smoke where "
                        + "the Grand Witch is about to land.")
                .items("What she leaves: the Coven Staff and her bust the first time (a bust in a rematch)", AllItems.COVEN_STAFF,
                        AllItems.GRAND_WITCH_TROPHY));

        out.add(entry("rowena_ally", JournalChapter.BOSSES).order(54).icon(AllItems.ROWENAS_CALLING_CARD)
                .unlock(any(seen(AllEntities.ROWENA_ALLY.get()), adv("grand_witch_defeated")))
                .creature(AllEntities.ROWENA_ALLY.get())
                .title("Rowena at Your Side")
                .entity(AllEntities.ROWENA_ALLY.get(), "Rowena, on your side for once")
                .text("When the Grand Witch's last phase begins, Rowena comes out of her own smoke beside you. Nothing touches her. She "
                        + "keeps near the hunters, throws a bolt at the coven every five seconds, and when the Grand Witch takes everyone's "
                        + "lock of hair she draws the green circles to stand in.")
                .text("When it is over she watches the Grand Witch fall, and goes home. That is where she will take the Mark off you."));

        out.add(entry("grand_witch_npc", JournalChapter.PLACES).order(22).icon(AllItems.GRAND_WITCH_TROPHY)
                .unlock(any(seen(AllEntities.GRAND_WITCH_NPC.get()), adv("rowena_defeated")))
                .creature(AllEntities.GRAND_WITCH_NPC.get())
                .title("The Grand Witch at Peace")
                .entity(AllEntities.GRAND_WITCH_NPC.get(), "The Grand Witch, owing a debt")
                .text("Rowena's death pleased the Grand Coven. When she falls to someone who kept the Book, the Grand Witch walks out of "
                        + "the smoke on the battlefield. She cannot be harmed, and to anyone else she has nothing to say.")
                .text("To you she owes one thing: the Mark taken off. She reads the cure from the Book, so you must carry it (she does "
                        + "not keep it), and she wants a vial of your own bound blood.")
                .text("She leaves after two minutes with nobody near. Read the Book in Rowena's house again and she comes back."));
    }

    // --- the spoils -------------------------------------------------------------------------------------------------------

    private static void spoils(List<SNJournal.Entry> out) {
        out.add(entry("rowenas_grimoire", JournalChapter.ARSENAL).order(29).icon(AllItems.ROWENAS_GRIMOIRE)
                .unlock(any(has(AllItems.ROWENAS_GRIMOIRE), adv("rowena_defeated")))
                .title("Rowena's Grimoire")
                .text("Her own book, left to whoever beats her the first time: a tier IV catalyst. It remembers the last aimed spell you "
                        + "spoke at a spell bowl while you carried it (a curse, a binding, a banishing: anything that needs a target, "
                        + "never a brew) and what named its target.")
                .text("Use it and it speaks that spell again, at once, with no bowl and no recitation, for the spell's usual mana, at "
                        + "most once a minute. Nothing from the remembered bowl is spent. Hold a taglock or a vial of bound blood in your "
                        + "other hand and it aims at them instead. If the spell finds nothing to act on, it does not count.")
                .items("Rowena's Grimoire", AllItems.ROWENAS_GRIMOIRE));

        out.add(entry("coven_staff", JournalChapter.ARSENAL).order(30).icon(AllItems.COVEN_STAFF)
                .unlock(any(has(AllItems.COVEN_STAFF), adv("grand_witch_defeated")))
                .title("The Coven Staff")
                .text("The Grand Witch's own staff, left to whoever beats her the first time: a tier IV Witchcraft catalyst. Held "
                        + "opposite a grimoire, Witchcraft sigils cost a fifth less and strike a quarter harder.")
                .text("Its own spell is the Coven's hex: the foe it points at, up to eighteen blocks away, takes nine (more as the staff "
                        + "ascends) and, unless it is a great enemy or a player, eight seconds of the Curse of Frailty. Ten mana.")
                .items("The Coven Staff", AllItems.COVEN_STAFF));
    }

    // --- the cure ---------------------------------------------------------------------------------------------------------

    private static void cure(List<SNJournal.Entry> out) {
        out.add(entry("call_the_grand_coven", JournalChapter.RITUALS).order(7).icon(AllItems.ROWENAS_CALLING_CARD)
                .unlock(any(adv("gave_the_book"), adv("grand_witch_defeated")))
                .title("Calling the Grand Coven")
                .text("Rowena told me how, once I had given her the Book. A great circle by night in the Overworld; bound blood, "
                        + "hexed ink, two amethyst shards, a ghast tear, a candle and two nether warts on the altar; her calling card "
                        + "to wake it (it is not spent); a hundred and twenty mana.")
                .recipe(ritual("call_the_grand_coven"), "Calling the Grand Coven")
                .text("Only someone who gave her the Book can do it. The fight opens round the altar, eighteen blocks wide."));

        out.add(entry("unmark", JournalChapter.BOWL).order(20).icon(AllItems.BOUND_BLOOD)
                .unlock(any(adv("gave_the_book"), adv("kept_the_book"), adv("unmarked")))
                .title("Unmark")
                .text("The cure for the Mark of Cain, out of the Book of the Damned. Only a witch who outlived your witch fight can "
                        + "work it: Rowena if you gave her the Book and the Grand Witch fell, the Grand Witch if you kept it and Rowena "
                        + "fell. The price is a vial of your own bound blood.")
                .text("The simple way is to ask her. Rowena does it in her house; the Grand Witch wherever she stands, reading from the "
                        + "Book you kept, so carry it. Either takes the vial from your pack.")
                .text("Or speak it at a bowl yourself, with the witch within eight blocks of it and, if you kept the Book, the Book on "
                        + "you (it is not spent). Dragon's breath, honey and holy water; your bound blood, an echo shard, an amethyst "
                        + "shard and a ghast tear; a hundred and fifty mana. It is a Master's page found in no chest: Rowena sells it "
                        + "only to an ally who saw the Grand Witch fall, for thirty-two emeralds, eight gold and an echo shard.")
                .recipe(bowl("unmark"), "Unmark")
                .items("The price", AllItems.BOUND_BLOOD, Items.ECHO_SHARD));

        out.add(entry("unmarked", JournalChapter.PLACES).order(23).icon(AllItems.FIRST_BLADE)
                .unlock(adv("unmarked"))
                .title("Unmarked")
                .text("The Mark is gone. My arm is just an arm. The First Blade is just a very old bone.")
                .text("If I was a Knight of Hell I still am: the rank stays, the Thirst does not. Nobody will put the Mark back on me "
                        + "behind my back.")
                .text("Cain's grave would still give it to me if I asked. It would come back hungry, half-way to bloodlust from the "
                        + "first day. I am not going to ask.")
                .text("Rowena would say I owe her. She would be right. She usually is."));
    }
}
