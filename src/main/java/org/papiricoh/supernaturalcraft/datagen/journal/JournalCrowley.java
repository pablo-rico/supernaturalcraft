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
 * Crowley, King of the Crossroads (v0.35): the asylum and its case, his fight, his people, his fate (executed or recruited), the
 * companion and the Contract Book. Every number here is the code's ({@code CrowleyBalance}, {@code CrowleySpoils}).
 */
final class JournalCrowley {

    private JournalCrowley() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("crowley_asylum", JournalChapter.PLACES).order(40).icon(AllItems.CONTRACT_BOOK)
                .unlock(any(seen(AllEntities.CROWLEY_SUIT.get()), seen(AllEntities.POSSESSED_PATIENT.get()), adv("king_of_the_crossroads")))
                .title("The Asylum")
                .text("Crowley never sat in the Pit's chain. While the Pit fought over thrones, the King of the Crossroads ran his own "
                        + "business from an abandoned asylum: red brick, a portico of columns, a balcony nobody stands on and stairs "
                        + "too steep for the old patients. The woods have nearly swallowed it.")
                .text("No map shows it. Once Dick Roman has fallen, a Man of Letters' next case is the asylum's: the file and the map "
                        + "lead there. The first hunter near it finds the hall full of his suits and the cells full of patients who are "
                        + "no longer alone in their heads.")
                .text("Step through the front door with the case in hand and the bars come down behind you."));

        out.add(entry("crowley", JournalChapter.BOSSES).order(56).icon(AllItems.CONTRACT_BOOK)
                .unlock(any(seen(AllEntities.CROWLEY.get()), adv("king_of_the_crossroads")))
                .creature(AllEntities.CROWLEY.get())
                .title("Crowley")
                .entity(AllEntities.CROWLEY.get(), "Crowley, King of the Crossroads")
                .text("A small man in a good black coat who would rather talk than fight, and who wins most fights by not being there. "
                        + "He takes only half of every blow while two of his people stand near him: thin them out first.")
                .text("The Hall: he snaps his fingers and is behind you, a violet line showing the shove that follows; he waves a "
                        + "red cone of contracts at you; a yellow cloud of red smoke blinds whoever stays in it; suits come through "
                        + "the doors; hounds come through red rings.")
                .text("The Cell Wing: he goes in smoke and the bars to the cells fall. The patients come in waves, and the hounds "
                        + "more often.")
                .text("The Throne Room: he waits on his throne, with crossroads demons at his side, and only stands once half of what "
                        + "is left of him is gone. Red lines of hellfire cross the floor. If you owe the crossroads anything, he "
                        + "comes to collect: a red ring on you, and the hounds go for you alone.")
                .text("Beaten, he kneels and bargains. The hunter whose case brought them decides: end him, or take his offer. Say "
                        + "nothing and he dies. There is only one Crowley, and only one choice.")
                .recipe(ritual("summon_crowley"), "Dragging him back (a rematch, in his throne room)")
                .items("What he leaves when he dies: his Contract Book", AllItems.CONTRACT_BOOK));

        out.add(entry("crowley_companion", JournalChapter.BOSSES).order(57).icon(AllItems.CONTRACT_BOOK)
                .unlock(adv("king_of_the_crossroads"))
                .creature(AllEntities.CROWLEY_COMPANION.get())
                .title("Crowley, at Your Side")
                .entity(AllEntities.CROWLEY_COMPANION.get(), "Crowley, on retainer")
                .text("Kept alive, he keeps his word, more or less. He follows the hunter who spared him everywhere but Heaven (he "
                        + "waits on Earth), fights beside them with a snap of his fingers and a pair of bound hounds, and never stops "
                        + "talking.")
                .text("Talk to him: what he knows (the next great enemy, a demon of the Pit and where it was last seen); a deal a day "
                        + "with no soul on the table (luck, the hounds shown, supplies for emeralds, or once a week a crossroads debt "
                        + "forgiven); for a demon, a word in the Pit that sets two of its officers at each other's throats.")
                .text("Tell him to stay and he sits where you leave him, on a step or a slab if there is one: your house, for "
                        + "instance. Beaten down, he goes in smoke and comes back ten minutes later."));

        out.add(entry("crowley_suit", JournalChapter.DEMONS).order(60).icon(AllItems.CROWLEY_SUIT_SPAWN_EGG)
                .unlock(any(seen(AllEntities.CROWLEY_SUIT.get()), adv("king_of_the_crossroads")))
                .creature(AllEntities.CROWLEY_SUIT.get())
                .title("Crowley's Suits")
                .entity(AllEntities.CROWLEY_SUIT.get(), "One of Crowley's suits, folder in hand")
                .text("Black-eyed demons in black suits, Crowley's civil service. They fight with their fists, never wear a rank in "
                        + "the Pit's chain, and fight the Pit's demons on sight."));

        out.add(entry("possessed_patient", JournalChapter.DEMONS).order(61).icon(AllItems.POSSESSED_PATIENT_SPAWN_EGG)
                .unlock(any(seen(AllEntities.POSSESSED_PATIENT.get()), adv("king_of_the_crossroads")))
                .creature(AllEntities.POSSESSED_PATIENT.get())
                .title("Possessed Patients")
                .entity(AllEntities.POSSESSED_PATIENT.get(), "A patient of the asylum, not alone in there")
                .text("The asylum's old inmates, ridden by the lowest demons Crowley keeps. Slow and many, until one of them lunges."));

        out.add(entry("contract_book", JournalChapter.ARSENAL).order(41).icon(AllItems.CONTRACT_BOOK)
                .unlock(any(has(AllItems.CONTRACT_BOOK), adv("king_of_the_crossroads")))
                .title("The Contract Book")
                .text("Crowley's ledger, taken from him when he is put down. Hold it to a creature and it signs: it fights for you for "
                        + "five minutes, and never turns on you while the ink is wet. It costs a level of experience for every ten "
                        + "points of the creature's health (two at least), and the book rests two minutes after a contract runs out.")
                .text("It will not bind bosses, players, anything the Colt cannot kill, or anything too big for ink.")
                .items("The Contract Book", AllItems.CONTRACT_BOOK));
    }
}
