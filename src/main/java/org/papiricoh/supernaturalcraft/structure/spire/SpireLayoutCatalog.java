package org.papiricoh.supernaturalcraft.structure.spire;

import net.minecraft.core.BlockPos;
import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.Noise;
import org.papiricoh.supernaturalcraft.structure.HeightSampler;
import org.papiricoh.supernaturalcraft.structure.SpireLayout;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Hymnal Spire's plans for the layout dump ({@code LayoutDumpTest}) and {@code tools/structview}: every part alone, and
 * the whole spire planned on a synthetic mountain ({@link #MOUNTAIN}) with the mountain's skin drawn under it, footings run
 * down to it as the world writer would.
 */
public final class SpireLayoutCatalog {

    public static final long SEED = 20261027L;
    static final byte[] HYMN = {4, 0, 6};

    /** A rough peak: a cone falling about a block per block, shoulders and a ledge or two from smooth noise. */
    public static final HeightSampler MOUNTAIN = (x, z) -> {
        double d = Math.hypot(x, z);
        double ledge = Noise.value2(x, z, 26, 11) * 14;
        double rough = Noise.fbm2(x, z, 9, 2, 12) * 5;
        return (int) Math.round(200 - d * 1.05 + ledge + rough - 7);
    };

    public Map<String, Supplier<Canvas>> layouts() {
        Map<String, Supplier<Canvas>> out = new LinkedHashMap<>();
        out.put("spire_summit", () -> SummitLayout.canvas(SEED));
        out.put("spire_temple", () -> TempleLayout.canvas(SEED, HYMN, 0));
        out.put("spire_monastery", () -> MonasteryLayout.canvas(SEED, 0));
        out.put("spire_belfry", () -> BelfryLayout.canvas(SEED, 0));
        for (int v = 0; v < LookoutLayout.VARIANTS; v++) {
            int variant = v;
            out.put("spire_lookout_" + v, () -> LookoutLayout.canvas(SEED, variant, 0));
        }
        out.put("spire_whole", () -> whole(MOUNTAIN, SEED, true));
        return out;
    }

    /** The plan of the demo spire on {@code h}. */
    public static SpireLayout.Plan plan(HeightSampler h, long seed) {
        int peak = h.top(0, 0);
        for (int x = -27; x <= 27; x += 9) for (int z = -27; z <= 27; z += 9) peak = Math.max(peak, h.top(x, z));
        return SpireLayout.plan(h, 0, peak, 0, seed);
    }

    /**
     * The whole spire on {@code h}, in the altar's coordinates: each part with its footings run down to the ground, stamped in
     * the order the pieces are built; with {@code skin}, the mountain's top few blocks under it all.
     */
    public static Canvas whole(HeightSampler h, long seed, boolean skin) {
        SpireLayout.Plan plan = plan(h, seed);
        BlockPos a = plan.altar();
        Canvas c = new Canvas("spire_whole");
        if (skin) {
            for (int x = -84; x <= 84; x++) {
                for (int z = -84; z <= 84; z++) {
                    if (Math.hypot(x, z) > 84) continue;
                    int top = h.top(a.getX() + x, a.getZ() + z) - a.getY();
                    for (int k = 0; k <= 2; k++) c.set(x, top - k, z, k == 0 && top <= -12 ? "minecraft:grass_block" : "minecraft:stone");
                }
            }
        }
        place(c, SummitLayout.canvas(seed), a, a, h, seed);
        place(c, AscentLayout.canvas(SpireLayout.ascentInput(plan)), a, a, h, seed);
        place(c, TempleLayout.canvas(seed, plan.melody(), plan.temple().turn()), plan.templeOrigin(), a, h, seed);
        for (SpireSites.Site s : plan.sites()) {
            long ps = seed + 31L * s.node();
            Canvas part = switch (s.kind()) {
                case MONASTERY -> MonasteryLayout.canvas(ps, s.turn());
                case BELFRY -> BelfryLayout.canvas(ps, s.turn());
                case LOOKOUT -> LookoutLayout.canvas(ps, s.variant(), s.turn());
                case TEMPLE -> TempleLayout.canvas(seed, plan.melody(), s.turn());
            };
            place(c, part, s.origin(), a, h, seed);
        }
        c.anchor("altar", 0, 0, 0);
        return c.finish();
    }

    private static void place(Canvas into, Canvas part, BlockPos origin, BlockPos altar, HeightSampler h, long seed) {
        SpireKit.extendFootings(part, (x, z) -> h.top(origin.getX() + x, origin.getZ() + z) - origin.getY(), (int) (seed ^ seed >>> 32) + 909);
        into.stamp(part, origin.getX() - altar.getX(), origin.getY() - altar.getY(), origin.getZ() - altar.getZ(), 0, Canvas.Mode.SET, null);
    }

    /** The box everything of the demo spire falls in (for bounds checks). */
    public static Box reach() {
        return new Box(-SpireLayout.MAX_REACH, -120, -SpireLayout.MAX_REACH, SpireLayout.MAX_REACH, 40, SpireLayout.MAX_REACH);
    }
}
