package org.papiricoh.supernaturalcraft.datagen.chain;

import java.util.function.BiConsumer;

/** English strings of the Chain of Command (v0.27): the officer's record, the bonds, the front, the talk screen and duels. */
public final class ChainGuiLang {
    private ChainGuiLang() {}

    private static final String K = "screen.supernaturalcraft.book.chain.";
    private static final String S = "screen.supernaturalcraft.chain.";

    public static void add(BiConsumer<String, String> add) {
        // The record.
        add.accept(K + "close", "Close");
        add.accept(K + "open", "Open the record of %s");
        add.accept(K + "you", "You");
        add.accept(K + "hunter_post", "A hunter in the chain");
        add.accept(K + "hunter_post.desc", "%s holds this post: a hunter sworn into the chain, answering to it and answered by it.");
        add.accept(K + "regard", "Its regard for you");
        add.accept(K + "regard.value", "Regard: %s of 100");
        add.accept(K + "regard.met", "Has dealt with you %s times");
        add.accept(K + "regard.unmet", "Never met");
        add.accept(K + "regard.warm", "Warm");
        add.accept(K + "regard.cold", "Cold");
        add.accept(K + "regard.indifferent", "Indifferent");
        add.accept(K + "tab.chronicle", "Chronicle");
        add.accept(K + "tab.bonds", "Bonds");
        add.accept(K + "tab.duels", "Duels");
        add.accept(K + "tab.actions", "Message");
        add.accept(K + "chronicle.empty", "Nothing about this officer has reached the chronicle yet.");
        // Bonds.
        add.accept(K + "bonds.show", "Show the bonds");
        add.accept(K + "bonds.hide", "Hide the bonds");
        add.accept(K + "bonds.none", "It holds no strong feelings, and none are held for it.");
        add.accept(K + "bonds.of", "What %s thinks of others");
        add.accept(K + "bonds.to", "What others think of %s");
        add.accept(K + "bond.mild", "Passing regard");
        add.accept(K + "bond.regard", "Regard: %s");
        add.accept(K + "bond.toward_you", "Towards you: %s");
        // Duels.
        add.accept(K + "duels.open", "Open challenges");
        add.accept(K + "duels.none", "No challenge stands open.");
        add.accept(K + "duels.vs", "%s challenges %s");
        add.accept(K + "duels.vs_you", "%s challenges you");
        add.accept(K + "duels.you_vs", "You challenge %s");
        add.accept(K + "duels.kind.officer_officer", "Between officers");
        add.accept(K + "duels.kind.hunter_officer", "A hunter's rite");
        add.accept(K + "duels.kind.officer_hunter", "Against a hunter");
        add.accept(K + "duels.kind.throne", "For the throne");
        add.accept(K + "duels.lethal", "to the death");
        add.accept(K + "duels.yield", "until one yields");
        add.accept(K + "duels.state.pending", "awaiting an answer");
        add.accept(K + "duels.state.staged", "being fought");
        add.accept(K + "duels.state.done", "settled");
        add.accept(K + "duels.answer", "You have %s left to answer.");
        add.accept(K + "duels.past", "Duels fought");
        add.accept(K + "duels.past.none", "It has fought no duel worth recording.");
        add.accept(K + "time.minutes", "about %s minutes of play");
        add.accept(K + "time.hours", "about %s hours of play");
        // Messages.
        add.accept(K + "actions.intro", "Send it word through the book. Its answer comes at dawn.");
        add.accept(K + "actions.sent", "Sent: %s. It will answer at dawn.");
        add.accept(K + "actions.yours", "This is your own post.");
        add.accept(K + "actions.hunter", "%s is a hunter: seek them out yourself.");
        add.accept(K + "actions.unreachable", "It cannot be reached while it is gone from the chain.");
        add.accept(K + "actions.human", "You serve neither side: it will hear no pact, plea or sponsorship from you.");
        add.accept(K + "actions.foe", "You serve the other side: it will hear no pact, plea or sponsorship from you.");
        add.accept(K + "actions.tip.pact", "Neither of you will challenge the other, and it will stand by you in a duel.");
        add.accept(K + "actions.tip.aid", "Ask it to walk at your side for a while.");
        add.accept(K + "actions.tip.sponsor", "Ask it to speak for you, to take you into the chain or a step up it.");
        add.accept(K + "actions.tip.insult", "A calculated insult. It will not forget it.");
        add.accept(K + "actions.tip.order", "As its Lord, set it on a rival, or declare war.");
        add.accept(K + "actions.order.pick", "Set %s on:");
        add.accept(K + "actions.order.tip", "Order %s to go after %s");
        add.accept(K + "actions.order.sent", "an order against %s");
        add.accept(K + "actions.order.none", "It has no rival to set on.");
        add.accept(K + "actions.order.war", "Declare war on the other side");
        add.accept(K + "actions.order.war.tip", "The chains go to war: skirmishes everywhere along the front.");
        add.accept(K + "actions.rite.title", "To call it out");
        add.accept(K + "actions.rite", "Write %1$s in a book and lay it on a ritual altar with a %2$s for a demon or a fallen angel, or a %3$s for an angel. It must come and answer.");
        add.accept(K + "item.gauntlet", "Gauntlet of Ash");
        add.accept(K + "item.seal", "Seal of Summons");
        // The front.
        add.accept(K + "front", "The front");
        add.accept(K + "war.until", "At war — ends on day %s");
        add.accept(K + "war.peace", "At peace");
        add.accept(K + "war.peace.desc", "The sides watch each other along the front; skirmishes still break out.");
        add.accept(K + "war.n", "N");
        add.accept(K + "war.e", "E");
        add.accept(K + "war.s", "S");
        add.accept(K + "war.w", "W");
        add.accept(K + "war.seat.demon", "The Pit's stronghold");
        add.accept(K + "war.seat.angel", "The Host's stronghold");
        add.accept(K + "war.seat.unbuilt", "Not yet raised");
        add.accept(K + "war.you", "You are here");
        add.accept(K + "war.held", "Held by %s");
        add.accept(K + "war.push", "%s pushing: %s of %s");
        add.accept(K + "war.sectors", "%s %s");
        add.accept(K + "war.score.title", "The war");
        add.accept(K + "war.score", "%s %s — %s %s");
        add.accept(K + "war.prestige", "Prestige: %s %s — %s %s");
        add.accept(K + "war.task", "Your task");
        add.accept(K + "war.task.from", "Set by the throne of %s.");
        add.accept(K + "war.challenges", "Challenges awaiting you");
        add.accept(K + "war.challenge", "%s has called you out, %s.");
        add.accept(K + "war.news", "From the front");
        add.accept(K + "war.news.none", "No news from the front.");
        // v0.32: Nations & War's dominions in place of the ring.
        add.accept(K + "dominions", "Dominions");
        add.accept(K + "dominions.none", "No power holds a town yet.");
        add.accept(K + "dominion.seat", "Seat of %s");
        add.accept(K + "dominion.enclave", "Enclave of %s");
        add.accept(K + "dominion.at", "%s, %s");
        add.accept(K + "dominion.contested", "contested");
        add.accept(K + "dominion.contested.tip", "Contested: an enemy is fighting for it.");
        add.accept(K + "realm.state", "Wars under way: %s");
        add.accept(K + "realm.wars", "Wars of the nations");
        add.accept(K + "realm.wars.none", "No chain is at war with a nation.");
        add.accept(K + "realm.war", "%s against %s");
        add.accept(K + "realm.war.state", "%s · %s · %s – %s");
        add.accept(K + "realm.kind.powers", "War of the powers");
        add.accept(K + "realm.kind.powers.tip", "Heaven and Hell at war, nation against nation.");
        add.accept(K + "realm.kind.incursion", "Incursion");
        add.accept(K + "realm.kind.incursion.tip", "A power marches on a hostile guild's nation.");
        add.accept(K + "realm.kind.assault", "Assault");
        add.accept(K + "realm.kind.assault.tip", "A guild's nation storms a power's seat.");
        add.accept(K + "realm.kind.guilds", "War of the guilds");
        add.accept(K + "realm.kind.guilds.tip", "Two guilds' nations at war.");
        add.accept(K + "realm.phase.preparation", "preparing");
        add.accept(K + "realm.phase.active", "under way");
        add.accept(K + "realm.prestige", "Prestige");
        // The talk screen and duels.
        add.accept(S + "talk", "Speaking with an officer");
        add.accept(S + "talk.close", "Click to leave");
        add.accept(S + "duel.lethal", "To the death");
        add.accept(S + "duel.yield", "Until one yields");
        add.accept(S + "duel.won", "%s stands victorious");
        add.accept(S + "duel.over", "The duel is over");
    }
}
