package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * Abaddon, the last Knight of Hell (v0.21 P5): her rite and her fight, her knights and her herald, and what she leaves (the
 * Book of the Damned and the Knights' Cleaver). Every number here is the code's ({@code AbaddonBalance}, {@code AbaddonSpoils}).
 */
final class JournalAbaddon {

    private JournalAbaddon() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("abaddon", JournalChapter.BOSSES).order(49).icon(AllItems.KNIGHTS_CLEAVER)
                .unlock(any(seen(AllEntities.ABADDON.get()), adv("king_of_the_damned"), adv("first_born")))
                .creature(AllEntities.ABADDON.get())
                .title("Abaddon")
                .entity(AllEntities.ABADDON.get(), "Abaddon, Knight of Hell")
                .text("The last Knight of Hell, the one Cain did not finish. She does not come unless called: a great circle by night "
                        + "in the Overworld, once Cain and three of the Horsemen (War, Famine and Pestilence) have fallen to you, with "
                        + "the First Blade to wake it (it is not spent).")
                .recipe(ritual("summon_abaddon"), "Calling Abaddon")
                .text("Most blades barely touch her: she takes about a third of every blow. The First Blade lands in full, and so does "
                        + "every blow of a hunter who bears the Mark of Cain. A devil's trap holds her for six seconds, every blow "
                        + "landing in full; then she tears it apart, and for half a minute no trap will hold her.")
                .text("The Knight of Hell: her herald marks a hunter with a red circle that follows them, stops, and burns, and her "
                        + "knights run at the spot; a sweep of hellfire before her; her retinue called back when it falls.")
                .text("Possession: her body turns to smoke and dives into one of her knights. Her smoke cannot be struck, but the "
                        + "knight's wounds are hers. Drive her out (the Exorcise sigil, Salt Shot, Banish by Name with her taglock or the "
                        + "knight's, the knight dragged onto a devil's trap, or the knight killed) and she is thrown back into her body, "
                        + "stunned for five seconds and taking twice the harm. Left alone she comes out by herself after sixteen.")
                .text("The Brand of the Knights: a telekinetic grip that holds whoever stays in its violet circle for two seconds, and "
                        + "the Brand itself, three red rings round her, each broken by two green gaps. Stand in a gap, or between the "
                        + "rings.")
                .items("What she leaves: the Book for everyone who fought her; the cleaver and her bust the first time",
                        AllItems.BOOK_OF_THE_DAMNED, AllItems.KNIGHTS_CLEAVER, AllItems.ABADDON_TROPHY));

        out.add(entry("abaddon_knight", JournalChapter.BOSSES).order(50).icon(AllItems.ABADDON_KNIGHT_SPAWN_EGG)
                .unlock(any(seen(AllEntities.ABADDON_KNIGHT.get()), adv("king_of_the_damned")))
                .creature(AllEntities.ABADDON_KNIGHT.get())
                .title("Her Knights")
                .entity(AllEntities.ABADDON_KNIGHT.get(), "One of Abaddon's knights")
                .text("Demons in blackened plate who rise out of hellfire round her, two at a time, three at most. A knight's slash "
                        + "sweeps a wide arc before him. Strike him from the front and he may raise his shield: while it is up, blows from "
                        + "the front glance off. Go round him.")
                .text("When Abaddon rides one he glows from inside, hits harder, moves faster and never raises his shield; his wounds "
                        + "are hers. When she falls, her knights crumble with her."));

        out.add(entry("abaddon_herald", JournalChapter.BOSSES).order(51).icon(AllItems.ABADDON_HERALD_SPAWN_EGG)
                .unlock(any(seen(AllEntities.ABADDON_HERALD.get()), adv("king_of_the_damned")))
                .creature(AllEntities.ABADDON_HERALD.get())
                .title("Her Herald")
                .entity(AllEntities.ABADDON_HERALD.get(), "Abaddon's herald, his bell raised")
                .text("A robed demon with a bell. He keeps his distance and rings it at a hunter: a red circle follows them for a "
                        + "second and a half, holds still, then burns, and the knights come running at the spot. Keep moving, and step "
                        + "out once it stops. Now and then he calls fire down at someone's feet.")
                .text("Only one at a time serves her. Kill him first."));

        out.add(entry("book_of_the_damned", JournalChapter.ARSENAL).order(27).icon(AllItems.BOOK_OF_THE_DAMNED)
                .unlock(any(has(AllItems.BOOK_OF_THE_DAMNED), adv("book_of_the_damned")))
                .title("The Book of the Damned")
                .text("Bound in skin, written in blood: the darkest spells ever set down. Abaddon carried it; everyone who fought her "
                        + "gets their own copy, straight into the pack (or kept for them if they were away). Lose it and win it again by "
                        + "fighting her again.")
                .text("Somewhere in it is the spell that undoes the Mark of Cain. Only a witch could read it, and I know one who would "
                        + "very much like to: see What to Do with the Book.")
                .items("The Book of the Damned", AllItems.BOOK_OF_THE_DAMNED));

        out.add(entry("knights_cleaver", JournalChapter.ARSENAL).order(28).icon(AllItems.KNIGHTS_CLEAVER)
                .unlock(any(has(AllItems.KNIGHTS_CLEAVER), adv("king_of_the_damned")))
                .title("The Knights' Cleaver")
                .text("Abaddon's own blade, left to whoever beats her the first time: a tier IV cursed cleaver. Use it and a wave of "
                        + "hellfire runs nine blocks down the ground before you, setting what it finds alight; then it needs six "
                        + "seconds. Without the Mark it hungers like the First Blade.")
                .items("The Knights' Cleaver", AllItems.KNIGHTS_CLEAVER));
    }
}
