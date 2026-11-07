package org.papiricoh.supernaturalcraft.apocworld.fortress;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Michael's fortress in the world (v0.41): {@link FortressPlan} placed at {@code origin} turned {@code q} quarter turns. What the
 * fight, the rift and the cinematics use.
 */
public record FortressSite(BlockPos origin, int q) {

    private static final LayoutPlan[] PLANS = new LayoutPlan[4];

    public static synchronized LayoutPlan plan(int q) {
        int t = Math.floorMod(q, 4);
        if (PLANS[t] == null) PLANS[t] = FortressPlan.plan(t);
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

    /** Where he stands before his throne. */
    public BlockPos throne() {
        BlockPos p = point(FortressAnchors.THRONE);
        return p == null ? origin : p;
    }

    public BlockPos centre() {
        BlockPos p = point(FortressAnchors.ARENA_CENTRE);
        return p == null ? origin : p;
    }

    public BlockPos rift() {
        BlockPos p = point(FortressAnchors.RIFT);
        return p == null ? throne() : p;
    }

    /** Which way the court faces from the throne (toward the gate), as a yaw. */
    public float throneYaw() {
        Vec3 t = Vec3.atBottomCenterOf(throne()), c = Vec3.atBottomCenterOf(centre());
        return (float) Math.toDegrees(Math.atan2(-(c.x - t.x), c.z - t.z));
    }

    /** Where his soldiers come in. */
    public List<Vec3> spawns() {
        List<Vec3> out = new ArrayList<>();
        for (int n = 0; ; n++) {
            BlockPos p = point(FortressAnchors.spawn(n));
            if (p == null) break;
            out.add(Vec3.atBottomCenterOf(p));
        }
        return out;
    }

    /** Every block of the gate's opening (inclusive box). */
    public List<BlockPos> gate() {
        BlockPos a = point(FortressAnchors.gateMin(FortressAnchors.GATE)), b = point(FortressAnchors.gateMax(FortressAnchors.GATE));
        if (a == null || b == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }
}
