package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The numbers and checks of the guilds hunters found (pure, v0.29). A guild keeps the Pit's rank caps, but only half of each
 * rank may be held by officers of flesh ({@link #npcCap}): the rest is for hunters. Its founder (or a Commander, below
 * their own rank) promotes by hand; a conclave only fills a gap left open for {@link #GRACE_DAYS}, and never recruits.
 */
public final class GuildRules {
    /** Days an NPC post must stand open before a conclave fills it by promotion (the founder had their chance). */
    public static final int GRACE_DAYS = 2;
    /** Days an invitation stands. */
    public static final int INVITE_DAYS = 3;
    /** World days a founder may be away before their guild falls into an interregnum. */
    public static final int INACTIVE_DAYS = 30;
    /** Length of a guild's interregnum. */
    public static final int INTERREGNUM_DAYS = 3;
    /** Days a guild may go without its standard before its ground is shared out. */
    public static final int HOMELESS_DAYS = 7;
    /** A guild's name: letters, digits, spaces and a little punctuation, {@link #NAME_MIN}–{@link #NAME_MAX} characters. */
    public static final int NAME_MIN = 3, NAME_MAX = 24;
    /** How many emblems there are to choose from, and dyes (the vanilla sixteen). */
    public static final int EMBLEMS = 12, DYES = 16;
    /** Where a standard may stand: this far from the world's origin, and from any other seat or standard. */
    public static final int MIN_FROM_ORIGIN = 300, MIN_FROM_SEAT = 256;
    /** The regard the Pit's or the Host's Lord must hold for a guild's founder to accept its peace. */
    public static final int PEACE_REGARD = Bonds.LOYAL;
    /** The highest rank a beaten officer may desert from, and the highest a rite may bind. */
    public static final ChainRank DESERT_MAX = ChainRank.LIEUTENANT, BIND_MAX = ChainRank.COMMANDER;

    private GuildRules() {
    }

    /** How many officers of flesh a guild keeps of a rank: half the cap (at least one below the throne, which is a hunter's). */
    public static int npcCap(ChainRank rank) {
        return rank == ChainRank.LORD ? 0 : Math.max(1, rank.cap() / 2);
    }

    /** The trimmed name if it may be a guild's, else null. */
    public static String validName(String raw) {
        if (raw == null) return null;
        String name = raw.strip().replaceAll("\\s+", " ");
        if (name.length() < NAME_MIN || name.length() > NAME_MAX) return null;
        for (char c : name.toCharArray())
            if (!Character.isLetterOrDigit(c) && " '-.&".indexOf(c) < 0) return null;
        return name;
    }

    /** Whether two names would read the same (case and spacing aside). */
    public static boolean sameName(String a, String b) {
        return a != null && b != null && a.strip().toLowerCase(Locale.ROOT).equals(b.strip().toLowerCase(Locale.ROOT));
    }

    /** Why a standard cannot stand there. */
    public enum Refusal {
        DIMENSION,
        TOO_CENTRAL,
        TOO_CLOSE
    }

    /** Why a standard may not be planted at ({@code x}, {@code z}) of the overworld, or null if it may. */
    public static Refusal plant(boolean overworld, int x, int z, List<SeatSite.Site> others) {
        return plant(overworld, x, z, others, true);
    }

    /** As {@link #plant(boolean, int, int, List)}; {@code ring}: the sectors' ring stands, so the centre is kept clear. */
    public static Refusal plant(boolean overworld, int x, int z, List<SeatSite.Site> others, boolean ring) {
        if (!overworld) return Refusal.DIMENSION;
        if (ring && Math.hypot(x, z) < MIN_FROM_ORIGIN) return Refusal.TOO_CENTRAL;
        for (SeatSite.Site s : others) if (Math.hypot(s.x() - x, s.z() - z) < MIN_FROM_SEAT) return Refusal.TOO_CLOSE;
        return null;
    }

    /** One item of a price ({@code item}: a registry id). */
    public record Cost(String item, int count) {}

    /** What a wild one of {@code kind} ({@link Kind}) asks to join. */
    public static List<Cost> price(Kind kind) {
        return switch (kind) {
            case DEMON -> List.of(new Cost("supernaturalcraft:sulfur", 16), new Cost("minecraft:gold_ingot", 8));
            case ANGEL -> List.of(new Cost("supernaturalcraft:holy_oil", 4), new Cost("minecraft:gold_ingot", 8));
            case FALLEN -> List.of(new Cost("supernaturalcraft:fallen_blood", 2), new Cost("minecraft:gold_ingot", 8));
            case HUNTER -> List.of(new Cost("minecraft:emerald", 16), new Cost("supernaturalcraft:salt", 8));
        };
    }

    /** What kind of recruit a mob makes (how it is paid, and how it speaks). */
    public enum Kind {
        DEMON,
        ANGEL,
        FALLEN,
        HUNTER
    }

    /** Whether an officer may be won over at all: never a Lord, a held post, a boss or one of SucroCorp. */
    public static boolean recruitable(Officer o) {
        return o != null && o.alive() && !o.held() && o.rank != ChainRank.LORD && o.faction != ChainFaction.LEVIATHAN;
    }

    /** The rank a beaten officer deserts at (one lower), or null if it is too high to desert. */
    public static ChainRank deserterRank(Officer o) {
        return recruitable(o) && o.rank.ordinal() <= DESERT_MAX.ordinal() ? o.rank.down() : null;
    }

    /** Whether a rite may bind this officer (it keeps its rank). */
    public static boolean bindable(Officer o) {
        return recruitable(o) && o.rank.ordinal() <= BIND_MAX.ordinal();
    }

    /** Whether {@code by} (a member's post) may move {@code target} to {@code rank}: the Lord anyone, a Commander those below. */
    public static boolean mayRank(Officer by, Officer target, ChainRank rank) {
        if (by == null || target == null || by == target || rank == ChainRank.LORD) return false;
        if (by.rank == ChainRank.LORD) return true;
        return by.rank == ChainRank.COMMANDER && target.rank.ordinal() < by.rank.ordinal() && rank.ordinal() < by.rank.ordinal();
    }

    /**
     * The NPC posts a conclave may fill today: each rank below the throne with room for an officer of flesh, once it has
     * stood open {@link #GRACE_DAYS}. Keeps {@code openSince} (rank → the day the gap opened) up to date.
     */
    public static Set<ChainRank> ripe(Hierarchy h, Map<ChainRank, Long> openSince, long day) {
        Set<ChainRank> out = EnumSet.noneOf(ChainRank.class);
        for (ChainRank rank : ChainRank.values()) {
            if (rank == ChainRank.LORD || !h.npcFree(rank)) {
                openSince.remove(rank);
                continue;
            }
            long since = openSince.computeIfAbsent(rank, r -> day);
            if (day - since >= GRACE_DAYS) out.add(rank);
        }
        return out;
    }

    /**
     * Who takes a guild's throne when its interregnum ends: the claimant who won most duels, then the highest rank, then the
     * longest serving (lowest id). {@code claims}: officer id → duels won. Null if nobody claimed it (the guild dissolves).
     */
    public static Officer heir(Hierarchy h, Map<Integer, Integer> claims) {
        Officer best = null;
        int bestWins = -1;
        for (var e : claims.entrySet()) {
            Officer o = h.get(e.getKey());
            if (o == null || !o.alive() || !o.hunter()) continue;
            int wins = e.getValue();
            if (best == null || wins > bestWins || wins == bestWins && (o.rank.ordinal() > best.rank.ordinal()
                    || o.rank == best.rank && o.id < best.id)) {
                best = o;
                bestWins = wins;
            }
        }
        return best;
    }
}
