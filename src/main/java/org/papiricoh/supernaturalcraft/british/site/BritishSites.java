package org.papiricoh.supernaturalcraft.british.site;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.british.compound.CompoundAnchors;
import org.papiricoh.supernaturalcraft.british.compound.CompoundPlan;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehouseAnchors;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehousePlan;

/**
 * Season 12's two unique places (v0.41): the British Men of Letters' compound (Hess's HQ, nearer the world's centre, which the
 * dossier Ketch carried points at) and Kelly Kline's safe house (a remote cabin far out, which the nephilim's trace finds).
 */
public final class BritishSites {

    public static final SiteSpec COMPOUND = new SiteSpec("compound", 0xB817_0041L,
            CompoundAnchors.MIN_X, CompoundAnchors.MAX_X, CompoundAnchors.MIN_Z, CompoundAnchors.MAX_Z,
            CompoundAnchors.BOTTOM_Y, CompoundAnchors.GROUND_Y, CompoundAnchors.TOP_Y, 1100, 140, 16, 6, CompoundPlan::plan);

    public static final SiteSpec SAFEHOUSE = new SiteSpec("safehouse", 0x5AFE_0041L,
            SafehouseAnchors.MIN_X, SafehouseAnchors.MAX_X, SafehouseAnchors.MIN_Z, SafehouseAnchors.MAX_Z,
            SafehouseAnchors.BOTTOM_Y, SafehouseAnchors.GROUND_Y, SafehouseAnchors.TOP_Y, 1500, 160, 16, 7, SafehousePlan::plan);

    private BritishSites() {
    }

    public static @Nullable PlacedSite compound(MinecraftServer server) {
        return SiteBuilder.site(server, COMPOUND);
    }

    public static @Nullable PlacedSite safehouse(MinecraftServer server) {
        return SiteBuilder.site(server, SAFEHOUSE);
    }

    /** Raises the compound if it is not yet; @return where it stands (Hess's war room) */
    public static BlockPos ensureCompound(ServerLevel overworld) {
        SiteBuilder.ensure(overworld, COMPOUND);
        PlacedSite s = compound(overworld.getServer());
        BlockPos p = s == null ? null : s.point(CompoundAnchors.HESS);
        return p != null ? p : s != null ? s.origin() : BlockPos.ZERO;
    }

    /** Raises the safe house if it is not yet; @return where it stands (Dagon's post) */
    public static BlockPos ensureSafehouse(ServerLevel overworld) {
        SiteBuilder.ensure(overworld, SAFEHOUSE);
        PlacedSite s = safehouse(overworld.getServer());
        BlockPos p = s == null ? null : s.point(SafehouseAnchors.GUARD);
        return p != null ? p : s != null ? s.origin() : BlockPos.ZERO;
    }
}
