package org.papiricoh.supernaturalcraft.client.book.chain;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainTreeLayout.Node;
import org.papiricoh.supernaturalcraft.client.book.chain.ChainTreeLayout.Place;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChainTreeLayoutTest {

    /** A full chain: Lord 0, Lieutenants 1–2, Captains 3–5, Soldiers 6–9, plus an orphan Captain 10. */
    private static List<Node> chain() {
        List<Node> n = new ArrayList<>();
        n.add(new Node(0, -1, 3, 6));
        n.add(new Node(1, 0, 2, 4));
        n.add(new Node(2, 0, 2, 5));
        n.add(new Node(3, 1, 1, 2));
        n.add(new Node(4, 1, 1, 2));
        n.add(new Node(5, 2, 1, 1));
        n.add(new Node(6, 3, 0, 0));
        n.add(new Node(7, 3, 0, 1));
        n.add(new Node(8, 5, 0, 0));
        n.add(new Node(9, 2, 0, 0));
        n.add(new Node(10, 99, 1, 0));
        return n;
    }

    @Test
    void everyOfficerIsPlacedOnItsRankRow() {
        var r = ChainTreeLayout.layout(chain());
        assertEquals(11, r.places().size());
        for (Place p : r.places().values()) assertEquals((3 - p.node().rank()) * ChainTreeLayout.GY, p.y());
    }

    @Test
    void noTwoOfficersShareASpotAndRowsKeepTheirGap() {
        var places = ChainTreeLayout.layout(chain()).places().values();
        for (Place a : places)
            for (Place b : places)
                if (a != b && a.y() == b.y()) assertTrue(Math.abs(a.x() - b.x()) >= ChainTreeLayout.GX - 0.01f, a + " overlaps " + b);
    }

    @Test
    void superiorsSitCentredOverTheirSubordinatesAndDrawFirst() {
        var r = ChainTreeLayout.layout(chain());
        List<Integer> order = new ArrayList<>(r.places().keySet());
        for (Place p : r.places().values()) {
            if (p.parent() < 0) continue;
            Place sup = r.places().get(p.parent());
            assertTrue(sup.y() < p.y(), "superior above " + p);
            assertTrue(order.indexOf(sup.node().id()) < order.indexOf(p.node().id()), "superior drawn first");
            float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
            for (Place k : r.places().values()) if (k.parent() == sup.node().id()) { lo = Math.min(lo, k.x()); hi = Math.max(hi, k.x()); }
            assertEquals((lo + hi) / 2, sup.x(), 0.01f);
        }
        assertEquals(-1, r.places().get(10).parent(), "a missing superior leaves an orphan root");
        assertEquals(-1, r.places().get(0).parent());
    }

    @Test
    void layoutIgnoresInputOrder() {
        var a = ChainTreeLayout.layout(chain());
        List<Node> shuffled = chain();
        Collections.reverse(shuffled);
        var b = ChainTreeLayout.layout(shuffled);
        assertEquals(a.places(), b.places());
        assertEquals(new ArrayList<>(a.places().keySet()), new ArrayList<>(b.places().keySet()));
    }

    @Test
    void emptyChainHasNoPlaces() {
        assertTrue(ChainTreeLayout.layout(List.of()).places().isEmpty());
    }
}
