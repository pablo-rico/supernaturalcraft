package org.papiricoh.supernaturalcraft.apocalypse.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Alastair's warehouse in the world (v0.40): {@link WarehousePlan} placed at {@code origin} turned {@code q} quarter turns. What
 * the trap, the fight and the cinematics use.
 */
public record WarehouseSite(BlockPos origin, int q) {

    private static final LayoutPlan[] PLANS = new LayoutPlan[4];

    public static synchronized LayoutPlan plan(int q) {
        int t = Math.floorMod(q, 4);
        if (PLANS[t] == null) PLANS[t] = WarehousePlan.plan(t);
        return PLANS[t];
    }

    public LayoutPlan plan() {
        return plan(q);
    }

    public @Nullable BlockPos point(String name) {
        LayoutPoint p = plan().points().get(name);
        return p == null ? null : origin.offset(p.x(), p.y(), p.z());
    }

    public Vec3 floor(String name) {
        BlockPos p = point(name);
        return p == null ? Vec3.atBottomCenterOf(origin) : Vec3.atBottomCenterOf(p);
    }

    public BlockPos trap() {
        BlockPos p = point(WarehouseAnchors.TRAP);
        return p == null ? origin : p;
    }

    /** Where the angels guarding him stand. */
    public List<Vec3> angels() {
        List<Vec3> out = new ArrayList<>();
        for (int n = 0; n < WarehouseAnchors.ANGELS; n++) out.add(floor(WarehouseAnchors.angel(n)));
        return out;
    }

    /** Where his demons climb out. */
    public List<Vec3> spawns() {
        List<Vec3> out = new ArrayList<>();
        for (int n = 0; ; n++) {
            BlockPos p = point(WarehouseAnchors.spawn(n));
            if (p == null) break;
            out.add(Vec3.atBottomCenterOf(p));
        }
        return out;
    }

    /** Every block of the loading doors' doorway (inclusive box). */
    public List<BlockPos> doors() {
        BlockPos a = point(WarehouseAnchors.gateMin(WarehouseAnchors.GATE_DOORS)), b = point(WarehouseAnchors.gateMax(WarehouseAnchors.GATE_DOORS));
        if (a == null || b == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }
}
