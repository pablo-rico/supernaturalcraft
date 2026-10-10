package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.18: Heaven. A hunter's own Heaven and the gate that leads there ({@code heaven}), its memory lane ({@code memories},
 * {@code kept_memories}), Ash's Roadhouse ({@code ash}), the home ({@code heaven_home}); Naomi and her room ({@code naomi},
 * {@code heaven_guard}, {@code training_copy}, {@code reprogramming_chair}); Zachariah and his office ({@code zachariah},
 * {@code clerk_angel}, {@code heavenly_forms}, {@code the_docket}); their spoils; and the wild crossroads ({@code wild_crossroads},
 * {@code wild_bargains}).
 */
final class JournalHeaven {

    private JournalHeaven() {
    }

    static void add(List<SNJournal.Entry> out) {
        places(out);
        naomi(out);
        zachariah(out);
        spoils(out);
        crossroads(out);
    }

    // --- Heaven itself ----------------------------------------------------------------------------------------------------

    private static void places(List<SNJournal.Entry> out) {
        out.add(entry("heaven", JournalChapter.PLACES).order(5).icon(AllItems.CLOUD_STONE)
                .unlock(any(adv("scribe_of_god"), adv("heavens_door")))
                .title("A Heaven of Your Own")
                .text("Heaven is not one place. Every soul that makes it up there gets a Heaven of its own: an island of its best days, "
                        + "in a light that never sets. Yours is waiting already. With Metatron fallen, nobody up there is keeping the "
                        + "door.")
                .text("Open a gate of light by day in a grace circle in the Overworld: a vial of Grace, two choir shards, two holy "
                        + "water, a golden apple and two feathers, woken with an angel blade (it is not used up). A frame of light "
                        + "stands over the altar for five minutes. Whoever crosses it lands in the Heaven of the hunter who opened it.")
                .text("The first time, your island has to be written: give it a moment. You land on its plaza, by a gate of your own "
                        + "that never closes and always takes you back where you came in. From the plaza a lane of shrines leads off "
                        + "(Memory Lane, in this book), and past them stand a white wing behind a golden seal, a house with its door "
                        + "sealed shut, and a door that opens on a road.")
                .items("What Heaven is made of", AllItems.CLOUD_STONE, AllItems.CLOUD_BRICKS)
                .text("Nothing hurts you here. Fall off the edge and you wake on the plaza, whole. Nothing can be broken or built "
                        + "either, not until your home is yours; then your own yard is yours to change."));

        out.add(entry("memories", JournalChapter.PLACES).order(6).icon(AllItems.CLOUD_BRICKS)
                .unlock(any(adv("heavens_door"), adv("memory_lane")))
                .title("Memory Lane")
                .text("Heaven remembers your road even if you don't. The fall of every great enemy, every deal at the crossroads, every "
                        + "case closed or lost, every rank you rose to, the pets you buried, the first time you saw each creature and "
                        + "the one you hunted most: all of it is kept, even what happened before you ever came up here.")
                .text("Twelve shrines line the lane, each holding one memory behind a shimmering veil. Step through a veil and the "
                        + "memory is staged before you, in the hollow at the end of the lane: the place as it was, the people as they "
                        + "stood. One of them glows. Touch it, and the memory is gathered: it gets its own page in the Memories tab of "
                        + "this book.")
                .text("Walk out through the veil at the stage's edge, or stay away long enough, and the stage clears for the next. "
                        + "Only one memory plays at a time. The shrines show what you have not gathered first; once you have them all, "
                        + "others take their turn.")
                .text("Gather three memories and the golden seal over the white wing opens. Somebody works in there."));

        out.add(entry("kept_memories", JournalChapter.PLACES).order(7).icon(AllItems.CLOUD_BRICKS)
                .unlock(adv("memory_lane"))
                .title("Kept Memories")
                .text("Memories that belong together are worth more together. Gather a whole set and it stays with you, wherever you go.")
                .text("Victories: five great enemies' falls make you a little harder to hurt by great enemies (+3% Aegis); every one "
                        + "of them, a heart more. The Crossroads: your deals' terms run a day longer. Cases: research runs a little "
                        + "faster at the desks. Kin: a deeper well of mana. Companions: your tamed pets close by heal over time. "
                        + "Sightings: ten first sightings, and you strike a little harder at every kind of creature you have seen "
                        + "(never the great enemies)."));

        out.add(entry("ash", JournalChapter.PLACES).order(8).icon(Items.JUKEBOX)
                .unlock(any(seen(AllEntities.ASH.get()), adv("met_ash")))
                .creature(AllEntities.ASH.get())
                .title("The Roadhouse")
                .entity(AllEntities.ASH.get(), "Ash, behind the bar")
                .text("The road door at the edge of your island leads to the heart of Heaven, where the souls' Heavens meet: Harvelle's "
                        + "Roadhouse, the way it was before it burned. Pool table, jukebox, the bar. Behind it, wiping a glass, is Ash: "
                        + "the mullet, the genius, the man who hacked Heaven's wiring and never left.")
                .text("Talk to him. He knows what is waiting for you next, more or less, and he keeps the list of Heavens that are "
                        + "open to visitors: hunters online who welcome guests, and the ones who trust you. Pick one and he sends you "
                        + "through. He can open or close your own Heaven to visitors too.")
                .text("The door at the back of the Roadhouse leads home to your own island."));

        out.add(entry("heaven_home", JournalChapter.PLACES).order(9).icon(AllItems.HEARTH)
                .unlock(any(adv("out_of_office"), adv("home_sweet_heaven")))
                .title("Home")
                .text("When Zachariah's file is closed, the seal on the house's door burns away. It is yours: a room for your trophies, "
                        + "a store room whose chests open for you and the hunters you trust, and the hearth. From now on the yard round "
                        + "the house is yours to build on.")
                .items("Home", AllItems.HEARTH)
                .text("Rest by the hearth and you are mended: whole health, a full belly, every harm and every trace of Hell's torment "
                        + "gone, and a minute of regeneration besides. The fire needs ten minutes before it can do it again.")
                .text("Coming home no longer needs the full rite. A smaller gate of light, opened at any hour once Zachariah has "
                        + "fallen, sets you down at your own front door."));
    }

