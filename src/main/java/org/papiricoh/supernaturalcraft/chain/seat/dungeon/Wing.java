package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Brush;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Dir;
import org.papiricoh.supernaturalcraft.buildkit.Family;
import org.papiricoh.supernaturalcraft.buildkit.Lighting;
import org.papiricoh.supernaturalcraft.buildkit.Openings;
import org.papiricoh.supernaturalcraft.buildkit.Roofs;
import org.papiricoh.supernaturalcraft.buildkit.Walls;

/**
 * A room module of a stronghold (pure): one wing drawn in its own frame facing south, then stamped turned into a slot of the
 * stronghold so its door opens on the court. The frame: walls on {@link #OUTLINE} (17 × 13), the floor at {@code y = -1},
 * the door 3 wide in the middle of the south wall ({@code z = 0}), the court in front of it at {@code z >= 1}. A wing names its
 * own guard anchors ({@code guard_<room>_<n>}, at least two) in this frame.
 *
 * @param room    the room's name in its guard anchors
 * @param drawer  draws the wing (shell and fittings) on an empty canvas
 */
record Wing(String room, Drawer drawer) {

    interface Drawer {
        void draw(Canvas c, DungeonStyle st, int seed);
    }

    static final int HW = 8, D = 12, WT = 6;
    static final Box OUTLINE = new Box(-HW, 0, -D, HW, WT, 0);
    /** The room inside the walls (floor cells at {@code y = -1} under it). */
    static final Box INSIDE = new Box(-HW + 1, 0, -D + 1, HW - 1, WT, -1);

    /** The south (door) wall, run from x = 8 to x = -8 (index i is x = 8 - i). */
    static Walls.Run front() {
        return Walls.around(OUTLINE).get(2);
    }

    /** The east wall, from z = -12 to 0 (index i is z = i - 12). */
    static Walls.Run east() {
        return Walls.around(OUTLINE).get(1);
    }

    /** The west wall, from z = 0 to -12 (index i is z = -i). */
    static Walls.Run west() {
        return Walls.around(OUTLINE).get(3);
    }

    Canvas draw(DungeonStyle st, int seed) {
        Canvas c = new Canvas("wing_" + room);
        drawer.draw(c, st, seed);
        return c;
    }

    // --- the shell every wing shares ------------------------------------------------------------------------------------------

    /**
     * Footings, floor, walls with a plinth course and buttresses, the arched door, windows in the side walls and the front, tie
     * beams across the room at the wall top (lanterns hang from them) and a steep roof with a sloped ceiling.
     */
    static void shell(Canvas c, DungeonStyle st, int s) {
        shell(c, st, s, st.roof, false);
    }

    /**
     * {@link #shell(Canvas, DungeonStyle, int)} with its own roof: a steep gable of {@code roof}, or ({@code flat}) a flat deck
     * behind battlements. The roofline is what tells the wings apart from outside.
     */
    static void shell(Canvas c, DungeonStyle st, int s, Family roof, boolean flat) {
        c.fill(new Box(-HW, -3, -D, HW, -2, 0), st.plinth);
        for (int x = -HW; x <= HW; x++) {
            for (int z = -D; z <= 0; z++) {
                boolean in = INSIDE.containsColumn(x, z);
                c.set(x, -1, z, (in ? st.floor : st.plinth).at(x, -1, z));
            }
        }
        Walls.enclose(c, OUTLINE, 0, WT, st.wall);
        for (Walls.Run r : Walls.around(OUTLINE)) Walls.body(c, r, 0, 0, st.plinth);
        c.carve(INSIDE);
        Walls.Run front = front(), east = east(), west = west();
        Brush pier = st.plinth;
        for (int i : new int[]{1, 5, 11, 15}) DungeonKit.buttress(c, front, i, 0, WT - 2, pier, st.trim);
        for (Walls.Run r : new Walls.Run[]{east, west}) {
            for (int i : new int[]{4, 8}) DungeonKit.buttress(c, r, i, 0, WT - 2, pier, st.trim);
            for (int i : new int[]{2, 6, 10}) window(c, st, r, i, s);
        }
        for (int i : new int[]{3, 13}) window(c, st, front, i, s);
        Openings.archway(c, front, 7, 0, 3, 4, st.trim.stairs());
        c.set(0, 4, 0, st.accent.block());
        for (Walls.Run r : new Walls.Run[]{front, east, west}) Walls.belt(c, r, WT, st.trim);
        for (int z : new int[]{-4, -8}) {
            for (int x = -HW + 1; x <= HW - 1; x++) c.set(x, WT, z, st.wood.log("x"));
        }
        if (flat) {
            c.fill(new Box(-HW, WT + 1, -D, HW, WT + 1, 0), st.ceiling);
            DungeonKit.battlements(c, OUTLINE, WT + 2, st.wall, st.pit ? st.accent : st.trim, s + 9, 0.1);
        } else {
            Roofs.gable(c, OUTLINE, WT, true, new Roofs.Style(roof, st.roofTrim, st.wall, 1, Roofs.Pitch.STEEP, 0, true, null, true));
        }
    }

    /** The top of the roof over local (x, z). */
    static int ridge(Canvas c, int x, int z) {
        return c.topAt(x, z, 40, WT);
    }

    /** A chimney stack through the roof at local (x, z). */
    static void chimney(Canvas c, int x, int z, Brush body, Family cap) {
        Roofs.chimney(c, x, z, WT + 1, body, cap, false);
    }

    private static void window(Canvas c, DungeonStyle st, Walls.Run r, int i, int s) {
        int x = r.x(i), z = r.z(i);
        String pane = st.pane(org.papiricoh.supernaturalcraft.buildkit.Noise.pick(x, 0, z, s + 5, 7) == 0 ? 2 : st.pit ? 0 : 1);
        int height = st.pit ? 2 : 3;
        Openings.window(c, r, i, st.pit ? 2 : 1, 1, height, new Openings.WindowStyle(pane, null, st.trim, st.trim, null, null, false, null));
    }

    /** Lanterns hanging from the tie beams (two per beam, off the middle). */
    static void beamLights(Canvas c, DungeonStyle st, int... xs) {
        for (int z : new int[]{-4, -8}) for (int x : xs) Lighting.hanging(c, x, WT, z, 2, st.pit);
    }

    /** The cells of the room's floor that stay clear for walking from the door: the middle strip. */
    static boolean aisle(int x, int z) {
        return Math.abs(x) <= 1;
    }

    static Dir inward() {
        return Dir.NORTH;
    }
}
