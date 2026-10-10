package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.bowl;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.has;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** Demons, angels, monsters and spirits: the bestiary (the great ones are in {@link JournalBosses}). */
final class JournalBestiary {

    private JournalBestiary() {
    }

    static void add(List<SNJournal.Entry> out) {
        demons(out);
        angels(out);
        monsters(out);
    }

    private static void demons(List<SNJournal.Entry> out) {
        out.add(entry("black_eyes", JournalChapter.DEMONS).order(0).icon(AllItems.DEMON_BLOOD)
                .creature(AllEntities.BLACK_EYED_DEMON.get())
                .title("Black Eyes")
                .entity(AllEntities.BLACK_EYED_DEMON.get(), "A black-eyed demon, riding a stolen body")
                .text("The common kind, and the one you will meet first: a demon riding someone it took, out in the dark of the "
                        + "Overworld. It brawls up close, and every few seconds it flicks you away with a gesture.")
                .text("Hurt one badly and it will try to flee its vessel as black smoke, taking its blood with it. Stand it on a "
                        + "devil's trap, or Bind it, and it cannot leave. Ruby's knife always draws blood. Holy water scalds them, "
                        + "Smite hits them twice as hard, and salt is a wall they will not cross.")
                .items("What they leave: blood, sulfur, now and then a page", AllItems.DEMON_BLOOD, AllItems.SULFUR, AllItems.SIGIL_PAGE)
                .text("Only a demon killed by a hunter leaves its blood. You will need a great deal of it."));

        out.add(entry("demon_occultist", JournalChapter.DEMONS).order(1).icon(AllItems.SULFUR)
                .creature(AllEntities.DEMON_OCCULTIST.get())
                .title("Occultists")
                .entity(AllEntities.DEMON_OCCULTIST.get(), "A demon occultist, robed")
                .text("A robed demon who keeps its distance and hurls hellfire. Rarer than the black-eyed kind, and smarter about "
                        + "it. Close the gap, or break its line of sight and make it come to you.")
                .text("Occultists are where the pages come from. They carry torn sigil pages and bowl spell pages far more often "
                        + "than the brawlers do, and more sulfur. In Hell some of them carry one of Crowley's damned contracts."));

        out.add(entry("demon_blood", JournalChapter.DEMONS).order(2).icon(AllItems.DEMON_BLOOD)
                .unlock(any(has(AllItems.DEMON_BLOOD), has(AllItems.SULFUR), has(AllItems.HELLFIRE_EMBER)))
                .title("What Demons Leave")
                .text("Demon blood is the currency of everything that follows: blood chalk, devil's traps, Ruby's knife, every "
                        + "summoning. Only a demon a hunter kills leaves it, and only if it does not smoke out first.")
                .items("Demon blood, sulfur, hellfire ember", AllItems.DEMON_BLOOD, AllItems.SULFUR, AllItems.HELLFIRE_EMBER)
                .text("Sulfur they drop freely, and it shows in Nether rock as ore. It feeds the Hellfire sigil and Enochian ink. "
                        + "Hellfire embers come only from exorcism: a demon cast back down leaves one where it stood."));

        out.add(entry("crossroads", JournalChapter.DEMONS).order(3).icon(AllItems.CROSSROADS_CONTRACT)
                .unlock(any(seen(AllEntities.CROSSROADS_DEMON.get()), has(AllItems.DAMNED_CONTRACT), has(AllItems.SPELL_PAGE),
                        adv("deal_with_the_devil")))
                .creature(AllEntities.CROSSROADS_DEMON.get())
                .title("The Crossroads")
                .entity(AllEntities.CROSSROADS_DEMON.get(), "A crossroads demon: sharp suit, red eyes")
                .text("Call the crossroads demon at night with the bowl: demon blood, a bone, grave dirt, an oxeye daisy and a damned "
                        + "contract. It steps out of the smoke beside the bowl, ready to deal. The old way works too: a box buried at "
                        + "night at a natural crossroads calls a wilder one (The Wild Crossroads, in this book).")
                .recipe(bowl("summon_crossroads"), "Summon a Crossroads Demon")
                .text("It waits on you a couple of minutes, never attacking. Use it to hear the offer. Hit it, walk off, or take too "
                        + "long, and it leaves in smoke.")
                .text("Name your wish and seal it with a kiss. Then the clock starts. One soul, one deal: you cannot owe twice."));

        out.add(entry("the_deal", JournalChapter.DEMONS).order(4).icon(AllItems.CROSSROADS_CONTRACT)
                .unlock(any(adv("deal_with_the_devil"), has(AllItems.CROSSROADS_CONTRACT)))
                .title("The Wishes")
                .text("What it offers, and what it costs in days before the hounds come. Two more hearts, or twenty-five more mana, "
                        + "yours for good: five days. Everything you dropped at your last death that nobody picked up, or a dead pet "
                        + "back on its feet: seven days.")
                .text("Something rare from its pockets, bullets, embers, shards and the like: ten days. Knowledge: a map to what "
                        + "hides nearby, a Hymnal Spire or a restless grave, or failing that, a book on how to kill the next great "
                        + "thing you have not killed yet: ten days.")
                .items("The contract it leaves in your hands", AllItems.CROSSROADS_CONTRACT)
                .text("The contract tells you whose soul, for what, and how long you have. Keep it. You will need it if you mean to "
                        + "break the deal."));

        out.add(entry("the_debt", JournalChapter.DEMONS).order(5).icon(AllItems.HELLHOUND_FANG)
                .unlock(any(adv("deal_with_the_devil"), has(AllItems.CROSSROADS_CONTRACT)))
                .title("The Debt")
                .text("On the last day you will hear the dogs. When it falls due, a pack of three to five hellhounds comes for you, "
                        + "wherever you are. Survive the hunt, by killing the pack or lasting two minutes, and the debt is paid.")
                .text("Die while they are on you and the debt is paid another way. You come back, but not all of you: for three "
                        + "days you are two hearts short and your mana does not return.")
                .text("Or break it. Burn the contract in a bowl, with holy water, salt and a hellfire ember, and the demon who holds it "
                        + "walks again, angry. Kill it before your time runs out and the contract is void. If it flees its vessel, it "
                        + "comes back another night.")
                .recipe(bowl("break_deal"), "Breaking the deal"));

        out.add(entry("hellhounds", JournalChapter.DEMONS).order(6).icon(AllItems.HELLHOUND_FANG)
                .creature(AllEntities.HELLHOUND.get())
                .title("Hellhounds")
                .entity(AllEntities.HELLHOUND.get(), "A hellhound, revealed", 0.55f)
                .text("You will not see them. Watch for the air that bends, the smoke of their breath, the paw prints that burn into "
                        + "the ground. They hunt in packs and their bite holds you where you stand.")
                .text("Holy water, any holy wound or the Reveal sigil shows them for a while; Second Sight outlines them; the Eclipse "
                        + "Sight shows them always. They roam Hell, they come to collect on crossroads debts, and Lilith keeps a few.")
                .items("Now and then, a fang", AllItems.HELLHOUND_FANG)
                .text("Kill one and you may pull a fang. War and Death both want them."));
    }

