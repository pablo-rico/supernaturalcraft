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

/** v0.12: the Archangel Michael, the Host of Heaven, the Lance of Michael, his Grace and the General's armour. */
final class JournalMichael {

    private JournalMichael() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("michael", JournalChapter.BOSSES).order(19).icon(AllItems.MICHAEL_LANCE)
                .unlock(any(seen(AllEntities.MICHAEL.get()), adv("scribe_of_god"), adv("sword_of_heaven")))
                .creature(AllEntities.MICHAEL.get())
                .title("Michael")
                .entity(AllEntities.MICHAEL.get(), "Michael, the Sword of Heaven")
                .text("The eldest archangel, Heaven's general, and Lucifer's brother: the end of Heaven's road as Lucifer Uncaged is "
                        + "the end of Hell's. Once Metatron has fallen, call him down by day in a great circle in the Overworld with "
                        + "the Angel Tablet, the Seraph Wings, three choir shards and two holy waters, woken with an angel blade. Win "
                        + "or lose, the Tablet and the Wings fall back to earth.")
                .recipe(ritual("summon_michael"), "Calling down Michael")
                .text("He comes down into a Garden of Heaven wearing his vessel, a young man in a grey suit, and fights with an angel "
                        + "blade. When his palm starts to shine, never let it reach your brow: break his reach or turn it with a "
                        + "shield. He will ask you for your yes. Say it, and for a while he wears you: your body walks to your friends and "
                        + "burns them, and he heals; when he lets go, his grace stays with you and your blows land twice as hard on him. "
                        + "Say no, or say nothing, and the Host of Heaven hunts you.")
                .text("Then the Host of Heaven takes the field, in ranks behind its captain; strike the captain down and they break. "
                        + "In the War in Heaven his shadow spreads its wings: his lance pins whoever it strikes and stays in the ground "
                        + "until he calls it back (he is quicker without it, but his reach is short). Then he takes to the sky with "
                        + "storms of steel feathers and dives; when he lands to gather himself, strike.")
                .text("In the Throne Room his vessel burns away and his true form stands up: a general in white and gold, four "
                        + "blocks and more, six wings of steel and a halo of spears. When the halo breaks, his lance can be pulled "
                        + "from the ground and thrown back at him.")
                .items("What he leaves, for every hunter, every time", AllItems.MICHAEL_LANCE, AllItems.MICHAELS_GRACE, AllItems.MICHAEL_TROPHY)
                .items("And one piece of his armour you do not have yet", AllItems.GENERAL_HELMET, AllItems.GENERAL_CHESTPLATE,
                        AllItems.GENERAL_LEGGINGS, AllItems.GENERAL_BOOTS));

        out.add(entry("host_of_heaven", JournalChapter.ANGELS).order(5).icon(AllItems.ANGEL_BLADE)
                .unlock(any(seen(AllEntities.HOST_ANGEL.get()), adv("sword_of_heaven")))
                .creature(AllEntities.HOST_ANGEL.get())
                .title("The Host of Heaven")
                .entity(AllEntities.HOST_ANGEL.get(), "A soldier of the Host")
                .text("Angels in their vessels, in suits, trench coats and fatigues, with breastplates, angel blades and tower shields: "
                        + "Michael's soldiers. They march in ranks and obey their captain, the one with the plumed helm and its wings "
                        + "open. While it stands they hold together; when it falls they break."));

        out.add(entry("michaels_lance", JournalChapter.ARSENAL).order(15).icon(AllItems.MICHAEL_LANCE)
                .unlock(any(has(AllItems.MICHAEL_LANCE), adv("sword_of_heaven")))
                .title("The Lance of Michael")
                .text("The one weapon an archangel fears from another. Throw it and it comes back to your hand; it strikes angels and "
                        + "demons twice as hard and pins what it strikes for a moment.")
                .items("The lance", AllItems.MICHAEL_LANCE));

        out.add(entry("michaels_grace", JournalChapter.ANGELS).order(6).icon(AllItems.MICHAELS_GRACE)
                .unlock(any(has(AllItems.MICHAELS_GRACE), adv("sword_of_heaven")))
                .title("Michael's Grace")
                .text("Lucifer's brother's grace. Take it in, once, and it stays with you. Wear the Seraph Wings on your back (or in "
                        + "your chest slot) and they will carry you as freely as a bird, for as long as their strength lasts; run them "
                        + "dry in the air and they fold, and you drift down. Touch the ground to let them rest.")
                .items("The grace and the wings", AllItems.MICHAELS_GRACE, AllItems.SERAPH_WINGS));

        out.add(entry("general_armor", JournalChapter.ARSENAL).order(16).icon(AllItems.GENERAL_HELMET)
                .unlock(any(has(AllItems.GENERAL_HELMET), has(AllItems.GENERAL_CHESTPLATE), has(AllItems.GENERAL_LEGGINGS),
                        has(AllItems.GENERAL_BOOTS), adv("general")))
                .title("The General's Armour")
                .text("Michael's own armour, white and gold and written over in Enochian. He leaves one piece each time he falls, one "
                        + "you do not have yet. Worn whole, it wards off holy harm and raises a wing of light that takes a blow for you "
                        + "every thirty seconds.")
                .items("The four pieces", AllItems.GENERAL_HELMET, AllItems.GENERAL_CHESTPLATE, AllItems.GENERAL_LEGGINGS,
                        AllItems.GENERAL_BOOTS));
    }
}
