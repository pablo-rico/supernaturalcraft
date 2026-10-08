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
 * v0.14: Gabriel, the Trickster. His pranks and how to bait him ({@code the_trickster}), the fight ({@code gabriel}), the
 * rules of TV Land channel by channel ({@code tv_land}), his doubles, and what he leaves: the remote, the blade, the candy.
 */
final class JournalGabriel {

    private JournalGabriel() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("the_trickster", JournalChapter.MONSTERS).order(21).icon(AllItems.CANDY_WRAPPER)
                .unlock(any(adv("trickster_sighted"), has(AllItems.CANDY_WRAPPER), seen(AllEntities.GABRIEL.get()), adv("changing_channels")))
                .title("Somebody's Laughing")
                .text("Ever since Lucifer went back in his box, little things have been happening. A candy wrapper by your feet when "
                        + "you haven't eaten any candy. A party hat on a cow. A laugh track somewhere out of sight. A villager quoting a "
                        + "soap opera. A chest that opens and shuts on its own, with nothing missing from it.")
                .text("None of it breaks anything, takes anything or hurts anyone, and it never happens more than once a day. That is "
                        + "how you know it is not a ghost: it's a Trickster, a pagan god of mischief who plays his jokes on the people "
                        + "who deserve them, and he has noticed you.")
                .text("Notice three of his pranks and you're on his trail. He can't resist sweets: cook a bait for him in a small "
                        + "circle with three sugar, a honey bottle, cocoa beans, holy oil and a gold nugget, lit with flint and steel.")
                .recipe(ritual("sweeten_the_pot"), "Sweetening the pot")
                .items("What he leaves lying around, and what he can't resist", AllItems.CANDY_WRAPPER, AllItems.TRICKSTER_BAIT));

        out.add(entry("gabriel", JournalChapter.BOSSES).order(16).icon(AllItems.TRICKSTER_REMOTE)
                .unlock(any(seen(AllEntities.GABRIEL.get()), adv("trickster_sighted"), adv("changing_channels")))
                .creature(AllEntities.GABRIEL.get())
                .title("The Trickster")
                .entity(AllEntities.GABRIEL.get(), "Gabriel, in his own jacket")
                .text("A pagan god of mischief, they say, who plays deadly pranks on the people who deserve them. He is not. He is "
                        + "Gabriel, the archangel who ran away from his brothers' war and hid in plain sight. Bait him by night with "
                        + "sweets inside a ring of holy oil, and he drags you into TV Land.")
                .recipe(ritual("summon_gabriel"), "Baiting the Trickster")
                .text("Lay two holy oils, a cake and two sugar in a small circle in the Overworld, by night, and wake it with the "
                        + "Trickster's Bait; the bait is eaten every time. He steps out of the fire laughing, and the world round the "
                        + "altar turns into a television studio. Nobody else's road needs him: he is a side show, for the fun of it.")
                .text("He fights through four channels, a quarter of his strength each, and changes channel when you take it: static, "
                        + "a new set built round you, a new costume. In the sitcom, the game show and the hospital he plays by the "
                        + "show's rules, and so must you. In the commercial, five of him stand at five podiums, and only one is real.")
                .text("He is not a fair fighter, but he plays fair by his own rules: learn each channel's and he is not as strong "
                        + "as he looks. (TV Land, in this book, tells them channel by channel.) Win, and he leaves his toys.")
                .items("What he leaves the first time", AllItems.TRICKSTER_REMOTE, AllItems.GABRIEL_BLADE, AllItems.GABRIEL_TROPHY,
                        AllItems.TRICKSTER_CANDY)
                .text("He can be baited again, as often as you like, one bait each time: then he leaves his trophy and some candy, "
                        + "and now and then his remote or his blade."));

        out.add(entry("tv_land", JournalChapter.BOSSES).order(17).icon(AllItems.TRICKSTER_BAIT)
                .unlock(any(seen(AllEntities.GABRIEL.get()), adv("changing_channels")))
                .title("Welcome to TV Land")
                .text("Watch the corner of your eye: when the static clears, the channel's number is there, and every channel has "
                        + "its own rule. The banner at the top is his health; the sign under it is the studio's.")
                .text("CH 2, the Sitcom. A living room, a staircase, doors, and an audience. While the LAUGH sign is lit he can't be "
                        + "touched and plays his gags: pies in the face that blind you, banana peels, a piano from the ceiling (watch "
                        + "for its shadow), extras through the doors. When the sign goes dark he takes more from every blow. Keep "
                        + "hitting him: if nobody touches him for a few seconds, the APPLAUSE sign lights and the audience heals him.")
                .text("CH 5, Nutcracker! A Japanese game show over a pit of foam: three platforms, red, blue and yellow. Every so "
                        + "often a question appears with three answers, one on each colour. Stand on the right platform before the "
                        + "buzzer: you come away stronger and he is stunned for a moment. Stand on a wrong one, or none, and the trap "
                        + "door or the giant mallet finds you. Fight him between the rounds. The questions are about everything in "
                        + "this book.")
                .text("CH 7, Dr. Sexy, M.D. A hospital ward with a giant heart monitor. Strike him on the beep (the trace on your "
                        + "screen spikes; the lit band in its middle is your moment) and the blow is critical. The beat quickens as "
                        + "the episode goes on. The defibrillator shocks marked zones of the floor; and nurses, doubles of him in "
                        + "caps, walk to him to heal him: one blow stops a nurse.")
                .text("CH 9, A Word from Our Sponsor. A white stage with five podiums and five spokesmen, all of him; only the real "
                        + "one casts a shadow of six golden wings. Strike a double and the joke is on you, and the podiums shuffle. "
                        + "They shuffle when you strike the real one too, and every ten seconds. Watch the floor, not the faces.")
                .text("A human's free will sees through his tricks a little: in the commercial, one more double flickers and fades "
                        + "for them. Demons are cast as the episode's villain and take more from him; an angel's grace drains in "
                        + "his commercial breaks, little brother."));

        out.add(entry("gabriel_double", JournalChapter.BOSSES).order(18).icon(AllItems.TRICKSTER_CANDY)
                .unlock(any(seen(AllEntities.GABRIEL_DOUBLE.get()), adv("changing_channels")))
                .creature(AllEntities.GABRIEL_DOUBLE.get())
                .title("The Trickster's Doubles")
                .entity(AllEntities.GABRIEL_DOUBLE.get(), "One of him. Or is it?")
                .text("Gabriel's oldest trick: more of himself. His doubles wear what he wears and grin the way he grins, but one "
                        + "blow and they are gone. In the sitcom they come through the doors as extras; in the hospital they are "
                        + "nurses in white caps, walking to him to patch him up; in the commercial they are the spokesmen at the "
                        + "podiums. A Reveal sigil dissolves one on the spot."));

        out.add(entry("trickster_remote", JournalChapter.ARSENAL).order(17).icon(AllItems.TRICKSTER_REMOTE)
                .unlock(any(has(AllItems.TRICKSTER_REMOTE), adv("changing_channels")))
                .title("The Trickster's Remote")
                .text("His remote control. Point it at a creature and change its channel: it might shrink for a while, turn up in a "
                        + "silly costume, or become another creature of its size altogether. It needs half a minute between uses, and "
                        + "it never works on a great enemy, a named creature or a villager with a trade.")
                .items("The remote", AllItems.TRICKSTER_REMOTE));

        out.add(entry("gabriel_blade", JournalChapter.ARSENAL).order(18).icon(AllItems.GABRIEL_BLADE)
                .unlock(any(has(AllItems.GABRIEL_BLADE), adv("changing_channels")))
                .title("Gabriel's Blade")
                .text("An archangel blade with a golden edge, and his sense of humour: now and then a strike calls up two fleeting "
                        + "doubles of you, who strike the same foe a little more before they wink out.")
                .items("The blade", AllItems.GABRIEL_BLADE));

        out.add(entry("trickster_candy", JournalChapter.ARSENAL).order(19).icon(AllItems.TRICKSTER_CANDY)
                .unlock(any(has(AllItems.TRICKSTER_CANDY), adv("changing_channels")))
                .title("Trickster Treats")
                .text("His candy, in a striped wrapper. Eat one and something happens for a little while: always something good, or "
                        + "at least nothing bad. You never know which. That is the point.")
                .items("A treat", AllItems.TRICKSTER_CANDY));
    }
}
