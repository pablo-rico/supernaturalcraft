package org.papiricoh.supernaturalcraft.apocalypse.seal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the plagues changed in the world and how to undo it (v0.40), pure: every block a plague turns (water to blood, a crop
 * withered, grass to dead earth) is written here with what it was, what it became and the clock tick it turns back. Bounded
 * ({@link #CAP}); one entry per block (the first change of a block is the one remembered, so a block changed twice still goes
 * back to what it first was). Kept in {@code SealSavedData}; the server reverts at most {@link #REVERT_PER_TICK} a tick, and only
 * a block still as the plague left it (a hunter who has dug it up or built over it keeps their work).
 *
 * <p>Positions are packed longs ({@code BlockPos.asLong}); states are their command strings ({@code minecraft:wheat[age=7]}).
 */
public final class PlagueLedger {

    /** The most changes kept at once: a plague asks again later when the ledger is full. */
    public static final int CAP = 2048;
    /** The most blocks turned back in one tick. */
    public static final int REVERT_PER_TICK = 64;

    /** One change: where, what it was, what the plague made it, and when it turns back. */
    public record Entry(long pos, String original, String placed, long revertAt) {
    }

    private final Map<Long, Entry> entries = new LinkedHashMap<>();

    /** Records a change. @return false (nothing recorded, so nothing should be changed) if full or the block is already there */
    public boolean add(long pos, String original, String placed, long revertAt) {
        if (entries.size() >= CAP || entries.containsKey(pos)) return false;
        entries.put(pos, new Entry(pos, original, placed, revertAt));
        return true;
    }

    /** Whether a plague has changed the block at {@code pos}. */
    public boolean has(long pos) {
        return entries.containsKey(pos);
    }

    public Entry get(long pos) {
        return entries.get(pos);
    }

    /** Forgets {@code pos} (it was reverted by hand, or its block is gone). */
    public Entry remove(long pos) {
        return entries.remove(pos);
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean full() {
        return entries.size() >= CAP;
    }

    public Collection<Entry> all() {
        return entries.values();
    }

    /** Takes out (and returns) up to {@code max} entries whose time has come, oldest first. */
    public List<Entry> due(long now, int max) {
        List<Entry> out = new ArrayList<>();
        for (Iterator<Entry> it = entries.values().iterator(); it.hasNext() && out.size() < max; ) {
            Entry e = it.next();
            if (e.revertAt() <= now) {
                out.add(e);
                it.remove();
            }
        }
        return out;
    }

    /** Takes out up to {@code max} entries whatever their time (the echo turns everything back; the cleansing rite). */
    public List<Entry> take(int max) {
        List<Entry> out = new ArrayList<>();
        for (Iterator<Entry> it = entries.values().iterator(); it.hasNext() && out.size() < max; ) {
            out.add(it.next());
            it.remove();
        }
        return out;
    }

    /** Brings forward to {@code now} every entry within {@code radius} (horizontally) of ({@code x}, {@code z}). @return how many */
    public int hasten(int x, int z, int radius, long now) {
        int n = 0;
        long r2 = (long) radius * radius;
        for (Map.Entry<Long, Entry> e : entries.entrySet()) {
            Entry v = e.getValue();
            long dx = unpackX(v.pos()) - x, dz = unpackZ(v.pos()) - z;
            if (dx * dx + dz * dz <= r2 && v.revertAt() > now) {
                e.setValue(new Entry(v.pos(), v.original(), v.placed(), now));
                n++;
            }
        }
        return n;
    }

    /** Brings every entry forward to {@code now}. */
    public void hastenAll(long now) {
        for (Map.Entry<Long, Entry> e : entries.entrySet()) {
            Entry v = e.getValue();
            if (v.revertAt() > now) e.setValue(new Entry(v.pos(), v.original(), v.placed(), now));
        }
    }

    public void clear() {
        entries.clear();
    }

    /** Whether a block found as {@code current} should go back: only as the plague left it. */
    public static boolean untouched(Entry e, String current) {
        return e.placed().equals(current);
    }

    // BlockPos.asLong's packing (26 bits x, 26 bits z, 12 bits y), so the ledger can measure without Minecraft's classes.
    private static final int PACKED_X_LENGTH = 26, PACKED_Z_LENGTH = 26, PACKED_Y_LENGTH = 12;
    private static final int Z_OFFSET = PACKED_Y_LENGTH, X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

    public static long pack(int x, int y, int z) {
        long l = 0L;
        l |= ((long) x & ((1L << PACKED_X_LENGTH) - 1L)) << X_OFFSET;
        l |= ((long) y & ((1L << PACKED_Y_LENGTH) - 1L));
        return l | ((long) z & ((1L << PACKED_Z_LENGTH) - 1L)) << Z_OFFSET;
    }

    public static int unpackX(long l) {
        return (int) (l << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
    }

    public static int unpackY(long l) {
        return (int) (l << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
    }

    public static int unpackZ(long l) {
        return (int) (l << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
    }
}
