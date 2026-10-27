package org.papiricoh.supernaturalcraft.chain.talk;

import java.util.Locale;

/**
 * The answers a hunter may give an officer (v0.27). The label is {@code chain.supernaturalcraft.talk.option.<id>}; the
 * officer's reply is a {@link org.papiricoh.supernaturalcraft.chain.narrative.Situation} line. Book messages
 * ({@link #messageable}) are the same choices sent from afar. Append only.
 */
public enum TalkOption {
    /** Same side: who it is feuding with, and where they were last seen. */
    RUMORS,
    /** Same side: swear a pact (neither challenges the other; it backs you in a duel). */
    PACT,
    /** Same side: it walks with you for a while. */
    AID,
    /** Same side, a superior: it speaks for you, to take you into the chain or a step up it. */
    SPONSOR,
    /** Same side, a lesser officer: it answers to you. */
    FOLLOW,
    /** The Lord: a task. */
    TASK,
    /** The Lord: a step up. */
    PROMOTION,
    /** The Lord, to a Commander: a duel for the throne. */
    THRONE,
    /** The Lord a hunter holds the throne as: an order to a subordinate (challenge a rival, take vengeance, swear a pact). */
    ORDER,
    /** The other side, crouching: a truce for a day, for a tribute. */
    TRUCE,
    /** The other side, a demon: coin for a rival's whereabouts. */
    BRIBE,
    /** The other side, a demon: it spares you if you kill a rival of its. */
    DEAL,
    /** Book only: a calculated insult. */
    INSULT,
    /** To a challenge: accept it. */
    ACCEPT,
    /** To a challenge: refuse it (it counts as cowardice). */
    DECLINE,
    /** Walk away. */
    LEAVE,
    /** v0.29, a guild's Lord or Commander to a wild one: pay its price and it joins the guild. */
    HIRE,
    /** v0.29, a guild's Lord or Commander crouching before an officer of another faction: a duel for its service. */
    RECRUIT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "chain.supernaturalcraft.talk.option." + id();
    }

    /** What a hunter may send from the book. */
    public boolean messageable() {
        return this == PACT || this == AID || this == SPONSOR || this == INSULT || this == ORDER;
    }
}
