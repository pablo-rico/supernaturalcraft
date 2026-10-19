package org.papiricoh.supernaturalcraft.datagen.journal;

import net.minecraft.world.item.Items;
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
 * Witches and Rowena (v0.21 P3): the coven's creatures in the bestiary, the places they keep (and the abandoned churches),
 * the rite that cleanses a church, Rowena's calling card, her house, the woman herself, and what she says of Cain; and Cain
 * himself (P4): his farm, his address and his honey, the fight, and the Mark of Cain.
 * Every number here is the code's ({@code WitchEntity}, {@code CovenWitchEntity}, {@code FamiliarEntity},
 * {@code HexedVillagerEntity}, {@code RowenaLessons}, {@code ConsecrateChurchEffect}).
 */
final class JournalCoven {

    private JournalCoven() {
    }

    static void add(List<SNJournal.Entry> out) {
        creatures(out);
        places(out);
    }

    private static void creatures(List<SNJournal.Entry> out) {
        out.add(entry("hedge_witch", JournalChapter.MONSTERS).order(30).icon(AllItems.TAGLOCK)
                .creature(AllEntities.HEDGE_WITCH.get())
                .title("Hedge Witches")
                .entity(AllEntities.HEDGE_WITCH.get(), "A hedge witch, satchel full of someone else's hair")
                .text("Not every witch is a crone over a cauldron. These ones walk the forests, swamps, taiga and plains at night, alone "
                        + "or in twos, and they work the same craft you do: sigils and the bowl. They keep six to fourteen blocks off and "
                        + "throw sigil bolts, mostly hexes that jinx you, now and then frost, and once in a while pain.")
                .text("Get close and she will slip a curse bag into your pack; a protection bag on you turns it away. Hurt her badly and "
                        + "she drinks something foul and mends. Strike her up close and she may vanish in yellow smoke and come out of it "
                        + "eight to fourteen blocks away. Hurt one and every sister within sixteen blocks comes for you.")
                .text("Left alone near a village, a hedge witch hexes villagers into her thralls, three at most in one place. And if her "
                        + "familiar steals a lock of your hair, she curses you with misfortune every thirty seconds, from wherever she is. "
                        + "A Ward of the Name or a protection bag turns the curse back on her; a Mirror Hex sends it home.")
                .items("What she carries: taglocks, a page, now and then a vial of her blood", AllItems.TAGLOCK, AllItems.SPELL_PAGE,
                        AllItems.BLOOD_VIAL)
                .text("A witch killed by a hunter sometimes leaves a vial of her own blood: something of hers for the bowl."));

        out.add(entry("coven_witch", JournalChapter.MONSTERS).order(31).icon(AllItems.HEXED_INK)
                .creature(AllEntities.COVEN_WITCH.get())
                .title("Coven Witches")
                .entity(AllEntities.COVEN_WITCH.get(), "A coven witch behind her bubble")
                .text("The elite of a coven: forty-eight health, light armour, stronger magic, and a violet bubble that swallows the "
                        + "first fourteen points of harm you do her. A salt round or Break Hex shatters it at once, and the Colt goes "
                        + "straight through. Twenty seconds later it rises again.")
                .text("Her worst trick is a curse. A violet circle opens under your feet: step out of it before she finishes, a little "
                        + "over a second, or you carry frailty or silence for thirty seconds. Silenced, you cast nothing.")
                .text("If her familiar is gone she calls another, a cat or a raven, out of the smoke. She blinks away in smoke more "
                        + "often than her sisters do.")
                .items("A quarter of them leave their blood; a third carry a calling card", AllItems.BLOOD_VIAL, AllItems.ROWENAS_CALLING_CARD)
                .text("Some of them carry an embossed card in green ink. Read the page on Rowena's calling card before you throw it away."));

        out.add(entry("familiar", JournalChapter.MONSTERS).order(32).icon(Items.FEATHER)
                .creature(AllEntities.FAMILIAR.get())
                .title("Familiars")
                .entity(AllEntities.FAMILIAR.get(), "A witch's cat, watching")
                .text("A cat that sits too still, a raven that never flies far: a witch's eyes. A familiar that sees you within sixteen "
                        + "blocks marks you. You glow for three seconds, you feel watched, and every witch nearby turns on you, even the "
                        + "ones who cannot see you.")
                .text("It steals, too. When it can, about once a minute, it runs at the nearest hunter, snips a lock of hair and bolts "
                        + "back to its witch, who curses you with it from afar until it is taken back. Kill the familiar and the lock "
                        + "drops where it dies.")
                .text("While it lives within thirty-two blocks of her, its witch's magic is a quarter stronger. A familiar whose witch "
                        + "is dead goes wild after half a minute. Kill it first."));

        out.add(entry("hexed_villager", JournalChapter.MONSTERS).order(33).icon(Items.EMERALD)
                .unlock(any(seen(AllEntities.HEXED_VILLAGER.get()), adv("hexed_villager_saved")))
                .creature(AllEntities.HEXED_VILLAGER.get())
                .title("The Hexed")
                .entity(AllEntities.HEXED_VILLAGER.get(), "A villager under a witch's hex")
                .text("Hollow eyes, a shambling walk, weak swings at whoever comes near. Somebody's neighbour, under a witch's hex. "
                        + "You find them near covens, in abandoned churches, and near villages where a hedge witch has been at work.")
                .text("You can kill one. Do that and its hex passes to you: jinxed and bad luck for two minutes, and nothing to show "
                        + "for it but rotten flesh.")
                .text("Or free it. A salt round does it, and so does Break Hex: cast at it by name, or with it standing by the bowl. "
                        + "It slumps, straightens, and is a villager again, the very villager it was if a witch hexed one. It presses one "
                        + "to three emeralds on you and tells the village what you did.")
                .items("Salt, or the bowl", AllItems.SALT, AllItems.SPELL_BOWL));
    }

