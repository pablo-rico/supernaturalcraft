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
        out.add(entry("chain_temperaments", JournalChapter.BASICS).order(62).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("Temperaments and Pasts")
                .text("Every named officer has one of six temperaments: Ambitious, Loyal, Cruel, Cunning, Proud or Zealous. It colours what you hear and whom the officer clashes with; it never changes how hard a blow lands. Read the person before you draw the blade.")
                .text("There is a past behind the title. Demons remember the rack and the bodies they stole; angels remember a choir, a willing vessel and the service that earned a post. The fallen remember why they lost Heaven. Choose an officer in the book to read what remains of that story."));

        out.add(entry("chain_bonds", JournalChapter.BASICS).order(63).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("Regard and Pacts")
                .text("Officers keep a measure of regard for one another and for you, from −100 to 100. At −40 you are a rival; at −70, a grudge. A grudge can become a challenge or a betrayal. At 50, regard becomes loyalty. The book shows which bonds have taken root.")
                .text("A stolen promotion, a lost feud or a betrayal sours regard; help and pacts warm it. Left alone, it fades day by day. A pact lasts days and prevents challenges between the two who swore it. An oath buys time, never eternity."));

        out.add(entry("chain_duels", JournalChapter.BASICS).order(64).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("The Ring")
                .text("A duel is fought inside a ring of 8 blocks. Escorts stand aside; step out and you forfeit. Two of one side fight until one yields, below 10% health. Across the sides, the duel is to the death: a proper kill, with a 1.5× reward. Keep your feet where your courage is.")
                .text("An officer with a grudge may call you out after an improper killing, a flight or a humiliation. You have 2 days of play to answer; time away does not count. Refuse or let it lapse, and the chain calls it cowardice: the officer gains a level and you lose a rank if you hold a post. At most one challenge waits on you."));

        out.add(entry("chain_challenge_rite", JournalChapter.BASICS).order(65).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("The Challenge Rite")
                .text("At a ritual altar, offer a book with the officer's name written inside and cast down a Gauntlet of Ash for a demon or fallen officer, or a Seal of Summons for an angel. A name written carelessly calls nobody; a name written correctly calls trouble.")
                .text("The officer comes alone within 30 seconds, to a ring around the altar. There is no escort to cut through and no excuse to step outside. Challenge a superior of your own side and the duel is for that officer's post."));

        out.add(entry("chain_sworn", JournalChapter.BASICS).order(66).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(adv("chain_sworn"))
                .title("A Place in the Chain")
                .text("A sworn angel or demon can hold a real post. Faction rank I means Soldier, II Captain, III Lieutenant and IV Commander. The throne comes only by defeating the Lord in a duel. A hunter who becomes Lord holds the title, not the stronghold; stone does not swear allegiance as easily as flesh.")
                .text("To enter, find a sponsor: an officer with at least 60 regard who will speak for you, or ask the Lord at an audience. Rise through a sponsor, through the Lord's tasks with at least 50 regard and a free post, or through a duel. Ambitious officers can challenge you in turn. A rank is a place to defend."));

        out.add(entry("chain_audience", JournalChapter.BASICS).order(67).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("Audiences and Messages")
                .text("Sworn hunters may walk through their own garrison. In the throne room, ask the Lord for an audience: a task, a promotion, or, if you are a Commander, a challenge for the throne. A task allows 5 days to slay a named officer of the other side, win a skirmish, or bring tribute. The throne has little patience for unfinished work.")
                .text("The book carries up to 3 messages a day, one per officer, with replies at dawn. Ask for a pact, aid or sponsorship, send an insult, or deliver a Lord's orders. Distance keeps the blade away for a while; it does not soften the answer."));

        out.add(entry("chain_parley", JournalChapter.BASICS).order(68).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("Across the Line")
                .text("Any hunter may crouch and speak to an officer of the other side. Temperament decides whether it listens: Cunning officers almost always hear an offer; Cruel ones never do. Lowering yourself is a request for a hearing, not a promise of safety.")
                .text("Tribute may buy a truce: the officer leaves you alone for one day. Demons may also take a bribe for a rival's whereabouts, or a deal: kill the rival for three days of peace. Break your word and the demon remembers. Loyal angels do not sell rivals or make those deals."));

        out.add(entry("chain_war", JournalChapter.BASICS).order(69).icon(AllItems.ASCENSION_SHARD_1)
                .unlock(any(adv("yellow_eyed"), adv("lucifer_rising")))
                .title("The Front and the Vow")
                .text("The officers' country, from 300–2900 blocks out, is divided into 12 sectors of 30°. Each side begins with the half facing its stronghold. Skirmishes and duels across the sides push a sector until it changes hands; the officers there fall back. The front is a border you can move with a blade.")
                .text("When a hunter is near, skirmishes are fought in the world. Help a side and earn its regard. A Lord wronged by a killing may swear a vow and name a champion, with a duel due within 3 days. A grievance at the throne can put a blade in the field.")
                .text("Every 14 days or so, war may be declared for 3–5 days. Skirmishes break out three times as often. The side ahead at the end takes a sector and its Lord gains a level; the losing chain is shaken for 2 days. Count the victories, not the speeches."));

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
