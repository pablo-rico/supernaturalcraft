package org.papiricoh.supernaturalcraft.bowl.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BearingTest {

    @Test
    void cardinalPointsFollowMinecraftAxes() {
        assertEquals(Bearing.Compass.NORTH, Bearing.compass(0, -10));
        assertEquals(Bearing.Compass.SOUTH, Bearing.compass(0, 10));
        assertEquals(Bearing.Compass.EAST, Bearing.compass(10, 0));
        assertEquals(Bearing.Compass.WEST, Bearing.compass(-10, 0));
    }

    @Test
    void diagonalsAndRounding() {
        assertEquals(Bearing.Compass.NORTH_EAST, Bearing.compass(10, -10));
        assertEquals(Bearing.Compass.SOUTH_EAST, Bearing.compass(10, 10));
        assertEquals(Bearing.Compass.SOUTH_WEST, Bearing.compass(-10, 10));
        assertEquals(Bearing.Compass.NORTH_WEST, Bearing.compass(-10, -10));
        // 20° east of north is still north; 25° is north-east.
        assertEquals(Bearing.Compass.NORTH, Bearing.compass(Math.sin(Math.toRadians(20)), -Math.cos(Math.toRadians(20))));
        assertEquals(Bearing.Compass.NORTH_EAST, Bearing.compass(Math.sin(Math.toRadians(25)), -Math.cos(Math.toRadians(25))));
        // Just west of due north wraps around to north, not north-west.
        assertEquals(Bearing.Compass.NORTH, Bearing.compass(-0.1, -10));
        assertEquals(Bearing.Compass.NORTH, Bearing.compass(0, 0));
    }

    @Test
    void distanceBands() {
        assertEquals(Bearing.Band.HERE, Bearing.band(3));
        assertEquals(Bearing.Band.NEAR, Bearing.band(Bearing.HERE_MAX));
        assertEquals(Bearing.Band.NEAR, Bearing.band(100));
        assertEquals(Bearing.Band.FAR, Bearing.band(500));
        assertEquals(Bearing.Band.DISTANT, Bearing.band(5000));
    }

    @Test
    void keysMatchLang() {
        assertEquals("direction.supernaturalcraft.north_east", Bearing.Compass.NORTH_EAST.key());
        assertEquals("message.supernaturalcraft.locate.band.far", Bearing.Band.FAR.key());
    }
}
