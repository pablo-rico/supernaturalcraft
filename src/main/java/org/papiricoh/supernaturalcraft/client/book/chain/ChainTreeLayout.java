package org.papiricoh.supernaturalcraft.client.book.chain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where each living officer of one chain sits on the book's canvas (pure). A tidy tree: leaves take consecutive slots
 * {@link #GX} apart, every officer sits centred over its subordinates, and the row is its rank (the Lord on top).
 * Siblings go by rank, then level, then id, so the same chain always draws the same way. Officers whose superior is
 * missing hang from nothing in their own rank's row.
 */
public final class ChainTreeLayout {

    /** Map units between neighbouring leaves and between rank rows. */
    public static final float GX = 46, GY = 60;

    /** One officer as the layout sees it: rank 0 (Soldier) to 4 (Lord). */
    public record Node(int id, int superior, int rank, int level) {
    }

    /** An officer's place; {@code parent} is the superior it is drawn under, or -1. */
    public record Place(Node node, float x, float y, int parent) {
    }

    /** Every place by id (in drawing order) and the bounds of their centres. */
    public record Result(Map<Integer, Place> places, float minX, float maxX, float minY, float maxY) {
        public static final Result EMPTY = new Result(Map.of(), 0, 0, 0, 0);
    }

    private static final Comparator<Node> ORDER = Comparator.comparingInt((Node n) -> -n.rank())
            .thenComparingInt(n -> -n.level()).thenComparingInt(Node::id);
    private static final int TOP_RANK = 4, MAX_DEPTH = 32;

    private ChainTreeLayout() {
    }

    public static Result layout(List<Node> nodes) {
        if (nodes.isEmpty()) return Result.EMPTY;
        Map<Integer, Node> byId = new HashMap<>();
        for (Node n : nodes) byId.put(n.id(), n);
        Map<Integer, List<Node>> children = new HashMap<>();
        List<Node> roots = new ArrayList<>();
        for (Node n : nodes) {
            Node sup = byId.get(n.superior());
            if (sup != null && sup.id() != n.id() && sup.rank() > n.rank()) children.computeIfAbsent(sup.id(), k -> new ArrayList<>()).add(n);
            else roots.add(n);
        }
        roots.sort(ORDER);
        for (List<Node> kids : children.values()) kids.sort(ORDER);
        Map<Integer, Place> out = new LinkedHashMap<>();
        float[] next = {0};
        for (Node root : roots) place(root, -1, children, out, next, 0);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Place p : out.values()) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }
        return new Result(out, minX, maxX, minY, maxY);
    }

    /** Places a subtree and returns its node's x. */
    private static float place(Node n, int parent, Map<Integer, List<Node>> children, Map<Integer, Place> out, float[] next, int depth) {
        List<Node> kids = depth < MAX_DEPTH ? children.getOrDefault(n.id(), List.of()) : List.of();
        float x;
        if (kids.isEmpty()) {
            x = next[0];
            next[0] += GX;
        } else {
            // The parent is reserved first so it draws before (under) its subordinates' links.
            out.put(n.id(), null);
            float first = 0, last = 0;
            for (int i = 0; i < kids.size(); i++) {
                float kx = place(kids.get(i), n.id(), children, out, next, depth + 1);
                if (i == 0) first = kx;
                last = kx;
            }
            x = (first + last) / 2;
        }
        out.put(n.id(), new Place(n, x, (TOP_RANK - n.rank()) * GY, parent));
        return x;
    }
}
