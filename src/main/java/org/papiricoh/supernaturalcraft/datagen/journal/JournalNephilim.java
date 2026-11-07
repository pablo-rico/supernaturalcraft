package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.entity.boss.michaelvessel.MichaelVesselBalance;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.lance.LanceBane;
import org.papiricoh.supernaturalcraft.nephilim.michael.MichaelPossession;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.41: Jack, the rift key, Asmodeus and the Lance, Michael's offer and vessel, soulless Jack, the Empty and the Shadow. Order
 * ranges: NEPHILIM 100-149; boss pages in BOSSES 103 (Asmodeus), 105-107 (vessel, Jack, Shadow). Every number is the code's.
 */
final class JournalNephilim {

    private JournalNephilim() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("jack", JournalChapter.NEPHILIM).order(100).icon(NephilimRegistry.NEPHILIM_BLOOD)
                .unlock(any(seen(NephilimRegistry.JACK.get()), adv("the_nephilim")))
                .creature(NephilimRegistry.JACK.get())
                .title("Jack Kline")
                .entity(NephilimRegistry.JACK.get(), "Jack, the nephilim")
                .text("Kelly Kline's son, and Lucifer's: a nephilim, born in a night and grown in a minute into a boy of eighteen who "
                        + "has never seen the world. He chose the hunters who were there when he was born. He walks with you from then "
                        + "on, strikes the monsters that come close with his grace (never a boss: that fight is yours), and raises a gold "
                        + "shield over you when you are hurt badly.")
                .text("Talk to him to tell him to stay or to follow, or for some of his blood: once a day, while he still has his "
                        + "grace, he fills a vial that tears the rift into the other world. If he falls he goes away to heal for two "
                        + "minutes and comes back. In the Apocalypse World he grows stronger; when he spends his grace driving Michael "
                        + "back, he is only a boy who can throw a punch."));
        out.add(entry("rift_key", JournalChapter.NEPHILIM).order(101).icon(NephilimRegistry.NEPHILIM_BLOOD)
                .unlock(any(adv("the_nephilim"), has(NephilimRegistry.NEPHILIM_BLOOD)))
                .title("The Rift Key")
                .text("The rift Jack tore at his birth closes after a while. To open it again you need what the hunters of season 13 "
                        + "used: an archangel's grace (Michael's or Lucifer's), and a nephilim's blood to wake the circle. Only in this "
                        + "world; it opens a few blocks behind the altar and stays open for ten minutes.")
                .recipe(ritual("open_apocalypse_rift"), "Opening the rift"));
        out.add(entry("lance_made_whole", JournalChapter.NEPHILIM).order(102).icon(NephilimRegistry.ARCHANGEL_BANE_SHARD)
                .unlock(any(has(NephilimRegistry.ARCHANGEL_BANE_SHARD), adv("bring_em_back_alive")))
                .title("The Lance Made Whole")
                .items("The shard and the lance", NephilimRegistry.ARCHANGEL_BANE_SHARD, AllItems.MICHAEL_LANCE)
                .text("Ramiel kept the Lance of Michael, the one spear that hurts an archangel; Asmodeus took a shard of it. Put the "
                        + "shard in a Hellforge's shard slot with Michael's Lance in the weapon slot and ascend: for "
                        + LanceBane.COST + " levels the lance is made whole, and its blows hurt any archangel "
                        + "×" + LanceBane.MULTIPLIER + " (the Michael of the other world among them)."));

        out.add(entry("asmodeus", JournalChapter.BOSSES).order(103).icon(NephilimRegistry.ASMODEUS_TROPHY_ITEM)
                .unlock(any(seen(NephilimRegistry.ASMODEUS.get()), adv("the_nephilim")))
                .creature(NephilimRegistry.ASMODEUS.get())
                .title("Asmodeus")
                .entity(NephilimRegistry.ASMODEUS.get(), "Asmodeus, Prince of Hell")
                .text("The last of Azazel's brothers, a Prince of Hell in a white suit who thinks himself its King while Crowley is "
                        + "gone. He keeps Gabriel in his cellar and drinks his grace from a vial; he wants the nephilim. Optional: call "
                        + "him by his rite once Jack is born, with Jack's blood to wake it.")
                .recipe(ritual("summon_asmodeus"), "Calling Asmodeus")
                .text("Three phases: his cane and his hellhounds, and a borrowed face to step behind you; then the stolen grace, "
                        + "gold lanes and gold circles under your feet; then three lanes at once and a gold nova round him. He leaves "
                        + "every fighter a shard of the Lance of Michael."));
        out.add(entry("michaels_offer", JournalChapter.NEPHILIM).order(110).icon(NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM)
                .unlock(any(seen(NephilimRegistry.MICHAEL_ENVOY.get()), adv("stranger_in_a_strange_land")))
                .creature(NephilimRegistry.MICHAEL_ENVOY.get())
                .title("Michael's Offer")
                .entity(NephilimRegistry.MICHAEL_ENVOY.get(), "Michael, come through the rift")
                .text("Driven out of his fortress, the Michael of the Apocalypse World came through the rift into this one. At dawn he "
                        + "comes to you with an offer: say yes, and together you will fix this world. Silence is no.")
                .text("Say yes, and for a minute he wears you: his strength, his speed, his light round you. Then he takes the wheel, "
                        + "and you wake inside your own head, in a bright room with an iron box he meant to keep you in, his projection "
                        + "between you and the way out (it hits a little softer there, ×" + MichaelVesselBalance.MINDSCAPE_DAMAGE + "). "
                        + "Throw him out and you are back where he took you. Say no, and he takes a vessel of his own and comes for you "
                        + "there and then. Either way, he comes back each dawn until his vessel falls."));
        out.add(entry("michael_vessel", JournalChapter.BOSSES).order(105).icon(NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM)
                .unlock(any(seen(NephilimRegistry.MICHAEL_VESSEL.get()), adv("stranger_in_a_strange_land")))
                .creature(NephilimRegistry.MICHAEL_VESSEL.get())
                .title("Michael, in a Vessel")
                .entity(NephilimRegistry.MICHAEL_VESSEL.get(), "Michael, in this world")
                .text("Three phases: a blade and a palm of blue light, and a snap of his fingers that brings a vampire or a werewolf "
                        + "he has made better (stronger, faster, harder to kill); then his wings flared in a blue ring, a choking hand "
                        + "that lifts you off the ground, a blink behind you; then three lanes of light and his monsters two at a time.")
                .recipe(ritual("summon_michael_vessel"), "Calling him back (rematch)"));
        out.add(entry("soulless_jack", JournalChapter.BOSSES).order(106).icon(NephilimRegistry.SOULLESS_JACK_TROPHY_ITEM)
                .unlock(any(seen(NephilimRegistry.SOULLESS_JACK.get()), adv("the_fall_of_the_son")))
                .creature(NephilimRegistry.SOULLESS_JACK.get())
                .title("Jack, Without His Soul")
                .entity(NephilimRegistry.SOULLESS_JACK.get(), "Jack, without his soul")
                .text("Graceless after Michael, Jack found a way to get his power back: it cost him his soul. The night after Michael's "
                        + "vessel falls he says goodbye; the night after that he comes back, eyes gold, and he isn't smiling.")
                .text("Three phases: gold blasts, a wave of his hand that throws you back, a snap of his fingers that unmakes what is "
                        + "under it (it withers); then three blasts at once and a gold nova; then the sky tears and he fights from the "
                        + "air. When he falls, black tar rises and takes him into the Empty, and he leaves every fighter a nephilim's "
                        + "grace.")
                .recipe(ritual("summon_soulless_jack"), "Calling him back (rematch)"));
        out.add(entry("nephilim_grace", JournalChapter.NEPHILIM).order(120).icon(NephilimRegistry.NEPHILIM_GRACE)
                .unlock(any(has(NephilimRegistry.NEPHILIM_GRACE), adv("moriah")))
                .title("A Nephilim's Grace")
                .text("What Jack leaves when the Empty takes him. Held up in its rite, it opens the way into the Empty (it is not "
                        + "spent there: keep one). Taken in, it widens your well for good: +" + Math.round(NephilimRules.GRACE_MANA)
                        + " max mana, once."));
        out.add(entry("the_empty", JournalChapter.NEPHILIM).order(121).icon(NephilimRegistry.EMPTY_FLOOR_ITEM)
                .unlock(any(adv("moriah"), adv("into_the_empty")))
                .title("The Empty")
                .text("Where angels and demons go when they die: a dark older than Heaven and Hell, a floor of black ground, and the "
                        + "dead asleep on it, wrapped in tar. Its keeper wants them all to sleep, forever. Open the way with a "
                        + "nephilim's grace held up in a void circle; every hunter beside you who has seen Jack fall goes in with you. "
                        + "Performed inside the Empty, the same rite is the way home.")
                .recipe(ritual("open_the_empty"), "Opening the Empty"));
        out.add(entry("shadow", JournalChapter.BOSSES).order(107).icon(NephilimRegistry.SHADOW_TROPHY_ITEM)
                .unlock(any(seen(NephilimRegistry.SHADOW.get()), adv("into_the_empty")))
                .creature(NephilimRegistry.SHADOW.get())
                .title("The Shadow")
                .entity(NephilimRegistry.SHADOW.get(), "The Shadow")
                .text("The Empty's keeper. Come into the Empty after Jack and it wakes a few seconds later, and it is very angry. Three "
                        + "phases, a tendril more each: a lash, a tendril that seizes and drags, the lull (a violet circle: whoever "
                        + "stands in it grows slow, weak and blind); then it sinks and rises behind you and the tar surges out of it; "
                        + "then three tendrils at once and the surge all round it. Beaten, it sinks back to sleep, leaves the page of "
                        + "the Empty's own sleep (Eternal Sleep) and sends you home.")
                .recipe(ritual("summon_shadow"), "Waking it again (rematch, in the Empty)"));
        out.add(entry("possession_note", JournalChapter.NEPHILIM).order(111).icon(NephilimRegistry.MICHAEL_VESSEL_TROPHY_ITEM)
                .unlock(adv("stranger_in_a_strange_land"))
                .title("Inside Your Own Head")
                .text("If you said yes, Michael wears you for " + MichaelPossession.WEAR_TICKS / 20 + " seconds before you wake in "
                        + "the mindscape. Nothing you do can stop it; make the most of his strength while it lasts. If another fight "
                        + "holds the world when the time comes, he keeps you a little longer."));
    }
}
