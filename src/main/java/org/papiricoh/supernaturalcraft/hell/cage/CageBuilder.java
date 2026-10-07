package org.papiricoh.supernaturalcraft.hell.cage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.List;

import static org.papiricoh.supernaturalcraft.hell.cage.CageLayout.*;

/**
 * Puts the Pit's centre into the world following {@link CageLayout}, one chunk at a time: every
 * placement is clipped to the box it is given. Nothing here reads server state, so the same calls
 * build the structure during worldgen, for {@code /supernatural cage place} and in the tests.
 */
public final class CageBuilder {

    private CageBuilder() {
    }

    private static BlockState frame() {
        return AllBlocks.CAGE_FRAME.get().defaultBlockState();
    }

    private static BlockState seal() {
        return AllBlocks.CAGE_SEAL.get().defaultBlockState();
    }

    private static BlockState flag() {
        return AllBlocks.ABYSSAL_FLAGSTONE.get().defaultBlockState();
    }

    private static BlockState bedrock() {
        return AllBlocks.ABYSSAL_BEDROCK.get().defaultBlockState();
    }

    private static BlockState brazier() {
        return AllBlocks.HELLFIRE_BRAZIER.get().defaultBlockState();
    }

    private static BlockState pillar(Direction.Axis axis) {
        return AllBlocks.ENOCHIAN_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static BlockState chain(Direction.Axis axis) {
        return AllBlocks.CAGE_CHAIN.get().defaultBlockState().setValue(ChainBlock.AXIS, axis);
    }

    static void put(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p)) level.setBlock(p, s, 2);
    }

    /** Places a block only into air (chains threading past the structure must not cut through it). */
    static void putInAir(WorldGenLevel level, BoundingBox box, BlockPos p, BlockState s) {
        if (box.isInside(p) && level.getBlockState(p).isAir()) level.setBlock(p, s, 2);
    }

    /** A deterministic 0..1 value per position, for the decorations' irregularity. */
    static double hash(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    public static void build(WorldGenLevel level, BoundingBox box, boolean withLucifer) {
        island(level, box);
        dais(level, box);
        pillars(level, box);
        bridges(level, box);
        gates(level, box);
        anchorsAndChains(level, box);
        cage(level, box, 0);
        if (withLucifer && box.isInside(THRONE)) CagedLuciferEntity.place(level, AllEntities.CAGED_LUCIFER.get());
    }

    // --- the island --------------------------------------------------------------------------------

    static void island(WorldGenLevel level, BoundingBox box) {
        int r = ISLAND_RADIUS + 2;
        for (int y = ISLAND_BOTTOM; y <= ISLAND_Y; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!box.isInside(p)) continue;
                    double d = Math.sqrt(x * x + z * z);
                    double rad = islandRadius(y, (hash(x, y, z) - 0.5) * 3);
                    if (rad < 0 || d > rad) continue;
                    put(level, box, p, y == ISLAND_Y ? floorAt(x, z, d) : y > ISLAND_Y - SLAB ? flag() : bedrock());
                }
            }
        }
    }

    /** The island's floor: dais, seal ring, an inlaid ring of frame, a lip at the edge. */
    static BlockState floorAt(int x, int z, double d) {
        if (Math.abs(x) <= 1 && Math.abs(z) <= 1) return AllBlocks.CAGE_RITUAL_STONE.get().defaultBlockState();
        if (Math.abs(x) <= 5 && Math.abs(z) <= 5) return Math.abs(x) == 5 || Math.abs(z) == 5 ? frame() : bedrock();
        if (onSealRing(x, z)) return seal();
        if (d >= 19.5 && d < 20.5) return frame();
        if (d >= ISLAND_RADIUS - 0.5) return frame();
        return flag();
    }

    static void dais(WorldGenLevel level, BoundingBox box) {
        for (BlockPos p : plinths()) put(level, box, p, AllBlocks.CAGE_RITUAL_STONE.get().defaultBlockState());
        // Braziers round the island's rim, clear of the four bridge mouths.
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12 + Math.PI / 12;
            BlockPos p = new BlockPos((int) Math.round(Math.cos(a) * 24), ISLAND_Y + 1, (int) Math.round(Math.sin(a) * 24));
            put(level, box, p, brazier());
        }
    }

    static void pillars(WorldGenLevel level, BoundingBox box) {
        for (BlockPos base : CageLayout.pillars()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) put(level, box, base.offset(dx, 0, dz), frame());
            }
            for (int y = base.getY() + 1; y < PILLAR_TOP; y++) put(level, box, new BlockPos(base.getX(), y, base.getZ()), pillar(Direction.Axis.Y));
            put(level, box, new BlockPos(base.getX(), PILLAR_TOP, base.getZ()), brazier());
            // A chain from just above the flame up to the Cage's floor.
            BlockPos from = new BlockPos(base.getX(), PILLAR_TOP + 1, base.getZ());
            BlockPos to = new BlockPos((int) Math.round(base.getX() * 8.0 / PILLAR_RADIUS), CAGE_FLOOR - 1,
                    (int) Math.round(base.getZ() * 8.0 / PILLAR_RADIUS));
            chainLine(level, box, from, to);
        }
    }

    // --- bridges and gates ------------------------------------------------------------------------

    static void bridges(WorldGenLevel level, BoundingBox box) {
        for (int[] s : SPOKES) {
            int px = -s[1], pz = s[0];
            for (int t = ISLAND_RADIUS - 2; t <= BRIDGE_END; t++) {
                for (int w = -1; w <= 1; w++) {
                    int x = s[0] * t + px * w, z = s[1] * t + pz * w;
                    put(level, box, new BlockPos(x, ISLAND_Y, z), bedrock());
                    put(level, box, new BlockPos(x, ISLAND_Y - 1, z), bedrock());
                    if (w == 0) put(level, box, new BlockPos(x, ISLAND_Y - 2, z), bedrock());
                }
                if (t <= ISLAND_RADIUS) continue;
                for (int w : new int[]{-2, 2}) {
                    int x = s[0] * t + px * w, z = s[1] * t + pz * w;
                    put(level, box, new BlockPos(x, ISLAND_Y, z), bedrock());
                    if (t % 6 == 0) {
                        put(level, box, new BlockPos(x, ISLAND_Y + 1, z), frame());
                        if (t % 12 == 0) put(level, box, new BlockPos(x, ISLAND_Y + 2, z), brazier());
                    } else {
                        put(level, box, new BlockPos(x, ISLAND_Y + 1, z), chain(Direction.Axis.Y));
                    }
                }
                // Chains dangling from under the span into the dark.
                if (t % 10 == 5) {
                    int len = 6 + (int) (hash(s[0], t, s[1]) * 14);
                    for (int dy = 1; dy <= len; dy++) {
                        putInAir(level, box, new BlockPos(s[0] * t, ISLAND_Y - 2 - dy, s[1] * t), chain(Direction.Axis.Y));
                    }
                }
            }
        }
    }

    static void gates(WorldGenLevel level, BoundingBox box) {
        BlockState bricks = AllBlocks.HELLSTONE_BRICKS.get().defaultBlockState();
        for (int[] s : SPOKES) {
            int px = -s[1], pz = s[0];
            // Over the outer bridge, cut away any rock the wall pushes over it (but leave the railings).
            for (int t = CLEAR_START; t < GATE_START; t++) {
                for (int w = -GATE_HALF; w <= GATE_HALF; w++) {
                    for (int y = ISLAND_Y + 1; y <= ISLAND_Y + GATE_HEIGHT; y++) {
                        BlockPos p = new BlockPos(s[0] * t + px * w, y, s[1] * t + pz * w);
                        if (!box.isInside(p)) continue;
                        var here = level.getBlockState(p);
                        if (!here.isAir() && !here.is(AllBlocks.CAGE_CHAIN.get()) && !here.is(AllBlocks.CAGE_FRAME.get())
                                && !here.is(AllBlocks.HELLFIRE_BRAZIER.get())) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            for (int t = GATE_START; t <= GATE_END; t++) {
                for (int w = -GATE_HALF - 1; w <= GATE_HALF + 1; w++) {
                    int x = s[0] * t + px * w, z = s[1] * t + pz * w;
                    boolean wall = Math.abs(w) == GATE_HALF + 1;
                    for (int y = ISLAND_Y - 1; y <= ISLAND_Y + GATE_HEIGHT + 1; y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (y <= ISLAND_Y) {
                            if (t > BRIDGE_END || Math.abs(w) > 1) put(level, box, p, y == ISLAND_Y ? bricks : bedrock());
                        } else if (wall || y == ISLAND_Y + GATE_HEIGHT + 1) {
                            if (t > BRIDGE_END) put(level, box, p, bricks);
                        } else {
                            put(level, box, p, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
            // The arch: two pillars, a lintel of frame with a seal at its heart, flames on top.
            int t = GATE_START;
            for (int w : new int[]{-GATE_HALF - 1, GATE_HALF + 1}) {
                int x = s[0] * t + px * w, z = s[1] * t + pz * w;
                for (int y = ISLAND_Y + 1; y <= ISLAND_Y + GATE_HEIGHT + 4; y++) put(level, box, new BlockPos(x, y, z), pillar(Direction.Axis.Y));
                put(level, box, new BlockPos(x, ISLAND_Y + GATE_HEIGHT + 5, z), brazier());
            }
            for (int w = -GATE_HALF - 1; w <= GATE_HALF + 1; w++) {
                int x = s[0] * t + px * w, z = s[1] * t + pz * w;
                for (int y = ISLAND_Y + GATE_HEIGHT + 1; y <= ISLAND_Y + GATE_HEIGHT + 3; y++) {
                    put(level, box, new BlockPos(x, y, z), w == 0 && y == ISLAND_Y + GATE_HEIGHT + 2 ? seal() : frame());
                }
            }
        }
    }

    // --- anchors and the great chains -----------------------------------------------------------------

    static void anchorsAndChains(WorldGenLevel level, BoundingBox box) {
        List<int[]> vs = vertices();
        List<BlockPos> anchors = anchors();
        for (int i = 0; i < vs.size(); i++) {
            BlockPos a = anchors.get(i);
            for (BlockPos p : BlockPos.betweenClosed(a.offset(-1, -1, -1), a.offset(1, 1, 1))) {
                put(level, box, p.immutable(), p.equals(a) ? seal() : frame());
            }
            int[] v = vs.get(i);
            BlockPos from = new BlockPos((int) Math.round(v[0] * 1.15), CHAIN_Y, (int) Math.round(v[1] * 1.15));
            BlockPos to = a.offset(-Integer.signum(a.getX()) * 2, 0, -Integer.signum(a.getZ()) * 2);
            chainLine(level, box, from, to);
        }
        // The great chain the whole Cage hangs from, up into the roof.
        for (int y = CAGE_ROOF + 4; y <= 226; y++) {
            BlockPos p = new BlockPos(0, y, 0);
            if (!box.isInside(p)) continue;
            if (!level.getBlockState(p).isAir() && y > CAGE_ROOF + 30) break;
            level.setBlock(p, chain(Direction.Axis.Y), 2);
        }
    }

    /** A chain from {@code a} to {@code b}, each link turned along the run's longest axis. */
    static void chainLine(WorldGenLevel level, BoundingBox box, BlockPos a, BlockPos b) {
        int dx = Math.abs(b.getX() - a.getX()), dy = Math.abs(b.getY() - a.getY()), dz = Math.abs(b.getZ() - a.getZ());
        Direction.Axis axis = dy >= dx && dy >= dz ? Direction.Axis.Y : dx >= dz ? Direction.Axis.X : Direction.Axis.Z;
        for (BlockPos p : line(a, b)) putInAir(level, box, p, chain(axis));
    }

    // --- the Cage ---------------------------------------------------------------------------------------

    /**
     * The Cage itself, with its iris open {@code irisStep} rings (0 = sealed). The controller calls
     * {@link #iris} alone to open and close it.
     */
    static void cage(WorldGenLevel level, BoundingBox box, int irisStep) {
        for (int x = -CAGE_HALF; x <= CAGE_HALF; x++) {
            for (int z = -CAGE_HALF; z <= CAGE_HALF; z++) {
                if (!inOctagon(x, z)) continue;
                boolean edge = onOctagonEdge(x, z), vertex = isVertex(x, z);
                for (int y = CAGE_FLOOR - 3; y <= CAGE_ROOF + 4; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = cageBlock(x, z, y, edge, vertex, irisStep);
                    if (s != null) put(level, box, p, s);
                }
            }
        }
        throne(level, box);
    }

    /** What the Cage holds at one cell (null: leave the cell alone). */
    static BlockState cageBlock(int x, int z, int y, boolean edge, boolean vertex, int irisStep) {
        if (y < CAGE_FLOOR) return vertex ? pillar(Direction.Axis.Y) : null;    // hanging pinnacles
        if (y == CAGE_FLOOR) return irisBlock(x, z, irisStep);
        if (y == CAGE_ROOF) return Math.abs(x) <= 1 && Math.abs(z) <= 1 ? seal() : frame();
        if (y > CAGE_ROOF) {
            if (y == CAGE_ROOF + 1) return edge ? frame() : null;
            return vertex ? (y == CAGE_ROOF + 4 ? brazier() : pillar(Direction.Axis.Y)) : null;
        }
        if (!edge) return Blocks.AIR.defaultBlockState();
        if (vertex) return frame();
        if (isBand(y)) return seal();
        return AllBlocks.CAGE_BARS.get().defaultBlockState();
    }

    static BlockState irisBlock(int x, int z, int irisStep) {
        if (!inIris(x, z)) return frame();
        if (irisOpen(x, z, irisStep)) return Blocks.AIR.defaultBlockState();
        return irisRing(x, z) % 2 == 0 ? seal() : frame();
    }

    /** Opens or shuts the iris to {@code step} rings, in place. */
    public static void iris(WorldGenLevel level, BoundingBox box, int step) {
        for (BlockPos p : irisCells()) put(level, box, p, irisBlock(p.getX(), p.getZ(), step));
    }

    static void throne(WorldGenLevel level, BoundingBox box) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -7; z <= -6; z++) put(level, box, new BlockPos(x, CAGE_FLOOR + 1, z), bedrock());
            for (int y = CAGE_FLOOR + 1; y <= CAGE_FLOOR + 5; y++) put(level, box, new BlockPos(x, y, -8), y == CAGE_FLOOR + 5 && x == 0 ? seal() : bedrock());
        }
        for (int x : new int[]{-2, 2}) {
            for (int z = -8; z <= -6; z++) put(level, box, new BlockPos(x, CAGE_FLOOR + 2, z), frame());
        }
        put(level, box, new BlockPos(-3, CAGE_FLOOR + 1, -7), brazier());
        put(level, box, new BlockPos(3, CAGE_FLOOR + 1, -7), brazier());
    }

    /** Just the Cage itself, sealed (tests and commands that need no island). */
    public static void cageOnly(WorldGenLevel level) {
        cage(level, new BoundingBox(-CAGE_HALF - 1, CAGE_FLOOR - 4, -CAGE_HALF - 1, CAGE_HALF + 1, CAGE_ROOF + 5, CAGE_HALF + 1), 0);
    }

    /** The structure's full extent. */
    public static BoundingBox extent() {
        return new BoundingBox(-REACH, ISLAND_BOTTOM - 4, -REACH, REACH, 232, REACH);
    }
}
