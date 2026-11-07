package org.papiricoh.supernaturalcraft.apocworld.camp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.army.AuPrisonerEntity;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.ArenaCell;
import org.papiricoh.supernaturalcraft.entity.boss.horsemen.arena.HorsemenGround;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.Map;

/**
 * Puts one of Michael's outposts ({@link CampPlan}) into the world round an origin (its middle, {@code y} = the first air layer
 * over the ground), touching only what lies inside the box it is given, then peoples it: a soldier at each guard post, a
 * prisoner in each cage. Every entity is put down by the chunk its anchor lies in, so each comes once.
 */
public final class CampBuilder {

    private CampBuilder() {
    }

    public static LayoutPlan plan(long seed) {
        return CampPlan.plan(CampPlan.kindOf(seed), CampPlan.turnOf(seed));
    }

    /** Everything the plan touches round {@code origin}. */
    public static BoundingBox fullBox(BlockPos origin, int q) {
        int[] a = CampPlan.rotate(CampAnchors.MIN_X, CampAnchors.MIN_Z, q), b = CampPlan.rotate(CampAnchors.MAX_X, CampAnchors.MAX_Z, q);
        return new BoundingBox(origin.getX() + Math.min(a[0], b[0]) - 1, origin.getY() + CampAnchors.BOTTOM_Y - 4, origin.getZ() + Math.min(a[1], b[1]) - 1,
                origin.getX() + Math.max(a[0], b[0]) + 1, origin.getY() + CampAnchors.TOP_Y, origin.getZ() + Math.max(a[1], b[1]) + 1);
    }

    /** Builds an outpost on the ground at {@code near} (commands, previews and tests). @return its origin */
    public static BlockPos placeDirect(ServerLevel level, BlockPos near, long seed) {
        BoundingBox reach = fullBox(near, CampPlan.turnOf(seed));
        for (int cx = reach.minX() >> 4; cx <= reach.maxX() >> 4; cx++) {
            for (int cz = reach.minZ() >> 4; cz <= reach.maxZ() >> 4; cz++) level.getChunk(cx, cz);
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.getX(), near.getZ()) - 1;
        BlockPos origin = new BlockPos(near.getX(), ground + 1, near.getZ());
        build(level, fullBox(origin, CampPlan.turnOf(seed)), origin, seed, plan(seed));
        return origin;
    }

    /** Builds a camp (never a checkpoint) right at {@code origin}, whatever the seed says (tests). */
    public static void placeCamp(ServerLevel level, BlockPos origin, int q) {
        LayoutPlan plan = CampPlan.plan(CampPlan.Kind.CAMP, q);
        BoundingBox box = fullBox(origin, q);
        write(level, box, origin, plan);
        people(level, box, origin, CampPlan.Kind.CAMP, plan);
    }

    public static void build(WorldGenLevel level, BoundingBox box, BlockPos origin, long seed, LayoutPlan plan) {
        write(level, box, origin, plan);
        people(level, box, origin, CampPlan.kindOf(seed), plan);
    }

    private static void write(WorldGenLevel level, BoundingBox box, BlockPos origin, LayoutPlan plan) {
        BlockState footing = ApocWorldRegistry.SCORCHED_STONE.get().defaultBlockState();
        for (ArenaCell cell : plan.cells()) {
            BlockPos p = origin.offset(cell.dx(), cell.dy(), cell.dz());
            if (!box.isInside(p)) continue;
            level.setBlock(p, HorsemenGround.state(cell.block()), Block.UPDATE_CLIENTS);
            // Footings under the lowest layer where the land dips away.
            if (cell.dy() == CampAnchors.BOTTOM_Y) {
                for (int y = p.getY() - 1; y > p.getY() - 6 && y >= level.getMinBuildHeight(); y--) {
                    BlockPos q = new BlockPos(p.getX(), y, p.getZ());
                    if (!level.getBlockState(q).canBeReplaced()) break;
                    level.setBlock(q, footing, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static void people(WorldGenLevel level, BoundingBox box, BlockPos origin, CampPlan.Kind kind, LayoutPlan plan) {
        ServerLevel server = level.getLevel();
        for (Map.Entry<String, LayoutPoint> e : plan.points().entrySet()) {
            String name = e.getKey();
            LayoutPoint pt = e.getValue();
            BlockPos at = origin.offset(pt.x(), pt.y(), pt.z());
            if (!box.isInside(at)) continue;
            if (name.startsWith(CampAnchors.GUARD_PREFIX)) {
                AuSoldierEntity s = ApocWorldRegistry.AU_ANGEL_SOLDIER.get().create(server);
                if (s == null) continue;
                put(level, s, at);
                s.guard(at, kind == CampPlan.Kind.CAMP ? origin : null);
                level.addFreshEntity(s);
            } else if (name.startsWith(CampAnchors.PRISONER_PREFIX)) {
                AuPrisonerEntity p = ApocWorldRegistry.AU_PRISONER.get().create(server);
                if (p == null) continue;
                put(level, p, at);
                p.holdIn(origin);
                level.addFreshEntity(p);
            }
        }
    }

    private static void put(ServerLevelAccessor level, net.minecraft.world.entity.Mob mob, BlockPos at) {
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, mob.getRandom().nextFloat() * 360f, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
    }
}
