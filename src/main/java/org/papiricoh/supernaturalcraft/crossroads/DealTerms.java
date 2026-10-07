package org.papiricoh.supernaturalcraft.crossroads;

import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State;

import java.util.Locale;

/**
 * The fine print of a crossroads deal, as pure rules (no Minecraft types, tested in JUnit): what
 * each wish costs in days, when the debt falls due, how the deal moves between its states and
 * what the hunt looks like.
 */
public final class DealTerms {

    /** One Minecraft day in ticks. */
    public static final long DAY = 24000;
    /** Extra hearts (not health points) one upgrade grants. */
    public static final int HEARTS_PER_DEAL = 2;
    /** Extra max mana one upgrade grants. */
    public static final int MANA_PER_DEAL = 25;
    /** The demon will not sell more than this many extra hearts, over all deals. */
    public static final int MAX_BONUS_HEARTS = 10;
    /** Nor more than this much extra mana. */
    public static final int MAX_BONUS_MANA = 100;
    /** Surviving the hounds this long settles the debt. */
    public static final int SURVIVE_TICKS = 2400;
    /** A debtor who was away when the debt fell due gets this long after logging in. */
    public static final int LOGIN_GRACE = 200;
    /** How long a collected soul stays hollow (three in-game days). */
    public static final int SOULLESS_TICKS = 72000;
    /** How long a summoned demon waits for its summoner to make up their mind. */
    public static final int NEUTRAL_LIFETIME = 2400;
    /** Pack size bounds. */
    public static final int MIN_PACK = 3, MAX_PACK = 5;
    /** How often, on the last day, the debtor hears the hounds (ticks). */
    public static final int OMEN_INTERVAL = 600;

    private DealTerms() {
    }

    /** What can be wished for, and the days it buys before the hounds come. */
    public enum Wish {
        /** arg 0: +2 hearts, arg 1: +25 max mana. Forever. */
        UPGRADE(5, 2),
        /** arg 0: the things left where you last died; arg 1: a dead pet (when a hook offers one). */
        RECOVER(7, 2),
        /** Something rare from the demon's pockets. */
        RARE_ITEM(10, 1),
        /** Maps to what is hidden nearby, or the weakness of the next great enemy. */
        KNOWLEDGE(10, 1);

        public final int days;
        /** How many variants ({@code arg} values) the wish has. */
        public final int variants;

        Wish(int days, int variants) {
            this.days = days;
            this.variants = variants;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** @return the wish called {@code id}, or null */
        public static Wish byId(String id) {
            for (Wish w : values()) if (w.id().equals(id)) return w;
            return null;
        }

        /** The contract's wording: {@code "upgrade.0"}. */
        public String clause(int arg) {
            return id() + "." + arg;
        }

        /** Translation key of a wish variant's name ({@code + ".desc"} for its description). */
        public String key(int arg) {
            return keyOf(clause(arg));
        }

        public boolean validArg(int arg) {
            return arg >= 0 && arg < variants;
        }
    }

    /** Translation key for a clause as written on a contract ({@code "upgrade.0"}). */
    public static String keyOf(String clause) {
        return "deal.supernaturalcraft.wish." + clause;
    }

    /** When the debt falls due (game time). */
    public static long dueAt(long sealedAt, Wish wish) {
        return sealedAt + wish.days * DAY;
    }

    public static boolean due(long now, long dueAt) {
        return now >= dueAt;
    }

    /** The last day before the debt is due: the howling starts. */
    public static boolean lastDay(long now, long dueAt) {
        return now < dueAt && dueAt - now <= DAY;
    }

    /** Whole days left, rounded up (0 once due). */
    public static int daysLeft(long now, long dueAt) {
        if (now >= dueAt) return 0;
        return (int) ((dueAt - now + DAY - 1) / DAY);
    }

    /** Hours left on the last day (rounded up, game hours of 1000 ticks). */
    public static int hoursLeft(long now, long dueAt) {
        if (now >= dueAt) return 0;
        return (int) ((dueAt - now + 999) / 1000);
    }

    /** A pack of {@link #MIN_PACK}..{@link #MAX_PACK} from any random int. */
    public static int packSize(int roll) {
        return MIN_PACK + Math.floorMod(roll, MAX_PACK - MIN_PACK + 1);
    }

    public static boolean survived(long now, long huntStartedAt) {
        return now - huntStartedAt >= SURVIVE_TICKS;
    }

    /** Whether one more upgrade of this kind stays within the demon's limits. */
    public static boolean canUpgrade(int bonusHearts, int bonusMana, int arg) {
        return arg == 0 ? bonusHearts + HEARTS_PER_DEAL <= MAX_BONUS_HEARTS : bonusMana + MANA_PER_DEAL <= MAX_BONUS_MANA;
    }

    /**
     * Which night a day time belongs to: constant from noon to the next noon, so everything
     * between one dusk and the following dawn shares an index.
     */
    public static long nightIndex(long dayTime) {
        return Math.floorDiv(dayTime - DAY / 2, DAY);
    }

    /** A hunted demon that fled its vessel comes back on a later night. */
    public static boolean demonReturns(boolean night, long dayTimeNow, long dayTimeGone) {
        return night && nightIndex(dayTimeNow) > nightIndex(dayTimeGone);
    }

    /** What dying while the hounds are out costs. */
    public enum Penalty { LOSE_UPGRADE, SOULLESS }

    public static Penalty penalty(Wish wish) {
        return wish == Wish.UPGRADE ? Penalty.LOSE_UPGRADE : Penalty.SOULLESS;
    }

    /** What happens to a deal. */
    public enum Event {
        SEAL, DUE, HOUNDS_SLAIN, SURVIVED, BREAK, DEMON_SLAIN, DIED
    }

    public static boolean active(State s) {
        return s == State.OPEN || s == State.COLLECTING || s == State.HUNTED;
    }

    /**
     * The state machine. Events that make no sense in a state leave it as it is.
     *
     * @param beforeDue whether the event happens before the debt is due (killing the demon only
     *                  frees you in time)
     */
    public static State next(State s, Event e, boolean beforeDue) {
        return switch (e) {
            case SEAL -> active(s) ? s : State.OPEN;
            case DUE -> s == State.OPEN || s == State.HUNTED ? State.COLLECTING : s;
            case HOUNDS_SLAIN, SURVIVED -> s == State.COLLECTING ? State.FREE : s;
            case BREAK -> s == State.OPEN && beforeDue ? State.HUNTED : s;
            case DEMON_SLAIN -> s == State.HUNTED && beforeDue ? State.FREE : s;
            case DIED -> s == State.COLLECTING ? State.COLLECTED : s;
        };
    }
}
