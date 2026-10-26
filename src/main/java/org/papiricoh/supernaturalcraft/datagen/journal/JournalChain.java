package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.any;
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
                        + "or an angel that kills a hunter earns a name and a place in its side's chain: Soldier, Captain, Lieutenant "
                        + "and, over them all, a single Lord. Each chain holds at most ten Soldiers, six Captains, three Lieutenants "
                        + "and one Lord. Its officers are the same creatures as ever, only stronger with every rank and every kill, "
                        + "quicker with their powers, and never alone above the lowest rank.")
                .text("The Pit's chain stirs once Azazel has fallen; the Host's once the angels walk the Earth, and its angels "
                        + "only Fall after Metatron. Each wakes with a Lord and a staff already in place.")
                .text("Kill a hunter, and an officer may rise. Its side marks the hunter, and from then on officers come looking "
                        + "for them, more often the more they have lost to that chain. Angels and demons who serve the same side are "
                        + "never hunted by its chain.")
                .text("Kill an officer, and its post falls vacant: the ones under it fight for it at once. The one above it will "
                        + "come for you within a few days. Strike it down with the wrong weapon and it may return, scarred, a few days "
                        + "later; a demon only stays dead to demon-bane or holy smiting, an angel only to an angelic blade. Slay a "
                        + "Lord and the whole chain falls into disarray for five days.")
                .text("Every day the chains move on their own. Underlings challenge their masters, empty posts are filled, "
                        + "officers turn on their masters, angels Fall, and the two chains skirmish with each other. The Lord and the "
                        + "Lieutenants of each side hold a seat far from the world's centre: the Pit Court and the Garrison. "
                        + "The Chain of Command tab of this book keeps both chains and their history.")
                .entity(AllEntities.BLACK_EYED_DEMON.get(), "An officer of the Pit"));
    }
}
