package org.papiricoh.supernaturalcraft.grave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a lonely graveyard's parts go, worked out before a block is placed (pure: testable, and
 * saved whole with its piece so it rebuilds the same in any chunk).
 *
 * <p>One to three graves stand side by side in a row. Each has a headstone at its head (one above
 * the ground) and a two-block mound of grave soil at ground level running from it in {@link Plan#dir()}.
 * Exactly one is restless: its bones lie {@link #DEPTH} blocks under the first mound cell (dig the
 * soil and the dirt under it and they are bare). One grave hides a chest under the foot of its
 * mound. Some graveyards keep a ring of old fence with a gap at the foot, some only corner posts.
 */
public final class GraveLayout {

    /** Blocks between graves along the row. */
    public static final int SPACING = 3;
    /** How far under the soil the bones and the chest lie (soil at y, dirt at y - 1, bones at y - 2). */
    public static final int DEPTH = 2;
    /** Margin between the graves and the fence. */
    public static final int MARGIN = 2;
    public static final int FENCE_NONE = 0, FENCE_POSTS = 1, FENCE_RING = 2;
    /** The tallest thing above the ground (the fence and headstone are one block; room for the cleared air). */
    public static final int HEADROOM = 3;

    /**
     * @param origin    the ground block (top solid block) at the row's centre, at the graves' heads
     * @param dir       from each headstone toward the foot of its grave
     * @param count     graves in the row (1–3)
     * @param restless  which grave holds the restless bones
     * @param chestGrave which grave hides the chest
     * @param fenceKind {@link #FENCE_NONE}, {@link #FENCE_POSTS} or {@link #FENCE_RING}
     */
    public record Plan(BlockPos origin, Direction dir, int count, int restless, int chestGrave, int fenceKind, long seed) {

        public Direction right() {
            return dir.getClockWise();
        }

        /** Offset of grave {@code i} along the row. */
        public int offset(int i) {
            return i * SPACING - ((count - 1) * SPACING) / 2;
        }

        /** Where grave {@code i}'s headstone stands (one above the ground). */
        public BlockPos headstone(int i) {
            return origin.relative(right(), offset(i)).above();
        }

        /** Grave {@code i}'s two mound cells at ground level, head first. */
        public List<BlockPos> mound(int i) {
            BlockPos head = origin.relative(right(), offset(i));
            return List.of(head.relative(dir, 1), head.relative(dir, 2));
        }

        public boolean isRestless(int i) {
            return i == restless;
        }

        /** The restless bones, under the head of their mound. */
        public BlockPos bones() {
            return mound(restless).get(0).below(DEPTH);
        }

        /** The buried chest, under the foot of its mound. */
        public BlockPos chest() {
            return mound(chestGrave).get(1).below(DEPTH);
        }

        /** Headstones face the foot of their grave, where a mourner would stand. */
        public Direction headstoneFacing() {
            return dir;
        }

        /** Fence cells (one above the ground), in a fixed order. */
        public List<BlockPos> fenceCells() {
            List<BlockPos> out = new ArrayList<>();
            if (fenceKind == FENCE_NONE) return out;
            int r0 = offset(0) - MARGIN, r1 = offset(count - 1) + MARGIN, d0 = -MARGIN, d1 = 2 + MARGIN;
            RandomSource random = RandomSource.create(seed ^ 0x5EEDFE11CEL);
            for (int r = r0; r <= r1; r++) {
                for (int d = d0; d <= d1; d++) {
                    boolean edgeR = r == r0 || r == r1, edgeD = d == d0 || d == d1;
                    if (!edgeR && !edgeD) continue;
                    boolean corner = edgeR && edgeD;
                    if (fenceKind == FENCE_POSTS && !corner) continue;
                    if (fenceKind == FENCE_RING) {
                        // The gate: a gap at the foot of the row, in the middle.
                        if (d == d1 && Math.abs(r) <= 1) continue;
                        // Time has taken a few panels (never a corner).
                        if (!corner && random.nextInt(6) == 0) continue;
                    }
                    out.add(origin.relative(right(), r).relative(dir, d).above());
                }
            }
            return out;
        }

        /** Everything the builder may touch, from the buried chest to the cleared air. */
        public BoundingBox fullBox() {
            BoundingBox flat = footprint();
            return new BoundingBox(flat.minX(), origin.getY() - DEPTH - 1, flat.minZ(), flat.maxX(), origin.getY() + HEADROOM, flat.maxZ());
        }

        /**
         * The structure piece's box. Its floor is the first layer above the ground: a thin beard
         * levels the terrain to it, so the ground under the graves is filled in and nothing is carved
         * from it (the buried blocks lie under the box, still within its chunks).
         */
        public BoundingBox pieceBox() {
            BoundingBox flat = footprint();
            return new BoundingBox(flat.minX(), origin.getY() + 1, flat.minZ(), flat.maxX(), origin.getY() + HEADROOM, flat.maxZ());
        }

        /** The horizontal extent: the fence ring (even when there is none), at ground level. */
        private BoundingBox footprint() {
            BlockPos a = origin.relative(right(), offset(0) - MARGIN).relative(dir, -MARGIN);
            BlockPos b = origin.relative(right(), offset(count - 1) + MARGIN).relative(dir, 2 + MARGIN);
            return BoundingBox.fromCorners(a, b);
        }
    }

    private GraveLayout() {
    }

    /** Plans a graveyard around the ground block {@code origin}. */
    public static Plan plan(BlockPos origin, long seed) {
        RandomSource random = RandomSource.create(seed);
        Direction dir = Direction.from2DDataValue(random.nextInt(4));
        int count = 1 + random.nextInt(3);
        int restless = random.nextInt(count);
        int chest = random.nextInt(count);
        int fence = random.nextInt(3);
        return new Plan(origin.immutable(), dir, count, restless, chest, fence, seed);
    }

    /**
     * Whether ground sampled at a few points (centre first) is flat and dry enough for graves:
     * tops within {@code 2} of each other, and each one solid ground rather than a water surface
     * ({@code tops[i] == floors[i]}), never below the sea.
     */
    public static boolean suitable(int[] tops, int[] floors, int seaLevel) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int i = 0; i < tops.length; i++) {
            if (tops[i] != floors[i]) return false;
            min = Math.min(min, tops[i]);
            max = Math.max(max, tops[i]);
        }
        return max - min <= 2 && tops[0] >= seaLevel;
    }
}
