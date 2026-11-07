package org.papiricoh.supernaturalcraft.pact;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.pact.layout.LabPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Crowley's lab in the world (v0.39): {@link LabPlan} placed at {@code origin} turned {@code q} quarter turns. What the rite,
 * Godstiel's fight and the cinematics use. Pure but for Minecraft's value types.
 */
public record LabSite(BlockPos origin, int q) {

    private static final LayoutPlan[] PLANS = new LayoutPlan[4];

    /** The plan in turn {@code q} (built once per turn). */
    public static synchronized LayoutPlan plan(int q) {
        int t = Math.floorMod(q, 4);
        if (PLANS[t] == null) PLANS[t] = LabPlan.plan(t);
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

    /** Spawn points of {@code room} ({@code hall} or {@code deck}). */
    public List<Vec3> spawns(String room) {
        List<Vec3> out = new ArrayList<>();
        for (int n = 0; ; n++) {
            BlockPos p = point(LabAnchors.spawn(room, n));
            if (p == null) break;
            out.add(Vec3.atBottomCenterOf(p));
        }
        return out;
    }

    /** Every block of the big doors' doorway (inclusive box). */
    public List<BlockPos> doors() {
        return box(LabAnchors.gateMin(LabAnchors.GATE_DOORS), LabAnchors.gateMax(LabAnchors.GATE_DOORS));
    }

    /** Every block of the reservoir's water (inclusive box; what is water there is the plan's). */
    public List<BlockPos> water() {
        return box(LabAnchors.WATER_MIN, LabAnchors.WATER_MAX);
    }

    private List<BlockPos> box(String lo, String hi) {
        BlockPos a = point(lo), b = point(hi);
        if (a == null || b == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }

    /** Whether {@code pos} is within {@code r} blocks of the rite's altar spot (horizontally) and near its floor. */
    public boolean atRite(BlockPos pos, int r) {
        BlockPos rite = point(LabAnchors.RITE);
        return rite != null && Math.abs(pos.getX() - rite.getX()) <= r && Math.abs(pos.getZ() - rite.getZ()) <= r
                && Math.abs(pos.getY() - rite.getY()) <= 3;
    }
}