    // --- Naomi and her room -------------------------------------------------------------------------------------------------

    private static void naomi(List<SNJournal.Entry> out) {
        out.add(entry("naomi", JournalChapter.BOSSES).order(40).icon(AllItems.NAOMIS_DRILL)
                .unlock(any(seen(AllEntities.NAOMI.get()), adv("heavens_door"), adv("deprogrammed")))
                .creature(AllEntities.NAOMI.get())
                .title("Naomi")
                .entity(AllEntities.NAOMI.get(), "Naomi, in her lab coat")
                .text("When an angel starts asking questions, Heaven sends it to Naomi. She sits it in a chair, puts a drill to its eye "
                        + "and keeps going until the questions stop; then she sends it back to work. A calm, severe woman in a grey suit "
                        + "and a white coat, she has run that room for longer than humans have had words.")
                .text("Her room is the white wing of your own Heaven. Its seal opens once you have gathered three memories; walk in and "
                        + "she is waiting, with the chairs, the console and the guards. There is no rite. Nobody else's road needs her: "
                        + "she is a challenge, not a duty.")
                .text("I. Intake. Her open palm comes down in a gold circle (raise a shield as it lands and she staggers); a gold ring "
                        + "holds whoever stands in it. A line runs from a chair to whoever stands most alone: if it catches you, you are "
                        + "strapped in (The Chair, in this book). She lunges with the drill in a cone and leaves you bleeding. A white "
                        + "ring spreads from her to wipe your mind: step into its green gaps, or see nothing for a while and move "
                        + "slowly after.")
                .text("She calls her guards, and while two of them stand she takes less from every blow. She stages a training test "
                        + "with copies of the people you love (The Training Test, in this book). When she is hurt she walks to the "
                        + "console and heals herself until somebody strikes it six times.")
                .text("II. Reprogramming. At half her strength the room changes round you and she stops being patient: the drill "
                        + "lunges three times in a row, and the chair holds harder.")
                .text("An angel of the second rank or more is the first one she straps in: you are one of hers, and you should sit. "
                        + "A demon feels the drill more.")
                .items("What she leaves the first time", AllItems.NAOMIS_DRILL, AllItems.NAOMIS_DIADEM, AllItems.NAOMI_TROPHY)
                .text("Once she has fallen, the room stays quiet when you walk in. Crouch as you cross into it to call her back.")
                .text("Met again, she leaves her bust, and now and then her drill or her diadem. Like every great enemy, she also "
                        + "leaves a shard for the Hellforge and, the first time, a heart of Vitality. And the lift at the back of her "
                        + "room starts working."));

        out.add(entry("reprogramming_chair", JournalChapter.BOSSES).order(41).icon(Items.LEATHER)
                .unlock(any(seen(AllEntities.NAOMI.get()), adv("deprogrammed")))
                .title("The Chair")
                .text("A white leather chair with straps at the wrists and ankles, and over it, on a jointed arm, the drill. When her "
                        + "line catches you, you are in it, and you cannot get up.")
                .text("Struggle: press jump as fast as you can. Eighteen good pulls in four seconds break the straps; in her second "
                        + "phase it takes twenty-four in three and a half. A human's will makes it easier; an angel was made to sit "
                        + "still, and it is harder. A friend can free you with three blows to the chair.")
                .text("Fail, and the drill comes down: a twentieth of your strength and more, through any armour, and for a few seconds "
                        + "you are Conditioned (your blows against angels fall soft). She heals a little, and lets you go."));

        out.add(entry("training_copy", JournalChapter.BOSSES).order(42).icon(Items.PLAYER_HEAD)
                .unlock(any(seen(AllEntities.TRAINING_COPY.get()), adv("deprogrammed")))
                .creature(AllEntities.TRAINING_COPY.get())
                .title("The Training Test")
                .entity(AllEntities.TRAINING_COPY.get(), "A copy, kneeling")
                .text("Naomi tests her work. Copies rise on the training floor: some kneel with their hands up, wearing the faces of "
                        + "people you would never hurt (Dean, Sam, Castiel, a pet you lost, you yourself); the others are hunters with "
                        + "demons' black eyes, and they come for you.")
                .text("Strike a kneeling copy and the test has worked: you are Conditioned for ten seconds (your blows against angels "
                        + "fall soft) and she heals. Kill every hostile copy within fifteen seconds without touching one that kneels, "
                        + "and the result is unexpected: she is stunned for three seconds and takes more from every blow."));

        out.add(entry("heaven_guard", JournalChapter.BOSSES).order(43).icon(AllItems.HEAVEN_GUARD_SPAWN_EGG)
                .unlock(any(seen(AllEntities.HEAVEN_GUARD.get()), adv("deprogrammed")))
                .creature(AllEntities.HEAVEN_GUARD.get())
                .title("Heaven's Guards")
                .entity(AllEntities.HEAVEN_GUARD.get(), "An angel guard")
                .text("Angels in dark suits and sunglasses, with a wire in one ear: Heaven's security. Naomi calls two or three at a "
                        + "time. While two of them stand she takes less from every blow, so cut them down first. They are only "
                        + "soldiers: a few good blows each."));
    }

