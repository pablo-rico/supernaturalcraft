package org.papiricoh.supernaturalcraft.datagen.chain;

import java.util.function.BiConsumer;

/**
 * English strings of the guilds in the Hunter's Book and the HUD (v0.29): the guild buttons, the guild's record (members,
 * diplomacy, chronicle), invitations, the guilds on the front and the war table, and a stranger's voice in the talk screen.
 */
public final class GuildGuiLang {
    private GuildGuiLang() {}

    private static final String K = "screen.supernaturalcraft.book.chain.";
    private static final String G = K + "guild.";

    public static void add(BiConsumer<String, String> add) {
        // The chain page: buttons, the plate at its foot, invitations.
        add.accept(G + "founder", "Founded by %s");
        add.accept(G + "members", "%s members");
        add.accept(G + "record", "The guild's record");
        add.accept(G + "mine", "My guild: %s");
        add.accept(G + "open", "Open the record of %s");
        add.accept(G + "serves", "Serves %s");
        add.accept(G + "invited", "Invited into %s");
        add.accept(G + "invited.until", "The invitation stands until day %s");
        add.accept(G + "accept", "Accept");
        add.accept(G + "decline", "Decline");
        add.accept(G + "standard.marker", "The standard of %s");
        // The record: header and left column.
        add.accept(G + "emblem.tip", "The guild's emblem");
        add.accept(G + "founded", "Founded on day %s");
        add.accept(G + "count", "%s hunters, %s hired");
        add.accept(G + "your_post", "You serve as %s");
        add.accept(G + "emblem", "Emblem");
        add.accept(G + "emblem.pick", "Take this emblem");
        add.accept(G + "emblem.current", "The guild's emblem");
        add.accept(G + "emblem.sent", "The emblem is being repainted.");
        add.accept(G + "standard", "Its standard stands at %s, %s, %s");
        add.accept(G + "standard.down", "Its standard has been down since day %s.");
        add.accept(G + "homeless", "Without it, its ground is shared out on day %s.");
        add.accept(G + "homeless.now", "Homeless: its ground is being shared out.");
        add.accept(G + "interregnum", "The throne stands empty until day %s.");
        add.accept(G + "proclaim", "Claim the throne");
        add.accept(G + "proclaim.tip", "Put yourself forward: rival claimants settle it in duels before the interregnum ends.");
        add.accept(G + "proclaim.sent", "You have claimed the throne.");
        add.accept(G + "leave", "Leave the guild");
        add.accept(G + "leave.confirm", "Click again to leave");
        add.accept(G + "leave.tip", "You give up your post and your place in the guild.");
        add.accept(G + "leave.tip.lord", "You lead it: if you leave, its throne stands empty and an interregnum begins.");
        add.accept(G + "leave.sent", "You have left the guild.");
        // Tabs.
        add.accept(G + "tab.members", "Members");
        add.accept(G + "tab.diplomacy", "Diplomacy");
        add.accept(G + "tab.chronicle", "Chronicle");
        // Members.
        add.accept(G + "invite", "Invite a hunter");
        add.accept(G + "invite.hint", "Their name…");
        add.accept(G + "invite.send", "Invite");
        add.accept(G + "invite.tip", "The hunter must be online; the invitation stands for three days.");
        add.accept(G + "invite.sent", "Invitation sent to %s.");
        add.accept(G + "rank", "%s · %s/%s · hired %s/%s");
        add.accept(G + "rank.tip", "At most %s of this rank may be hired officers; the rest are for hunters.");
        add.accept(G + "rank.lord.tip", "The throne is always a hunter's.");
        add.accept(G + "rank.empty", "No one holds this rank.");
        add.accept(G + "you", "%s (you)");
        add.accept(G + "hunter", "A hunter of the guild");
        add.accept(G + "hired", "Hired: %s");
        add.accept(G + "promote", "Promote to %s");
        add.accept(G + "promote.full", "No room as %s");
        add.accept(G + "promote.sent", "%s is raised to %s.");
        add.accept(G + "demote", "Demote to %s");
        add.accept(G + "demote.full", "No room as %s");
        add.accept(G + "demote.sent", "%s is lowered to %s.");
        add.accept(G + "dismiss.hunter", "Put %s out of the guild");
        add.accept(G + "dismiss.hired", "Let %s go, back to the wild");
        add.accept(G + "dismiss.confirm", "Click again to dismiss %s");
        add.accept(G + "dismiss.sent", "%s is dismissed.");
        // Diplomacy.
        add.accept(G + "diplomacy.intro", "How %s stands with the rest of the world.");
        add.accept(G + "diplomacy.lead", "How %s stands with the rest of the world. As its Lord, you may declare hostility alone; neutrality and alliance need both sides.");
        add.accept(G + "diplomacy.none", "No other faction stands yet.");
        add.accept(G + "stance.hostile", "Hostile");
        add.accept(G + "stance.neutral", "Neutral");
        add.accept(G + "stance.allied", "Allied");
        add.accept(G + "stance.line", "%s: %s");
        add.accept(G + "stance.fixed.board", "SucroCorp makes no peace with anyone.");
        add.accept(G + "stance.fixed.world", "This stance cannot change.");
        add.accept(G + "offer", "Offer:");
        add.accept(G + "offer.in", "They offer: %s");
        add.accept(G + "offer.out", "You offered: %s; awaiting their answer.");
        add.accept(G + "offer.tip", "Offer %s to %s: it holds once they accept.");
        add.accept(G + "offer.already", "You have already offered %s to %s.");
        add.accept(G + "offer.lord", "Their Lord accepts only if they hold the guild's founder loyal.");
        add.accept(G + "offer.sent", "You offered %s to %s.");
        add.accept(G + "accept.tip", "Accept: the stance becomes %s.");
        add.accept(G + "accept.sent", "You accepted the offer of %s.");
        add.accept(G + "refuse", "Refuse");
        add.accept(G + "refuse.sent", "You turned down the offer of %s.");
        add.accept(G + "declare.tip", "Turn hostile on %s, at once and alone; every offer between you lapses.");
        add.accept(G + "declare.sent", "You declared hostility on %s.");
        add.accept(G + "chronicle.empty", "Nothing about this guild has reached the chronicle yet.");
        // v0.32: a guild consecrated as a Nations & War nation.
        add.accept(G + "nation", "Consecrated as %s");
        add.accept(G + "nation.citizens", "Citizens: %s");
        add.accept(G + "oath", "Swear the Oath");
        add.accept(G + "oath.tip", "You are a citizen of %s: swear the oath to join its guild.");
        add.accept(G + "oath.sent", "You swore the oath to %s.");
        add.accept(G + "stance.nations", "Set by the nations");
        add.accept(G + "stance.nations.tip", "Both guilds are nations: their stance follows Nations & War. Ally or go to war there.");
        // The war table and the talk screen.
        add.accept("gui.supernaturalcraft.war_table.standard", "Standard of %s");
        add.accept("screen.supernaturalcraft.chain.talk.stranger", "A stranger");
    }
}
