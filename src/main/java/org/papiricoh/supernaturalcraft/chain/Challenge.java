package org.papiricoh.supernaturalcraft.chain;

import java.util.Locale;

/**
 * A formal challenge to a duel (pure, mutable, owned by {@link ChainSavedData}). Between two officers of the two sides (a
 * skirmish, a champion's duel) it is settled at a later day's roll, or fought in the world if a hunter comes near
 * ({@code ClashStage}); feuds inside one chain keep their own court ({@link Feud}). One thrown at a hunter waits for them:
 * {@link #remaining} counts down only while they are online, and lapsing counts as refusing ({@link #lapsed}). One a hunter
 * throws (the challenge rite) is fought at once. A duel between two of one side ends when one yields
 * ({@link #YIELD_FRACTION}); one across the sides is to the death ({@link #lethal}).
 */
public final class Challenge {
    public enum Kind {
        /** One officer against another, of a side or across. */
        OFFICER_OFFICER,
        /** A hunter called an officer out with the rite. */
        HUNTER_OFFICER,
        /** An officer called a hunter out. */
        OFFICER_HUNTER,
        /** A hunter of the side, a Commander, for the Lord's throne. */
        THRONE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum State { PENDING, STAGED, DONE }

    /** A hunter has two days of play to answer. */
    public static final int DEADLINE_TICKS = 2 * 24000;
    /** In a duel that is not to the death the loser yields below this share of its health. */
    public static final float YIELD_FRACTION = 0.1f;

    public final int id;
    public final Kind kind;
    public final Bonds.Actor challenger, defender;
    public final boolean lethal;
    public final long issuedDay;
    public int remaining = DEADLINE_TICKS;
    /**
     * Why it was thrown, for the outcome: {@code "post"} (a duel for the defender's post), {@code "throne"}, {@code "grudge"},
     * {@code "rite"}, {@code "skirmish"} (two officers across the sides with their escorts), {@code "vow"} (a wronged Lord's
     * champion), {@code "order"} (a hunter Lord's order). Empty for none.
     */
    public String reason = "";
    public State state = State.PENDING;

    public Challenge(int id, Kind kind, Bonds.Actor challenger, Bonds.Actor defender, boolean lethal, long issuedDay) {
        this.id = id;
        this.kind = kind;
        this.challenger = challenger;
        this.defender = defender;
        this.lethal = lethal;
        this.issuedDay = issuedDay;
    }

    /** Ticks pass for the hunter it waits on (only while they are online). */
    public void tickOnline(int ticks) {
        if (state == State.PENDING) remaining = Math.max(0, remaining - ticks);
    }

    public boolean lapsed() {
        return state == State.PENDING && remaining <= 0;
    }

    /** Whether this challenge waits on (or was thrown by) that actor. */
    public boolean involves(Bonds.Actor a) {
        return challenger.equals(a) || defender.equals(a);
    }

    /** Lethal unless both sides are the same: two of one side fight until one yields. */
    public static boolean lethal(boolean sameSide) {
        return !sameSide;
    }
}