    // --- Zachariah and his office ------------------------------------------------------------------------------------------

    private static void zachariah(List<SNJournal.Entry> out) {
        out.add(entry("zachariah", JournalChapter.BOSSES).order(44).icon(AllItems.ZACHARIAHS_BLADE)
                .unlock(any(seen(AllEntities.ZACHARIAH.get()), adv("deprogrammed"), adv("out_of_office")))
                .creature(AllEntities.ZACHARIAH.get())
                .title("Zachariah")
                .entity(AllEntities.ZACHARIAH.get(), "Zachariah, with his clipboard")
                .text("Heaven's middle management: a balding angel in a grey suit who believes in procedure and in nothing else. Every "
                        + "prophecy has a form, every form has a cabinet, and every hunter is a file he would like to close.")
                .text("Once Naomi has fallen, the lift at the back of her room goes up to his office: rows of desks and cubicles under "
                        + "flat white light, and they do not end. Walk far enough in any direction and you come back in from the other "
                        + "side. He does not need to walk: he blinks. Nobody else's road needs him.")
                .text("Everyone in his office does paperwork. He hands each of you a Heavenly Form, and while you hold one that is not "
                        + "filed, your blows are worth a quarter. File it (Heavenly Forms, in this book).")
                .text("I. Intake. A storm of memos in a cone. A rubber stamp slams a square of floor: DENIED, and your wounds stop "
                        + "closing for a while. Clerks come to help him. He strikes where you stand now, and again where you stood three "
                        + "seconds ago. He hands one of you a Termination Notice.")
                .text("II. Review. The cubicles shuffle round you and block your sight; he reassigns you across the office.")
                .text("III. It Was Already Written. His six wings open, and they buffet. From now on he tells you what comes next: "
                        + "read the docket (The Docket, in this book).")
                .text("IV. Final Judgment. The office comes apart, there is only a golden sky, and he smites Heaven itself in lines of "
                        + "gold that run out from him: find the ring of safe floor between them.")
                .items("What he leaves the first time", AllItems.ZACHARIAHS_BLADE, AllItems.HEAVENS_SEAL, AllItems.ZACHARIAH_TROPHY)
                .text("Once he has fallen, the lift carries you up to an empty office. Crouch as you ride it to call him back.")
                .text("Met again, he leaves his bust, and now and then his blade or his seal. Like every great enemy, he also leaves a "
                        + "shard and, the first time, a heart. His fall makes your house in Heaven your home."));

        out.add(entry("clerk_angel", JournalChapter.BOSSES).order(45).icon(AllItems.APPROVAL_STAMP)
                .unlock(any(seen(AllEntities.CLERK_ANGEL.get()), adv("out_of_office")))
                .creature(AllEntities.CLERK_ANGEL.get())
                .title("Angel Clerks")
                .entity(AllEntities.CLERK_ANGEL.get(), "An angel clerk")
                .text("Heaven's office staff, in shirtsleeves, vests and green visors. Zachariah calls three at a time. Each carries a "
                        + "rubber stamp: a clerk's stamp says DENIED just as his does, and your wounds stop closing for a while.")
                .text("Cut one down and it may drop its Approval Stamp. Use it on your form and the form is filed on the spot, wherever "
                        + "you stand.")
                .items("A clerk's stamp", AllItems.APPROVAL_STAMP));

        out.add(entry("heavenly_forms", JournalChapter.BOSSES).order(46).icon(AllItems.HEAVENLY_FORM)
                .unlock(any(seen(AllEntities.ZACHARIAH.get()), has(AllItems.HEAVENLY_FORM), adv("out_of_office")))
                .title("Heavenly Forms")
                .text("At the start of every phase, and every thirty seconds, he hands every hunter a Heavenly Form, numbered I to IV. "
                        + "While you hold one that is not filed, your blows against him are worth a quarter.")
                .items("The form, and what files it", AllItems.HEAVENLY_FORM, AllItems.APPROVAL_STAMP, AllItems.FILING_CABINET)
                .text("Four filing cabinets stand in his office, numbered I to IV. Use the form on the cabinet with its number and it is "
                        + "Approved: for ten seconds every blow you land on him counts half as much again. A clerk's Approval Stamp files "
                        + "any form at once.")
                .text("Let a form run overdue and he notices: a smite comes down on you for it."));

        out.add(entry("the_docket", JournalChapter.BOSSES).order(47).icon(Items.WRITABLE_BOOK)
                .unlock(any(seen(AllEntities.ZACHARIAH.get()), adv("out_of_office")))
                .title("The Docket")
                .text("Everything in Heaven was written before it happened, and from his third phase Zachariah stops pretending "
                        + "otherwise. The docket on your screen lists what he will do next, in order: three attacks, then five in his "
                        + "last phase. Each one's mark appears on the floor three seconds early. Read it and stand where it isn't.")
                .text("It is honest, almost. Once a phase he revises it: one line is struck through, and a moment later something else "
                        + "takes its place.")
                .text("The Termination Notice is the one to fear. The hunter it names is smitten five seconds later for two fifths of "
                        + "their strength, unless they stand at one of the desks that are safe from it, or share it: everyone within "
                        + "three blocks of them takes a part of the blow."));
    }

