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
 * The Apocalypse (v0.40, the core): Ruby, the demon blood and its powers, the Panic Room, Alastair and his warehouse, Swan Song
 * and the Cage Shackle. Order ranges of the core: APOCALYPSE 50-99 (the Seals slice keeps 0-49). Every number here is the
 * code's ({@code BloodRules}, {@code BloodPowers}, {@code PanicRoomRules}, {@code AlastairBalance}, {@code RubyEntity}).
 */
final class JournalApocalypse {

    private JournalApocalypse() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("ruby", JournalChapter.APOCALYPSE).order(50).icon(AllItems.RUBYS_FLASK)
                .unlock(any(seen(AllEntities.RUBY_NPC.get()), seen(AllEntities.RUBY.get()), adv("i_know_what_you_did")))
                .creature(AllEntities.RUBY.get())
                .title("Ruby")
                .entity(AllEntities.RUBY_NPC.get(), "Ruby, the demon on your side")
                .text("A demon with a knife that kills demons, who comes at dawn once Azazel is dead and says she wants Lilith dead too. "
                        + "She offers her flask of demon blood, and either way she knows where the angels are holding Alastair: her "
                        + "compass points to the warehouse. Once he is dead she comes again, to tell you how to call Lilith.")
                .text("She speaks to angels and demons as well as hunters; she just picks her words. Send her away and she is back "
                        + "the next dawn.")
                .text("When Lilith falls, Ruby shows what she is: the last seal was the point all along. She fights fast and close, "
                        + "with her knife, a shove that throws you off her (a red ring round her) and a dash through smoke (a red lane). "
                        + "Nothing ends her but a blade that kills demons in the hand that strikes last: her own knife will do.")
                .entity(AllEntities.RUBY.get(), "Ruby, revealed")
                .text("Beaten, she leaves the Colt she mended and carried, one for every hunter who fought her. Lose her, and she will "
                        + "find you again some night.")
                .items("What she leaves", AllItems.THE_COLT, AllItems.RUBYS_KNIFE));

        out.add(entry("drinking_blood", JournalChapter.APOCALYPSE).order(52).icon(AllItems.DEMON_BLOOD)
                .unlock(any(adv("when_the_levee_breaks"), has(AllItems.RUBYS_FLASK)))
                .title("Demon Blood")
                .text("Drink a vial of demon blood (or a swig from Ruby's flask) and it runs through you for a few minutes. While it "
                        + "does, the power wheel opens for anyone, a hunter too, with the blood's own powers. They are paid in blood, "
                        + "not in grace or corruption, and the more doses you have ever drunk, the more of them you have.")
                .text("One dose: a psychic shove that throws everything in front of you back. Six: an exorcism by will alone, "
                        + "tearing a demon out of its host from afar (the common ones die). Fifteen: Sam's raised hand, crushing a "
                        + "demon from the inside, cruellest of all to Alastair, Lilith and Ruby.")
                .text("Every dose also feeds a need. Let the blood run dry while the need is on you and withdrawal comes: a hunter "
                        + "weakens and slows and sees things that are not there; a demon's corruption drains away; an angel's grace "
                        + "turns sour and refills at half the pace, and both of them burn harder in holy fire. Demons get hooked "
                        + "half as fast as hunters; angels twice as fast. The need fades only a little each day.")
                .items("The blood", AllItems.DEMON_BLOOD, AllItems.RUBYS_FLASK));

        out.add(entry("panic_room", JournalChapter.APOCALYPSE).order(54).icon(AllItems.PANIC_ROOM_DOOR)
                .unlock(any(adv("when_the_levee_breaks"), has(AllItems.PANIC_ROOM_DOOR), adv("the_panic_room")))
                .title("The Panic Room")
                .text("Bobby's cure. Build a room sealed in iron, walls, floor and ceiling, no wider than seven blocks on any side, "
                        + "with a devil's trap painted on its ceiling and a Panic Room door. Step inside with the blood still in "
                        + "your veins and use the door: it locks behind you.")
                .text("Ride it out for a whole day (sleeping counts). Then the door opens, and the blood and the need are gone; the "
                        + "powers you learnt stay learnt. Step out early and the cure is broken off.")
                .items("Bobby's door", AllItems.PANIC_ROOM_DOOR));

        out.add(entry("alastair", JournalChapter.BOSSES).order(23).icon(AllItems.ALASTAIR_SCALPEL)
                .unlock(any(seen(AllEntities.ALASTAIR.get()), adv("on_the_head_of_a_pin"), adv("i_know_what_you_did")))
                .creature(AllEntities.ALASTAIR.get())
                .title("Alastair")
                .entity(AllEntities.ALASTAIR.get(), "Alastair, Hell's torturer")
                .text("The demon who ran the racks in Hell, and who taught a righteous man to pick up the knife himself: the first "
                        + "seal. The angels have him chained in a devil's trap in a freight warehouse; Ruby's compass leads there. "
                        + "Walk up to the trap and the fight begins.")
                .text("Bound, he can barely be hurt by anything that is not holy: holy water, salt, an angel's blade. His chains still "
                        + "whip the floor round the trap (a red ring). At two thirds he tears free, the trap is scuffed out and his "
                        + "angel jailers burn.")
                .text("Free, he cuts (a red cone), throws three scalpels (three red lanes), casts his hook (a red lane: whoever it "
                        + "bites is dragged to him) and whips his chains round him. At a third the white smoke comes out of him "
                        + "(yellow), his demons climb out of the floor, and the rack rises under a hunter (a red circle): iron closes "
                        + "round whoever is caught.")
                .recipe(ritual("summon_alastair"), "Calling him back, by the trap")
                .items("What he leaves", AllItems.ALASTAIR_SCALPEL, AllItems.ALASTAIR_TROPHY));

        out.add(entry("swan_song", JournalChapter.APOCALYPSE).order(60).icon(AllItems.CAGE_SHACKLE)
                .unlock(any(adv("swan_song"), has(AllItems.CAGE_SHACKLE)))
                .title("Swan Song")
                .text("The end of season five. Beaten, Lucifer does not walk back into the Cage this time: the four rings, forged "
                        + "into the Key, open it under him and he falls in. The rings come home to you, and a shackle of the Cage "
                        + "lies on the ground.")
                .text("The first time it happens in a world, the Apocalypse is over: the omens fade to a faint echo. Keep the shackle. "
                        + "One day the Cage will have to be opened again, and nothing else will open it.")
                .items("What is left of the Cage", AllItems.CAGE_SHACKLE));
    }
}
