package org.papiricoh.supernaturalcraft.entity.boss.michael.host;

import java.util.ArrayList;
import java.util.List;

/**
 * How a company of the Host stands (pure, tested in JUnit): the slot of each soldier in the phalanx for the order it was
 * given, and its disorder once its captain falls.
 *
 * <p>Slots are offsets in the company's own frame, in blocks: {@code right} across, {@code forward} toward the foe.
 * <ul>
 *   <li>{@link Order#SHIELD_WALL}: two ranks across the field, shields up, between Michael and the hunters.</li>
 *   <li>{@link Order#CHARGE}: a wedge, its point on the foe, that runs in.</li>
 *   <li>{@link Order#ENCIRCLE}: a ring round the foe (the frame's origin is the foe here), closing in.</li>
 * </ul>
 * The captain always takes slot 0: the wall's centre, the wedge's point, or behind the ring.
 */
public final class HostFormation {

    public enum Order { SHIELD_WALL, CHARGE, ENCIRCLE }

    /** Gap between two soldiers in a rank, and between the ranks. */
    public static final double SPACING = 1.6, RANK_GAP = 1.8;
    /** Radius of the ring round a foe. */
    public static final double RING = 4.5;
    /** Speed of a broken company, as a multiplier. */
    public static final float DISORDER_SPEED = 0.65f;
    /** Ticks an order holds before the next. */
    public static final int ORDER_TICKS = 160;

    private Order order = Order.SHIELD_WALL;
    private boolean captainFallen;

    public Order order() {
        return order;
    }

    public void give(Order order) {
        if (!captainFallen) this.order = order;
    }

    /** Its captain is dead: the company breaks and fights on as a mob, slower. */
    public void captainFalls() {
        captainFallen = true;
    }

    public boolean disordered() {
        return captainFallen;
    }

    /** The speed multiplier of its soldiers. */
    public float speed() {
        return captainFallen ? DISORDER_SPEED : order == Order.CHARGE ? 1.35f : 1.0f;
    }

    /** Where soldier {@code index} of {@code size} stands, as {right, forward}; null once the company is broken. */
    public double[] slot(int index, int size) {
        if (captainFallen) return null;
        return slots(order, size).get(Math.floorMod(index, Math.max(1, size)));
    }

    /** Every slot of an order for a company of {@code size}, all different, the captain's first. */
    public static List<double[]> slots(Order order, int size) {
        List<double[]> out = new ArrayList<>(size);
        switch (order) {
            case SHIELD_WALL -> {
                // Front rank as wide as half the company (the captain in its centre), the rest a rank behind.
                int front = Math.max(1, (size + 1) / 2);
                for (int i = 0; i < size; i++) {
                    boolean first = i < front;
                    int k = first ? i : i - front, n = first ? front : size - front;
                    out.add(new double[]{centred(k, n) * SPACING, first ? 0 : -RANK_GAP});
                }
            }
            case CHARGE -> {
                // A wedge: the point, then pairs further back and wider each row.
                out.add(new double[]{0, 0});
                for (int i = 1; i < size; i++) {
                    int row = (i + 1) / 2;
                    double side = (i % 2 == 1) ? -1 : 1;
                    out.add(new double[]{side * row * SPACING * 0.8, -row * RANK_GAP * 0.8});
                }
            }
            case ENCIRCLE -> {
                // The captain stands off; the rest share the ring evenly.
                out.add(new double[]{0, -(RING + 2.5)});
                int ring = Math.max(1, size - 1);
                for (int i = 1; i < size; i++) {
                    double a = (i - 1) * Math.PI * 2 / ring;
                    out.add(new double[]{Math.sin(a) * RING, -Math.cos(a) * RING});
                }
            }
        }
        return out;
    }

    /** Offset of the {@code k}-th of {@code n} in a rank, centred on zero (the middle one first: the captain's place). */
    private static double centred(int k, int n) {
        // Places in the rank, nearest the middle first: the middle, then one to the left, one to the right...
        int middle = (n - 1) / 2;
        List<Integer> places = new ArrayList<>();
        for (int p = 0; p < n; p++) places.add(p);
        places.sort((a, b) -> Math.abs(a - middle) != Math.abs(b - middle) ? Math.abs(a - middle) - Math.abs(b - middle) : a - b);
        return places.get(k) - (n - 1) / 2.0;
    }

    /**
     * The next order: a company far from the foe charges, one round a lone close foe encircles it, otherwise it walls up.
     * Never the same order twice running, so the field keeps changing.
     */
    public static Order next(Order last, double distanceToFoe, int foes) {
        Order want = distanceToFoe > 12 ? Order.CHARGE : foes <= 1 && distanceToFoe < 8 ? Order.ENCIRCLE : Order.SHIELD_WALL;
        if (want != last) return want;
        return switch (last) {
            case SHIELD_WALL -> distanceToFoe > 6 ? Order.CHARGE : Order.ENCIRCLE;
            case CHARGE -> Order.SHIELD_WALL;
            case ENCIRCLE -> Order.CHARGE;
        };
    }
}
