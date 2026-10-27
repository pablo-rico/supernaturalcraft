package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

import java.util.Arrays;

/**
 * The front between the chains (pure): the officers' country, the ring of {@link #INNER} to {@link #OUTER} blocks around the
 * world's origin, cut into {@link #SECTORS} sectors of 30°. Each starts held by the faction whose seat lies nearer its bearing
 * ({@link #initial}); a faction that wakes later takes the sectors round its own seat ({@link #admit}). Skirmishes and duels
 * between factions build a faction's pressure on a sector ({@link #push}); past {@link #FLIP} it changes hands. The officers
 * below court rank roam only their own faction's sectors ({@link #remap}): a lost sector is a retreat.
 */
public final class Front {
    public static final int SECTORS = 12, INNER = 300, OUTER = 2900, FLIP = 6;
    public static final double WIDTH = 2 * Math.PI / SECTORS;
    private static final int FACTIONS = ChainFaction.values().length;

    private final ChainFaction[] owner = new ChainFaction[SECTORS];
    /** Each faction's pressure on each sector, {@code [sector * FACTIONS + faction]}; the holder's win clears the sector's. */
    private final int[] pressure = new int[SECTORS * FACTIONS];

    /** Sectors split between the seats: each goes to the faction whose seat's bearing is nearer its middle. */
    public static Front initial(java.util.Map<ChainFaction, SeatSite.Site> seats) {
        if (seats.isEmpty()) throw new IllegalArgumentException("A front needs at least one seat");
        Front f = new Front();
        for (int i = 0; i < SECTORS; i++) {
            double mid = middle(i), best = Double.MAX_VALUE;
            for (var e : seats.entrySet()) {
                double g = gap(mid, bearing(e.getValue()));
                if (g < best) { best = g; f.owner[i] = e.getKey(); }
            }
        }
        return f;
    }

    /**
     * A faction arriving on a front already shared out takes the sectors nearest its seat's bearing: an equal share
     * ({@link #SECTORS} over the factions then holding ground, itself included), never a holder's last sector. No-op if it
     * already holds ground. @return how many sectors it took
     */
    public int admit(ChainFaction f, SeatSite.Site seat) {
        if (held(f) > 0) return 0;
        java.util.Set<ChainFaction> holders = java.util.EnumSet.noneOf(ChainFaction.class);
        for (ChainFaction o : owner) holders.add(o);
        int share = SECTORS / (holders.size() + 1), taken = 0;
        double b = bearing(seat);
        Integer[] order = new Integer[SECTORS];
        for (int i = 0; i < SECTORS; i++) order[i] = i;
        Arrays.sort(order, java.util.Comparator.comparingDouble(i -> gap(middle(i), b)));
        for (int i : order) {
            if (taken >= share) break;
            if (held(owner[i]) <= 1) continue;
            set(i, f);
            taken++;
        }
        return taken;
    }

    /**
     * A front read back from a save ({@code owners}: one byte per sector, the faction's ordinal). {@code scores} is the
     * pressure table, or, from saves before v0.28, one signed score per sector (positive: the Pit pushing, negative: the Host).
     */
    public static Front restore(byte[] owners, int[] scores) {
        Front f = new Front();
        for (int i = 0; i < SECTORS; i++)
            f.owner[i] = ChainFaction.values()[Math.floorMod(i < owners.length ? owners[i] : 0, FACTIONS)];
        if (scores.length == SECTORS) {
            for (int i = 0; i < SECTORS; i++) {
                if (scores[i] > 0) f.pressure[i * FACTIONS + ChainFaction.DEMON.ordinal()] = scores[i];
                if (scores[i] < 0) f.pressure[i * FACTIONS + ChainFaction.ANGEL.ordinal()] = -scores[i];
            }
        } else if (scores.length > 0 && scores.length % SECTORS == 0) {
            int saved = scores.length / SECTORS;
            for (int i = 0; i < SECTORS; i++)
                for (int k = 0; k < Math.min(saved, FACTIONS); k++) f.pressure[i * FACTIONS + k] = scores[i * saved + k];
        }
        return f;
    }

