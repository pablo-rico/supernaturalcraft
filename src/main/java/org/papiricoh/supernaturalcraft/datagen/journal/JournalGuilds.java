package org.papiricoh.supernaturalcraft.datagen.journal;

import org.papiricoh.supernaturalcraft.journal.JournalChapter;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.adv;
import static org.papiricoh.supernaturalcraft.datagen.journal.JournalContent.ritual;
import static org.papiricoh.supernaturalcraft.datagen.journal.SNJournal.entry;

/** Player guilds (v0.29): founding, service, diplomacy and succession after Lucifer's defeat. */
final class JournalGuilds {
    private JournalGuilds() {}

    static void add(List<SNJournal.Entry> out) {
        out.add(entry("player_guilds", JournalChapter.BASICS).order(80).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Player Guilds")
                .text("After killing Lucifer, any hunter may found a guild: human, demon or angel. At most four may stand in a world. Choose a name, colour and emblem of your own; your standard is your seat, and you are its Lord.")
                .text("Invite hunters from the Chain of Command tab of the Hunter's Book; they answer there too. Membership is exclusive: joining a guild leaves your post in the Pit or the Host. A hunter cannot serve two guilds.")
                .text("A guild has the same ranks as the older chains, but hired NPCs may fill only half the places at each rank. The remaining places belong to hunters. See Recruiting for paid service, duels and binding, and Guild Interregnum for what happens when the Lord is gone."));

        out.add(entry("guild_standard", JournalChapter.PLACES).order(70).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("The Standard")
                .text("Rename a vanilla banner on an anvil first: its name becomes the guild's name, 3–24 letters, digits, spaces or the marks ' - . &. After Lucifer's defeat, use that banner to activate the founding rite. It becomes a Guild Standard; planting it founds the guild.")
                .text("Plant it in the Overworld, at least 300 blocks from the origin and at least 256 blocks from any other seat or standard. This is a seat you choose yourself; see Player Guilds for membership and command.")
                .text("Enemies can break the standard. Without it the guild is homeless and its NPC officers do not appear. Raise it again within seven days: after that the guild loses its ground on the front. Replanting restores its seat.")
                .recipe(ritual("found_guild"), "The founding rite"));

        out.add(entry("guild_recruiting", JournalChapter.BASICS).order(81).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Recruiting")
                .text("Only the Lord or a Commander may hire for the guild. Pay a wild demon, angel, fallen angel or rival hunter and it enters at rank I. A demon asks 16 sulfur and 8 gold ingots; an angel, 4 holy oil and 8 gold ingots; a fallen angel, 2 fallen blood and 8 gold ingots; a hunter, 16 emeralds and 8 salt.")
                .text("An enemy officer of rank I–III may stake its service on a duel. Beat it and it deserts to your guild one rank lower, with rank I as the floor. Ask it to fight for its allegiance rather than simply killing it.")
                .text("The binding rite is the costly third road: write the name of an officer of any rank up to IV and bind it to your guild. It keeps its rank. Every method needs a vacant NPC place at the resulting rank; at most half of any rank may be NPCs. Read Guild Diplomacy before bringing old enemies under one standard.")
                .recipe(ritual("bind_officer"), "Binding a named officer"));

        out.add(entry("guild_diplomacy", JournalChapter.BASICS).order(82).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Guild Diplomacy")
                .text("Your guild keeps its own stance toward every faction: hostile, neutral or allied. A new guild is hostile to the Pit and the Host, and neutral to other guilds. SucroCorp is always hostile; there is no bargain with the Board.")
                .text("Declare hostility on your own authority; peace requires both sides. Send and answer offers from the book. Allies never wage war against one another. Wild demons and angels judge a guild member by the stance of that guild, even when the hunter once served another side.")
                .text("The front is divided into 24 sectors of 15°. A guild can hold ground there like the older factions, but needs its standard to keep a home. See The Standard before leaving your seat undefended."));

        out.add(entry("guild_interregnum", JournalChapter.BASICS).order(83).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Guild Interregnum")
                .text("If the founder leaves or is away for thirty days, the throne stands empty. An interregnum lasts three days. The hunters of the guild may claim the throne from the book; hired NPCs cannot claim it.")
                .text("Rival claimants fight each other for the succession. The victor takes the throne and the guild continues under its new Lord. If nobody claims it, the guild dissolves. A planted standard cannot save a guild that has nobody willing to lead it."));

    }
}
