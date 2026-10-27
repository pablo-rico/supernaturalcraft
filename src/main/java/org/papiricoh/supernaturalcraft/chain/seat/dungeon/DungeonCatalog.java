package org.papiricoh.supernaturalcraft.chain.seat.dungeon;

import org.papiricoh.supernaturalcraft.buildkit.Box;
import org.papiricoh.supernaturalcraft.buildkit.Canvas;
import org.papiricoh.supernaturalcraft.buildkit.LayoutCatalog;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.layout.LayoutPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * The strongholds of the Chain of Command (pure plans, see {@link DungeonAnchors} for the contract): the demons' <b>Pit
 * Castle</b> and the Host's <b>Monastery</b>, about 70 × 70, built by seed at the faction's seat.
 *
 * <p>The route is the same in both: an approach from the south ({@link DungeonAnchors#ENTRANCE}) over the castle's lava moat by
 * a drawbridge, or up the Monastery's processional way, through a gatehouse into the court (the castle's courtyard round the
 * Pit, the Monastery's cloister round its garth), to three wings and a yard in four slots beside the court, and north into the
 * throne hall, where the Lord's throne stands raised on a dais with a Commander's post at either hand. A stair in the hall goes
 * down to the vault under the dais. The seed picks which three of the faction's five wings are built (see {@link #rooms}),
 * which slot each takes and which stays a yard, and the smaller things: ruined towers, spires, the vault's size.
 *
 * <p>Every block is planned (no block entities; loot chests go on the {@code vault_<n>} anchors after the plan is written). The
 * same faction and seed always give the same plan.
 */
public final class DungeonCatalog implements LayoutCatalog {

    /** Seeds drawn for the visual review ({@code LayoutDumpTest}). */
    static final long[] REVIEW_SEEDS = {1L, 6L};

    @Override
    public Map<String, Supplier<LayoutPlan>> layouts() {
        Map<String, Supplier<LayoutPlan>> out = new LinkedHashMap<>();
        for (ChainFaction f : ChainFaction.values()) {
            if (f.guild()) continue;
            for (long seed : REVIEW_SEEDS) out.put(name(f) + "_" + seed, () -> plan(f, seed));
        }
        return out;
    }

    /** The stronghold's layout name: {@code chain_pit_castle} or {@code chain_monastery}. */
    public static String name(ChainFaction faction) {
        return faction == ChainFaction.DEMON ? "chain_pit_castle" : "chain_monastery";
    }

    public static LayoutPlan plan(ChainFaction faction, long seed) {
        return canvas(faction, seed).toPlan();
    }

    /** The stronghold as a finished kit canvas (tests and the layout dump). */
    public static Canvas canvas(ChainFaction faction, long seed) {
        return new Stronghold(faction, seed).draw();
    }

    /** The three wings (room names, in slot order west-north, west-south, east-north, east-south; the yard's name in its slot). */
    public static List<String> rooms(ChainFaction faction, long seed) {
        List<String> out = new ArrayList<>();
        for (Wing w : layout(faction, seed)) out.add(w.room());
        return out;
    }

    /** The wing (or yard) in each of the four slots, by seed. */
    static List<Wing> layout(ChainFaction faction, long seed) {
        Random rnd = new Random(seed * 0x9E3779B97F4A7C15L + (faction == ChainFaction.DEMON ? 17 : 31));
        List<Wing> pool = new ArrayList<>(faction == ChainFaction.DEMON ? CastleWings.POOL : MonasteryWings.POOL);
        Collections.shuffle(pool, rnd);
        List<Wing> slots = new ArrayList<>(pool.subList(0, 3));
        slots.add(faction == ChainFaction.DEMON ? CastleWings.YARD : MonasteryWings.YARD);
        Collections.shuffle(slots, rnd);
        return slots;
    }

    /** The whole plan's box. */
    static Box bounds() {
        int r = DungeonAnchors.RADIUS;
        return new Box(-r, -DungeonAnchors.DEPTH, -r, r, DungeonAnchors.HEIGHT, r);
    }
}
