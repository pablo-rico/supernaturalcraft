package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.bowl;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.craft;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.glyph;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * The Leviathans (v0.25): the Old Ones in borrowed skins, borax, the severed head, SucroCorp's food, the righteous bone and
 * Dick Roman. Every number here is the code's ({@code LeviathanRules}, {@code DickBalance}, {@code DickSpoils}).
 */
final class JournalLeviathans {

    private JournalLeviathans() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("leviathan", JournalChapter.MONSTERS).order(63).icon(AllItems.BORAX)
                .unlock(any(seen(AllEntities.LEVIATHAN.get()), adv("free_to_be_you_and_me")))
                .creature(AllEntities.LEVIATHAN.get())
                .title("Leviathans")
                .entity(AllEntities.LEVIATHAN.get(), "A Leviathan, its head split open")
                .text("The first beasts God made, locked in Purgatory before there was anything else. When Raphael fell they came out "
                        + "with him, and now they walk the villages in borrowed faces: at night one takes a villager nobody is looking "
                        + "at and wears its face, three to a village at most.")
                .text("In a borrowed face it keeps to itself and hands out free samples of SucroCorp food. Second Sight or the "
                        + "Spellwright's Spectacles show it for what it is, and so does any blow: its head splits into a maw full of "
                        + "teeth and it hunts anyone who is not sedated.")
                .text("Ordinary harm barely touches one (about a third of every blow), and it knits back a heart every two seconds. "
                        + "Borax burns it and stops the knitting for ten seconds; while it burns, every blow lands in full. The Men of "
                        + "Letters send hunters to villages they hold, too."));

        out.add(entry("decapitation", JournalChapter.MONSTERS).order(64).icon(AllItems.BORAX_SPRAY)
                .unlock(any(seen(AllEntities.LEVIATHAN_HEAD.get()), seen(AllEntities.LEVIATHAN.get()), adv("slash_fiction")))
                .creature(AllEntities.LEVIATHAN_HEAD.get())
                .title("Off With Its Head")
                .entity(AllEntities.LEVIATHAN_HEAD.get(), "A severed head, crawling home")
                .text("A sword or an axe through the neck of a Leviathan at half its health or less takes the head off. The body "
                        + "drops and lies helpless; the head crawls back to it on its tongue, and if they meet the Leviathan gets up "
                        + "again at half its health.")
                .text("Blows do not hurt the head; they only knock it away. Keep it more than twelve blocks from its body for five "
                        + "seconds, bag it with an empty hand, burn it or throw borax on it, and the Leviathan is done for. A body "
                        + "left without its head for a minute dies anyway."));

        out.add(entry("killing_leviathans", JournalChapter.MONSTERS).order(66).icon(AllItems.BORAX_SPRAY)
                .unlock(any(seen(AllEntities.LEVIATHAN.get()), adv("free_to_be_you_and_me"), adv("hello_cruel_world")))
                .title("How to Kill a Leviathan")
                .text("Bobby's notes, copied out clean. One: find it. In a village after Raphael's fall, distrust anyone too eager "
                        + "to hand you a free burger. Second Sight or the Spellwright's Spectacles show the maw under the face; a "
                        + "splash of borax does it for anyone. Do not eat what they hand out: a sedated hunter cannot see them.")
                .text("Two: burn it. Ordinary blows land for a third and it knits back a heart every two seconds. Borax (thrown, "
                        + "rubbed in, or Leviathan's Bane) burns it for ten seconds: no knitting, and every blow lands in full. Keep it "
                        + "burning.")
                .text("Three: take the head. At half its health or less, a sword or an axe through the neck takes the head off. "
                        + "The body drops. Do not stop there.")
                .text("Four: finish the head. It crawls home on its tongue; if it gets there, the Leviathan stands up again. Bag it "
                        + "with an empty hand, burn it, borax it, or knock it twelve blocks off and keep it there for five seconds.")
                .recipe(craft("borax_spray"), "Never go without it"));

        out.add(entry("red_handed", JournalChapter.MONSTERS).order(67).icon(AllItems.TURDUCKEN_SLAMMER)
                .unlock(adv("meet_the_new_boss"))
                .title("Caught Red-Handed")
                .text("Now you have seen one with your own eyes: a neighbour's face sliding off like a wet paper bag, and the maw "
                        + "underneath. They are patient. They take one villager at a time, at night, when nobody is looking — and "
                        + "sometimes when somebody is.")
                .text("Count the villagers. A village never loses its last two to them, and never holds more than three at once: if "
                        + "the baker has stopped sleeping and keeps offering you sandwiches, you know where to start."));

        out.add(entry("borax", JournalChapter.ARSENAL).order(40).icon(AllItems.BORAX)
                .unlock(any(has(AllItems.BORAX), has(AllItems.BORAX_BLEND), seen(AllEntities.LEVIATHAN.get())))
                .title("Borax")
                .text("Plain laundry borax is the one thing in the world that burns a Leviathan. Grind two salt, sand and bone meal "
                        + "into a blend, and bake it.")
                .recipe(craft("borax_blend"), "Borax blend")
                .recipe(craft("borax_from_smelting"), "Baked into borax")
                .text("Rub a box straight into a Leviathan, or bottle it to throw: it bursts like a splash potion, burns every "
                        + "Leviathan it touches, sheds their disguises, finishes a severed head and dries their black goo.")
                .recipe(craft("borax_spray"), "Borax spray"));

        out.add(entry("sucrocorp", JournalChapter.MONSTERS).order(65).icon(AllItems.TURDUCKEN_SLAMMER)
                .unlock(any(has(AllItems.TURDUCKEN_SLAMMER), has(AllItems.BIGGERSONS_BURGER)))
                .title("SucroCorp")
                .text("Biggerson's new menu, the Turducken Slammer above all, is very filling and laced with SucroCorp's additive. "
                        + "Whoever eats it is sedated for minutes: slow, unable to run, and blind to a Leviathan's borrowed face.")
                .text("The Leviathans leave sedated people alone: cattle are not hunted, only fattened. Milk or a splash of holy "
                        + "water clears the head.")
                .items("On the menu", AllItems.TURDUCKEN_SLAMMER, AllItems.BIGGERSONS_BURGER));

        out.add(entry("righteous_bone", JournalChapter.RITUALS).order(30).icon(AllItems.ANOINTED_RIGHTEOUS_BONE)
                .unlock(any(has(AllItems.RIGHTEOUS_BONE), has(AllItems.FALLEN_BLOOD), adv("free_to_be_you_and_me")))
                .title("The Righteous Bone")
                .text("Only one thing kills the head of the Leviathans: the bone of a righteous mortal, washed in the blood of three "
                        + "who fell: a fallen angel, a King of Hell and an Alpha.")
                .text("The bone is a nun's, kept in the crypt chest of an abandoned church. Raphael leaves his own blood (and the page "
                        + "to work it); a crossroads demon killed in a fight may leave a King's; a vampire, now and then, an Alpha's.")
                .recipe(bowl("anoint_the_bone"), "Anointing the bone")
                .items("The bone and the three bloods", AllItems.RIGHTEOUS_BONE, AllItems.FALLEN_BLOOD, AllItems.KINGS_BLOOD,
                        AllItems.ALPHA_BLOOD));

        out.add(entry("dick_roman", JournalChapter.BOSSES).order(55).icon(AllItems.ANOINTED_RIGHTEOUS_BONE)
                .unlock(any(seen(AllEntities.DICK_ROMAN.get()), has(AllItems.ANOINTED_RIGHTEOUS_BONE), adv("survival_of_the_fittest")))
                .creature(AllEntities.DICK_ROMAN.get())
                .title("Dick Roman")
                .entity(AllEntities.DICK_ROMAN.get(), "Dick Roman, head of Roman Enterprises")
                .text("The head of the Leviathans wears a good suit and a better smile. He is called in the Overworld once Raphael "
                        + "has fallen, with the anointed bone to wake a great circle; the bone comes back if you lose. Optional: "
                        + "nothing else needs him.")
                .recipe(ritual("summon_dick_roman"), "A meeting with Dick Roman")
                .text("The Executive: a red ring round him throws everyone out of it; violet lanes are a contract, and whoever "
                        + "stands in one when he signs is bound; now and then his staff walk in, and they are not villagers.")
                .text("The Maw: his head splits. A red cone is a bite; a red lane, a lunge that swallows the first hunter in it and "
                        + "spits them back out; red circles, black goo that stays and burns.")
                .text("The Copy: he takes a hunter's face and hides among doubles in it. Hidden he takes almost nothing. Borax shows "
                        + "him: the doubles burst and he staggers, every blow landing harder.")
                .text("The Flood: the boardroom drowns in goo that slows and burns. Borax dries islands in it, until a yellow ring "
                        + "warns that the goo is rolling back. Tentacles rise behind you down red lanes.")
                .items("What he leaves: a shard of the Leviathan Tablet and his own borax sprayer the first time; borax after",
                        AllItems.LEVIATHAN_TABLET, AllItems.BORAX_SPRAYER));

        out.add(entry("leviathan_tablet", JournalChapter.SIGILS).order(40).icon(AllItems.LEVIATHAN_TABLET)
                .unlock(any(has(AllItems.LEVIATHAN_TABLET), adv("survival_of_the_fittest")))
                .title("The Leviathan Tablet")
                .image(glyph("leviathans_bane"), 32, 32, "Leviathan's Bane")
                .text("A shard of the Word of God, the tablet that tells how the Leviathans end. Read, it teaches Leviathan's Bane "
                        + "(second tier, Hunter): a burst of borax that burns whatever borax burns and dries the goo where it lands.")
                .text("Roman Enterprises' sprayer comes with it: a cone of borax five blocks long, forty-eight puffs; sneak and use it "
                        + "with borax in your pack to refill sixteen a box."));
    }
}
