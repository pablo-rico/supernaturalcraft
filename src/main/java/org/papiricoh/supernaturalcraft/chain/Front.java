package org.papiricoh.supernaturalcraft.chain;

import org.papiricoh.supernaturalcraft.chain.seat.SeatSite;

import java.util.Arrays;

/**
 * The front between the Pit and the Host (pure): the officers' country, the ring of {@link #INNER} to {@link #OUTER} blocks
 * around the world's origin, cut into {@link #SECTORS} sectors of 30°. Each starts held by the side whose seat lies nearer its
 * bearing. Skirmishes and duels across the sides push a sector's {@link #score} ({@link #push}); past {@link #FLIP} it changes
 * hands. The officers below court rank roam only their own side's sectors ({@link #remap}): a lost sector is a retreat.
 */
public final class Front {
    public static final int SECTORS = 12, INNER = 300, OUTER = 2900, FLIP = 6;
    public static final double WIDTH = 2 * Math.PI / SECTORS;

    private final ChainFaction[] owner = new ChainFaction[SECTORS];
    /** Positive: the Pit pushing, negative: the Host. */
    private final int[] score = new int[SECTORS];

    /** Sectors split between the two seats: each goes to the side whose seat's bearing is nearer its middle. */
    public static Front initial(SeatSite.Site demonSeat, SeatSite.Site angelSeat) {
        Front f = new Front();
        double d = Math.atan2(demonSeat.z(), demonSeat.x()), a = Math.atan2(angelSeat.z(), angelSeat.x());
        for (int i = 0; i < SECTORS; i++) {
            double mid = middle(i);
            f.owner[i] = gap(mid, d) <= gap(mid, a) ? ChainFaction.DEMON : ChainFaction.ANGEL;
        }
        return f;
    }

    /** A front read back from a save ({@code owners}: one byte per sector, the faction's ordinal). */
    public static Front restore(byte[] owners, int[] scores) {
        Front f = new Front();
        for (int i = 0; i < SECTORS; i++) {
            f.owner[i] = ChainFaction.values()[Math.floorMod(i < owners.length ? owners[i] : 0, ChainFaction.values().length)];
            f.score[i] = i < scores.length ? scores[i] : 0;
        }
        return f;
    }

    public byte[] owners() {
        byte[] b = new byte[SECTORS];
        for (int i = 0; i < SECTORS; i++) b[i] = (byte) owner[i].ordinal();
        return b;
    }

    public int[] scores() {
        return Arrays.copyOf(score, SECTORS);
    }

    public ChainFaction owner(int sector) {
        return owner[Math.floorMod(sector, SECTORS)];
    }

    public int score(int sector) {
        return score[Math.floorMod(sector, SECTORS)];
    }

    public void set(int sector, ChainFaction f) {
        owner[Math.floorMod(sector, SECTORS)] = f;
        score[Math.floorMod(sector, SECTORS)] = 0;
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
     * {@code by} wins ground in a sector. @return true if the sector changed hands (its score starts over). A side never loses
     * its last sector.
     */
    public boolean push(int sector, ChainFaction by, int points) {
        int i = Math.floorMod(sector, SECTORS);
        score[i] += by == ChainFaction.DEMON ? points : -points;
        ChainFaction pushing = score[i] > 0 ? ChainFaction.DEMON : ChainFaction.ANGEL;
        if (Math.abs(score[i]) < FLIP || owner[i] == pushing || held(owner[i]) <= 1) {
            if (owner[i] == pushing) score[i] = 0;
            return false;
        }
        owner[i] = pushing;
        score[i] = 0;
        return true;
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

    /** The sector held by {@code f} that borders the other side's (where a skirmish breaks out), or -1. */
    public int border(ChainFaction f, int salt) {
        for (int k = 0; k < SECTORS; k++) {
            int i = Math.floorMod(salt + k, SECTORS);
            if (owner[i] == f && (owner[Math.floorMod(i + 1, SECTORS)] != f || owner[Math.floorMod(i - 1, SECTORS)] != f)) return i;
        }
        return -1;
    }

    private static double gap(double a, double b) {
        double d = Math.abs(Math.IEEEremainder(a - b, 2 * Math.PI));
        return Math.min(d, 2 * Math.PI - d);
    }
}
