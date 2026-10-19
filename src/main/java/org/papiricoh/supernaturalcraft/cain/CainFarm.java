package org.papiricoh.supernaturalcraft.cain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.cain.layout.CainFarmLayout;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Cain's farm in the world (v0.21): its plan for each turn (one design, drawn once per turn), its boxes, where the plan's
 * points fall, and building it all at once (commands, older worlds).
 */
public final class CainFarm {

    private static final LayoutPlan[] PLANS = new LayoutPlan[4];

    private CainFarm() {
    }

    public static LayoutPlan plan(int quarter) {
        int q = Math.floorMod(quarter, 4);
        synchronized (PLANS) {
            if (PLANS[q] == null) PLANS[q] = CainFarmLayout.plan(q);
            return PLANS[q];
        }
    }

    public static BoundingBox pieceBox(BlockPos origin, int quarter) {
        return PlanWriter.pieceBox(origin, plan(quarter), CainFarmLayout.HEIGHT);
    }

    public static BoundingBox fullBox(BlockPos origin, int quarter) {
        return PlanWriter.fullBox(origin, plan(quarter), CainFarmLayout.HEIGHT);
    }

    /** A point of the plan (drawn for turn 0) in the world, for a farm at {@code origin} turned {@code quarter}. */
    public static BlockPos at(BlockPos origin, int quarter, LayoutPoint p) {
        LayoutPoint r = CainFarmLayout.at(p, quarter);
        return origin.offset(r.x(), r.y(), r.z());
    }

    public static BlockPos at(CainSite.Site site, LayoutPoint p) {
        return at(site.origin(), site.rotation(), p);
    }

    /** His hives, in the world. */
    public static List<BlockPos> hives(CainSite.Site site) {
        List<BlockPos> out = new ArrayList<>();
        for (LayoutPoint p : CainFarmLayout.HIVES) out.add(at(site, p));
        return out;
    }

    /** The origin of a farm on the ground at column ({@code x}, {@code z}). */
    public static BlockPos originOn(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Builds the whole farm now. */
    public static void placeAt(ServerLevel level, BlockPos origin, int quarter) {
        BoundingBox box = fullBox(origin, quarter);
        PlanWriter.load(level, box);
        PlanWriter.build(level, box, origin, plan(quarter));
    }
}
