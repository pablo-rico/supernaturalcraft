package org.papiricoh.supernaturalcraft.coven.rowena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.papiricoh.supernaturalcraft.coven.PlanWriter;
import org.papiricoh.supernaturalcraft.coven.layout.RowenaHouseLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

/**
 * Rowena's house in the world (v0.21): its plan for each turn (the design is fixed, so each is drawn once), its boxes, where
 * the plan's points fall, and building it all at once (commands, older worlds).
 */
public final class RowenaHouse {

    private static final LayoutPlan[] PLANS = new LayoutPlan[4];

    private RowenaHouse() {
    }

    public static LayoutPlan plan(int quarter) {
        int q = Math.floorMod(quarter, 4);
        synchronized (PLANS) {
            if (PLANS[q] == null) PLANS[q] = RowenaHouseLayout.plan(q);
            return PLANS[q];
        }
    }

    public static BoundingBox pieceBox(BlockPos origin, int quarter) {
        return PlanWriter.pieceBox(origin, plan(quarter), RowenaHouseLayout.HEIGHT);
    }

    public static BoundingBox fullBox(BlockPos origin, int quarter) {
        return PlanWriter.fullBox(origin, plan(quarter), RowenaHouseLayout.HEIGHT);
    }

    /** A point of the plan (drawn facing south) in the world, for a house at {@code origin} turned {@code quarter}. */
    public static BlockPos at(BlockPos origin, int quarter, LayoutPoint p) {
        LayoutPoint r = RowenaHouseLayout.at(p, quarter);
        return origin.offset(r.x(), r.y(), r.z());
    }

    public static BlockPos at(RowenaSite.Site site, LayoutPoint p) {
        return at(site.origin(), site.rotation(), p);
    }

    /** The origin of a house standing on the ground at column ({@code x}, {@code z}). */
    public static BlockPos originOn(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Builds the whole house now. */
    public static void placeAt(ServerLevel level, BlockPos origin, int quarter) {
        BoundingBox box = fullBox(origin, quarter);
        PlanWriter.load(level, box);
        PlanWriter.build(level, box, origin, plan(quarter));
    }
}
