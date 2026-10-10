package org.papiricoh.supernaturalcraft.crossroads;

import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

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

    // --- Wild bargains (v0.18): a box buried at a natural crossroads. Better wishes, worse price. ----------------------------

    /** A wild bargain's pack is bigger... */
    public static final int WILD_MIN_PACK = 5, WILD_MAX_PACK = 7;
    /** ...and has to be outlasted for longer (three minutes). */
    public static final int WILD_SURVIVE_TICKS = 3600;
    /** The demon raises a weapon no higher than this, whatever its bearer has beaten. */
    public static final int MAX_WILD_ASCENSION = 4;
    /** How many trophies the demon puts on the table at once. */
    public static final int TROPHY_OFFERS = 3;
    /**
     * The great enemies whose trophy (and shard) the demon can fetch, in a fixed order: a TROPHY wish's {@code arg} is an index
     * here, written on contracts and saved with deals, so new bosses are only ever appended.
     */
    public static final List<Boss> TROPHY_BOSSES = List.of(Boss.AZAZEL, Boss.LILITH, Boss.LUCIFER, Boss.GABRIEL, Boss.WAR, Boss.FAMINE,
            Boss.PESTILENCE, Boss.RAPHAEL, Boss.BROKEN_CHORUS, Boss.METATRON, Boss.NAOMI, Boss.ZACHARIAH, Boss.AMARA, Boss.DEATH,
            Boss.MICHAEL);

    /** What an UNCURSE wish lifts ({@code arg} = ordinal). */
    public enum Affliction {
        /** A cursed weapon's hunger: every hungry blade carried is sated. */
        HUNGER,
        /** Heaven's Mark. */
        HEAVENS_MARK,
        /** A collected soul's hollowness. */
        SOULLESS,
        /** A hex bag's bad luck. */
        JINXED;

        /** @return the affliction an UNCURSE {@code arg} names, or null */
        public static Affliction of(int arg) {
            return arg >= 0 && arg < values().length ? values()[arg] : null;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

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
        KNOWLEDGE(10, 1),
        /** "Make me one of you" (v0.13): a demon at once (no term, no hounds); the crossroads keeps two hearts until a cure. */
        CONVERT(0, 1),
        /** Wild only (v0.18): the weapon in your hand, one Ascension higher (up to {@link #ascendCap}). */
        ASCEND(8, 1, true),
        /** Wild only: the trophy of a great enemy you have beaten, and one of its shards ({@code arg}: {@link #TROPHY_BOSSES}). */
        TROPHY(10, TROPHY_BOSSES.size(), true),
        /** Wild only: your last dead pet, even with nothing left of it to bring back. */
        REVIVE(7, 1, true),
        /** Wild only: a curse lifted ({@code arg}: {@link Affliction}). */
        UNCURSE(6, Affliction.values().length, true);

        public final int days;
        /** How many variants ({@code arg} values) the wish has. */
        public final int variants;
        /** Only a demon called at a natural crossroads grants it. */
        public final boolean wildOnly;

        Wish(int days, int variants) {
            this(days, variants, false);
        }

        Wish(int days, int variants, boolean wildOnly) {
            this.days = days;
            this.variants = variants;
            this.wildOnly = wildOnly;
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

        /** Settled the moment it is sealed: nothing falls due. */
        public boolean settledAtOnce() {
            return days == 0;
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

    /** When a debt of {@code days} days falls due (game time). */
    public static long dueAt(long sealedAt, int days) {
        return sealedAt + days * DAY;
    }

    /**
     * The term of a wish, in days: a wild bargain leaves only {@code wildFactor} of it (rounded up, at least a day); a wish
     * settled at once stays settled at once.
     */
    public static int daysFor(Wish wish, boolean wild, double wildFactor) {
        if (!wild || wish.days == 0) return wish.days;
        return Math.max(1, (int) Math.ceil(wish.days * wildFactor - 1e-9));
    }

    /** {@link #daysFor(Wish, boolean, double)} plus {@code extraDays} of grace (a memory set's gift); none for a wish settled at once. */
    public static int daysFor(Wish wish, boolean wild, double wildFactor, int extraDays) {
        int days = daysFor(wish, wild, wildFactor);
        return days == 0 ? 0 : days + Math.max(0, extraDays);
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

    /** A wild bargain's pack: {@link #WILD_MIN_PACK}..{@link #WILD_MAX_PACK}; otherwise {@link #packSize(int)}. */
    public static int packSize(int roll, boolean wild) {
        return wild ? WILD_MIN_PACK + Math.floorMod(roll, WILD_MAX_PACK - WILD_MIN_PACK + 1) : packSize(roll);
    }

    public static boolean survived(long now, long huntStartedAt) {
        return survived(now, huntStartedAt, false);
    }

    /** How long the hounds must be outlasted. */
    public static int surviveTicks(boolean wild) {
        return wild ? WILD_SURVIVE_TICKS : SURVIVE_TICKS;
    }

    public static boolean survived(long now, long huntStartedAt, boolean wild) {
        return now - huntStartedAt >= surviveTicks(wild);
    }

    /** "Bind my soul" comes ticked on a wild contract (it can still be struck out). */
    public static boolean soulClausePreTicked(boolean wild) {
        return wild;
    }

    // --- Wild wishes ------------------------------------------------------------------------------------------------------

    /** The highest Ascension the demon raises a weapon to: the weapon's own limit, its bearer's tier, {@link #MAX_WILD_ASCENSION}. */
    public static int ascendCap(int playerTier, int maxLevel) {
        return Math.min(MAX_WILD_ASCENSION, Math.min(playerTier, maxLevel));
    }

    public static boolean canAscend(int level, int playerTier, int maxLevel) {
        return level < ascendCap(playerTier, maxLevel);
    }

    /**
     * The trophies on offer: of the great enemies {@code beaten}, the {@link #TROPHY_OFFERS} furthest along the road.
     *
     * @return TROPHY wish args (indices into {@link #TROPHY_BOSSES}), furthest first
     */
    public static List<Integer> trophyOffer(Predicate<Boss> beaten) {
        List<Boss> won = new ArrayList<>();
        for (Boss b : TROPHY_BOSSES) if (beaten.test(b)) won.add(b);
        won.sort(Comparator.comparingInt(Boss::ordinal).reversed());
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < Math.min(TROPHY_OFFERS, won.size()); i++) out.add(TROPHY_BOSSES.indexOf(won.get(i)));
        return out;
    }

    /** @return the boss a TROPHY {@code arg} names, or null */
    public static Boss trophyBoss(int arg) {
        return arg >= 0 && arg < TROPHY_BOSSES.size() ? TROPHY_BOSSES.get(arg) : null;
    }

    /**
     * Whether a box buried now is answered: only at night, and once a night per hunter ({@code lastWildNight} is the
     * {@link #nightIndex} of the night they last buried one).
     */
    public static boolean wildAnswers(boolean night, long dayTime, long lastWildNight) {
        return night && nightIndex(dayTime) != lastWildNight;
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
