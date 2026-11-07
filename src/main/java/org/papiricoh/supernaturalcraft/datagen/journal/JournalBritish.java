package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.41: season 12: the British Men of Letters, Ketch, Hess, their arsenal, Dagon, Kelly and the birth. Order ranges: NEPHILIM
 * 0-49; boss pages in BOSSES 100-102 (Ketch 100, Hess 101, Dagon 102). Every number here is the code's ({@code BritishRules},
 * {@code ArsenalRules}, {@code KetchBalance}, {@code HessBalance}, {@code DagonBalance}).
 */
final class JournalBritish {

    private JournalBritish() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("british_men_of_letters", JournalChapter.NEPHILIM).order(0).icon(BritishRegistry.BRITISH_DOSSIER)
                .unlock(any(seen(BritishRegistry.HESS_ENVOY.get()), adv("keep_calm_and_carry_on")))
                .creature(BritishRegistry.HESS_ENVOY.get())
                .title("The British Men of Letters")
                .entity(BritishRegistry.HESS_ENVOY.get(), "Dr. Mildred Hess, at dawn")
                .text("The London chapter of the Men of Letters never fell. While the Americans were wiped out, the British kept their "
                        + "code, their funding and their discipline, and they have watched the Darkness nearly swallow the world on the "
                        + "American hunters' watch. Now they are crossing the ocean to take over.")
                .text("Once Amara is beaten, their head, Dr. Hess, comes to you in person at dawn with an ultimatum: work with them, "
                        + "by their rules, or step aside. Say nothing and she takes it as a no. If you belong to the American Men of "
                        + "Letters, she has her eye on your bunker as well.")
                .text("Join them and they hand you their kit: a suppressed pistol, a dozen enchanted rounds and three hex grenades. Do not "
                        + "get comfortable. Three days later London decides you are a liability, and you are written off like anyone who "
                        + "refused.")
                .text("Either way, she lets something slip: a nephilim, Lucifer's child, is about to be born. That is what the British "
                        + "are really here for."));

        out.add(entry("british_arsenal", JournalChapter.NEPHILIM).order(4).icon(BritishRegistry.BRITISH_PISTOL)
                .unlock(any(has(BritishRegistry.BRITISH_PISTOL.get()), has(BritishRegistry.HEX_GRENADE.get()), adv("the_british_invasion")))
                .title("The British Arsenal")
                .text("The suppressed pistol fires enchanted rounds from your inventory, along your line of sight, once a second: each one "
                        + "hurts and carries a hex that slows and weakens what it hits for a few seconds. A boss only takes the round itself.")
                .text("A hex grenade is thrown like a splash potion and bursts into a hex: whatever stands in it is hurt a little, slowed, "
                        + "weakened and blinded. Bosses shrug off everything but a little of the damage.")
                .text("Rounds are made from a copper ingot, an iron nugget, salt and gunpowder (four at a time); grenades from an iron "
                        + "ingot, gunpowder, an ink sac and salt (two at a time).")
                .items("The kit", BritishRegistry.BRITISH_PISTOL, BritishRegistry.ENCHANTED_ROUND, BritishRegistry.HEX_GRENADE));

        out.add(entry("british_agent", JournalChapter.NEPHILIM).order(6).icon(BritishRegistry.ENCHANTED_ROUND)
                .unlock(any(seen(BritishRegistry.BRITISH_AGENT.get()), adv("who_we_are")))
                .creature(BritishRegistry.BRITISH_AGENT.get())
                .title("British Field Agents")
                .entity(BritishRegistry.BRITISH_AGENT.get(), "A British field agent")
                .text("Hess's men: black vests, white shirts and short carbines. They keep their distance and fire short bursts of "
                        + "enchanted rounds (every hit carries the round's hex), and club whoever closes in with the butt of the carbine. "
                        + "They come in at the compound's doors when Hess calls for them."));

        out.add(entry("british_compound", JournalChapter.NEPHILIM).order(8).icon(BritishRegistry.HESS_TROPHY_ITEM)
                .unlock(any(has(BritishRegistry.BRITISH_DOSSIER.get()), adv("the_british_invasion"), adv("who_we_are")))
                .title("The British Compound")
                .text("An old English manor somewhere in this world, turned into the British Men of Letters' headquarters: stone, iron "
                        + "railings, a gravel drive and, in the middle, a war room with a long table. It does not exist until someone reads "
                        + "Ketch's dossier; then it is raised, and the dossier gives you a compass to it.")
                .text("Walk into the war room once you have had Hess's ultimatum, and she will be waiting for you there.")
                .items("Ketch's dossier", BritishRegistry.BRITISH_DOSSIER));

        out.add(entry("trace_the_nephilim", JournalChapter.NEPHILIM).order(10).icon(BritishRegistry.ENCHANTED_ROUND)
                .unlock(adv("keep_calm_and_carry_on"))
                .title("The Nephilim's Trace")
                .text("Hess told you more than she meant to: a nephilim is coming. Once you have had her ultimatum, this rite finds its "
                        + "mother. Draw the tracing circle (chalk lines round the altar, a lit white candle at each corner of the inner "
                        + "square) and work it with a compass.")
                .text("The compass you get back points to Kelly Kline's safe house, wherever it is in this world.")
                .recipe(ritual("trace_the_nephilim"), "The trace"));

        out.add(entry("kelly_safehouse", JournalChapter.NEPHILIM).order(12).icon(BritishRegistry.DAGON_TROPHY_ITEM)
                .unlock(any(adv("trace_the_nephilim"), adv("all_along_the_watchtower")))
                .creature(BritishRegistry.KELLY_KLINE.get())
                .title("Kelly Kline's Safe House")
                .entity(BritishRegistry.KELLY_KLINE.get(), "Kelly Kline")
                .text("A log cabin far out in the wild, with a clearing in front of its porch and a nursery corner inside. Kelly Kline, "
                        + "who is carrying Lucifer's child, is hiding there, and the child is close.")
                .text("She is not alone. A Prince of Hell stands guard before the door, and the moment a hunter on the nephilim's trail "
                        + "walks into the clearing, the fight begins."));

        out.add(entry("jack_birth", JournalChapter.NEPHILIM).order(14).icon(BritishRegistry.DAGONS_BRAND)
                .unlock(adv("the_nephilim"))
                .creature(BritishRegistry.ROURKE_LUCIFER.get())
                .title("The Birth")
                .entity(BritishRegistry.ROURKE_LUCIFER.get(), "Lucifer, in the President's body")
                .text("When Dagon falls, Kelly's time comes. Lucifer walks in, wearing the President of the United States, to meet his "
                        + "son. Kelly names the child Jack, and his light pours out of her and fills the cabin.")
                .text("Lucifer reaches for the boy, and the light throws him back through a rift torn open behind him: he is gone into "
                        + "another world. Kelly fades. Jack is born, and he goes with you; the rift stays open a while longer."));

        // --- the bosses -------------------------------------------------------------------------------------------------------

        out.add(entry("ketch", JournalChapter.BOSSES).order(100).icon(BritishRegistry.KETCH_TROPHY_ITEM)
                .unlock(any(seen(BritishRegistry.KETCH.get()), adv("the_british_invasion")))
                .creature(BritishRegistry.KETCH.get())
                .title("Arthur Ketch")
                .entity(BritishRegistry.KETCH.get(), "Arthur Ketch, the British Men of Letters' operative")
                .text("Turn Hess down (or join her and be written off) and Ketch is sent after you. He comes at night, wherever you are, "
                        + "and makes a small arena round you that leaves anyone else's fight alone. Lose, and he will be back another night.")
                .text("He shoots: a single round (a red lane) or a volley of three (three red lanes), and cuts whoever comes close (a red "
                        + "cone). At two thirds he lunges with the knife (a red lane he crosses himself) and throws hex grenades (a violet "
                        + "circle: slowed and weakened). At a third he drops smoke (yellow: blinded) and steps out of it behind you.")
                .text("Beaten, he leaves every hunter who fought him the British arsenal, his bust and his dossier on the British compound.")
                .recipe(ritual("summon_ketch"), "Calling him back, by night")
                .items("What he leaves", BritishRegistry.BRITISH_PISTOL, BritishRegistry.BRITISH_DOSSIER, BritishRegistry.KETCH_TROPHY_ITEM));

        out.add(entry("hess", JournalChapter.BOSSES).order(101).icon(BritishRegistry.HESS_FOLIO)
                .unlock(any(seen(BritishRegistry.HESS.get()), adv("who_we_are")))
                .creature(BritishRegistry.HESS.get())
                .title("Dr. Mildred Hess")
                .entity(BritishRegistry.HESS.get(), "Dr. Hess, head of the British Men of Letters")
                .text("She waits in the war room of the British compound, with two of her agents. Her wand fires bolts (a violet lane); "
                        + "she tosses hex bags (a violet circle: slowed and weakened) and calls more agents in at the doors (red rings).")
                .text("At two thirds she raises wards: a violet circle round her throws out whoever stands in it, and while the ward "
                        + "holds she takes less than half of every blow. Wait it out or keep hitting. At a third she reads from her folio: "
                        + "a curse over the whole room (a wide violet ring), and only a small green circle is spared. Get into it.")
                .text("Beaten, she leaves every hunter her bust, her folio and the Order's ammunition. One of the American Men of Letters "
                        + "also carries off London's files, straight into the Archive.")
                .recipe(ritual("summon_hess"), "Calling her back, at the compound")
                .items("What she leaves", BritishRegistry.HESS_FOLIO, BritishRegistry.HESS_TROPHY_ITEM));

        out.add(entry("hess_folio", JournalChapter.NEPHILIM).order(16).icon(BritishRegistry.HESS_FOLIO)
                .unlock(any(has(BritishRegistry.HESS_FOLIO.get()), adv("who_we_are")))
                .title("Hess's Folio")
                .text("One ward of the Old Ways, read aloud once. The reader is warded for half a minute (resistance and a shield of "
                        + "absorption), and every foe within eight blocks is slowed and weakened. Bosses ignore the hex. The folio is "
                        + "spent with the reading.")
                .items("The folio", BritishRegistry.HESS_FOLIO));

        out.add(entry("dagon", JournalChapter.BOSSES).order(102).icon(BritishRegistry.DAGON_TROPHY_ITEM)
                .unlock(any(seen(BritishRegistry.DAGON.get()), adv("all_along_the_watchtower")))
                .creature(BritishRegistry.DAGON.get())
                .title("Dagon")
                .entity(BritishRegistry.DAGON.get(), "Dagon, Prince of Hell")
                .text("One of the Princes of Hell, Azazel's kin, set by Lucifer to guard his child's mother. She rarely moves her feet. "
                        + "With a flick of her hand she hurls whoever stands in a violet lane aside; her fist closes slowly round a hunter "
                        + "(a violet circle: long to close, hard when it does); her palm throws hellfire down a red lane.")
                .text("At two thirds she sweeps a ring of fire round her (a red band: only close to her is safe) and pins hunters in the "
                        + "air (a small violet circle). At a third she lifts off the ground and shrieks (a red ring round her: thrown "
                        + "back and dazed). Run from her and she drags you back. She is a demon: what kills demons hurts her most.")
                .text("Her fall is the end of season twelve: the birth follows. She leaves every hunter her bust and her brand.")
                .recipe(ritual("summon_dagon"), "Calling her back, at the safe house")
                .items("What she leaves", BritishRegistry.DAGONS_BRAND, BritishRegistry.DAGON_TROPHY_ITEM));

        out.add(entry("dagons_brand", JournalChapter.NEPHILIM).order(18).icon(BritishRegistry.DAGONS_BRAND)
                .unlock(any(has(BritishRegistry.DAGONS_BRAND.get()), adv("all_along_the_watchtower")))
                .title("Dagon's Brand")
                .text("A charm of a Prince of Hell's fire. Raise it and a ring of hellfire goes out round you: every foe within four "
                        + "blocks burns and is hurt, and the flames leave you alone for ten seconds. It rests twenty seconds between uses, "
                        + "and wears out after two dozen.")
                .items("The brand", BritishRegistry.DAGONS_BRAND));
    }
}