    // --- what they leave ----------------------------------------------------------------------------------------------------

    private static void spoils(List<SNJournal.Entry> out) {
        out.add(entry("naomis_drill", JournalChapter.ARSENAL).order(23).icon(AllItems.NAOMIS_DRILL)
                .unlock(any(has(AllItems.NAOMIS_DRILL), adv("deprogrammed")))
                .title("Naomi's Drill")
                .text("The drill she used on angels for a thousand years: a holy weapon of the fourth tier. Strike the same creature "
                        + "three times and its will is yours: for ten seconds it fights for you. It never works on a great enemy. "
                        + "Raise it at the Hellforge and it holds them longer.")
                .items("The drill", AllItems.NAOMIS_DRILL));

        out.add(entry("naomis_diadem", JournalChapter.ARSENAL).order(24).icon(AllItems.NAOMIS_DIADEM)
                .unlock(any(has(AllItems.NAOMIS_DIADEM), adv("deprogrammed")))
                .title("Naomi's Diadem")
                .text("A thin band of white gold she wore to keep her own head clear. Carry it in your other hand, or as a charm: nothing "
                        + "can condition you, mark you for Heaven or possess you, and great enemies hurt you a little less (+3% Aegis).")
                .items("The diadem", AllItems.NAOMIS_DIADEM));

        out.add(entry("zachariahs_blade", JournalChapter.ARSENAL).order(25).icon(AllItems.ZACHARIAHS_BLADE)
                .unlock(any(has(AllItems.ZACHARIAHS_BLADE), adv("out_of_office")))
                .title("Zachariah's Blade")
                .text("An ornate silver angel blade, a holy weapon of the fourth tier. Every third blow files its target: for three "
                        + "seconds it takes a quarter more from everything. Raise it at the Hellforge and the filing bites deeper.")
                .items("The blade", AllItems.ZACHARIAHS_BLADE));

        out.add(entry("heavens_seal", JournalChapter.ARSENAL).order(26).icon(AllItems.HEAVENS_SEAL)
                .unlock(any(has(AllItems.HEAVENS_SEAL), adv("out_of_office")))
                .title("Heaven's Seal")
                .text("Zachariah's own seal of office. Carry it in your other hand, or as a charm: the first blow a great enemy lands on "
                        + "you in any minute is halved.")
                .items("The seal", AllItems.HEAVENS_SEAL));
    }

