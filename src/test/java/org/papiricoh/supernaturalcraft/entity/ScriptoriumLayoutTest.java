package org.papiricoh.supernaturalcraft.entity;

import org.junit.jupiter.api.Test;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScriptoriumLayout;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScriptoriumLayout.Kind;
import org.papiricoh.supernaturalcraft.entity.boss.metatron.ScriptoriumLayout.Place;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptoriumLayoutTest {

    @Test
    void theLibraryLeavesTheStairsAndTheDaisClear() {
        List<Place> lib = ScriptoriumLayout.library();
        assertTrue(lib.size() > 80, "a thin library: " + lib.size());
        for (Place p : lib) {
            assertFalse(ScriptoriumLayout.daisFootprint(p.dx(), p.dz()), "the library blocks the dais at " + p);
            assertTrue(Math.abs(p.dx()) > ScriptoriumLayout.CORRIDOR, "the library blocks the stairs' corridor at " + p);
            assertTrue(Math.hypot(p.dx(), p.dz()) < 20, "outside the arena: " + p);
        }
    }

    @Test
    void theShelfRingsHaveGaps() {
        // Walking straight out from the centre at some angle must find a way through each ring.
        Set<Long> shelves = new HashSet<>();
        for (Place p : ScriptoriumLayout.library()) if (p.kind() == Kind.SHELF) shelves.add(((long) p.dx() << 32) | (p.dz() & 0xFFFFFFFFL));
        int open = 0;
        for (int deg = 0; deg < 360; deg += 5) {
            boolean blocked = false;
            for (double r = 3; r < 17 && !blocked; r += 0.5) {
                int x = (int) Math.round(Math.cos(Math.toRadians(deg)) * r), z = (int) Math.round(Math.sin(Math.toRadians(deg)) * r);
                blocked = shelves.contains(((long) x << 32) | (z & 0xFFFFFFFFL));
            }
            if (!blocked) open++;
        }
        assertTrue(open >= 6, "too few ways through the shelves: " + open);
    }

    @Test
    void theDaisIsCentredWithTwoFlights() {
        List<Place> dais = ScriptoriumLayout.dais();
        long top = dais.stream().filter(p -> p.kind() == Kind.DAIS_TOP).count();
        assertEquals((2 * ScriptoriumLayout.DAIS_HALF + 1) * (2 * ScriptoriumLayout.DAIS_HALF + 1), top);
        for (Place p : dais) assertTrue(ScriptoriumLayout.daisFootprint(p.dx(), p.dz()), "outside its own footprint: " + p);
        long north = dais.stream().filter(p -> p.kind() == Kind.STAIR && p.north()).count();
        long south = dais.stream().filter(p -> p.kind() == Kind.STAIR && !p.north()).count();
        assertEquals(north, south);
        assertEquals(ScriptoriumLayout.DAIS_HEIGHT * (2 * ScriptoriumLayout.CORRIDOR + 1), north);
        // Each step is one block lower than the last, so the flight can be walked.
        for (Place p : dais) {
            if (p.kind() != Kind.STAIR) continue;
            int step = Math.abs(p.dz()) - ScriptoriumLayout.DAIS_HALF;
            assertEquals(ScriptoriumLayout.DAIS_HEIGHT - step, p.dy());
        }
    }
}