    private static void angels(List<SNJournal.Entry> out) {
        out.add(entry("on_angels", JournalChapter.ANGELS).order(0).icon(AllItems.ANGEL_BLADE)
                .unlock(any(adv("angel_blade"), seen(AllEntities.LUCIFER.get()), seen(AllEntities.BROKEN_CHORUS.get()),
                        seen(AllEntities.METATRON.get()), seen(AllEntities.CHOIR_ECHO.get())))
                .title("On Angels")
                .text("Forget the halos. An angel is a soldier, old as the world and twice as proud, and most of what hurts a demon "
                        + "does nothing to one. Salt, traps and exorcism: no.")
                .text("Holy things are what works. Angel blades, the Smite sigil, holy water, a Sanctity rune graven in your weapon. "
                        + "Watch the ground, too: everything an archangel does is written on it first.")
                .items("Holy steel", AllItems.ANGEL_BLADE, AllItems.ARCHANGEL_BLADE, AllItems.EXORCISTS_MACE)
                .text("The ones in this book: Lucifer in his Cage, the Broken Chorus over the mountains, and Metatron, who writes "
                        + "everything down."));

        out.add(entry("lucifers_grace", JournalChapter.ANGELS).order(1).icon(AllItems.LUCIFERS_GRACE)
                .unlock(any(has(AllItems.LUCIFERS_GRACE), adv("grace"), adv("devil_went_down")))
                .title("Lucifer's Grace")
                .text("When Lucifer falls, a shard of his grace falls with him. Use it to take it in. Light pours into you, and it "
                        + "does not leave.")
                .items("A shard of an archangel's grace", AllItems.LUCIFERS_GRACE)
                .text("Your mana runs fifty deeper, for good. And the third-tier sigils, the pages written in the hand that burned "
                        + "your eyes, become legible. Echo among them.")
                .text("You only get to carry one shard. The rest are no use to you; give them to someone who hunts with you."));

        out.add(entry("choir_echo", JournalChapter.ANGELS).order(2).icon(AllItems.CHOIR_SHARD)
                .creature(AllEntities.CHOIR_ECHO.get())
                .title("Choir Echoes")
                .entity(AllEntities.CHOIR_ECHO.get(), "A Choir Echo: one small voice")
                .text("One small voice of the Broken Chorus, a wheel around a single eye, set loose every time one of its faces "
                        + "breaks. It circles the choir and sings.")
                .text("While an echo lives, the Chorus winds up faster and its Hymn cuts deeper, and now and then the echo sings a "
                        + "sharp note at whoever is closest. Kill them as they come. The Colt takes one with a single round."));

        out.add(entry("seraph_wings", JournalChapter.ANGELS).order(3).icon(AllItems.SERAPH_WINGS)
                .unlock(any(has(AllItems.SERAPH_WINGS), adv("seraph_wings"), adv("silence_falls")))
                .title("Seraph Wings")
                .text("Six wings, what is left of the Broken Chorus when it is silenced. Worn on the back (Curios), they rest, fold "
                        + "when you crouch, and open wide when you fall.")
                .items("Six wings", AllItems.SERAPH_WINGS)
                .text("They will not carry you anywhere. They are only for show, and what a show."));

        out.add(entry("angel_tablet", JournalChapter.ANGELS).order(4).icon(AllItems.ANGEL_TABLET)
                .unlock(any(has(AllItems.ANGEL_TABLET), adv("scribe_of_god")))
                .title("The Angel Tablet")
                .text("The word of God, cut in stone, taken from Metatron still burning. Use it and it rewrites your story: healed "
                        + "whole, every harm undone, the fire put out.")
                .items("The Angel Tablet", AllItems.ANGEL_TABLET)
                .text("It needs two minutes before it will write again. Save it for the moment you would otherwise not walk away."));
    }

