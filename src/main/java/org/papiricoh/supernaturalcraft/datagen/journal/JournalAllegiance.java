package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.bowl;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * v0.13 Allegiance: staying human (the hunter's ranks), becoming an angel or a demon (four ranks each, by rite), Grace and
 * Corruption, the powers on the wheel, the weaknesses that come with them, the cures, Heaven's messenger and the rival
 * hunters who hunt the sworn. Numbers follow {@code allegiance/power/Power}, {@code EssenceRules} and the rites' JSON.
 */
final class JournalAllegiance {

    private JournalAllegiance() {
    }

    static void add(List<SNJournal.Entry> out) {
        basics(out);
        heaven(out);
        hell(out);
        monsters(out);
        rites(out);
    }

    private static void basics(List<SNJournal.Entry> out) {
        out.add(entry("allegiance", JournalChapter.BASICS).order(20).icon(AllItems.HUNTERS_AMULET)
                .unlock(any(adv("yellow_eyed"), adv("deal_with_the_devil"), adv("fine_print")))
                .title("Heaven, Hell and Free Will")
                .text("Where each road begins. Heaven: defeat Azazel and wait for the dawn, when its messenger comes to offer you "
                        + "Grace (see The Angel's Road). Hell: call a crossroads demon with the bowl (Summon a Crossroads Demon) and ask for \"Make me one of you\", "
                        + "or tick \"Bind my soul\" on any deal and let the hounds take you when it falls due (see The Demon's Road). "
                        + "The hunter's road: swear the Hunter's Oath at the altar (see Rites of the Road).")
                .text("Every hunter starts human, and free. Heaven may send for you, Hell may bargain for you, or you may stay what "
                        + "you are. An angel climbs four ranks (Lesser Angel, Seraph, Archangel, General of the Host), a demon four "
                        + "more (Crossroads Demon, Prince of Hell, Knight of Hell, King of Hell); each rank is a rite at the altar, paid "
                        + "for with what the great enemies leave behind. The fourth road of the roadmap follows all three branches.")
                .text("A side has its own strength: Grace for an angel, Corruption for a demon, in a ring around the emblem beside "
                        + "your mana. The bar holds 100 at the first rank, then 150, 200 and 250. Hold V to open the wheel of your "
                        + "powers and point at one, let go to choose it, and press B to use it on whatever you are looking at. "
                        + "Passive gifts work on their own. Every rank, on any road, also deepens your mana by 25 (125 at the "
                        + "first, up to 200 at the fourth), so that each rite can be paid at the rank before it.")
                .text("Choosing has a price. Holy things burn what you have become, monsters of your side leave you alone and the "
                        + "other side hunts you, and so do other hunters. Your eyes give you away when you use your powers. "
                        + "A human keeps their free will: no possession takes them, Heaven cannot mark them, and Michael never asks "
                        + "them for their yes. Their rites cost a quarter less mana and they are given longer to recite at the bowl.")
                .text("The way back costs too: a demon can be cured over three nights, an angel can have their Grace cut out. Either "
                        + "way you are human again, without your ranks, and must wait three days before choosing a side once more."));

        out.add(entry("hunter_ranks", JournalChapter.BASICS).order(21).icon(AllItems.COLT_BULLET)
                .unlock(any(adv("fine_print"), adv("hunter_1")))
                .title("The Hunter's Ranks")
                .text("A hunter who stays human can still rise: three ranks, each sworn at the altar. Only a human sworn to no side "
                        + "may take the oath, and choosing Heaven or Hell later gives the hunter's ranks up.")
                .text("Hunter: the oath, sworn over a Colt bullet, four salt, two holy waters and a silver machete; your mana runs "
                        + "deeper (25 more for each rank). Veteran: a night's vigil over Lilith's and Metatron's trophies; supernatural things within 24 blocks glow "
                        + "faintly to you. Legend: the four Horsemen's rings and a Fallen Star; your hunter's weapons strike the "
                        + "supernatural a fifth harder, and possession slides off you.")
                .recipe(ritual("hunters_oath"), "The hunter's oath")
                .recipe(ritual("veterans_vigil"), "The veteran's vigil")
                .recipe(ritual("legend_of_the_road"), "A legend of the road"));
    }

    private static void heaven(List<SNJournal.Entry> out) {
        out.add(entry("the_messenger", JournalChapter.ANGELS).order(20).icon(AllItems.VIAL_OF_GRACE)
                .unlock(any(adv("fine_print"), seen(AllEntities.MESSENGER.get()), adv("heeded_the_call")))
                .creature(AllEntities.MESSENGER.get())
                .title("Heaven's Messenger")
                .entity(AllEntities.MESSENGER.get(), "An angel in a borrowed trench coat")
                .text("At the first dawn after you first bring Azazel down, an angel comes to you: a tired man in a trench coat, with "
                        + "the shadow of wings behind him. He cannot be hurt, and he only wants to talk. He offers you Grace.")
                .text("Hear him out and he gives you a Vial of Grace and the way to take it in. Turn him away and he comes back three "
                        + "dawns later. Once heeded he no longer comes on his own, but a spell at the bowl can call him again. He never "
                        + "comes to a demon.")
                .items("What he brings", AllItems.VIAL_OF_GRACE)
                .recipe(bowl("summon_messenger"), "Calling the messenger"));

        out.add(entry("angel_path", JournalChapter.ANGELS).order(21).icon(AllItems.ANGEL_BLADE)
                .unlock(any(adv("fine_print"), adv("heeded_the_call"), adv("angel_1")))
                .title("The Angel's Road")
                .text("How to begin. There is no spell for it: Heaven has to come to you. Defeat Azazel, the Yellow-Eyed Demon, "
                        + "and at the next dawn an angel in a trench coat finds you wherever you are in the Overworld. Hear him out and "
                        + "take what he offers: a Vial of Grace, and the page to call him again (the bowl spell Summon the Messenger). "
                        + "Turned away, he comes back three dawns later.")
                .text("Then raise the rite Receive Grace: a ritual altar inside a grace circle (a ring of chalk with four lit white "
                        + "candles, north, south, east and west, two blocks from the altar), by day, in the Overworld. Lay the Vial of "
                        + "Grace, two holy waters and four gold ingots on it and light it with an Angel Blade, which you keep.")
                .text("Lesser Angel. Receive Grace by day in the Overworld: the Vial of Grace, two holy waters and four gold ingots, "
                        + "with an Angel Blade to raise it. Teleport where you look, heal with a touch, draw an angel blade from your "
                        + "sleeve, and listen to Angel Radio: for a while, demons and bosses within 128 blocks whisper where they are.")
                .text("Seraph. A Damned Contract, three choir shards and the Seraph Wings: your own wings, folded at rest and open in "
                        + "flight, carry you for as long as their strength lasts. Smite: a palm to the face of what you look at.")
                .text("Archangel. By day, Lucifer's Grace and an Archangel Blade. Your wings turn to light and you can show your true "
                        + "form for ten seconds: every demon that sees it burns, and the light blinds the rest.")
                .text("General of the Host. Michael's Grace and a Fallen Star, with Michael's Lance in hand: a captain and four "
                        + "soldiers of the Host answer you for a minute, and you can hurl lances of light.")
                .recipe(ritual("receive_grace"), "Receiving Grace")
                .recipe(ritual("seraph_ascension"), "The Seraph's ascension")
                .recipe(ritual("archangel_ascension"), "The Archangel's ascension")
                .recipe(ritual("usurp_the_host"), "Usurping the Host"));

        out.add(entry("grace", JournalChapter.ANGELS).order(22).icon(AllItems.VIAL_OF_GRACE)
                .unlock(adv("angel_1"))
                .title("Grace")
                .text("An angel's powers burn Grace, and it does not come back by itself. Slay demons for it (a demon boss is worth "
                        + "far more). Kneel, crouched and still, on consecrated ground to pray it back, or simply stand there and it "
                        + "trickles in. Consecrated ground is the ground around a ritual altar, a choir altar or an Enochian pillar, "
                        + "or any ground the consecration rite has blessed.")
                .text("What hurts an angel: holy oil set alight (it cannot cross the ring), the Enochian banishing sigil, an angel "
                        + "blade or Ruby's knife in another's hand (three times the harm), and the Darkness, who drinks Grace in her "
                        + "eclipse. Ghosts will not touch you, and the Host of Heaven does not see you as an enemy.")
                .recipe(ritual("consecrate_ground"), "Consecrating ground")
                .recipe(ritual("rip_out_grace"), "Cutting the Grace out"));

        out.add(entry("holy_oil", JournalChapter.ANGELS).order(23).icon(AllItems.HOLY_OIL)
                .unlock(any(has(AllItems.HOLY_OIL), adv("angel_1")))
                .title("Holy Oil")
                .text("The one fire an angel fears. Pour it on the ground and it runs out in a ring two blocks wide and catches at "
                        + "once: an angel inside cannot leave until it burns out, and one who crosses the flames is burned.")
                .items("Holy oil", AllItems.HOLY_OIL));

        out.add(entry("hosts_answer", JournalChapter.ANGELS).order(24).icon(AllItems.MICHAEL_LANCE)
                .unlock(any(seen(AllEntities.HOST_ALLY.get()), adv("angel_4")))
                .creature(AllEntities.HOST_ALLY.get())
                .title("The Host Answers")
                .entity(AllEntities.HOST_ALLY.get(), "A soldier of the Host, at a General's side")
                .text("A General of the Host calls down a squad: a captain and four soldiers who follow you, fight what you fight and go "
                        + "back to Heaven after a minute."));
    }

    private static void hell(List<SNJournal.Entry> out) {
        out.add(entry("demon_path", JournalChapter.DEMONS).order(20).icon(AllItems.DEMON_BLOOD)
                .unlock(any(adv("fine_print"), adv("deal_with_the_devil"), adv("soul_bound"), adv("demon_1")))
                .title("The Demon's Road")
                .text("Crossroads Demon. At the crossroads, tick \"Bind my soul\" before you seal a deal: if the hounds come for you "
                        + "and take you, you rise a demon instead of losing what you were owed. Or wish to be made one of them and "
                        + "become one at once, for two hearts the demon keeps until you are cured. Smoke rushes you where you look, "
                        + "fire cannot burn you, a hellhound comes when you call, and black eyes see in the dark.")
                .text("Prince of Hell. By night in the Overworld: Azazel's blood, a choir shard and two demon bloods. You no longer "
                        + "hunger nor sleep; you can seize what you look at and hurl it, or ride a mob for twenty seconds. Your eyes turn "
                        + "yellow.")
                .text("Knight of Hell. The Mark of Cain: the Angel Tablet and two demon bloods, with the First Blade to raise it. The "
                        + "Blade answers its Knight and every kill heals you, but the Mark thirsts: go too long without a kill and it "
                        + "drinks your Corruption, then your blood.")
                .text("King of Hell. Usurp the throne on the dais of Lucifer's Cage with a Fallen Star and Michael's Grace. Demons "
                        + "and hellhounds obey you, and from the throne every mob near you kneels. You wear the crown and the cloak.")
                .recipe(ritual("prince_of_hell"), "A Prince of Hell")
                .recipe(ritual("mark_of_cain"), "The Mark of Cain")
                .recipe(ritual("usurp_the_throne"), "Usurping the throne"));

        out.add(entry("corruption", JournalChapter.DEMONS).order(21).icon(AllItems.CROSSROADS_CONTRACT)
                .unlock(adv("demon_1"))
                .title("Corruption")
                .text("A demon's powers burn Corruption, and nothing gives it back but harm done. Slay angels and rival hunters, and "
                        + "strike pacts: crouch and offer a villager an emerald, and they give you something for it, once each. "
                        + "Villagers who see your eyes run.")
                .text("What hurts a demon still hurts you: salt lines stop you, a devil's trap holds you, holy water burns, and an "
                        + "exorcism tears the smoke out of you (it does not kill, but it leaves you empty). Demons and hellhounds of "
                        + "your own side leave you alone; the Host and angels hunt you."));

        out.add(entry("demon_cure", JournalChapter.DEMONS).order(22).icon(AllItems.PURIFIED_BLOOD)
                .unlock(any(adv("demon_1"), has(AllItems.PURIFIED_BLOOD)))
                .title("The Cure")
                .text("A demon can be made human again, a night at a time. Mix purified blood at the bowl (a vial of blood in holy "
                        + "water), then stand inside a devil's trap and give it, with holy water, at the altar: three times, on three "
                        + "different nights. The last night gives back the hearts the crossroads took. You lose your ranks, and must "
                        + "wait three days before you may choose a side again.")
                .recipe(bowl("purified_blood"), "Purified blood")
                .recipe(ritual("demon_cure"), "The cure, one night of three"));
    }

    private static void monsters(List<SNJournal.Entry> out) {
        out.add(entry("rival_hunter", JournalChapter.MONSTERS).order(20).icon(AllItems.SILVER_MACHETE)
                .unlock(any(seen(AllEntities.RIVAL_HUNTER.get()), adv("angel_1"), adv("demon_1")))
                .creature(AllEntities.RIVAL_HUNTER.get())
                .title("Rival Hunters")
                .entity(AllEntities.RIVAL_HUNTER.get(), "A hunter on the road")
                .text("Other hunters work these roads too: in a leather jacket, a flannel shirt or a long coat, with a sawn-off "
                        + "shotgun loaded with rock salt, a machete and a flask of holy water. To a human they are friends, and they "
                        + "will help you against demons. To an angel or a demon they are death: they come by night, shoot from range, "
                        + "close in with the machete and throw holy water at whatever has black eyes.")
                .text("What they carry: salt, holy water, and once in a while a bullet for the Colt."));
    }

    private static void rites(List<SNJournal.Entry> out) {
        out.add(entry("rites_of_heaven", JournalChapter.RITUALS).order(20).icon(AllItems.VIAL_OF_GRACE)
                .unlock(any(adv("fine_print"), adv("heeded_the_call"), adv("angel_1")))
                .title("Rites of Heaven")
                .text("The angel's ranks, from the first Grace to the Host. Each needs the rank before it.")
                .recipe(ritual("receive_grace"), "Lesser Angel")
                .recipe(ritual("seraph_ascension"), "Seraph")
                .recipe(ritual("archangel_ascension"), "Archangel")
                .recipe(ritual("usurp_the_host"), "General of the Host"));

        out.add(entry("rites_of_hell", JournalChapter.RITUALS).order(21).icon(AllItems.AZAZEL_BLOOD)
                .unlock(any(adv("fine_print"), adv("demon_1")))
                .title("Rites of Hell")
                .text("The demon's ranks after the crossroads. Each needs the rank before it.")
                .recipe(ritual("prince_of_hell"), "Prince of Hell")
                .recipe(ritual("mark_of_cain"), "Knight of Hell")
                .recipe(ritual("usurp_the_throne"), "King of Hell"));

        out.add(entry("rites_of_the_road", JournalChapter.RITUALS).order(22).icon(AllItems.SALT)
                .unlock(any(adv("fine_print"), adv("hunter_1")))
                .title("Rites of the Road")
                .text("The hunter's oaths, and the rites that undo a side and bless the ground.")
                .recipe(ritual("hunters_oath"), "Hunter")
                .recipe(ritual("veterans_vigil"), "Veteran")
                .recipe(ritual("legend_of_the_road"), "Legend")
                .recipe(ritual("demon_cure"), "The demon's cure")
                .recipe(ritual("rip_out_grace"), "Cutting out Grace")
                .recipe(ritual("consecrate_ground"), "Consecration of ground"));
    }
}
