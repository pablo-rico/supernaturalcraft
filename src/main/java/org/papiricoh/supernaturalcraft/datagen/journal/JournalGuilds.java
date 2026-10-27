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
        out.add(entry("player_guilds", JournalChapter.CHAIN).order(20).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Player Guilds")
                .text("After killing Lucifer, any hunter may found a guild: human, demon or angel. At most four may stand in a world. Choose a name, colour and emblem of your own; your standard is your seat, and you are its Lord.")
                .text("Invite hunters from the Chain of Command tab of the Hunter's Book; they answer there too. Membership is exclusive: joining a guild leaves your post in the Pit or the Host. A hunter cannot serve two guilds.")
                .text("A guild has the same ranks as the older chains, but hired NPCs may fill only half the places at each rank. The remaining places belong to hunters. See Recruiting for paid service, duels and binding, and Guild Interregnum for what happens when the Lord is gone."));

        out.add(entry("guild_founding", JournalChapter.CHAIN).order(21).icon(AllItems.GUILD_STANDARD)
                .unlock(adv("devil_went_down"))
                .title("Founding a Guild")
                .text("What you need: to have seen Lucifer fall, no place in another guild, 60 mana and one of the world's four guild "
                        + "places still free. Human, demon or angel, it makes no difference. Then go step by step.")
                .text("1. The banner. Dye a banner the colour your guild will wear: its base colour becomes the guild's colour, "
                        + "patterns are not kept. Rename it on an anvil with the guild's name, 3 to 24 letters, digits, spaces or the "
                        + "marks ' - . &. No two guilds may share a name.")
                .text("2. The circle. Set down a ritual altar and draw the small circle around it: eight chalk lines. Lay the "
                        + "offerings on the altar one at a time: two blocks of gold, two diamonds, salt, and a book for the charter, "
                        + "blank or written.")
                .recipe(ritual("found_guild"), "The founding rite")
                .text("3. The rite. Light it with the renamed banner itself. The banner burns into a Guild Standard that carries the "
                        + "name and the colour. The rite refuses a banner never named on an anvil, and a hunter who already serves "
                        + "a guild.")
                .text("4. The seat. Carry the standard into the Overworld, at least 300 blocks from the world's centre and 256 from "
                        + "any other seat (the Pit Castle, the Monastery, SucroCorp Tower or another guild's standard), and plant it. "
                        + "That founds the guild: you sit on its throne as its Lord, it takes its share of the front, and any post "
                        + "you held in the Pit or the Host is left behind.")
                .text("5. First orders. Open the Chain of Command tab: your guild has its own record there. Choose an emblem, invite "
                        + "your first hunters and decide how you stand with the world. A new guild starts hostile to the Pit and the "
                        + "Host, neutral to the other guilds, and hostile to SucroCorp for good."));

        out.add(entry("guild_standard", JournalChapter.CHAIN).order(22).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("The Standard")
                .text("Rename a vanilla banner on an anvil first: its name becomes the guild's name, 3–24 letters, digits, spaces or the marks ' - . &. After Lucifer's defeat, use that banner to activate the founding rite. It becomes a Guild Standard; planting it founds the guild.")
                .text("Plant it in the Overworld, at least 300 blocks from the origin and at least 256 blocks from any other seat or standard. This is a seat you choose yourself; see Player Guilds for membership and command.")
                .text("Enemies can break the standard. Without it the guild is homeless and its NPC officers do not appear. Raise it again within seven days: after that the guild loses its ground on the front. Replanting restores its seat.")
                .recipe(ritual("found_guild"), "The founding rite"));

        out.add(entry("guild_leading", JournalChapter.CHAIN).order(23).icon(AllItems.GUILD_STANDARD)
                .unlock(adv("devil_went_down"))
                .title("Leading a Guild")
                .text("Your guild's record in the Chain of Command tab has three pages: Members, Diplomacy and Chronicle. Only the "
                        + "Lord chooses the emblem and sets the guild's stances; the Lord and the Commanders invite, promote, demote "
                        + "and dismiss, and only they may plant a fallen standard again.")
                .text("Invite a hunter by name. They must be online, and the invitation stands for three days; they accept or "
                        + "decline from their own book and come in at the lowest rank with room. Joining leaves a post in the Pit or "
                        + "the Host behind: nobody serves two masters.")
                .text("The ranks keep the old chains' limits (ten Soldiers, six Captains, three Lieutenants, two Commanders, one "
                        + "Lord), but hired officers may hold only half of each: five, three, one and one. The throne is always a "
                        + "hunter's. A Commander may only raise or lower those below its own rank, and never up to it.")
                .text("Promotions are yours to give. The guild only promotes on its own when a hired officer's place has stood "
                        + "open for two days, and then it raises one of your hired officers from the rank below; it never hires "
                        + "anyone new. Dismiss a hired officer and it goes back to the wild; dismiss a hunter and they are out.")
                .text("Your hired officers roam out from the standard into your sectors of the front, and your Commanders keep "
                        + "watch beside the standard itself. While it is down, none of them shows itself.")
                .text("Wild demons and angels judge you by your guild's stance toward their side. Members of one guild, and of "
                        + "allied guilds, cannot hurt each other; between hostile guilds, blades are drawn as far as the server "
                        + "allows fighting between hunters."));

        out.add(entry("guild_recruiting", JournalChapter.CHAIN).order(24).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Recruiting")
                .text("Only the Lord or a Commander may hire for the guild. Pay a wild demon, angel, fallen angel or rival hunter and it enters at rank I. A demon asks 16 sulfur and 8 gold ingots; an angel, 4 holy oil and 8 gold ingots; a fallen angel, 2 fallen blood and 8 gold ingots; a hunter, 16 emeralds and 8 salt.")
                .text("An enemy officer of rank I–III may stake its service on a duel. Beat it and it deserts to your guild one rank lower, with rank I as the floor. Ask it to fight for its allegiance rather than simply killing it.")
                .text("The binding rite is the costly third road: write the name of an officer of any rank up to IV and bind it to your guild. It keeps its rank. Every method needs a vacant NPC place at the resulting rank; at most half of any rank may be NPCs. Read Guild Diplomacy before bringing old enemies under one standard.")
                .recipe(ritual("bind_officer"), "Binding a named officer")
                .text("How to hire: carry the price, crouch and use a wild one. It names its price, and you pay it or walk away. "
                        + "How to win a deserter: crouch and use an enemy officer to parley, and ask it to fight for its allegiance. "
                        + "Not every temper takes the offer; one that does meets you in a ring until one of you yields.")
                .text("How to bind: write the officer's name in a book, exactly as the Chain of Command tab spells it. Lay it on "
                        + "the altar of a small circle with two blocks of gold, two diamonds and an eye of ender, and light the rite "
                        + "with bound blood from the spell bowl. It costs 80 mana. A Lord, a Leviathan or an officer already in a "
                        + "duel cannot be bound."));

        out.add(entry("guild_diplomacy", JournalChapter.CHAIN).order(25).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Guild Diplomacy")
                .text("Your guild keeps its own stance toward every faction: hostile, neutral or allied. A new guild is hostile to the Pit and the Host, and neutral to other guilds. SucroCorp is always hostile; there is no bargain with the Board.")
                .text("Declare hostility on your own authority; peace requires both sides. Send and answer offers from the book. Allies never wage war against one another. Wild demons and angels judge a guild member by the stance of that guild, even when the hunter once served another side.")
                .text("The front is divided into 24 sectors of 15°. A guild can hold ground there like the older factions, but needs its standard to keep a home. See The Standard before leaving your seat undefended."));

        out.add(entry("guild_interregnum", JournalChapter.CHAIN).order(26).icon(AllItems.SEAL_OF_SUMMONS)
                .unlock(adv("devil_went_down"))
                .title("Guild Interregnum")
                .text("If the founder leaves or is away for thirty days, the throne stands empty. An interregnum lasts three days. The hunters of the guild may claim the throne from the book; hired NPCs cannot claim it.")
                .text("Rival claimants fight each other for the succession. The victor takes the throne and the guild continues under its new Lord. If nobody claims it, the guild dissolves. A planted standard cannot save a guild that has nobody willing to lead it."));

    }
}
