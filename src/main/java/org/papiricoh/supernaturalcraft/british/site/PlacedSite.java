package org.papiricoh.supernaturalcraft.british.site;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A {@link SiteSpec} placed in the world (v0.41): its plan at {@code origin} turned {@code q} quarter turns. */
public record PlacedSite(SiteSpec spec, BlockPos origin, int q) {

    private static final Map<String, LayoutPlan> PLANS = new ConcurrentHashMap<>();

    public static LayoutPlan plan(SiteSpec spec, int q) {
        int t = Math.floorMod(q, 4);
        return PLANS.computeIfAbsent(spec.id() + "#" + t, k -> spec.plan().apply(t));
    }

    public LayoutPlan plan() {
        return plan(spec, q);
    }

    public @Nullable BlockPos point(String name) {
        LayoutPoint p = plan().points().get(name);
        return p == null ? null : origin.offset(p.x(), p.y(), p.z());
    }

    /** The bottom centre of point {@code name} (the origin's if the plan lacks it). */
    public Vec3 floor(String name) {
        BlockPos p = point(name);
        return p == null ? Vec3.atBottomCenterOf(origin) : Vec3.atBottomCenterOf(p);
    }

    /** {@code prefix + n} for every n from 0 while the plan has them. */
    public List<Vec3> series(String prefix) {
        List<Vec3> out = new ArrayList<>();
        for (int n = 0; ; n++) {
            BlockPos p = point(prefix + n);
            if (p == null) break;
            out.add(Vec3.atBottomCenterOf(p));
        }
        return out;
    }

    /** Every block of the inclusive box between {@code <gate>_min} and {@code <gate>_max}. */
    public List<BlockPos> box(String gate) {
        BlockPos a = point(gate + "_min"), b = point(gate + "_max");
        if (a == null || b == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }
}