    public byte[] owners() {
        byte[] b = new byte[SECTORS];
        for (int i = 0; i < SECTORS; i++) b[i] = (byte) owner[i].ordinal();
        return b;
    }

    /** The pressure table ({@code [sector * factions + faction]}), as saved and synced. */
    public int[] scores() {
        return Arrays.copyOf(pressure, pressure.length);
    }

    public ChainFaction owner(int sector) {
        return owner[Math.floorMod(sector, SECTORS)];
    }

    /** {@code f}'s pressure on a sector. */
    public int pressure(int sector, ChainFaction f) {
        return pressure[Math.floorMod(sector, SECTORS) * FACTIONS + f.ordinal()];
    }

    public void set(int sector, ChainFaction f) {
        owner[Math.floorMod(sector, SECTORS)] = f;
        clear(Math.floorMod(sector, SECTORS));
    }

    public int held(ChainFaction f) {
        int n = 0;
        for (ChainFaction o : owner) if (o == f) n++;
        return n;
    }

    public static int sector(double x, double z) {
        return Math.floorMod((int) Math.floor(Math.atan2(z, x) / WIDTH), SECTORS);
    }

    public static double middle(int sector) {
        return (Math.floorMod(sector, SECTORS) + 0.5) * WIDTH;
    }

    /**
     * {@code by} wins ground in a sector. @return true if the sector changed hands (its pressure starts over). A holder's own
     * win clears the sector; a faction never loses its last sector.
     */
    public boolean push(int sector, ChainFaction by, int points) {
        int i = Math.floorMod(sector, SECTORS);
        if (owner[i] == by) {
            clear(i);
            return false;
        }
        int p = pressure[i * FACTIONS + by.ordinal()] += points;
        if (p < FLIP || held(owner[i]) <= 1) return false;
        owner[i] = by;
        clear(i);
        return true;
    }

    private void clear(int sector) {
        Arrays.fill(pressure, sector * FACTIONS, (sector + 1) * FACTIONS, 0);
    }

    /** The bearing nearest {@code angle} inside a sector {@code f} holds (the angle itself if it already lies in one). */
    public double remap(double angle, ChainFaction f) {
        int here = sector(Math.cos(angle), Math.sin(angle));
        if (owner[here] == f) return angle;
        double offset = angle - Math.floor(angle / WIDTH) * WIDTH;
        for (int step = 1; step <= SECTORS / 2; step++) {
            for (int dir : new int[]{1, -1}) {
                int s = Math.floorMod(here + dir * step, SECTORS);
                if (owner[s] == f) return s * WIDTH + offset;
            }
        }
        return angle;
    }

    /** The sector held by {@code f} that borders another faction's (where a skirmish breaks out), or -1. */
    public int border(ChainFaction f, int salt) {
        return border(f, null, salt);
    }

    /** The sector held by {@code f} next to one held by {@code against} (any other faction if null), or -1. */
    public int border(ChainFaction f, ChainFaction against, int salt) {
        for (int k = 0; k < SECTORS; k++) {
            int i = Math.floorMod(salt + k, SECTORS);
            if (owner[i] != f) continue;
            for (int n : new int[]{i + 1, i - 1}) {
                ChainFaction o = owner[Math.floorMod(n, SECTORS)];
                if (o != f && (against == null || o == against)) return i;
            }
        }
        return -1;
    }

    private static double bearing(SeatSite.Site seat) {
        return Math.atan2(seat.z(), seat.x());
    }

    private static double gap(double a, double b) {
        double d = Math.abs(Math.IEEEremainder(a - b, 2 * Math.PI));
        return Math.min(d, 2 * Math.PI - d);
    }
}