    private static void monsters(List<SNJournal.Entry> out) {
        out.add(entry("ghosts", JournalChapter.MONSTERS).order(0).icon(AllItems.ECTOPLASM)
                .unlock(any(seen(AllEntities.GHOST.get()), has(AllItems.ECTOPLASM), has(AllItems.GRAVE_DIRT), adv("salt_and_burn")))
                .creature(AllEntities.GHOST.get())
                .title("Restless Dead")
                .entity(AllEntities.GHOST.get(), "A vengeful spirit, manifesting")
                .text("Old graves hold more than bones. At night a ghost rises near them and haunts whoever comes close: the cold "
                        + "bites and slows you, the lights die, things fly. You will see it only in flickers, and when it lashes out.")
                .text("Ordinary harm goes straight through it. Cold iron, holy water or anything holy scatters it for a while, and "
                        + "now and then it leaves ectoplasm behind. Salt is a line it cannot cross, walls or no walls. Second Sight and "
                        + "the Reveal sigil show it.")
                .items("Iron, salt, holy water", Items.IRON_SWORD, AllItems.SALT, AllItems.HOLY_WATER, AllItems.ECTOPLASM)
                .text("It cannot be killed. It stays near its bones and sinks back into its grave at dawn. To be rid of it, see to "
                        + "the bones."));

        out.add(entry("salt_and_burn", JournalChapter.MONSTERS).order(1).icon(Items.FLINT_AND_STEEL)
                .unlock(any(seen(AllEntities.GHOST.get()), has(AllItems.GRAVE_DIRT), adv("salt_and_burn")))
                .title("Salt and Burn")
                .text("A lonely graveyard, one to three graves on flat dry ground, headstones and mounds of grave soil. Only one of "
                        + "them is restless. Its bones lie two blocks under the end of the mound by the headstone.")
                .items("Headstone, grave soil, the bones", AllItems.GRAVE_HEADSTONE, AllItems.GRAVE_SOIL, AllItems.GRAVE_BONES)
                .text("Dig the soil and the dirt under it until the bones are bare; while the spirit is restless, they will not "
                        + "break. Salt them. Then set them alight with flint and steel or a fire charge. The ghost goes with them, for "
                        + "good. A Banishing does the same from the bowl.")
                .text("One grave in each yard hides a chest under the foot of its mound. Grave dirt from the mounds goes into hex "
                        + "bags and darker work."));

        out.add(entry("amara_shade", JournalChapter.MONSTERS).order(2).icon(AllItems.VOID_ESSENCE)
                .creature(AllEntities.AMARA_SHADE.get())
                .title("Shades")
                .entity(AllEntities.AMARA_SHADE.get(), "A shade of the Darkness")
                .text("Small echoes of the Darkness that hunt in the dark around her. Light burns them, and strong light unmakes "
                        + "them outright: torches and lanterns set down, the censer's beam, a burning well.")
                .text("Fight them where it is bright and they are no trouble. Let the dark close in and they come in numbers. The "
                        + "Colt kills one with a single round, if you can spare it."));
    }
}
