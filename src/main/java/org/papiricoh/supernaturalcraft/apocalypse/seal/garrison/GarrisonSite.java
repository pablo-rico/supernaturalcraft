package org.papiricoh.supernaturalcraft.apocalypse.seal.garrison;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Uriel's garrison in the world (v0.40): {@link GarrisonPlan} placed at {@code origin} turned {@code q} quarter turns. Pure but for
 * Minecraft's value types.
 */
public record GarrisonSite(BlockPos origin, int q) {

    private static final LayoutPlan[] PLANS = new LayoutPlan[4];

    public static synchronized LayoutPlan plan(int q) {
        int t = Math.floorMod(q, 4);
        if (PLANS[t] == null) PLANS[t] = GarrisonPlan.plan(t);
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

    /** The rebels' spawn points. */
    public List<Vec3> spawns() {
        List<Vec3> out = new ArrayList<>();
        for (int n = 0; ; n++) {
            BlockPos p = point(GarrisonAnchors.spawn(n));
            if (p == null) break;
            out.add(Vec3.atBottomCenterOf(p));
        }
        return out;
    }

    /** Every block of the gateway (inclusive box). */
    public List<BlockPos> gate() {
        BlockPos a = point(GarrisonAnchors.gateMin(GarrisonAnchors.GATE)), b = point(GarrisonAnchors.gateMax(GarrisonAnchors.GATE));
        if (a == null || b == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }

    /** Whether {@code pos} is within {@code r} blocks of Uriel's spot (horizontally) and near its floor. */
    public boolean near(Vec3 pos, double r) {
        Vec3 u = floor(GarrisonAnchors.URIEL);
        double dx = pos.x - u.x, dz = pos.z - u.z;
        return dx * dx + dz * dz <= r * r && Math.abs(pos.y - u.y) <= 6;
    }
}
