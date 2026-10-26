package org.papiricoh.supernaturalcraft.chain.narrative;

import java.util.Locale;

/**
 * What an officer is speaking about (pure). Each one has lines per voice ({@link Voice}) and temperament in the lang files,
 * counted in {@link VoiceLines}. Arguments every line may use: {@code %1$s} the hunter spoken to (or the target), {@code %2$s}
 * another officer (a rival, the foe, the Lord), {@code %3$s} a detail (a region, a rank, a task); unused ones are passed empty.
 * Append only: the ids are lang keys.
 */
public enum Situation {
    // In the world, said aloud to everyone near.
    /** Seeing a hunter of the other side for the first time. */
    SIGHT_FOE,
    /** Seeing again a hunter it has met (%1$s), maybe one it killed. */
    SIGHT_AGAIN,
    /** Seeing a hunter of its own side. */
    SIGHT_ALLY,
    /** Over a hunter it has just killed. */
    KILL_HUNTER,
    /** Fleeing, badly hurt. */
    FLEE,
    /** Back from a death that was not done properly, with a new scar. */
    RETURN_SCARRED,
    /** Throwing down a challenge to %1$s (a hunter) or %2$s (an officer). */
    CHALLENGE_ISSUED,
    /** As a duel begins. */
    DUEL_START,
    /** Over the beaten foe of a duel. */
    DUEL_WON,
    /** Dying in a duel it lost. */
    DUEL_LOST,
    /** Yielding a duel between two of one side. */
    YIELD,
    /** Taunting its rival %2$s in a feud. */
    FEUD_TAUNT,
    /** A war cry in a skirmish against the other side. */
    SKIRMISH_CRY,
    /** The Lord swearing vengeance on %2$s and naming a champion. */
    VOW,
    // In a conversation (its own screen), said to one hunter.
    /** An officer of the hunter's own side greets them. */
    GREET_ALLY,
    /** An officer of the other side hears a crouching hunter out. */
    GREET_PARLEY,
    /** An officer of the other side will not hear a hunter out. */
    PARLEY_REFUSE,
    /** Gossip: its rival %2$s, last seen %3$s. */
    RUMOR,
    /** No rival worth naming. */
    RUMOR_NONE,
    PACT_YES,
    PACT_NO,
    AID_YES,
    AID_NO,
    SPONSOR_YES,
    SPONSOR_NO,
    /** It agrees to answer to the hunter. */
    FOLLOW_YES,
    FOLLOW_NO,
    /** The Lord receives a hunter of its side in the throne room. */
    AUDIENCE_GREET,
    /** The Lord gives a task (%3$s describes it). */
    TASK_GIVEN,
    /** The Lord is still waiting on the task already given. */
    TASK_PENDING,
    /** The Lord rewards a task done. */
    TASK_DONE,
    PROMOTION_YES,
    PROMOTION_NO,
    /** The Lord accepts a challenge for the throne. */
    THRONE_ACCEPT,
    TRUCE_YES,
    TRUCE_NO,
    /** Bribed, it gives up %2$s, last seen %3$s. */
    BRIBE_YES,
    BRIBE_NO,
    /** A crossroads-style deal: it spares the hunter if they kill %2$s. */
    DEAL_YES,
    DEAL_NO,
    /** A reply to a message sent from the book, delivered at the next dawn. */
    MESSAGE_ACCEPTED,
    MESSAGE_REFUSED,
    /** A reply to an insult sent from the book. */
    MESSAGE_INSULTED,
    /** Mocking a hunter who refused or let a challenge lapse. */
    COWARD;

    /** Translation-key segment: {@code sight_foe}, … */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