    private static void places(List<SNJournal.Entry> out) {
        out.add(entry("abandoned_church", JournalChapter.PLACES).order(10).icon(Items.BELL)
                .unlock(any(has(AllItems.CHRISM), seen(AllEntities.HEXED_VILLAGER.get()), adv("consecrated_church")))
                .title("Abandoned Churches")
                .text("Out on the plains, in the forests and the taiga, snow or none, stand ruined chapels nobody prays in any more: "
                        + "rotten pews, a broken altar, a bell tower, and a crypt under the floor.")
                .text("Their chests keep sigil pages, Enochian and Hunter, of the first three tiers, with holy water, salt, candles "
                        + "and now and then chrism. Their quiet does not always hold: a ghost haunts about a third of them, and a "
                        + "hexed villager or two about a quarter.")
                .items("What they keep", AllItems.SIGIL_PAGE, AllItems.HOLY_WATER, Items.WHITE_CANDLE, AllItems.CHRISM)
                .text("A church can be cleansed: see Consecrate Church, among the rites."));

        out.add(entry("consecrate_church", JournalChapter.RITUALS).order(6).icon(AllItems.CHRISM)
                .unlock(any(has(AllItems.CHRISM), adv("consecrated_church")))
                .title("Consecrate Church")
                .text("Set a ritual altar where an abandoned church's broken altar stands and chalk a small circle round it. Chrism, "
                        + "two holy waters and three white candles on the altar, flint and steel to light it, forty-five mana.")
                .recipe(ritual("consecrate_church"), "Consecrate Church")
                .text("It only takes on a real church's ground, and only once per church. It blesses sixteen blocks round the altar as "
                        + "holy ground and makes the church a sanctuary: every second, any demon or spirit inside is driven out to its "
                        + "edge. The great ones are not bound by it.")
                .text("The first time a church is cleansed it leaves something on the altar: an Enochian sigil page, of the third "
                        + "tier at most."));

        out.add(entry("witch_coven", JournalChapter.PLACES).order(11).icon(AllItems.CURSE_BAG)
                .unlock(any(seen(AllEntities.HEDGE_WITCH.get()), seen(AllEntities.COVEN_WITCH.get()), seen(AllEntities.FAMILIAR.get())))
                .title("Covensteads")
                .text("Deep in swamps, dark forests and old taiga, the witches gather: three huts round a ring of standing stones "
                        + "and a working spell bowl, bones in the grass. Two to four hedge witches keep it with their familiars; in about a third of them "
                        + "a coven witch leads. Their victims shuffle at the edges.")
                .text("Go in with salt and something to turn a curse. Burn the familiars first; break the coven witch's bubble before "
                        + "you waste blows on it.")
                .items("Their chests: pages, taglocks, hexed ink, blood, bags", AllItems.SPELL_PAGE, AllItems.TAGLOCK, AllItems.HEXED_INK,
                        AllItems.BLOOD_VIAL, AllItems.CURSE_BAG)
                .text("The bowl is real, and yours to use once its keepers are gone."));

        out.add(entry("rowenas_calling_card", JournalChapter.PLACES).order(12).icon(AllItems.ROWENAS_CALLING_CARD)
                .unlock(any(has(AllItems.ROWENAS_CALLING_CARD), adv("met_rowena")))
                .title("A Calling Card")
                .text("\"R. MacLeod. Spells, Hexes, Discretion.\" An embossed card in green ink, taken off a coven witch. The ink is "
                        + "not quite dry, and it never will be: it runs toward its owner's door.")
                .items("Rowena's calling card", AllItems.ROWENAS_CALLING_CARD)
                .text("Use it in the Overworld and it tells you which way the ink runs and roughly how far. Anywhere else the ink only "
                        + "pools. No ordinary map leads to her house."));

        out.add(entry("rowenas_house", JournalChapter.PLACES).order(13).icon(Items.FLOWER_POT)
                .unlock(any(has(AllItems.ROWENAS_CALLING_CARD), adv("met_rowena")))
                .title("A House at the Forest's Edge")
                .text("Somewhere between twelve hundred and thirty-two hundred blocks from the middle of the world, where a forest meets "
                        + "open ground, stands a house far nicer than it has any right to be: a garden, a library, a workroom with a "
                        + "spell bowl. Every world has exactly one.")
                .text("Its owner is home whenever a hunter is about. She would like you to wipe your boots."));

        out.add(entry("rowena", JournalChapter.PLACES).order(14).icon(AllItems.SPELL_PAGE)
                .unlock(any(seen(AllEntities.ROWENA_NPC.get()), adv("met_rowena")))
                .creature(AllEntities.ROWENA_NPC.get())
                .title("Rowena MacLeod")
                .entity(AllEntities.ROWENA_NPC.get(), "Rowena, at home")
                .text("Three hundred years a witch, thrown out of the Grand Coven for being better than them, and not shy about it. She "
                        + "will not fight you in her own house. Talk to her.")
                .text("She teaches for a price, copying Witchcraft pages out for you herself: five at most on offer, the lowest tier "
                        + "first, only pages you do not know and your Witchcraft mastery lets you learn. A first-tier page costs eight "
                        + "emeralds; a second-tier one sixteen, two gold ingots and two amethyst shards; a third-tier one twenty-four, four "
                        + "gold and a ghast tear. A Master's page she keeps for her allies, and only one: see Unmark.")
                .items("Her prices", Items.EMERALD, Items.GOLD_INGOT, Items.AMETHYST_SHARD, Items.GHAST_TEAR)
                .text("Every page you buy earns a little of her trust, and some pages she keeps for those she trusts: her own Familiar "
                        + "Bond only after five. She also sells three taglocks for three emeralds, and Witchcraft hexed ink for six.")
                .text("She hears things. Ask her the right question at the right time, and she may tell you where to find someone you "
                        + "would rather not meet."));

        out.add(entry("rowenas_clue", JournalChapter.PLACES).order(15).icon(Items.HONEYCOMB)
                .unlock(adv("rowenas_clue"))
                .title("Where Cain Keeps His Bees")
                .text("With Lucifer back in his box, Rowena told me where Cain is. The Father of Murder, the first demon-killer and the "
                        + "first Knight of Hell, keeps bees now: a little farm out where the roads give up, honey and silence.")
                .text("\"Mind the bees. Mind him more.\""));

        // --- Cain (v0.21 P4): the man at home, his honey, his address, the fight and the Mark ----------------------------------
        out.add(entry("cains_farm", JournalChapter.PLACES).order(16).icon(AllItems.CAINS_HONEY)
                .unlock(any(seen(AllEntities.CAIN_NPC.get()), adv("met_cain"), has(AllItems.CAINS_ADDRESS)))
                .creature(AllEntities.CAIN_NPC.get())
                .title("The Beekeeper")
                .entity(AllEntities.CAIN_NPC.get(), "Cain, among his hives")
                .text("A quiet farm, far out where the roads give up, between eighteen hundred and four thousand blocks from the "
                        + "world's heart: a fieldstone farmhouse, a red barn, four fields round a yard, a well, a row of hives on the "
                        + "east side and a fenced plot under an oak. The man who keeps it is Cain: the first murderer, the first Knight "
                        + "of Hell, retired.")
                .text("He only comes out to hunters who have put Lucifer back in his box and have not yet beaten him. He tends his "
                        + "hives, and he will talk: who he is, a jar of honey once a day, and the Mark, which he will not hand over.")
                .text("Push him and he will test you, but only at night, in his own yard. Nobody makes you do it."));

        out.add(entry("cains_address", JournalChapter.PLACES).order(17).icon(AllItems.CAINS_ADDRESS)
                .unlock(any(has(AllItems.CAINS_ADDRESS), adv("rowenas_clue")))
                .title("Cain's Address")
                .text("Rowena wrote it down for me in her elegant hand: \"Bees. End of the roads. Knock.\" The ink is witchcraft: use "
                        + "the note and it tells which way his farm lies and how far, and the needle drawn on it turns toward his gate.")
                .text("Lose it and ask her again: she will write another, with a sigh.")
                .items("Cain's address", AllItems.CAINS_ADDRESS));

        out.add(entry("cains_honey", JournalChapter.PLACES).order(18).icon(AllItems.CAINS_HONEY)
                .unlock(any(has(AllItems.CAINS_HONEY), adv("met_cain"), adv("marked")))
                .title("Cain's Honey")
                .text("A jar from the east hives. Cain gives one a day to whoever asks him nicely, and leaves three when he falls the "
                        + "first time (one in a rematch).")
                .text("It is food like any honey. To someone who bears the Mark of Cain it is more: a spoonful quiets a quarter of the "
                        + "Mark's Thirst. That is how he kept his own Mark asleep for a century: bees, and this.")
                .items("Cain's honey", AllItems.CAINS_HONEY));

        out.add(entry("cain", JournalChapter.BOSSES).order(48).icon(AllItems.CAIN_TROPHY)
                .unlock(any(seen(AllEntities.CAIN.get()), adv("met_cain"), adv("first_born")))
                .creature(AllEntities.CAIN.get())
                .title("Cain")
                .entity(AllEntities.CAIN.get(), "Cain, the Father of Murder")
                .text("Nobody makes you fight him. If you do, it is at night in his yard, a ring sixteen blocks wide that his own hives "
                        + "stand just outside. Three phases, a third of his strength each.")
                .text("The Beekeeper: three lunges in a row, each down a red lane drawn just before it, and swarms of bees in yellow "
                        + "circles that sting, poison and blind.")
                .text("The Mark awakens: a ploughing swing that tears up a red cone of earth; Abel's blood, a red ring of spikes round "
                        + "him with the green ground at his feet the only safe place; and farm tools hurled down violet lanes.")
                .text("The Father of Murder takes up the First Blade: a red circle where you stand, then he is behind you and the "
                        + "blade comes down; and the Mark's flare, which burns the whole yard but for three green shelters toward his "
                        + "hives. Run for them.")
                .text("His own First Blade bites him half as hard again. As he dies he kneels and offers the Mark to everyone who fought "
                        + "him. Refuse it and you may still take it later, at his grave in the plot under the oak.")
                .items("What he leaves: his bust and three jars of honey (a bust and one jar in a rematch)", AllItems.CAIN_TROPHY,
                        AllItems.CAINS_HONEY));

        out.add(entry("the_mark", JournalChapter.DEMONS).order(23).icon(AllItems.FIRST_BLADE)
                .unlock(any(adv("marked"), adv("first_born"), adv("demon_3")))
                .title("The Mark of Cain")
                .text("Taken from Cain as he dies, or later at his grave, it burns into the forearm. Every Knight of Hell bears it, and "
                        + "the Knight's rite will not take a Prince who does not already.")
                .text("The Mark thirsts. For five minutes after a kill it rests; then its Thirst climbs a hundredth every minute. "
                        + "Every kill slakes it, a hundredth for each soul the victim was worth (two at least, thirty at most), and "
                        + "Cain's honey takes a quarter off. The more it thirsts, the less healing does: at its worst, only half.")
                .text("From half Thirst you are in bloodlust: your blows land a fifth harder, it whispers, and a demon's Corruption "
                        + "drains and will not come back. At full it feeds on you, a point of health every ten seconds. Die at full "
                        + "Thirst as a human, or as an angel whose grace it burns out, and you do not stay dead: you rise a demon, "
                        + "and the Thirst is left at half.")
                .text("The First Blade answers it: in a Marked hand it strikes three times as hard and never hungers, for the Mark's "
                        + "Thirst takes the blade's place. A Mark that was ever taken off and is taken again remembers the way: it comes "
                        + "back already at half.")
                .text("There is a cure in the Book of the Damned, if a witch will read it for you: see Unmark. A Knight whose Mark "
                        + "is taken off stays a Knight.")
                .items("Its keepers", AllItems.FIRST_BLADE, AllItems.CAINS_HONEY));
    }
}
