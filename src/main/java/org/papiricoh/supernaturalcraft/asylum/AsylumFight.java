package org.papiricoh.supernaturalcraft.asylum;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumAnchors;
import org.papiricoh.supernaturalcraft.asylum.layout.AsylumPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * The asylum's anchors in the world (v0.35): {@link AsylumPlan} placed at {@code origin} turned {@code q} quarter turns. What
 * Crowley's fight uses to walk from room to room. Pure but for Minecraft's value types.
 */
public record AsylumFight(BlockPos origin, int q) {

    public LayoutPlan plan() {
        return AsylumPlan.plan(q);
    }

    public @Nullable BlockPos point(String name) {
        LayoutPoint p = plan().points().get(name);
        return p == null ? null : origin.offset(p.x(), p.y(), p.z());
    }

    public Vec3 floor(String name) {
        BlockPos p = point(name);
        return p == null ? Vec3.atBottomCenterOf(origin) : Vec3.atBottomCenterOf(p);
    }

    /** The room of {@code phase} (1 hall, 2 cells, 3 throne). */
    public static String room(int phase) {
        return AsylumAnchors.ROOMS.get(Math.max(0, Math.min(2, phase - 1)));
    }

    public Vec3 roomCentre(int phase) {
        return floor(room(phase));
    }

    public List<Vec3> spawns(int phase) {
        List<Vec3> out = new ArrayList<>();
        LayoutPlan plan = plan();
        for (int n = 0; ; n++) {
            LayoutPoint p = plan.points().get(AsylumAnchors.spawn(room(phase), n));
            if (p == null) break;
            out.add(Vec3.atBottomCenterOf(origin.offset(p.x(), p.y(), p.z())));
        }
        return out;
    }

    /** Every block of a gate's doorway (inclusive box). */
    public List<BlockPos> gate(String gate) {
        BlockPos a = point(AsylumAnchors.gateMin(gate)), b = point(AsylumAnchors.gateMax(gate));
        if (a == null || b == null) return List.of();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(a, b)) out.add(p.immutable());
        return out;
    }

    /** The gate that opens when the fight reaches {@code phase} (null for phase 1). */
    public static @Nullable String opensAt(int phase) {
        return phase == 2 ? AsylumAnchors.GATE_CELLS : phase >= 3 ? AsylumAnchors.GATE_THRONE : null;
    }
}
