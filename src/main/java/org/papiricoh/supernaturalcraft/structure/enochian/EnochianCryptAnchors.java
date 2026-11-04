package org.papiricoh.supernaturalcraft.structure.enochian;

import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;

/**
 * The contract between the buried Enochian crypt's blueprint ({@link EnochianCryptPlan}, by ART) and the gameplay code (v0.36,
 * pure): every spot the code relies on, relative to the plan's origin. Get one from {@link EnochianCryptPlan#anchors()}: the
 * fixed sizes are constants here, the rest may move a little with the seed.
 *
 * <h2>Plan space</h2>
 * X east, Y up, Z south. The footprint is {@code x = 0 .. SIZE_X - 1}, {@code z = 0 .. SIZE_Z - 1}; vertically the plan spans
 * {@link #BOTTOM_Y} .. {@link #TOP_Y}. {@code y = 0} is the crypt's feet level (its floor blocks are at {@code y = -1}). The
 * surface shrine stands on the ground at {@link #GROUND_Y}: place the plan so that {@code origin.y + GROUND_Y} is the first air
 * cell above the terrain (the shrine's paving replaces the terrain's top block at {@code GROUND_Y - 1}). Everything below the
 * surface is drawn as an explicit shell (floor, walls, ceiling) with explicit air inside; unplanned cells keep the world's rock.
 *
 * <p>Positions are block cells. A "feet" spot is an air cell with a floor under it (where a mob or a player stands).
 */
public final class EnochianCryptAnchors {

    public static final int SIZE_X = 39, SIZE_Z = 33;
    /** Lowest planned cell (the crypt's floor blocks) and highest (the top of the shrine's gate). */
    public static final int BOTTOM_Y = -1, TOP_Y = 17;
    public static final int SIZE_Y = TOP_Y - BOTTOM_Y + 1;
    /** The surface's feet level relative to the origin (the crypt floor's feet level is 0). */
    public static final int GROUND_Y = 12;
    /** The sealed doors, in the order the player meets them. */
    public static final int DOORS = 3;
    /** The sigil that opens door {@code i} (the {@code sigil} property of its plate). */
    public static final List<String> DOOR_SIGILS = List.of("banishing", "ward", "alarm");

    /**
     * A sigil plate: a full block in a wall at {@code pos}; its face points into the room toward {@code facing} (the block
     * state's {@code facing}); {@code sigil} is its {@code sigil} property.
     */
    public record Plate(LayoutPoint pos, String facing, String sigil) {
    }

    /** A Word-of-God mural: a full block in a wall at {@code pos}, face toward {@code facing}, texture {@code variant} (0–3). */
    public record Mural(LayoutPoint pos, String facing, int variant) {
    }

    private final LayoutPoint entrance, pedestal;
    private final List<LayoutPoint> chests, guardianSpots, trapCentres, doorMin, doorMax;
    private final List<Plate> plates;
    private final List<Mural> murals;

    EnochianCryptAnchors(LayoutPoint entrance, LayoutPoint pedestal, List<LayoutPoint> chests, List<LayoutPoint> guardianSpots,
                         List<LayoutPoint> trapCentres, List<Plate> plates, List<LayoutPoint> doorMin, List<LayoutPoint> doorMax,
                         List<Mural> murals) {
        this.entrance = entrance;
        this.pedestal = pedestal;
        this.chests = List.copyOf(chests);
        this.guardianSpots = List.copyOf(guardianSpots);
        this.trapCentres = List.copyOf(trapCentres);
        this.plates = List.copyOf(plates);
        this.doorMin = List.copyOf(doorMin);
        this.doorMax = List.copyOf(doorMax);
        this.murals = List.copyOf(murals);
    }

    /** Feet spot on the shrine's paving at the head of the stair (at {@link #GROUND_Y}). */
    public LayoutPoint entrance() {
        return entrance;
    }

    /** The {@code tablet_pedestal[full=true]} block in the vault's centre (it stands on the floor: {@code y = 0}). */
    public LayoutPoint pedestal() {
        return pedestal;
    }

    /** The vault's two chests (block cells, {@code y = 0}). */
    public List<LayoutPoint> chests() {
        return chests;
    }

    /** Feet spots for the demon guardians: in the antechamber and the trap corridor (4–6). */
    public List<LayoutPoint> guardianSpots() {
        return guardianSpots;
    }

    /**
     * The centre cell (part 4) of each devil's trap painted on the trap corridor's ceiling: the air cell right under the
     * ceiling block, so the 3×3 is {@code centre ± 1} in x and z at the same y.
     */
    public List<LayoutPoint> ceilingTrapCentres() {
        return trapCentres;
    }

    /** The sigil plate beside door {@code i}. */
    public Plate plate(int i) {
        return plates.get(i);
    }

    /** Door {@code i}'s box of {@code sealed_crypt_door} blocks, inclusive corners (3 wide × 3 high × 1 thick). */
    public LayoutPoint doorMin(int i) {
        return doorMin.get(i);
    }

    public LayoutPoint doorMax(int i) {
        return doorMax.get(i);
    }

    /** {@link #DOOR_SIGILS}{@code [i]}. */
    public String doorSigil(int i) {
        return DOOR_SIGILS.get(i);
    }

    /** Every mural block of the library (10). */
    public List<Mural> murals() {
        return murals;
    }
}