    // --- the wild crossroads -----------------------------------------------------------------------------------------------

    private static void crossroads(List<SNJournal.Entry> out) {
        out.add(entry("wild_crossroads", JournalChapter.DEMONS).order(7).icon(AllItems.CROSSROADS_BOX)
                .unlock(any(adv("deal_with_the_devil"), has(AllItems.CROSSROADS_BOX), has(AllItems.CROSSROADS_SOIL), adv("wild_bargain")))
                .title("The Wild Crossroads")
                .text("The bowl is the quick way. The old way still works, out where two dirt roads meet in open country, far from any "
                        + "town: a weathered marker, a dead tree, and in the very middle a patch of loose, dark soil that nothing grows "
                        + "on.")
                .items("The box, and where it goes", AllItems.CROSSROADS_BOX, AllItems.CROSSROADS_SOIL)
                .text("Fill a crossroads box: a bone, grave soil, paper, sulfur and an iron nugget. Bury it in that soil at night and stay "
                        + "close: a few moments later the demon comes up the road, and no bowl or contract is asked of you. A crossroads "
                        + "takes one box from you a night; walk off before it comes and the box is wasted.")
                .text("A wild demon offers more than the one in the bowl, and charges more for it (Wild Bargains, in this book)."));

        out.add(entry("wild_bargains", JournalChapter.DEMONS).order(8).icon(AllItems.CROSSROADS_CONTRACT)
                .unlock(any(adv("wild_bargain"), has(AllItems.CROSSROADS_BOX)))
                .title("Wild Bargains")
                .text("Better wishes, worse prices. Besides everything the bowl's demon offers, a wild one can raise the weapon in your "
                        + "hand one tier of Ascension (never past the fourth, nor past what you have earned); give you the trophies of up "
                        + "to three great enemies you have already beaten, with a shard for each; bring back the last pet you lost, "
                        + "collar or not; or lift a curse: a hungry weapon's, Heaven's Mark, the hollowness of a soul taken, or a jinx.")
                .text("The price: half the usual days before the hounds come, the soul clause already ticked, and a bigger pack (five "
                        + "to seven) that you must outlast for three minutes, not two. Sealing one is a Wild Bargain."));
    }
}
