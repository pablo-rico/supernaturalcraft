package org.papiricoh.supernaturalcraft.darkness.superior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.british.site.SiteBuilder;
import org.papiricoh.supernaturalcraft.british.site.SiteSpec;
import org.papiricoh.supernaturalcraft.entity.boss.amara.arena.FogTownLayout;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;
import org.papiricoh.supernaturalcraft.layout.LayoutPoint;

import java.util.List;
import java.util.Map;

/**
 * Superior (v0.42, "Form and Void"): the town the black fog swallowed first, one per world, raised by the unique-site machinery
 * ({@link SiteBuilder}) the first time a hunter lets the Darkness out. It is {@link FogTownLayout}'s town for good (the same
 * streets her fight raises for a while), far out on the world's ring; its square is {@link #SQUARE}. The fog lives there for as
 * long as the sun is dying ({@code DarknessEvents}).
 */
public final class Superior {

    /** The square's point (the crossroads, one block over the road). */
    public static final String SQUARE = "square";
    /** How near the square a hunter must come to have reached Superior. */
    public static final int REACHED = 28;

    public static final SiteSpec SPEC = new SiteSpec("superior", 0x5E70_0042L,
            -FogTownLayout.RADIUS, FogTownLayout.RADIUS, -FogTownLayout.RADIUS, FogTownLayout.RADIUS,
            FogTownLayout.BOTTOM, 0, FogTownLayout.TOP, 900, 150, 16, 8, q -> plan());

    private Superior() {
    }

    public static LayoutPlan plan() {
        return new LayoutPlan(List.of(new LayoutPlan.Zone("town", FogTownLayout.plan())), List.of(),
                Map.of(SQUARE, new LayoutPoint(0, 1, 0)));
    }

    /** Raises Superior if it is not yet; @return its square */
    public static BlockPos ensure(ServerLevel overworld) {
        SiteBuilder.ensure(overworld, SPEC);
        PlacedSite s = site(overworld.getServer());
        BlockPos p = s == null ? null : s.point(SQUARE);
        return p != null ? p : s != null ? s.origin() : BlockPos.ZERO;
    }

    public static @Nullable PlacedSite site(MinecraftServer server) {
        return SiteBuilder.site(server, SPEC);
    }

    /** Its square, if it was ever placed. */
    public static @Nullable BlockPos square(MinecraftServer server) {
        PlacedSite s = site(server);
        if (s == null) return null;
        BlockPos p = s.point(SQUARE);
        return p != null ? p : s.origin();
    }
}
