package org.papiricoh.supernaturalcraft.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance counters for audits (off unless switched on with {@code /supernatural debug stats on}): packets and bytes per
 * payload type each way, and time spent in probed server code. Server thread only; nothing is kept while it is off.
 */
public final class AuditStats {

    /** Packets, bytes (or calls, nanoseconds) and the largest single one, per key. */
    public record Line(String key, long count, long total, long max) {
    }

    /** On from the start with {@code -Dsn.audit=true} ({@code ./gradlew runX -Paudit}). */
    private static volatile boolean enabled = Boolean.getBoolean("sn.audit");
    private static long sinceMillis = System.currentTimeMillis();
    private static final Map<String, long[]> OUT = new HashMap<>();
    private static final Map<String, long[]> IN = new HashMap<>();
    private static final Map<String, long[]> PROBES = new HashMap<>();

    private AuditStats() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static synchronized void enable(boolean on) {
        enabled = on;
        reset();
    }

    public static synchronized void reset() {
        OUT.clear();
        IN.clear();
        PROBES.clear();
        sinceMillis = System.currentTimeMillis();
    }

    /** Seconds since the counters were last reset. */
    public static double seconds() {
        return Math.max(0.001, (System.currentTimeMillis() - sinceMillis) / 1000.0);
    }

    /** A payload of {@code bytes} sent to {@code recipients} players. */
    public static synchronized void sent(String type, int bytes, int recipients) {
        if (!enabled || recipients <= 0) return;
        add(OUT, type, recipients, (long) bytes * recipients, bytes);
    }

    /** A payload received from a client. */
    public static synchronized void received(String type, int bytes) {
        if (!enabled) return;
        add(IN, type, 1, bytes, bytes);
    }

    /** Starts timing probed code: pass the result to {@link #stop}. Free when the counters are off. */
    public static long start() {
        return enabled ? System.nanoTime() : 0L;
    }

    public static synchronized void stop(String probe, long started) {
        if (!enabled || started == 0L) return;
        long n = System.nanoTime() - started;
        add(PROBES, probe, 1, n, n);
    }

    private static void add(Map<String, long[]> map, String key, long count, long total, long single) {
        long[] v = map.computeIfAbsent(key, k -> new long[3]);
        v[0] += count;
        v[1] += total;
        v[2] = Math.max(v[2], single);
    }

    public static synchronized List<Line> sent() {
        return lines(OUT);
    }

    public static synchronized List<Line> received() {
        return lines(IN);
    }

    public static synchronized List<Line> probes() {
        return lines(PROBES);
    }

    private static List<Line> lines(Map<String, long[]> map) {
        List<Line> out = new ArrayList<>();
        map.forEach((k, v) -> out.add(new Line(k, v[0], v[1], v[2])));
        out.sort(Comparator.comparingLong(Line::total).reversed());
        return out;
    }
}
