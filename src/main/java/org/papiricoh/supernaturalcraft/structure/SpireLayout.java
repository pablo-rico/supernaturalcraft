package org.papiricoh.supernaturalcraft.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.papiricoh.supernaturalcraft.chorus.Melody;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a Hymnal Spire's parts go, worked out before a single block is placed (pure: testable,
 * and saved with the pieces so a reloaded piece builds exactly the same):
 *
 * <ul>
 *   <li>the <b>summit</b>: a ruined platform of {@link #PLATFORM_RADIUS} hovering just over the peak,
 *   the Choir Altar at its heart and seven bells around it;</li>
 *   <li>the <b>ascent</b>: a stair that spirals down from the platform's edge around the mountain,
 *   never climbing and never dropping more than a block per step — bridged where the slope falls
 *   away, tunnelled where it rises;</li>
 *   <li>the <b>temple</b>: a small hall beside the stair, part way down, whose window gives the
 *   hymn away.</li>
 * </ul>
 */
public final class SpireLayout {

    public static final int PLATFORM_RADIUS = 22, LIFT = 4;
    /** Pieces must stay this close to the start chunk, or the chunk generator never places them. */
    public static final int MAX_REACH = 112;
    public static final int TEMPLE_WIDTH = 11, TEMPLE_DEPTH = 13, TEMPLE_HEIGHT = 9;
    private static final double STEP = 2.0, OUTWARD = 0.32;
    private static final int MAX_NODES = 150;

    /** Altar position (one above the platform), the hymn, the ascent's nodes, and the temple. */
    public record Plan(BlockPos altar, byte[] melody, List<BlockPos> path, BlockPos templeOrigin, Direction templeFacing, long seed) {
    }

    private SpireLayout() {
    }

    /** Plans a spire on the peak at (x, peakY, z). */
    public static Plan plan(HeightSampler h, int x, int peakY, int z, long seed) {
        RandomSource random = RandomSource.create(seed);
        byte[] melody = Melody.generate(random);
        int floor = peakY + LIFT;
        BlockPos altar = new BlockPos(x, floor + 1, z);

        List<BlockPos> path = new ArrayList<>();
        double angle = random.nextDouble() * Math.PI * 2;
        double r = PLATFORM_RADIUS + 1.5;
        int y = floor;
        int lastX = Integer.MIN_VALUE, lastZ = Integer.MIN_VALUE;
        for (int i = 0; i < MAX_NODES; i++) {
            int px = x + (int) Math.round(Math.cos(angle) * r), pz = z + (int) Math.round(Math.sin(angle) * r);
            if (px != lastX || pz != lastZ) {
                // Down with the ground, a block a step at most; never up.
                int ground = h.top(px, pz) + 1;
                if (!path.isEmpty()) y = Mth.clamp(ground, y - 1, y);
                path.add(new BlockPos(px, y, pz));
                lastX = px;
                lastZ = pz;
            }
            angle += STEP / r;
            r += OUTWARD;
            if (r > MAX_REACH - 60) break;
            if (floor - y >= 40 && Math.abs(h.top(px, pz) + 1 - y) <= 1 && i > 40) break;
        }

        // The temple stands beside the stair a third of the way down, its door toward the path.
        BlockPos at = path.get(Math.min(path.size() - 1, Math.max(4, path.size() / 3)));
        double ox = at.getX() - x, oz = at.getZ() - z, len = Math.max(1, Math.sqrt(ox * ox + oz * oz));
        Direction outward = Direction.getNearest((float) (ox / len), 0, (float) (oz / len));
        BlockPos door = at.relative(outward, 3);
        Direction facing = outward.getOpposite();
        return new Plan(altar, melody, List.copyOf(path), door, facing, seed);
    }

    public static net.minecraft.world.level.levelgen.structure.BoundingBox summitBox(Plan plan) {
        BlockPos a = plan.altar();
        int r = PLATFORM_RADIUS + 1, floor = floorY(plan);
        return new net.minecraft.world.level.levelgen.structure.BoundingBox(a.getX() - r, floor - 46, a.getZ() - r, a.getX() + r, floor + 17, a.getZ() + r);
    }

    public static net.minecraft.world.level.levelgen.structure.BoundingBox ascentBox(Plan plan) {
        var b = net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(plan.path()).orElseThrow();
        return new net.minecraft.world.level.levelgen.structure.BoundingBox(b.minX() - 3, b.minY() - 50, b.minZ() - 3, b.maxX() + 3, b.maxY() + 5, b.maxZ() + 3);
    }

    public static net.minecraft.world.level.levelgen.structure.BoundingBox templeBox(Plan plan) {
        BlockPos d = plan.templeOrigin();
        int r = TEMPLE_DEPTH + 1;
        return new net.minecraft.world.level.levelgen.structure.BoundingBox(d.getX() - r, d.getY() - 42, d.getZ() - r, d.getX() + r,
                d.getY() + TEMPLE_HEIGHT + 1, d.getZ() + r);
    }

    /** The floor of the summit platform (its top layer). */
    public static int floorY(Plan plan) {
        return plan.altar().getY() - 1;
    }
}
