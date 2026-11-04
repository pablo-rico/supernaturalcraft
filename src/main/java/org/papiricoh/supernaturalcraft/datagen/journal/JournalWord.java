package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * The Word and the Ward (v0.36): the wall sigils, the trap on the ceiling and under the rug, the oil left unlit, the
 * interrogation, the Enochian crypt, the tablets and their prophets, the sigils of the Word and the Trials. Every number is
 * the code's ({@code WardGeometry}, {@code BanishingSigilBlock}, {@code AlarmSigilBlock}, {@code DevilsTrapBlock},
 * {@code InterrogationRules}, {@code WordCipher}, {@code TabletReading}, {@code TrialRules}).
 */
final class JournalWord {

    private JournalWord() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("wall_sigils", JournalChapter.BASICS).order(7).icon(AllItems.BANISHING_SIGIL)
                .unlock(any(has(AllItems.BANISHING_SIGIL), has(AllItems.ENOCHIAN_WARD), has(AllItems.ALARM_SIGIL), has(AllItems.ENOCHIAN_INK)))
                .title("Sigils on the Walls")
                .text("Some sigils are not cast: they are painted, on a wall, a floor or a ceiling, and they work for as long as the paint "
                        + "lasts. Enochian ink is the base of all three. Running water washes them off, and any tool scrapes them away.")
                .text("The banishing sigil is drawn in your own blood: painting one costs two hearts, and you cannot do it with three or "
                        + "less left. Slam your palm on it and every angel within sixteen blocks is sent away. Every one: friends, "
                        + "the angel who stands beside you, you yourself if that is what you are. Never an archangel or anything that "
                        + "size. The blood is spent in the blast.")
                .text("The Enochian ward keeps angels out of the eight blocks around it. They are thrown back from the edge, they cannot "
                        + "blink inside, and the ones outside forget whoever shelters in it. An angel who walks in on two feet finds "
                        + "their grace answers nothing there. Paint several and they add up.")
                .text("The alarm sigil watches eight blocks. When a demon, an angel or one of this world's monsters comes near it glows, "
                        + "chimes, gives off a redstone signal (through the wall it is painted on too), marks the intruder with a glow "
                        + "and tells whoever painted it, wherever they are. It never rings for you.")
                .items("Banishing sigil, Enochian ward, alarm sigil", AllItems.BANISHING_SIGIL, AllItems.ENOCHIAN_WARD, AllItems.ALARM_SIGIL));

        out.add(entry("hidden_traps", JournalChapter.BASICS).order(8).icon(AllItems.DEVILS_TRAP)
                .unlock(any(has(AllItems.DEVILS_TRAP), adv("caught_in_the_trap")))
                .title("Traps They Don't See")
                .text("A devil's trap works on a ceiling too. Paint it on the underside of a block and it holds any demon standing in the "
                        + "3x3 below it, all the way down to the floor (eight blocks at most). Demon princes pay it no mind.")
                .text("On a floor, a rug hides it: use any carpet on a trap and the whole seal is covered. It still holds. Break it and "
                        + "you only lift the rug; the paint is still there.")
                .text("Holy oil poured while crouching is left unlit, a ring on the ground waiting. Flint and steel, a fire charge, a "
                        + "burning arrow, a redstone signal or fire beside it, and the whole ring catches at once. An alarm sigil "
                        + "beside the oil lights it for you.")
                .items("Devil's trap, holy oil, a carpet", AllItems.DEVILS_TRAP, AllItems.HOLY_OIL, net.minecraft.world.item.Items.RED_CARPET));

        out.add(entry("interrogation", JournalChapter.DEMONS).order(30).icon(AllItems.HOLY_WATER)
                .unlock(any(adv("first_confession"), adv("caught_in_the_trap")))
                .title("Interrogation")
                .text("A demon or an angel held in a trap can be made to talk. Use what is in your hand on it: holy water on a demon, "
                        + "holy oil on an angel, a book read aloud, a blade, or only words. Its resistance shows above your hotbar.")
                .text("Each one has a weak point, and flinches differently when you find it. The same thing pressed over and over "
                        + "works less each time. The harsh ones hurt it: push too far and it dies, or flees its vessel, before it "
                        + "says a word.")
                .text("Broken, it offers what it knows: the way to the nearest Enochian crypt, drawn on a map; what it has heard about "
                        + "the princes; an angel, a few letters of the Word. Then it is gone."));

        out.add(entry("enochian_crypt", JournalChapter.PLACES).order(41).icon(AllItems.CHISELED_ENOCHIAN_STONE)
                .unlock(any(adv("first_confession"), adv("the_crypt")))
                .title("The Enochian Crypt")
                .text("Buried under the old shrines of the angels, the crypts were built to keep one thing: the Demon Tablet. Their "
                        + "corridors still have their traps, painted on the ceilings, and unlit oil on the floors.")
                .text("Three sealed doors stand in the way. Beside each, a stone shows the outline of a sigil: paint that sigil on it "
                        + "and the door crumbles. The library beyond is covered in murals of the Word; study them and they give away "
                        + "letters of the Demon Tablet.")
                .text("At its heart a pedestal keeps a tablet for every hunter who reaches it."));

        out.add(entry("tablets", JournalChapter.SIGILS).order(41).icon(AllItems.DEMON_TABLET)
                .unlock(any(has(AllItems.DEMON_TABLET), has(AllItems.ANGEL_TABLET), has(AllItems.LEVIATHAN_TABLET)))
                .title("The Word of God")
                .text("Three tablets: the Angel Tablet, taken from Metatron; the Leviathan Tablet, from Dick Roman; the Demon Tablet, "
                        + "from the crypt. Use one to read it (the Angel Tablet crouching). Each has three chapters, read one at a time.")
                .text("A chapter is a Latin sentence in the glyphs of the Word, one glyph for each letter, and the key is your own. "
                        + "You see the letters you know; work out the rest, then read. A wrong reading hurts, and says how many glyphs "
                        + "you got wrong. A right one teaches you every letter in it, so each chapter makes the next one easier.")
                .text("Letters come from the crypt's murals, from angels who confess, from a prophet's dreams. A tablet read to its "
                        + "end adds twenty-five to your mana.")
                .items("The tablets", AllItems.ANGEL_TABLET, AllItems.DEMON_TABLET, AllItems.LEVIATHAN_TABLET));

        out.add(entry("prophets", JournalChapter.SIGILS).order(42).icon(AllItems.ENOCHIAN_INK)
                .unlock(any(adv("prophet")))
                .title("Prophets")
                .text("A tablet chooses the first one who touches it, and that one is a prophet for good. A prophet is shown more letters "
                        + "when a chapter opens, and one more after each wrong reading; the Word hurts them less.")
                .text("A prophet who sleeps the night through dreams: letters of a tablet, or a glimpse of what is coming. Write them down."));

        out.add(entry("word_sigils", JournalChapter.SIGILS).order(43).icon(AllItems.DEMON_TABLET)
                .unlock(any(adv("word_of_god"), adv("prophet")))
                .title("Sigils of the Word")
                .text("No page holds them and no mastery opens them: only the tablets. The Angel Tablet teaches the Fall (an angel "
                        + "struck out of the sky), makes every ward you paint supreme (twice the reach, and it burns what it throws "
                        + "out) and ends with the Mass Expulsion (every angel near what you strike, sent away).")
                .text("The Demon Tablet teaches the Mass Exorcism, then how to cut a devil's trap into stone (no prince scuffs it out), "
                        + "and ends with the Trials. The Leviathan Tablet teaches Purgatory's Bind (held and dragged to you) and Seal in "
                        + "a Jar: an empty Purgatory jar in your pack and a monster goes into it, to be let out wherever you open it.")
                .items("Engraved devil's trap, Purgatory jar", AllItems.ENGRAVED_DEVILS_TRAP, AllItems.PURGATORY_JAR));

        out.add(entry("the_trials", JournalChapter.SIGILS).order(44).icon(AllItems.PURIFIED_BLOOD)
                .unlock(any(adv("word_of_god"), adv("gates_sealed")))
                .title("The Trials")
                .text("The Demon Tablet's last chapter tells how to shut the gates of Hell. Three trials, in order: kill a hellhound "
                        + "and bathe in its blood; bring an innocent soul up out of Hell (a demon you kill down there gives one up); "
                        + "cure a demon, eight injections of purified blood while it is held, an hour apart.")
                .text("They take from you. Each trial begun costs two hearts, and you are weak while one is under way. Before the last "
                        + "injection you choose. Stop, and what was taken comes back, two hearts a day; you can begin again. Finish, "
                        + "and the demon is a man again, every demon within sixty-four blocks is sent below, none ever rises there "
                        + "again, and two hearts stay with the gates for good.")
                .items("Hellhound blood, an innocent soul, purified blood", AllItems.HELLHOUND_BLOOD, AllItems.INNOCENT_SOUL, AllItems.PURIFIED_BLOOD));
    }
}
