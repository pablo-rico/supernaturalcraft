package org.papiricoh.supernaturalcraft.apocworld.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;

/**
 * What is left of the people who lived here (v0.41): the shell of a burned house (soot-black brick walls, broken down at the top,
 * a charred floor, no roof), a car burned out on its rims, a stretch of broken wall, a heap of rubble. One of them where it lands,
 * on fairly level ground; never more than a few blocks out from its origin (so a feature never writes past its neighbours).
 */
public class AuRuinFeature extends Feature<NoneFeatureConfiguration> {

    public enum Kind { HOUSE, CAR, WALL, RUBBLE }

    public AuRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** Which ruin a roll picks (pure, tests). */
    public static Kind pick(int roll) {
        int k = Math.floorMod(roll, 10);
        return k < 3 ? Kind.HOUSE : k < 6 ? Kind.CAR : k < 8 ? Kind.WALL : Kind.RUBBLE;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource r = ctx.random();
        BlockPos at = ctx.origin();
        if (!level.getBlockState(at.below()).isSolidRender(level, at.below())) return false;
        Kind kind = pick(r.nextInt(10));
        return switch (kind) {
            case HOUSE -> house(level, at, r);
            case CAR -> car(level, at, r);
            case WALL -> wall(level, at, r);
            case RUBBLE -> rubble(level, at, r);
        };
    }

    private static int ground(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    private static boolean level(WorldGenLevel level, BlockPos at, int half) {
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int dx = -half; dx <= half; dx += half) {
            for (int dz = -half; dz <= half; dz += half) {
                int y = ground(level, at.getX() + dx, at.getZ() + dz);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        return hi - lo <= 2;
    }

    private static void put(WorldGenLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    private static boolean house(WorldGenLevel level, BlockPos at, RandomSource r) {
        int w = 2 + r.nextInt(2), d = 2 + r.nextInt(2);
        if (!level(level, at, Math.max(w, d))) return false;
        int y0 = at.getY();
        BlockState brick = ApocWorldRegistry.RUINED_BRICKS.get().defaultBlockState();
        BlockState planks = ApocWorldRegistry.BURNED_PLANKS.get().defaultBlockState();
        BlockState rubble = ApocWorldRegistry.RUBBLE.get().defaultBlockState();
        Direction door = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        for (int dx = -w; dx <= w; dx++) {
            for (int dz = -d; dz <= d; dz++) {
                BlockPos floor = new BlockPos(at.getX() + dx, y0 - 1, at.getZ() + dz);
                // Footing under the floor where the land dips.
                for (int y = y0 - 2; y >= y0 - 4 && level.getBlockState(new BlockPos(floor.getX(), y, floor.getZ())).canBeReplaced(); y--) {
                    put(level, new BlockPos(floor.getX(), y, floor.getZ()), rubble);
                }
                put(level, floor, r.nextInt(5) == 0 ? rubble : planks);
                boolean edge = Math.abs(dx) == w || Math.abs(dz) == d;
                for (int y = 0; y < 5; y++) {
                    BlockPos p = floor.above(1 + y);
                    if (!edge) {
                        put(level, p, Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    // The walls burn down unevenly: higher at the corners, gaps for windows and the door.
                    boolean corner = Math.abs(dx) == w && Math.abs(dz) == d;
                    int top = corner ? 4 : 1 + r.nextInt(4);
                    boolean doorway = y < 2 && (door.getStepX() != 0 ? dx == door.getStepX() * w && dz == 0 : dz == door.getStepZ() * d && dx == 0);
                    boolean window = y == 2 && !corner && r.nextInt(3) == 0;
                    put(level, p, y < top && !doorway && !window ? brick : Blocks.AIR.defaultBlockState());
                }
            }
        }
        // Fallen roof beams and ash inside.
        for (int i = 0; i < 3; i++) {
            BlockPos p = new BlockPos(at.getX() - w + 1 + r.nextInt(2 * w - 1), y0, at.getZ() - d + 1 + r.nextInt(2 * d - 1));
            put(level, p, r.nextBoolean() ? ApocWorldRegistry.BURNED_LOG.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, r.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z)
                    : ApocWorldRegistry.ASH_LAYER.get().defaultBlockState());
        }
        return true;
    }

    private static boolean car(WorldGenLevel level, BlockPos at, RandomSource r) {
        if (!level(level, at, 2)) return false;
        boolean alongX = r.nextBoolean();
        BlockState metal = ApocWorldRegistry.WRECKED_METAL.get().defaultBlockState();
        // The body: four long, two wide; the cab a block higher in the middle; burnt-out wheels (coal) at the corners.
        for (int i = -2; i <= 1; i++) {
            for (int j = 0; j <= 1; j++) {
                BlockPos p = alongX ? at.offset(i, 0, j) : at.offset(j, 0, i);
                put(level, p, metal);
                if (i == -1 || i == 0) {
                    put(level, p.above(), r.nextInt(4) == 0 ? Blocks.AIR.defaultBlockState() : metal);
                }
            }
        }
        for (int[] w : new int[][]{{-2, -1}, {1, -1}, {-2, 2}, {1, 2}}) {
            BlockPos p = alongX ? at.offset(w[0], 0, w[1]) : at.offset(w[1], 0, w[0]);
            if (level.getBlockState(p).canBeReplaced()) put(level, p, Blocks.COAL_BLOCK.defaultBlockState());
        }
        BlockPos ash = at.offset(alongX ? 2 : 0, 0, alongX ? 0 : 2);
        if (level.getBlockState(ash).canBeReplaced()) put(level, ash, ApocWorldRegistry.ASH_LAYER.get().defaultBlockState());
        return true;
    }

    private static boolean wall(WorldGenLevel level, BlockPos at, RandomSource r) {
        boolean alongX = r.nextBoolean();
        int len = 3 + r.nextInt(5);
        for (int i = -len / 2; i <= len / 2; i++) {
            BlockPos base = alongX ? at.offset(i, 0, 0) : at.offset(0, 0, i);
            int g = ground(level, base.getX(), base.getZ());
            int h = 1 + r.nextInt(3) + (Math.abs(i) < len / 4 ? 1 : 0);
            for (int y = 0; y < h; y++) {
                BlockPos p = new BlockPos(base.getX(), g + y, base.getZ());
                put(level, p, (r.nextInt(4) == 0 ? ApocWorldRegistry.RUBBLE : ApocWorldRegistry.RUINED_BRICKS).get().defaultBlockState());
            }
        }
        return true;
    }

    private static boolean rubble(WorldGenLevel level, BlockPos at, RandomSource r) {
        int n = 6 + r.nextInt(8);
        for (int i = 0; i < n; i++) {
            int dx = r.nextInt(5) - 2, dz = r.nextInt(5) - 2;
            int g = ground(level, at.getX() + dx, at.getZ() + dz);
            BlockPos p = new BlockPos(at.getX() + dx, g, at.getZ() + dz);
            put(level, p, (r.nextInt(3) == 0 ? ApocWorldRegistry.RUINED_BRICKS : ApocWorldRegistry.RUBBLE).get().defaultBlockState());
        }
        return true;
    }
}
