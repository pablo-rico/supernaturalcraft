package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.seen;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/**
 * The Chain of Command: the named officers of the Pit and the Host. Numbers follow {@code chain/ChainRank} and
 * {@code chain/ChainRules}.
 */
final class JournalChain {

    private JournalChain() {
    }

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("chain_of_command", JournalChapter.BASICS).order(60).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("The Chain of Command")
                .text("Hell and Heaven both keep ranks, and the rank and file are not as faceless as they look. A black-eyed demon "
                        + "or an angel that kills a hunter earns a name and a place in its side's chain: Soldier, Captain, Lieutenant, "
                        + "Commander and, over them all, a single Lord. Each chain holds at most ten Soldiers, six Captains, three "
                        + "Lieutenants, two Commanders and one Lord. Its officers are the same creatures as ever, only stronger with every rank and every kill, "
                        + "quicker with their powers, and never alone above the lowest rank.")
                .text("The Pit's chain stirs once Azazel has fallen; the Host's once the angels walk the Earth, and its angels "
                        + "only Fall after Metatron. Each wakes with a Lord and a staff already in place.")
                .text("Kill a hunter, and an officer may rise. Its side marks the hunter, and from then on officers come looking "
                        + "for them, more often the more they have lost to that chain. Angels and demons who serve the same side are "
                        + "never hunted by its chain.")
                .text("Kill an officer, and its post falls vacant; it is filled again a day or two later. The one above it will "
                        + "come for you within a few days. Strike it down with the wrong weapon and it may return, scarred, a few days "
                        + "later; a demon only stays dead to demon-bane or holy smiting, an angel only to an angelic blade. Slay a "
                        + "Lord and its throne stands empty for days while its Commanders feud over it.")
                .text("Every day the chains move on their own. Underlings challenge their masters, empty posts are filled, "
                        + "officers turn on their masters, angels Fall, and the two chains skirmish with each other. The Chain of "
                        + "Command tab of this book keeps both chains and their history.")
                .text("Every officer can be found. The Lord and its Commanders hold court in a stronghold far from the world's "
                        + "centre: the Pit Castle and the Monastery. Soldiers roam territories near the centre, where hunters live; "
                        + "Captains roam further out, toward their stronghold, and Lieutenants on its marches; come close and the "
                        + "officer shows itself, escort and all. A promotion moves it. Choose an officer in this book to read roughly where it roams and "
                        + "where it was last seen; a Men of Letters war table marks every territory exactly, and the chosen one's "
                        + "last sighting.")
                .entity(AllEntities.BLACK_EYED_DEMON.get(), "An officer of the Pit"));

        out.add(entry("chain_strongholds", JournalChapter.PLACES).order(40).icon(AllItems.ASCENSION_SHARD_2)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("The Strongholds")
                .text("Each chain's Lord keeps a stronghold, thousands of blocks from the world's centre and on opposite sides of "
                        + "it. The Pit's is a castle of blackstone ringed by fire, with cages, chains and rooms nobody should see. The "
                        + "Host's is a white monastery, a cloister and a bell tower, quiet as a held breath. A Men of Letters war "
                        + "table marks both.")
                .text("Past the gate, rooms of guards with no names, the Pit's or the Host's rank and file; at the far end, the "
                        + "throne room, where the Lord sits with its Commanders and no one else. Below it, a vault. The guards you "
                        + "kill stay dead until a new Lord takes the throne; while the throne stands empty, half the garrison is "
                        + "gone."));

        out.add(entry("chain_feuds", JournalChapter.BASICS).order(61).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("Feuds and Interregnums")
                .text("The officers of a chain are always at each other's throats. Every few days two of them fight it out: an "
                        + "underling against its master, or two rivals of one rank. Win against a master and you take its rank; any "
                        + "other win is worth a level. The loser leaves with a scar, and one feud in four ends in a death, for a while.")
                .text("Come upon a feud, near the territory of either of them, and you will see it: both officers and their "
                        + "escorts tearing into each other. Wait for the winner, or step in; if you kill one, the feud is the "
                        + "other's.")
                .text("When a Lord dies the throne is not filled at once. For three to five days it stands empty, the chain's "
                        + "feuds flare up every day, and the Commanders fight over it. The one with the most wins is crowned."));

        out.add(creature("demon_brute", JournalChapter.DEMONS, 40, AllEntities.DEMON_BRUTE.get(), "Possessed Brute",
                "A possessed brute, chains round its fist",
                "A demon that picked the biggest body it could find and then made it bigger. It lumbers until it has room, "
                        + "then it charges; get close and it brings both fists down on the ground around it. Keep moving and keep "
                        + "your distance."));
        out.add(creature("sulfur_priest", JournalChapter.DEMONS, 41, AllEntities.SULFUR_PRIEST.get(), "Sulfur Priest",
                "A sulfur priest, censer in hand",
                "The Pit's clergy. It hangs back and throws balls of burning sulfur; corner it and it whirls its censer into "
                        + "a cloud of smoke that blinds and poisons. Close the distance fast, and don't stand in the cloud."));
        out.add(creature("demon_flayer", JournalChapter.DEMONS, 42, AllEntities.DEMON_FLAYER.get(), "Flayer",
                "A flayer, knives out",
                "It folds into black smoke and comes out behind you, and its first cut out of the smoke lands twice. "
                        + "When it vanishes, turn around."));
        out.add(creature("demon_tormentor", JournalChapter.DEMONS, 43, AllEntities.DEMON_TORMENTOR.get(), "Tormentor",
                "A tormentor with its hook",
                "The Pit's jailer. It throws a meat hook on a chain, drags you to it and holds you there while its friends "
                        + "go to work. Break line of sight before it throws."));
        out.add(creature("angel_templar", JournalChapter.ANGELS, 40, AllEntities.ANGEL_TEMPLAR.get(), "Templar",
                "A templar of the Host behind its shield",
                "A soldier of the Host with a tower shield. While it is raised, blows from the front barely land; go round it. "
                        + "Up close its sword cuts wide."));
        out.add(creature("angel_herald", JournalChapter.ANGELS, 41, AllEntities.ANGEL_HERALD.get(), "Herald of Light",
                "A herald, a lance of light in hand",
                "Heaven's messengers still carry spears, only now they are made of light. It keeps away and throws them, "
                        + "straight and fast. Cover is your friend."));
        out.add(creature("angel_choirmaster", JournalChapter.ANGELS, 42, AllEntities.ANGEL_CHOIRMASTER.get(), "Choirmaster",
                "A choirmaster mid-hymn",
                "Its hymn mends every angel around it and rings in the ears of its enemies. Kill it first, or the fight "
                        + "never ends."));
        out.add(creature("angel_dominion", JournalChapter.ANGELS, 43, AllEntities.ANGEL_DOMINION.get(), "Dominion",
                "A dominion with its hammer",
                "Slow, towering and armoured, it does not blink like the others. When it lifts its hammer high, get out of the "
                        + "way: the judgement lands all around it."));
        out.add(creature("grigori", JournalChapter.ANGELS, 44, AllEntities.GRIGORI.get(), "Grigori",
                "A Grigori, wings burnt",
                "The Watchers were sent to look after us, and some of them got hungry. A Grigori fades from sight and feeds on "
                        + "the soul of whatever it grabs, healing as it drinks. It serves the fallen."));
    }

    private static SNJournal.Entry creature(String id, JournalChapter chapter, int order, net.minecraft.world.entity.EntityType<?> type,
                                            String title, String caption, String text) {
        return entry(id, chapter).order(order).icon(chapter == JournalChapter.DEMONS ? AllItems.DEMON_BLOOD : AllItems.ANGEL_BLADE)
                .unlock(any(seen(type)))
                .creature(type)
                .title(title)
                .entity(type, caption)
                .text(text)
                .text("It serves its side's chain of command: in the strongholds, in the escorts of its officers, and now and then "
                        + "as an officer itself.");
    }
}
